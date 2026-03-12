package com.shxc.fundagent.entity;

import com.shxc.fundagent.enums.MessageType;
import com.shxc.fundagent.enums.NotificationChannel;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 消息推送记录实体
 * 对应详细设计文档中的 message_log 表
 */
@Entity
@Table(name = "message_log")
@Data
public class MessageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 消息业务ID（用于外部引用）
     */
    @Column(name = "message_id", unique = true, length = 50)
    private String messageId;

    /**
     * 消息类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    private MessageType messageType;

    /**
     * 消息标题
     */
    @Column(name = "message_title", nullable = false, length = 100)
    private String messageTitle;

    /**
     * 消息内容
     */
    @Lob
    @Column(name = "message_content", nullable = false)
    private String messageContent;

    /**
     * 接收者标识
     */
    @Column(name = "recipient", length = 100)
    private String recipient;

    /**
     * 推送渠道
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;

    /**
     * 发送状态：0-失败，1-成功，2-发送中
     */
    @Column(name = "send_status", nullable = false)
    private Integer sendStatus = 2;

    /**
     * 重试次数
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /**
     * 最大重试次数
     */
    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    /**
     * 错误信息
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * 计划发送时间
     */
    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    /**
     * 实际发送时间
     */
    @Column(name = "send_time")
    private LocalDateTime sendTime;

    /**
     * 过期时间
     */
    @Column(name = "expire_time")
    private LocalDateTime expireTime;

    /**
     * 优先级：1-最高，2-高，3-中，4-低
     */
    @Column(name = "priority")
    private Integer priority = 3;

    /**
     * 是否紧急
     */
    @Column(name = "is_urgent")
    private Boolean isUrgent = false;

    /**
     * 相关数据ID（如基金代码、策略日志ID等）
     */
    @Column(name = "related_id", length = 50)
    private String relatedId;

    /**
     * 相关类型
     */
    @Column(name = "related_type", length = 50)
    private String relatedType;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 无参构造函数
     */
    public MessageLog() {
    }

    /**
     * 带基本参数的构造函数
     */
    public MessageLog(MessageType messageType, String messageTitle, String messageContent,
                     NotificationChannel channel, String recipient) {
        this.messageType = messageType;
        this.messageTitle = messageTitle;
        this.messageContent = messageContent;
        this.channel = channel;
        this.recipient = recipient;
        this.scheduledTime = LocalDateTime.now();
    }

    /**
     * 获取状态描述
     */
    public String getStatusDescription() {
        switch (sendStatus) {
            case 0: return "发送失败";
            case 1: return "发送成功";
            case 2: return "发送中";
            default: return "未知状态";
        }
    }

    /**
     * 标记为发送成功
     */
    public void markAsSuccess() {
        this.sendStatus = 1;
        this.sendTime = LocalDateTime.now();
        this.errorMessage = null;
    }

    /**
     * 标记为发送失败
     */
    public void markAsFailed(String error) {
        this.sendStatus = 0;
        this.sendTime = LocalDateTime.now();
        this.errorMessage = error;
        this.retryCount++;
    }

    /**
     * 判断是否可以重试
     */
    public boolean canRetry() {
        return sendStatus == 0 && retryCount < maxRetries;
    }

    /**
     * 判断是否已过期
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 获取消息类型显示名称
     */
    public String getMessageTypeDisplayName() {
        return messageType != null ? messageType.getDisplayName() : "未知";
    }

    /**
     * 获取渠道显示名称
     */
    public String getChannelDisplayName() {
        return channel != null ? channel.getDisplayName() : "未知";
    }
}