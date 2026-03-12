package com.shxc.fundagent.notification;

import com.shxc.fundagent.enums.MessageType;
import com.shxc.fundagent.enums.NotificationChannel;
import com.shxc.fundagent.enums.SuggestionType;
import com.shxc.fundagent.notification.model.NotificationMessage;
import com.shxc.fundagent.strategy.model.StrategyDecisionResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息推送服务接口
 * 负责处理各种通知的发送和管理
 */
public interface NotificationService {

    // ================ 消息发送方法 ================

    /**
     * 发送单个消息
     *
     * @param message 消息对象
     * @return 发送结果
     */
    SendResult sendMessage(NotificationMessage message);

    /**
     * 批量发送消息
     *
     * @param messages 消息列表
     * @return 发送结果列表
     */
    List<SendResult> sendMessages(List<NotificationMessage> messages);

    /**
     * 发送文本消息到指定渠道
     *
     * @param title     消息标题
     * @param content   消息内容
     * @param channels  推送渠道
     * @param recipients 接收者列表
     * @return 发送结果
     */
    SendResult sendTextMessage(String title, String content,
                               List<NotificationChannel> channels,
                               List<String> recipients);

    /**
     * 发送模板消息
     *
     * @param templateId     模板ID
     * @param templateParams 模板参数
     * @param channels       推送渠道
     * @param recipients     接收者列表
     * @return 发送结果
     */
    SendResult sendTemplateMessage(String templateId, Map<String, Object> templateParams,
                                   List<NotificationChannel> channels,
                                   List<String> recipients);

    // ================ 业务消息方法 ================

    /**
     * 发送风险警报
     *
     * @param fundCode       基金代码
     * @param fundName       基金名称
     * @param yieldRate      收益率
     * @param dailyChange    日涨跌幅
     * @param riskDescription 风险描述
     * @param channels       推送渠道
     * @param recipients     接收者列表
     * @return 发送结果
     */
    SendResult sendRiskAlert(String fundCode, String fundName,
                             BigDecimal yieldRate, BigDecimal dailyChange,
                             String riskDescription,
                             List<NotificationChannel> channels,
                             List<String> recipients);

    /**
     * 发送策略提醒
     *
     * @param fundCode            基金代码
     * @param fundName            基金名称
     * @param suggestionType      建议类型
     * @param confidence          置信度
     * @param strategyDescription 策略描述
     * @param channels            推送渠道
     * @param recipients          接收者列表
     * @return 发送结果
     */
    SendResult sendStrategyAlert(String fundCode, String fundName,
                                 SuggestionType suggestionType, BigDecimal confidence,
                                 String strategyDescription,
                                 List<NotificationChannel> channels,
                                 List<String> recipients);

    /**
     * 发送决策结果通知
     *
     * @param decisionResult 决策结果
     * @param channels       推送渠道
     * @param recipients     接收者列表
     * @return 发送结果
     */
    SendResult sendDecisionNotification(StrategyDecisionResult decisionResult,
                                        List<NotificationChannel> channels,
                                        List<String> recipients);

    /**
     * 发送日报
     *
     * @param reportDate   报告日期
     * @param reportData   报告数据
     * @param channels     推送渠道
     * @param recipients   接收者列表
     * @return 发送结果
     */
    SendResult sendDailyReport(String reportDate, Map<String, Object> reportData,
                               List<NotificationChannel> channels,
                               List<String> recipients);

    /**
     * 发送周报
     *
     * @param reportDate   报告日期
     * @param reportData   报告数据
     * @param channels     推送渠道
     * @param recipients   接收者列表
     * @return 发送结果
     */
    SendResult sendWeeklyReport(String reportDate, Map<String, Object> reportData,
                                List<NotificationChannel> channels,
                                List<String> recipients);

    /**
     * 发送月报
     *
     * @param reportDate   报告日期
     * @param reportData   报告数据
     * @param channels     推送渠道
     * @param recipients   接收者列表
     * @return 发送结果
     */
    SendResult sendMonthlyReport(String reportDate, Map<String, Object> reportData,
                                 List<NotificationChannel> channels,
                                 List<String> recipients);

