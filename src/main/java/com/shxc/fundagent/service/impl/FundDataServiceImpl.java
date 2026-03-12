package com.shxc.fundagent.service.impl;

import com.shxc.fundagent.dto.external.FundBasicInfoDTO;
import com.shxc.fundagent.dto.external.FundHistoryDataDTO;
import com.shxc.fundagent.dto.external.FundRealTimeDataDTO;
import com.shxc.fundagent.entity.FundDailyData;
import com.shxc.fundagent.entity.FundInfo;
import com.shxc.fundagent.enums.FundType;
import com.shxc.fundagent.repository.FundDailyDataRepository;
import com.shxc.fundagent.repository.FundInfoRepository;
import com.shxc.fundagent.service.FundDataService;
import com.shxc.fundagent.service.FundDataSource;
import com.shxc.fundagent.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.DayOfWeek;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基金数据服务实现类
 */
@Service
@Slf4j
public class FundDataServiceImpl implements FundDataService {

    private final FundInfoRepository fundInfoRepository;
    private final FundDailyDataRepository fundDailyDataRepository;
    private final List<FundDataSource> dataSources;
    private final TianTianFundDataSource tianTianFundDataSource; // 主数据源
    private final TransactionService transactionService;

    @Autowired
    public FundDataServiceImpl(FundInfoRepository fundInfoRepository,
                               FundDailyDataRepository fundDailyDataRepository,
                               List<FundDataSource> dataSources,
                               TianTianFundDataSource tianTianFundDataSource, TransactionService transactionService) {
        this.fundInfoRepository = fundInfoRepository;
        this.fundDailyDataRepository = fundDailyDataRepository;
        this.dataSources = dataSources;
        this.tianTianFundDataSource = tianTianFundDataSource;
        this.transactionService = transactionService;
        log.info("FundDataService initialized with {} data sources", dataSources.size());
    }

    @Override
    public FundDailyData getRealTimeData(String fundCode) {
        log.debug("Getting real-time data for fund: {}", fundCode);

        // 1. 尝试从数据库获取当天的实时数据
        LocalDate today = LocalDate.now();
        Optional<FundDailyData> existingData = fundDailyDataRepository
                .findByFundCodeAndTradeDate(fundCode, today);

        if (existingData.isPresent()) {
            FundDailyData data = existingData.get();
            // 检查数据是否足够新鲜（1小时内）
            if (data.getCreateTime() != null &&
                    data.getCreateTime().isAfter(LocalDateTime.now().minusHours(1))) {
                log.debug("Returning cached real-time data for fund: {}", fundCode);
                return data;
            }
        }

        // 2. 根据当前时间和交易日情况决定数据获取策略
        FundRealTimeDataDTO realTimeData = null;

        if (shouldUseOfficialNetValue()) {
            log.debug("Current time suggests using official net value for fund: {}", fundCode);
            // 优先尝试获取当天的官方净值
            realTimeData = tryGetOfficialNetValue(fundCode, today);

            if (realTimeData != null && realTimeData.isValid()) {
                log.info("Successfully fetched official net value for fund: {}", fundCode);
            } else {
                // 如果无法获取官方净值，回退到实时估值
                log.debug("Failed to get official net value, falling back to real-time estimate for fund: {}", fundCode);
                realTimeData = fetchFromDataSources(fundCode, "REAL_TIME");
            }
        } else {
            // 交易时间内，使用实时估值
            log.debug("Current time suggests using real-time estimate for fund: {}", fundCode);
            realTimeData = fetchFromDataSources(fundCode, "REAL_TIME");
        }

        if (realTimeData != null && realTimeData.isValid()) {
            // 3. 转换并保存到数据库
            FundDailyData dailyData = convertToFundDailyData(realTimeData);
            FundDailyData savedData = saveDailyData(dailyData);
            log.info("Successfully fetched and saved real-time data for fund: {}", fundCode);
            return savedData;
        }

        log.warn("Failed to get real-time data for fund: {}", fundCode);
        return existingData.orElse(null);
    }

    @Override
    public List<FundDailyData> batchGetRealTimeData(List<String> fundCodes) {
        log.debug("Batch getting real-time data for {} funds", fundCodes.size());
        List<FundDailyData> results = new ArrayList<>();

        for (String fundCode : fundCodes) {
            try {
                FundDailyData data = getRealTimeData(fundCode);
                results.add(data);
            } catch (Exception e) {
                log.error("Error getting real-time data for fund: {}", fundCode, e);
                results.add(null);
            }
        }

        return results;
    }

