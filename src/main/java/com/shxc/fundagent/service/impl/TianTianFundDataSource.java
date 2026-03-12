package com.shxc.fundagent.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shxc.fundagent.dto.external.FundBasicInfoDTO;
import com.shxc.fundagent.dto.external.FundHistoryDataDTO;
import com.shxc.fundagent.dto.external.FundRealTimeDataDTO;
import com.shxc.fundagent.service.FundDataSource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 天天基金网数据源实现
 * 使用天天基金网公开API获取基金数据
 */
@Component
@Slf4j
public class TianTianFundDataSource implements FundDataSource {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;

    // 配置参数
    @Value("${fund.data.tiantian.base-url:https://fundgz.1234567.com.cn}")
    private String baseUrl;

    @Value("${fund.data.tiantian.history-base-url:https://fundf10.eastmoney.com}")
    private String historyBaseUrl;

    @Value("${fund.data.tiantian.info-base-url:https://fund.eastmoney.com}")
    private String infoBaseUrl;

    @Value("${fund.data.tiantian.timeout:10000}")
    private int timeout;

    @Value("${fund.data.tiantian.rate-limit-per-minute:60}")
    private int rateLimitPerMinute;

    // 状态跟踪
    private final AtomicInteger requestCount = new AtomicInteger(0);
    private final ConcurrentHashMap<String, LocalDateTime> lastRequestTime = new ConcurrentHashMap<>();
    private String lastError;
    private LocalDateTime lastHealthCheckTime;

    public TianTianFundDataSource(OkHttpClient okHttpClient, ObjectMapper objectMapper) {
        this.okHttpClient = okHttpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getSourceName() {
        return "TianTianFund";
    }

    @Override
    public FundRealTimeDataDTO fetchRealTimeData(String fundCode) {
        try {
            checkRateLimit(fundCode);

            // 构建API URL
            String url = String.format("%s/js/%s.js", baseUrl, fundCode);
            long timestamp = System.currentTimeMillis();
            URI uri = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("rt", timestamp)
                    .build()
                    .toUri();

            log.debug("Fetching real-time data for fund {} from {}", fundCode, uri);

            Request request = new Request.Builder()
                    .url(uri.toString())
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().string();
                    // 解析jsonpgz({...})格式
                    if (body.startsWith("jsonpgz(") && body.endsWith(");")) {
                        String json = body.substring(8, body.length() - 2);
                        FundRealTimeDataDTO data = objectMapper.readValue(json, FundRealTimeDataDTO.class);
                        if (!data.getNetValueDateAsLocalDate().equals(LocalDate.now())) {
                            // 如果净值日期是当天，则保留单位净值, 如果不是只保留估值信息
                            data.setNetValue(null);
                        }
                        data.setNetValue(null);
                        if (data.isValid()) {
                            log.info("Successfully fetched real-time data for fund {}", fundCode);
                            return data;
                        } else {
                            log.warn("Invalid real-time data received for fund {}", fundCode);
                            lastError = "Invalid data format";
                        }
                    } else {
                        log.warn("Unexpected response format for fund {}", fundCode);
                        lastError = "Unexpected response format";
                    }
                } else {
                    log.warn("Failed to fetch real-time data for fund {}, status: {}",
                            fundCode, response.code());
                    lastError = "HTTP status: " + response.code();
                }
            }
        } catch (Exception e) {
            log.error("Error fetching real-time data for fund {}", fundCode, e);
            lastError = e.getMessage();
        } finally {
            recordRequest(fundCode);
        }

