package com.shxc.fundagent.strategy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.shxc.fundagent.entity.FundHolding;
import com.shxc.fundagent.enums.SuggestionType;
import com.shxc.fundagent.strategy.StrategyRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 策略决策结果类
 * 包含了单次决策的完整结果信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StrategyDecisionResult {

    /**
     * 决策ID
     */
    private String decisionId;

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 基金名称
     */
    private String fundName;

    /**
     * 持仓ID（如果有）
     */
    private Long holdingId;

    /**
     * 最终建议类型
     */
    private SuggestionType finalSuggestion;

    /**
     * 最终置信度（0-1）
     */
    private BigDecimal finalConfidence;

    /**
     * 决策时间
     */
    private LocalDateTime decisionTime;

    // ================ 财务指标 ================

    /**
     * 当前价格（元）
     */
    private BigDecimal currentPrice;

    /**
     * 当前收益率（%）
     */
    private BigDecimal currentYieldRate;

    /**
     * 日涨跌幅（%）
     */
    private BigDecimal dailyChange;

    /**
     * 周涨跌幅（%）
     */
    private BigDecimal weeklyChange;

    /**
     * 月涨跌幅（%）
     */
    private BigDecimal monthlyChange;

    /**
     * 年化收益率（%）
     */
    private BigDecimal annualizedReturn;

    /**
     * 波动率（%）
     */
    private BigDecimal volatility;

    /**
     * 夏普比率
     */
    private BigDecimal sharpeRatio;

    /**
     * 最大回撤（%）
     */
    private BigDecimal maxDrawdown;

    /**
     * 持仓天数（如果有持仓）
     */
    private Integer holdingDays;

    /**
     * 持仓成本（元）
     */
    private BigDecimal holdingCost;

    /**
     * 持仓数量
     */
    private BigDecimal holdingQuantity;

    /**
     * 持仓市值（元）
     */
    private BigDecimal holdingValue;

    /**
     * 持仓盈亏（元）
     */
    private BigDecimal holdingProfit;

    /**
     * 持仓盈亏率（%）
     */
    private BigDecimal holdingProfitRate;

    // ================ 规则匹配结果 ================

    /**
     * 触发的规则列表
     */
    private List<RuleMatchResult> matchedRules;

    /**
     * 触发的规则类型
     */
    private List<StrategyRuleType> triggeredRuleTypes;

    /**
     * 触发的规则数量
     */
    private Integer triggeredRuleCount;

    // ================ 执行建议 ================

    /**
     * 建议执行金额（元）
     */
    private BigDecimal suggestedAmount;

    /**
     * 建议执行数量
     */
    private BigDecimal suggestedQuantity;

    /**
     * 建议执行比例（0-1）
     */
    private BigDecimal suggestedRatio;

    /**
     * 建议执行时间
     */
    private LocalDateTime suggestedExecutionTime;

    /**
     * 建议有效期（小时）
     */
    private Integer suggestionValidityHours;

    // ================ 风险提示 ================

    /**
     * 风险等级（1-5，1为最低，5为最高）
     */
    private Integer riskLevel;

    /**
     * 风险提示信息
     */
    private String riskMessage;

    /**
     * 风险评估得分（0-100）
     */
    private Integer riskScore;

    // ================ 市场环境 ================

    /**
     * 市场情绪指数（0-100）
     */
    private Integer marketSentiment;

    /**
     * 行业表现评级（A-F）
     */
    private String industryRating;

    /**
     * 宏观经济指标得分（0-100）
     */
    private Integer macroeconomicScore;

    // ================ 元数据 ================

    /**
     * 决策引擎版本
     */
    private String engineVersion;

    /**
     * 数据更新时间
     */
    private LocalDateTime dataUpdateTime;

    /**
     * 决策计算耗时（毫秒）
     */
    private Long calculationTimeMs;

    /**
     * 是否来自缓存
     */
    private boolean fromCache;

    /**
     * 备注信息
     */
    private String remarks;

    // ================ 辅助方法 ================

    /**
     * 初始化匹配规则列表
     */
    public void initMatchedRules() {
        if (this.matchedRules == null) {
            this.matchedRules = new ArrayList<>();
        }
        if (this.triggeredRuleTypes == null) {
            this.triggeredRuleTypes = new ArrayList<>();
        }
    }

    /**
     * 添加匹配的规则结果
     */
    public void addMatchedRule(RuleMatchResult ruleResult) {
        initMatchedRules();
        this.matchedRules.add(ruleResult);
        if (ruleResult.getRuleType() != null && !this.triggeredRuleTypes.contains(ruleResult.getRuleType())) {
            this.triggeredRuleTypes.add(ruleResult.getRuleType());
        }
        this.triggeredRuleCount = this.matchedRules.size();
    }

    /**
     * 检查是否有触发任何规则
     */
    public boolean hasTriggeredRules() {
        return triggeredRuleCount != null && triggeredRuleCount > 0;
    }

    /**
     * 获取最高优先级的建议（如果有冲突）
     */
    public SuggestionType getHighestPrioritySuggestion() {
        if (matchedRules == null || matchedRules.isEmpty()) {
            return finalSuggestion;
        }

        SuggestionType highestPriority = null;
        int highestPriorityValue = Integer.MAX_VALUE;

        for (RuleMatchResult rule : matchedRules) {
            if (rule.getSuggestion() != null) {
                int priority = rule.getSuggestion().getPriority();
                if (priority < highestPriorityValue) {
                    highestPriorityValue = priority;
                    highestPriority = rule.getSuggestion();
                }
            }
        }

        return highestPriority != null ? highestPriority : finalSuggestion;
    }

    /**
     * 计算平均置信度
     */
    public BigDecimal calculateAverageConfidence() {
        if (matchedRules == null || matchedRules.isEmpty()) {
            return finalConfidence != null ? finalConfidence : BigDecimal.ZERO;
        }

        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;

        for (RuleMatchResult rule : matchedRules) {
            if (rule.getConfidence() != null) {
                sum = sum.add(rule.getConfidence());
                count++;
            }
        }

        return count > 0 ? sum.divide(BigDecimal.valueOf(count), 4, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * 生成决策摘要
     */
    public String generateSummary() {
        return String.format("基金[%s] %s - 建议: %s (置信度: %.2f%%)",
                fundCode, fundName,
                finalSuggestion != null ? finalSuggestion.getDescription() : "无建议",
                finalConfidence != null ? finalConfidence.multiply(BigDecimal.valueOf(100)) : BigDecimal.ZERO);
    }

    /**
     * 基于持仓信息更新财务指标
     */
    public void updateFromHolding(FundHolding holding) {
        if (holding == null) {
            return;
        }

        this.holdingId = holding.getId();
        this.holdingCost = holding.getCostPrice();
        this.holdingQuantity = holding.getHoldShare();
        this.holdingValue = holding.getCurrentValue();
        this.holdingProfit = holding.getHoldProfit();
        this.holdingProfitRate = holding.getHoldProfitRate();
        this.currentYieldRate = holding.getHoldProfitRate();
        this.holdingDays = holding.getHoldDays();

        // 如果有实时数据，也更新其他指标
        if (holding.getCurrentPrice() != null) {
            this.currentPrice = holding.getCurrentPrice();
        }
    }

    /**
     * 检查建议是否有效（在有效期内）
     */
    public boolean isSuggestionValid() {
        if (suggestedExecutionTime == null || suggestionValidityHours == null) {
            return true; // 没有有效期限制
        }

        LocalDateTime expiryTime = suggestedExecutionTime.plusHours(suggestionValidityHours);
        return LocalDateTime.now().isBefore(expiryTime);
    }

    /**
     * 获取剩余有效时间（小时）
     */
    public Long getRemainingValidityHours() {
        if (suggestedExecutionTime == null || suggestionValidityHours == null) {
            return null;
        }

        LocalDateTime expiryTime = suggestedExecutionTime.plusHours(suggestionValidityHours);
        long remainingSeconds = java.time.Duration.between(LocalDateTime.now(), expiryTime).getSeconds();
        return remainingSeconds > 0 ? remainingSeconds / 3600 : 0L;
    }
}