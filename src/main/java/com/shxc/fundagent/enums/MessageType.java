package com.shxc.fundagent.enums;

/**
 * 消息类型枚举
 * 对应详细设计文档中的消息推送类型
 */
public enum MessageType {
    DAILY_REPORT("理财日报", "每日理财报告"),
    WEEKLY_REPORT("周度报告", "每周理财报告"),
    MONTHLY_REPORT("月度报告", "月度理财报告"),
    RISK_ALERT("风险预警", "风险预警通知"),
    STRATEGY_ALERT("策略提醒", "策略触发提醒"),
    SYSTEM_ALERT("系统告警", "系统异常告警");

    private final String displayName;
    private final String description;

    MessageType(String displayName, String description) {
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
    public static MessageType fromDisplayName(String displayName) {
        for (MessageType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return DAILY_REPORT; // 默认日报
    }
}