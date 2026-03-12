package com.shxc.fundagent.enums;

/**
 * 推送渠道枚举
 * 对应详细设计文档中的消息推送渠道
 */
public enum NotificationChannel {
    WECHAT("微信", "微信服务号推送"),
    WECOM("企业微信", "企业微信机器人推送"),
    EMAIL("邮件", "电子邮件推送"),
    SMS("短信", "短信推送");

    private final String displayName;
    private final String description;

    NotificationChannel(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据显示名称获取枚举
     */
    public static NotificationChannel fromDisplayName(String displayName) {
        for (NotificationChannel channel : values()) {
            if (channel.displayName.equals(displayName)) {
                return channel;
            }
        }
        return EMAIL; // 默认邮件
    }
}