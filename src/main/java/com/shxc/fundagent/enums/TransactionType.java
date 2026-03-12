package com.shxc.fundagent.enums;

/**
 * 交易类型枚举
 * 记录基金交易的类型：购买或赎回
 */
public enum TransactionType {
    BUY("购买", "基金申购或买入操作"),
    SELL("赎回", "基金赎回或卖出操作");

    private final String displayName;
    private final String description;

    TransactionType(String displayName, String description) {
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
    public static TransactionType fromDisplayName(String displayName) {
        for (TransactionType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return BUY; // 默认购买
    }
}