    /**
     * 发送系统告警
     *
     * @param systemName   系统名称
     * @param errorMessage 错误信息
     * @param severity     严重程度
     * @param channels     推送渠道
     * @param recipients   接收者列表
     * @return 发送结果
     */
    SendResult sendSystemAlert(String systemName, String errorMessage,
                               String severity,
                               List<NotificationChannel> channels,
                               List<String> recipients);

    // ================ 消息管理方法 ================

    /**
     * 获取消息状态
     *
     * @param messageId 消息ID
     * @return 消息状态
     */
    NotificationMessage.MessageStatus getMessageStatus(String messageId);

    /**
     * 获取消息详情
     *
     * @param messageId 消息ID
     * @return 消息详情
     */
    NotificationMessage getMessageDetail(String messageId);

    /**
     * 重试发送失败的消息
     *
     * @param messageId 消息ID
     * @return 重试结果
     */
    SendResult retryMessage(String messageId);

    /**
     * 取消待发送的消息
     *
     * @param messageId 消息ID
     * @return 取消结果
     */
    boolean cancelMessage(String messageId);

    /**
     * 获取待发送消息列表
     *
     * @param messageType 消息类型（可选）
     * @param status      消息状态（可选）
     * @param limit       限制数量
     * @return 消息列表
     */
    List<NotificationMessage> getPendingMessages(MessageType messageType,
                                                 NotificationMessage.MessageStatus status,
                                                 int limit);

    /**
     * 清理过期消息
     *
     * @param beforeTime 清理此时间之前的消息
     * @return 清理数量
     */
    int cleanupExpiredMessages(LocalDateTime beforeTime);

    // ================ 渠道管理方法 ================

    /**
     * 检查渠道是否可用
     *
     * @param channel 推送渠道
     * @return 是否可用
     */
    boolean isChannelAvailable(NotificationChannel channel);

    /**
     * 获取所有可用渠道
     *
     * @return 可用渠道列表
     */
    List<NotificationChannel> getAvailableChannels();

    /**
     * 获取渠道统计信息
     *
     * @param channel 推送渠道
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计信息
     */
    ChannelStatistics getChannelStatistics(NotificationChannel channel,
                                           LocalDateTime startTime,
                                           LocalDateTime endTime);

    /**
     * 重新加载渠道配置
     *
     * @param channel 推送渠道
     * @return 重载结果
     */
    boolean reloadChannelConfig(NotificationChannel channel);

    // ================ 服务状态方法 ================

    /**
     * 获取服务状态
     *
     * @return 服务状态
     */
    ServiceStatus getServiceStatus();

    /**
     * 检查服务是否就绪
     *
     * @return 是否就绪
     */
    boolean isReady();

    /**
     * 获取服务版本
     *
     * @return 版本号
     */
    String getVersion();

    // ================ 数据模型类 ================

    /**
     * 发送结果类
     */
    class SendResult {
        private boolean success;
        private String messageId;
        private String taskId;
        private NotificationChannel channel;
        private LocalDateTime sendTime;
        private String errorMessage;
        private Map<String, Object> extraInfo;

        // 构造器
        public SendResult() {
        }

        public SendResult(boolean success, String messageId, String taskId,
                          NotificationChannel channel, LocalDateTime sendTime,
                          String errorMessage, Map<String, Object> extraInfo) {
            this.success = success;
            this.messageId = messageId;
            this.taskId = taskId;
            this.channel = channel;
            this.sendTime = sendTime;
            this.errorMessage = errorMessage;
            this.extraInfo = extraInfo;
        }

