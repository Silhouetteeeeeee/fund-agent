package com.shxc.fundagent.service.impl;

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
 * 腾讯财经市场数据服务实现
 * 使用腾讯财经接口获取实时市场数据
 * 接口格式：http://qt.gtimg.cn/q=sh000001,sz399001
 */
@Slf4j
@Service
public class TencentMarketDataService implements MarketDataService {

    private static final String TENCENT_API_URL = "http://qt.gtimg.cn/q=";
    private static final String DATA_SOURCE_NAME = "腾讯财经";

    // 主要市场指数代码映射
    private static final Map<String, String> MAJOR_INDICES = new LinkedHashMap<>();

    static {
        MAJOR_INDICES.put("sh000001", "上证指数");
        MAJOR_INDICES.put("sz399001", "深证成指");
        MAJOR_INDICES.put("sz399006", "创业板指");
        MAJOR_INDICES.put("sh000300", "沪深300");
        MAJOR_INDICES.put("sh000016", "上证50");
        MAJOR_INDICES.put("sh000905", "中证500");
        MAJOR_INDICES.put("sz399005", "中小板指");
        MAJOR_INDICES.put("sh000688", "科创50");
    }

    private final OkHttpClient httpClient;

    public TencentMarketDataService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    @Override
    public MarketContext.IndexData getIndexRealTimeData(String indexCode) {
        try {
            List<MarketContext.IndexData> results = batchGetIndexRealTimeData(Collections.singletonList(indexCode));
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

        String codes = String.join(",", indexCodes);
        String url = TENCENT_API_URL + codes;

        try {
            String response = fetchData(url);
            return parseIndexData(response, indexCodes);
        } catch (Exception e) {
            log.error("批量获取指数数据失败: {}", codes, e);
            return Collections.emptyList();
        }
    }

    @Override
    public List<MarketContext.IndexData> getMajorIndicesData() {
        List<String> codes = new ArrayList<>(MAJOR_INDICES.keySet());
        return batchGetIndexRealTimeData(codes);
    }

    @Override
    public List<IndexHistoryData> getIndexHistoryData(String indexCode, LocalDate startDate, LocalDate endDate) {
        // 腾讯财经接口不直接支持历史数据，需要接入其他数据源
        // 这里返回空列表，建议后续接入Tushare或东方财富
        log.warn("腾讯财经暂不支持历史数据查询，建议接入Tushare Pro");
        return Collections.emptyList();
    }

    @Override
    public Map<String, BigDecimal> getSectorPerformance() {
        // 腾讯财经需要通过板块代码获取，这里使用预设的行业龙头股票模拟
        Map<String, String> sectorLeaders = new HashMap<>();
        sectorLeaders.put("科技", "sh600519,sz000858"); // 茅台、五粮液代表消费
        sectorLeaders.put("金融", "sh601398,sh601318"); // 工行、平安
        sectorLeaders.put("医药", "sh600276,sh603259"); // 恒瑞、药明康德
        sectorLeaders.put("能源", "sh601857,sh600028"); // 中石油、中石化

        Map<String, BigDecimal> result = new HashMap<>();
        for (Map.Entry<String, String> entry : sectorLeaders.entrySet()) {
            BigDecimal avgChange = calculateSectorChange(entry.getValue());
            result.put(entry.getKey(), avgChange);
        }
        return result;
    }

    @Override
    public Map<String, BigDecimal> getConceptPerformance() {
        // 概念板块数据需要接入其他数据源
        log.warn("腾讯财经概念板块数据有限");
        return Collections.emptyMap();
    }

    @Override
    public FundFlowData getMarketFundFlow() {
        // 腾讯财经不直接提供资金流向数据，需要接入其他数据源
        log.warn("腾讯财经暂不支持资金流向数据，建议接入东方财富");
        return null;
    }

    @Override
    public NorthboundFlowData getNorthboundFlow() {
        // 北向资金数据需要接入其他数据源
        log.warn("腾讯财经暂不支持北向资金数据");
        return null;
    }

    @Override
    public MarketSentimentData getMarketSentiment() {
        // 使用公共的情绪计算器
        List<MarketContext.IndexData> indices = getMajorIndicesData();

        // 腾讯财经不支持北向资金数据
        NorthboundFlowData northboundFlow = null;

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
        // 估值数据需要接入其他数据源（如Tushare）
        log.warn("腾讯财经暂不支持估值数据查询");
        return null;
    }

    @Override
    public StockRealTimeData getStockRealTimeData(String stockCode) {
        List<StockRealTimeData> results = batchGetStockRealTimeData(Collections.singletonList(stockCode));
        return results.isEmpty() ? null : results.get(0);
    }

    @Override
    public List<StockRealTimeData> batchGetStockRealTimeData(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        String codes = String.join(",", stockCodes);
        String url = TENCENT_API_URL + codes;

        try {
            String response = fetchData(url);
            return parseStockData(response, stockCodes);
        } catch (Exception e) {
            log.error("批量获取股票数据失败: {}", codes, e);
            return Collections.emptyList();
        }
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

    // ================ 私有方法 ================

    /**
     * 从URL获取数据
     */
    private String fetchData(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected response code: " + response);
            }
            return new String(response.body().bytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 解析指数数据
     * 腾讯返回格式：v_sh000001="1~上证指数~3200.50~...";
     */
    private List<MarketContext.IndexData> parseIndexData(String response, List<String> indexCodes) {
        List<MarketContext.IndexData> results = new ArrayList<>();

        String[] lines = response.split(";");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            try {
                // 提取代码和数据
                int start = line.indexOf('"');
                int end = line.lastIndexOf('"');
                if (start == -1 || end == -1 || start >= end) continue;

                String data = line.substring(start + 1, end);
                String[] fields = data.split("~");

                if (fields.length < 45) {
                    log.warn("数据字段不足: {}", line);
                    continue;
                }

                MarketContext.IndexData indexData = new MarketContext.IndexData();
                indexData.setIndexCode(fields[2]);      // 代码
                indexData.setIndexName(fields[1]);      // 名称
                indexData.setCurrentValue(parseDecimal(fields[3]));     // 当前价格
                indexData.setPreviousClose(parseDecimal(fields[4]));    // 昨收
                indexData.setOpen(parseDecimal(fields[5]));             // 开盘
                indexData.setHigh(parseDecimal(fields[6]));             // 最高
                indexData.setLow(parseDecimal(fields[7]));              // 最低

                // 计算涨跌幅
                BigDecimal current = indexData.getCurrentValue();
                BigDecimal previous = indexData.getPreviousClose();
                if (current != null && previous != null && previous.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal change = current.subtract(previous);
                    BigDecimal changePercent = change.divide(previous, 4, RoundingMode.HALF_UP);
                    indexData.setChange(change);
                    indexData.setChangePercent(changePercent);
                }

                // 成交量和成交额
                indexData.setVolume(parseLong(fields[36]));     // 成交量（手）
                indexData.setTurnover(parseDecimal(fields[37])); // 成交额（万）

                results.add(indexData);

            } catch (Exception e) {
                log.error("解析指数数据失败: {}", line, e);
            }
        }

        return results;
    }

    /**
     * 解析股票数据
     * 格式与指数类似
     */
    private List<StockRealTimeData> parseStockData(String response, List<String> stockCodes) {
        List<StockRealTimeData> results = new ArrayList<>();

        String[] lines = response.split(";");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            try {
                int start = line.indexOf('"');
                int end = line.lastIndexOf('"');
                if (start == -1 || end == -1 || start >= end) continue;

                String data = line.substring(start + 1, end);
                String[] fields = data.split("~");

                if (fields.length < 45) continue;

                StockRealTimeData stockData = new StockRealTimeData();
                stockData.setStockCode(fields[2]);
                stockData.setStockName(fields[1]);
                stockData.setCurrentPrice(parseDecimal(fields[3]));
                stockData.setPreviousClose(parseDecimal(fields[4]));
                stockData.setOpen(parseDecimal(fields[5]));
                stockData.setHigh(parseDecimal(fields[33]));
                stockData.setLow(parseDecimal(fields[34]));
                stockData.setVolume(parseLong(fields[36]));
                stockData.setTurnover(parseDecimal(fields[37]));
                stockData.setMarketCap(parseDecimal(fields[44]));

                // 计算涨跌幅
                BigDecimal current = stockData.getCurrentPrice();
                BigDecimal previous = stockData.getPreviousClose();
                if (current != null && previous != null && previous.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal change = current.subtract(previous);
                    BigDecimal changePercent = change.divide(previous, 4, RoundingMode.HALF_UP);
                    stockData.setChange(change);
                    stockData.setChangePercent(changePercent);
                }

                results.add(stockData);

            } catch (Exception e) {
                log.error("解析股票数据失败: {}", line, e);
            }
        }

        return results;
    }

    /**
     * 计算板块涨跌幅（基于成分股平均）
     */
    private BigDecimal calculateSectorChange(String stockCodes) {
        String[] codes = stockCodes.split(",");
        List<StockRealTimeData> stocks = batchGetStockRealTimeData(Arrays.asList(codes));

        if (stocks.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return stocks.stream()
                .map(StockRealTimeData::getChangePercent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(new BigDecimal(stocks.size()), 4, RoundingMode.HALF_UP);
    }

    /**
     * 解析BigDecimal
     */
    private BigDecimal parseDecimal(String value) {
        if (value == null || value.isEmpty() || "-".equals(value)) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 解析Long
     */
    private Long parseLong(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 转换StockRealTimeData为IndexData
     * 用于行业/概念板块数据转换
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
