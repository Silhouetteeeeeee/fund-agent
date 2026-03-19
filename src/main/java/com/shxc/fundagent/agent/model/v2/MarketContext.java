package com.shxc.fundagent.agent.model.v2;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 市场环境上下文
 * 包含当前市场状态、指数数据、市场情绪等信息
 */
@Data
public class MarketContext {

    /**
     * 市场状态枚举
     */
    public enum MarketStatus {
        /** 牛市 */
        BULL_MARKET,
        /** 熊市 */
        BEAR_MARKET,
        /** 震荡市 */
        RANGING_MARKET,
        /** 调整市 */
        CORRECTION_MARKET,
        /** 未知 */
        UNKNOWN
    }

    /**
     * 市场风险等级
     */
    public enum RiskLevel {
        /** 极低风险 */
        VERY_LOW,
        /** 低风险 */
        LOW,
        /** 中等风险 */
        MEDIUM,
        /** 高风险 */
        HIGH,
        /** 极高风险 */
        VERY_HIGH
    }

    /**
     * 市场情绪
     */
    public enum MarketSentiment {
        /** 极度悲观 */
        EXTREMELY_PESSIMISTIC,
        /** 悲观 */
        PESSIMISTIC,
        /** 中性 */
        NEUTRAL,
        /** 乐观 */
        OPTIMISTIC,
        /** 极度乐观 */
        EXTREMELY_OPTIMISTIC
    }

    // 基本信息
    private String contextId;
    private LocalDateTime timestamp;
    private LocalDate marketDate;

    // 市场整体状态
    private MarketStatus marketStatus = MarketStatus.UNKNOWN;
    private RiskLevel riskLevel = RiskLevel.MEDIUM;
    private MarketSentiment sentiment = MarketSentiment.NEUTRAL;
    private BigDecimal sentimentScore = BigDecimal.valueOf(50); // 情绪分数 0-100
    private String sentimentLevel = "中性"; // 情绪等级描述
    private BigDecimal marketTemperature = BigDecimal.valueOf(0.5); // 0.0-1.0，市场热度

    // 主要指数数据
    private Map<String, IndexData> indexData = new HashMap<>();

    // 行业表现
    private Map<String, BigDecimal> sectorPerformance = new HashMap<>();

    // 概念板块表现
    private Map<String, BigDecimal> conceptPerformance = new HashMap<>();

    // 技术指标
    private TechnicalIndicators technicalIndicators;

    // 基本面指标
    private FundamentalIndicators fundamentalIndicators;

    // 市场资金流向
    private FundFlowData fundFlowData;

    // 北向资金流向
    private NorthboundFlowData northboundFlowData;

    // 市场估值数据
    private ValuationData valuationData;

    // 关键事件
    private Map<String, MarketEvent> keyEvents = new HashMap<>();

    // 预警信号
    private Map<String, WarningSignal> warningSignals = new HashMap<>();

    // 市场建议
    private String marketAdvice;
    private BigDecimal confidenceScore = BigDecimal.ZERO;

    // 数据来源和元数据
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 指数数据类
     */
    @Data
    public static class IndexData {
        private String indexCode;           // 指数代码，如：SH000001（上证指数）
        private String indexName;           // 指数名称
        private BigDecimal currentValue;    // 当前值
        private BigDecimal change;          // 涨跌值
        private BigDecimal changePercent;   // 涨跌幅
        private BigDecimal open;            // 开盘价
        private BigDecimal high;            // 最高价
        private BigDecimal low;             // 最低价
        private BigDecimal previousClose;   // 前收盘价
        private Long volume;                // 成交量
        private BigDecimal turnover;        // 成交额
        private LocalDateTime updateTime;   // 更新时间
        private BigDecimal peRatio;         // 市盈率
        private BigDecimal pbRatio;         // 市净率
        private BigDecimal dividendYield;   // 股息率
        private String trend;               // 趋势：UP, DOWN, SIDEWAYS
    }

