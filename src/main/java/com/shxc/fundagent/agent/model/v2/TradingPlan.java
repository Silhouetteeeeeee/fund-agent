package com.shxc.fundagent.agent.model.v2;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 交易计划
 * 包含具体的交易指令、执行策略和风险管理
 */
@Data
public class TradingPlan {

    /**
     * 交易计划状态
     */
    public enum PlanStatus {
        /** 草稿 */
        DRAFT,
        /** 待审核 */
        PENDING_REVIEW,
        /** 已批准 */
        APPROVED,
        /** 已拒绝 */
        REJECTED,
        /** 执行中 */
        EXECUTING,
        /** 部分完成 */
        PARTIALLY_COMPLETED,
        /** 已完成 */
        COMPLETED,
        /** 已取消 */
        CANCELLED,
        /** 执行失败 */
        FAILED
    }

    /**
     * 交易计划类型
     */
    public enum PlanType {
        /** 买入计划 */
        BUY_PLAN,
        /** 卖出计划 */
        SELL_PLAN,
        /** 调仓计划 */
        REBALANCE_PLAN,
        /** 对冲计划 */
        HEDGE_PLAN,
        /** 套利计划 */
        ARBITRAGE_PLAN,
        /** 止损计划 */
        STOP_LOSS_PLAN,
        /** 止盈计划 */
        TAKE_PROFIT_PLAN,
        /** 定投计划 */
        REGULAR_INVESTMENT_PLAN
    }

    /**
     * 交易执行策略
     */
    public enum ExecutionStrategy {
        /** 市价单 */
        MARKET_ORDER,
        /** 限价单 */
        LIMIT_ORDER,
        /** 条件单 */
        CONDITIONAL_ORDER,
        /** 分批执行 */
        BATCH_EXECUTION,
        /** 时间加权平均价格 */
        TWAP,
        /** 成交量加权平均价格 */
        VWAP,
        /** 智能路由 */
        SMART_ROUTING
    }

    /**
     * 风险管理级别
     */
    public enum RiskLevel {
        /** 保守型 */
        CONSERVATIVE(0.2),
        /** 稳健型 */
        MODERATE(0.4),
        /** 平衡型 */
        BALANCED(0.6),
        /** 进取型 */
        AGGRESSIVE(0.8),
        /** 激进型 */
        VERY_AGGRESSIVE(1.0);

        private final double riskTolerance;

        RiskLevel(double riskTolerance) {
            this.riskTolerance = riskTolerance;
        }

        public double getRiskTolerance() {
            return riskTolerance;
        }
    }

    // 基本信息
    private String planId;
    private String planName;
    private String description;
    private PlanType planType;
    private PlanStatus status = PlanStatus.DRAFT;
    private RiskLevel riskLevel = RiskLevel.BALANCED;

    // 创建和更新信息
    private String createdBy; // Agent名称或用户ID
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    // 审核信息
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String reviewComments;
    private String approvalStatus; // AUTO_APPROVED, MANUAL_APPROVED, REJECTED

    // 执行信息
    private String executedBy;
    private LocalDateTime executionStartTime;
    private LocalDateTime executionEndTime;
    private ExecutionStrategy executionStrategy = ExecutionStrategy.MARKET_ORDER;

    // 时间安排
    private LocalDateTime scheduledStartTime;
    private LocalDateTime scheduledEndTime;
    private String timeHorizon; // INTRADAY, SHORT_TERM, MEDIUM_TERM, LONG_TERM

    // 交易指令列表
    private List<TradeOrder> tradeOrders = new ArrayList<>();

    // 资金分配
    private FundAllocation fundAllocation;

    // 风险管理
    private RiskManagement riskManagement;

    // 业绩目标
    private PerformanceTarget performanceTarget;

    // 监控条件
    private MonitoringConditions monitoringConditions;

    // 执行结果
    private ExecutionResult executionResult;

