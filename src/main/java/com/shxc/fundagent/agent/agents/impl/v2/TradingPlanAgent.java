package com.shxc.fundagent.agent.agents.impl.v2;

import com.shxc.fundagent.agent.core.AbstractAgentV2;
import com.shxc.fundagent.agent.core.AgentV2;
import com.shxc.fundagent.agent.capabilities.MemoryManager;
import com.shxc.fundagent.agent.capabilities.PromptBuilder;
import com.shxc.fundagent.agent.capabilities.OutputParser;
import com.shxc.fundagent.agent.model.v2.AgentContext;
import com.shxc.fundagent.agent.model.v2.TradingPlan;
import com.shxc.fundagent.agent.model.v2.MarketContext;
import com.shxc.fundagent.agent.model.v2.NewsContext;
import com.shxc.fundagent.agent.model.AgentResult;
import com.shxc.fundagent.llm.LlmProvider;
import com.shxc.fundagent.llm.LlmProviderFactory;
import com.shxc.fundagent.llm.model.LlmRequest;
import com.shxc.fundagent.llm.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 交易计划生成Agent v2
 * 功能：将基金分析结果转化为具体的交易计划，包括订单生成、风险管理和执行策略
 * 对应需求：FR-008 交易计划生成
 */
@Component
public class TradingPlanAgent extends AbstractAgentV2 {

    private static final Logger logger = LoggerFactory.getLogger(TradingPlanAgent.class);

    private static final String AGENT_NAME = "trading_plan_agent_v2";
    private static final String AGENT_DESCRIPTION = "交易计划生成Agent，将分析结果转化为具体的交易计划";

    private static final String[] CAPABILITIES = {
            "trading_plan_generation",
            "order_creation",
            "risk_management",
            "execution_strategy",
            "portfolio_rebalancing",
            "structured_output",
            "tool_integration"
    };

    private static final String[] SUPPORTED_CONTEXT_TYPES = {
            "analysis_result",
            "market_context",
            "news_context",
            "fund_data",
            "portfolio_data"
    };