    /**
     * 技术指标
     */
    @Data
    public static class TechnicalIndicators {
        // 趋势指标
        private BigDecimal ma5;             // 5日均线
        private BigDecimal ma10;            // 10日均线
        private BigDecimal ma20;            // 20日均线
        private BigDecimal ma60;            // 60日均线
        private BigDecimal ma250;           // 250日均线

        // 震荡指标
        private BigDecimal rsi;             // 相对强弱指数 (0-100)
        private BigDecimal macd;            // MACD值
        private BigDecimal macdSignal;      // MACD信号线
        private BigDecimal macdHistogram;   // MACD柱状图
        private BigDecimal stochasticK;     // 随机指标K值
        private BigDecimal stochasticD;     // 随机指标D值

        // 波动率指标
        private BigDecimal bollingerUpper;  // 布林线上轨
        private BigDecimal bollingerMiddle; // 布林线中轨
        private BigDecimal bollingerLower;  // 布林线下轨
        private BigDecimal atr;             // 平均真实波幅

        // 成交量指标
        private BigDecimal volumeRatio;     // 量比
        private BigDecimal obv;             // 能量潮指标

        // 综合评估
        private String trendStrength;       // 趋势强度：STRONG_UP, UP, NEUTRAL, DOWN, STRONG_DOWN
        private BigDecimal trendScore = BigDecimal.ZERO;    // 趋势评分 (-1.0到1.0)
        private String marketPhase;         // 市场阶段：ACCUMULATION, UPTREND, DISTRIBUTION, DOWNTREND
    }

    /**
     * 基本面指标
     */
    @Data
    public static class FundamentalIndicators {
        // 宏观经济指标
        private BigDecimal gdpGrowth;               // GDP增长率
        private BigDecimal cpi;                     // 消费者物价指数
        private BigDecimal ppi;                     // 生产者物价指数
        private BigDecimal unemploymentRate;        // 失业率
        private BigDecimal interestRate;            // 利率
        private BigDecimal reserveRatio;            // 存款准备金率

        // 市场估值指标
        private BigDecimal marketCapToGDP;          // 股市总市值/GDP（巴菲特指标）
        private BigDecimal peRatioHistoric;         // 历史市盈率分位数
        private BigDecimal pbRatioHistoric;         // 历史市净率分位数
        private BigDecimal dividendYieldHistoric;   // 历史股息率分位数

        // 资金面指标
        private BigDecimal m2Growth;                // M2货币供应量增长率
        private BigDecimal socialFinancing;         // 社会融资规模增量
        private BigDecimal foreignInvestment;       // 外资流入/流出

        // 政策环境
        private String monetaryPolicy;              // 货币政策：LOOSE, NEUTRAL, TIGHT
        private String fiscalPolicy;                // 财政政策：EXPANSIVE, NEUTRAL, CONTRACTIVE
        private String regulatoryEnvironment;       // 监管环境：FRIENDLY, NEUTRAL, STRICT

        // 综合评估
        private BigDecimal fundamentalScore = BigDecimal.ZERO; // 基本面评分 (-1.0到1.0)
        private String economicCycle;               // 经济周期：RECOVERY, EXPANSION, PEAK, RECESSION
    }

    /**
     * 资金流向数据
     */
    @Data
    public static class FundFlowData {
        // 北向资金（沪股通+深股通）
        private BigDecimal northboundInflow;        // 北向资金净流入
        private BigDecimal northboundInflow5dAvg;   // 5日平均净流入
        private BigDecimal northboundInflow20dAvg;  // 20日平均净流入

        // 南向资金（港股通）
        private BigDecimal southboundInflow;        // 南向资金净流入

        // 主力资金
        private BigDecimal mainFundInflow;          // 主力资金净流入
        private Map<String, BigDecimal> mainFundInflowBySector = new HashMap<>(); // 分行业主力资金流入

        // 散户资金
        private BigDecimal retailFundInflow;        // 散户资金净流入

        // 资金流向趋势
        private String fundFlowTrend;               // 资金流向趋势：INFLOW, OUTFLOW, BALANCED
        private BigDecimal fundFlowScore = BigDecimal.ZERO; // 资金流向评分 (-1.0到1.0)
    }

