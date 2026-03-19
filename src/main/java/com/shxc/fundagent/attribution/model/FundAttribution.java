package com.shxc.fundagent.attribution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 单只基金归因明细
 * 记录每只基金在归因分析中的表现
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundAttribution {

    /**
     * 基金代码
     */
    private String fundCode;

    /**
     * 基金名称
     */
    private String fundName;

    /**
     * 所属资产类别
     */
    private AssetClass assetClass;

    /**
     * 基金类型
     */
    private String fundType;

    // ========== 权重与持仓 ==========

    /**
     * 组合中的权重
     */
    private BigDecimal portfolioWeight;

    /**
     * 持仓市值
     */
    private BigDecimal holdingValue;

    /**
     * 持仓成本
     */
    private BigDecimal holdingCost;

    // ========== 收益数据 ==========

    /**
     * 基金收益率（分析周期内）
     */
    private BigDecimal fundReturn;

    /**
     * 同类平均收益率
     */
    private BigDecimal categoryAverageReturn;

    /**
     * 同类排名
     */
    private Integer categoryRank;

    /**
     * 同类总数
     */
    private Integer categoryTotal;

    /**
     * 超额收益（相对同类平均）
     */
    private BigDecimal excessReturn;

    // ========== 归因贡献 ==========

    /**
     * 对组合总收益的贡献
     * Formula: 权重 * 基金收益
     */
    private BigDecimal contributionToPortfolio;

    /**
     * 对超额收益的贡献
     */
    private BigDecimal contributionToExcess;

    /**
     * 选择效应（该基金的选择能力）
     */
    private BigDecimal selectionEffect;

    // ========== 风险指标 ==========

    /**
     * 波动率
     */
    private BigDecimal volatility;

    /**
     * 最大回撤
     */
    private BigDecimal maxDrawdown;

    /**
     * 夏普比率
     */
    private BigDecimal sharpeRatio;

    // ========== 辅助方法 ==========

    /**
     * 计算对组合收益的贡献
     */
    public BigDecimal calculateContribution() {
        if (portfolioWeight == null || fundReturn == null) {
            return BigDecimal.ZERO;
        }
        return portfolioWeight.multiply(fundReturn);
    }

    /**
     * 计算超额收益
     */
    public BigDecimal calculateExcessReturn() {
        if (fundReturn == null || categoryAverageReturn == null) {
            return BigDecimal.ZERO;
        }
        return fundReturn.subtract(categoryAverageReturn);
    }

    /**
     * 获取同类排名百分比（越小越好）
     */
    public BigDecimal getRankPercentile() {
        if (categoryRank == null || categoryTotal == null || categoryTotal == 0) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(categoryRank)
                .divide(new BigDecimal(categoryTotal), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * 是否跑赢同类平均
     */
    public boolean beatCategory() {
        return excessReturn != null && excessReturn.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 是否排名前25%（优秀）
     */
    public boolean isTopQuartile() {
        if (categoryRank == null || categoryTotal == null) {
            return false;
        }
        return categoryRank <= categoryTotal / 4;
    }

    /**
     * 是否排名后25%（较差）
     */
    public boolean isBottomQuartile() {
        if (categoryRank == null || categoryTotal == null) {
            return false;
        }
        return categoryRank >= categoryTotal * 3 / 4;
    }

    /**
     * 获取表现评级
     */
    public String getPerformanceRating() {
        if (isTopQuartile()) {
            return "优秀";
        } else if (categoryRank != null && categoryTotal != null && categoryRank <= categoryTotal / 2) {
            return "良好";
        } else if (!isBottomQuartile()) {
            return "一般";
        } else {
            return "较差";
        }
    }

    /**
     * 获取基金归因摘要
     */
    public String getAttributionSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s (%s): ", fundName, fundCode));
        sb.append(String.format("收益 %.2f%%, ", fundReturn.multiply(new BigDecimal("100"))));
        sb.append(String.format("权重 %.1f%%, ", portfolioWeight.multiply(new BigDecimal("100"))));
        sb.append(String.format("贡献 %.2f%%", contributionToPortfolio.multiply(new BigDecimal("100"))));

        if (categoryRank != null && categoryTotal != null) {
            sb.append(String.format(", 同类排名 %d/%d", categoryRank, categoryTotal));
        }

        return sb.toString();
    }
}
