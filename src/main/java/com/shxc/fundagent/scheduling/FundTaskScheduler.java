package com.shxc.fundagent.scheduling;

import com.shxc.fundagent.entity.FundDailyData;
import com.shxc.fundagent.entity.FundHolding;
import com.shxc.fundagent.entity.FundTransactionRecord;
import com.shxc.fundagent.enums.TransactionStatus;
import com.shxc.fundagent.enums.TransactionType;
import com.shxc.fundagent.repository.FundDailyDataRepository;
import com.shxc.fundagent.repository.FundHoldingRepository;
import com.shxc.fundagent.repository.FundTransactionRecordRepository;
import com.shxc.fundagent.service.FundDataService;
import com.shxc.fundagent.service.ReportGenerationService;
import com.shxc.fundagent.service.YieldCalculationService;
import com.shxc.fundagent.strategy.StrategyDecisionEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 定时任务调度器
 * 负责执行系统各种定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FundTaskScheduler {

    private final FundDataService fundDataService;
    private final StrategyDecisionEngine strategyDecisionEngine;
    private final YieldCalculationService yieldCalculationService;
    private final ReportGenerationService reportGenerationService;
    private final CacheManager cacheManager;
    private final FundHoldingRepository fundHoldingRepository;
    private final FundTransactionRecordRepository fundTransactionRecordRepository;
    private final FundDailyDataRepository fundDailyDataRepository;
    private static final Set<LocalDate> tradeDays = new HashSet<>();

    // 持仓状态常量
    private static final String HOLDING_STATUS_ACTIVE = "ACTIVE";
    private static final String HOLDING_STATUS_SOLD = "SOLD";

    // ================ 数据采集任务 ================

    @Scheduled(cron = "0 0 0 * * MON-FRI")
    public void init() {
        executeScheduledTask("初始化任务", () -> {
            log.info("美好的一天开始啦😘😘😘");
            // 清理统计交易日的缓存
            log.info("目前已经交易的日期 {}", tradeDays);
            if (tradeDays.size() >= 10) {
                tradeDays.clear();
            }
        });
    }

    /**
     * 每日开盘数据采集（每个交易日9:30执行）
     * 获取基金实时估值数据
     */
    @Scheduled(cron = "0 30 9 * * MON-FRI")
    public void collectOpeningData() {
        executeScheduledTask("开盘数据采集任务", () -> {
            List<String> fundCodes = getMonitoredFundCodes();

            if (fundCodes.isEmpty()) {
                log.info("没有需要监控的基金，跳过数据采集");
                return;
            }

            log.info("开始采集 {} 只基金的实时数据...", fundCodes.size());
            fundDataService.batchGetRealTimeData(fundCodes);
        });
    }

    /**
     * 每日收盘数据采集（每个交易日15:00执行）
     * 获取的根据上季度持仓计划预估净值
     * 获取基金收盘净值数据
     */
    @Scheduled(cron = "0 0 15 * * MON-FRI")
    public void collectClosingData() {
        List<String> fundCodes = getMonitoredFundCodes();
        executeFundCodeTask("收盘数据采集任务", fundCodes, fundCode -> {
            // 收盘时可能需要更详细的数据，如净值、成交量等
            // 这里可以调用专门获取收盘数据的方法
            fundDataService.getRealTimeData(fundCode);
        });
    }

    /**
     * 周末数据同步（每周六上午9点执行）
     * 同步一周的基金基础信息
     */
    @Scheduled(cron = "0 0 9 * * SAT")
    public void syncWeekendData() {
        List<String> fundCodes = getMonitoredFundCodes();
        executeFundCodeTask("周末数据同步任务", fundCodes, fundCode -> {
            // 同步基金基础信息
            fundDataService.getFundBasicInfo(fundCode);
            log.debug("基金 {} 基础信息同步成功", fundCode);
        });
    }

    // ================ 策略决策任务 ================

    /**
     * 策略决策执行（每个交易日10:00执行）
     * 对所有监控基金执行策略决策
     */
    @Scheduled(cron = "0 0 10 * * MON-FRI")
    public void executeStrategyDecisions() {
        List<String> fundCodes = getMonitoredFundCodes();
        executeFundCodeTask("策略决策任务", fundCodes, fundCode -> {
            strategyDecisionEngine.decideForFund(fundCode);
            log.debug("基金 {} 策略决策执行成功", fundCode);
        });
    }

    /**
     * 持仓收益计算（每个交易日20:00执行）
     * 计算所有持仓基金的收益情况
     */
//    @Scheduled(cron = "0 30 20 * * MON-FRI")
    public void calculateHoldingYields() {
        List<String> holdingFundCodes = getHoldingFundCodes();
        executeFundCodeTask("持仓收益计算任务", holdingFundCodes, fundCode -> {
            yieldCalculationService.calculateFundYield(fundCode, null);
            log.debug("基金 {} 收益计算成功", fundCode);
        });
    }

    /**
     * 重新计算所有持仓基金的成本价格（每个交易日20:00执行）
     * 计算所有持仓基金的收益情况
     * 并且对交易记录的净值以及状态进行更新
     */
    @Scheduled(cron = "0 0 20 * * MON-FRI")
    public void recalculateHoldingCostPrice() {
        executeScheduledTask("重新计算持仓成本价格任务", () -> {
            List<FundHolding> fundHoldings = fundHoldingRepository.findAllActiveHoldings();
            for (FundHolding fundHolding : fundHoldings) {
                calculateHoldingCostPrice(fundHolding);
                if (fundHolding.getHoldingAmount().equals(BigDecimal.ZERO)) {
                    fundHolding.setStatus(HOLDING_STATUS_SOLD);
                }
            }
            fundHoldingRepository.saveAll(fundHoldings);
        });
    }

    @Scheduled(cron = "0 */30 20-23 * * MON-FRI")
    public void fallback() {
        executeScheduledTask("兜底任务", () -> {
            // 近七天净值计算失败的所有基金
            List<FundDailyData> fundDailyData = fundDailyDataRepository.findByTradeDateBetween(LocalDate.now().minusDays(6), LocalDate.now());
            List<FundDailyData> failedFundDailyData = fundDailyData.stream().filter(it -> it.getNetValue() == null).toList();
            Map<String, List<FundDailyData>> failFundDataMap = failedFundDailyData.stream().collect(Collectors.groupingBy(FundDailyData::getFundCode, Collectors.toList()));

            for (String fundCode: failFundDataMap.keySet()) {
                log.info("基金 {} 净值计算失败，开始兜底", fundCode);
                List<FundDailyData> datas = failFundDataMap.get(fundCode);
                datas.sort(Comparator.comparing(FundDailyData::getTradeDate));
                Map<LocalDate, FundDailyData> historyMap = fundDataService.getHistoryData(fundCode, datas.get(0).getTradeDate(), datas.get(datas.size() - 1).getTradeDate())
                        .stream().collect(Collectors.toMap(FundDailyData::getTradeDate, it -> it));
                for (FundDailyData data: datas) {
                    FundDailyData historyData = historyMap.get(data.getTradeDate());
                    if (historyData != null && historyData.getNetValue() != null) {
                        log.info("基金 {} 于{}的净值成功，开始入库", fundCode, data.getTradeDate());
                        data.setNetValue(historyData.getNetValue());
                        data.setChangeRate(historyData.getChangeRate());
                        fundDailyDataRepository.save(data);
                    }
                }
            }

            // 获取所有需要确认的交易记录
            List<String> toEstimate = fundTransactionRecordRepository.findByEstimatedConfirmDateBeforeAndStatus(LocalDate.now(), TransactionStatus.PENDING)
                    .stream().map(FundTransactionRecord::getFundCode).distinct().toList();
            for (String fundCode: toEstimate) {
                FundHolding fundHolding = fundHoldingRepository.findAcitveHoldingByFundCode(fundCode);
                calculateHoldingCostPrice(fundHolding);
                if (fundHolding.getHoldingAmount().equals(BigDecimal.ZERO)) {
                    fundHolding.setStatus(HOLDING_STATUS_SOLD);
                }
                fundHoldingRepository.save(fundHolding);
            }

            List<FundTransactionRecord> records = fundTransactionRecordRepository.findByEstimatedConfirmDateBeforeAndStatus(LocalDate.now(), TransactionStatus.PENDING);
            if (records.isEmpty() && !tradeDays.contains(LocalDate.now())) {
                // 说明当前所有基金的交易记录已经确认 每天只执行一遍
                tradeDays.add(LocalDate.now());
                calculateHoldingYields();
            }
        });
    }

    /**
     * 计算持仓成本价格和份额
     * 基于交易记录计算当前持仓的成本价格、持有份额和持有价值
     *
     * @param fundHolding 持仓记录（会被更新）
     */
    private void calculateHoldingCostPrice(FundHolding fundHolding) {
        List<FundTransactionRecord> transactionRecords =
                fundTransactionRecordRepository.findActiveTransactionRecord(fundHolding.getFundCode());
        if (transactionRecords.isEmpty()) return;

        BigDecimal holdingAmount = BigDecimal.ZERO; // 计算所有份额
        BigDecimal realHoldingValue = BigDecimal.ZERO; // 实际持有价值 = sum(each total_amount * (1 - fee))

        for (FundTransactionRecord record: transactionRecords) {
            if (!confirmedTransaction(record)) break;
            BigDecimal sign = new BigDecimal(record.getTransactionSign());
            holdingAmount = holdingAmount.add(record.getAmount().multiply(sign));
            // 实际持有价值 += 全部交易金额 * (1 - 手续费 / 100)
            realHoldingValue = realHoldingValue.add(record.getTotalCost().multiply(sign));
        }

        if (holdingAmount.compareTo(BigDecimal.ZERO) == 0) {
            fundHolding.setCostPrice(BigDecimal.ZERO);
            fundHolding.setHoldingAmount(BigDecimal.ZERO);
            fundHolding.setHoldingValue(BigDecimal.ZERO);
        } else {
            BigDecimal costPrice = realHoldingValue.divide(holdingAmount, 4, RoundingMode.HALF_UP);
            fundHolding.setCostPrice(costPrice);
            fundHolding.setHoldingAmount(holdingAmount);
            fundHolding.setHoldingValue(realHoldingValue);
        }
    }

    /**
     * 确认交易记录状态
     * 如果交易状态为PENDING，尝试获取净值进行确认
     * 对于购买交易，计算实际确认份额；对于赎回交易，计算实际赎回金额
     *
     * @param record 交易记录（可能会被更新状态）
     * @return true如果交易已确认或已经是确认状态，false如果无法确认（净值未计算）
     */
    private boolean confirmedTransaction(FundTransactionRecord record) {
        String fundCode = record.getFundCode();
        if (record.getStatus() == TransactionStatus.PENDING) {
            // 通过实时净值接口获取当天净值，获取报道说明净值信息还未更新
            // 一般都是今天 是为了避免存在历史脏数据才获取的预估确认日期
            BigDecimal currentPrice = fetchPrice(fundCode, record.getEstimatedConfirmDate());
            if (currentPrice == null) {
                // TODO: 尝试加入重试机制
                log.warn("基金代码为{}的交易时间于{}的交易由于净值未计算无法结算", fundCode, record.getTransactionTime());
                return false;
            }
            record.setPrice(currentPrice);
            record.setStatus(TransactionStatus.CONFIRMED);
            record.setActualConfirmTime(LocalDateTime.now());

            if (TransactionType.BUY.equals(record.getTransactionType())) { // 说明净值还未计算
                // 说明当天净值还未计算出来 直接放弃当前持仓成本金额的计算
                record.setAmount(record.getTotalAmount().divide(record.getPrice(), 4, RoundingMode.HALF_UP));
                // 计算完后保存交易记录的状态
            } else if (TransactionType.SELL.equals(record.getTransactionType())) {
                BigDecimal amount = record.getAmount(); // 卖出份额
                record.setTotalAmount(amount.multiply(record.getPrice()));
            }
            fundTransactionRecordRepository.save(record);
        }
        return true;
    }

    /**
     * 获取指定基金在特定日期的净值
     *
     * @param fundCode 基金代码
     * @param targetDate 目标日期
     * @return 净值，如果未找到则返回null
     */
    private BigDecimal fetchPrice(String fundCode, LocalDate targetDate) {
        List<FundDailyData> dataList = fundDataService.getHistoryData(fundCode, targetDate, targetDate);
        return dataList.stream()
                .findFirst()
                .map(FundDailyData::getNetValue)
                .orElse(null);
    }

    // ================ 报告生成任务 ================

    /**
     * 日报生成（每个交易日16:00执行）
     */
    @Scheduled(cron = "0 0 16 * * MON-FRI")
    public void generateDailyReport() {
        executeScheduledTask("日报生成任务", () -> {
            String reportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            reportGenerationService.generateDailyReport(reportDate);
            log.info("日报生成任务完成，报告日期: {}", reportDate);
        });
    }

    /**
     * 周报生成（每周五16:30执行）
     */
    @Scheduled(cron = "0 30 16 * * FRI")
    public void generateWeeklyReport() {
        executeScheduledTask("周报生成任务", () -> {
            String reportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            reportGenerationService.generateWeeklyReport(reportDate);
            log.info("周报生成任务完成，报告日期: {}", reportDate);
        });
    }

    /**
     * 月报生成（每月最后交易日16:30执行）
     */
    @Scheduled(cron = "0 30 16 L * ?")
    public void generateMonthlyReport() {
        executeScheduledTask("月报生成任务", () -> {
            String reportDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
            reportGenerationService.generateMonthlyReport(reportDate);
            log.info("月报生成任务完成，报告月份: {}", reportDate);
        });
    }

    // ================ 系统维护任务 ================

    /**
     * 缓存清理（每天凌晨2点执行）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupCache() {
        executeScheduledTask("缓存清理任务", () -> {
            if (cacheManager != null) {
                cacheManager.getCacheNames().forEach(cacheName -> {
                    try {
                        cacheManager.getCache(cacheName).clear();
                        log.debug("缓存 {} 清理完成", cacheName);
                    } catch (Exception e) {
                        log.warn("缓存 {} 清理失败: {}", cacheName, e.getMessage());
                    }
                });
            }

            // 同时调用策略引擎的缓存清理
            strategyDecisionEngine.clearCache("all");
        });
    }

    /**
     * 任务状态检查（每小时执行一次）
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void checkTaskStatus() {
        executeScheduledTask("任务状态检查", () -> {
            log.debug("任务状态检查...");
            // 这里可以检查定时任务的执行状态，记录日志等
        });
    }

    // ================ 私有辅助方法 ================

    /**
     * 获取需要监控的基金代码列表
     * 当前实现返回所有持仓基金代码，未来可以扩展为从配置中获取
     *
     * @return 需要监控的基金代码列表
     */
    private List<String> getMonitoredFundCodes() {
        // TODO: 未来可以从数据库或配置中获取需要监控的基金代码
        // 当前简化实现：返回所有持仓基金代码
        return getHoldingFundCodes();
    }

    /**
     * 获取持仓基金代码列表
     */
    private List<String> getHoldingFundCodes() {
        // 这里应该从数据库中获取持仓基金代码
        List<FundHolding> holdings = fundHoldingRepository.findAllActiveHoldings();
        return holdings.stream().map(FundHolding::getFundCode).collect(Collectors.toList());
    }

    // ================ 私有辅助方法 ================

    /**
     * 执行定时任务的标准模板方法
     * 提供统一的错误处理和日志记录
     *
     * @param taskName 任务名称（用于日志记录）
     * @param taskFunction 任务执行逻辑
     */
    private void executeScheduledTask(String taskName, Runnable taskFunction) {
        log.info("开始执行{}...", taskName);

        try {
            taskFunction.run();
            log.info("{}完成", taskName);
        } catch (Exception e) {
            log.error("{}失败", taskName, e);
        }
    }

    /**
     * 执行基金代码列表相关的定时任务模板方法
     * 提供统一的错误处理、日志记录和成功失败统计
     *
     * @param taskName 任务名称（用于日志记录）
     * @param fundCodes 基金代码列表
     * @param fundCodeProcessor 对每个基金代码的处理逻辑
     */
    private void executeFundCodeTask(String taskName, List<String> fundCodes,
                                    java.util.function.Consumer<String> fundCodeProcessor) {
        log.info("开始执行{}...", taskName);

        try {
            if (fundCodes.isEmpty()) {
                log.info("没有需要处理的基金，跳过{}", taskName);
                return;
            }

            int successCount = 0;
            int failCount = 0;

            for (String fundCode : fundCodes) {
                try {
                    fundCodeProcessor.accept(fundCode);
                    successCount++;
                    log.debug("基金 {} {}成功", fundCode, taskName);
                } catch (Exception e) {
                    failCount++;
                    log.warn("基金 {} {}失败: {}", fundCode, taskName, e.getMessage());
                }
            }

            log.info("{}完成，成功: {}，失败: {}", taskName, successCount, failCount);
        } catch (Exception e) {
            log.error("{}失败", taskName, e);
        }
    }
}