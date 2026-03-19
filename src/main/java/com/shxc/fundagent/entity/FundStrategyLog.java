package com.shxc.fundagent.entity;

import com.shxc.fundagent.enums.SuggestionType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 策略日志实体
 * 对应详细设计文档中的 fund_strategy_log 表
 */
@Entity
@Table(name = "fund_strategy_log")
@Data
public class FundStrategyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 基金代码
     */
    @Column(name = "fund_code", nullable = false, length = 10)
    private String fundCode;

    /**
     * 交易日
     */
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    /**
     * 当日收益率（%）
     */
    @Column(name = "yield_rate", nullable = false)
    private BigDecimal yieldRate;

    /**
     * 日涨跌幅（%）
     */
    @Column(name = "daily_change")
    private BigDecimal dailyChange;

    /**
     * 周涨跌幅（%）
     */
    @Column(name = "weekly_change")
    private BigDecimal weeklyChange;

    /**
     * 月涨跌幅（%）
     */
    @Column(name = "monthly_change")
    private BigDecimal monthlyChange;

    /**
     * 当前价格
     */
    @Column(name = "current_price")
    private BigDecimal currentPrice;

    /**
     * 持仓成本
     */
    @Column(name = "cost_price")
    private BigDecimal costPrice;

    /**
     * 操作建议
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "suggestion", nullable = false, length = 20)
    private SuggestionType suggestion;

    /**
     * 建议原因
     */
    @Column(name = "suggestion_reason", length = 200)
    private String suggestionReason;

    /**
     * 触发规则名称
     */
    @Column(name = "triggered_rule", length = 50)
    private String triggeredRule;

    /**
     * 规则优先级
     */
    @Column(name = "rule_priority")
    private Integer rulePriority;

    /**
     * 决策置信度（0-1）
     */
    @Column(name = "confidence")
    private BigDecimal confidence;

    /**
     * 是否已通知
     */
    @Column(name = "is_notified")
    private Boolean isNotified = false;

    /**
     * 通知时间
     */
    @Column(name = "notify_time")
    private LocalDateTime notifyTime;

    /**
     * 是否已执行
     */
    @Column(name = "is_executed")
    private Boolean isExecuted = false;

    /**
     * 执行时间
     */
    @Column(name = "execute_time")
    private LocalDateTime executeTime;

    /**
     * 执行结果
     */
    @Column(name = "execute_result", length = 100)
    private String executeResult;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 无参构造函数
     */
    public FundStrategyLog() {
    }

    /**
     * 带基本参数的构造函数
     */
    public FundStrategyLog(String fundCode, LocalDate tradeDate, BigDecimal yieldRate, SuggestionType suggestion) {
        this.fundCode = fundCode;
        this.tradeDate = tradeDate;
        this.yieldRate = yieldRate;
        this.suggestion = suggestion;
    }

    /**
     * 获取建议类型显示名称
     */
    public String getSuggestionDisplayName() {
        return suggestion != null ? suggestion.getDisplayName() : "未知";
    }

    /**
     * 获取建议类型描述
     */
    public String getSuggestionDescription() {
        return suggestion != null ? suggestion.getDescription() : "";
    }

    /**
     * 判断是否为紧急建议（需要立即通知）
     */
    public boolean isUrgent() {
        return suggestion == SuggestionType.RISK_ALERT ||
               suggestion == SuggestionType.CLEAR ||
               (dailyChange != null && dailyChange.compareTo(BigDecimal.valueOf(-4)) < 0);
    }

    /**
     * 获取建议优先级（用于排序）
     */
    public int getSuggestionPriority() {
        if (suggestion == null) {
            return 0;
        }
        switch (suggestion) {
            case RISK_ALERT: return 1;
            case CLEAR: return 2;
            case SELL: return 3;
            case BUY: return 4;
            case HOLD: return 5;
            default: return 6;
        }
    }

    /**
     * 标记为已通知
     */
    public void markAsNotified() {
        this.isNotified = true;
        this.notifyTime = LocalDateTime.now();
    }

    /**
     * 标记为已执行
     */
    public void markAsExecuted(String result) {
        this.isExecuted = true;
        this.executeTime = LocalDateTime.now();
        this.executeResult = result;
    }

    // ================ 兼容性方法（供StrategyDecisionEngineImpl使用） ================

    /**
     * 设置基金名称（兼容性方法）
     * 注意：FundStrategyLog实体没有基金名字段，此方法什么也不做
     */
    public void setFundName(String fundName) {
        // 此实体没有基金名字段，无法存储
    }

    /**
     * 设置建议类型（兼容性方法）
     */
    public void setSuggestionType(SuggestionType suggestionType) {
        this.suggestion = suggestionType;
    }

    /**
     * 设置决策时间（兼容性方法）
     */
    public void setDecisionTime(LocalDateTime decisionTime) {
        // 此实体没有决策时间字段，使用createTime代替
        // createTime由@CreationTimestamp自动设置，无法手动设置
    }

    /**
     * 设置触发规则数量（兼容性方法）
     * 注意：FundStrategyLog实体没有此字段，此方法什么也不做
     */
    public void setTriggeredRuleCount(Integer count) {
        // 此实体没有触发规则数量字段
    }

    /**
     * 设置计算时间（兼容性方法）
     * 注意：FundStrategyLog实体没有此字段，此方法什么也不做
     */
    public void setCalculationTimeMs(Long ms) {
        // 此实体没有计算时间字段
    }

    /**
     * 设置风险等级（兼容性方法）
     * 注意：FundStrategyLog实体没有此字段，此方法什么也不做
     */
    public void setRiskLevel(Integer riskLevel) {
        // 此实体没有风险等级字段
    }

    /**
     * 设置风险分数（兼容性方法）
     * 注意：FundStrategyLog实体没有此字段，此方法什么也不做
     */
    public void setRiskScore(Integer riskScore) {
        // 此实体没有风险分数字段
    }

    /**
     * 设置创建时间（兼容性方法）
     */
    public void setCreatedTime(LocalDateTime createdTime) {
        // createTime由@CreationTimestamp自动设置，无法手动设置
    }

    /**
     * 设置日涨跌幅（兼容性方法）
     */
    public void setDailyChange(BigDecimal dailyChange) {
        this.dailyChange = dailyChange;
    }
}