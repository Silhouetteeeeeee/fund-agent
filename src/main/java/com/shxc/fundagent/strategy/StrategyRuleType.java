package com.shxc.fundagent.strategy;

/**
 * 策略规则类型枚举
 * 定义了系统支持的各种策略规则类型
 */
public enum StrategyRuleType {
    /**
     * 极端风险规则 - 当日或周跌幅过大时发出风险警报
     */
    EXTREME_RISK("EXTREME_RISK", "极端风险规则", "当日或周跌幅过大时发出风险警报", 1, true),

    /**
     * 止盈规则 - 收益率达到一定阈值时建议清仓
     */
    PROFIT_TAKING("PROFIT_TAKING", "止盈规则", "收益率达到一定阈值时建议清仓", 2, true),

    /**
     * 高估规则 - 基金价格高估时建议卖出
     */
    OVERVALUED("OVERVALUED", "高估规则", "基金价格高估时建议卖出", 3, true),

    /**
     * 低估规则 - 基金价格低估时建议买入
     */
    UNDERVALUED("UNDERVALUED", "低估规则", "基金价格低估时建议买入", 4, true),

    /**
     * 正常持有规则 - 收益在正常范围内建议持有
     */
    NORMAL_HOLD("NORMAL_HOLD", "正常持有规则", "收益在正常范围内建议持有", 5, true),

    /**
     * 趋势跟踪规则 - 基于趋势指标进行决策
     */
    TREND_FOLLOWING("TREND_FOLLOWING", "趋势跟踪规则", "基于趋势指标进行决策", 6, false),

    /**
     * 均值回归规则 - 基于均值回归理论进行决策
     */
    MEAN_REVERSION("MEAN_REVERSION", "均值回归规则", "基于均值回归理论进行决策", 7, false),

    /**
     * 技术指标规则 - 基于技术指标进行决策
     */
    TECHNICAL_INDICATOR("TECHNICAL_INDICATOR", "技术指标规则", "基于技术指标进行决策", 8, false),

    /**
     * 自定义规则 - 用户自定义策略规则
     */
    CUSTOM("CUSTOM", "自定义规则", "用户自定义策略规则", 9, false);

    private final String code;
    private final String name;
    private final String description;
    private final int priority;
    private final boolean enabledByDefault;

    StrategyRuleType(String code, String name, String description, int priority, boolean enabledByDefault) {
        this.code = code;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.enabledByDefault = enabledByDefault;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    /**
     * 根据代码获取策略规则类型
     */
    public static StrategyRuleType fromCode(String code) {
        for (StrategyRuleType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的策略规则类型: " + code);
    }

    /**
     * 检查是否有效的策略规则类型
     */
    public static boolean isValid(String code) {
        for (StrategyRuleType type : values()) {
            if (type.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }
}