    /**
     * 北向资金流向数据
     */
    @Data
    public static class NorthboundFlowData {
        // 当日净流入
        private BigDecimal totalInflow;             // 北向资金总净流入
        private BigDecimal shanghaiInflow;          // 沪股通净流入
        private BigDecimal shenzhenInflow;          // 深股通净流入

        // 累计数据
        private BigDecimal cumulativeInflow;        // 累计净流入

        // 趋势判断
        private String trend;                       // 趋势：INFLOW, OUTFLOW, BALANCED

        // 5日和20日平均
        private BigDecimal inflow5dAvg;             // 5日平均净流入
        private BigDecimal inflow20dAvg;            // 20日平均净流入
    }

    /**
     * 市场估值数据
     */
    @Data
    public static class ValuationData {
        // 估值指标
        private BigDecimal peRatio;                 // 市盈率
        private BigDecimal pbRatio;                 // 市净率
        private BigDecimal psRatio;                 // 市销率
        private BigDecimal dividendYield;           // 股息率

        // 历史分位数
        private BigDecimal pePercentile;            // PE分位数（0-100）
        private BigDecimal pbPercentile;            // PB分位数（0-100）

        // 估值水平判断
        private String valuationLevel;              // 估值水平：UNDERVALUED, FAIR, OVERVALUED

        // 风险溢价
        private BigDecimal riskPremium;             // 股权风险溢价
    }

    /**
     * 市场事件
     */
    @Data
    public static class MarketEvent {
        private String eventId;
        private String eventType;           // 事件类型：ECONOMIC_DATA, POLICY, GEOPOLITICAL, COMPANY_NEWS
        private String eventName;
        private String description;
        private LocalDateTime eventTime;
        private BigDecimal impactScore = BigDecimal.ZERO;    // 影响评分 (-1.0到1.0)
        private String impactDirection;     // 影响方向：POSITIVE, NEGATIVE, NEUTRAL
        private Map<String, Object> details = new HashMap<>();
    }

    /**
     * 预警信号
     */
    @Data
    public static class WarningSignal {
        private String signalId;
        private String signalType;          // 信号类型：TECHNICAL, FUNDAMENTAL, SENTIMENT, LIQUIDITY
        private String signalName;
        private String description;
        private String severity;            // 严重程度：LOW, MEDIUM, HIGH, CRITICAL
        private BigDecimal confidence = BigDecimal.ZERO;    // 置信度 (0.0-1.0)
        private String action;              // 建议行动：MONITOR, REDUCE_POSITION, HEDGE, EXIT
        private Map<String, Object> details = new HashMap<>();
    }

    /**
     * 添加指数数据
     */
    public void addIndexData(IndexData indexData) {
        if (indexData != null && indexData.getIndexCode() != null) {
            this.indexData.put(indexData.getIndexCode(), indexData);
        }
    }

    /**
     * 获取指数数据
     */
    public IndexData getIndexData(String indexCode) {
        return indexData.get(indexCode);
    }

    /**
     * 添加行业表现
     */
    public void addSectorPerformance(String sector, BigDecimal performance) {
        if (sector != null && performance != null) {
            this.sectorPerformance.put(sector, performance);
        }
    }

    /**
     * 添加关键事件
     */
    public void addKeyEvent(MarketEvent event) {
        if (event != null && event.getEventId() != null) {
            this.keyEvents.put(event.getEventId(), event);
        }
    }

    /**
     * 添加预警信号
     */
    public void addWarningSignal(WarningSignal signal) {
        if (signal != null && signal.getSignalId() != null) {
            this.warningSignals.put(signal.getSignalId(), signal);
        }
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (key != null && value != null) {
            this.metadata.put(key, value);
        }
    }

    /**
     * 设置概念板块表现
     */
    public void setConceptPerformance(Map<String, BigDecimal> conceptPerformance) {
        if (conceptPerformance != null) {
            this.conceptPerformance = conceptPerformance;
        }
    }

