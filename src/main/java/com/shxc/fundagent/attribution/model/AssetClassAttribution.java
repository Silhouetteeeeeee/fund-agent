package com.shxc.fundagent.attribution.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 资产类别归因明细
 * 记录每个资产类别的配置、收益和归因效应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetClassAttribution {

    /**
     * 资产类别
     */
    private AssetClass assetClass;

    /**
     * 资产类别名称
     */
    private String assetClassName;

    // ========== 权重配置 ==========

    /**
     * 组合权重（实际配置比例）
     */
    private BigDecimal portfolioWeight;

    /**
     * 基准权重（基准配置比例）
     */
    private BigDecimal benchmarkWeight;

    /**
     * 超配/低配比例（组合权重 - 基准权重）
     */
    private BigDecimal weightDifference;

    // ========== 收益数据 ==========

    /**
     * 组合中该类资产的收益率
     */
    private BigDecimal portfolioReturn;

    /**
     * 基准中该类资产的收益率
     */
    private BigDecimal benchmarkReturn;

    /**
     * 超额收益（组合收益 - 基准收益）
     */
    private BigDecimal excessReturn;

    // ========== Brinson归因三效应 ==========

    /**
     * 资产配置效应
     * Formula: (Wp - Wb) * Rb
     * Wp = 组合权重, Wb = 基准权重, Rb = 基准收益
     */
    private BigDecimal allocationEffect;

    /**
     * 证券选择效应
     * Formula: Wb * (Rp - Rb)
     * Rp = 组合收益
     */
    private BigDecimal selectionEffect;

    /**
     * 交互效应
     * Formula: (Wp - Wb) * (Rp - Rb)
     */
    private BigDecimal interactionEffect;

    /**
     * 总效应（三类效应之和）
     */
    private BigDecimal totalEffect;

    // ========== 贡献度分析 ==========

    /**
     * 对组合总收益的贡献
     */
    private BigDecimal contributionToPortfolio;

    /**
     * 对基准总收益的贡献
     */
    private BigDecimal contributionToBenchmark;

    /**
     * 对超额收益的贡献
     */
    private BigDecimal contributionToExcess;

    // ========== 辅助方法 ==========

    /**
     * 计算权重差异
     */
    public BigDecimal calculateWeightDifference() {
        if (portfolioWeight == null || benchmarkWeight == null) {
            return BigDecimal.ZERO;
        }
        return portfolioWeight.subtract(benchmarkWeight);
    }

    /**
     * 计算超额收益
     */
    public BigDecimal calculateExcessReturn() {
        if (portfolioReturn == null || benchmarkReturn == null) {
            return BigDecimal.ZERO;
        }
        return portfolioReturn.subtract(benchmarkReturn);
    }

    /**
     * 是否是超配
     */
    public boolean isOverweight() {
        return weightDifference != null && weightDifference.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 是否是低配
     */
    public boolean isUnderweight() {
        return weightDifference != null && weightDifference.compareTo(BigDecimal.ZERO) < 0;
    }

    /**
     * 选择效应是否为正（说明选基能力强）
     */
    public boolean hasPositiveSelection() {
        return selectionEffect != null && selectionEffect.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 获取配置方向描述
     */
    public String getAllocationDirection() {
        if (weightDifference == null) {
            return "中性配置";
        }
        BigDecimal absDiff = weightDifference.abs();
        if (absDiff.compareTo(new BigDecimal("0.05")) < 0) {
            return "中性配置";
        } else if (weightDifference.compareTo(BigDecimal.ZERO) > 0) {
            return String.format("超配 %.1f%%", weightDifference.multiply(new BigDecimal("100")));
        } else {
            return String.format("低配 %.1f%%", weightDifference.abs().multiply(new BigDecimal("100")));
        }
    }

    /**
     * 获取效应分析摘要
     */
    public String getEffectSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s: ", assetClassName));
        sb.append(String.format("配置%s, ", getAllocationDirection()));
        
        if (allocationEffect != null && allocationEffect.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("配置贡献+%.2f%%, ", allocationEffect.multiply(new BigDecimal("100"))));
        } else if (allocationEffect != null) {
            sb.append(String.format("配置贡献%.2f%%, ", allocationEffect.multiply(new BigDecimal("100"))));
        }
        
        if (selectionEffect != null && selectionEffect.compareTo(BigDecimal.ZERO) > 0) {
            sb.append(String.format("选基贡献+%.2f%%", selectionEffect.multiply(new BigDecimal("100"))));
        } else if (selectionEffect != null) {
            sb.append(String.format("选基贡献%.2f%%", selectionEffect.multiply(new BigDecimal("100"))));
        }
        
        return sb.toString();
    }
}