    // 关联的分析上下文
    private String analysisContextId; // 关联的MarketContext或NewsContext的ID
    private Map<String, Object> analysisContextSnapshot = new HashMap<>();

    // 元数据
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 交易指令
     */
    @Data
    public static class TradeOrder {
        private String orderId;
        private int orderSequence; // 执行顺序
        private String assetType; // FUND, STOCK, BOND, etc.
        private String assetCode; // 基金代码、股票代码等
        private String assetName;

        // 交易方向
        private String direction; // BUY, SELL
        private BigDecimal quantity; // 交易数量（份额）
        private BigDecimal amount; // 交易金额

        // 价格信息
        private BigDecimal targetPrice; // 目标价格
        private BigDecimal currentPrice; // 当前价格
        private BigDecimal priceTolerance = BigDecimal.valueOf(0.02); // 价格容忍度 ±2%

        // 执行条件
        private ExecutionCondition executionCondition;

        // 风险管理
        private OrderRiskManagement orderRiskManagement;

        // 状态跟踪
        private String orderStatus = "PENDING"; // PENDING, EXECUTING, PARTIALLY_FILLED, FILLED, CANCELLED, FAILED
        private BigDecimal filledQuantity = BigDecimal.ZERO;
        private BigDecimal filledAmount = BigDecimal.ZERO;
        private BigDecimal averagePrice = BigDecimal.ZERO;
        private List<ExecutionRecord> executionRecords = new ArrayList<>();

        // 分析依据
        private String rationale;
        private BigDecimal confidence = BigDecimal.ZERO;
        private List<String> supportingAnalysisIds = new ArrayList<>();

        /**
         * 执行条件
         */
        @Data
        public static class ExecutionCondition {
            private String conditionType; // PRICE, TIME, MARKET_CONDITION, COMBINATION
            private Map<String, Object> conditionParameters = new HashMap<>();
            private LocalDateTime validFrom;
            private LocalDateTime validUntil;
            private boolean isMandatory = true; // 是否必须满足条件才能执行
        }

        /**
         * 订单风险管理
         */
        @Data
        public static class OrderRiskManagement {
            private BigDecimal stopLossPrice; // 止损价格
            private BigDecimal takeProfitPrice; // 止盈价格
            private BigDecimal maxLossAmount; // 最大亏损金额
            private BigDecimal maxLossPercent; // 最大亏损百分比
            private String contingencyPlan; // 应急计划
        }

        /**
         * 执行记录
         */
        @Data
        public static class ExecutionRecord {
            private String executionId;
            private LocalDateTime executionTime;
            private BigDecimal executedQuantity;
            private BigDecimal executedPrice;
            private BigDecimal executedAmount;
            private BigDecimal commission;
            private String executionVenue; // 执行场所
            private String executionReference; // 执行参考号
            private String status; // SUCCESS, PARTIAL, FAILED
            private String remarks;
        }
    }

    /**
     * 资金分配
     */
    @Data
    public static class FundAllocation {
        private BigDecimal totalCapital; // 总资金
        private BigDecimal allocatedCapital = BigDecimal.ZERO; // 已分配资金
        private BigDecimal reservedCapital = BigDecimal.ZERO; // 预留资金

        // 按资产类别分配
        private Map<String, BigDecimal> allocationByAssetClass = new HashMap<>();

        // 资金使用计划
        private List<CapitalUsage> capitalUsagePlan = new ArrayList<>();

        // 资金来源
        private String fundingSource; // CASH, MARGIN, LOAN, etc.
        private BigDecimal fundingCost = BigDecimal.ZERO; // 资金成本（利率）

        /**
         * 资金使用详情
         */
        @Data
        public static class CapitalUsage {
            private String usageId;
            private String purpose; // TRADE, FEE, RESERVE, HEDGE
            private BigDecimal amount;
            private LocalDateTime scheduledTime;
            private String priority; // HIGH, MEDIUM, LOW
        }
    }