        return null;
    }

    @Override
    public List<FundRealTimeDataDTO> batchFetchRealTimeData(List<String> fundCodes) {
        List<FundRealTimeDataDTO> results = new ArrayList<>();
        for (String fundCode : fundCodes) {
            try {
                FundRealTimeDataDTO data = fetchRealTimeData(fundCode);
                results.add(data);
                // 避免请求过于频繁
                Thread.sleep(100);
            } catch (Exception e) {
                log.warn("Failed to fetch real-time data for fund {} in batch", fundCode, e);
                results.add(null);
            }
        }
        return results;
    }

    @Override
    public FundHistoryDataDTO fetchHistoryData(String fundCode, int days) {
        try {
            checkRateLimit(fundCode);

            // 构建API URL
            String url = String.format("%s/F10DataApi.aspx", historyBaseUrl);
            URI uri = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("type", "lsjz")
                    .queryParam("code", fundCode)
                    .queryParam("page", 1)
                    .queryParam("per", days > 0 ? days : 10000) // 默认获取所有数据
                    .queryParam("sdate", "")
                    .queryParam("edate", "")
                    .build()
                    .toUri();

            log.debug("Fetching history data for fund {} from {}", fundCode, uri);

            Request request = new Request.Builder()
                    .url(uri.toString())
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    // 天天基金网历史数据API返回的是包含在特定格式中的JSON
                    String body = response.body().string();
                    // 这里需要根据实际返回格式进行解析
                    // 由于格式较复杂，这里简化处理
                    List<FundHistoryDataDTO.HistoryDataItem> items =
                            parseHtmlDataToDTO(body.substring(12, body.length() - 1)
                                    .replaceAll("([{, ])(\\w+)(:)", "$1\"$2\"$3"));
                    // 实际实现需要解析具体的返回格式
                    FundHistoryDataDTO data = new FundHistoryDataDTO();
                    data.setFundCode(fundCode);
                    data.setData(items);
                    log.info("Successfully fetched history data for fund {} ({} days)",
                            fundCode, days);
                    return data;
                } else {
                    log.warn("Failed to fetch history data for fund {}, status: {}",
                            fundCode, response.code());
                    lastError = "HTTP status: " + response.code();
                }
            }
        } catch (Exception e) {
            log.error("Error fetching history data for fund {}", fundCode, e);
            lastError = e.getMessage();
        } finally {
            recordRequest(fundCode);
        }

        return null;
    }

    @Override
    public FundHistoryDataDTO fetchHistoryData(String fundCode, LocalDate sDate, LocalDate eDate) {
        try {
            checkRateLimit(fundCode);

            // 构建API URL
            String url = String.format("%s/F10DataApi.aspx", historyBaseUrl);
            URI uri = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("type", "lsjz")
                    .queryParam("code", fundCode)
                    .queryParam("page", 1)
                    .queryParam("per", 10000) // 默认获取所有数据
                    .queryParam("sdate", sDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .queryParam("edate", eDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                    .build()
                    .toUri();

            log.debug("Fetching history data for fund {} from {}", fundCode, uri);

            Request request = new Request.Builder()
                    .url(uri.toString())
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    // 天天基金网历史数据API返回的是包含在特定格式中的JSON
                    String body = response.body().string();
                    // 这里需要根据实际返回格式进行解析
                    // 由于格式较复杂，这里简化处理
                    List<FundHistoryDataDTO.HistoryDataItem> items = parseHtmlDataToDTO(body.substring(14));
                    // 实际实现需要解析具体的返回格式
                    FundHistoryDataDTO data = new FundHistoryDataDTO();
                    data.setFundCode(fundCode);
                    data.setData(items);
                    log.info("Successfully fetched history data for fund {}",
                            fundCode);
                    return data;
                } else {
                    log.warn("Failed to fetch history data for fund {}, status: {}",
                            fundCode, response.code());
                    lastError = "HTTP status: " + response.code();
                }
            }
        } catch (Exception e) {
            log.error("Error fetching history data for fund {}", fundCode, e);
            lastError = e.getMessage();
        } finally {
            recordRequest(fundCode);
        }

        return null;
    }

    @Override
    public FundBasicInfoDTO fetchFundBasicInfo(String fundCode) {
        try {
            checkRateLimit(fundCode);

            // 构建API URL
            String url = String.format("%s/pingzhongdata/%s.js", infoBaseUrl, fundCode);
            URI uri = URI.create(url);

            log.debug("Fetching basic info for fund {} from {}", fundCode, uri);

            Request request = new Request.Builder()
                    .url(uri.toString())
                    .build();

            try (Response response = okHttpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    // 天天基金网基金详情API返回的是JavaScript文件，包含基金信息
                    // 这里需要解析JavaScript中的变量
                    String body = response.body().string();
                    FundBasicInfoDTO info = parseFundInfoFromJs(body, fundCode);

                    if (info != null && info.isValid()) {
                        log.info("Successfully fetched basic info for fund {}", fundCode);
                        return info;
                    } else {
                        log.warn("Invalid basic info received for fund {}", fundCode);
                        lastError = "Invalid data format";
                    }
                } else {
                    log.warn("Failed to fetch basic info for fund {}, status: {}",
                            fundCode, response.code());
                    lastError = "HTTP status: " + response.code();
                }
            }
        } catch (Exception e) {
            log.error("Error fetching basic info for fund {}", fundCode, e);
            lastError = e.getMessage();
        } finally {
            recordRequest(fundCode);
        }

        return null;
    }

    @Override
    public List<FundBasicInfoDTO> batchFetchFundBasicInfo(List<String> fundCodes) {
        List<FundBasicInfoDTO> results = new ArrayList<>();
        for (String fundCode : fundCodes) {
            try {
                FundBasicInfoDTO info = fetchFundBasicInfo(fundCode);
                results.add(info);
                // 避免请求过于频繁
                Thread.sleep(150);
            } catch (Exception e) {
                log.warn("Failed to fetch basic info for fund {} in batch", fundCode, e);
                results.add(null);
            }
        }
        return results;
    }

    @Override
    public boolean isAvailable() {
        // 简单的健康检查：尝试获取一个常见基金的实时数据
        try {
            String testFundCode = "000001"; // 华夏成长混合
            FundRealTimeDataDTO data = fetchRealTimeData(testFundCode);
            lastHealthCheckTime = LocalDateTime.now();
            return data != null;
        } catch (Exception e) {
            log.warn("Health check failed for TianTianFund data source", e);
            lastError = "Health check failed: " + e.getMessage();
            return false;
        }
    }

    @Override
    public String getHealthStatus() {
        if (lastHealthCheckTime == null) {
            return "未检查";
        }

        long minutesSinceLastCheck = java.time.Duration.between(
                lastHealthCheckTime, LocalDateTime.now()).toMinutes();

        if (minutesSinceLastCheck > 5) {
            return "需要重新检查";
        }

        try {
            return isAvailable() ? "健康" : "异常";
        } catch (Exception e) {
            return "检查失败";
        }
    }

    @Override
    public String getLastError() {
        return lastError;
    }

    @Override
    public List<String> getSupportedFundTypes() {
        return List.of("混合型", "股票型", "指数型", "债券型", "货币型", "QDII");
    }

    @Override
    public int getRateLimitPerMinute() {
        return rateLimitPerMinute;
    }

    @Override
    public boolean validateFundCode(String fundCode) {
        // 简单的基金代码验证：6位数字
        return fundCode != null && fundCode.matches("\\d{6}");
    }

    @Override
    public int getPriority() {
        return 1; // 最高优先级
    }

    @Override
    public LocalDate getLastUpdateTime(String fundCode) {
        // 默认返回今天
        return LocalDate.now();
    }

    /**
     * 检查请求频率限制
     */
    private void checkRateLimit(String fundCode) {
        LocalDateTime lastRequest = lastRequestTime.get(fundCode);
        if (lastRequest != null) {
            long secondsSinceLastRequest = java.time.Duration.between(
                    lastRequest, LocalDateTime.now()).getSeconds();
            if (secondsSinceLastRequest < 1) {
                // 至少1秒间隔
                try {
                    Thread.sleep(1000 - secondsSinceLastRequest * 1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * 记录请求
     */
    private void recordRequest(String fundCode) {
        lastRequestTime.put(fundCode, LocalDateTime.now());
        requestCount.incrementAndGet();
    }

    /**
     * 从JavaScript文件中解析基金信息
     */
    private FundBasicInfoDTO parseFundInfoFromJs(String jsContent, String fundCode) {
        try {
            FundBasicInfoDTO info = new FundBasicInfoDTO();
            info.setFundCode(fundCode);

            // 简化实现：实际需要解析JavaScript中的特定变量
            // 这里只是示例，实际实现需要根据实际返回格式编写解析逻辑

            // 示例：从JavaScript中提取基金名称
            String namePattern = "fS_name = \"([^\"]+)\"";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(namePattern);
            java.util.regex.Matcher matcher = pattern.matcher(jsContent);
            if (matcher.find()) {
                info.setName(matcher.group(1));
            }

            // 示例：从JavaScript中提取基金类型
            String typePattern = "fS_type = \"([^\"]+)\"";
            pattern = java.util.regex.Pattern.compile(typePattern);
            matcher = pattern.matcher(jsContent);
            if (matcher.find()) {
                info.setFundType(matcher.group(1));
            }

            return info;
        } catch (Exception e) {
            log.error("Error parsing fund info from JS for fund {}", fundCode, e);
            return null;
        }
    }

    /**
     * 获取请求统计
     */
    public int getRequestCount() {
        return requestCount.get();
    }

    /**
     * 重置请求统计
     */
    public void resetRequestCount() {
        requestCount.set(0);
        lastRequestTime.clear();
    }

    private List<FundHistoryDataDTO.HistoryDataItem> parseHtmlDataToDTO(String jsonString) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(jsonString);
        String htmlContent = root.get("content").asText();

        // 2. 解析 HTML 表格
        Document doc = Jsoup.parse(htmlContent);
        Element table = doc.selectFirst("table.lsjz");
        if (table == null) {
            throw new RuntimeException("未找到净值表格");
        }

        Elements rows = table.select("tbody tr");
        List<FundHistoryDataDTO.HistoryDataItem> navList = new ArrayList<>(rows.size());

        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() < 6) continue; // 忽略无效行

            // 提取数据（注意移除 HTML 标签、多余空格）
            String dateStr = cols.get(0).text().trim();
            String unitNavStr = cols.get(1).text().trim();
            String accNavStr = cols.get(2).text().trim();
            String dailyGrowthStr = cols.get(3).text().trim(); // 包含正负号和百分号
            String purchaseStatusStr = cols.get(4).text().trim();
            String redemptionStatusStr = cols.get(5).text().trim();
            String dividendStr = cols.get(6).text().trim();
            dailyGrowthStr = dailyGrowthStr.replace("%", "");

            // 转换为所需类型
            LocalDate navDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            FundHistoryDataDTO.HistoryDataItem historyDataItem = new FundHistoryDataDTO.HistoryDataItem();
            // 保证格式相同
            historyDataItem.setDate(navDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            historyDataItem.setNetValue(unitNavStr);
            historyDataItem.setAccumulatedNetValue(accNavStr);
            historyDataItem.setDailyGrowthRate(dailyGrowthStr);
            historyDataItem.setPurchaseStatus(purchaseStatusStr);
            historyDataItem.setRedemptionStatus(redemptionStatusStr);
            historyDataItem.setDividend(dividendStr);

            navList.add(historyDataItem);
        }

        return navList;
    }

}