package com.shxc.fundagent.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shxc.fundagent.agent.model.v2.MarketContext;
import com.shxc.fundagent.service.MarketDataService;
import com.shxc.fundagent.utils.MarketSentimentCalculator;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 东方财富市场数据服务实现
 * 使用东方财富接口获取实时市场数据
 * 优势：提供资金流向、北向资金、板块数据等更全面的数据
 */
@Slf4j
@Service
public class EastMoneyMarketDataService implements MarketDataService {

    private static final String DATA_SOURCE_NAME = "东方财富";

    // 东方财富API接口
    private static final String INDEX_API_URL = "https://push2.eastmoney.com/api/qt/ulist.np/get";
    private static final String STOCK_API_URL = "https://push2.eastmoney.com/api/qt/stock/get";
    private static final String FUND_FLOW_URL = "https://push2.eastmoney.com/api/qt/ulist.np/get";
    private static final String NORTHBOUND_URL = "https://push2.eastmoney.com/api/qt/kamt.rtmin/get";
    private static final String SECTOR_URL = "https://push2.eastmoney.com/api/qt/clist/get";

    // 主要指数代码映射（东方财富格式）
    private static final Map<String, String> MAJOR_INDICES = new LinkedHashMap<>();

    static {
        MAJOR_INDICES.put("1.000001", "上证指数");
        MAJOR_INDICES.put("0.399001", "深证成指");
        MAJOR_INDICES.put("0.399006", "创业板指");
        MAJOR_INDICES.put("1.000300", "沪深300");
        MAJOR_INDICES.put("1.000016", "上证50");
        MAJOR_INDICES.put("1.000905", "中证500");
        MAJOR_INDICES.put("0.399005", "中小板指");
        MAJOR_INDICES.put("1.000688", "科创50");
    }

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EastMoneyMarketDataService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public MarketContext.IndexData getIndexRealTimeData(String indexCode) {
        try {
            // 转换代码格式
            String eastMoneyCode = convertToEastMoneyCode(indexCode);
            String url = buildIndexUrl(Collections.singletonList(eastMoneyCode));

            String response = fetchData(url);
            List<MarketContext.IndexData> results = parseIndexData(response);

            return results.isEmpty() ? null : results.get(0);
        } catch (Exception e) {
            log.error("获取指数 {} 实时数据失败", indexCode, e);
            return null;
        }
    }

