package com.shxc.fundagent.strategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.shxc.fundagent.enums.SuggestionType;
import com.shxc.fundagent.strategy.StrategyRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 策略规则配置类
 * 定义了策略规则的条件、参数和执行配置
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrategyRuleConfig {

    /**
     * 规则ID
     */
    private String ruleId;

    /**
     * 规则类型
     */
    private StrategyRuleType ruleType;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 是否启用
     */
    private boolean enabled;

    /**
     * 优先级（数字越小优先级越高）
     */
    private int priority;

    /**
     * 建议类型
     */
    private SuggestionType suggestionType;

    /**
     * 置信度阈值（0-1）
     */
    private BigDecimal confidenceThreshold;

    // ================ 条件参数 ================

    /**
     * 日涨跌幅阈值（%）
     * 正数表示涨幅，负数表示跌幅
     */
    private BigDecimal dailyChangeThreshold;

    /**
     * 周涨跌幅阈值（%）
     */
    private BigDecimal weeklyChangeThreshold;

    /**
     * 收益率阈值（%）
     */
    private BigDecimal yieldRateThreshold;

    /**
     * 最小收益率阈值（%）
     */
    private BigDecimal minYieldRateThreshold;

    /**
     * 最大收益率阈值（%）
     */
    private BigDecimal maxYieldRateThreshold;

    /**
     * 波动率阈值（%）
     */
    private BigDecimal volatilityThreshold;

    /**
     * 夏普比率阈值
     */
    private BigDecimal sharpeRatioThreshold;

    /**
     * 最大回撤阈值（%）
     */
    private BigDecimal maxDrawdownThreshold;

    /**
     * 持仓时间阈值（天）
     */
    private Integer holdingDaysThreshold;

    /**
     * 成交量变化阈值（%）
     */
    private BigDecimal volumeChangeThreshold;

    /**
     * RSI阈值（0-100）
     */
    private BigDecimal rsiThreshold;

    // ================ 执行配置 ================

    /**
     * 是否发送通知
     */
    private boolean sendNotification;

    /**
     * 是否为紧急通知
     */
    private boolean urgentNotification;

    /**
     * 通知通道（JSON数组字符串，如 ["EMAIL", "WECOM"]）
     */
    private String notificationChannels;

    /**
     * 是否自动执行建议
     */
    private boolean autoExecute;

    /**
     * 执行前需要确认
     */
    private boolean requireConfirmation;

    /**
     * 最小执行金额（元）
     */
    private BigDecimal minExecutionAmount;

    /**
     * 最大执行金额（元）
     */
    private BigDecimal maxExecutionAmount;

    /**
     * 执行比例（0-1，表示建议执行的比例）
     */
    private BigDecimal executionRatio;

    // ================ 时间配置 ================

    /**
     * 生效开始时间
     */
    private LocalDateTime effectiveStartTime;

    /**
     * 生效结束时间
     */
    private LocalDateTime effectiveEndTime;

    /**
     * 只在交易日执行
     */
    private boolean onlyTradingDays;

    /**
     * 执行时间段开始（格式："HH:mm"）
     */
    private String executionStartTime;

    /**
     * 执行时间段结束（格式："HH:mm"）
     */
    private String executionEndTime;

    // ================ 附加配置 ================

    /**
     * 自定义条件表达式（支持SpEL）
     */
    private String customConditionExpression;

    /**
     * 自定义参数（JSON格式）
     */
    private String customParams;

    /**
     * 标签（用于分类和过滤）
     */
    private String tags;

    /**
     * 备注
     */
    private String remarks;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    // ================ 辅助方法 ================

    /**
     * 检查规则是否有效（在有效期内）
     */
    public boolean isEffective() {
        if (!enabled) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        // 检查生效时间
        if (effectiveStartTime != null && now.isBefore(effectiveStartTime)) {
            return false;
        }

        if (effectiveEndTime != null && now.isAfter(effectiveEndTime)) {
            return false;
        }

        // 检查执行时间段
        if (executionStartTime != null && executionEndTime != null) {
            // 这里可以添加时间段检查逻辑
            // 简单起见，假设始终有效
        }

        return true;
    }

    /**
     * 获取规则配置摘要
     */
    public String getSummary() {
        return String.format("%s [%s] - 优先级: %d, 建议: %s, 启用: %s",
                ruleName, ruleType.getName(), priority,
                suggestionType != null ? suggestionType.name() : "N/A",
                enabled ? "是" : "否");
    }

    /**
     * 转换为Map格式（用于条件检查）
     */
    public Map<String, Object> toConditionMap() {
        Map<String, Object> map = new HashMap<>();

        if (dailyChangeThreshold != null) {
            map.put("dailyChangeThreshold", dailyChangeThreshold);
        }
        if (weeklyChangeThreshold != null) {
            map.put("weeklyChangeThreshold", weeklyChangeThreshold);
        }
        if (yieldRateThreshold != null) {
            map.put("yieldRateThreshold", yieldRateThreshold);
        }
        if (minYieldRateThreshold != null) {
            map.put("minYieldRateThreshold", minYieldRateThreshold);
        }
        if (maxYieldRateThreshold != null) {
            map.put("maxYieldRateThreshold", maxYieldRateThreshold);
        }
        if (volatilityThreshold != null) {
            map.put("volatilityThreshold", volatilityThreshold);
        }
        if (sharpeRatioThreshold != null) {
            map.put("sharpeRatioThreshold", sharpeRatioThreshold);
        }
        if (maxDrawdownThreshold != null) {
            map.put("maxDrawdownThreshold", maxDrawdownThreshold);
        }
        if (holdingDaysThreshold != null) {
            map.put("holdingDaysThreshold", holdingDaysThreshold);
        }
        if (volumeChangeThreshold != null) {
            map.put("volumeChangeThreshold", volumeChangeThreshold);
        }
        if (rsiThreshold != null) {
            map.put("rsiThreshold", rsiThreshold);
        }

        return map;
    }

    /**
     * 检查是否包含特定条件的配置
     */
    public boolean hasCondition(String conditionName) {
        switch (conditionName) {
            case "dailyChange":
                return dailyChangeThreshold != null;
            case "weeklyChange":
                return weeklyChangeThreshold != null;
            case "yieldRate":
                return yieldRateThreshold != null;
            case "volatility":
                return volatilityThreshold != null;
            case "sharpeRatio":
                return sharpeRatioThreshold != null;
            case "maxDrawdown":
                return maxDrawdownThreshold != null;
            case "holdingDays":
                return holdingDaysThreshold != null;
            case "volumeChange":
                return volumeChangeThreshold != null;
            case "rsi":
                return rsiThreshold != null;
            default:
                return false;
        }
    }
}