    /**
     * 风险管理
     */
    @Data
    public static class RiskManagement {
        // 风险限制
        private BigDecimal maxTotalRisk = BigDecimal.valueOf(10000); // 最大总风险敞口
        private BigDecimal maxPositionRisk = BigDecimal.valueOf(5000); // 最大单笔风险
        private BigDecimal maxDrawdownLimit = BigDecimal.valueOf(0.1); // 最大回撤限制 10%

        // 波动率控制
        private BigDecimal maxPortfolioVolatility = BigDecimal.valueOf(0.2); // 最大组合波动率 20%
        private BigDecimal targetVolatility = BigDecimal.valueOf(0.15); // 目标波动率 15%

        // 相关性控制
        private BigDecimal maxAssetCorrelation = BigDecimal.valueOf(0.8); // 最大资产相关性
        private BigDecimal minDiversificationScore = BigDecimal.valueOf(0.6); // 最小分散化评分

        // 流动性风险
        private BigDecimal minLiquidityRatio = BigDecimal.valueOf(0.3); // 最小流动性比率
        private BigDecimal maxIlliquidAllocation = BigDecimal.valueOf(0.2); // 最大非流动性资产配置

        // 压力测试
        private StressTestScenario stressTest;

        // 风险监控指标
        private List<RiskMetric> riskMetrics = new ArrayList<>();

        /**
         * 压力测试场景
         */
        @Data
        public static class StressTestScenario {
            private String scenarioName;
            private String description;
            private BigDecimal marketDownside = BigDecimal.valueOf(0.2); // 市场下跌20%
            private BigDecimal interestRateShock = BigDecimal.valueOf(0.02); // 利率冲击2%
            private BigDecimal liquidityShock = BigDecimal.valueOf(0.3); // 流动性冲击30%
            private BigDecimal estimatedLoss = BigDecimal.ZERO; // 预估损失
            private boolean isAcceptable = true; // 风险是否可接受
        }

        /**
         * 风险指标
         */
        @Data
        public static class RiskMetric {
            private String metricName; // VAR, CVAR, SHARPE_RATIO, etc.
            private BigDecimal currentValue;
            private BigDecimal targetValue;
            private BigDecimal thresholdValue;
            private String status; // WITHIN_LIMIT, WARNING, EXCEEDED
        }
    }

    /**
     * 业绩目标
     */
    @Data
    public static class PerformanceTarget {
        // 收益目标
        private BigDecimal targetReturn = BigDecimal.valueOf(0.1); // 目标收益率 10%
        private BigDecimal minAcceptableReturn = BigDecimal.valueOf(0.05); // 最低可接受收益率 5%
        private BigDecimal maxExpectedReturn = BigDecimal.valueOf(0.15); // 最大期望收益率 15%

        // 风险调整后收益
        private BigDecimal targetSharpeRatio = BigDecimal.valueOf(1.5); // 目标夏普比率
        private BigDecimal targetSortinoRatio = BigDecimal.valueOf(2.0); // 目标索提诺比率

        // 相对基准
        private String benchmark; // 基准指数
        private BigDecimal targetAlpha = BigDecimal.valueOf(0.03); // 目标阿尔法 3%
        private BigDecimal targetTrackingError = BigDecimal.valueOf(0.05); // 目标跟踪误差 5%

        // 时间框架
        private String evaluationPeriod; // 评估周期：DAILY, WEEKLY, MONTHLY, QUARTERLY, YEARLY
        private LocalDateTime targetCompletionDate;

        // 里程碑
        private List<Milestone> milestones = new ArrayList<>();

        /**
         * 里程碑
         */
        @Data
        public static class Milestone {
            private String milestoneId;
            private String description;
            private LocalDateTime targetDate;
            private BigDecimal targetValue;
            private BigDecimal currentValue = BigDecimal.ZERO;
            private String status; // NOT_STARTED, IN_PROGRESS, ACHIEVED, DELAYED
        }
    }

    /**
     * 监控条件
     */
    @Data
    public static class MonitoringConditions {
        // 价格监控
        private List<PriceAlert> priceAlerts = new ArrayList<>();

