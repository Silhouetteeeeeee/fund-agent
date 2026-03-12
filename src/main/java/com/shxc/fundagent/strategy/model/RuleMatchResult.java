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
 * 规则匹配结果类
 * 表示单个规则与基金数据的匹配结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RuleMatchResult {

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
     * 是否匹配成功
     */
    private boolean matched;

    /**
     * 建议类型（如果匹配成功）
     */
    private SuggestionType suggestion;

    /**
     * 置信度（0-1）
     */
    private BigDecimal confidence;

    /**
     * 匹配时间
     */
    private LocalDateTime matchTime;

    /**
     * 计算耗时（毫秒）
     */
    private Long calculationTimeMs;

    // ================ 条件检查详情 ================

    /**
     * 条件检查结果详情
     * key: 条件名称, value: 是否满足
     */
    private Map<String, Boolean> conditionResults;

    /**
     * 条件值详情
     * key: 条件名称, value: 实际值
     */
    private Map<String, Object> conditionValues;

    /**
     * 阈值详情
     * key: 条件名称, value: 阈值
     */
    private Map<String, Object> thresholdValues;

    /**
     * 是否所有条件都满足
     */
    private Boolean allConditionsMet;

    // ================ 触发详情 ================

    /**
     * 触发级别（1-5，1为轻微，5为严重）
     */
    private Integer triggerLevel;

    /**
     * 触发描述
     */
    private String triggerDescription;

    /**
     * 触发值（实际触发条件的数值）
     */
    private BigDecimal triggerValue;

    /**
     * 触发阈值
     */
    private BigDecimal triggerThreshold;

    /**
     * 触发偏差（实际值-阈值）
     */
    private BigDecimal triggerDeviation;

    /**
     * 触发偏差百分比（%）
     */
    private BigDecimal triggerDeviationPercentage;

    // ================ 权重和优先级 ================

    /**
     * 规则权重（0-1）
     */
    private BigDecimal ruleWeight;

    /**
     * 规则优先级
     */
    private Integer rulePriority;

    /**
     * 匹配得分（0-100）
     */
    private Integer matchScore;

    // ================ 附加信息 ================

    /**
     * 规则描述
     */
    private String ruleDescription;

    /**
     * 规则配置摘要
     */
    private String ruleConfigSummary;

    /**
     * 错误信息（如果匹配失败）
     */
    private String errorMessage;

    /**
     * 异常堆栈（如果发生异常）
     */
    private String exceptionStackTrace;

    /**
     * 是否来自缓存
     */
    private boolean fromCache;

    /**
     * 缓存键
     */
    private String cacheKey;

    /**
     * 备注
     */
    private String remarks;

    // ================ 辅助方法 ================

    /**
     * 初始化条件检查结果
     */
    public void initConditionResults() {
        if (this.conditionResults == null) {
            this.conditionResults = new HashMap<>();
        }
        if (this.conditionValues == null) {
            this.conditionValues = new HashMap<>();
        }
        if (this.thresholdValues == null) {
            this.thresholdValues = new HashMap<>();
        }
    }

    /**
     * 添加条件检查结果
     */
    public void addConditionResult(String conditionName, boolean met, Object actualValue, Object thresholdValue) {
        initConditionResults();
        this.conditionResults.put(conditionName, met);
        this.conditionValues.put(conditionName, actualValue);
        this.thresholdValues.put(conditionName, thresholdValue);
        updateAllConditionsMet();
    }

    /**
     * 更新所有条件是否满足的标志
     */
    private void updateAllConditionsMet() {
        if (conditionResults == null || conditionResults.isEmpty()) {
            this.allConditionsMet = null;
            return;
        }

        for (boolean met : conditionResults.values()) {
            if (!met) {
                this.allConditionsMet = false;
                return;
            }
        }
        this.allConditionsMet = true;
    }

    /**
     * 检查是否所有条件都满足
     */
    public boolean isAllConditionsMet() {
        return allConditionsMet != null && allConditionsMet;
    }

    /**
     * 计算匹配得分（基于满足的条件数量和触发级别）
     */
    public int calculateMatchScore() {
        if (!matched) {
            return 0;
        }

        int score = 0;

        // 基础得分：匹配成功
        score += 30;

        // 条件满足数量加分
        if (conditionResults != null) {
            int totalConditions = conditionResults.size();
            int metConditions = 0;
            for (boolean met : conditionResults.values()) {
                if (met) metConditions++;
            }
            if (totalConditions > 0) {
                score += (int) ((double) metConditions / totalConditions * 40);
            }
        }

        // 触发级别加分
        if (triggerLevel != null) {
            score += triggerLevel * 6; // 1-5级，每级加6分
        }

        // 置信度加分
        if (confidence != null) {
            score += (int) (confidence.doubleValue() * 20);
        }

        return Math.min(score, 100);
    }

    /**
     * 生成规则匹配摘要
     */
    public String generateMatchSummary() {
        if (!matched) {
            return String.format("规则[%s]未匹配: %s", ruleName, errorMessage != null ? errorMessage : "条件不满足");
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("规则[%s]已匹配 - 建议: %s", ruleName,
                suggestion != null ? suggestion.getDescription() : "无建议"));

        if (confidence != null) {
            summary.append(String.format(" (置信度: %.2f%%)", confidence.multiply(BigDecimal.valueOf(100))));
        }

        if (triggerDescription != null) {
            summary.append(String.format(" - 触发: %s", triggerDescription));
        }

        return summary.toString();
    }

    /**
     * 生成详细的条件检查报告
     */
    public String generateConditionReport() {
        if (conditionResults == null || conditionResults.isEmpty()) {
            return "无条件检查记录";
        }

        StringBuilder report = new StringBuilder();
        report.append("条件检查结果:\n");

        for (Map.Entry<String, Boolean> entry : conditionResults.entrySet()) {
            String condition = entry.getKey();
            boolean met = entry.getValue();
            Object actualValue = conditionValues.get(condition);
            Object thresholdValue = thresholdValues.get(condition);

            report.append(String.format("  %s: %s", condition, met ? "✓" : "✗"));

            if (actualValue != null) {
                report.append(String.format(" (实际值: %s", formatValue(actualValue)));
                if (thresholdValue != null) {
                    report.append(String.format(", 阈值: %s", formatValue(thresholdValue)));
                }
                report.append(")");
            }

            report.append("\n");
        }

        return report.toString();
    }

    /**
     * 格式化值显示
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }

        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).toPlainString();
        }

        return value.toString();
    }

    /**
     * 计算触发偏差
     */
    public void calculateTriggerDeviation() {
        if (triggerValue == null || triggerThreshold == null) {
            return;
        }

        this.triggerDeviation = triggerValue.subtract(triggerThreshold);
        if (triggerThreshold.compareTo(BigDecimal.ZERO) != 0) {
            this.triggerDeviationPercentage = triggerDeviation
                    .divide(triggerThreshold.abs(), 4, BigDecimal.ROUND_HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
    }

    /**
     * 检查是否为紧急触发
     */
    public boolean isUrgentTrigger() {
        if (triggerLevel == null) {
            return false;
        }

        // 触发级别4-5为紧急
        return triggerLevel >= 4;
    }

    /**
     * 获取建议执行优先级（数字越小优先级越高）
     */
    public int getExecutionPriority() {
        int priority = 999; // 默认低优先级

        if (rulePriority != null) {
            priority = rulePriority;
        }

        // 紧急触发提高优先级
        if (isUrgentTrigger()) {
            priority -= 100;
        }

        // 高置信度提高优先级
        if (confidence != null && confidence.compareTo(new BigDecimal("0.8")) > 0) {
            priority -= 50;
        }

        return Math.max(1, priority); // 确保至少为1
    }
}