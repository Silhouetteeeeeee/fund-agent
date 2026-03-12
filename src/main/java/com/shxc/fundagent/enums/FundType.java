package com.shxc.fundagent.enums;

/**
 * 基金类型枚举
 * 对应详细设计文档中的基金类型分类
 */
public enum FundType {
    MIXED("混合型", "混合基金，投资于股票和债券等多种资产"),
    STOCK("股票型", "主要投资于股票的基金"),
    INDEX("指数型", "跟踪特定指数的基金"),
    BOND("债券型", "主要投资于债券的基金"),
    INDUSTRY("行业型", "专注于特定行业或主题的基金"),
    MONEY_MARKET("货币型", "投资于短期货币市场工具的基金"),
    QDII("QDII型", "投资于海外市场的基金"),
    OTHER("其他", "其他类型基金");

    private final String displayName;
    private final String description;

    FundType(String displayName, String description) {
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
    public static FundType fromDisplayName(String displayName) {
        for (FundType type : values()) {
            if (type.displayName.equals(displayName)) {
                return type;
            }
        }
        return OTHER;
    }
}