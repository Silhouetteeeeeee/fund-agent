package com.shxc.fundagent.notification.impl;

import com.shxc.fundagent.entity.MessageLog;
import com.shxc.fundagent.enums.MessageType;
import com.shxc.fundagent.enums.NotificationChannel;
import com.shxc.fundagent.enums.SuggestionType;
import com.shxc.fundagent.notification.NotificationService;
import com.shxc.fundagent.notification.model.NotificationMessage;
import com.shxc.fundagent.repository.MessageLogRepository;
import com.shxc.fundagent.strategy.model.StrategyDecisionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import okhttp3.OkHttpClient;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 消息推送服务实现类
 * 集成邮件、企业微信等多种推送渠道
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    // ================ 依赖注入 ================
    private final MessageLogRepository messageLogRepository;
    private final JavaMailSender mailSender;
    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    // ================ 配置参数 ================
    @Value("${notification.email.from:${spring.mail.username}}")
    private String emailFrom;

    @Value("${notification.wecom.webhook:}")
    private String wecomWebhookUrl;

    @Value("${notification.wecom.enabled:false}")
    private boolean wecomEnabled;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.max-retries:3}")
    private int maxRetries;

    @Value("${notification.retry-delay-minutes:5}")
    private int retryDelayMinutes;

    // ================ 服务状态 ================
    private final Map<String, NotificationMessage> messageCache = new ConcurrentHashMap<>();
    private final AtomicLong totalMessagesSent = new AtomicLong(0);
    private final AtomicLong totalMessagesFailed = new AtomicLong(0);
    private final Map<NotificationChannel, ChannelStatistics> channelStatistics = new ConcurrentHashMap<>();
    private long serviceStartTime;
    private final String serviceVersion = "1.0.0";

    // ================ 渠道实现映射 ================
    private final Map<NotificationChannel, MessageSender> channelSenders = new HashMap<>();

    /**
     * 消息发送器接口
     */
    private interface MessageSender {
        SendResult send(NotificationMessage message);
        boolean isAvailable();
    }

    /**
     * 初始化方法
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        serviceStartTime = System.currentTimeMillis();
        initializeChannelSenders();
        log.info("消息推送服务初始化完成，版本: {}，可用渠道: {}", serviceVersion, channelSenders.size());
    }

    /**
     * 初始化渠道发送器
     */
    private void initializeChannelSenders() {
        // 邮件发送器
        if (emailEnabled) {
            channelSenders.put(NotificationChannel.EMAIL, new EmailSender());
            log.info("邮件渠道已启用");
        }

        // 企业微信发送器
        if (wecomEnabled && wecomWebhookUrl != null && !wecomWebhookUrl.isEmpty()) {
            channelSenders.put(NotificationChannel.WECOM, new WecomSender());
            log.info("企业微信渠道已启用");
        }

        // 初始化渠道统计
        for (NotificationChannel channel : NotificationChannel.values()) {
            channelStatistics.put(channel, new ChannelStatistics(
                    channel, 0, 0, 0, 0, 0.0, 0.0, null, null, null));
        }
    }

    // ================ 消息发送方法实现 ================

    @Override
    @Transactional
    public SendResult sendMessage(NotificationMessage message) {
        if (message == null) {
            throw new IllegalArgumentException("消息不能为空");
        }

        String messageId = message.getMessageId();
        if (messageId == null) {
            messageId = generateMessageId();
            message.setMessageId(messageId);
        }

        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        message.setStatus(NotificationMessage.MessageStatus.PENDING);

        // 保存到缓存
        messageCache.put(messageId, message);

        // 保存到数据库
        saveMessageToLog(message);

        // 异步发送
        asyncSendMessage(message);

        return createSendResult(messageId, null, null, true, "消息已排队等待发送");
    }

    @Override
    public List<SendResult> sendMessages(List<NotificationMessage> messages) {
        return messages.stream()
                .map(this::sendMessage)
                .collect(Collectors.toList());
    }

    @Override
    public SendResult sendTextMessage(String title, String content,
                                      List<NotificationChannel> channels,
                                      List<String> recipients) {
        NotificationMessage message = NotificationMessage.builder()
                .title(title)
                .content(content)
                .channels(channels)
                .recipients(recipients)
                .messageType(MessageType.STRATEGY_ALERT)
                .urgent(false)
                .priority(5)
                .createdAt(LocalDateTime.now())
                .status(NotificationMessage.MessageStatus.PENDING)
                .build();

        return sendMessage(message);
    }

    @Override
    public SendResult sendTemplateMessage(String templateId, Map<String, Object> templateParams,
                                          List<NotificationChannel> channels,
                                          List<String> recipients) {
        NotificationMessage message = NotificationMessage.builder()
                .templateId(templateId)
                .templateParams(templateParams)
                .channels(channels)
                .recipients(recipients)
                .messageType(MessageType.DAILY_REPORT)
                .urgent(false)
                .priority(3)
                .createdAt(LocalDateTime.now())
                .status(NotificationMessage.MessageStatus.PENDING)
                .build();

        return sendMessage(message);
    }

    // ================ 业务消息方法实现 ================

    @Override
    public SendResult sendRiskAlert(String fundCode, String fundName,
                                    BigDecimal yieldRate, BigDecimal dailyChange,
                                    String riskDescription,
                                    List<NotificationChannel> channels,
                                    List<String> recipients) {
        NotificationMessage message = NotificationMessage.createRiskAlert(
                fundCode, fundName, yieldRate, dailyChange, riskDescription);
        message.setChannels(channels);
        message.setRecipients(recipients);
        message.setUrgent(true);
        message.setPriority(10);

        return sendMessage(message);
    }

    @Override
    public SendResult sendStrategyAlert(String fundCode, String fundName,
                                        SuggestionType suggestionType, BigDecimal confidence,
                                        String strategyDescription,
                                        List<NotificationChannel> channels,
                                        List<String> recipients) {
        NotificationMessage message = NotificationMessage.createStrategyAlert(
                fundCode, fundName, suggestionType, confidence, strategyDescription);
        message.setChannels(channels);
        message.setRecipients(recipients);
        message.setUrgent(suggestionType == SuggestionType.RISK_ALERT || suggestionType == SuggestionType.CLEAR);
        message.setPriority(suggestionType == SuggestionType.RISK_ALERT ? 10 : 8);

        return sendMessage(message);
    }

    @Override
    public SendResult sendDecisionNotification(StrategyDecisionResult decisionResult,
                                               List<NotificationChannel> channels,
                                               List<String> recipients) {
        if (decisionResult == null) {
            throw new IllegalArgumentException("决策结果不能为空");
        }

        String title = String.format("策略决策通知: %s(%s)",
                decisionResult.getFundName(), decisionResult.getFundCode());

        String content = buildDecisionNotificationContent(decisionResult);

        NotificationMessage message = NotificationMessage.builder()
                .messageType(MessageType.STRATEGY_ALERT)
                .title(title)
                .content(content)
                .channels(channels)
                .recipients(recipients)
                .fundCode(decisionResult.getFundCode())
                .fundName(decisionResult.getFundName())
                .suggestionType(decisionResult.getFinalSuggestion())
                .yieldRate(decisionResult.getCurrentYieldRate())
                .dailyChange(decisionResult.getDailyChange())
                .riskLevel(decisionResult.getRiskLevel())
                .urgent(decisionResult.getRiskLevel() != null && decisionResult.getRiskLevel() >= 4)
                .priority(decisionResult.getRiskLevel() != null && decisionResult.getRiskLevel() >= 4 ? 9 : 6)
                .createdAt(LocalDateTime.now())
                .status(NotificationMessage.MessageStatus.PENDING)
                .build();

        return sendMessage(message);
    }

    @Override
    public SendResult sendDailyReport(String reportDate, Map<String, Object> reportData,
                                      List<NotificationChannel> channels,
                                      List<String> recipients) {
        NotificationMessage message = NotificationMessage.createDailyReport(reportDate, reportData);
        message.setChannels(channels);
        message.setRecipients(recipients);

        return sendMessage(message);
    }

    @Override
    public SendResult sendWeeklyReport(String reportDate, Map<String, Object> reportData,
                                       List<NotificationChannel> channels,
                                       List<String> recipients) {
        NotificationMessage message = NotificationMessage.builder()
                .messageType(MessageType.WEEKLY_REPORT)
                .title(String.format("理财周报 - %s", reportDate))
                .content("本周理财周报已生成，请查收。")
                .templateId("weekly-report")
                .templateParams(reportData)
                .channels(channels)
                .recipients(recipients)
                .urgent(false)
                .priority(4)
                .createdAt(LocalDateTime.now())
                .status(NotificationMessage.MessageStatus.PENDING)
                .build();

        return sendMessage(message);
    }

    @Override
    public SendResult sendMonthlyReport(String reportDate, Map<String, Object> reportData,
                                        List<NotificationChannel> channels,
                                        List<String> recipients) {
        NotificationMessage message = NotificationMessage.builder()
                .messageType(MessageType.MONTHLY_REPORT)
                .title(String.format("理财月报 - %s", reportDate))
                .content("本月理财月报已生成，请查收。")
                .templateId("monthly-report")
                .templateParams(reportData)
                .channels(channels)
                .recipients(recipients)
                .urgent(false)
                .priority(3)
                .createdAt(LocalDateTime.now())
                .status(NotificationMessage.MessageStatus.PENDING)
                .build();

        return sendMessage(message);
    }

    @Override
    public SendResult sendSystemAlert(String systemName, String errorMessage,
                                      String severity,
                                      List<NotificationChannel> channels,
                                      List<String> recipients) {
        NotificationMessage message = NotificationMessage.builder()
                .messageType(MessageType.SYSTEM_ALERT)
                .title(String.format("系统告警: %s - %s", systemName, severity))
                .content(String.format("系统%s发生异常:\n%s", systemName, errorMessage))
                .channels(channels)
                .recipients(recipients)
                .urgent("CRITICAL".equals(severity) || "ERROR".equals(severity))
                .priority("CRITICAL".equals(severity) ? 10 :
                         "ERROR".equals(severity) ? 9 : 7)
                .createdAt(LocalDateTime.now())
                .status(NotificationMessage.MessageStatus.PENDING)
                .build();

        return sendMessage(message);
    }

    // ================ 消息管理方法实现 ================

    @Override
    public NotificationMessage.MessageStatus getMessageStatus(String messageId) {
        NotificationMessage message = messageCache.get(messageId);
        if (message != null) {
            return message.getStatus();
        }

        // 从数据库查询
        Optional<MessageLog> logEntry = messageLogRepository.findByMessageId(messageId);
        return logEntry.map(log -> convertCodeToStatus(log.getSendStatus()))
                .orElse(NotificationMessage.MessageStatus.FAILED);
    }

    @Override
    public NotificationMessage getMessageDetail(String messageId) {
        NotificationMessage message = messageCache.get(messageId);
        if (message != null) {
            return message;
        }

        // 从数据库查询并重建
        Optional<MessageLog> logEntry = messageLogRepository.findByMessageId(messageId);
        return logEntry.map(this::convertToNotificationMessage).orElse(null);
    }

    @Override
    public SendResult retryMessage(String messageId) {
        NotificationMessage message = messageCache.get(messageId);
        if (message == null) {
            return createSendResult(messageId, null, null, false, "消息不存在");
        }

        if (!message.canRetry()) {
            return createSendResult(messageId, null, null, false, "消息不可重试");
        }

        message.prepareForRetry(retryDelayMinutes);
        asyncSendMessage(message);

        return createSendResult(messageId, null, null, true, "消息已加入重试队列");
    }

    @Override
    public boolean cancelMessage(String messageId) {
        NotificationMessage message = messageCache.get(messageId);
        if (message == null) {
            return false;
        }

        if (message.getStatus() == NotificationMessage.MessageStatus.PENDING ||
            message.getStatus() == NotificationMessage.MessageStatus.RETRYING) {
            message.setStatus(NotificationMessage.MessageStatus.CANCELLED);
            message.setUpdatedAt(LocalDateTime.now());
            return true;
        }

        return false;
    }

    @Override
    public List<NotificationMessage> getPendingMessages(MessageType messageType,
                                                        NotificationMessage.MessageStatus status,
                                                        int limit) {
        return messageCache.values().stream()
                .filter(msg -> messageType == null || msg.getMessageType() == messageType)
                .filter(msg -> status == null || msg.getStatus() == status)
                .filter(NotificationMessage::isValid)
                .sorted(Comparator.comparingInt(NotificationMessage::getPriority).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public int cleanupExpiredMessages(LocalDateTime beforeTime) {
        int count = 0;
        Iterator<Map.Entry<String, NotificationMessage>> iterator = messageCache.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<String, NotificationMessage> entry = iterator.next();
            NotificationMessage message = entry.getValue();

            if (message.getExpiryTime() != null && message.getExpiryTime().isBefore(beforeTime)) {
                message.setStatus(NotificationMessage.MessageStatus.EXPIRED);
                iterator.remove();
                count++;
            }
        }

        if (count > 0) {
            log.info("清理了 {} 条过期消息", count);
        }

        return count;
    }

    // ================ 渠道管理方法实现 ================

    @Override
    public boolean isChannelAvailable(NotificationChannel channel) {
        MessageSender sender = channelSenders.get(channel);
        return sender != null && sender.isAvailable();
    }

    @Override
    public List<NotificationChannel> getAvailableChannels() {
        return channelSenders.entrySet().stream()
                .filter(entry -> entry.getValue().isAvailable())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public ChannelStatistics getChannelStatistics(NotificationChannel channel,
                                                  LocalDateTime startTime,
                                                  LocalDateTime endTime) {
        ChannelStatistics stats = channelStatistics.get(channel);
        if (stats == null) {
            return new ChannelStatistics(channel, 0, 0, 0, 0, 0.0, 0.0, null, startTime, endTime);
        }

        // 这里应该根据时间范围筛选统计数据
        // 简化实现：返回整体统计
        return stats;
    }

    @Override
    public boolean reloadChannelConfig(NotificationChannel channel) {
        // 重新加载渠道配置
        // 简化实现：重新初始化发送器
        try {
            if (channel == NotificationChannel.EMAIL) {
                channelSenders.put(channel, new EmailSender());
            } else if (channel == NotificationChannel.WECOM) {
                channelSenders.put(channel, new WecomSender());
            }
            return true;
        } catch (Exception e) {
            log.error("重新加载渠道配置失败: {}", channel, e);
            return false;
        }
    }

    // ================ 服务状态方法实现 ================

    @Override
    public ServiceStatus getServiceStatus() {
        long uptime = (System.currentTimeMillis() - serviceStartTime) / 1000;
        int activeChannels = (int) channelSenders.values().stream()
                .filter(MessageSender::isAvailable)
                .count();

        return new ServiceStatus(
                true,
                totalMessagesSent.get(),
                totalMessagesFailed.get(),
                activeChannels,
                channelSenders.size(),
                serviceVersion,
                uptime,
                LocalDateTime.now()
        );
    }

    @Override
    public boolean isReady() {
        return !channelSenders.isEmpty() && serviceStartTime > 0;
    }

    @Override
    public String getVersion() {
        return serviceVersion;
    }

    // ================ 私有辅助方法 ================

    /**
     * 将NotificationMessage状态转换为MessageLog状态码
     */
    private Integer convertStatusToCode(NotificationMessage.MessageStatus status) {
        if (status == null) {
            return 2; // 默认发送中
        }
        switch (status) {
            case PENDING:
            case SENDING:
            case RETRYING:
                return 2; // 发送中
            case SENT:
            case DELIVERED:
            case READ:
                return 1; // 成功
            case FAILED:
            case EXPIRED:
            case CANCELLED:
                return 0; // 失败
            default:
                return 2;
        }
    }

    /**
     * 将MessageLog状态码转换为NotificationMessage状态
     */
    private NotificationMessage.MessageStatus convertCodeToStatus(Integer statusCode) {
        if (statusCode == null) {
            return NotificationMessage.MessageStatus.PENDING;
        }
        switch (statusCode) {
            case 0:
                return NotificationMessage.MessageStatus.FAILED;
            case 1:
                return NotificationMessage.MessageStatus.SENT;
            case 2:
                return NotificationMessage.MessageStatus.SENDING;
            default:
                return NotificationMessage.MessageStatus.PENDING;
        }
    }

    /**
     * 异步发送消息
     */
    @Async
    protected void asyncSendMessage(NotificationMessage message) {
        try {
            sendMessageInternal(message);
        } catch (Exception e) {
            log.error("消息发送失败: {}", message.getMessageId(), e);
            handleSendFailure(message, e.getMessage());
        }
    }

    /**
     * 内部消息发送逻辑
     */
    private void sendMessageInternal(NotificationMessage message) {
        String messageId = message.getMessageId();
        log.info("开始发送消息: {}", messageId);

        message.setStatus(NotificationMessage.MessageStatus.SENDING);
        message.setUpdatedAt(LocalDateTime.now());

        // 如果没有指定渠道，使用所有可用渠道
        List<NotificationChannel> channels = message.getChannels();
        if (channels == null || channels.isEmpty()) {
            channels = getAvailableChannels();
            message.setChannels(channels);
        }

        // 发送到每个渠道
        List<SendResult> results = new ArrayList<>();
        for (NotificationChannel channel : channels) {
            if (!isChannelAvailable(channel)) {
                log.warn("渠道不可用: {}", channel);
                continue;
            }

            MessageSender sender = channelSenders.get(channel);
            if (sender != null) {
                try {
                    SendResult result = sender.send(message);
                    results.add(result);

                    // 更新渠道统计
                    updateChannelStatistics(channel, result.isSuccess());
                } catch (Exception e) {
                    log.error("渠道{}发送失败: {}", channel, e.getMessage());
                    SendResult errorResult = createSendResult(messageId, null, channel,
                            false, "发送失败: " + e.getMessage());
                    results.add(errorResult);
                    updateChannelStatistics(channel, false);
                }
            }
        }

        // 检查发送结果
        boolean anySuccess = results.stream().anyMatch(SendResult::isSuccess);
        if (anySuccess) {
            message.markAsSent();
            totalMessagesSent.incrementAndGet();
            log.info("消息发送成功: {}", messageId);
        } else {
            handleSendFailure(message, "所有渠道发送失败");
        }

        // 更新消息状态
        message.setUpdatedAt(LocalDateTime.now());
        updateMessageLog(message);
    }

    /**
     * 处理发送失败
     */
    private void handleSendFailure(NotificationMessage message, String errorMessage) {
        String messageId = message.getMessageId();

        if (message.canRetry()) {
            message.prepareForRetry(retryDelayMinutes);
            log.info("消息{}将重试，第{}次重试", messageId, message.getRetryCount());

            // 安排重试
            scheduleRetry(message);
        } else {
            message.markAsFailed(errorMessage);
            totalMessagesFailed.incrementAndGet();
            log.error("消息{}最终发送失败: {}", messageId, errorMessage);
        }

        updateMessageLog(message);
    }

    /**
     * 安排重试
     */
    private void scheduleRetry(NotificationMessage message) {
        // 使用Spring的定时任务或线程池进行重试
        // 简化实现：立即加入重试队列
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                asyncSendMessage(message);
            }
        }, retryDelayMinutes * 60 * 1000L);
    }

    /**
     * 更新渠道统计
     */
    private void updateChannelStatistics(NotificationChannel channel, boolean success) {
        ChannelStatistics stats = channelStatistics.get(channel);
        if (stats == null) {
            stats = new ChannelStatistics(channel, 0, 0, 0, 0, 0.0, 0.0, null, null, null);
            channelStatistics.put(channel, stats);
        }

        stats.setTotalMessages(stats.getTotalMessages() + 1);
        if (success) {
            stats.setSuccessfulMessages(stats.getSuccessfulMessages() + 1);
        } else {
            stats.setFailedMessages(stats.getFailedMessages() + 1);
        }

        if (stats.getTotalMessages() > 0) {
            stats.setSuccessRate((double) stats.getSuccessfulMessages() / stats.getTotalMessages() * 100);
        }

        stats.setLastSendTime(LocalDateTime.now());
    }

    /**
     * 保存消息到日志
     */
    private void saveMessageToLog(NotificationMessage message) {
        try {
            MessageLog logEntry = new MessageLog();
            logEntry.setMessageId(message.getMessageId());
            logEntry.setMessageType(message.getMessageType());
            logEntry.setMessageTitle(message.getTitle());
            logEntry.setMessageContent(message.getContent());

            // 设置渠道（MessageLog只有一个渠道，取第一个）
            if (message.getChannels() != null && !message.getChannels().isEmpty()) {
                logEntry.setChannel(message.getChannels().get(0));
            }

            // 设置接收者（取第一个接收者）
            if (message.getRecipients() != null && !message.getRecipients().isEmpty()) {
                logEntry.setRecipient(message.getRecipients().get(0));
            }

            logEntry.setSendStatus(convertStatusToCode(message.getStatus()));
            logEntry.setErrorMessage(message.getErrorMessage());
            logEntry.setSendTime(message.getSendTime());
            logEntry.setCreateTime(LocalDateTime.now());

            messageLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("保存消息日志失败: {}", e.getMessage());
        }
    }

    /**
     * 更新消息日志
     */
    private void updateMessageLog(NotificationMessage message) {
        try {
            Optional<MessageLog> logEntryOpt = messageLogRepository.findByMessageId(message.getMessageId());
            if (logEntryOpt.isPresent()) {
                MessageLog logEntry = logEntryOpt.get();
                logEntry.setSendStatus(convertStatusToCode(message.getStatus()));
                logEntry.setErrorMessage(message.getErrorMessage());
                logEntry.setSendTime(message.getSendTime());

                messageLogRepository.save(logEntry);
            }
        } catch (Exception e) {
            log.warn("更新消息日志失败: {}", e.getMessage());
        }
    }

    /**
     * 将MessageLog转换为NotificationMessage
     */
    private NotificationMessage convertToNotificationMessage(MessageLog logEntry) {
        NotificationMessage message = NotificationMessage.builder()
                .messageId(logEntry.getMessageId())
                .messageType(logEntry.getMessageType())
                .title(logEntry.getMessageTitle())
                .content(logEntry.getMessageContent())
                .status(convertCodeToStatus(logEntry.getSendStatus()))
                .errorMessage(logEntry.getErrorMessage())
                .sendTime(logEntry.getSendTime())
                .createdAt(logEntry.getCreateTime())
                .build();

        // 设置渠道（如果存在）
        if (logEntry.getChannel() != null) {
            List<NotificationChannel> channels = new ArrayList<>();
            channels.add(logEntry.getChannel());
            message.setChannels(channels);
        }

        // 设置接收者（如果存在）
        if (logEntry.getRecipient() != null && !logEntry.getRecipient().isEmpty()) {
            List<String> recipients = new ArrayList<>();
            recipients.add(logEntry.getRecipient());
            message.setRecipients(recipients);
        }

        return message;
    }

    /**
     * 构建决策通知内容
     */
    private String buildDecisionNotificationContent(StrategyDecisionResult decisionResult) {
        StringBuilder content = new StringBuilder();
        content.append(String.format("基金: %s(%s)\n", decisionResult.getFundName(), decisionResult.getFundCode()));
        content.append(String.format("建议: %s\n", decisionResult.getFinalSuggestion().getDescription()));
        content.append(String.format("置信度: %.2f%%\n",
                decisionResult.getFinalConfidence() != null ?
                        decisionResult.getFinalConfidence().multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO));

        if (decisionResult.getCurrentYieldRate() != null) {
            content.append(String.format("当前收益率: %.2f%%\n", decisionResult.getCurrentYieldRate()));
        }

        if (decisionResult.getDailyChange() != null) {
            content.append(String.format("日涨跌幅: %.2f%%\n", decisionResult.getDailyChange()));
        }

        if (decisionResult.getRiskLevel() != null) {
            content.append(String.format("风险等级: %d/5\n", decisionResult.getRiskLevel()));
        }

        if (decisionResult.getRiskMessage() != null) {
            content.append(String.format("风险提示: %s\n", decisionResult.getRiskMessage()));
        }

        if (decisionResult.getTriggeredRuleCount() != null && decisionResult.getTriggeredRuleCount() > 0) {
            content.append(String.format("触发规则: %d 条\n", decisionResult.getTriggeredRuleCount()));
        }

        return content.toString();
    }

    /**
     * 生成消息ID
     */
    private String generateMessageId() {
        return "MSG_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 创建发送结果
     */
    private SendResult createSendResult(String messageId, String taskId,
                                        NotificationChannel channel,
                                        boolean success, String errorMessage) {
        SendResult result = new SendResult();
        result.setSuccess(success);
        result.setMessageId(messageId);
        result.setTaskId(taskId);
        result.setChannel(channel);
        result.setSendTime(LocalDateTime.now());
        result.setErrorMessage(errorMessage);
        return result;
    }

    // ================ 渠道发送器实现 ================

    /**
     * 邮件发送器
     */
    private class EmailSender implements MessageSender {
        @Override
        public SendResult send(NotificationMessage message) {
            try {
                if (message.isHtmlFormat()) {
                    sendHtmlEmail(message);
                } else {
                    sendTextEmail(message);
                }

                log.debug("邮件发送成功: {} -> {}", message.getMessageId(), message.getRecipients());
                return createSendResult(message.getMessageId(), "EMAIL_TASK",
                        NotificationChannel.EMAIL, true, null);
            } catch (Exception e) {
                log.error("邮件发送失败: {}", e.getMessage(), e);
                return createSendResult(message.getMessageId(), "EMAIL_TASK",
                        NotificationChannel.EMAIL, false, e.getMessage());
            }
        }

        @Override
        public boolean isAvailable() {
            return emailEnabled && mailSender != null;
        }

        private void sendTextEmail(NotificationMessage message) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(emailFrom);
            mailMessage.setTo(message.getRecipients() != null ?
                    message.getRecipients().toArray(new String[0]) : new String[0]);
            mailMessage.setCc(message.getCcList() != null ?
                    message.getCcList().toArray(new String[0]) : new String[0]);
            mailMessage.setSubject(message.getTitle());
            mailMessage.setText(message.getContent());

            if (message.getBccList() != null && !message.getBccList().isEmpty()) {
                mailMessage.setBcc(message.getBccList().toArray(new String[0]));
            }

            mailSender.send(mailMessage);
        }

        private void sendHtmlEmail(NotificationMessage message) throws Exception {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(emailFrom);
            helper.setTo(message.getRecipients() != null ?
                    message.getRecipients().toArray(new String[0]) : new String[0]);
            helper.setSubject(message.getTitle());
            helper.setText(message.getContent(), true); // true表示HTML格式

            if (message.getCcList() != null && !message.getCcList().isEmpty()) {
                helper.setCc(message.getCcList().toArray(new String[0]));
            }

            if (message.getBccList() != null && !message.getBccList().isEmpty()) {
                helper.setBcc(message.getBccList().toArray(new String[0]));
            }

            // 这里可以添加附件处理
            // if (message.getAttachments() != null) {
            //     for (String attachment : message.getAttachments()) {
            //         File file = new File(attachment);
            //         helper.addAttachment(file.getName(), file);
            //     }
            // }

            mailSender.send(mimeMessage);
        }
    }

    /**
     * 企业微信发送器
     */
    private class WecomSender implements MessageSender {
        @Override
        public SendResult send(NotificationMessage message) {
            try {
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("msgtype", "text");

                Map<String, Object> textContent = new HashMap<>();
                textContent.put("content", String.format("%s\n\n%s", message.getTitle(), message.getContent()));

                if (message.getRecipients() != null && !message.getRecipients().isEmpty()) {
                    textContent.put("mentioned_list", message.getRecipients());
                }

                requestBody.put("text", textContent);

                // 发送请求 - 使用OkHttp
                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                RequestBody body = RequestBody.create(jsonBody, JSON);

                Request request = new Request.Builder()
                        .url(wecomWebhookUrl)
                        .post(body)
                        .build();

                try (Response response = okHttpClient.newCall(request).execute()) {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseBody = response.body().string();
                        Map<String, Object> responseMap = objectMapper.readValue(responseBody,
                                new TypeReference<Map<String, Object>>() {});

                        boolean success = responseMap != null && "0".equals(String.valueOf(responseMap.get("errcode")));

                        if (success) {
                            log.debug("企业微信发送成功: {}", message.getMessageId());
                            return createSendResult(message.getMessageId(), "WECOM_TASK",
                                    NotificationChannel.WECOM, true, null);
                        } else {
                            String errorMsg = responseMap != null ? String.valueOf(responseMap.get("errmsg")) : "未知错误";
                            log.error("企业微信发送失败: {}", errorMsg);
                            return createSendResult(message.getMessageId(), "WECOM_TASK",
                                    NotificationChannel.WECOM, false, errorMsg);
                        }
                    } else {
                        String errorMsg = response != null ? "HTTP " + response.code() + ": " + response.message() : "请求失败";
                        log.error("企业微信发送失败: {}", errorMsg);
                        return createSendResult(message.getMessageId(), "WECOM_TASK",
                                NotificationChannel.WECOM, false, errorMsg);
                    }
                }
            } catch (Exception e) {
                log.error("企业微信发送失败: {}", e.getMessage(), e);
                return createSendResult(message.getMessageId(), "WECOM_TASK",
                        NotificationChannel.WECOM, false, e.getMessage());
            }
        }

        @Override
        public boolean isAvailable() {
            return wecomEnabled && wecomWebhookUrl != null && !wecomWebhookUrl.isEmpty();
        }
    }

    // ================ 定时任务 ================

    /**
     * 定时清理过期消息
     */
    @Scheduled(cron = "0 0 4 * * ?") // 每天凌晨4点执行
    public void scheduledCleanup() {
        log.info("开始定时清理过期消息...");
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7); // 清理7天前的消息
        int cleanedCount = cleanupExpiredMessages(cutoffTime);
        log.info("定时清理完成，共清理 {} 条过期消息", cleanedCount);
    }

    /**
     * 定时重试失败消息
     */
    @Scheduled(fixedDelay = 300000) // 每5分钟执行一次
    public void scheduledRetry() {
        List<NotificationMessage> failedMessages = getPendingMessages(
                null, NotificationMessage.MessageStatus.FAILED, 10);

        for (NotificationMessage message : failedMessages) {
            if (message.canRetry()) {
                log.info("定时重试消息: {}", message.getMessageId());
                retryMessage(message.getMessageId());
            }
        }
    }
}