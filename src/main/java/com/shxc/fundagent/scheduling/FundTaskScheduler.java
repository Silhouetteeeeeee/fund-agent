package com.shxc.fundagent.scheduling;

import com.shxc.fundagent.agent.impl.FundAnalysisAgent;
import com.shxc.fundagent.agent.model.AgentResult;
import com.shxc.fundagent.entity.FundDailyData;
import com.shxc.fundagent.entity.FundHolding;
import com.shxc.fundagent.entity.FundInfo;
import com.shxc.fundagent.entity.FundTransactionRecord;
import com.shxc.fundagent.enums.TransactionStatus;
import com.shxc.fundagent.enums.TransactionType;
import com.shxc.fundagent.llm.template.FundTemplate;
import com.shxc.fundagent.repository.FundDailyDataRepository;
import com.shxc.fundagent.repository.FundHoldingRepository;
import com.shxc.fundagent.repository.FundInfoRepository;
import com.shxc.fundagent.repository.FundTransactionRecordRepository;
import com.shxc.fundagent.service.*;
import com.shxc.fundagent.strategy.StrategyDecisionEngine;
import dev.ai4j.openai4j.Json;
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

import com.shxc.fundagent.strategy.model.FundPositionContext;
import com.shxc.fundagent.strategy.model.PortfolioContext;

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
    private final FundInfoRepository fundInfoRepository;
    private final RegularInvestmentPlanService regularInvestmentPlanService;
    private final HolidayCalendarService holidayCalendarService;
    private final FundAnalysisAgent fundAnalysisAgent;

    // 持仓状态常量
    private static final String HOLDING_STATUS_ACTIVE = "ACTIVE";
    private static final String HOLDING_STATUS_SOLD = "SOLD";

    // ================ 数据采集任务 ================

    @Scheduled(cron = "0 0 0 * * MON-FRI")
    public void init() {
        executeScheduledTask("初始化任务", () -> {
            log.info("美好的一天开始啦😘😘😘");
            // 初始化交易日缓存

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
     * 计算所有持仓基金前一日的收益情况
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI")
    public void calculateHoldingYields() {
        List<String> holdingFundCodes = getHoldingFundCodes();
        List<YieldCalculationService.FundYield> result = new ArrayList<>();
        executeFundCodeTask("持仓收益计算任务", holdingFundCodes, fundCode -> {
            YieldCalculationService.FundYield fundYield = yieldCalculationService.calculateFundYield(fundCode, null);
            if (fundYield != null) result.add(fundYield);
            log.debug("基金 {} 收益计算成功", fundCode);
        });
        PortfolioContext context = resolveYieldsToAgentCtx(result);
        String msg = FundTemplate.buildFfoPrompt(context);
        log.info(msg);
//        AgentResult agentResult = fundAnalysisAgent.process("持仓建议任务", msg);
//        if (agentResult.isSuccess()) {
//            log.info(Json.toJson(agentResult));
//        }
    }

    private PortfolioContext resolveYieldsToAgentCtx(List<YieldCalculationService.FundYield> result) {
        List<FundPositionContext> funds = new ArrayList<>();
        BigDecimal totalCost = BigDecimal.ZERO;
        List<FundHolding> fundHoldings = fundHoldingRepository.findByStatus(HOLDING_STATUS_ACTIVE);
        Map<String, FundInfo> fundInfoMap = fundInfoRepository.findAllActiveFunds()
                .stream()
                .collect(Collectors.toMap(FundInfo::getFundCode, f -> f));
        BigDecimal totalHoldingValue = fundHoldings.stream().map(FundHolding::getHoldingValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        
        for (YieldCalculationService.FundYield fundYield : result) {
            FundInfo fundInfo = fundInfoMap.get(fundYield.getFundCode());
            if (fundInfo == null) continue;
            String fundCode = fundYield.getFundCode();
            FundHolding fundHolding = fundHoldings.stream()
                    .filter(h -> h.getFundCode().equals(fundCode))
                    .findFirst()
                    .orElse(null);
            
            if (fundHolding == null) continue;
            
            totalCost = totalCost.add(fundHolding.getTotalCost());
            BigDecimal netValue = fundYield.getCurrentPrice();
            
            FundPositionContext context = FundPositionContext.builder()
                    .fundCode(fundCode)
                    .fundName(fundYield.getFundName())
                    .fundType(fundInfo.getFundType().getDescription())
                    .managerInfo(fundInfo.getManagerInfo())
                    .netValue(netValue)
                    .dailyChangePercent(fundYield.getDailyChangeRate())
                    .weeklyChangePercent(fundYield.getWeeklyChangeRate())
                    .monthlyChangePercent(fundYield.getMonthlyChangeRate())
                    .yearlyChangePercent(fundInfo.getSyl1n())
                    .riskLevel(fundInfo.getRiskLevel())
                    // 持仓信息
                    .holdShares(fundHolding.getHoldShare())
                    .holdAmount(fundHolding.getHoldingValue())
                    .avgCost(fundHolding.getCostPrice())
                    .costAmount(fundHolding.getTotalCost())
                    .profit(fundHolding.getHoldProfit(netValue))
                    .profitRate(fundHolding.getHoldProfitRate(netValue))
                    .position(fundHolding.getHoldingValue().divide(totalHoldingValue, 2, RoundingMode.HALF_UP))
                    .holdDays(fundHolding.getHoldDays())
                    .build();
            
            funds.add(context);
        }
        
        BigDecimal totalProfit = totalHoldingValue.subtract(totalCost);
        
        return PortfolioContext.builder()
                .funds(funds)
                .totalAssets(totalHoldingValue)
                .totalCost(totalCost)
                .totalProfit(totalProfit)
                .totalProfitRate(totalProfit.divide(totalCost, 2, RoundingMode.HALF_UP))
                .availableCash(BigDecimal.valueOf(100000))
                .targetPosition(new BigDecimal("0.5"))
                .currentPosition(totalHoldingValue.divide(totalHoldingValue.add(BigDecimal.valueOf(100000)), 2, RoundingMode.HALF_UP))
                .build();
    }

    /**
     * 重新计算所有持仓基金的成本价格（每个交易日20:00执行）
     * 计算所有持仓基金的收益情况
     * 并且对交易记录的净值以及状态进行更新
     */
    @Scheduled(cron = "0 0 20 * * MON-FRI")
    public void recalculateHoldingCostPrice() {
        executeScheduledTask("重新计算持仓成本价格任务", () -> {
            // 卖出的也有可能再重新买入，买入的需要重新计算成本价格
            List<FundHolding> fundHoldings = fundHoldingRepository.findAll();
            for (FundHolding fundHolding : fundHoldings) {
                calculateHoldingCostPrice(fundHolding);
                if (fundHolding.getHoldingAmount().equals(BigDecimal.ZERO)) {
                    fundHolding.setStatus(HOLDING_STATUS_SOLD);
                } else if (fundHolding.getStatus().equals(HOLDING_STATUS_SOLD)) {
                    fundHolding.setStatus(HOLDING_STATUS_ACTIVE);
                }
            }
            fundHoldingRepository.saveAll(fundHoldings);
        });
    }

    @Scheduled(cron = "0 */30 20-23 * * MON-FRI")
    public void fallback() {
        executeScheduledTask("兜底任务", () -> {
            // 获取所有需要确认的交易记录
            List<String> toEstimate = fundTransactionRecordRepository.findByEstimatedConfirmDateBeforeAndStatus(LocalDate.now(), TransactionStatus.PENDING)
                    .stream().map(FundTransactionRecord::getFundCode).distinct().toList();
            for (String fundCode: toEstimate) {
                FundHolding fundHolding = fundHoldingRepository.findActiveHoldingByFundCode(fundCode);
                if (fundHolding == null) {
                    fundHolding = new FundHolding();
                    fundHolding.setFundCode(fundCode);
                }
                calculateHoldingCostPrice(fundHolding);
                if (fundHolding.getHoldingAmount().equals(BigDecimal.ZERO)) {
                    fundHolding.setStatus(HOLDING_STATUS_SOLD);
                }
                fundHoldingRepository.save(fundHolding);
            }
            // 计算所有未计算交易持仓价值的基金
            LocalDate latestTradeDate = holidayCalendarService.findLatestTradeDay(LocalDate.now());
            List<FundHolding> fundHoldings = fundHoldingRepository.findFundHoldingsByCalculateDateBefore(latestTradeDate);
            for (FundHolding fundHolding : fundHoldings) {
                FundDailyData data = findLatestValidNetValue(fundHolding.getFundCode(), latestTradeDate, 7);
                if (data != null && data.getNetValue() != null) {
                    fundHolding.updateHoldingValue(data.getNetValue());
                    fundHolding.setCalculateDate(data.getTradeDate());
                    fundHoldingRepository.save(fundHolding);
                } else {
                    log.warn("基金 {} 在{}及之前交易日均未找到有效净值", fundHolding.getFundCode(), latestTradeDate);
                }
            }

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
                        log.info("基金 {} 于 {} 的净值获取成功，开始入库", fundCode, data.getTradeDate());
                        data.setNetValue(historyData.getNetValue());
                        data.setChangeRate(historyData.getChangeRate());
                        data = fundDailyDataRepository.save(data);
                    }
                }
            }
        });
    }

    /**
     * 从指定日期开始向前查找，返回第一个有有效净值的交易日的基金数据
     *
     * @param fundCode 基金代码
     * @param startDate 开始查找的日期
     * @param maxRetryDays 最大重试天数（防止无限循环）
     * @return 包含有效净值的基金数据，如果找不到则返回 null
     */
    private FundDailyData findLatestValidNetValue(String fundCode, LocalDate startDate, int maxRetryDays) {
        LocalDate date = startDate;
        int retryCount = 0;

        while (retryCount < maxRetryDays) {
            try {
                List<FundDailyData> historyData = fundDataService.getHistoryData(fundCode, date, date);
                if (historyData != null && !historyData.isEmpty()) {
                    FundDailyData data = historyData.get(0);
                    if (data != null && data.getNetValue() != null) {
                        return data;
                    }
                }

                // 向前推一个交易日
                date = holidayCalendarService.findLatestTradeDay(date.minusDays(1));
                retryCount++;

                // 防止日期倒退太多（可能遇到节假日连续问题）
                if (date.isBefore(startDate.minusDays(maxRetryDays + 7))) {
                    log.warn("基金 {} 回退日期过多，停止查找", fundCode);
                    break;
                }
            } catch (Exception e) {
                log.error("获取基金 {} 在 {} 的净值失败", fundCode, date, e);
                date = holidayCalendarService.findLatestTradeDay(date.minusDays(1));
                retryCount++;
            }
        }

        return null;
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

        if (holdingAmount.equals(BigDecimal.ZERO)) {
            fundHolding.setCostPrice(BigDecimal.ZERO);
            fundHolding.setHoldingAmount(BigDecimal.ZERO);
        } else {
            BigDecimal costPrice = realHoldingValue.divide(holdingAmount, 4, RoundingMode.HALF_UP);
            fundHolding.setCostPrice(costPrice);
            fundHolding.setHoldingAmount(holdingAmount);
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
                record.setAmount(record.getTotalAmount().divide(record.getPrice(), 2, RoundingMode.HALF_UP));
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

    // ================ 定投计划任务 ================

    /**
     * 定投计划执行（每个交易日15:30执行）
     * 执行当天到期的定投计划，生成购买交易记录
     */
    @Scheduled(cron = "0 30 15 * * MON-FRI")
    public void executeRegularInvestmentPlans() {
        executeScheduledTask("定投计划执行任务", () -> {
            int executedCount = regularInvestmentPlanService.executeDuePlans();
            log.info("定投计划执行完成，共执行{}个计划", executedCount);
        });
    }

    /**
     * 定投计划状态刷新（每天凌晨1点执行）
     * 刷新所有定投计划的下次执行日期
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void refreshInvestmentPlans() {
        executeScheduledTask("定投计划状态刷新任务", () -> {
            regularInvestmentPlanService.refreshNextExecutionDates();
            log.info("定投计划状态刷新完成");
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
        List<FundHolding> holdings = fundHoldingRepository.findByStatus(HOLDING_STATUS_ACTIVE);
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