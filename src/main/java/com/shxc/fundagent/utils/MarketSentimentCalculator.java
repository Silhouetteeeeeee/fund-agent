package com.shxc.fundagent.utils;

import com.shxc.fundagent.agent.model.v2.MarketContext;
import com.shxc.fundagent.service.MarketDataService;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

/**
 * 市场情绪计算器
 * 提供科学、客观的市场情绪计算方法
 * 
 * 情绪评分模型基于多维度指标：
 * 1. 涨跌幅指标（30%）- 市场涨跌情况
 * 2. 上涨家数比例（20%）- 市场广度
 * 3. 成交量变化（20%）- 市场参与度
 * 4. 波动率指标（15%）- 市场稳定性
 * 5. 北向资金流向（15%）- 外资情绪
 */
@Slf4j
public class MarketSentimentCalculator {

    // 权重配置
    private static final BigDecimal WEIGHT_PRICE_CHANGE = new BigDecimal("0.30");
    private static final BigDecimal WEIGHT_RISING_RATIO = new BigDecimal("0.20");
    private static final BigDecimal WEIGHT_VOLUME = new BigDecimal("0.20");
    private static final BigDecimal WEIGHT_VOLATILITY = new BigDecimal("0.15");
    private static final BigDecimal WEIGHT_NORTHBOUND = new BigDecimal("0.15");

    // 指数权重（市值加权）
    private static final BigDecimal WEIGHT_SH_INDEX = new BigDecimal("0.40");      // 上证指数
    private static final BigDecimal WEIGHT_SZ_INDEX = new BigDecimal("0.30");      // 深证成指
    private static final BigDecimal WEIGHT_CY_INDEX = new BigDecimal("0.20");      // 创业板指
    private static final BigDecimal WEIGHT_HS300 = new BigDecimal("0.10");         // 沪深300

    /**
     * 计算市场情绪
     * 
     * @param indices 主要指数数据列表
     * @param northboundFlow 北向资金流向（可选）
     * @return 市场情绪数据
     */
    public static MarketSentimentResult calculateSentiment(
            List<MarketContext.IndexData> indices,
            MarketDataService.NorthboundFlowData northboundFlow) {

        if (indices == null || indices.isEmpty()) {
            log.warn("指数数据为空，无法计算市场情绪");
            return createNeutralSentiment();
        }

        try {
            // 1. 计算加权涨跌幅得分（30%）
            BigDecimal priceChangeScore = calculatePriceChangeScore(indices);

            // 2. 计算上涨家数比例得分（20%）
            BigDecimal risingRatioScore = calculateRisingRatioScore(indices);

            // 3. 计算成交量变化得分（20%）
            BigDecimal volumeScore = calculateVolumeScore(indices);

            // 4. 计算波动率得分（15%）
            BigDecimal volatilityScore = calculateVolatilityScore(indices);

            // 5. 计算北向资金得分（15%）
            BigDecimal northboundScore = calculateNorthboundScore(northboundFlow);

            // 综合得分（0-100）
            BigDecimal totalScore = priceChangeScore.multiply(WEIGHT_PRICE_CHANGE)
                    .add(risingRatioScore.multiply(WEIGHT_RISING_RATIO))
                    .add(volumeScore.multiply(WEIGHT_VOLUME))
                    .add(volatilityScore.multiply(WEIGHT_VOLATILITY))
                    .add(northboundScore.multiply(WEIGHT_NORTHBOUND));

            // 确保分数在0-100范围内
            totalScore = totalScore.max(BigDecimal.ZERO).min(new BigDecimal("100"));

            // 计算恐惧贪婪指数（与情绪分数相同，但解释不同）
            BigDecimal fearGreedIndex = totalScore;

            // 计算交易热情（基于成交量）
            BigDecimal tradingEnthusiasm = volumeScore;

            // 计算波动率预期
            BigDecimal volatilityExpectation = new BigDecimal("100").subtract(volatilityScore);

            return MarketSentimentResult.builder()
                    .sentimentScore(totalScore)
                    .sentimentLevel(convertToLevel(totalScore))
                    .fearGreedIndex(fearGreedIndex)
                    .tradingEnthusiasm(tradingEnthusiasm)
                    .volatilityExpectation(volatilityExpectation)
                    .priceChangeScore(priceChangeScore)
                    .risingRatioScore(risingRatioScore)
                    .volumeScore(volumeScore)
                    .volatilityScore(volatilityScore)
                    .northboundScore(northboundScore)
                    .build();

        } catch (Exception e) {
            log.error("计算市场情绪失败", e);
            return createNeutralSentiment();
        }
    }

