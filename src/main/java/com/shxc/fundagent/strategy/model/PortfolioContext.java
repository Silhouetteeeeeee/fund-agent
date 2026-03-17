package com.shxc.fundagent.strategy.model;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 投资组合分析上下文
 * 用于封装整个投资组合的完整信息，传递给 Agent 或策略引擎
 */
@Data
@Builder
public class PortfolioContext {
    
    /**
     * 持仓基金列表
     */
    private List<FundPositionContext> funds;
    
    /**
     * 总资产 (市值)
     */
    private BigDecimal totalAssets;
    
    /**
     * 总成本
     */
    private BigDecimal totalCost;
    
    /**
     * 总收益 (金额)
     */
    private BigDecimal totalProfit;
    
    /**
     * 总收益率 (%)
     */
    private BigDecimal totalProfitRate;
    
    /**
     * 可用现金
     */
    private BigDecimal availableCash;
    
    /**
     * 目标仓位 (0-1)
     */
    private BigDecimal targetPosition;
    
    /**
     * 当前仓位 (0-1)
     */
    private BigDecimal currentPosition;
    
    /**
     * 可投资金额
     */
    public BigDecimal getAvailableInvestmentAmount() {
        if (totalAssets == null || targetPosition == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal targetValue = totalAssets.divide(targetPosition, 4, RoundingMode.HALF_UP);
        BigDecimal currentTotal = totalAssets.add(availableCash != null ? availableCash : BigDecimal.ZERO);
        return targetValue.subtract(currentTotal).max(BigDecimal.ZERO);
    }
}
