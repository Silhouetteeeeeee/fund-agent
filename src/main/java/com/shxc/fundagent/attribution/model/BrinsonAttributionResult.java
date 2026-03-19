package com.shxc.fundagent.attribution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Brinson归因分析结果
 * 包含单期归因和多期归因结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BrinsonAttributionResult {

    /**
     * 归因分析ID
     */
    private String attributionId;

    /**
     * 分析日期
     */
    private LocalDate analysisDate;

    /**
     * 分析周期（开始日期）
     */
    private LocalDate startDate;

    /**
     * 分析周期（结束日期）
     */
    private LocalDate endDate;

    /**
     * 组合总收益
     */
    private BigDecimal portfolioReturn;

    /**
     * 基准总收益
     */
    private BigDecimal benchmarkReturn;

    /**
     * 超额收益（组合收益 - 基准收益）
     */
    private BigDecimal excessReturn;

    /**
     * 资产配置效应
     * 由于超配/低配某类资产带来的超额收益
     */
    private BigDecimal allocationEffect;

    /**
     * 证券选择效应
     * 由于选择优于基准的基金带来的超额收益
     */
    private BigDecimal selectionEffect;

    /**
     * 交互效应
     * 配置与选择的交互作用
     */
    private BigDecimal interactionEffect;

    /**
     * 总归因（应该等于超额收益）
     */
    private BigDecimal totalAttribution;

    /**
     * 归因残差（计算误差）
     */
    private BigDecimal residual;

    /**
     * 各资产类别的归因明细
     */
    private List<AssetClassAttribution> assetClassAttributions;

    /**
     * 各基金归因明细
     */
    private List<FundAttribution> fundAttributions;

    /**
     * 归因贡献度分析
     */
    private AttributionContribution contributionAnalysis;

    /**
     * 资产配置分析
     */
    private AllocationAnalysis allocationAnalysis;

    /**
     * 计算时间（毫秒）
     */
    private Long calculationTimeMs;

    /**
     * 获取资产配置效应占比
     */
    public BigDecimal getAllocationEffectRatio() {
        if (excessReturn == null || excessReturn.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return allocationEffect.divide(excessReturn.abs(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * 获取证券选择效应占比
     */
    public BigDecimal getSelectionEffectRatio() {
        if (excessReturn == null || excessReturn.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return selectionEffect.divide(excessReturn.abs(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * 获取交互效应占比
     */
    public BigDecimal getInteractionEffectRatio() {
        if (excessReturn == null || excessReturn.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return interactionEffect.divide(excessReturn.abs(), 4, BigDecimal.ROUND_HALF_UP)
                .multiply(new BigDecimal("100"));
    }

    /**
     * 验证归因平衡性（总归因应该约等于超额收益）
     */
    public boolean isBalanced() {
        if (totalAttribution == null || excessReturn == null) {
            return false;
        }
        BigDecimal diff = totalAttribution.subtract(excessReturn).abs();
        // 允许0.01%的误差
        return diff.compareTo(new BigDecimal("0.0001")) < 0;
    }

    /**
     * 获取归因分析摘要
     */
    public String getSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("Brinson归因分析摘要:\n");
        sb.append(String.format("分析周期: %s 至 %s\n", startDate, endDate));
        sb.append(String.format("组合收益: %.2f%%\n", portfolioReturn.multiply(new BigDecimal("100"))));
        sb.append(String.format("基准收益: %.2f%%\n", benchmarkReturn.multiply(new BigDecimal("100"))));
        sb.append(String.format("超额收益: %.2f%%\n", excessReturn.multiply(new BigDecimal("100"))));
        sb.append(String.format("资产配置效应: %.2f%% (%.1f%%)\n", 
                allocationEffect.multiply(new BigDecimal("100")), 
                getAllocationEffectRatio()));
        sb.append(String.format("证券选择效应: %.2f%% (%.1f%%)\n", 
                selectionEffect.multiply(new BigDecimal("100")), 
                getSelectionEffectRatio()));
        sb.append(String.format("交互效应: %.2f%% (%.1f%%)\n", 
                interactionEffect.multiply(new BigDecimal("100")), 
                getInteractionEffectRatio()));
        return sb.toString();
    }
}