    /**
     * 计算加权涨跌幅得分
     * 基于主要指数的加权平均涨跌幅，转换为0-100分
     */
    private static BigDecimal calculatePriceChangeScore(List<MarketContext.IndexData> indices) {
        BigDecimal weightedChange = BigDecimal.ZERO;
        BigDecimal totalWeight = BigDecimal.ZERO;

        for (MarketContext.IndexData index : indices) {
            if (index.getChangePercent() == null) continue;

            BigDecimal weight = getIndexWeight(index.getIndexCode());
            weightedChange = weightedChange.add(index.getChangePercent().multiply(weight));
            totalWeight = totalWeight.add(weight);
        }

        if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
            return new BigDecimal("50"); // 中性
        }

        // 加权平均涨跌幅
        BigDecimal avgChange = weightedChange.divide(totalWeight, 4, RoundingMode.HALF_UP);

        // 转换为得分：涨跌幅范围假设为-5%到+5%，映射到0-100分
        // -5% -> 0分, 0% -> 50分, +5% -> 100分
        BigDecimal score = avgChange.multiply(new BigDecimal("1000")) // 放大10倍
                .add(new BigDecimal("50"));

        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * 计算上涨家数比例得分
     * 基于上涨指数占比
     */
    private static BigDecimal calculateRisingRatioScore(List<MarketContext.IndexData> indices) {
        if (indices.isEmpty()) {
            return new BigDecimal("50");
        }

        long risingCount = indices.stream()
                .filter(i -> i.getChangePercent() != null && i.getChangePercent().compareTo(BigDecimal.ZERO) > 0)
                .count();

        // 上涨比例直接映射到0-100分
        return new BigDecimal(risingCount)
                .multiply(new BigDecimal("100"))
                .divide(new BigDecimal(indices.size()), 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算成交量变化得分
     * 基于成交量相对近期平均的变化
     */
    private static BigDecimal calculateVolumeScore(List<MarketContext.IndexData> indices) {
        // 简化处理：假设成交量越大，市场参与度越高
        // 实际应该与历史平均成交量比较

        BigDecimal totalVolumeRatio = BigDecimal.ZERO;
        int count = 0;

        for (MarketContext.IndexData index : indices) {
            if (index.getVolume() == null || index.getVolume() == 0) continue;

            // 使用成交量与成交额的关系估算活跃度
            // 这里简化处理，假设成交量大表示活跃
            BigDecimal volumeScore = new BigDecimal("50"); // 默认中性

            // 如果涨跌幅为正且成交量大，加分
            if (index.getChangePercent() != null && index.getChangePercent().compareTo(BigDecimal.ZERO) > 0) {
                volumeScore = new BigDecimal("60"); // 偏乐观
            } else if (index.getChangePercent() != null && index.getChangePercent().compareTo(BigDecimal.ZERO) < 0) {
                volumeScore = new BigDecimal("40"); // 偏悲观
            }

            totalVolumeRatio = totalVolumeRatio.add(volumeScore);
            count++;
        }

        if (count == 0) {
            return new BigDecimal("50");
        }

        return totalVolumeRatio.divide(new BigDecimal(count), 2, RoundingMode.HALF_UP);
    }

    /**
     * 计算波动率得分
     * 波动率越低，得分越高（市场越稳定）
     */
    private static BigDecimal calculateVolatilityScore(List<MarketContext.IndexData> indices) {
        if (indices.size() < 2) {
            return new BigDecimal("50");
        }

        // 计算涨跌幅的标准差作为波动率指标
        BigDecimal sumChange = indices.stream()
                .map(MarketContext.IndexData::getChangePercent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgChange = sumChange.divide(new BigDecimal(indices.size()), 4, RoundingMode.HALF_UP);

        // 计算方差
        BigDecimal sumSquaredDiff = indices.stream()
                .map(MarketContext.IndexData::getChangePercent)
                .filter(Objects::nonNull)
                .map(change -> change.subtract(avgChange).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = sumSquaredDiff.divide(new BigDecimal(indices.size()), 4, RoundingMode.HALF_UP);

        // 标准差（简化计算，不开方）
        BigDecimal volatility = variance;

        // 波动率得分：波动率越低，得分越高
        // 假设波动率范围0-0.001，映射到100-0分
        BigDecimal score = new BigDecimal("100").subtract(
                volatility.multiply(new BigDecimal("100000")));

        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * 计算北向资金得分
     */
    private static BigDecimal calculateNorthboundScore(MarketDataService.NorthboundFlowData northboundFlow) {
        if (northboundFlow == null || northboundFlow.getTotalInflow() == null) {
            return new BigDecimal("50"); // 无数据时中性
        }

        BigDecimal inflow = northboundFlow.getTotalInflow();

        // 北向资金流入映射到得分
        // 假设流入范围：-50亿到+50亿，映射到0-100分
        BigDecimal score = inflow.divide(new BigDecimal("100000000"), 2, RoundingMode.HALF_UP) // 转换为亿
                .multiply(new BigDecimal("1")) // 系数
                .add(new BigDecimal("50"));

        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * 获取指数权重
     */
    private static BigDecimal getIndexWeight(String indexCode) {
        if (indexCode == null) {
            return new BigDecimal("0.1");
        }

        String code = indexCode.toUpperCase();
        if (code.contains("000001")) {
            return WEIGHT_SH_INDEX;
        } else if (code.contains("399001")) {
            return WEIGHT_SZ_INDEX;
        } else if (code.contains("399006")) {
            return WEIGHT_CY_INDEX;
        } else if (code.contains("000300")) {
            return WEIGHT_HS300;
        }
        return new BigDecimal("0.1");
    }

    /**
     * 将分数转换为情绪等级
     */
    private static String convertToLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("85")) >= 0) return "极度乐观";
        if (score.compareTo(new BigDecimal("70")) >= 0) return "乐观";
        if (score.compareTo(new BigDecimal("55")) >= 0) return "偏乐观";
        if (score.compareTo(new BigDecimal("45")) >= 0) return "中性";
        if (score.compareTo(new BigDecimal("30")) >= 0) return "偏悲观";
        if (score.compareTo(new BigDecimal("15")) >= 0) return "恐慌";
        return "极度恐慌";
    }

    /**
     * 创建中性情绪
     */
    private static MarketSentimentResult createNeutralSentiment() {
        return MarketSentimentResult.builder()
                .sentimentScore(new BigDecimal("50"))
                .sentimentLevel("中性")
                .fearGreedIndex(new BigDecimal("50"))
                .tradingEnthusiasm(new BigDecimal("50"))
                .volatilityExpectation(new BigDecimal("50"))
                .priceChangeScore(new BigDecimal("50"))
                .risingRatioScore(new BigDecimal("50"))
                .volumeScore(new BigDecimal("50"))
                .volatilityScore(new BigDecimal("50"))
                .northboundScore(new BigDecimal("50"))
                .build();
    }

    /**
     * 市场情绪计算结果
     */
    @Data
    @Builder
    public static class MarketSentimentResult {
        // 综合得分
        private BigDecimal sentimentScore;
        private String sentimentLevel;
        private BigDecimal fearGreedIndex;
        private BigDecimal tradingEnthusiasm;
        private BigDecimal volatilityExpectation;

        // 分项得分（用于分析）
        private BigDecimal priceChangeScore;    // 涨跌幅得分
        private BigDecimal risingRatioScore;    // 上涨比例得分
        private BigDecimal volumeScore;         // 成交量得分
        private BigDecimal volatilityScore;     // 波动率得分
        private BigDecimal northboundScore;     // 北向资金得分

        /**
         * 获取情绪分析摘要
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("市场情绪分析:\n");
            sb.append(String.format("综合得分: %.2f/100 (%s)\n", sentimentScore, sentimentLevel));
            sb.append(String.format("恐惧贪婪指数: %.2f\n", fearGreedIndex));
            sb.append(String.format("交易热情: %.2f\n", tradingEnthusiasm));
            sb.append("分项得分:\n");
            sb.append(String.format("  涨跌幅: %.2f\n", priceChangeScore));
            sb.append(String.format("  上涨比例: %.2f\n", risingRatioScore));
            sb.append(String.format("  成交量: %.2f\n", volumeScore));
            sb.append(String.format("  波动率: %.2f\n", volatilityScore));
            sb.append(String.format("  北向资金: %.2f\n", northboundScore));
            return sb.toString();
        }
    }
}