        // 市场条件监控
        private List<MarketConditionAlert> marketConditionAlerts = new ArrayList<>();

        // 时间监控
        private List<TimeBasedAlert> timeBasedAlerts = new ArrayList<>();

        // 自动调整规则
        private List<AutoAdjustmentRule> autoAdjustmentRules = new ArrayList<>();

        /**
         * 价格提醒
         */
        @Data
        public static class PriceAlert {
            private String alertId;
            private String assetCode;
            private String condition; // ABOVE, BELOW, CROSS_ABOVE, CROSS_BELOW
            private BigDecimal triggerPrice;
            private String action; // NOTIFY, ADJUST_ORDER, CANCEL_ORDER, EXECUTE_HEDGE
        }

        /**
         * 市场条件提醒
         */
        @Data
        public static class MarketConditionAlert {
            private String alertId;
            private String conditionType; // VOLATILITY, VOLUME, SENTIMENT, etc.
            private String condition;
            private BigDecimal triggerValue;
            private String action;
        }

        /**
         * 时间提醒
         */
        @Data
        public static class TimeBasedAlert {
            private String alertId;
            private LocalDateTime triggerTime;
            private String action;
        }

        /**
         * 自动调整规则
         */
        @Data
        public static class AutoAdjustmentRule {
            private String ruleId;
            private String condition;
            private String adjustmentType; // MODIFY_QUANTITY, MODIFY_PRICE, CANCEL_ORDER, ADD_ORDER
            private Map<String, Object> adjustmentParameters = new HashMap<>();
        }
    }

    /**
     * 执行结果
     */
    @Data
    public static class ExecutionResult {
        // 执行摘要
        private ExecutionSummary summary;

        // 详细执行记录
        private List<TradeOrder.ExecutionRecord> detailedRecords = new ArrayList<>();

        // 业绩分析
        private PerformanceAnalysis performanceAnalysis;

        // 风险评估
        private RiskAssessment riskAssessment;

        // 经验教训
        private List<LessonLearned> lessonsLearned = new ArrayList<>();

        // 建议和改进
        private List<Recommendation> recommendations = new ArrayList<>();

        /**
         * 执行摘要
         */
        @Data
        public static class ExecutionSummary {
            private int totalOrders;
            private int completedOrders;
            private int partiallyCompletedOrders;
            private int failedOrders;
            private int cancelledOrders;

            private BigDecimal totalPlannedAmount;
            private BigDecimal totalExecutedAmount;
            private BigDecimal totalCommission;

            private BigDecimal completionRate = BigDecimal.ZERO; // 完成率
            private BigDecimal averageExecutionPriceDeviation = BigDecimal.ZERO; // 平均执行价格偏差

            private LocalDateTime executionStartTime;
            private LocalDateTime executionEndTime;
            private long executionDurationSeconds;
        }

        /**
         * 业绩分析
         */
        @Data
        public static class PerformanceAnalysis {
            private BigDecimal totalReturn = BigDecimal.ZERO;
            private BigDecimal returnVsTarget = BigDecimal.ZERO;
            private BigDecimal returnVsBenchmark = BigDecimal.ZERO;

            private BigDecimal realizedProfit = BigDecimal.ZERO;
            private BigDecimal unrealizedProfit = BigDecimal.ZERO;

            private BigDecimal sharpeRatio = BigDecimal.ZERO;
            private BigDecimal sortinoRatio = BigDecimal.ZERO;

            private BigDecimal maximumDrawdown = BigDecimal.ZERO;
            private BigDecimal volatility = BigDecimal.ZERO;

            private String performanceRating; // EXCELLENT, GOOD, FAIR, POOR
        }

        /**
         * 风险评估
         */
        @Data
        public static class RiskAssessment {
            private BigDecimal actualRiskExposure = BigDecimal.ZERO;
            private BigDecimal riskVsLimit = BigDecimal.ZERO;
            private BigDecimal var95 = BigDecimal.ZERO; // 95%置信度的VaR
            private BigDecimal cvar95 = BigDecimal.ZERO; // 95%置信度的CVaR

