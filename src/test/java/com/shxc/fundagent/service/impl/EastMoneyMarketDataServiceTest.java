package com.shxc.fundagent.service.impl;

import com.shxc.fundagent.agent.model.v2.MarketContext;
import com.shxc.fundagent.service.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 东方财富市场数据服务测试类
 * 测试东方财富接口的数据获取功能
 */
@ExtendWith(MockitoExtension.class)
class EastMoneyMarketDataServiceTest {

    private EastMoneyMarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        marketDataService = new EastMoneyMarketDataService();
    }

    // ================ 基础功能测试 ================

    @Test
    void testGetDataSourceName() {
        String dataSourceName = marketDataService.getDataSourceName();
        assertEquals("东方财富", dataSourceName);
    }

    @Test
    void testIsDataSourceHealthy() {
        boolean healthy = marketDataService.isDataSourceHealthy();
        System.out.println("东方财富数据源健康状态: " + healthy);
    }

    // ================ 指数数据测试 ================

    @Test
    void testGetIndexRealTimeData_ShangHaiIndex() {
        MarketContext.IndexData indexData = marketDataService.getIndexRealTimeData("sh000001");

        assertNotNull(indexData, "应该成功获取上证指数数据");
        assertNotNull(indexData.getIndexCode());
        assertNotNull(indexData.getIndexName());
        assertNotNull(indexData.getCurrentValue(), "当前价格不应为空");
        assertTrue(indexData.getCurrentValue().compareTo(BigDecimal.ZERO) > 0, "当前价格应该大于0");

        System.out.println("上证指数: " + indexData.getIndexName());
        System.out.println("当前值: " + indexData.getCurrentValue());
        System.out.println("涨跌幅: " + indexData.getChangePercent());
    }

    @Test
    void testGetIndexRealTimeData_ShenZhenIndex() {
        MarketContext.IndexData indexData = marketDataService.getIndexRealTimeData("sz399001");

        assertNotNull(indexData, "应该成功获取深证成指数据");
        assertNotNull(indexData.getCurrentValue());
        assertTrue(indexData.getCurrentValue().compareTo(BigDecimal.ZERO) > 0);

        System.out.println("深证成指: " + indexData.getCurrentValue());
    }

    @Test
    void testBatchGetIndexRealTimeData() {
        List<String> indexCodes = Arrays.asList("sh000001", "sz399001", "sz399006");
        List<MarketContext.IndexData> indexDataList = marketDataService.batchGetIndexRealTimeData(indexCodes);

        assertNotNull(indexDataList);
        System.out.println("批量获取指数数量: " + indexDataList.size());

        for (MarketContext.IndexData data : indexDataList) {
            System.out.println(data.getIndexName() + ": " + data.getCurrentValue());
        }
    }

    @Test
    void testGetMajorIndicesData() {
        List<MarketContext.IndexData> majorIndices = marketDataService.getMajorIndicesData();

        assertNotNull(majorIndices);
        assertFalse(majorIndices.isEmpty(), "应该返回主要指数数据");

        System.out.println("主要指数数量: " + majorIndices.size());
        for (MarketContext.IndexData index : majorIndices) {
            System.out.println(index.getIndexCode() + " - " + index.getIndexName() + ": " + index.getCurrentValue());
        }
    }

    // ================ 资金流向测试 ================

    @Test
    void testGetMarketFundFlow() {
        MarketDataService.FundFlowData fundFlow = marketDataService.getMarketFundFlow();

        // 东方财富支持资金流向数据
        System.out.println("市场资金流向: " + (fundFlow != null ? "支持" : "不支持"));

        if (fundFlow != null) {
            System.out.println("主力资金净流入: " + fundFlow.getMainForceInflow());
            System.out.println("大单净流入: " + fundFlow.getLargeOrderInflow());
            System.out.println("趋势: " + fundFlow.getTrend());
        }
    }

    // ================ 北向资金测试 ================

    @Test
    void testGetNorthboundFlow() {
        MarketDataService.NorthboundFlowData northboundFlow = marketDataService.getNorthboundFlow();

        // 东方财富支持北向资金数据
        System.out.println("北向资金: " + (northboundFlow != null ? "支持" : "不支持"));

        if (northboundFlow != null) {
            System.out.println("沪股通净流入: " + northboundFlow.getShanghaiInflow());
            System.out.println("深股通净流入: " + northboundFlow.getShenzhenInflow());
            System.out.println("总净流入: " + northboundFlow.getTotalInflow());
            System.out.println("趋势: " + northboundFlow.getTrend());
        }
    }

    // ================ 行业板块测试 ================

    @Test
    void testGetSectorPerformance() {
        Map<String, BigDecimal> sectorPerformance = marketDataService.getSectorPerformance();

        assertNotNull(sectorPerformance);
        System.out.println("行业板块数量: " + sectorPerformance.size());

        for (Map.Entry<String, BigDecimal> entry : sectorPerformance.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    @Test
    void testGetConceptPerformance() {
        Map<String, BigDecimal> conceptPerformance = marketDataService.getConceptPerformance();

        assertNotNull(conceptPerformance);
        System.out.println("概念板块数量: " + conceptPerformance.size());

        for (Map.Entry<String, BigDecimal> entry : conceptPerformance.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
    }

    // ================ 市场情绪测试 ================

    @Test
    void testGetMarketSentiment() {
        MarketDataService.MarketSentimentData sentiment = marketDataService.getMarketSentiment();

        assertNotNull(sentiment);
        assertNotNull(sentiment.getSentimentScore());
        assertNotNull(sentiment.getSentimentLevel());

        System.out.println("市场情绪分数: " + sentiment.getSentimentScore());
        System.out.println("市场情绪等级: " + sentiment.getSentimentLevel());
    }

    // ================ 股票数据测试 ================

    @Test
    void testGetStockRealTimeData() {
        MarketDataService.StockRealTimeData stockData = marketDataService.getStockRealTimeData("sh600519");

        assertNotNull(stockData, "应该成功获取股票数据");
        assertNotNull(stockData.getStockName());
        assertNotNull(stockData.getCurrentPrice());
        assertTrue(stockData.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0);

        System.out.println("股票: " + stockData.getStockName());
        System.out.println("当前价格: " + stockData.getCurrentPrice());
    }

    // ================ 综合测试 ================

    @Test
    void testFullMarketDataFlow() {
        System.out.println("=== 东方财富完整市场数据测试 ===");

        // 1. 获取主要指数
        List<MarketContext.IndexData> majorIndices = marketDataService.getMajorIndicesData();
        System.out.println("1. 主要指数数量: " + majorIndices.size());

        // 2. 获取行业板块
        Map<String, BigDecimal> sectors = marketDataService.getSectorPerformance();
        System.out.println("2. 行业板块数量: " + sectors.size());

        // 3. 获取概念板块
        Map<String, BigDecimal> concepts = marketDataService.getConceptPerformance();
        System.out.println("3. 概念板块数量: " + concepts.size());

        // 4. 获取资金流向
        MarketDataService.FundFlowData fundFlow = marketDataService.getMarketFundFlow();
        System.out.println("4. 资金流向: " + (fundFlow != null ? "支持" : "不支持"));

        // 5. 获取北向资金
        MarketDataService.NorthboundFlowData northbound = marketDataService.getNorthboundFlow();
        System.out.println("5. 北向资金: " + (northbound != null ? "支持" : "不支持"));

        // 6. 获取市场情绪
        MarketDataService.MarketSentimentData sentiment = marketDataService.getMarketSentiment();
        System.out.println("6. 市场情绪: " + (sentiment != null ? sentiment.getSentimentLevel() : "N/A"));

        // 7. 验证数据源
        boolean healthy = marketDataService.isDataSourceHealthy();
        System.out.println("7. 数据源健康: " + healthy);

        System.out.println("=== 测试完成 ===");

        assertFalse(majorIndices.isEmpty(), "应该能获取到主要指数数据");
    }

    // ================ 性能测试 ================

    @Test
    void testPerformance_BatchRequest() {
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 5; i++) {
            marketDataService.getMajorIndicesData();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("5次批量请求耗时: " + duration + "ms");
        System.out.println("平均每次请求耗时: " + (duration / 5) + "ms");

        assertTrue(duration < 30000, "批量请求应该在30秒内完成");
    }
}