    @Override
    public List<FundDailyData> getHistoryData(String fundCode, LocalDate sDate, LocalDate eDate) {
        if (sDate == null || eDate == null || sDate.isAfter(eDate)) {
            return null;
        }
        List<FundDailyData> existingData = fundDailyDataRepository
                .findByFundCodeAndTradeDateBetween(fundCode, sDate, eDate);

        // 2. 从数据源获取历史数据
        FundHistoryDataDTO historyData = fetchFromDataSources(fundCode, "HISTORY_RANGE", sDate, eDate);

        if (historyData != null && historyData.isValid()) {
            // 3. 转换并保存到数据库
            List<FundDailyData> dailyDataList = convertToFundDailyDataList(historyData);
            List<FundDailyData> savedDataList = saveDailyDataList(dailyDataList);
            log.info("Successfully fetched and saved history data for fund: {} ({} records)",
                    fundCode, savedDataList.size());
            return savedDataList;
        }
        return existingData;
    }

    @Override
    public List<FundDailyData> getHistoryData(String fundCode, int days) {
        log.debug("Getting {} days history data for fund: {}", days, fundCode);

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days);
        List<FundDailyData> existingData = fundDailyDataRepository
                .findByFundCodeAndTradeDateBetween(fundCode, startDate, endDate);
        // 2. 从数据源获取历史数据
        FundHistoryDataDTO historyData = fetchFromDataSources(fundCode, "HISTORY", days);

