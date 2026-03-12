package com.shxc.fundagent.strategy;

import com.shxc.fundagent.entity.FundHolding;
import com.shxc.fundagent.entity.FundInfo;
import com.shxc.fundagent.enums.SuggestionType;
import com.shxc.fundagent.strategy.model.StrategyDecisionResult;
import com.shxc.fundagent.strategy.model.StrategyRuleConfig;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 策略决策引擎接口
 * 负责执行策略规则并生成投资建议
 */
public interface StrategyDecisionEngine {

    /**
     * 对单只基金进行策略决策
     *
     * @param fundCode 基金代码
     * @return 策略决策结果
     */
    StrategyDecisionResult decideForFund(String fundCode);

    /**
     * 对持仓基金进行策略决策
     *
     * @param holding 持仓信息
     * @return 策略决策结果
     */
    StrategyDecisionResult decideForHolding(FundHolding holding);

    /**
     * 对基金信息进行策略决策
     *
     * @param fundInfo 基金信息
     * @return 策略决策结果
     */
    StrategyDecisionResult decideForFundInfo(FundInfo fundInfo);

    /**
     * 批量对多只基金进行策略决策
     *
     * @param fundCodes 基金代码列表
     * @return 决策结果映射表 key: 基金代码, value: 决策结果
     */
    Map<String, StrategyDecisionResult> decideForFunds(List<String> fundCodes);

    /**
     * 批量对多个持仓进行策略决策
     *
     * @param holdings 持仓列表
     * @return 决策结果映射表 key: 持仓ID, value: 决策结果
     */
    Map<Long, StrategyDecisionResult> decideForHoldings(List<FundHolding> holdings);

    /**
     * 获取所有可用的策略规则配置
     *
     * @return 策略规则配置列表
     */
    List<StrategyRuleConfig> getAllStrategyRules();

    /**
     * 根据规则类型获取策略规则配置
     *
     * @param ruleType 规则类型
     * @return 策略规则配置
     */
    StrategyRuleConfig getStrategyRule(StrategyRuleType ruleType);

    /**
     * 根据规则ID获取策略规则配置
     *
     * @param ruleId 规则ID
     * @return 策略规则配置
     */
    StrategyRuleConfig getStrategyRuleById(String ruleId);

    /**
     * 启用或禁用策略规则
     *
     * @param ruleId  规则ID
     * @param enabled 是否启用
     * @return 更新后的规则配置
     */
    StrategyRuleConfig updateRuleStatus(String ruleId, boolean enabled);

    /**
     * 更新策略规则配置
     *
     * @param ruleConfig 规则配置
     * @return 更新后的规则配置
     */
    StrategyRuleConfig updateStrategyRule(StrategyRuleConfig ruleConfig);

    /**
     * 添加新的策略规则
     *
     * @param ruleConfig 规则配置
     * @return 添加后的规则配置
     */
    StrategyRuleConfig addStrategyRule(StrategyRuleConfig ruleConfig);

    /**
     * 删除策略规则
     *
     * @param ruleId 规则ID
     * @return 是否删除成功
     */
    boolean deleteStrategyRule(String ruleId);

    /**
     * 重新加载策略规则配置
     */
    void reloadStrategyRules();

    /**
     * 获取决策引擎状态
     *
     * @return 引擎状态信息
     */
    EngineStatus getEngineStatus();

    /**
     * 清理决策缓存
     *
     * @param cacheType 缓存类型（fund, holding, all）
     */
    void clearCache(String cacheType);

    /**
     * 获取决策统计信息
     *
     * @return 统计信息
     */
    DecisionStatistics getDecisionStatistics();

    /**
     * 执行决策建议（如果配置为自动执行）
     *
     * @param decisionResult 决策结果
     * @param requireConfirmation 是否需要确认
     * @return 执行结果
     */
    ExecutionResult executeDecision(StrategyDecisionResult decisionResult, boolean requireConfirmation);

    /**
     * 批量执行决策建议
     *
     * @param decisionResults 决策结果列表
     * @param requireConfirmation 是否需要确认
     * @return 执行结果列表
     */
    List<ExecutionResult> executeDecisions(List<StrategyDecisionResult> decisionResults, boolean requireConfirmation);

    /**
     * 获取决策引擎版本
     *
     * @return 版本号
     */
    String getVersion();

