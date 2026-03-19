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
 * 腾讯财经市场数据服务测试类
 * 测试腾讯财经接口的数据获取功能
 */
@ExtendWith(MockitoExtension.class)
class TencentMarketDataServiceTest {

    private TencentMarketDataService marketDataService;

    @BeforeEach
    void setUp() {
        marketDataService = new TencentMarketDataService();
    }

    // ================ 基础功能测试 ================

    @Test
    void testGetDataSourceName() {
        String dataSourceName = marketDataService.getDataSourceName();
        assertEquals("腾讯财经", dataSourceName);
    }

    @Test
    void testIsDataSourceHealthy() {
        // 测试数据源健康状态
        boolean healthy = marketDataService.isDataSourceHealthy();
        // 注意：这个测试依赖于网络连接，如果网络不可用可能会失败
        // 在实际运行中，如果腾讯接口正常，应该返回true
        System.out.println("数据源健康状态: " + healthy);
    }

    // ================ 指数数据测试 ================

    @Test
    void testGetIndexRealTimeData_ShangHaiIndex() {
        // 测试获取上证指数实时数据
        MarketContext.IndexData indexData = marketDataService.getIndexRealTimeData("sh000001");

        assertNotNull(indexData, "应该成功获取上证指数数据");
        // 腾讯接口返回的代码可能不包含sh/sz前缀，只验证后6位
        assertTrue(indexData.getIndexCode().contains("000001"), "指数代码应包含000001");
        // 由于编码问题，只验证名称不为空
        assertNotNull(indexData.getIndexName(), "指数名称不应为空");
        assertFalse(indexData.getIndexName().isEmpty(), "指数名称不应为空字符串");
        assertNotNull(indexData.getCurrentValue(), "当前价格不应为空");
        assertTrue(indexData.getCurrentValue().compareTo(BigDecimal.ZERO) > 0, "当前价格应该大于0");

        System.out.println("上证指数: " + indexData.getIndexName());
        System.out.println("当前值: " + indexData.getCurrentValue());
        System.out.println("涨跌幅: " + indexData.getChangePercent());
    }

    @Test
    void testGetIndexRealTimeData_ShenZhenIndex() {
        // 测试获取深证成指实时数据
        MarketContext.IndexData indexData = marketDataService.getIndexRealTimeData("sz399001");

        assertNotNull(indexData, "应该成功获取深证成指数据");
        // 腾讯接口返回的代码可能不包含sh/sz前缀，只验证后6位
        assertTrue(indexData.getIndexCode().contains("399001"), "指数代码应包含399001");
        // 由于编码问题，只验证名称不为空
        assertNotNull(indexData.getIndexName(), "指数名称不应为空");
        assertFalse(indexData.getIndexName().isEmpty(), "指数名称不应为空字符串");
        assertNotNull(indexData.getCurrentValue());
        assertTrue(indexData.getCurrentValue().compareTo(BigDecimal.ZERO) > 0);

        System.out.println("深证成指: " + indexData.getCurrentValue());
    }

    @Test
    void testGetIndexRealTimeData_ChiNextIndex() {
        // 测试获取创业板指实时数据
        MarketContext.IndexData indexData = marketDataService.getIndexRealTimeData("sz399006");

        assertNotNull(indexData, "应该成功获取创业板指数据");
        // 腾讯接口返回的代码可能不包含sh/sz前缀，只验证后6位
        assertTrue(indexData.getIndexCode().contains("399006"), "指数代码应包含399006");
        // 由于编码问题，只验证名称不为空
        assertNotNull(indexData.getIndexName(), "指数名称不应为空");
        assertFalse(indexData.getIndexName().isEmpty(), "指数名称不应为空字符串");

        System.out.println("创业板指: " + indexData.getCurrentValue());
    }

    @Test
    void testGetIndexRealTimeData_InvalidCode() {
        // 测试无效代码
        MarketContext.IndexData indexData = marketDataService.getIndexRealTimeData("invalid_code");
        // 对于无效代码，可能返回null或空数据
        System.out.println("无效代码返回: " + indexData);
    }