    @Override
    public List<MarketContext.IndexData> batchGetIndexRealTimeData(List<String> indexCodes) {
        if (indexCodes == null || indexCodes.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<String> eastMoneyCodes = indexCodes.stream()
                    .map(this::convertToEastMoneyCode)
                    .collect(Collectors.toList());

            String url = buildIndexUrl(eastMoneyCodes);
            String response = fetchData(url);
            return parseIndexData(response);
        } catch (Exception e) {
            log.error("批量获取指数数据失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MarketContext.IndexData> getMajorIndicesData() {
        List<String> codes = new ArrayList<>(MAJOR_INDICES.keySet());
        return batchGetIndexRealTimeData(codes.stream()
                .map(this::convertFromEastMoneyCode)
                .collect(Collectors.toList()));
    }

    @Override
    public List<IndexHistoryData> getIndexHistoryData(String indexCode, LocalDate startDate, LocalDate endDate) {
        // 东方财富支持历史数据，可以实现
        log.warn("东方财富历史数据接口待实现");
        return Collections.emptyList();
    }

    @Override
    public Map<String, BigDecimal> getSectorPerformance() {
        try {
            String url = buildSectorUrl();
            String response = fetchData(url);
            return parseSectorData(response);
        } catch (Exception e) {
            log.error("获取行业板块数据失败", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public Map<String, BigDecimal> getConceptPerformance() {
        // 概念板块数据
        try {
            String url = buildConceptUrl();
            String response = fetchData(url);
            return parseConceptData(response);
        } catch (Exception e) {
            log.error("获取概念板块数据失败", e);
            return Collections.emptyMap();
        }
    }

    @Override
    public FundFlowData getMarketFundFlow() {
        try {
            String url = buildFundFlowUrl();
            String response = fetchData(url);
            return parseFundFlowData(response);
        } catch (Exception e) {
            log.error("获取市场资金流向失败", e);
            return null;
        }
    }

    @Override
    public NorthboundFlowData getNorthboundFlow() {
        try {
            String url = buildNorthboundUrl();
            String response = fetchData(url);
            return parseNorthboundData(response);
        } catch (Exception e) {
            log.error("获取北向资金数据失败", e);
            return null;
        }
    }

    @Override
    public MarketSentimentData getMarketSentiment() {
        // 使用公共的情绪计算器
        List<MarketContext.IndexData> indices = getMajorIndicesData();

        // 获取北向资金数据（东方财富支持）
        NorthboundFlowData northboundFlow = getNorthboundFlow();

        MarketSentimentCalculator.MarketSentimentResult result =
                MarketSentimentCalculator.calculateSentiment(indices, northboundFlow);

        // 转换为MarketSentimentData格式
        MarketSentimentData sentiment = new MarketSentimentData();
        sentiment.setSentimentScore(result.getSentimentScore());
        sentiment.setSentimentLevel(result.getSentimentLevel());
        sentiment.setFearGreedIndex(result.getFearGreedIndex());
        sentiment.setTradingEnthusiasm(result.getTradingEnthusiasm());
        sentiment.setVolatilityExpectation(result.getVolatilityExpectation());

        return sentiment;
    }

    @Override
    public ValuationData getIndexValuation(String indexCode) {
        // 东方财富提供估值数据
        log.warn("东方财富估值数据接口待实现");
        return null;
    }

    @Override
    public StockRealTimeData getStockRealTimeData(String stockCode) {
        try {
            String eastMoneyCode = convertToEastMoneyCode(stockCode);
            String url = buildStockUrl(eastMoneyCode);

            String response = fetchData(url);
            return parseStockData(response);
        } catch (Exception e) {
            log.error("获取股票 {} 实时数据失败", stockCode, e);
            return null;
        }
    }

    @Override
    public List<StockRealTimeData> batchGetStockRealTimeData(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        List<StockRealTimeData> results = new ArrayList<>();
        for (String code : stockCodes) {
            StockRealTimeData data = getStockRealTimeData(code);
            if (data != null) {
                results.add(data);
            }
        }
        return results;
    }

    @Override
    public boolean isDataSourceHealthy() {
        try {
            MarketContext.IndexData data = getIndexRealTimeData("sh000001");
            return data != null && data.getCurrentValue() != null;
        } catch (Exception e) {
            log.error("数据源健康检查失败", e);
            return false;
        }
    }

    @Override
    public String getDataSourceName() {
        return DATA_SOURCE_NAME;
    }

    // ================ 私有方法：URL构建 ================

    private String buildIndexUrl(List<String> codes) {
        String codeList = String.join(",", codes);
        return INDEX_API_URL + "?fltt=2&invt=2&fields=f12,f13,f14,f2,f3,f4,f5,f6,f7,f8,f9,f10,f11,f18,f20,f21,f33,f34,f35,f36,f37,f38,f39,f40,f41,f42,f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65,f66,f67,f68,f69,f70,f71,f72,f73,f74,f75,f76,f77,f78,f79,f80,f81,f82,f83,f84,f85,f86,f87,f88,f89,f90,f91,f92,f93,f94,f95,f96,f97,f98,f99,f100&secids=" + codeList;
    }

    private String buildStockUrl(String code) {
        return STOCK_API_URL + "?secid=" + code + "&fields=f43,f44,f45,f46,f47,f48,f50,f51,f52,f57,f58,f60,f107,f116,f117,f118,f119,f120,f121,f122,f123,f124,f125,f126,f127,f128,f129,f130,f131,f132,f133,f134,f135,f136,f137,f138,f139,f140,f141,f142,f143,f144,f145,f146,f147,f148,f149,f150,f151,f152,f153,f154,f155,f156,f157,f158,f159,f160,f161,f162,f163,f164,f165,f166,f167,f168,f169,f170";
    }

    private String buildFundFlowUrl() {
        // 主力资金流向
        return FUND_FLOW_URL + "?fltt=2&invt=2&fields=f12,f13,f14,f20,f21,f22,f23,f24,f25,f26,f27,f28,f29,f30,f31,f32,f33,f34,f35,f36,f37,f38,f39,f40,f41,f42,f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65,f66,f67,f68,f69,f70,f71,f72,f73,f74,f75,f76,f77,f78,f79,f80,f81,f82,f83,f84,f85,f86,f87,f88,f89,f90,f91,f92,f93,f94,f95,f96,f97,f98,f99,f100&secids=1.000001,0.399001";
    }

    private String buildNorthboundUrl() {
        // 北向资金
        return NORTHBOUND_URL + "?fields1=f1,f2,f3,f4&fields2=f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65,f66,f67,f68,f69,f70,f71,f72,f73,f74,f75,f76,f77,f78,f79,f80,f81,f82,f83,f84,f85,f86,f87,f88,f89,f90,f91,f92,f93,f94,f95,f96,f97,f98,f99,f100";
    }

    private String buildSectorUrl() {
        // 行业板块
        return SECTOR_URL + "?pn=1&pz=20&po=1&np=1&fltt=2&invt=2&fid=f20&fs=m:90+t:2&fields=f12,f13,f14,f20,f21,f22,f23,f24,f25,f26,f27,f28,f29,f30,f31,f32,f33,f34,f35,f36,f37,f38,f39,f40,f41,f42,f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65,f66,f67,f68,f69,f70,f71,f72,f73,f74,f75,f76,f77,f78,f79,f80,f81,f82,f83,f84,f85,f86,f87,f88,f89,f90,f91,f92,f93,f94,f95,f96,f97,f98,f99,f100";
    }

    private String buildConceptUrl() {
        // 概念板块
        return SECTOR_URL + "?pn=1&pz=20&po=1&np=1&fltt=2&invt=2&fid=f20&fs=m:90+t:3&fields=f12,f13,f14,f20,f21,f22,f23,f24,f25,f26,f27,f28,f29,f30,f31,f32,f33,f34,f35,f36,f37,f38,f39,f40,f41,f42,f43,f44,f45,f46,f47,f48,f49,f50,f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f62,f63,f64,f65,f66,f67,f68,f69,f70,f71,f72,f73,f74,f75,f76,f77,f78,f79,f80,f81,f82,f83,f84,f85,f86,f87,f88,f89,f90,f91,f92,f93,f94,f95,f96,f97,f98,f99,f100";
    }

    // ================ 私有方法：数据解析 ================

    private List<MarketContext.IndexData> parseIndexData(String response) {
        List<MarketContext.IndexData> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.path("data");

            if (dataNode.isMissingNode() || !dataNode.has("diff")) {
                log.warn("返回数据格式不正确");
                return results;
            }

            JsonNode diffArray = dataNode.path("diff");
            for (JsonNode item : diffArray) {
                MarketContext.IndexData indexData = new MarketContext.IndexData();

                // 解析字段（东方财富字段映射）
                indexData.setIndexCode(getStringValue(item, "f12"));      // 代码
                indexData.setIndexName(getStringValue(item, "f14"));      // 名称
                indexData.setCurrentValue(getDecimalValue(item, "f2"));   // 最新价
                indexData.setChangePercent(getDecimalValue(item, "f3"));  // 涨跌幅
                indexData.setChange(getDecimalValue(item, "f4"));         // 涨跌额
                indexData.setVolume(getLongValue(item, "f5"));            // 成交量
                indexData.setTurnover(getDecimalValue(item, "f6"));       // 成交额
                indexData.setOpen(getDecimalValue(item, "f17"));          // 开盘价
                indexData.setHigh(getDecimalValue(item, "f15"));          // 最高价
                indexData.setLow(getDecimalValue(item, "f16"));           // 最低价
                indexData.setPreviousClose(getDecimalValue(item, "f18")); // 昨收

                results.add(indexData);
            }
        } catch (Exception e) {
            log.error("解析指数数据失败", e);
        }

        return results;
    }

    private StockRealTimeData parseStockData(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.path("data");

            if (dataNode.isMissingNode()) {
                return null;
            }

            StockRealTimeData stockData = new StockRealTimeData();
            stockData.setStockCode(getStringValue(dataNode, "f57"));
            stockData.setStockName(getStringValue(dataNode, "f58"));
            stockData.setCurrentPrice(getDecimalValue(dataNode, "f43"));
            stockData.setChangePercent(getDecimalValue(dataNode, "f170"));
            stockData.setChange(getDecimalValue(dataNode, "f169"));
            stockData.setOpen(getDecimalValue(dataNode, "f46"));
            stockData.setHigh(getDecimalValue(dataNode, "f44"));
            stockData.setLow(getDecimalValue(dataNode, "f45"));
            stockData.setPreviousClose(getDecimalValue(dataNode, "f60"));
            stockData.setVolume(getLongValue(dataNode, "f47"));
            stockData.setTurnover(getDecimalValue(dataNode, "f48"));
            stockData.setMarketCap(getDecimalValue(dataNode, "f116"));

            return stockData;
        } catch (Exception e) {
            log.error("解析股票数据失败", e);
            return null;
        }
    }

    private Map<String, BigDecimal> parseSectorData(String response) {
        Map<String, BigDecimal> result = new HashMap<>();

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.path("data");

            if (dataNode.isMissingNode() || !dataNode.has("diff")) {
                return result;
            }

            JsonNode diffArray = dataNode.path("diff");
            for (JsonNode item : diffArray) {
                String name = getStringValue(item, "f14");
                BigDecimal changePercent = getDecimalValue(item, "f3");
                if (name != null && changePercent != null) {
                    result.put(name, changePercent);
                }
            }
        } catch (Exception e) {
            log.error("解析行业板块数据失败", e);
        }

        return result;
    }

    private Map<String, BigDecimal> parseConceptData(String response) {
        // 概念板块解析与行业板块相同
        return parseSectorData(response);
    }

    private FundFlowData parseFundFlowData(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.path("data");

            if (dataNode.isMissingNode() || !dataNode.has("diff")) {
                return null;
            }

            FundFlowData fundFlow = new FundFlowData();
            JsonNode diffArray = dataNode.path("diff");

            // 解析主力资金流向
            for (JsonNode item : diffArray) {
                String code = getStringValue(item, "f12");
                if ("000001".equals(code)) {
                    // 上证指数主力资金
                    fundFlow.setMainForceInflow(getDecimalValue(item, "f20"));
                    fundFlow.setLargeOrderInflow(getDecimalValue(item, "f21"));
                }
            }

            fundFlow.setTrend(fundFlow.getMainForceInflow() != null &&
                    fundFlow.getMainForceInflow().compareTo(BigDecimal.ZERO) > 0 ? "INFLOW" : "OUTFLOW");

            return fundFlow;
        } catch (Exception e) {
            log.error("解析资金流向数据失败", e);
            return null;
        }
    }

    private NorthboundFlowData parseNorthboundData(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode dataNode = root.path("data");

            if (dataNode.isMissingNode()) {
                return null;
            }

            NorthboundFlowData northbound = new NorthboundFlowData();

            // 解析当日北向资金数据
            JsonNode s2nNode = dataNode.path("s2n");
            if (!s2nNode.isMissingNode() && s2nNode.isArray() && s2nNode.size() > 0) {
                JsonNode latest = s2nNode.get(s2nNode.size() - 1);
                // 解析沪股通和深股通净流入
                northbound.setShanghaiInflow(getDecimalValue(latest, "f51"));
                northbound.setShenzhenInflow(getDecimalValue(latest, "f52"));
            }

            // 计算总流入
            if (northbound.getShanghaiInflow() != null && northbound.getShenzhenInflow() != null) {
                northbound.setTotalInflow(northbound.getShanghaiInflow().add(northbound.getShenzhenInflow()));
            }

            northbound.setTrend(northbound.getTotalInflow() != null &&
                    northbound.getTotalInflow().compareTo(BigDecimal.ZERO) > 0 ? "INFLOW" : "OUTFLOW");

            return northbound;
        } catch (Exception e) {
            log.error("解析北向资金数据失败", e);
            return null;
        }
    }