            private List<RiskEvent> riskEvents = new ArrayList<>();

            @Data
            public static class RiskEvent {
                private String eventId;
                private String eventType;
                private String description;
                private String severity; // LOW, MEDIUM, HIGH, CRITICAL
                private String impact; // NEGLIGIBLE, MINOR, MODERATE, MAJOR, SEVERE
                private String responseAction;
            }
        }

        /**
         * 经验教训
         */
        @Data
        public static class LessonLearned {
            private String lessonId;
            private String category; // EXECUTION, RISK_MANAGEMENT, MARKET_ANALYSIS, etc.
            private String description;
            private String impact; // POSITIVE, NEGATIVE, NEUTRAL
            private List<String> actionItems = new ArrayList<>();
        }

        /**
         * 建议
         */
        @Data
        public static class Recommendation {
            private String recommendationId;
            private String type; // PROCESS_IMPROVEMENT, RISK_MANAGEMENT, TOOL_ENHANCEMENT, etc.
            private String description;
            private String priority; // HIGH, MEDIUM, LOW
            private String suggestedAction;
            private BigDecimal estimatedImpact = BigDecimal.ZERO;
        }
    }

    /**
     * 添加交易指令
     */
    public void addTradeOrder(TradeOrder order) {
        if (order != null) {
            if (order.getOrderSequence() <= 0) {
                order.setOrderSequence(tradeOrders.size() + 1);
            }
            tradeOrders.add(order);

            // 更新已分配资金
            if (fundAllocation != null && order.getAmount() != null) {
                fundAllocation.setAllocatedCapital(
                        fundAllocation.getAllocatedCapital().add(order.getAmount())
                );
            }
        }
    }

    /**
     * 获取计划总金额
     */
    public BigDecimal getTotalPlanAmount() {
        return tradeOrders.stream()
                .map(TradeOrder::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取已完成金额
     */
    public BigDecimal getCompletedAmount() {
        return tradeOrders.stream()
                .map(order -> order.getFilledAmount() != null ? order.getFilledAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 计算完成率
     */
    public BigDecimal getCompletionRate() {
        BigDecimal totalAmount = getTotalPlanAmount();
        if (totalAmount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal completedAmount = getCompletedAmount();
        return completedAmount.divide(totalAmount, 4, BigDecimal.ROUND_HALF_UP);
    }

    /**
     * 获取计划简要信息
     */
    public String getPlanSummary() {
        return String.format("[%s] %s - 状态: %s, 类型: %s, 总金额: %s, 完成率: %.1f%%",
                planId, planName, status, planType, getTotalPlanAmount(),
                getCompletionRate().multiply(BigDecimal.valueOf(100)).doubleValue());
    }

    /**
     * 检查计划是否可执行
     */
    public List<String> validateForExecution() {
        List<String> errors = new ArrayList<>();

        if (status != PlanStatus.APPROVED) {
            errors.add("计划状态不是已批准，当前状态: " + status);
        }

        if (tradeOrders.isEmpty()) {
            errors.add("没有交易指令");
        }

        if (scheduledStartTime != null && LocalDateTime.now().isBefore(scheduledStartTime)) {
            errors.add("计划执行时间未到");
        }

        if (scheduledEndTime != null && LocalDateTime.now().isAfter(scheduledEndTime)) {
            errors.add("计划已超过执行截止时间");
        }

        // 检查资金是否充足
        if (fundAllocation != null) {
            BigDecimal totalNeeded = getTotalPlanAmount();
            if (totalNeeded.compareTo(fundAllocation.getTotalCapital()) > 0) {
                errors.add("资金不足，需要: " + totalNeeded + ", 可用: " + fundAllocation.getTotalCapital());
            }
        }

        return errors;
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (key != null && value != null) {
            this.metadata.put(key, value);
        }
    }
}