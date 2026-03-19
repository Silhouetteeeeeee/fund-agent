package com.shxc.fundagent.attribution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 归因贡献度分析
 * 分析各类资产和基金对收益的贡献程度
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributionContribution {

    // ========== 资产配置贡献 ==========

    /**
     * 权益类资产配置贡献
     */
    private BigDecimal equityAllocationContribution;

    /**
     * 固收类资产配置贡献
     */
    private BigDecimal fixedIncomeAllocationContribution;

    /**
     * 其他资产配置贡献
     */
    private BigDecimal otherAllocationContribution;

    // ========== 证券选择贡献 ==========

    /**
     * 权益类证券选择贡献
     */
    private BigDecimal equitySelectionContribution;

    /**
     * 固收类证券选择贡献
     */
    private BigDecimal fixedIncomeSelectionContribution;

    /**
     * 其他证券选择贡献
     */
    private BigDecimal otherSelectionContribution;

    // ========== 正向贡献分析 ==========

    /**
     * 最大正向贡献资产类别
     */
    private AssetClass topPositiveAssetClass;

    /**
     * 最大正向贡献值
     */
    private BigDecimal topPositiveContribution;

    /**
     * 最大正向贡献基金
     */
    private String topPositiveFund;

    /**
     * 最大正向贡献基金代码
     */
    private String topPositiveFundCode;

    // ========== 负向贡献分析 ==========

    /**
     * 最大负向贡献资产类别
     */
    private AssetClass topNegativeAssetClass;

    /**
     * 最大负向贡献值
     */
    private BigDecimal topNegativeContribution;

    /**
     * 最大负向贡献基金
     */
    private String topNegativeFund;

    /**
     * 最大负向贡献基金代码
     */
    private String topNegativeFundCode;

    // ========== 贡献分布 ==========

    /**
     * 各资产类别贡献分布
     */
    private Map<AssetClass, BigDecimal> assetClassContributions;

    /**
     * 各基金贡献分布（Top 10）
     */
    private Map<String, BigDecimal> topFundContributions;

    // ========== 辅助方法 ==========

    /**
     * 获取资产配置总贡献
     */
    public BigDecimal getTotalAllocationContribution() {
        BigDecimal total = BigDecimal.ZERO;
        if (equityAllocationContribution != null) {
            total = total.add(equityAllocationContribution);
        }
        if (fixedIncomeAllocationContribution != null) {
            total = total.add(fixedIncomeAllocationContribution);
        }
        if (otherAllocationContribution != null) {
            total = total.add(otherAllocationContribution);
        }
        return total;
    }

    /**
     * 获取证券选择总贡献
     */
    public BigDecimal getTotalSelectionContribution() {
        BigDecimal total = BigDecimal.ZERO;
        if (equitySelectionContribution != null) {
            total = total.add(equitySelectionContribution);
        }
        if (fixedIncomeSelectionContribution != null) {
            total = total.add(fixedIncomeSelectionContribution);
        }
        if (otherSelectionContribution != null) {
            total = total.add(otherSelectionContribution);
        }
        return total;
    }

    /**
     * 获取贡献分析摘要
     */
    public String getContributionSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("归因贡献分析:\n");
        sb.append(String.format("资产配置总贡献: %.2f%%\n", 
                getTotalAllocationContribution().multiply(new BigDecimal("100"))));
        sb.append(String.format("证券选择总贡献: %.2f%%\n", 
                getTotalSelectionContribution().multiply(new BigDecimal("100"))));

        if (topPositiveAssetClass != null) {
            sb.append(String.format("最大正向贡献资产: %s (%.2f%%)\n", 
                    topPositiveAssetClass.getName(),
                    topPositiveContribution.multiply(new BigDecimal("100"))));
        }

        if (topNegativeAssetClass != null) {
            sb.append(String.format("最大负向贡献资产: %s (%.2f%%)\n", 
                    topNegativeAssetClass.getName(),
                    topNegativeContribution.multiply(new BigDecimal("100"))));
        }

        return sb.toString();
    }
}