        if (historyData != null && historyData.isValid()) {
            // 3. 转换并保存到数据库
            List<FundDailyData> dailyDataList = convertToFundDailyDataList(historyData);
            List<FundDailyData> savedDataList = saveDailyDataList(dailyDataList);
            log.info("Successfully fetched and saved history data for fund: {} ({} records)",
                    fundCode, savedDataList.size());
            return savedDataList;
        }
        log.warn("Failed to get history data for fund: {}", fundCode);
        return existingData;
    }

    @Override
    public FundInfo getFundBasicInfo(String fundCode) {
        log.debug("Getting basic info for fund: {}", fundCode);

        // 1. 尝试从数据库获取
        Optional<FundInfo> existingInfo = fundInfoRepository.findByFundCode(fundCode);

        if (existingInfo.isPresent()) {
            FundInfo info = existingInfo.get();
            // 检查数据是否足够新鲜（7天内）
            if (info.getUpdateTime() != null &&
                    info.getUpdateTime().isAfter(LocalDateTime.now().minusDays(7))) {
                log.debug("Returning cached basic info for fund: {}", fundCode);
                return info;
            }
        }

        // 2. 从数据源获取最新信息
        FundBasicInfoDTO basicInfo = fetchFromDataSources(fundCode, "BASIC_INFO");

        if (basicInfo != null && basicInfo.isValid()) {
            // 3. 转换并保存到数据库
            FundInfo fundInfo = convertToFundInfo(basicInfo);
            FundInfo savedInfo = saveFundInfo(fundInfo);
            log.info("Successfully fetched and saved basic info for fund: {}", fundCode);
            return savedInfo;
        }

        log.warn("Failed to get basic info for fund: {}", fundCode);
        return existingInfo.orElse(null);
    }

    @Override
    @Transactional
    public FundInfo updateFundBasicInfo(String fundCode) {
        log.info("Updating basic info for fund: {}", fundCode);

        FundBasicInfoDTO basicInfo = fetchFromDataSources(fundCode, "BASIC_INFO");
        if (basicInfo == null || !basicInfo.isValid()) {
            throw new RuntimeException("Failed to fetch basic info for fund: " + fundCode);
        }

        FundInfo fundInfo = convertToFundInfo(basicInfo);
        return saveFundInfo(fundInfo);
    }

    @Override
    @Transactional
    public boolean syncFundData(String fundCode, LocalDate date) {
        log.info("Syncing fund data for fund: {} on date: {}", fundCode, date);

        try {
            // 1. 检查是否已存在数据
            if (fundDailyDataRepository.existsByFundCodeAndTradeDate(fundCode, date)) {
                log.debug("Data already exists for fund: {} on date: {}", fundCode, date);
                return true;
            }

            // 2. 获取数据
            FundRealTimeDataDTO realTimeData = fetchFromDataSources(fundCode, "REAL_TIME");

            if (realTimeData != null && realTimeData.isValid()) {
                // 3. 转换并保存
                FundDailyData dailyData = convertToFundDailyData(realTimeData);
                dailyData.setTradeDate(date);
                saveDailyData(dailyData);

                log.info("Successfully synced fund data for fund: {} on date: {}", fundCode, date);
                return true;
            }

            log.warn("No valid data to sync for fund: {} on date: {}", fundCode, date);
            return false;
        } catch (Exception e) {
            log.error("Error syncing fund data for fund: {} on date: {}", fundCode, date, e);
            return false;
        }
    }

    @Override
    @Transactional
    public int batchSyncFundData(List<String> fundCodes, LocalDate date) {
        log.info("Batch syncing fund data for {} funds on date: {}", fundCodes.size(), date);

        int successCount = 0;
        for (String fundCode : fundCodes) {
            try {
                if (syncFundData(fundCode, date)) {
                    successCount++;
                }
                // 避免请求过于频繁
                Thread.sleep(100);
            } catch (Exception e) {
                log.error("Error in batch sync for fund: {}", fundCode, e);
            }
        }

        log.info("Batch sync completed: {}/{} funds successful", successCount, fundCodes.size());
        return successCount;
    }

    @Override
    public LocalDate getLatestTradeDate() {
        return fundDailyDataRepository.findLatestTradeDate()
                .orElse(LocalDate.now().minusDays(1));
    }

    @Override
    public BigDecimal getCurrentPrice(String fundCode) {
        // 优先获取当天的实时数据
        FundDailyData latestData = getRealTimeData(fundCode);
        if (latestData != null && latestData.getEffectivePrice().compareTo(BigDecimal.ZERO) > 0) {
            return latestData.getEffectivePrice();
        }

        // 其次获取最新的历史数据
        Optional<FundDailyData> latestHistorical = fundDailyDataRepository
                .findLatestByFundCode(fundCode);
        if (latestHistorical.isPresent()) {
            return latestHistorical.get().getEffectivePrice();
        }

        log.warn("No price data available for fund: {}", fundCode);
        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getAbsoluteCurrentPrice(String fundCode) {
        LocalDate today = LocalDate.now();
        Optional<FundDailyData> existingData = fundDailyDataRepository
                .findByFundCodeAndTradeDate(fundCode, today);

        // 说明已经是实时的数据了
        if (existingData.isPresent() && existingData.get().getNetValue() != null) {
            return existingData.get().getNetValue();
        } else {
            // 尝试获取历史数据
            FundHistoryDataDTO historyData = fetchFromDataSources(fundCode, "HISTORY", 1);
            if (historyData != null && historyData.isValid()) {
                FundHistoryDataDTO.HistoryDataItem todayData = historyData.getData().get(0);
                BigDecimal netValue = todayData.getNetValueAsBigDecimal();
                return netValue;
            }
        }
        // 获取不到就返回null
        return null;
    }

    @Override
    public BigDecimal getDailyChangeRate(String fundCode) {
        FundDailyData latestData = getRealTimeData(fundCode);
        if (latestData != null && latestData.getChangeRate() != null) {
            return latestData.getChangeRate();
        }

        // 计算基于前一天的涨跌幅
        LocalDate today = LocalDate.now();
        Optional<FundDailyData> todayData = fundDailyDataRepository
                .findByFundCodeAndTradeDate(fundCode, today);
        Optional<FundDailyData> yesterdayData = fundDailyDataRepository
                .findByFundCodeAndTradeDate(fundCode, today.minusDays(1));

        if (todayData.isPresent() && yesterdayData.isPresent()) {
            BigDecimal todayPrice = todayData.get().getEffectivePrice();
            BigDecimal yesterdayPrice = yesterdayData.get().getEffectivePrice();

            if (yesterdayPrice.compareTo(BigDecimal.ZERO) > 0) {
                return todayPrice.subtract(yesterdayPrice)
                        .divide(yesterdayPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        return BigDecimal.ZERO;
    }

    // 其他方法的实现...

    @Override
    public BigDecimal getWeeklyChangeRate(String fundCode) {
        // 简化实现：计算过去7天的涨跌幅
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(7);

        List<FundDailyData> weeklyData = fundDailyDataRepository
                .findByFundCodeAndTradeDateBetween(fundCode, startDate, endDate);

        if (weeklyData.size() >= 2) {
            // 按日期排序
            weeklyData.sort(Comparator.comparing(FundDailyData::getTradeDate));

            FundDailyData firstData = weeklyData.get(0);
            FundDailyData lastData = weeklyData.get(weeklyData.size() - 1);

            BigDecimal firstPrice = firstData.getEffectivePrice();
            BigDecimal lastPrice = lastData.getEffectivePrice();

            if (firstPrice.compareTo(BigDecimal.ZERO) > 0) {
                return lastPrice.subtract(firstPrice)
                        .divide(firstPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        return BigDecimal.ZERO;
    }

    @Override
    public BigDecimal getMonthlyChangeRate(String fundCode) {
        // 简化实现：计算过去30天的涨跌幅
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(30);

        List<FundDailyData> monthlyData = fundDailyDataRepository
                .findByFundCodeAndTradeDateBetween(fundCode, startDate, endDate);

        if (monthlyData.size() >= 2) {
            // 按日期排序
            monthlyData.sort(Comparator.comparing(FundDailyData::getTradeDate));

            FundDailyData firstData = monthlyData.get(0);
            FundDailyData lastData = monthlyData.get(monthlyData.size() - 1);

            BigDecimal firstPrice = firstData.getEffectivePrice();
            BigDecimal lastPrice = lastData.getEffectivePrice();

            if (firstPrice.compareTo(BigDecimal.ZERO) > 0) {
                return lastPrice.subtract(firstPrice)
                        .divide(firstPrice, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        return BigDecimal.ZERO;
    }

    @Override
    public boolean isDataUpToDate(String fundCode) {
        Optional<FundDailyData> latestData = fundDailyDataRepository
                .findLatestByFundCode(fundCode);

        if (latestData.isPresent()) {
            LocalDate latestDate = latestData.get().getTradeDate();
            LocalDate today = LocalDate.now();

            // 如果是今天的数据，或者最近1个工作日内的数据，认为是最新的
            return latestDate.equals(today) ||
                    (latestDate.isAfter(today.minusDays(2)) && !isWeekend(today));
        }

        return false;
    }

    @Override
    public Map<String, Object> getDataQualityReport(String fundCode) {
        Map<String, Object> report = new HashMap<>();

        // 统计数据
        long totalRecords = fundDailyDataRepository.countByFundCode(fundCode);
        Optional<FundDailyData> latestRecord = fundDailyDataRepository
                .findLatestByFundCode(fundCode);

        report.put("fundCode", fundCode);
        report.put("totalRecords", totalRecords);
        report.put("hasLatestData", latestRecord.isPresent());

        if (latestRecord.isPresent()) {
            FundDailyData data = latestRecord.get();
            report.put("latestDate", data.getTradeDate());
            report.put("latestPrice", data.getEffectivePrice());
            report.put("dataQuality", data.getDataQualityDescription());
            report.put("isClosingData", data.isClosingData());
            report.put("isEstimateData", data.isEstimateData());
        }

        // 数据完整性评估
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now();
        List<FundDailyData> recentData = fundDailyDataRepository
                .findByFundCodeAndTradeDateBetween(fundCode, startDate, endDate);

        int expectedDays = calculateTradingDays(startDate, endDate);
        int actualDays = recentData.size();
        double completenessRate = expectedDays > 0 ? (double) actualDays / expectedDays : 0.0;

        report.put("recentDataDays", actualDays);
        report.put("expectedTradingDays", expectedDays);
        report.put("completenessRate", String.format("%.2f%%", completenessRate * 100));

        return report;
    }

    @Override
    @Transactional
    public int cleanUpExpiredData(LocalDate beforeDate) {
        log.info("Cleaning up expired data before: {}", beforeDate);
        return fundDailyDataRepository.deleteByTradeDateBefore(beforeDate);
    }

    @Override
    public Map<String, String> getDataSourceHealthStatus() {
        Map<String, String> healthStatus = new HashMap<>();

        for (FundDataSource dataSource : dataSources) {
            try {
                String status = dataSource.getHealthStatus();
                healthStatus.put(dataSource.getSourceName(), status);
            } catch (Exception e) {
                log.error("Error checking health status for data source: {}",
                        dataSource.getSourceName(), e);
                healthStatus.put(dataSource.getSourceName(), "检查失败");
            }
        }

        return healthStatus;
    }

    @Override
    public boolean triggerDataCollection(String fundCode, String dataType) {
        log.info("Triggering data collection for fund: {}, type: {}", fundCode, dataType);

        try {
            switch (dataType.toUpperCase()) {
                case "REAL_TIME":
                    FundRealTimeDataDTO realTimeData = fetchFromDataSources(fundCode, "REAL_TIME");
                    if (realTimeData != null && realTimeData.isValid()) {
                        FundDailyData dailyData = convertToFundDailyData(realTimeData);
                        saveDailyData(dailyData);
                        return true;
                    }
                    break;

                case "HISTORY":
                    FundHistoryDataDTO historyData = fetchFromDataSources(fundCode, "HISTORY", 30);
                    if (historyData != null && historyData.isValid()) {
                        List<FundDailyData> dailyDataList = convertToFundDailyDataList(historyData);
                        saveDailyDataList(dailyDataList);
                        return true;
                    }
                    break;

                case "BASIC_INFO":
                    FundBasicInfoDTO basicInfo = fetchFromDataSources(fundCode, "BASIC_INFO");
                    if (basicInfo != null && basicInfo.isValid()) {
                        FundInfo fundInfo = convertToFundInfo(basicInfo);
                        saveFundInfo(fundInfo);
                        return true;
                    }
                    break;

                default:
                    log.error("Unknown data type: {}", dataType);
                    return false;
            }
        } catch (Exception e) {
            log.error("Error triggering data collection for fund: {}, type: {}",
                    fundCode, dataType, e);
        }

        return false;
    }

    @Override
    public boolean validateFundCode(String fundCode) {
        // 使用主数据源验证
        return tianTianFundDataSource.validateFundCode(fundCode);
    }

    @Override
    public List<FundInfo> searchFunds(String keyword) {
        log.debug("Searching funds with keyword: {}", keyword);

        List<FundInfo> results = new ArrayList<>();

        // 1. 先按基金代码精确匹配
        if (keyword.matches("\\d{6}")) {
            fundInfoRepository.findByFundCode(keyword).ifPresent(results::add);
        }

        // 2. 按基金名称模糊匹配
        results.addAll(fundInfoRepository.findByFundNameContaining(keyword));

        // 3. 去重
        return results.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Object> getDataStatistics() {
        Map<String, Object> stats = new HashMap<>();

        // 基金数量统计
        long totalFunds = fundInfoRepository.count();
        long activeFunds = fundInfoRepository.findAllActiveFunds().size();

        // 数据记录统计
        long totalDataRecords = fundDailyDataRepository.count();
        Optional<LocalDate> latestTradeDate = fundDailyDataRepository.findLatestTradeDate();

        // 数据源统计
        Map<String, String> dataSourceHealth = getDataSourceHealthStatus();

        stats.put("totalFunds", totalFunds);
        stats.put("activeFunds", activeFunds);
        stats.put("totalDataRecords", totalDataRecords);
        stats.put("latestTradeDate", latestTradeDate.orElse(null));
        stats.put("dataSourceHealth", dataSourceHealth);
        stats.put("lastUpdateTime", LocalDateTime.now());

        return stats;
    }

    @Override
    @Transactional
    public boolean fixDataIssue(String fundCode, LocalDate date) {
        log.info("Fixing data issue for fund: {} on date: {}", fundCode, date);

        try {
            // 1. 删除有问题的数据
            fundDailyDataRepository.findByFundCodeAndTradeDate(fundCode, date)
                    .ifPresent(data -> fundDailyDataRepository.delete(data));

            // 2. 重新同步数据
            return syncFundData(fundCode, date);
        } catch (Exception e) {
            log.error("Error fixing data issue for fund: {} on date: {}", fundCode, date, e);
            return false;
        }
    }

    // ================ 私有辅助方法 ================

    /**
     * 从数据源获取数据（支持重试和降级）
     */
    private <T> T fetchFromDataSources(String fundCode, String dataType, Object... params) {
        // 按优先级排序数据源
        List<FundDataSource> sortedSources = dataSources.stream()
                .sorted(Comparator.comparingInt(FundDataSource::getPriority))
                .toList();

        for (FundDataSource source : sortedSources) {
            try {
                if (!source.isAvailable()) {
                    log.debug("Data source {} is not available, skipping", source.getSourceName());
                    continue;
                }

                log.debug("Trying to fetch {} data for fund {} from {}",
                        dataType, fundCode, source.getSourceName());

                T data = switch (dataType) {
                    case "REAL_TIME" -> (T) source.fetchRealTimeData(fundCode);
                    case "HISTORY" -> (T) source.fetchHistoryData(fundCode, (Integer) params[0]);
                    case "HISTORY_RANGE" -> (T) source.fetchHistoryData(fundCode,
                            (LocalDate) params[0], (LocalDate) params[1]);
                    case "BASIC_INFO" -> (T) source.fetchFundBasicInfo(fundCode);
                    default -> null;
                };

                if (data != null) {
                    log.debug("Successfully fetched {} data for fund {} from {}",
                            dataType, fundCode, source.getSourceName());
                    return data;
                }
            } catch (Exception e) {
                log.warn("Error fetching {} data for fund {} from {}: {}",
                        dataType, fundCode, source.getSourceName(), e.getMessage());
            }
        }

        log.error("Failed to fetch {} data for fund {} from all data sources",
                dataType, fundCode);
        return null;
    }

    /**
     * 尝试获取官方净值（从历史数据API）
     * 如果是交易日，只返回当天的官方净值；如果是非交易日，返回最近一个交易日的官方净值
     */
    private FundRealTimeDataDTO tryGetOfficialNetValue(String fundCode, LocalDate date) {
        log.debug("Trying to get official net value for fund: {} on date: {}", fundCode, date);

        try {
            // 从主数据源获取历史数据（最近7天，确保包含当天或最近交易日）
            FundHistoryDataDTO historyData = tianTianFundDataSource.fetchHistoryData(fundCode, date.minusDays(6L), date);
            if (historyData != null && historyData.isValid()) {
                List<FundHistoryDataDTO.HistoryDataItem> items = historyData.getValidDataItems();

                if (isTradingDay(date)) {
                    // 如果是交易日，只查找当天的官方净值
                    for (FundHistoryDataDTO.HistoryDataItem item : items) {
                        LocalDate itemDate = item.getDateAsLocalDate();
                        if (itemDate != null && itemDate.equals(date)) {
                            log.debug("Found official net value for trading day: {} on date: {}", fundCode, date);
                            return createOfficialNetValueDTO(fundCode, date, item);
                        }
                    }
                    // 交易日但未找到当天的官方净值（可能还未公布）
                    log.debug("No official net value found for trading day: {} on date: {}", fundCode, date);
                    return null;
                } else {
                    // 如果是非交易日，查找最近一个交易日的官方净值
                    FundHistoryDataDTO.HistoryDataItem latestTradingDayItem = null;
                    for (FundHistoryDataDTO.HistoryDataItem item : items) {
                        LocalDate itemDate = item.getDateAsLocalDate();
                        if (itemDate != null && isTradingDay(itemDate)) {
                            if (latestTradingDayItem == null || itemDate.isAfter(latestTradingDayItem.getDateAsLocalDate())) {
                                latestTradingDayItem = item;
                            }
                        }
                    }

                    if (latestTradingDayItem != null) {
                        LocalDate latestDate = latestTradingDayItem.getDateAsLocalDate();
                        log.debug("Using latest trading day official net value for fund: {} on date: {} (requested non-trading day: {})",
                                fundCode, latestDate, date);
                        return createOfficialNetValueDTO(fundCode, latestDate, latestTradingDayItem);
                    }

                    log.debug("No official net value found for fund: {} (non-trading day)", fundCode);
                }
            }
        } catch (Exception e) {
            log.warn("Error fetching official net value for fund: {} on date: {}", fundCode, date, e);
        }
        return null;
    }

    /**
     * 创建官方净值DTO
     */
    private FundRealTimeDataDTO createOfficialNetValueDTO(String fundCode, LocalDate date, FundHistoryDataDTO.HistoryDataItem item) {
        FundRealTimeDataDTO dto = new FundRealTimeDataDTO();
        dto.setFundCode(fundCode);
        dto.setNetValue(item.getNetValue());
        dto.setNetValueDate(date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        dto.setEstimateValue(null);
        dto.setEstimateChangeRate(item.getDailyGrowthRate());
        // 估算时间设为当前时间（因为是官方净值，无实时估算时间）
        dto.setEstimateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        log.debug("Created official net value DTO for fund: {} on date: {}", fundCode, date);
        return dto;
    }

    /**
     * 判断当前时间是否在交易时间后（15:30后应该使用官方净值）
     */
    private boolean isAfterTradingHours() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();

        // 如果时间在15:30之后，认为应该使用官方净值
        return hour > 15 || (hour == 15 && minute >= 30);
    }

    /**
     * 判断是否为交易日（周一至周五）
     */
    private boolean isTradingDay(LocalDate date) {
        return transactionService.isTradeDay(date);
    }

    /**
     * 根据当前时间和交易日情况获取数据获取策略
     * 返回true表示应该优先获取官方净值，false表示可以使用估值
     */
    private boolean shouldUseOfficialNetValue() {
        LocalDate today = LocalDate.now();

        // 如果是非交易日，使用最近一个交易日的官方净值
        if (!isTradingDay(today)) {
            return true;
        }

        // 如果是交易日且在交易时间后，使用官方净值
        if (isAfterTradingHours()) {
            return true;
        }

        // 其他情况（交易时间内）可以使用估值
        return false;
    }

    /**
     * 将实时数据DTO转换为数据库实体
     */
    private FundDailyData convertToFundDailyData(FundRealTimeDataDTO dto) {
        FundDailyData data = new FundDailyData();
        data.setFundCode(dto.getFundCode());
        data.setTradeDate(LocalDate.now());

        if (dto.getNetValueAsBigDecimal() != null) {
            data.setNetValue(dto.getNetValueAsBigDecimal());
        }

        if (dto.getEstimateValueAsBigDecimal() != null) {
            data.setEstimateValue(dto.getEstimateValueAsBigDecimal());
        }

        if (dto.getEstimateChangeRateAsBigDecimal() != null) {
            data.setChangeRate(dto.getEstimateChangeRateAsBigDecimal());
        }

        data.setDataSource("tianTianFund");
        data.setDataQuality(dto.getDataQuality());

        return data;
    }

    /**
     * 将历史数据DTO转换为数据库实体列表
     */
    private List<FundDailyData> convertToFundDailyDataList(FundHistoryDataDTO dto) {
        List<FundDailyData> result = new ArrayList<>();

        if (dto.getValidDataItems() != null) {
            for (FundHistoryDataDTO.HistoryDataItem item : dto.getValidDataItems()) {
                FundDailyData data = new FundDailyData();
                data.setFundCode(dto.getFundCode());
                data.setTradeDate(item.getDateAsLocalDate());
                data.setNetValue(item.getNetValueAsBigDecimal());
                data.setChangeRate(item.getDailyGrowthRateAsBigDecimal());
                data.setDataSource("tianTianFund");
                data.setDataQuality("HIGH"); // 历史数据通常是高质量的

                result.add(data);
            }
        }

        return result;
    }

    /**
     * 将基本信息DTO转换为数据库实体
     */
    private FundInfo convertToFundInfo(FundBasicInfoDTO dto) {
        FundInfo info = new FundInfo();
        info.setFundCode(dto.getFundCode());
        info.setFundName(dto.getName());

        // 转换基金类型
        String normalizedType = dto.getFundTypeNormalized();
        try {
            FundType fundType = FundType.valueOf(normalizedType);
            info.setFundType(fundType);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown fund type: {}, defaulting to OTHER", normalizedType);
            info.setFundType(FundType.OTHER);
        }

        info.setRiskLevel(dto.getRiskLevelAsInteger());
        info.setFundCompany(dto.getCompany());
        info.setEstablishedDate(dto.getEstablishDateAsLocalDate());
        info.setManager(dto.getManager());
        info.setFundSize(convertToDouble(dto.getScaleAsBigDecimal()));
        info.setManagementFee(convertToDouble(dto.getManagementFeeAsBigDecimal()));
        info.setCustodyFee(convertToDouble(dto.getCustodyFeeAsBigDecimal()));
        info.setIsActive(true);

        return info;
    }

    /**
     * 保存每日数据（如果已存在则更新）
     */
    @Transactional
    public FundDailyData saveDailyData(FundDailyData data) {
        // 检查是否已存在相同基金代码和交易日的数据
        Optional<FundDailyData> existingData = fundDailyDataRepository
                .findByFundCodeAndTradeDate(data.getFundCode(), data.getTradeDate());

        if (existingData.isPresent()) {
            // 更新现有数据
            FundDailyData existing = existingData.get();
            updateDailyDataFields(existing, data);
            return fundDailyDataRepository.save(existing);
        } else {
            // 插入新数据
            return fundDailyDataRepository.save(data);
        }
    }

    /**
     * 批量保存每日数据
     */
    @Transactional
    public List<FundDailyData> saveDailyDataList(List<FundDailyData> dataList) {
        List<FundDailyData> savedList = new ArrayList<>();

        for (FundDailyData data : dataList) {
            try {
                FundDailyData savedData = saveDailyData(data);
                savedList.add(savedData);
            } catch (Exception e) {
                log.error("Error saving daily data for fund: {} on date: {}",
                        data.getFundCode(), data.getTradeDate(), e);
            }
        }

        return savedList;
    }

    /**
     * 保存基金信息（如果已存在则更新）
     */
    @Transactional
    public FundInfo saveFundInfo(FundInfo info) {
        Optional<FundInfo> existingInfo = fundInfoRepository.findByFundCode(info.getFundCode());

        if (existingInfo.isPresent()) {
            // 更新现有信息
            FundInfo existing = existingInfo.get();
            updateFundInfoFields(existing, info);
            return fundInfoRepository.save(existing);
        } else {
            // 插入新信息
            return fundInfoRepository.save(info);
        }
    }

    /**
     * 更新每日数据字段
     */
    private void updateDailyDataFields(FundDailyData existing, FundDailyData newData) {
        if (newData.getNetValue() != null) {
            existing.setNetValue(newData.getNetValue());
        }
        if (newData.getEstimateValue() != null) {
            existing.setEstimateValue(newData.getEstimateValue());
        }
        if (newData.getChangeRate() != null) {
            existing.setChangeRate(newData.getChangeRate());
        }
        if (newData.getDataSource() != null) {
            existing.setDataSource(newData.getDataSource());
        }
        if (newData.getDataQuality() != null) {
            existing.setDataQuality(newData.getDataQuality());
        }
    }

    /**
     * 更新基金信息字段
     */
    private void updateFundInfoFields(FundInfo existing, FundInfo newInfo) {
        if (newInfo.getFundName() != null) {
            existing.setFundName(newInfo.getFundName());
        }
        if (newInfo.getFundType() != null) {
            existing.setFundType(newInfo.getFundType());
        }
        if (newInfo.getRiskLevel() != null) {
            existing.setRiskLevel(newInfo.getRiskLevel());
        }
        if (newInfo.getFundCompany() != null) {
            existing.setFundCompany(newInfo.getFundCompany());
        }
        if (newInfo.getEstablishedDate() != null) {
            existing.setEstablishedDate(newInfo.getEstablishedDate());
        }
        if (newInfo.getManager() != null) {
            existing.setManager(newInfo.getManager());
        }
        if (newInfo.getFundSize() != null) {
            existing.setFundSize(newInfo.getFundSize());
        }
        if (newInfo.getManagementFee() != null) {
            existing.setManagementFee(newInfo.getManagementFee());
        }
        if (newInfo.getCustodyFee() != null) {
            existing.setCustodyFee(newInfo.getCustodyFee());
        }
        if (newInfo.getIsActive() != null) {
            existing.setIsActive(newInfo.getIsActive());
        }
        if (newInfo.getRemark() != null) {
            existing.setRemark(newInfo.getRemark());
        }
    }

    /**
     * 将BigDecimal安全转换为Double
     */
    private Double convertToDouble(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.doubleValue();
    }

    /**
     * 判断是否为周末
     */
    private boolean isWeekend(LocalDate date) {
        java.time.DayOfWeek dayOfWeek = date.getDayOfWeek();
        return dayOfWeek == java.time.DayOfWeek.SATURDAY || dayOfWeek == java.time.DayOfWeek.SUNDAY;
    }

    /**
     * 计算交易日数量
     */
    private int calculateTradingDays(LocalDate startDate, LocalDate endDate) {
        int count = 0;
        LocalDate date = startDate;

        while (!date.isAfter(endDate)) {
            if (!isWeekend(date)) {
                count++;
            }
            date = date.plusDays(1);
        }

        return count;
    }
}