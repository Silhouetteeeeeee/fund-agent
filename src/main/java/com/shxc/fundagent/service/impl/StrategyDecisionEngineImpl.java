package com.shxc.fundagent.service.impl;

import com.shxc.fundagent.entity.FundDailyData;
import com.shxc.fundagent.entity.FundHolding;
import com.shxc.fundagent.entity.FundInfo;
import com.shxc.fundagent.entity.FundStrategyLog;
import com.shxc.fundagent.enums.SuggestionType;
import com.shxc.fundagent.repository.FundDailyDataRepository;
import com.shxc.fundagent.repository.FundHoldingRepository;
import com.shxc.fundagent.repository.FundInfoRepository;
import com.shxc.fundagent.repository.FundStrategyLogRepository;
import com.shxc.fundagent.service.FundDataService;
import com.shxc.fundagent.service.YieldCalculationService;
import com.shxc.fundagent.strategy.StrategyDecisionEngine;
import com.shxc.fundagent.strategy.StrategyRuleType;
import com.shxc.fundagent.strategy.model.RuleMatchResult;
import com.shxc.fundagent.strategy.model.StrategyDecisionResult;
import com.shxc.fundagent.strategy.model.StrategyRuleConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 策略决策引擎实现类
 * 基于规则引擎的投资决策系统
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyDecisionEngineImpl implements StrategyDecisionEngine {

    // ================ 依赖注入 ================
    private final FundInfoRepository fundInfoRepository;
    private final FundHoldingRepository fundHoldingRepository;
    private final FundDailyDataRepository fundDailyDataRepository;
    private final FundStrategyLogRepository strategyLogRepository;
    private final FundDataService fundDataService;
    private final YieldCalculationService yieldCalculationService;

    // ================ 引擎状态 ================
    private final Map<String, StrategyRuleConfig> ruleConfigs = new ConcurrentHashMap<>();
    private final Map<String, StrategyDecisionResult> decisionCache = new ConcurrentHashMap<>();
    private final AtomicLong totalDecisions = new AtomicLong(0);
    private final AtomicLong successfulDecisions = new AtomicLong(0);
    private final AtomicLong failedDecisions = new AtomicLong(0);
    private final AtomicLong cachedDecisions = new AtomicLong(0);
    private long engineStartTime;
    private String engineVersion = "1.0.0";

    // ================ 默认规则配置 ================
    private static final Map<StrategyRuleType, StrategyRuleConfig> DEFAULT_RULES = new HashMap<>();

    static {
        // 极端风险规则
        DEFAULT_RULES.put(StrategyRuleType.EXTREME_RISK, StrategyRuleConfig.builder()
                .ruleId("RULE_001")
                .ruleType(StrategyRuleType.EXTREME_RISK)
                .ruleName("极端风险警报规则")
                .description("当日跌幅超过4%或周跌幅超过8%时发出风险警报")
                .enabled(true)
                .priority(1)
                .suggestionType(SuggestionType.RISK_ALERT)
                .confidenceThreshold(new BigDecimal("0.85"))
                .dailyChangeThreshold(new BigDecimal("-4.0"))
                .weeklyChangeThreshold(new BigDecimal("-8.0"))
                .sendNotification(true)
                .urgentNotification(true)
                .notificationChannels("[\"EMAIL\", \"WECOM\"]")
                .autoExecute(false)
                .requireConfirmation(true)
                .minExecutionAmount(new BigDecimal("1000"))
                .executionRatio(new BigDecimal("0.1"))
                .onlyTradingDays(true)
                .createdAt(LocalDateTime.now())
                .build());

        // 止盈规则
        DEFAULT_RULES.put(StrategyRuleType.PROFIT_TAKING, StrategyRuleConfig.builder()
                .ruleId("RULE_002")
                .ruleType(StrategyRuleType.PROFIT_TAKING)
                .ruleName("止盈规则")
                .description("收益率达到30%时建议清仓")
                .enabled(true)
                .priority(2)
                .suggestionType(SuggestionType.CLEAR)
                .confidenceThreshold(new BigDecimal("0.80"))
                .yieldRateThreshold(new BigDecimal("30.0"))
                .sendNotification(true)
                .urgentNotification(true)
                .notificationChannels("[\"EMAIL\", \"WECOM\"]")
                .autoExecute(false)
                .requireConfirmation(true)
                .minExecutionAmount(new BigDecimal("1000"))
                .executionRatio(new BigDecimal("1.0"))
                .onlyTradingDays(true)
                .createdAt(LocalDateTime.now())
                .build());

        // 高估规则
        DEFAULT_RULES.put(StrategyRuleType.OVERVALUED, StrategyRuleConfig.builder()
                .ruleId("RULE_003")
                .ruleType(StrategyRuleType.OVERVALUED)
                .ruleName("高估卖出规则")
                .description("收益率达到20%时建议卖出")
                .enabled(true)
                .priority(3)
                .suggestionType(SuggestionType.SELL)
                .confidenceThreshold(new BigDecimal("0.75"))
                .yieldRateThreshold(new BigDecimal("20.0"))
                .sendNotification(true)
                .urgentNotification(false)
                .notificationChannels("[\"EMAIL\", \"WECOM\"]")
                .autoExecute(false)
                .requireConfirmation(true)
                .minExecutionAmount(new BigDecimal("1000"))
                .executionRatio(new BigDecimal("0.5"))
                .onlyTradingDays(true)
                .createdAt(LocalDateTime.now())
                .build());

        // 低估规则
        DEFAULT_RULES.put(StrategyRuleType.UNDERVALUED, StrategyRuleConfig.builder()
                .ruleId("RULE_004")
                .ruleType(StrategyRuleType.UNDERVALUED)
                .ruleName("低估买入规则")
                .description("收益率低于-15%且当日跌幅超过3%时建议买入")
                .enabled(true)
                .priority(4)
                .suggestionType(SuggestionType.BUY)
                .confidenceThreshold(new BigDecimal("0.70"))
                .yieldRateThreshold(new BigDecimal("-15.0"))
                .dailyChangeThreshold(new BigDecimal("-3.0"))
                .sendNotification(true)
                .urgentNotification(false)
                .notificationChannels("[\"EMAIL\", \"WECOM\"]")
                .autoExecute(false)
                .requireConfirmation(true)
                .minExecutionAmount(new BigDecimal("1000"))
                .executionRatio(new BigDecimal("0.3"))
                .onlyTradingDays(true)
                .createdAt(LocalDateTime.now())
                .build());

        // 正常持有规则
        DEFAULT_RULES.put(StrategyRuleType.NORMAL_HOLD, StrategyRuleConfig.builder()
                .ruleId("RULE_005")
                .ruleType(StrategyRuleType.NORMAL_HOLD)
                .ruleName("正常持有规则")
                .description("收益率在-15%到15%之间时建议持有")
                .enabled(true)
                .priority(5)
                .suggestionType(SuggestionType.HOLD)
                .confidenceThreshold(new BigDecimal("0.60"))
                .minYieldRateThreshold(new BigDecimal("-15.0"))
                .maxYieldRateThreshold(new BigDecimal("15.0"))
                .sendNotification(false)
                .urgentNotification(false)
                .autoExecute(false)
                .onlyTradingDays(true)
                .createdAt(LocalDateTime.now())
                .build());
    }

    /**
     * 初始化方法
     */
    @Override
    @Transactional
    public void reloadStrategyRules() {
        log.info("开始加载策略规则配置...");

        // 清空现有规则
        ruleConfigs.clear();

        // 加载默认规则
        DEFAULT_RULES.forEach((type, config) -> {
            ruleConfigs.put(config.getRuleId(), config);
            log.debug("加载默认规则: {} - {}", config.getRuleId(), config.getRuleName());
        });

        // 这里可以添加从数据库加载自定义规则的逻辑
        // List<FundStrategyLog> customRules = strategyLogRepository.findCustomRules();
        // customRules.forEach(rule -> {
        //     StrategyRuleConfig config = convertToRuleConfig(rule);
        //     ruleConfigs.put(config.getRuleId(), config);
        // });

        log.info("策略规则加载完成，共加载 {} 条规则", ruleConfigs.size());
    }

    /**
     * 对单只基金进行策略决策
     */
    @Override
    @Cacheable(value = "strategy", key = "'fund:' + #fundCode")
    public StrategyDecisionResult decideForFund(String fundCode) {
        long startTime = System.currentTimeMillis();
        log.debug("开始对基金[{}]进行策略决策", fundCode);

        try {
            // 获取基金信息
            Optional<FundInfo> fundInfoOpt = fundInfoRepository.findByFundCode(fundCode);
            if (fundInfoOpt.isEmpty()) {
                throw new IllegalArgumentException("基金不存在: " + fundCode);
            }

            FundInfo fundInfo = fundInfoOpt.get();

            // 获取最新日线数据
            Optional<FundDailyData> dailyDataOpt = fundDailyDataRepository
                    .findLatestByFundCode(fundCode);

            // 创建决策结果
            StrategyDecisionResult result = StrategyDecisionResult.builder()
                    .decisionId(generateDecisionId())
                    .fundCode(fundCode)
                    .fundName(fundInfo.getFundName())
                    .decisionTime(LocalDateTime.now())
                    .currentPrice(dailyDataOpt.map(d -> d.getEffectivePrice()).orElse(null))
                    .currentYieldRate(dailyDataOpt.map(d -> d.getChangeRate()).orElse(null))
                    .build();

            // 如果有持仓信息，也获取（取第一个活跃持仓）
            List<FundHolding> holdings = fundHoldingRepository.findByFundCodeAndStatus(fundCode, "ACTIVE");
            if (!holdings.isEmpty()) {
                result.updateFromHolding(holdings.get(0));
            }

            // 如果有日线数据，更新涨跌幅信息
            dailyDataOpt.ifPresent(dailyData -> {
                result.setDailyChange(dailyData.getDailyChangeRate());
                // 这里可以添加周涨跌幅、月涨跌幅的计算逻辑
            });

            // 执行规则匹配（使用第一个持仓，如果没有则为null）
            FundHolding holding = !holdings.isEmpty() ? holdings.get(0) : null;
            executeRuleMatching(result, fundInfo, dailyDataOpt.orElse(null), holding);

            // 计算最终建议和置信度
            calculateFinalSuggestion(result);

            // 记录决策日志
            saveDecisionLog(result);

            // 更新统计信息
            successfulDecisions.incrementAndGet();
            totalDecisions.incrementAndGet();

            long endTime = System.currentTimeMillis();
            result.setCalculationTimeMs(endTime - startTime);

            log.info("基金[{}]策略决策完成，最终建议: {}，置信度: {}%，耗时: {}ms",
                    fundCode, result.getFinalSuggestion(),
                    result.getFinalConfidence() != null ?
                            result.getFinalConfidence().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) : "N/A",
                    result.getCalculationTimeMs());

            return result;

        } catch (Exception e) {
            failedDecisions.incrementAndGet();
            totalDecisions.incrementAndGet();
            log.error("基金[{}]策略决策失败: {}", fundCode, e.getMessage(), e);
            throw new RuntimeException("策略决策失败: " + e.getMessage(), e);
        }
    }

    /**
     * 对持仓基金进行策略决策
     */
    @Override
    public StrategyDecisionResult decideForHolding(FundHolding holding) {
        if (holding == null) {
            throw new IllegalArgumentException("持仓信息不能为空");
        }

        // 使用基金代码进行决策，但会包含持仓信息
        StrategyDecisionResult result = decideForFund(holding.getFundCode());
        result.updateFromHolding(holding);

        // 重新计算最终建议（考虑持仓信息）
        calculateFinalSuggestion(result);

        return result;
    }

    /**
     * 对基金信息进行策略决策
     */
    @Override
    public StrategyDecisionResult decideForFundInfo(FundInfo fundInfo) {
        if (fundInfo == null) {
            throw new IllegalArgumentException("基金信息不能为空");
        }

        return decideForFund(fundInfo.getFundCode());
    }

    /**
     * 批量对多只基金进行策略决策
     */
    @Override
    public Map<String, StrategyDecisionResult> decideForFunds(List<String> fundCodes) {
        Map<String, StrategyDecisionResult> results = new HashMap<>();

        for (String fundCode : fundCodes) {
            try {
                StrategyDecisionResult result = decideForFund(fundCode);
                results.put(fundCode, result);
            } catch (Exception e) {
                log.warn("基金[{}]决策失败: {}", fundCode, e.getMessage());
                // 创建失败结果
                StrategyDecisionResult failedResult = StrategyDecisionResult.builder()
                        .decisionId(generateDecisionId())
                        .fundCode(fundCode)
                        .fundName("决策失败")
                        .decisionTime(LocalDateTime.now())
                        .finalConfidence(BigDecimal.ZERO)
                        .build();
                results.put(fundCode, failedResult);
            }
        }

        return results;
    }

    /**
     * 批量对多个持仓进行策略决策
     */
    @Override
    public Map<Long, StrategyDecisionResult> decideForHoldings(List<FundHolding> holdings) {
        Map<Long, StrategyDecisionResult> results = new HashMap<>();

        for (FundHolding holding : holdings) {
            try {
                StrategyDecisionResult result = decideForHolding(holding);
                results.put(holding.getId(), result);
            } catch (Exception e) {
                log.warn("持仓[{}]决策失败: {}", holding.getId(), e.getMessage());
            }
        }

        return results;
    }

    /**
     * 获取所有可用的策略规则配置
     */
    @Override
    public List<StrategyRuleConfig> getAllStrategyRules() {
        return new ArrayList<>(ruleConfigs.values()).stream()
                .sorted(Comparator.comparingInt(StrategyRuleConfig::getPriority))
                .collect(Collectors.toList());
    }

    /**
     * 根据规则类型获取策略规则配置
     */
    @Override
    public StrategyRuleConfig getStrategyRule(StrategyRuleType ruleType) {
        return ruleConfigs.values().stream()
                .filter(config -> config.getRuleType() == ruleType)
                .findFirst()
                .orElse(null);
    }

    /**
     * 根据规则ID获取策略规则配置
     */
    @Override
    public StrategyRuleConfig getStrategyRuleById(String ruleId) {
        return ruleConfigs.get(ruleId);
    }

    /**
     * 启用或禁用策略规则
     */
    @Override
    public StrategyRuleConfig updateRuleStatus(String ruleId, boolean enabled) {
        StrategyRuleConfig config = ruleConfigs.get(ruleId);
        if (config == null) {
            throw new IllegalArgumentException("规则不存在: " + ruleId);
        }

        config.setEnabled(enabled);
        config.setUpdatedAt(LocalDateTime.now());

        log.info("规则[{}]状态更新为: {}", ruleId, enabled ? "启用" : "禁用");
        return config;
    }

    /**
     * 更新策略规则配置
     */
    @Override
    public StrategyRuleConfig updateStrategyRule(StrategyRuleConfig ruleConfig) {
        if (ruleConfig.getRuleId() == null) {
            throw new IllegalArgumentException("规则ID不能为空");
        }

        if (!ruleConfigs.containsKey(ruleConfig.getRuleId())) {
            throw new IllegalArgumentException("规则不存在: " + ruleConfig.getRuleId());
        }

        ruleConfig.setUpdatedAt(LocalDateTime.now());
        ruleConfigs.put(ruleConfig.getRuleId(), ruleConfig);

        log.info("规则[{}]配置已更新", ruleConfig.getRuleId());
        return ruleConfig;
    }

    /**
     * 添加新的策略规则
     */
    @Override
    public StrategyRuleConfig addStrategyRule(StrategyRuleConfig ruleConfig) {
        if (ruleConfig.getRuleId() == null) {
            ruleConfig.setRuleId(generateRuleId());
        }

        if (ruleConfigs.containsKey(ruleConfig.getRuleId())) {
            throw new IllegalArgumentException("规则ID已存在: " + ruleConfig.getRuleId());
        }

        ruleConfig.setCreatedAt(LocalDateTime.now());
        ruleConfig.setUpdatedAt(LocalDateTime.now());
        ruleConfigs.put(ruleConfig.getRuleId(), ruleConfig);

        log.info("新规则[{}]添加成功: {}", ruleConfig.getRuleId(), ruleConfig.getRuleName());
        return ruleConfig;
    }

    /**
     * 删除策略规则
     */
    @Override
    public boolean deleteStrategyRule(String ruleId) {
        if (!ruleConfigs.containsKey(ruleId)) {
            throw new IllegalArgumentException("规则不存在: " + ruleId);
        }

        ruleConfigs.remove(ruleId);
        log.info("规则[{}]已删除", ruleId);
        return true;
    }

    /**
     * 获取决策引擎状态
     */
    @Override
    public EngineStatus getEngineStatus() {
        long uptime = engineStartTime > 0 ? (System.currentTimeMillis() - engineStartTime) / 1000 : 0;

        return new EngineStatus(
                true, // running
                totalDecisions.get(),
                successfulDecisions.get(),
                failedDecisions.get(),
                cachedDecisions.get(),
                calculateAverageDecisionTime(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                (int) ruleConfigs.values().stream().filter(StrategyRuleConfig::isEnabled).count(),
                ruleConfigs.size(),
                engineVersion,
                uptime
        );
    }

    /**
     * 清理决策缓存
     */
    @Override
    public void clearCache(String cacheType) {
        if ("all".equalsIgnoreCase(cacheType) || "fund".equalsIgnoreCase(cacheType)) {
            int size = decisionCache.size();
            decisionCache.clear();
            log.info("清理基金决策缓存，共清理 {} 条记录", size);
        }

        // 这里可以添加其他类型缓存的清理逻辑
    }

    /**
     * 获取决策统计信息
     */
    @Override
    public DecisionStatistics getDecisionStatistics() {
        // 这里实现统计信息计算逻辑
        // 暂时返回空对象
        return new DecisionStatistics();
    }

    /**
     * 执行决策建议
     */
    @Override
    public ExecutionResult executeDecision(StrategyDecisionResult decisionResult, boolean requireConfirmation) {
        // 这里实现决策执行逻辑
        // 暂时返回成功结果
        return new ExecutionResult(true, generateExecutionId(),
                decisionResult.getFundCode(), decisionResult.getFinalSuggestion(),
                decisionResult.getSuggestedAmount(), decisionResult.getSuggestedQuantity(),
                LocalDateTime.now(), "执行成功", null, generateTransactionId());
    }

    /**
     * 批量执行决策建议
     */
    @Override
    public List<ExecutionResult> executeDecisions(List<StrategyDecisionResult> decisionResults, boolean requireConfirmation) {
        return decisionResults.stream()
                .map(result -> executeDecision(result, requireConfirmation))
                .collect(Collectors.toList());
    }

    /**
     * 获取决策引擎版本
     */
    @Override
    public String getVersion() {
        return engineVersion;
    }

    /**
     * 检查决策引擎是否就绪
     */
    @Override
    public boolean isReady() {
        return !ruleConfigs.isEmpty() && engineStartTime > 0;
    }

    // ================ 私有辅助方法 ================

    /**
     * 执行规则匹配
     */
    private void executeRuleMatching(StrategyDecisionResult result, FundInfo fundInfo,
                                     FundDailyData dailyData, FundHolding holding) {
        result.initMatchedRules();

        // 按优先级排序规则
        List<StrategyRuleConfig> sortedRules = ruleConfigs.values().stream()
                .filter(StrategyRuleConfig::isEffective) // 只检查有效的规则
                .sorted(Comparator.comparingInt(StrategyRuleConfig::getPriority))
                .collect(Collectors.toList());

        for (StrategyRuleConfig rule : sortedRules) {
            try {
                RuleMatchResult matchResult = evaluateRule(rule, result, fundInfo, dailyData, holding);
                if (matchResult.isMatched()) {
                    result.addMatchedRule(matchResult);
                    log.debug("规则[{}]匹配成功: {}", rule.getRuleId(), matchResult.getTriggerDescription());
                }
            } catch (Exception e) {
                log.warn("规则[{}]评估失败: {}", rule.getRuleId(), e.getMessage());
            }
        }
    }

    /**
     * 评估单个规则
     */
    private RuleMatchResult evaluateRule(StrategyRuleConfig rule, StrategyDecisionResult result,
                                         FundInfo fundInfo, FundDailyData dailyData, FundHolding holding) {
        long startTime = System.currentTimeMillis();
        RuleMatchResult matchResult = RuleMatchResult.builder()
                .ruleId(rule.getRuleId())
                .ruleType(rule.getRuleType())
                .ruleName(rule.getRuleName())
                .matchTime(LocalDateTime.now())
                .rulePriority(rule.getPriority())
                .ruleDescription(rule.getDescription())
                .ruleConfigSummary(rule.getSummary())
                .build();

        try {
            // 初始化条件检查结果
            matchResult.initConditionResults();

            // 检查各种条件
            boolean allConditionsMet = true;
            BigDecimal confidence = BigDecimal.ONE;

            // 检查日涨跌幅条件
            if (rule.hasCondition("dailyChange") && dailyData != null) {
                BigDecimal dailyChange = dailyData.getDailyChangeRate();
                BigDecimal threshold = rule.getDailyChangeThreshold();
                boolean met = evaluateCondition("dailyChange", dailyChange, threshold);
                matchResult.addConditionResult("dailyChange", met, dailyChange, threshold);
                if (!met) allConditionsMet = false;
                confidence = confidence.multiply(calculateConditionConfidence(dailyChange, threshold));
            }

            // 检查收益率条件
            if (rule.hasCondition("yieldRate") && result.getCurrentYieldRate() != null) {
                BigDecimal yieldRate = result.getCurrentYieldRate();
                BigDecimal threshold = rule.getYieldRateThreshold();
                boolean met = evaluateCondition("yieldRate", yieldRate, threshold);
                matchResult.addConditionResult("yieldRate", met, yieldRate, threshold);
                if (!met) allConditionsMet = false;
                confidence = confidence.multiply(calculateConditionConfidence(yieldRate, threshold));

                // 设置触发值
                if (met) {
                    matchResult.setTriggerValue(yieldRate);
                    matchResult.setTriggerThreshold(threshold);
                    matchResult.calculateTriggerDeviation();
                    matchResult.setTriggerDescription(String.format("收益率 %.2f%% 达到阈值 %.2f%%",
                            yieldRate, threshold));
                    matchResult.setTriggerLevel(calculateTriggerLevel(yieldRate, threshold));
                }
            }

            // 检查收益率范围条件
            if (rule.hasCondition("minYieldRate") && rule.hasCondition("maxYieldRate")
                    && result.getCurrentYieldRate() != null) {
                BigDecimal yieldRate = result.getCurrentYieldRate();
                BigDecimal minThreshold = rule.getMinYieldRateThreshold();
                BigDecimal maxThreshold = rule.getMaxYieldRateThreshold();
                boolean met = yieldRate.compareTo(minThreshold) >= 0 && yieldRate.compareTo(maxThreshold) <= 0;
                matchResult.addConditionResult("yieldRateRange", met, yieldRate,
                        String.format("[%s, %s]", minThreshold, maxThreshold));
                if (!met) allConditionsMet = false;
                confidence = confidence.multiply(calculateRangeConfidence(yieldRate, minThreshold, maxThreshold));
            }

            // 设置匹配结果
            matchResult.setMatched(allConditionsMet);
            matchResult.setAllConditionsMet(allConditionsMet);

            if (allConditionsMet) {
                matchResult.setSuggestion(rule.getSuggestionType());
                // 应用规则权重和置信度阈值
                confidence = confidence.multiply(rule.getConfidenceThreshold() != null ?
                        rule.getConfidenceThreshold() : new BigDecimal("0.7"));
                matchResult.setConfidence(confidence.setScale(4, RoundingMode.HALF_UP));
                matchResult.setMatchScore(matchResult.calculateMatchScore());
            }

            long endTime = System.currentTimeMillis();
            matchResult.setCalculationTimeMs(endTime - startTime);

        } catch (Exception e) {
            matchResult.setMatched(false);
            matchResult.setErrorMessage("规则评估异常: " + e.getMessage());
            matchResult.setExceptionStackTrace(Arrays.toString(e.getStackTrace()));
        }

        return matchResult;
    }

    /**
     * 评估条件
     */
    private boolean evaluateCondition(String conditionName, BigDecimal actualValue, BigDecimal threshold) {
        if (actualValue == null || threshold == null) {
            return false;
        }

        switch (conditionName) {
            case "dailyChange":
            case "yieldRate":
                // 对于负阈值（下跌），实际值应该小于等于阈值
                // 对于正阈值（上涨），实际值应该大于等于阈值
                if (threshold.compareTo(BigDecimal.ZERO) < 0) {
                    return actualValue.compareTo(threshold) <= 0;
                } else {
                    return actualValue.compareTo(threshold) >= 0;
                }
            default:
                return actualValue.compareTo(threshold) >= 0;
        }
    }

    /**
     * 计算条件置信度
     */
    private BigDecimal calculateConditionConfidence(BigDecimal actualValue, BigDecimal threshold) {
        if (actualValue == null || threshold == null) {
            return BigDecimal.ZERO;
        }

        // 计算偏差比例
        BigDecimal deviation;
        if (threshold.compareTo(BigDecimal.ZERO) != 0) {
            deviation = actualValue.subtract(threshold)
                    .divide(threshold.abs(), 4, RoundingMode.HALF_UP)
                    .abs();
        } else {
            deviation = actualValue.abs();
        }

        // 偏差越大，置信度越高（对于阈值条件）
        // 使用sigmoid-like函数：confidence = 1 / (1 + exp(-k * deviation))
        BigDecimal k = new BigDecimal("5"); // 调节系数
        BigDecimal expTerm = BigDecimal.valueOf(Math.exp(-k.multiply(deviation).doubleValue()));
        return BigDecimal.ONE.divide(BigDecimal.ONE.add(expTerm), 4, RoundingMode.HALF_UP);
    }

    /**
     * 计算范围置信度
     */
    private BigDecimal calculateRangeConfidence(BigDecimal actualValue, BigDecimal minThreshold, BigDecimal maxThreshold) {
        if (actualValue == null || minThreshold == null || maxThreshold == null) {
            return BigDecimal.ZERO;
        }

        // 计算到范围边界的距离
        BigDecimal range = maxThreshold.subtract(minThreshold);
        if (range.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 计算到范围中心的距离
        BigDecimal center = minThreshold.add(maxThreshold).divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
        BigDecimal distance = actualValue.subtract(center).abs();

        // 距离越小，置信度越高
        BigDecimal normalizedDistance = distance.divide(range.divide(new BigDecimal("2")), 4, RoundingMode.HALF_UP);
        return BigDecimal.ONE.subtract(normalizedDistance.min(BigDecimal.ONE).max(BigDecimal.ZERO));
    }

    /**
     * 计算触发级别
     */
    private int calculateTriggerLevel(BigDecimal actualValue, BigDecimal threshold) {
        if (actualValue == null || threshold == null) {
            return 1;
        }

        BigDecimal deviation = actualValue.subtract(threshold).abs();
        BigDecimal relativeDeviation = threshold.compareTo(BigDecimal.ZERO) != 0 ?
                deviation.divide(threshold.abs(), 4, RoundingMode.HALF_UP) : deviation;

        if (relativeDeviation.compareTo(new BigDecimal("0.5")) >= 0) {
            return 5; // 严重偏离
        } else if (relativeDeviation.compareTo(new BigDecimal("0.3")) >= 0) {
            return 4; // 较大偏离
        } else if (relativeDeviation.compareTo(new BigDecimal("0.2")) >= 0) {
            return 3; // 中等偏离
        } else if (relativeDeviation.compareTo(new BigDecimal("0.1")) >= 0) {
            return 2; // 轻微偏离
        } else {
            return 1; // 正常偏离
        }
    }

    /**
     * 计算最终建议和置信度
     */
    private void calculateFinalSuggestion(StrategyDecisionResult result) {
        if (!result.hasTriggeredRules()) {
            // 没有触发任何规则，建议持有
            result.setFinalSuggestion(SuggestionType.HOLD);
            result.setFinalConfidence(new BigDecimal("0.5"));
            return;
        }

        // 获取所有匹配的规则
        List<RuleMatchResult> matchedRules = result.getMatchedRules();

        // 按优先级排序（优先级数字越小，优先级越高）
        matchedRules.sort(Comparator.comparingInt(RuleMatchResult::getExecutionPriority));

        // 选择最高优先级的建议
        RuleMatchResult topRule = matchedRules.get(0);
        result.setFinalSuggestion(topRule.getSuggestion());
        result.setFinalConfidence(topRule.getConfidence());

        // 计算平均置信度
        BigDecimal avgConfidence = result.calculateAverageConfidence();
        result.setFinalConfidence(avgConfidence.max(topRule.getConfidence()));

        // 设置建议执行参数
        StrategyRuleConfig ruleConfig = getStrategyRuleById(topRule.getRuleId());
        if (ruleConfig != null) {
            result.setSuggestedRatio(ruleConfig.getExecutionRatio());
            result.setSuggestedExecutionTime(LocalDateTime.now());
            result.setSuggestionValidityHours(24); // 默认24小时有效

            // 如果有持仓，计算建议执行金额和数量
            if (result.getHoldingValue() != null && ruleConfig.getExecutionRatio() != null) {
                BigDecimal suggestedValue = result.getHoldingValue()
                        .multiply(ruleConfig.getExecutionRatio())
                        .setScale(2, RoundingMode.HALF_UP);
                result.setSuggestedAmount(suggestedValue);

                if (result.getCurrentPrice() != null && result.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal suggestedQuantity = suggestedValue
                            .divide(result.getCurrentPrice(), 4, RoundingMode.HALF_UP);
                    result.setSuggestedQuantity(suggestedQuantity);
                }
            }
        }

        // 设置风险等级
        result.setRiskLevel(calculateRiskLevel(matchedRules));
        result.setRiskMessage(generateRiskMessage(matchedRules));
        result.setRiskScore(calculateRiskScore(matchedRules));
    }

    /**
     * 计算风险等级
     */
    private int calculateRiskLevel(List<RuleMatchResult> matchedRules) {
        int maxRiskLevel = 1;
        for (RuleMatchResult rule : matchedRules) {
            if (rule.getTriggerLevel() != null && rule.getTriggerLevel() > maxRiskLevel) {
                maxRiskLevel = rule.getTriggerLevel();
            }
        }
        return maxRiskLevel;
    }

    /**
     * 生成风险提示信息
     */
    private String generateRiskMessage(List<RuleMatchResult> matchedRules) {
        if (matchedRules.isEmpty()) {
            return "无显著风险";
        }

        List<String> riskMessages = new ArrayList<>();
        for (RuleMatchResult rule : matchedRules) {
            if (rule.getTriggerDescription() != null) {
                riskMessages.add(rule.getTriggerDescription());
            }
        }

        if (riskMessages.isEmpty()) {
            return "检测到规则触发，但无具体风险描述";
        }

        return String.join("; ", riskMessages);
    }

    /**
     * 计算风险得分
     */
    private int calculateRiskScore(List<RuleMatchResult> matchedRules) {
        int score = 50; // 基础分

        for (RuleMatchResult rule : matchedRules) {
            if (rule.getTriggerLevel() != null) {
                score += (rule.getTriggerLevel() - 3) * 10; // 每级偏离基础分加减10分
            }
            if (rule.getConfidence() != null && rule.getConfidence().compareTo(new BigDecimal("0.8")) > 0) {
                score += 5; // 高置信度增加风险得分
            }
        }

        return Math.min(Math.max(score, 0), 100);
    }

    /**
     * 保存决策日志
     */
    private void saveDecisionLog(StrategyDecisionResult result) {
        try {
            FundStrategyLog logEntry = new FundStrategyLog();
            logEntry.setFundCode(result.getFundCode());
            logEntry.setTradeDate(LocalDate.now()); // 设置交易日为今天

            // 设置建议类型（使用suggestion字段）
            logEntry.setSuggestion(result.getFinalSuggestion());

            // 设置收益率和涨跌幅（如果有）
            if (result.getCurrentYieldRate() != null) {
                logEntry.setYieldRate(result.getCurrentYieldRate());
            }
            if (result.getDailyChange() != null) {
                logEntry.setDailyChange(result.getDailyChange());
            }

            // 设置置信度
            if (result.getFinalConfidence() != null) {
                logEntry.setConfidence(result.getFinalConfidence());
            }

            // 设置当前价格（如果有）
            if (result.getCurrentPrice() != null) {
                logEntry.setCurrentPrice(result.getCurrentPrice());
            }

            // 设置持仓成本（如果有持仓信息）
            if (result.getHoldingCost() != null) {
                logEntry.setCostPrice(result.getHoldingCost());
            }

            // 设置建议原因（使用规则名称）
            if (result.getMatchedRules() != null && !result.getMatchedRules().isEmpty()) {
                StringBuilder reason = new StringBuilder();
                for (int i = 0; i < Math.min(result.getMatchedRules().size(), 3); i++) {
                    if (i > 0) reason.append("; ");
                    reason.append(result.getMatchedRules().get(i).getRuleName());
                }
                logEntry.setSuggestionReason(reason.toString());
                logEntry.setTriggeredRule(result.getMatchedRules().get(0).getRuleName());
            }

            strategyLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("保存决策日志失败: {}", e.getMessage());
        }
    }

    /**
     * 生成决策ID
     */
    private String generateDecisionId() {
        return "DEC_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 生成规则ID
     */
    private String generateRuleId() {
        return "RULE_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * 生成执行ID
     */
    private String generateExecutionId() {
        return "EXEC_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 生成交易ID
     */
    private String generateTransactionId() {
        return "TX_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 计算平均决策时间
     */
    private long calculateAverageDecisionTime() {
        // 这里实现平均决策时间计算逻辑
        // 暂时返回固定值
        return 50L;
    }

    /**
     * 定时清理缓存任务
     */
    @Scheduled(cron = "0 0 3 * * ?") // 每天凌晨3点执行
    public void scheduledCacheCleanup() {
        log.info("开始定时清理策略决策缓存...");
        clearCache("fund");
        log.info("策略决策缓存清理完成");
    }

    /**
     * 初始化引擎
     */
    @jakarta.annotation.PostConstruct
    public void init() {
        engineStartTime = System.currentTimeMillis();
        reloadStrategyRules();
        log.info("策略决策引擎初始化完成，版本: {}，加载规则: {} 条", engineVersion, ruleConfigs.size());
    }
}