    /**
     * 检查决策引擎是否就绪
     *
     * @return 是否就绪
     */
    boolean isReady();

    /**
     * 引擎状态类
     */
    class EngineStatus {
        private boolean running;
        private long totalDecisions;
        private long successfulDecisions;
        private long failedDecisions;
        private long cachedDecisions;
        private long averageDecisionTimeMs;
        private String lastDecisionTime;
        private int activeRuleCount;
        private int totalRuleCount;
        private String engineVersion;
        private long uptimeSeconds;

        // 构造器
        public EngineStatus() {
        }

        public EngineStatus(boolean running, long totalDecisions, long successfulDecisions,
                           long failedDecisions, long cachedDecisions, long averageDecisionTimeMs,
                           String lastDecisionTime, int activeRuleCount, int totalRuleCount,
                           String engineVersion, long uptimeSeconds) {
            this.running = running;
            this.totalDecisions = totalDecisions;
            this.successfulDecisions = successfulDecisions;
            this.failedDecisions = failedDecisions;
            this.cachedDecisions = cachedDecisions;
            this.averageDecisionTimeMs = averageDecisionTimeMs;
            this.lastDecisionTime = lastDecisionTime;
            this.activeRuleCount = activeRuleCount;
            this.totalRuleCount = totalRuleCount;
            this.engineVersion = engineVersion;
            this.uptimeSeconds = uptimeSeconds;
        }

        // Getter 和 Setter 方法
        public boolean isRunning() { return running; }
        public void setRunning(boolean running) { this.running = running; }

        public long getTotalDecisions() { return totalDecisions; }
        public void setTotalDecisions(long totalDecisions) { this.totalDecisions = totalDecisions; }

        public long getSuccessfulDecisions() { return successfulDecisions; }
        public void setSuccessfulDecisions(long successfulDecisions) { this.successfulDecisions = successfulDecisions; }

        public long getFailedDecisions() { return failedDecisions; }
        public void setFailedDecisions(long failedDecisions) { this.failedDecisions = failedDecisions; }

        public long getCachedDecisions() { return cachedDecisions; }
        public void setCachedDecisions(long cachedDecisions) { this.cachedDecisions = cachedDecisions; }

        public long getAverageDecisionTimeMs() { return averageDecisionTimeMs; }
        public void setAverageDecisionTimeMs(long averageDecisionTimeMs) { this.averageDecisionTimeMs = averageDecisionTimeMs; }

        public String getLastDecisionTime() { return lastDecisionTime; }
        public void setLastDecisionTime(String lastDecisionTime) { this.lastDecisionTime = lastDecisionTime; }

        public int getActiveRuleCount() { return activeRuleCount; }
        public void setActiveRuleCount(int activeRuleCount) { this.activeRuleCount = activeRuleCount; }

        public int getTotalRuleCount() { return totalRuleCount; }
        public void setTotalRuleCount(int totalRuleCount) { this.totalRuleCount = totalRuleCount; }

        public String getEngineVersion() { return engineVersion; }
        public void setEngineVersion(String engineVersion) { this.engineVersion = engineVersion; }

        public long getUptimeSeconds() { return uptimeSeconds; }
        public void setUptimeSeconds(long uptimeSeconds) { this.uptimeSeconds = uptimeSeconds; }
    }

    /**
     * 决策统计类
     */
    class DecisionStatistics {
        private Map<SuggestionType, Long> suggestionCounts;
        private Map<StrategyRuleType, Long> ruleTriggerCounts;
        private double averageConfidence;
        private double averageDecisionTimeMs;
        private int totalDecisions;
        private int successfulDecisions;
        private int failedDecisions;
        private String statisticsPeriod;
        private LocalDateTime periodStart;
        private LocalDateTime periodEnd;

        // 构造器
        public DecisionStatistics() {
        }

        public DecisionStatistics(Map<SuggestionType, Long> suggestionCounts,
                                 Map<StrategyRuleType, Long> ruleTriggerCounts,
                                 double averageConfidence, double averageDecisionTimeMs,
                                 int totalDecisions, int successfulDecisions, int failedDecisions,
                                 String statisticsPeriod, LocalDateTime periodStart, LocalDateTime periodEnd) {
            this.suggestionCounts = suggestionCounts;
            this.ruleTriggerCounts = ruleTriggerCounts;
            this.averageConfidence = averageConfidence;
            this.averageDecisionTimeMs = averageDecisionTimeMs;
            this.totalDecisions = totalDecisions;
            this.successfulDecisions = successfulDecisions;
            this.failedDecisions = failedDecisions;
            this.statisticsPeriod = statisticsPeriod;
            this.periodStart = periodStart;
            this.periodEnd = periodEnd;
        }

