package com.shxc.fundagent.notification.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.shxc.fundagent.enums.MessageType;
import com.shxc.fundagent.enums.NotificationChannel;
import com.shxc.fundagent.enums.SuggestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 推送消息模型类
 * 包含推送消息的所有信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NotificationMessage {

    /**
     * 消息ID
     */
    private String messageId;

    /**
     * 消息类型
     */
    private MessageType messageType;

    /**
     * 推送渠道列表
     */
    private List<NotificationChannel> channels;

    /**
     * 消息标题
     */
    private String title;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息模板ID
     */
    private String templateId;

    /**
     * 模板参数
     */
    private Map<String, Object> templateParams;

    /**
     * 接收者列表（邮箱、微信OpenID、手机号等）
     */
    private List<String> recipients;

    /**
     * 抄送列表
     */
    private List<String> ccList;

    /**
     * 密送列表
     */
    private List<String> bccList;

    /**
     * 是否紧急
     */
    private boolean urgent;

    /**
     * 是否需要确认
     */
    private boolean requireConfirmation;

    /**
     * 确认截止时间
     */
    private LocalDateTime confirmationDeadline;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 最大重试次数
     */
    private Integer maxRetries;

    /**
     * 下次重试时间
     */
    private LocalDateTime nextRetryTime;

    /**
     * 消息优先级（1-10，10为最高）
     */
    private Integer priority;

    /**
     * 消息标签（用于分类）
     */
    private List<String> tags;

    /**
     * 关联的基金代码
     */
    private String fundCode;

    /**
     * 关联的基金名称
     */
    private String fundName;

    /**
     * 关联的建议类型
     */
    private SuggestionType suggestionType;

    /**
     * 关联的收益率
     */
    private BigDecimal yieldRate;

    /**
     * 关联的日涨跌幅
     */
    private BigDecimal dailyChange;

    /**
     * 关联的风险等级
     */
    private Integer riskLevel;

    /**
     * 消息发送时间
     */
    private LocalDateTime sendTime;

    /**
     * 消息过期时间
     */
    private LocalDateTime expiryTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 元数据（自定义字段）
     */
    private Map<String, Object> metadata;

    /**
     * 回调URL（用于消息状态回调）
     */
    private String callbackUrl;

    /**
     * 是否使用HTML格式（邮件）
     */
    private boolean htmlFormat;

    /**
     * 附件列表（文件路径或URL）
     */
    private List<String> attachments;

    /**
     * 消息状态
     */
    private MessageStatus status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 消息状态枚举
     */
    public enum MessageStatus {
        PENDING("待发送"),
        SENDING("发送中"),
        SENT("已发送"),
        DELIVERED("已送达"),
        READ("已阅读"),
        FAILED("发送失败"),
        RETRYING("重试中"),
        EXPIRED("已过期"),
        CANCELLED("已取消");

        private final String description;

        MessageStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ================ 辅助方法 ================

    /**
     * 初始化模板参数
     */
    public void initTemplateParams() {
        if (this.templateParams == null) {
            this.templateParams = new HashMap<>();
        }
    }

    /**
     * 添加模板参数
     */
    public void addTemplateParam(String key, Object value) {
        initTemplateParams();
        this.templateParams.put(key, value);
    }

    /**
     * 检查消息是否有效（未过期）
     */
    public boolean isValid() {
        if (status == MessageStatus.EXPIRED || status == MessageStatus.CANCELLED) {
            return false;
        }

        if (expiryTime != null && LocalDateTime.now().isAfter(expiryTime)) {
            return false;
        }

        return true;
    }

    /**
     * 检查是否可重试
     */
    public boolean canRetry() {
        if (maxRetries == null || retryCount == null) {
            return false;
        }

        return status == MessageStatus.FAILED &&
                retryCount < maxRetries &&
                (nextRetryTime == null || LocalDateTime.now().isAfter(nextRetryTime));
    }

    /**
     * 准备重试
     */
    public void prepareForRetry(int delayMinutes) {
        this.status = MessageStatus.RETRYING;
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        this.nextRetryTime = LocalDateTime.now().plusMinutes(delayMinutes);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为失败
     */
    public void markAsFailed(String errorMessage) {
        this.status = MessageStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为成功
     */
    public void markAsSent() {
        this.status = MessageStatus.SENT;
        this.sendTime = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 生成消息摘要
     */
    public String generateSummary() {
        return String.format("[%s] %s -> %s (%s)",
                messageType != null ? messageType.getDisplayName() : "Unknown",
                title,
                channels != null ? channels.toString() : "No channels",
                status != null ? status.getDescription() : "Unknown status");
    }

    /**
     * 创建紧急风险警报消息
     */
    public static NotificationMessage createRiskAlert(String fundCode, String fundName,
                                                      BigDecimal yieldRate, BigDecimal dailyChange,
                                                      String riskDescription) {
        return NotificationMessage.builder()
                .messageType(MessageType.RISK_ALERT)
                .title(String.format("风险警报: %s(%s)", fundName, fundCode))
                .content(String.format("检测到基金%s(%s)存在风险:\n" +
                                "当前收益率: %.2f%%\n" +
                                "日涨跌幅: %.2f%%\n" +
                                "风险描述: %s",
                        fundName, fundCode,
                        yieldRate != null ? yieldRate : BigDecimal.ZERO,
                        dailyChange != null ? dailyChange : BigDecimal.ZERO,
                        riskDescription))
                .urgent(true)
                .priority(10)
                .fundCode(fundCode)
                .fundName(fundName)
                .yieldRate(yieldRate)
                .dailyChange(dailyChange)
                .createdAt(LocalDateTime.now())
                .status(MessageStatus.PENDING)
                .build();
    }

    /**
     * 创建策略提醒消息
     */
    public static NotificationMessage createStrategyAlert(String fundCode, String fundName,
                                                          SuggestionType suggestionType,
                                                          BigDecimal confidence,
                                                          String strategyDescription) {
        return NotificationMessage.builder()
                .messageType(MessageType.STRATEGY_ALERT)
                .title(String.format("策略提醒: %s(%s) - %s",
                        fundName, fundCode, suggestionType.getDescription()))
                .content(String.format("基金%s(%s)触发策略建议:\n" +
                                "建议类型: %s\n" +
                                "置信度: %.2f%%\n" +
                                "策略描述: %s",
                        fundName, fundCode,
                        suggestionType.getDescription(),
                        confidence != null ? confidence.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO,
                        strategyDescription))
                .urgent(suggestionType == SuggestionType.RISK_ALERT || suggestionType == SuggestionType.CLEAR)
                .priority(suggestionType == SuggestionType.RISK_ALERT ? 10 : 8)
                .fundCode(fundCode)
                .fundName(fundName)
                .suggestionType(suggestionType)
                .createdAt(LocalDateTime.now())
                .status(MessageStatus.PENDING)
                .build();
    }

    /**
     * 创建日报消息
     */
    public static NotificationMessage createDailyReport(String reportDate,
                                                        Map<String, Object> reportData) {
        return NotificationMessage.builder()
                .messageType(MessageType.DAILY_REPORT)
                .title(String.format("理财日报 - %s", reportDate))
                .content("今日理财日报已生成，请查收。")
                .urgent(false)
                .priority(5)
                .templateId("daily-report")
                .templateParams(reportData)
                .createdAt(LocalDateTime.now())
                .status(MessageStatus.PENDING)
                .build();
    }
}