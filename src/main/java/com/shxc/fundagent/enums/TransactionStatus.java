package com.shxc.fundagent.enums;

/**
 * 交易状态枚举
 * 记录交易的生命周期状态
 */
public enum TransactionStatus {
    PENDING("待确认", "交易已提交，等待份额确认"),
    CONFIRMED("已确认", "交易已确认，份额已到账"),
    CANCELLED("已取消", "交易已取消或失败"),
    SETTLED("已结算", "交易已完成结算");

    private final String displayName;
    private final String description;

    TransactionStatus(String displayName, String description) {
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
    public static TransactionStatus fromDisplayName(String displayName) {
        for (TransactionStatus status : values()) {
            if (status.displayName.equals(displayName)) {
                return status;
            }
        }
        return PENDING; // 默认待确认
    }
}