    // 交易计划输出Schema定义
    private static final String TRADING_PLAN_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "planSummary": {
              "type": "string",
              "description": "交易计划摘要"
            },
            "planType": {
              "type": "string",
              "description": "计划类型",
              "enum": ["BUY_PLAN", "SELL_PLAN", "REBALANCE_PLAN", "HEDGE_PLAN", "STOP_LOSS_PLAN", "TAKE_PROFIT_PLAN"]
            },
            "recommendedActions": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "assetType": {"type": "string"},
                  "assetCode": {"type": "string"},
                  "assetName": {"type": "string"},
                  "action": {"type": "string", "enum": ["BUY", "SELL", "HOLD", "ADJUST"]},
                  "quantity": {"type": "number"},
                  "amount": {"type": "number"},
                  "targetPrice": {"type": "number"},
                  "priority": {"type": "string", "enum": ["HIGH", "MEDIUM", "LOW"]},
                  "timeHorizon": {"type": "string", "enum": ["IMMEDIATE", "SHORT_TERM", "MEDIUM_TERM", "LONG_TERM"]},
                  "riskLevel": {"type": "string", "enum": ["LOW", "MEDIUM", "HIGH", "VERY_HIGH"]},
                  "reasoning": {"type": "string"}
                },
                "required": ["assetType", "assetCode", "action", "reasoning"]
              }
            },
            "totalAmount": {"type": "number"},
            "riskAssessment": {
              "type": "object",
              "properties": {
                "overallRiskLevel": {"type": "string", "enum": ["LOW", "MEDIUM", "HIGH", "VERY_HIGH"]},
                "maxDrawdownLimit": {"type": "number"},
                "stopLossLevels": {"type": "array"},
                "riskMitigationStrategies": {"type": "array"}
              }
            },
            "executionStrategy": {
              "type": "object",
              "properties": {
                "strategyType": {"type": "string", "enum": ["MARKET_ORDER", "LIMIT_ORDER", "BATCH_EXECUTION", "TWAP", "VWAP"]},
                "executionTiming": {"type": "string"},
                "conditions": {"type": "array"}
              }
            },
            "confidenceScore": {"type": "number", "minimum": 0, "maximum": 1},
            "requiresApproval": {"type": "boolean"}
          },
          "required": ["planSummary", "planType", "recommendedActions", "confidenceScore"]
        }
        """;

    private final LlmProviderFactory llmProviderFactory;
    private final PromptBuilder promptBuilder;
    private final OutputParser outputParser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 交易计划生成模板
    private String tradingPlanTemplate = """
        你是一个专业的交易策略师，需要基于市场分析、基金分析和风险偏好，生成具体可执行的交易计划。

        === 市场环境分析 ===
        {market_context}

        === 新闻资讯分析 ===
        {news_context}

        === 基金分析结果 ===
        {analysis_result}

        === 账户当前状况 ===
        {portfolio_summary}

        === 风险偏好 ===
        {risk_preference}

        请生成详细的交易计划，包括：

        1. **计划类型**：选择合适的计划类型（买入计划、卖出计划、调仓计划等）

        2. **具体交易指令**（针对每只基金）：
           - 资产类型（基金）
           - 基金代码和名称
           - 操作类型（买入/卖出/持有/调整）
           - 建议数量或金额
           - 目标价格（如果适用）
           - 优先级（高/中/低）
           - 执行时间框架（立即/短期/中期/长期）
           - 风险等级
           - 详细理由

        3. **风险控制**：
           - 总体风险等级评估
           - 最大回撤限制建议
           - 止损级别设置
           - 风险缓解策略

        4. **执行策略**：
           - 执行策略类型（市价单、限价单、分批执行等）
           - 最佳执行时机建议
           - 执行条件

        5. **计划评估**：
           - 整体置信度评分（0-1）
           - 是否需要人工审核

        要求：
        - 输出必须严格遵守指定的JSON Schema格式
        - 交易计划必须切实可行，考虑市场流动性和交易成本
        - 充分考虑风险收益比
        - 保持风险控制和资金管理的原则
        - 明确优先级和执行顺序

        请以JSON格式返回交易计划，确保符合预定义的模式。
        """;

    @Autowired
    public TradingPlanAgent(
            LlmProviderFactory llmProviderFactory,
            PromptBuilder promptBuilder,
            OutputParser outputParser) {
        super(AGENT_NAME, AGENT_DESCRIPTION, CAPABILITIES, SUPPORTED_CONTEXT_TYPES);
        this.llmProviderFactory = llmProviderFactory;
        this.promptBuilder = promptBuilder;
        this.outputParser = outputParser;

        // 初始化Agent
        initAgent();
    }

    /**
     * 初始化Agent
     */
    private void initAgent() {
        logger.info("初始化交易计划生成Agent: {}", AGENT_NAME);

        // 注册支持的输出模式
        addSupportedOutputSchema("trading_plan");
        addSupportedOutputSchema("order_generation");
        addSupportedOutputSchema("risk_assessment");

        // 初始化默认配置
        this.config = new DefaultAgentConfig();
    }

    @Override
    protected AgentResult doProcessWithTools(String task, AgentContext context) throws Exception {
        logger.info("交易计划生成Agent处理任务: {}", task);

        try {
            // 根据任务类型执行不同的处理逻辑
            if (task.contains("generate") || task.contains("生成")) {
                return generateTradingPlan(context);
            } else if (task.contains("review") || task.contains("审核")) {
                return reviewExistingPlan(context);
            } else if (task.contains("optimize") || task.contains("优化")) {
                return optimizeTradingPlan(context);
            } else if (task.contains("execute") || task.contains("执行")) {
                return executeTradingPlan(context);
            } else {
                // 默认处理：生成交易计划
                return generateTradingPlan(context);
            }
        } catch (Exception e) {
            logger.error("交易计划生成Agent处理任务失败: {}", task, e);
            return buildErrorResult(e, 0);
        }
    }

    /**
     * 生成交易计划
     */
    private AgentResult generateTradingPlan(AgentContext context) {
        long startTime = System.currentTimeMillis();
        logger.info("开始生成交易计划");

        try {
            // 1. 准备交易计划生成数据
            Map<String, Object> planData = preparePlanData(context);

            // 2. 构建交易计划生成提示
            String tradingPlanPrompt = buildTradingPlanPrompt(planData);

            // 3. 调用LLM生成结构化交易计划
            LlmProvider llmProvider = getLlmProvider();
            LlmRequest request = LlmRequest.builder()
                    .messages(List.of(Message.user(tradingPlanPrompt)))
                    .model("gpt-3.5-turbo")
                    .temperature(0.1) // 较低温度以获得更结构化的输出
                    .maxTokens(2500)
                    .build();

            var llmResponse = llmProvider.call(request);
            if (!llmResponse.isSuccess()) {
                throw new RuntimeException("LLM交易计划生成失败: " + llmResponse.getErrorMessage());
            }

            // 4. 解析和验证结构化输出
            Map<String, Object> structuredPlan = outputParser.parseStructuredOutput(
                    llmResponse.getContent(), TRADING_PLAN_SCHEMA);

            // 5. 转换为TradingPlan对象
            TradingPlan tradingPlan = convertToTradingPlan(structuredPlan, planData);

            // 6. 保存交易计划到记忆
            saveTradingPlanToMemory(tradingPlan, structuredPlan, planData);

            long processingTime = System.currentTimeMillis() - startTime;

            // 7. 构建返回结果
            return AgentResult.builder()
                    .agentName(getName())
                    .status(AgentResult.Status.SUCCESS)
                    .content(tradingPlan)
                    .confidence(extractConfidence(structuredPlan))
                    .reasoning("基于市场分析、基金分析和风险偏好生成的交易计划")
                    .extraData(Map.of(
                            "tradingPlan", tradingPlan,
                            "structuredPlan", structuredPlan,
                            "dataSourcesUsed", Arrays.asList("market_data", "news_data", "analysis_result", "portfolio_data"),
                            "processingTimeMs", processingTime,
                            "llmProvider", llmProvider.getProviderName(),
                            "requiresApproval", structuredPlan.getOrDefault("requiresApproval", true)
                    ))
                    .processingTimeMs(processingTime)
                    .build();

        } catch (Exception e) {
            logger.error("交易计划生成失败", e);
            return buildErrorResult(e, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 准备交易计划生成数据
     */
    private Map<String, Object> preparePlanData(AgentContext context) {
        Map<String, Object> data = new HashMap<>();

        // 1. 获取市场环境数据
        MarketContext marketContext = extractMarketContext(context);
        data.put("marketContext", marketContext);
        data.put("marketSummary", marketContext != null ? marketContext.getMarketSummary() : "无市场数据");

        // 2. 获取新闻资讯数据
        NewsContext newsContext = extractNewsContext(context);
        data.put("newsContext", newsContext);
        data.put("newsSummary", newsContext != null ? newsContext.getBriefSummary() : "无新闻数据");

        // 3. 获取分析结果
        Map<String, Object> analysisResult = extractAnalysisResult(context);
        data.put("analysisResult", analysisResult);

        // 4. 获取投资组合数据
        Map<String, Object> portfolioData = extractPortfolioData(context);
        data.put("portfolioData", portfolioData);
        data.put("portfolioSummary", portfolioData != null ? formatPortfolioSummary(portfolioData) : "无投资组合数据");

        // 5. 风险偏好（可从上下文或配置中获取）
        String riskPreference = extractRiskPreference(context);
        data.put("riskPreference", riskPreference);

        // 6. 资金限制（可从上下文或配置中获取）
        BigDecimal capitalLimit = extractCapitalLimit(context);
        data.put("capitalLimit", capitalLimit);

        return data;
    }

    /**
     * 构建交易计划生成提示
     */
    private String buildTradingPlanPrompt(Map<String, Object> planData) {
        Map<String, Object> promptVariables = new HashMap<>();

        // 市场环境部分
        if (planData.get("marketContext") != null) {
            MarketContext marketContext = (MarketContext) planData.get("marketContext");
            promptVariables.put("market_context", formatMarketContext(marketContext));
        } else {
            promptVariables.put("market_context", "暂无最新的市场环境数据");
        }

        // 新闻资讯部分
        if (planData.get("newsContext") != null) {
            NewsContext newsContext = (NewsContext) planData.get("newsContext");
            promptVariables.put("news_context", formatNewsContext(newsContext));
        } else {
            promptVariables.put("news_context", "暂无相关的新闻资讯数据");
        }

        // 分析结果部分
        if (planData.get("analysisResult") != null) {
            Map<String, Object> analysisResult = (Map<String, Object>) planData.get("analysisResult");
            promptVariables.put("analysis_result", formatAnalysisResult(analysisResult));
        } else {
            promptVariables.put("analysis_result", "暂无基金分析结果");
        }

        // 投资组合部分
        promptVariables.put("portfolio_summary", planData.getOrDefault("portfolioSummary", "暂无投资组合数据"));

        // 风险偏好部分
        promptVariables.put("risk_preference", planData.getOrDefault("riskPreference", "平衡型风险偏好"));

        return promptBuilder.buildPrompt(tradingPlanTemplate, promptVariables);
    }

    /**
     * 提取市场环境数据
     */
    private MarketContext extractMarketContext(AgentContext context) {
        if (context != null && context.containsData("marketData")) {
            return (MarketContext) context.getData("marketData");
        }

        logger.warn("未提供市场环境数据");
        return null;
    }

    /**
     * 提取新闻资讯数据
     */
    private NewsContext extractNewsContext(AgentContext context) {
        if (context != null && context.containsData("newsData")) {
            return (NewsContext) context.getData("newsData");
        }

        logger.warn("未提供新闻资讯数据");
        return null;
    }

    /**
     * 提取分析结果
     */
    private Map<String, Object> extractAnalysisResult(AgentContext context) {
        if (context != null && context.containsData("analysisResult")) {
            Object data = context.getData("analysisResult");
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = (Map<String, Object>) data;
                return result;
            }
        }

        logger.warn("未提供分析结果数据");
        return Collections.emptyMap();
    }

    /**
     * 提取投资组合数据
     */
    private Map<String, Object> extractPortfolioData(AgentContext context) {
        if (context != null && context.containsData("portfolioData")) {
            Object data = context.getData("portfolioData");
            if (data instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> portfolio = (Map<String, Object>) data;
                return portfolio;
            }
        }

        logger.warn("未提供投资组合数据");
        return Collections.emptyMap();
    }

    /**
     * 提取风险偏好
     */
    private String extractRiskPreference(AgentContext context) {
        if (context != null && context.containsData("riskPreference")) {
            Object data = context.getData("riskPreference");
            if (data instanceof String) {
                return (String) data;
            }
        }

        // 默认风险偏好
        return "BALANCED";
    }

    /**
     * 提取资金限制
     */
    private BigDecimal extractCapitalLimit(AgentContext context) {
        if (context != null && context.containsData("capitalLimit")) {
            Object data = context.getData("capitalLimit");
            if (data instanceof BigDecimal) {
                return (BigDecimal) data;
            } else if (data instanceof Number) {
                return BigDecimal.valueOf(((Number) data).doubleValue());
            }
        }

        // 默认资金限制（10万元）
        return new BigDecimal("100000");
    }

    /**
     * 格式化市场环境数据
     */
    private String formatMarketContext(MarketContext marketContext) {
        if (marketContext == null) {
            return "市场环境数据不可用";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("市场日期: ").append(marketContext.getMarketDate()).append("\n");
        sb.append("市场温度: ").append(marketContext.getMarketTemperature()).append("\n");
        sb.append("风险等级: ").append(marketContext.getRiskLevel()).append("\n");
        sb.append("市场状态: ").append(marketContext.getMarketStatus()).append("\n");

        if (marketContext.getMarketSummary() != null) {
            sb.append("市场摘要: ").append(marketContext.getMarketSummary()).append("\n");
        }

        return sb.toString();
    }

    /**
     * 格式化新闻资讯数据
     */
    private String formatNewsContext(NewsContext newsContext) {
        if (newsContext == null) {
            return "新闻资讯数据不可用";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(newsContext.getBriefSummary());
        return sb.toString();
    }

    /**
     * 格式化分析结果
     */
    private String formatAnalysisResult(Map<String, Object> analysisResult) {
        if (analysisResult == null || analysisResult.isEmpty()) {
            return "分析结果不可用";
        }

        StringBuilder sb = new StringBuilder();

        // 提取关键信息
        Object summary = analysisResult.get("summary");
        if (summary != null) {
            sb.append("分析总结: ").append(summary).append("\n");
        }

        Object marketAssessment = analysisResult.get("marketAssessment");
        if (marketAssessment != null) {
            sb.append("市场评估: ").append(marketAssessment).append("\n");
        }

        Object fundAnalyses = analysisResult.get("fundAnalyses");
        if (fundAnalyses instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> funds = (List<Map<String, Object>>) fundAnalyses;
            sb.append("\n基金分析详情:\n");
            for (int i = 0; i < funds.size(); i++) {
                Map<String, Object> fund = funds.get(i);
                sb.append(i + 1).append(". ")
                        .append(fund.getOrDefault("fundCode", "未知")).append(" - ")
                        .append(fund.getOrDefault("assessment", "未知")).append(" (置信度: ")
                        .append(fund.getOrDefault("confidence", "N/A")).append(")\n");
            }
        }

        return sb.toString();
    }

    /**
     * 格式化投资组合摘要
     */
    private String formatPortfolioSummary(Map<String, Object> portfolioData) {
        if (portfolioData == null || portfolioData.isEmpty()) {
            return "投资组合数据不可用";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("总资产: ").append(formatNumber(portfolioData.get("totalAssets"))).append(" 元\n");
        sb.append("基金数量: ").append(portfolioData.getOrDefault("fundCount", 0)).append("\n");

        if (portfolioData.containsKey("totalProfit")) {
            sb.append("总收益: ").append(formatNumber(portfolioData.get("totalProfit"))).append(" 元\n");
        }

        return sb.toString();
    }

    /**
     * 将结构化计划转换为TradingPlan对象
     */
    private TradingPlan convertToTradingPlan(Map<String, Object> structuredPlan, Map<String, Object> planData) {
        TradingPlan tradingPlan = new TradingPlan();

        // 基本信息
        tradingPlan.setPlanId("trading_plan_" + System.currentTimeMillis());
        tradingPlan.setPlanName("自动生成的交易计划");
        tradingPlan.setDescription((String) structuredPlan.getOrDefault("planSummary", "基于AI分析生成的交易计划"));

        // 计划类型
        String planTypeStr = (String) structuredPlan.get("planType");
        if (planTypeStr != null) {
            try {
                tradingPlan.setPlanType(TradingPlan.PlanType.valueOf(planTypeStr));
            } catch (IllegalArgumentException e) {
                logger.warn("无效的计划类型: {}, 使用默认值", planTypeStr);
                tradingPlan.setPlanType(TradingPlan.PlanType.REBALANCE_PLAN);
            }
        } else {
            tradingPlan.setPlanType(TradingPlan.PlanType.REBALANCE_PLAN);
        }

        // 状态
        tradingPlan.setStatus(TradingPlan.PlanStatus.DRAFT);
        tradingPlan.setCreatedAt(LocalDateTime.now());
        tradingPlan.setCreatedBy(getName());

        // 风险等级（从结构化计划或分析数据中提取）
        Object riskObj = structuredPlan.get("riskAssessment");
        if (riskObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> riskAssessment = (Map<String, Object>) riskObj;
            String riskLevel = (String) riskAssessment.getOrDefault("overallRiskLevel", "BALANCED");
            try {
                tradingPlan.setRiskLevel(TradingPlan.RiskLevel.valueOf(riskLevel.toUpperCase()));
            } catch (Exception e) {
                tradingPlan.setRiskLevel(TradingPlan.RiskLevel.BALANCED);
            }
        } else {
            tradingPlan.setRiskLevel(TradingPlan.RiskLevel.BALANCED);
        }

        // 资金分配（简化实现）
        TradingPlan.FundAllocation fundAllocation = new TradingPlan.FundAllocation();
        BigDecimal capitalLimit = (BigDecimal) planData.getOrDefault("capitalLimit", new BigDecimal("100000"));
        fundAllocation.setTotalCapital(capitalLimit);
        tradingPlan.setFundAllocation(fundAllocation);

        // 添加交易订单（从recommendedActions中提取）
        Object actionsObj = structuredPlan.get("recommendedActions");
        if (actionsObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> actions = (List<Map<String, Object>>) actionsObj;
            for (Map<String, Object> action : actions) {
                TradingPlan.TradeOrder order = createTradeOrderFromAction(action);
                if (order != null) {
                    tradingPlan.addTradeOrder(order);
                }
            }
        }

        // 元数据
        tradingPlan.addMetadata("confidence", structuredPlan.get("confidenceScore"));
        tradingPlan.addMetadata("requiresApproval", structuredPlan.getOrDefault("requiresApproval", true));
        tradingPlan.addMetadata("generatedBy", getName());
        tradingPlan.addMetadata("generationTime", LocalDateTime.now().toString());

        return tradingPlan;
    }

    /**
     * 从操作建议创建交易订单
     */
    private TradingPlan.TradeOrder createTradeOrderFromAction(Map<String, Object> action) {
        try {
            TradingPlan.TradeOrder order = new TradingPlan.TradeOrder();

            // 基本信息
            order.setOrderId("order_" + System.currentTimeMillis() + "_" + (int)(Math.random() * 1000));
            order.setAssetType((String) action.getOrDefault("assetType", "FUND"));
            order.setAssetCode((String) action.getOrDefault("assetCode", "UNKNOWN"));
            order.setAssetName((String) action.getOrDefault("assetName", "未知资产"));

            // 交易方向
            String actionStr = (String) action.get("action");
            if ("BUY".equalsIgnoreCase(actionStr)) {
                order.setDirection("BUY");
            } else if ("SELL".equalsIgnoreCase(actionStr)) {
                order.setDirection("SELL");
            } else {
                // 如果不是明确的买卖操作，则跳过
                return null;
            }

            // 数量或金额
            Object quantityObj = action.get("quantity");
            Object amountObj = action.get("amount");
            if (amountObj != null) {
                order.setAmount(convertToBigDecimal(amountObj));
            } else if (quantityObj != null) {
                order.setQuantity(convertToBigDecimal(quantityObj));
                // 简单假设价格为1来计算金额
                order.setAmount(convertToBigDecimal(quantityObj));
            }

            // 目标价格（如果提供）
            Object targetPriceObj = action.get("targetPrice");
            if (targetPriceObj != null) {
                order.setTargetPrice(convertToBigDecimal(targetPriceObj));
            }

            // 理由
            order.setRationale((String) action.getOrDefault("reasoning", "基于AI分析的建议"));

            // 置信度
            Object confidenceObj = action.get("confidence");
            if (confidenceObj != null) {
                order.setConfidence(convertToBigDecimal(confidenceObj));
            }

            return order;

        } catch (Exception e) {
            logger.warn("创建交易订单失败: {}", action, e);
            return null;
        }
    }

    /**
     * 保存交易计划到记忆
     */
    private void saveTradingPlanToMemory(TradingPlan tradingPlan, Map<String, Object> structuredPlan, Map<String, Object> planData) {
        if (memoryManager == null) {
            return;
        }

        try {
            // 构建记忆内容
            String memoryContent = objectMapper.writeValueAsString(tradingPlan);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("planType", tradingPlan.getPlanType().name());
            metadata.put("status", tradingPlan.getStatus().name());
            metadata.put("totalAmount", tradingPlan.getTotalPlanAmount());
            metadata.put("confidence", structuredPlan.get("confidenceScore"));
            metadata.put("requiresApproval", structuredPlan.getOrDefault("requiresApproval", true));

            // 保存到中期记忆
            memoryManager.storeMemory(
                    getName(),
                    memoryContent,
                    MemoryManager.MemoryType.MID_TERM,
                    0.8, // 重要性评分较高
                    metadata
            );

            logger.debug("交易计划已保存到记忆");
        } catch (Exception e) {
            logger.warn("保存交易计划到记忆失败", e);
        }
    }

    /**
     * 提取置信度
     */
    private Double extractConfidence(Map<String, Object> structuredPlan) {
        try {
            Object confidenceObj = structuredPlan.get("confidenceScore");
            if (confidenceObj instanceof Number) {
                return ((Number) confidenceObj).doubleValue();
            }
        } catch (Exception e) {
            logger.warn("无法从结构化计划中提取置信度", e);
        }

        // 默认置信度
        return 0.7;
    }

    /**
     * 审核现有计划
     */
    private AgentResult reviewExistingPlan(AgentContext context) {
        // TODO: 实现交易计划审核逻辑
        logger.info("审核现有交易计划功能待实现");
        return AgentResult.builder()
                .agentName(getName())
                .status(AgentResult.Status.SUCCESS)
                .content("交易计划审核功能待实现")
                .build();
    }

    /**
     * 优化交易计划
     */
    private AgentResult optimizeTradingPlan(AgentContext context) {
        // TODO: 实现交易计划优化逻辑
        logger.info("优化交易计划功能待实现");
        return AgentResult.builder()
                .agentName(getName())
                .status(AgentResult.Status.SUCCESS)
                .content("交易计划优化功能待实现")
                .build();
    }

    /**
     * 执行交易计划
     */
    private AgentResult executeTradingPlan(AgentContext context) {
        // TODO: 实现交易计划执行逻辑
        logger.info("执行交易计划功能待实现");
        return AgentResult.builder()
                .agentName(getName())
                .status(AgentResult.Status.SUCCESS)
                .content("交易计划执行功能待实现")
                .build();
    }

    /**
     * 获取LLM提供商
     */
    private LlmProvider getLlmProvider() {
        // 这里应该实现逻辑选择适当的LLM提供商
        // 简化实现：使用第一个可用提供商
        return llmProviderFactory.getBestAvailableProvider();
    }

    /**
     * 辅助方法：格式化数字
     */
    private String formatNumber(Object value) {
        if (value == null) return "0.00";
        try {
            if (value instanceof BigDecimal) {
                return ((BigDecimal) value).setScale(2, BigDecimal.ROUND_HALF_UP).toString();
            }
            return String.format("%.2f", Double.parseDouble(value.toString()));
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * 辅助方法：转换为BigDecimal
     */
    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            } else {
                return new BigDecimal(value.toString());
            }
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 默认Agent配置
     */
    private static class DefaultAgentConfig implements AgentV2.AgentConfig {
        @Override
        public boolean isToolCallingEnabled() {
            return false;
        }

        @Override
        public boolean isMemoryManagementEnabled() {
            return true;
        }

        @Override
        public int getMaxToolCalls() {
            return 5;
        }

        @Override
        public long getDefaultToolTimeoutMs() {
            return 30000;
        }

        @Override
        public boolean isToolCallConfirmationRequired() {
            return false;
        }

        @Override
        public int getMemoryRetrievalLimit() {
            return 10;
        }

        @Override
        public String getDefaultOutputSchema() {
            return "trading_plan";
        }

        @Override
        public boolean isOutputValidationEnabled() {
            return true;
        }
    }

    /**
     * 构建错误结果（覆盖父类方法）
     */
    @Override
    protected AgentResult buildErrorResult(Exception e, long processingTimeMs) {
        return AgentResult.builder()
                .agentName(getName())
                .status(AgentResult.Status.ERROR)
                .content("交易计划生成失败")
                .errorMessage(e.getMessage())
                .errorCode("TRADING_PLAN_GENERATION_ERROR")
                .processingTimeMs(processingTimeMs)
                .build();
    }
}