        // Getter和Setter
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }

        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }

        public NotificationChannel getChannel() { return channel; }
        public void setChannel(NotificationChannel channel) { this.channel = channel; }

        public LocalDateTime getSendTime() { return sendTime; }
        public void setSendTime(LocalDateTime sendTime) { this.sendTime = sendTime; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public Map<String, Object> getExtraInfo() { return extraInfo; }
        public void setExtraInfo(Map<String, Object> extraInfo) { this.extraInfo = extraInfo; }
    }

    /**
     * 渠道统计类
     */
    class ChannelStatistics {
        private NotificationChannel channel;
        private long totalMessages;
        private long successfulMessages;
        private long failedMessages;
        private long pendingMessages;
        private double successRate;
        private double averageSendTimeMs;
        private LocalDateTime lastSendTime;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;

        // 构造器
        public ChannelStatistics() {
        }

        public ChannelStatistics(NotificationChannel channel, long totalMessages,
                                long successfulMessages, long failedMessages,
                                long pendingMessages, double successRate,
                                double averageSendTimeMs, LocalDateTime lastSendTime,
                                LocalDateTime periodStart, LocalDateTime periodEnd) {
            this.channel = channel;
            this.totalMessages = totalMessages;
            this.successfulMessages = successfulMessages;
            this.failedMessages = failedMessages;
            this.pendingMessages = pendingMessages;
            this.successRate = successRate;
            this.averageSendTimeMs = averageSendTimeMs;
            this.lastSendTime = lastSendTime;
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
        }

        // Getter和Setter
        public NotificationChannel getChannel() { return channel; }
        public void setChannel(NotificationChannel channel) { this.channel = channel; }

        public long getTotalMessages() { return totalMessages; }
        public void setTotalMessages(long totalMessages) { this.totalMessages = totalMessages; }

        public long getSuccessfulMessages() { return successfulMessages; }
        public void setSuccessfulMessages(long successfulMessages) { this.successfulMessages = successfulMessages; }

        public long getFailedMessages() { return failedMessages; }
        public void setFailedMessages(long failedMessages) { this.failedMessages = failedMessages; }

        public long getPendingMessages() { return pendingMessages; }
        public void setPendingMessages(long pendingMessages) { this.pendingMessages = pendingMessages; }

        public double getSuccessRate() { return successRate; }
        public void setSuccessRate(double successRate) { this.successRate = successRate; }

        public double getAverageSendTimeMs() { return averageSendTimeMs; }
        public void setAverageSendTimeMs(double averageSendTimeMs) { this.averageSendTimeMs = averageSendTimeMs; }

        public LocalDateTime getLastSendTime() { return lastSendTime; }
        public void setLastSendTime(LocalDateTime lastSendTime) { this.lastSendTime = lastSendTime; }

        public LocalDateTime getPeriodStart() { return periodStart; }
        public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }

        public LocalDateTime getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
    }

    /**
     * 服务状态类
     */
    class ServiceStatus {
        private boolean running;
        private long totalMessagesSent;
        private long totalMessagesFailed;
        private int activeChannels;
        private int totalChannels;
        private String serviceVersion;
        private long uptimeSeconds;
        private LocalDateTime lastActivityTime;

        // 构造器
        public ServiceStatus() {
        }

        public ServiceStatus(boolean running, long totalMessagesSent,
                            long totalMessagesFailed, int activeChannels,
                            int totalChannels, String serviceVersion,
                            long uptimeSeconds, LocalDateTime lastActivityTime) {
            this.running = running;
            this.totalMessagesSent = totalMessagesSent;
            this.totalMessagesFailed = totalMessagesFailed;
            this.activeChannels = activeChannels;
            this.totalChannels = totalChannels;
            this.serviceVersion = serviceVersion;
            this.uptimeSeconds = uptimeSeconds;
            this.lastActivityTime = lastActivityTime;
        }

        // Getter和Setter
        public boolean isRunning() { return running; }
        public void setRunning(boolean running) { this.running = running; }

        public long getTotalMessagesSent() { return totalMessagesSent; }
        public void setTotalMessagesSent(long totalMessagesSent) { this.totalMessagesSent = totalMessagesSent; }

        public long getTotalMessagesFailed() { return totalMessagesFailed; }
        public void setTotalMessagesFailed(long totalMessagesFailed) { this.totalMessagesFailed = totalMessagesFailed; }

        public int getActiveChannels() { return activeChannels; }
        public void setActiveChannels(int activeChannels) { this.activeChannels = activeChannels; }

        public int getTotalChannels() { return totalChannels; }
        public void setTotalChannels(int totalChannels) { this.totalChannels = totalChannels; }

        public String getServiceVersion() { return serviceVersion; }
        public void setServiceVersion(String serviceVersion) { this.serviceVersion = serviceVersion; }

        public long getUptimeSeconds() { return uptimeSeconds; }
        public void setUptimeSeconds(long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }

        public LocalDateTime getLastActivityTime() { return lastActivityTime; }
        public void setLastActivityTime(LocalDateTime lastActivityTime) { this.lastActivityTime = lastActivityTime; }
    }
}