    /**
     * 设置北向资金流向数据
     */
    public void setNorthboundFlowData(com.shxc.fundagent.service.MarketDataService.NorthboundFlowData data) {
        if (data != null) {
            NorthboundFlowData internalData = new NorthboundFlowData();
            internalData.setTotalInflow(data.getTotalInflow());
            internalData.setShanghaiInflow(data.getShanghaiInflow());
            internalData.setShenzhenInflow(data.getShenzhenInflow());
            internalData.setCumulativeInflow(data.getCumulativeInflow());
            internalData.setTrend(data.getTrend());
            this.northboundFlowData = internalData;
        }
    }

    /**
     * 设置市场估值数据
     */
    public void setValuationData(com.shxc.fundagent.service.MarketDataService.ValuationData data) {
        if (data != null) {
            ValuationData internalData = new ValuationData();
            internalData.setPeRatio(data.getPeRatio());
            internalData.setPbRatio(data.getPbRatio());
            internalData.setPsRatio(data.getPsRatio());
            internalData.setDividendYield(data.getDividendYield());
            internalData.setPePercentile(data.getPePercentile());
            internalData.setPbPercentile(data.getPbPercentile());
            internalData.setValuationLevel(data.getValuationLevel());
            this.valuationData = internalData;
        }
    }

    /**
     * 设置资金流向数据
     */
    public void setFundFlowData(com.shxc.fundagent.service.MarketDataService.FundFlowData data) {
        if (data != null) {
            FundFlowData internalData = new FundFlowData();
            internalData.setNorthboundInflow(data.getTotalInflow());
            internalData.setMainFundInflow(data.getMainForceInflow());
            internalData.setRetailFundInflow(data.getRetailInflow());
            internalData.setFundFlowTrend(data.getTrend());
            this.fundFlowData = internalData;
        }
    }

    /**
     * 计算市场温度（综合评分）
     */
    public BigDecimal calculateMarketTemperature() {
        // 简单实现：基于多个指标的加权平均
        // 实际应用中应该使用更复杂的算法
        BigDecimal totalScore = BigDecimal.ZERO;
        int factorCount = 0;

        if (technicalIndicators != null && technicalIndicators.getTrendScore() != null) {
            totalScore = totalScore.add(technicalIndicators.getTrendScore().multiply(BigDecimal.valueOf(0.4)));
            factorCount++;
        }

        if (fundamentalIndicators != null && fundamentalIndicators.getFundamentalScore() != null) {
            totalScore = totalScore.add(fundamentalIndicators.getFundamentalScore().multiply(BigDecimal.valueOf(0.3)));
            factorCount++;
        }

        if (fundFlowData != null && fundFlowData.getFundFlowScore() != null) {
            totalScore = totalScore.add(fundFlowData.getFundFlowScore().multiply(BigDecimal.valueOf(0.3)));
            factorCount++;
        }

        // 将评分转换为0-1范围
        if (factorCount > 0) {
            // 原始评分范围是-1到1，转换为0到1
            BigDecimal normalizedScore = totalScore.add(BigDecimal.ONE).divide(BigDecimal.valueOf(2), 4, BigDecimal.ROUND_HALF_UP);
            this.marketTemperature = normalizedScore;
        }

        return this.marketTemperature;
    }

    /**
     * 获取简要的市场状态描述
     */
    public String getMarketSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append("市场状态: ").append(marketStatus);
        summary.append(", 风险等级: ").append(riskLevel);
        summary.append(", 市场情绪: ").append(sentiment);
        summary.append(", 市场温度: ").append(marketTemperature.multiply(BigDecimal.valueOf(100)).intValue()).append("%");

        if (technicalIndicators != null) {
            summary.append(", 趋势强度: ").append(technicalIndicators.getTrendStrength());
        }

        if (fundamentalIndicators != null) {
            summary.append(", 经济周期: ").append(fundamentalIndicators.getEconomicCycle());
        }

        if (!warningSignals.isEmpty()) {
            summary.append(", 预警信号: ").append(warningSignals.size()).append("个");
        }

        return summary.toString();
    }
}