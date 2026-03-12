package com.shxc.fundagent.enums;

/**
 * 操作建议类型枚举
 * 对应详细设计文档中的策略决策结果
 */
public enum SuggestionType {
    BUY("加仓", "建议买入或加仓", 4),
    HOLD("持有", "建议继续持有", 5),
    SELL("减仓", "建议卖出部分持仓", 3),
    CLEAR("清仓", "建议全部卖出", 2),
    RISK_ALERT("风险预警", "风险预警提示", 1);

    private final String displayName;
    private final String description;
    private final int priority;

    SuggestionType(String displayName, String description, int priority) {
        this.displayName = displayName;
        this.description = description;
        this.priority = priority;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * 根据显示名称获取枚举
     */
    public static SuggestionType fromDisplayName(String displayName) {
        for (SuggestionType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return HOLD; // 默认持有
    }
}