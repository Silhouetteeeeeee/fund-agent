package com.shxc.fundagent.service;

import com.shxc.fundagent.agent.model.v2.MarketContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 市场数据服务接口
 * 提供股票指数、行业数据、市场指标等市场数据的获取功能
 */
public interface MarketDataService {

    /**
     * 获取指数实时数据
     *
     * @param indexCode 指数代码（如：sh000001, sz399001）
     * @return 指数实时数据，如果获取失败返回null
     */
    MarketContext.IndexData getIndexRealTimeData(String indexCode);

    /**
     * 批量获取指数实时数据
     *
     * @param indexCodes 指数代码列表
     * @return 指数实时数据列表
     */
    List<MarketContext.IndexData> batchGetIndexRealTimeData(List<String> indexCodes);

    /**
     * 获取主要市场指数数据
     * 包括：上证指数、深证成指、创业板指、沪深300等
     *
     * @return 主要指数数据列表
     */
    List<MarketContext.IndexData> getMajorIndicesData();

    /**
     * 获取指数历史数据
     *
     * @param indexCode 指数代码
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 历史数据列表
     */
    List<IndexHistoryData> getIndexHistoryData(String indexCode, LocalDate startDate, LocalDate endDate);

    /**
     * 获取行业板块数据
     *
     * @return 行业板块表现数据
     */
    Map<String, BigDecimal> getSectorPerformance();

    /**
     * 获取概念板块数据
     *
     * @return 概念板块表现数据
     */
    Map<String, BigDecimal> getConceptPerformance();

    /**
     * 获取市场资金流向
     *
     * @return 资金流向数据
     */
    FundFlowData getMarketFundFlow();

    /**
     * 获取北向资金流向
     *
     * @return 北向资金流向数据
     */
    NorthboundFlowData getNorthboundFlow();

    /**
     * 获取市场情绪指标
     *
     * @return 市场情绪数据
     */
    MarketSentimentData getMarketSentiment();

    /**
     * 获取市场估值数据
     *
     * @param indexCode 指数代码
     * @return 估值数据（PE、PB等）
     */
    ValuationData getIndexValuation(String indexCode);

    /**
     * 获取个股实时数据（用于计算行业/概念指数）
     *
     * @param stockCode 股票代码
     * @return 股票实时数据
     */
    StockRealTimeData getStockRealTimeData(String stockCode);

    /**
     * 批量获取个股实时数据
     *
     * @param stockCodes 股票代码列表
     * @return 股票实时数据列表
     */
    List<StockRealTimeData> batchGetStockRealTimeData(List<String> stockCodes);

    /**
     * 检查数据源健康状态
     *
     * @return true如果数据源正常
     */
    boolean isDataSourceHealthy();

    /**
     * 获取数据源名称
     *
     * @return 数据源名称
     */
    String getDataSourceName();

    // ================ 数据类定义 ================

    /**
     * 指数历史数据
     */
    class IndexHistoryData {
        private String indexCode;
        private String indexName;
        private LocalDate tradeDate;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal close;
        private BigDecimal change;
        private BigDecimal changePercent;
        private Long volume;
        private BigDecimal turnover;

