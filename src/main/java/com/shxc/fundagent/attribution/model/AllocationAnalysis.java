package com.shxc.fundagent.attribution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 资产配置分析
 * 分析当前组合的配置结构与基准的偏离
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationAnalysis {

    // ========== 组合配置结构 ==========

    /**
     * 组合权益类资产占比
     */
    private BigDecimal portfolioEquityRatio;

    /**
     * 组合固收类资产占比
     */
    private BigDecimal portfolioFixedIncomeRatio;

    /**
     * 组合其他资产占比
     */
    private BigDecimal portfolioOtherRatio;

    // ========== 基准配置结构 ==========

    /**
     * 基准权益类资产占比
     */
    private BigDecimal benchmarkEquityRatio;

    /**
     * 基准固收类资产占比
     */
    private BigDecimal benchmarkFixedIncomeRatio;

    /**
     * 基准其他资产占比
     */
    private BigDecimal benchmarkOtherRatio;

    // ========== 配置偏离 ==========

    /**
     * 权益类偏离（组合 - 基准）
     */
    private BigDecimal equityDeviation;

    /**
     * 固收类偏离（组合 - 基准）
     */
    private BigDecimal fixedIncomeDeviation;

    /**
     * 其他类偏离（组合 - 基准）
     */
    private BigDecimal otherDeviation;

    // ========== 配置集中度 ==========

    /**
     * 前三大资产类别集中度
     */
    private BigDecimal top3Concentration;

    /**
     * 最大单一资产类别占比
     */
    private BigDecimal maxSingleAssetRatio;

    /**
     * 分散化评分（0-100）
     */
    private BigDecimal diversificationScore;

    // ========== 配置建议 ==========

    /**
     * 是否过度集中
     */
    private boolean isOverConcentrated;

    /**
     * 配置风险警告
     */
    private String allocationWarning;

    /**
     * 再平衡建议
     */
    private String rebalanceSuggestion;

    // ========== 详细配置数据 ==========

    /**
     * 各资产类别配置详情
     */
    private Map<AssetClass, AssetAllocationDetail> assetAllocationDetails;

    // ========== 内部类：资产类别配置详情 ==========

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetAllocationDetail {
        /**
         * 资产类别
         */
        private AssetClass assetClass;

        /**
         * 组合权重
         */
        private BigDecimal portfolioWeight;

        /**
         * 基准权重
         */
        private BigDecimal benchmarkWeight;

        /**
         * 偏离度
         */
        private BigDecimal deviation;

        /**
         * 偏离状态：OVERWEIGHT, UNDERWEIGHT, NEUTRAL
         */
        private String deviationStatus;

        /**
         * 建议调整方向
         */
        private String adjustmentSuggestion;

        /**
         * 建议调整幅度
         */
        private BigDecimal suggestedAdjustment;
    }

    // ========== 辅助方法 ==========

    /**
     * 计算权益类偏离
     */
    public BigDecimal calculateEquityDeviation() {
        if (portfolioEquityRatio == null || benchmarkEquityRatio == null) {
            return BigDecimal.ZERO;
        }
        return portfolioEquityRatio.subtract(benchmarkEquityRatio);
    }

    /**
     * 是否超配权益
     */
    public boolean isOverweightEquity() {
        return equityDeviation != null && equityDeviation.compareTo(new BigDecimal("0.05")) > 0;
    }

    /**
     * 是否低配权益
     */
    public boolean isUnderweightEquity() {
        return equityDeviation != null && equityDeviation.compareTo(new BigDecimal("-0.05")) < 0;
    }

    /**
     * 获取配置风格描述
     */
    public String getAllocationStyle() {
        if (isOverweightEquity()) {
            return "进取型（超配权益）";
        } else if (isUnderweightEquity()) {
            return "保守型（低配权益）";
        } else {
            return "平衡型（中性配置）";
        }
    }

    /**
     * 获取配置分析摘要
     */
    public String getAllocationSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("资产配置分析:\n");
        sb.append(String.format("组合权益占比: %.1f%%\n", portfolioEquityRatio.multiply(new BigDecimal("100"))));
        sb.append(String.format("基准权益占比: %.1f%%\n", benchmarkEquityRatio.multiply(new BigDecimal("100"))));
        sb.append(String.format("权益偏离: %+.1f%% (%s)\n", 
                equityDeviation.multiply(new BigDecimal("100")),
                getAllocationStyle()));
        sb.append(String.format("分散化评分: %.1f/100\n", diversificationScore));

        if (isOverConcentrated) {
            sb.append("⚠️ ").append(allocationWarning).append("\n");
        }

        if (rebalanceSuggestion != null) {
            sb.append("💡 ").append(rebalanceSuggestion).append("\n");
        }

        return sb.toString();
    }
}