    // ================ 私有方法：工具方法 ================

    private String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .header("Referer", "https://quote.eastmoney.com/")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }
            return new String(response.body().bytes(), StandardCharsets.UTF_8);
        }
    }

    private String convertToEastMoneyCode(String code) {
        // 转换标准代码到东方财富格式
        // sh000001 -> 1.000001
        // sz399001 -> 0.399001
        if (code == null || code.length() < 8) {
            return code;
        }

        if (code.startsWith("sh")) {
            return "1." + code.substring(2);
        } else if (code.startsWith("sz")) {
            return "0." + code.substring(2);
        }
        return code;
    }

    private String convertFromEastMoneyCode(String code) {
        // 转换东方财富格式到标准代码
        // 1.000001 -> sh000001
        // 0.399001 -> sz399001
        if (code == null || !code.contains(".")) {
            return code;
        }

        String[] parts = code.split("\\.");
        if (parts.length != 2) {
            return code;
        }

        if ("1".equals(parts[0])) {
            return "sh" + parts[1];
        } else if ("0".equals(parts[0])) {
            return "sz" + parts[1];
        }
        return code;
    }

    private String getStringValue(JsonNode node, String field) {
        JsonNode valueNode = node.path(field);
        return valueNode.isMissingNode() || valueNode.isNull() ? null : valueNode.asText();
    }

    private BigDecimal getDecimalValue(JsonNode node, String field) {
        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        try {
            // 东方财富数据需要除以100
            return new BigDecimal(valueNode.asText()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Long getLongValue(JsonNode node, String field) {
        JsonNode valueNode = node.path(field);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }
        try {
            return valueNode.asLong();
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 转换StockRealTimeData为IndexData
     */
    public MarketContext.IndexData convertToIndexData(StockRealTimeData stockData) {
        if (stockData == null) {
            return null;
        }

        MarketContext.IndexData indexData = new MarketContext.IndexData();
        indexData.setIndexCode(stockData.getStockCode());
        indexData.setIndexName(stockData.getStockName());
        indexData.setCurrentValue(stockData.getCurrentPrice());
        indexData.setChange(stockData.getChange());
        indexData.setChangePercent(stockData.getChangePercent());
        indexData.setOpen(stockData.getOpen());
        indexData.setHigh(stockData.getHigh());
        indexData.setLow(stockData.getLow());
        indexData.setPreviousClose(stockData.getPreviousClose());
        indexData.setVolume(stockData.getVolume());
        indexData.setTurnover(stockData.getTurnover());

        return indexData;
    }
}