        // Getters and Setters
        public String getIndexCode() { return indexCode; }
        public void setIndexCode(String indexCode) { this.indexCode = indexCode; }
        public String getIndexName() { return indexName; }
        public void setIndexName(String indexName) { this.indexName = indexName; }
        public LocalDate getTradeDate() { return tradeDate; }
        public void setTradeDate(LocalDate tradeDate) { this.tradeDate = tradeDate; }
        public BigDecimal getOpen() { return open; }
        public void setOpen(BigDecimal open) { this.open = open; }
        public BigDecimal getHigh() { return high; }
        public void setHigh(BigDecimal high) { this.high = high; }
        public BigDecimal getLow() { return low; }
        public void setLow(BigDecimal low) { this.low = low; }
        public BigDecimal getClose() { return close; }
        public void setClose(BigDecimal close) { this.close = close; }
        public BigDecimal getChange() { return change; }
        public void setChange(BigDecimal change) { this.change = change; }
        public BigDecimal getChangePercent() { return changePercent; }
        public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }
        public Long getVolume() { return volume; }
        public void setVolume(Long volume) { this.volume = volume; }
        public BigDecimal getTurnover() { return turnover; }
        public void setTurnover(BigDecimal turnover) { this.turnover = turnover; }
    }

    /**
     * 资金流向数据
     */
    class FundFlowData {
        private BigDecimal mainForceInflow;      // 主力资金净流入
        private BigDecimal retailInflow;         // 散户资金净流入
        private BigDecimal totalInflow;          // 总净流入
        private BigDecimal largeOrderInflow;     // 大单净流入
        private BigDecimal mediumOrderInflow;    // 中单净流入
        private BigDecimal smallOrderInflow;     // 小单净流入
        private String trend;                    // 趋势：INFLOW/OUTFLOW

        // Getters and Setters
        public BigDecimal getMainForceInflow() { return mainForceInflow; }
        public void setMainForceInflow(BigDecimal mainForceInflow) { this.mainForceInflow = mainForceInflow; }
        public BigDecimal getRetailInflow() { return retailInflow; }
        public void setRetailInflow(BigDecimal retailInflow) { this.retailInflow = retailInflow; }
        public BigDecimal getTotalInflow() { return totalInflow; }
        public void setTotalInflow(BigDecimal totalInflow) { this.totalInflow = totalInflow; }
        public BigDecimal getLargeOrderInflow() { return largeOrderInflow; }
        public void setLargeOrderInflow(BigDecimal largeOrderInflow) { this.largeOrderInflow = largeOrderInflow; }
        public BigDecimal getMediumOrderInflow() { return mediumOrderInflow; }
        public void setMediumOrderInflow(BigDecimal mediumOrderInflow) { this.mediumOrderInflow = mediumOrderInflow; }
        public BigDecimal getSmallOrderInflow() { return smallOrderInflow; }
        public void setSmallOrderInflow(BigDecimal smallOrderInflow) { this.smallOrderInflow = smallOrderInflow; }
        public String getTrend() { return trend; }
        public void setTrend(String trend) { this.trend = trend; }
    }

    /**
     * 北向资金流向数据
     */
    class NorthboundFlowData {
        private BigDecimal shanghaiInflow;       // 沪股通净流入
        private BigDecimal shenzhenInflow;       // 深股通净流入
        private BigDecimal totalInflow;          // 北向资金总净流入
        private BigDecimal cumulativeInflow;     // 累计净流入
        private String trend;                    // 趋势

        // Getters and Setters
        public BigDecimal getShanghaiInflow() { return shanghaiInflow; }
        public void setShanghaiInflow(BigDecimal shanghaiInflow) { this.shanghaiInflow = shanghaiInflow; }
        public BigDecimal getShenzhenInflow() { return shenzhenInflow; }
        public void setShenzhenInflow(BigDecimal shenzhenInflow) { this.shenzhenInflow = shenzhenInflow; }
        public BigDecimal getTotalInflow() { return totalInflow; }
        public void setTotalInflow(BigDecimal totalInflow) { this.totalInflow = totalInflow; }
        public BigDecimal getCumulativeInflow() { return cumulativeInflow; }
        public void setCumulativeInflow(BigDecimal cumulativeInflow) { this.cumulativeInflow = cumulativeInflow; }
        public String getTrend() { return trend; }
        public void setTrend(String trend) { this.trend = trend; }
    }

    /**
     * 市场情绪数据
     */
    class MarketSentimentData {
        private BigDecimal sentimentScore;       // 情绪分数（0-100）
        private String sentimentLevel;           // 情绪等级：极度恐慌/恐慌/中性/乐观/极度乐观
        private BigDecimal fearGreedIndex;       // 恐惧贪婪指数
        private BigDecimal tradingEnthusiasm;    // 交易热情
        private BigDecimal volatilityExpectation;// 波动率预期

        // Getters and Setters
        public BigDecimal getSentimentScore() { return sentimentScore; }
        public void setSentimentScore(BigDecimal sentimentScore) { this.sentimentScore = sentimentScore; }
        public String getSentimentLevel() { return sentimentLevel; }
        public void setSentimentLevel(String sentimentLevel) { this.sentimentLevel = sentimentLevel; }
        public BigDecimal getFearGreedIndex() { return fearGreedIndex; }
        public void setFearGreedIndex(BigDecimal fearGreedIndex) { this.fearGreedIndex = fearGreedIndex; }
        public BigDecimal getTradingEnthusiasm() { return tradingEnthusiasm; }
        public void setTradingEnthusiasm(BigDecimal tradingEnthusiasm) { this.tradingEnthusiasm = tradingEnthusiasm; }
        public BigDecimal getVolatilityExpectation() { return volatilityExpectation; }
        public void setVolatilityExpectation(BigDecimal volatilityExpectation) { this.volatilityExpectation = volatilityExpectation; }
    }

    /**
     * 估值数据
     */
    class ValuationData {
        private BigDecimal peRatio;              // 市盈率
        private BigDecimal pbRatio;              // 市净率
        private BigDecimal psRatio;              // 市销率
        private BigDecimal dividendYield;        // 股息率
        private BigDecimal pePercentile;         // PE分位数
        private BigDecimal pbPercentile;         // PB分位数
        private String valuationLevel;           // 估值水平：低估/合理/高估

        // Getters and Setters
        public BigDecimal getPeRatio() { return peRatio; }
        public void setPeRatio(BigDecimal peRatio) { this.peRatio = peRatio; }
        public BigDecimal getPbRatio() { return pbRatio; }
        public void setPbRatio(BigDecimal pbRatio) { this.pbRatio = pbRatio; }
        public BigDecimal getPsRatio() { return psRatio; }
        public void setPsRatio(BigDecimal psRatio) { this.psRatio = psRatio; }
        public BigDecimal getDividendYield() { return dividendYield; }
        public void setDividendYield(BigDecimal dividendYield) { this.dividendYield = dividendYield; }
        public BigDecimal getPePercentile() { return pePercentile; }
        public void setPePercentile(BigDecimal pePercentile) { this.pePercentile = pePercentile; }
        public BigDecimal getPbPercentile() { return pbPercentile; }
        public void setPbPercentile(BigDecimal pbPercentile) { this.pbPercentile = pbPercentile; }
        public String getValuationLevel() { return valuationLevel; }
        public void setValuationLevel(String valuationLevel) { this.valuationLevel = valuationLevel; }
    }

    /**
     * 个股实时数据
     */
    class StockRealTimeData {
        private String stockCode;
        private String stockName;
        private BigDecimal currentPrice;
        private BigDecimal change;
        private BigDecimal changePercent;
        private BigDecimal open;
        private BigDecimal high;
        private BigDecimal low;
        private BigDecimal previousClose;
        private Long volume;
        private BigDecimal turnover;
        private BigDecimal marketCap;
        private String industry;

        // Getters and Setters
        public String getStockCode() { return stockCode; }
        public void setStockCode(String stockCode) { this.stockCode = stockCode; }
        public String getStockName() { return stockName; }
        public void setStockName(String stockName) { this.stockName = stockName; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
        public BigDecimal getChange() { return change; }
        public void setChange(BigDecimal change) { this.change = change; }
        public BigDecimal getChangePercent() { return changePercent; }
        public void setChangePercent(BigDecimal changePercent) { this.changePercent = changePercent; }
        public BigDecimal getOpen() { return open; }
        public void setOpen(BigDecimal open) { this.open = open; }
        public BigDecimal getHigh() { return high; }
        public void setHigh(BigDecimal high) { this.high = high; }
        public BigDecimal getLow() { return low; }
        public void setLow(BigDecimal low) { this.low = low; }
        public BigDecimal getPreviousClose() { return previousClose; }
        public void setPreviousClose(BigDecimal previousClose) { this.previousClose = previousClose; }
        public Long getVolume() { return volume; }
        public void setVolume(Long volume) { this.volume = volume; }
        public BigDecimal getTurnover() { return turnover; }
        public void setTurnover(BigDecimal turnover) { this.turnover = turnover; }
        public BigDecimal getMarketCap() { return marketCap; }
        public void setMarketCap(BigDecimal marketCap) { this.marketCap = marketCap; }
        public String getIndustry() { return industry; }
        public void setIndustry(String industry) { this.industry = industry; }
    }
}