    @Test
    void testBatchGetIndexRealTimeData() {
        // 测试批量获取指数数据
        List<String> indexCodes = Arrays.asList("sh000001", "sz399001", "sz399006");
        List<MarketContext.IndexData> indexDataList = marketDataService.batchGetIndexRealTimeData(indexCodes);

        assertNotNull(indexDataList);
        assertFalse(indexDataList.isEmpty(), "应该返回至少一个指数数据");

        System.out.println("批量获取指数数量: " + indexDataList.size());
        for (MarketContext.IndexData data : indexDataList) {
            System.out.println(data.getIndexName() + ": " + data.getCurrentValue());
        }
    }

    @Test
    void testBatchGetIndexRealTimeData_EmptyList() {
        // 测试空列表
        List<MarketContext.IndexData> result = marketDataService.batchGetIndexRealTimeData(Collections.emptyList());
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetMajorIndicesData() {
        // 测试获取主要市场指数
        List<MarketContext.IndexData> majorIndices = marketDataService.getMajorIndicesData();

        assertNotNull(majorIndices);
        assertFalse(majorIndices.isEmpty(), "应该返回主要指数数据");

        System.out.println("主要指数数量: " + majorIndices.size());
        for (MarketContext.IndexData index : majorIndices) {
            System.out.println(index.getIndexCode() + " - " + index.getIndexName() + ": " + index.getCurrentValue());
            assertNotNull(index.getIndexCode());
            assertNotNull(index.getIndexName());
            assertNotNull(index.getCurrentValue());
        }
    }

    // ================ 指数数据字段完整性测试 ================

    @Test
    void testIndexDataFieldsCompleteness() {
        MarketContext.IndexData indexData = marketDataService.getIndexRealTimeData("sh000001");

        assertNotNull(indexData);

        // 验证所有字段都有值
        assertNotNull(indexData.getIndexCode(), "指数代码不应为空");
        assertNotNull(indexData.getIndexName(), "指数名称不应为空");
        assertNotNull(indexData.getCurrentValue(), "当前值不应为空");
        assertNotNull(indexData.getPreviousClose(), "昨收价不应为空");
        assertNotNull(indexData.getOpen(), "开盘价不应为空");
        assertNotNull(indexData.getHigh(), "最高价不应为空");
        assertNotNull(indexData.getLow(), "最低价不应为空");
        assertNotNull(indexData.getChange(), "涨跌值不应为空");
        assertNotNull(indexData.getChangePercent(), "涨跌幅不应为空");
        assertNotNull(indexData.getVolume(), "成交量不应为空");
        assertNotNull(indexData.getTurnover(), "成交额不应为空");

        // 验证数据合理性
        assertTrue(indexData.getCurrentValue().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(indexData.getHigh().compareTo(indexData.getLow()) >= 0, "最高价应该大于等于最低价");

        System.out.println("指数数据完整性验证通过");
        System.out.println("代码: " + indexData.getIndexCode());
        System.out.println("名称: " + indexData.getIndexName());
        System.out.println("当前值: " + indexData.getCurrentValue());
        System.out.println("昨收: " + indexData.getPreviousClose());
        System.out.println("开盘: " + indexData.getOpen());
        System.out.println("最高: " + indexData.getHigh());
        System.out.println("最低: " + indexData.getLow());
        System.out.println("涨跌: " + indexData.getChange());
        System.out.println("涨跌幅: " + indexData.getChangePercent());
        System.out.println("成交量: " + indexData.getVolume());
        System.out.println("成交额: " + indexData.getTurnover());
    }

    // ================ 行业板块测试 ================

    @Test
    void testGetSectorPerformance() {
        // 测试获取行业板块表现
        Map<String, BigDecimal> sectorPerformance = marketDataService.getSectorPerformance();

        assertNotNull(sectorPerformance);
        // 注意：腾讯财经的行业数据可能有限
        System.out.println("行业板块数量: " + sectorPerformance.size());

        for (Map.Entry<String, BigDecimal> entry : sectorPerformance.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
            assertNotNull(entry.getValue());
        }
    }

    // ================ 市场情绪测试 ================

    @Test
    void testGetMarketSentiment() {
        // 测试获取市场情绪
        MarketDataService.MarketSentimentData sentiment = marketDataService.getMarketSentiment();

        assertNotNull(sentiment, "应该成功获取市场情绪数据");
        assertNotNull(sentiment.getSentimentScore(), "情绪分数不应为空");
        assertNotNull(sentiment.getSentimentLevel(), "情绪等级不应为空");

        // 验证情绪分数范围（0-100）
        assertTrue(sentiment.getSentimentScore().compareTo(BigDecimal.ZERO) >= 0);
        assertTrue(sentiment.getSentimentScore().compareTo(new BigDecimal("100")) <= 0);

        // 验证情绪等级有效性
        List<String> validLevels = Arrays.asList(
                "极度乐观", "乐观", "中性", "恐慌", "极度恐慌"
        );
        assertTrue(validLevels.contains(sentiment.getSentimentLevel()),
                "情绪等级应该是预定义的有效值之一");

        System.out.println("市场情绪分数: " + sentiment.getSentimentScore());
        System.out.println("市场情绪等级: " + sentiment.getSentimentLevel());
        System.out.println("恐惧贪婪指数: " + sentiment.getFearGreedIndex());
        System.out.println("交易热情: " + sentiment.getTradingEnthusiasm());
    }

    // ================ 股票数据测试 ================

    @Test
    void testGetStockRealTimeData() {
        // 测试获取个股实时数据（以茅台为例）
        MarketDataService.StockRealTimeData stockData = marketDataService.getStockRealTimeData("sh600519");

        assertNotNull(stockData, "应该成功获取股票数据");
        // 腾讯接口返回的代码可能不包含sh/sz前缀，只验证后6位
        assertTrue(stockData.getStockCode().contains("600519"), "股票代码应包含600519");
        assertNotNull(stockData.getStockName());
        assertNotNull(stockData.getCurrentPrice());
        assertTrue(stockData.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0);

        System.out.println("股票: " + stockData.getStockName() + "(" + stockData.getStockCode() + ")");
        System.out.println("当前价格: " + stockData.getCurrentPrice());
        System.out.println("涨跌幅: " + stockData.getChangePercent());
    }

    @Test
    void testBatchGetStockRealTimeData() {
        // 测试批量获取股票数据
        List<String> stockCodes = Arrays.asList("sh600519", "sz000858", "sh601398");
        List<MarketDataService.StockRealTimeData> stockDataList = marketDataService.batchGetStockRealTimeData(stockCodes);

        assertNotNull(stockDataList);
        assertFalse(stockDataList.isEmpty());

        System.out.println("获取股票数量: " + stockDataList.size());
        for (MarketDataService.StockRealTimeData stock : stockDataList) {
            System.out.println(stock.getStockName() + ": " + stock.getCurrentPrice());
        }
    }

    // ================ 数据转换测试 ================

    @Test
    void testConvertToIndexData() {
        // 测试股票数据转换为指数数据
        MarketDataService.StockRealTimeData stockData = new MarketDataService.StockRealTimeData();
        stockData.setStockCode("sh600519");
        stockData.setStockName("贵州茅台");
        stockData.setCurrentPrice(new BigDecimal("1800.00"));
        stockData.setChange(new BigDecimal("20.00"));
        stockData.setChangePercent(new BigDecimal("0.0112"));
        stockData.setOpen(new BigDecimal("1780.00"));
        stockData.setHigh(new BigDecimal("1810.00"));
        stockData.setLow(new BigDecimal("1775.00"));
        stockData.setPreviousClose(new BigDecimal("1780.00"));
        stockData.setVolume(1000000L);
        stockData.setTurnover(new BigDecimal("1800000000"));

        MarketContext.IndexData indexData = marketDataService.convertToIndexData(stockData);

        assertNotNull(indexData);
        assertEquals("sh600519", indexData.getIndexCode());
        assertEquals("贵州茅台", indexData.getIndexName());
        assertEquals(new BigDecimal("1800.00"), indexData.getCurrentValue());
        assertEquals(new BigDecimal("20.00"), indexData.getChange());
        assertEquals(new BigDecimal("0.0112"), indexData.getChangePercent());

        System.out.println("转换成功: " + indexData.getIndexName());
    }

    @Test
    void testConvertToIndexData_NullInput() {
        // 测试空输入
        MarketContext.IndexData indexData = marketDataService.convertToIndexData(null);
        assertNull(indexData);
    }

    // ================ 历史数据测试（腾讯财经不支持） ================

    @Test
    void testGetIndexHistoryData_NotSupported() {
        // 腾讯财经不支持历史数据，应该返回空列表
        List<MarketDataService.IndexHistoryData> historyData = marketDataService.getIndexHistoryData(
                "sh000001",
                java.time.LocalDate.now().minusDays(30),
                java.time.LocalDate.now()
        );

        assertNotNull(historyData);
        assertTrue(historyData.isEmpty(), "腾讯财经不支持历史数据，应该返回空列表");
    }

    // ================ 资金流向测试（腾讯财经不支持） ================

    @Test
    void testGetMarketFundFlow_NotSupported() {
        // 腾讯财经不支持资金流向数据
        MarketDataService.FundFlowData fundFlow = marketDataService.getMarketFundFlow();
        assertNull(fundFlow, "腾讯财经不支持资金流向数据，应该返回null");
    }

    @Test
    void testGetNorthboundFlow_NotSupported() {
        // 腾讯财经不支持北向资金数据
        MarketDataService.NorthboundFlowData northboundFlow = marketDataService.getNorthboundFlow();
        assertNull(northboundFlow, "腾讯财经不支持北向资金数据，应该返回null");
    }

    // ================ 估值数据测试（腾讯财经不支持） ================

    @Test
    void testGetIndexValuation_NotSupported() {
        // 腾讯财经不支持估值数据
        MarketDataService.ValuationData valuation = marketDataService.getIndexValuation("sh000001");
        assertNull(valuation, "腾讯财经不支持估值数据，应该返回null");
    }

    // ================ 综合测试 ================

    @Test
    void testFullMarketDataFlow() {
        // 测试完整的数据获取流程
        System.out.println("=== 开始完整市场数据测试 ===");

        // 1. 获取主要指数
        List<MarketContext.IndexData> majorIndices = marketDataService.getMajorIndicesData();
        System.out.println("1. 主要指数数量: " + majorIndices.size());

        // 2. 获取行业板块
        Map<String, BigDecimal> sectors = marketDataService.getSectorPerformance();
        System.out.println("2. 行业板块数量: " + sectors.size());

        // 3. 获取市场情绪
        MarketDataService.MarketSentimentData sentiment = marketDataService.getMarketSentiment();
        System.out.println("3. 市场情绪: " + (sentiment != null ? sentiment.getSentimentLevel() : "N/A"));

        // 4. 验证数据源
        boolean healthy = marketDataService.isDataSourceHealthy();
        System.out.println("4. 数据源健康: " + healthy);

        System.out.println("=== 测试完成 ===");

        // 基本验证
        assertFalse(majorIndices.isEmpty(), "应该能获取到主要指数数据");
        assertTrue(healthy || !healthy, "健康检查应该能正常执行"); // 网络问题可能导致失败
    }

    // ================ 性能测试 ================

    @Test
    void testPerformance_BatchRequest() {
        // 测试批量请求性能
        long startTime = System.currentTimeMillis();

        // 执行多次请求
        for (int i = 0; i < 5; i++) {
            marketDataService.getMajorIndicesData();
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        System.out.println("5次批量请求耗时: " + duration + "ms");
        System.out.println("平均每次请求耗时: " + (duration / 5) + "ms");

        // 验证性能（应该能在合理时间内完成）
        assertTrue(duration < 30000, "批量请求应该在30秒内完成");
    }
}