        // Getter 和 Setter 方法
        public Map<SuggestionType, Long> getSuggestionCounts() { return suggestionCounts; }
        public void setSuggestionCounts(Map<SuggestionType, Long> suggestionCounts) { this.suggestionCounts = suggestionCounts; }

        public Map<StrategyRuleType, Long> getRuleTriggerCounts() { return ruleTriggerCounts; }
        public void setRuleTriggerCounts(Map<StrategyRuleType, Long> ruleTriggerCounts) { this.ruleTriggerCounts = ruleTriggerCounts; }

        public double getAverageConfidence() { return averageConfidence; }
        public void setAverageConfidence(double averageConfidence) { this.averageConfidence = averageConfidence; }

        public double getAverageDecisionTimeMs() { return averageDecisionTimeMs; }
        public void setAverageDecisionTimeMs(double averageDecisionTimeMs) { this.averageDecisionTimeMs = averageDecisionTimeMs; }

        public int getTotalDecisions() { return totalDecisions; }
        public void setTotalDecisions(int totalDecisions) { this.totalDecisions = totalDecisions; }

        public int getSuccessfulDecisions() { return successfulDecisions; }
        public void setSuccessfulDecisions(int successfulDecisions) { this.successfulDecisions = successfulDecisions; }

        public int getFailedDecisions() { return failedDecisions; }
        public void setFailedDecisions(int failedDecisions) { this.failedDecisions = failedDecisions; }

        public String getStatisticsPeriod() { return statisticsPeriod; }
        public void setStatisticsPeriod(String statisticsPeriod) { this.statisticsPeriod = statisticsPeriod; }

        public LocalDateTime getPeriodStart() { return periodStart; }
        public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }

        public LocalDateTime getPeriodEnd() { return periodEnd; }
        public void setPeriodEnd(LocalDateTime periodEnd) { this.periodEnd = periodEnd; }
    }

    /**
     * 执行结果类
     */
    class ExecutionResult {
        private boolean success;
        private String executionId;
        private String fundCode;
        private SuggestionType suggestion;
        private BigDecimal executedAmount;
        private BigDecimal executedQuantity;
        private LocalDateTime executionTime;
        private String executionMessage;
        private String errorMessage;
        private String transactionId;

        // 构造器
        public ExecutionResult() {
        }

        public ExecutionResult(boolean success, String executionId, String fundCode,
                              SuggestionType suggestion, BigDecimal executedAmount,
                              BigDecimal executedQuantity, LocalDateTime executionTime,
                              String executionMessage, String errorMessage, String transactionId) {
            this.success = success;
            this.executionId = executionId;
            this.fundCode = fundCode;
            this.suggestion = suggestion;
            this.executedAmount = executedAmount;
            this.executedQuantity = executedQuantity;
            this.executionTime = executionTime;
            this.executionMessage = executionMessage;
            this.errorMessage = errorMessage;
            this.transactionId = transactionId;
        }

        // Getter 和 Setter 方法
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getExecutionId() { return executionId; }
        public void setExecutionId(String executionId) { this.executionId = executionId; }

        public String getFundCode() { return fundCode; }
        public void setFundCode(String fundCode) { this.fundCode = fundCode; }

        public SuggestionType getSuggestion() { return suggestion; }
        public void setSuggestion(SuggestionType suggestion) { this.suggestion = suggestion; }

        public BigDecimal getExecutedAmount() { return executedAmount; }
        public void setExecutedAmount(BigDecimal executedAmount) { this.executedAmount = executedAmount; }

        public BigDecimal getExecutedQuantity() { return executedQuantity; }
        public void setExecutedQuantity(BigDecimal executedQuantity) { this.executedQuantity = executedQuantity; }

        public LocalDateTime getExecutionTime() { return executionTime; }
        public void setExecutionTime(LocalDateTime executionTime) { this.executionTime = executionTime; }

        public String getExecutionMessage() { return executionMessage; }
        public void setExecutionMessage(String executionMessage) { this.executionMessage = executionMessage; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public String getTransactionId() { return transactionId; }
        public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    }
}