package com.shxc.fundagent.strategy.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 基金持仓分析上下文数据
 * 用于封装单只基金的完整信息，传递给 Agent 或策略引擎
 */
@Data
@Builder
public class FundPositionContext {
    
    /**
     * 基金代码
     */
    private String fundCode;
    
    /**
     * 基金名称
     */
    private String fundName;

    private String fundType;

    private String managerInfo;
    
    /**
     * 当前净值
     */
    private BigDecimal netValue;
    
    /**
     * 日涨跌幅 (%)
     */
    private BigDecimal dailyChangePercent;
    
    /**
     * 周涨跌幅 (%)
     */
    private BigDecimal weeklyChangePercent;
    
    /**
     * 月涨跌幅 (%)
     */
    private BigDecimal monthlyChangePercent;

    private BigDecimal yearlyChangePercent;
    
    /**
     * 风险等级 (1-5)
     */
    private Integer riskLevel;
    
    // ==================== 持仓信息 ====================
    
    /**
     * 持仓份额
     */
    private BigDecimal holdShares;
    
    /**
     * 持仓市值
     */
    private BigDecimal holdAmount;
    
    /**
     * 平均成本价
     */
    private BigDecimal avgCost;
    
    /**
     * 持仓成本总额
     */
    private BigDecimal costAmount;
    
    /**
     * 持仓收益 (金额)
     */
    private BigDecimal profit;
    
    /**
     * 持仓收益率 (%)
     */
    private BigDecimal profitRate;

    /**
     * 仓位占比 (0-1)
     */
    private BigDecimal position;
    
    /**
     * 持仓天数
     */
    private Integer holdDays;
}
