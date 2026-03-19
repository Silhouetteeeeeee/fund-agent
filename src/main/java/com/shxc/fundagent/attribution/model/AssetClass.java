package com.shxc.fundagent.attribution.model;

import lombok.Getter;

/**
 * 资产类别枚举
 * 用于Brinson归因分析的大类资产配置
 */
@Getter
public enum AssetClass {
    EQUITY_LARGE("EQUITY_LARGE", "大盘股票", "大盘股基金"),
    EQUITY_SMALL("EQUITY_SMALL", "小盘股票", "小盘股基金"),
    EQUITY_GROWTH("EQUITY_GROWTH", "成长股", "成长风格基金"),
    EQUITY_VALUE("EQUITY_VALUE", "价值股", "价值风格基金"),
    BOND("BOND", "债券", "债券型基金"),
    MONEY_MARKET("MONEY_MARKET", "货币市场", "货币型基金"),
    COMMODITY("COMMODITY", "商品", "商品型基金"),
    QDII("QDII", "海外资产", "QDII基金"),
    HYBRID("HYBRID", "混合配置", "混合型基金"),
    INDEX("INDEX", "指数", "指数型基金");

    private final String code;
    private final String name;
    private final String description;

    AssetClass(String code, String name, String description) {
        this.code = code;
        this.name = name;
        this.description = description;
    }

    /**
     * 根据基金类型映射到资产类别
     */
    public static AssetClass fromFundType(String fundType) {
        if (fundType == null) {
            return HYBRID;
        }
        String type = fundType.toUpperCase();
        if (type.contains("股票") && type.contains("指数")) {
            return INDEX;
        } else if (type.contains("股票")) {
            return EQUITY_LARGE;
        } else if (type.contains("债券")) {
            return BOND;
        } else if (type.contains("货币")) {
            return MONEY_MARKET;
        } else if (type.contains("QDII") || type.contains("海外")) {
            return QDII;
        } else if (type.contains("商品")) {
            return COMMODITY;
        } else {
            return HYBRID;
        }
    }

    /**
     * 判断是否为权益类资产
     */
    public boolean isEquity() {
        return this == EQUITY_LARGE || this == EQUITY_SMALL || 
               this == EQUITY_GROWTH || this == EQUITY_VALUE || 
               this == INDEX || this == HYBRID;
    }

    /**
     * 判断是否为固收类资产
     */
    public boolean isFixedIncome() {
        return this == BOND || this == MONEY_MARKET;
    }
}
