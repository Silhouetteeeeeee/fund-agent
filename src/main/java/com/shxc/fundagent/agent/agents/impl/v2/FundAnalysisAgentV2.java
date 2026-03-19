package com.shxc.fundagent.agent.agents.impl.v2;

import com.shxc.fundagent.agent.core.AbstractAgentV2;
import com.shxc.fundagent.agent.capabilities.MemoryManager;
import com.shxc.fundagent.agent.capabilities.PromptBuilder;
import com.shxc.fundagent.agent.capabilities.OutputParser;
import com.shxc.fundagent.agent.model.v2.AgentContext;
import com.shxc.fundagent.agent.model.v2.MarketContext;
import com.shxc.fundagent.agent.model.v2.NewsContext;
import com.shxc.fundagent.agent.model.AgentResult;
import com.shxc.fundagent.agent.model.v2.TradingPlan;
import com.shxc.fundagent.llm.LlmProvider;
import com.shxc.fundagent.llm.LlmProviderFactory;
import com.shxc.fundagent.llm.model.LlmRequest;
import com.shxc.fundagent.llm.model.Message;
import com.shxc.fundagent.service.FundDataService;
import com.shxc.fundagent.service.YieldCalculationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 增强版基金分析Agent v2
 * 功能：整合市场环境、新闻资讯、持仓数据，提供增强型基金分析
 * 对应需求：FR-003 增强型分析Prompt构建，FR-007 结构化输出与解析
 */
@Component
public class FundAnalysisAgentV2 extends AbstractAgentV2 {

    private static final Logger logger = LoggerFactory.getLogger(FundAnalysisAgentV2.class);

    private static final String AGENT_NAME = "fund_analysis_agent_v2";
    private static final String AGENT_DESCRIPTION = "增强版基金分析Agent，整合市场、新闻、持仓数据，提供结构化分析和投资建议";

    private static final String[] CAPABILITIES = {
            "fund_analysis",
            "investment_advice",
            "risk_assessment",
            "market_analysis",
            "news_analysis",
            "structured_output",
            "tool_integration"
    };

    private static final String[] SUPPORTED_CONTEXT_TYPES = {
            "fund_data",
            "market_context",
            "news_context",
            "portfolio_data"
    };

    // 结构化输出Schema定义
    private static final String ANALYSIS_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "summary": {
              "type": "string",
              "description": "分析总结"
            },
            "marketAssessment": {
              "type": "string",
              "description": "市场环境评估",
              "enum": ["BULLISH", "NEUTRAL", "BEARISH", "HIGHLY_BULLISH", "HIGHLY_BEARISH"]
            },
            "fundAnalyses": {
              "type": "array",
              "items": {
                "type": "object",
                "properties": {
                  "fundCode": {"type": "string"},
                  "fundName": {"type": "string"},
                  "assessment": {
                    "type": "string",
                    "enum": ["STRONG_BUY", "BUY", "HOLD", "SELL", "STRONG_SELL"]
                  },
                  "confidence": {"type": "number", "minimum": 0, "maximum": 1},
                  "reasoning": {"type": "string"},
                  "recommendedAction": {
                    "type": "string",
                    "enum": ["INCREASE_POSITION", "HOLD_POSITION", "REDUCE_POSITION", "EXIT_POSITION"]
                  },
                  "suggestedAmount": {"type": "number"},
                  "riskLevel": {
                    "type": "string",
                    "enum": ["LOW", "MEDIUM", "HIGH", "VERY_HIGH"]
                  }
                },
                "required": ["fundCode", "assessment", "confidence", "reasoning"]
              }
            },
            "portfolioAdjustment": {
              "type": "object",
              "properties": {
                "recommendedAssetAllocation": {
                  "type": "object",
                  "additionalProperties": {"type": "number"}
                },
                "rebalancingNeeded": {"type": "boolean"},
                "rebalancingSuggestions": {"type": "array"}
              }
            },
            "keyRisks": {"type": "array", "items": {"type": "string"}},
            "timeHorizon": {
              "type": "string",
              "enum": ["SHORT_TERM", "MEDIUM_TERM", "LONG_TERM"]
            },
            "confidenceScore": {"type": "number", "minimum": 0, "maximum": 1}
          },
          "required": ["summary", "marketAssessment", "fundAnalyses", "confidenceScore"]
        }
        """;

    private final LlmProviderFactory llmProviderFactory;
    private final FundDataService fundDataService;
    private final YieldCalculationService yieldCalculationService;
    private final PromptBuilder promptBuilder;
    private final OutputParser outputParser;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 分析模板
    private String enhancedAnalysisTemplate = """
        你是一个专业的基金投资顾问，需要基于全面的市场环境、新闻资讯和基金持仓数据，提供详细的结构化分析和投资建议。

        === 市场环境分析 ===
        {market_context}

        === 新闻资讯摘要 ===
        {news_context}

        === 基金持仓数据 ===
        {fund_data}

        === 账户整体情况 ===
        {portfolio_summary}

        请提供以下详细分析：

        1. **综合市场评估**：结合市场数据和新闻，给出市场整体评级（BULLISH/NEUTRAL/BEARISH）

        2. **逐只基金分析**（针对每只持仓基金）：
           - 当前表现评估（STRONG_BUY/BUY/HOLD/SELL/STRONG_SELL）
           - 置信度评分（0-1之间）
           - 详细推理过程（结合市场、新闻、基金特性）
           - 具体操作建议（INCREASE_POSITION/HOLD_POSITION/REDUCE_POSITION/EXIT_POSITION）
           - 建议调整金额（如果适用）
           - 风险评估（LOW/MEDIUM/HIGH/VERY_HIGH）

        3. **组合调整建议**：
           - 推荐资产配置比例
           - 是否需要再平衡
           - 具体调仓建议

        4. **关键风险提示**：列出最重要的3-5个风险点

        5. **投资时间框架建议**：短期/中期/长期

        要求：
        - 输出必须严格遵守指定的JSON Schema格式
        - 每项建议必须有明确的数据支持和推理过程
        - 考虑风险收益比和投资目标
        - 保持专业性和客观性

        请以JSON格式返回分析结果，确保符合预定义的模式。
        """;

    @Autowired
    public FundAnalysisAgentV2(
            LlmProviderFactory llmProviderFactory,
            FundDataService fundDataService,
            YieldCalculationService yieldCalculationService,
            PromptBuilder promptBuilder,
            OutputParser outputParser) {
        super(AGENT_NAME, AGENT_DESCRIPTION, CAPABILITIES, SUPPORTED_CONTEXT_TYPES);
        this.llmProviderFactory = llmProviderFactory;
        this.fundDataService = fundDataService;
        this.yieldCalculationService = yieldCalculationService;
        this.promptBuilder = promptBuilder;
        this.outputParser = outputParser;

        // 初始化Agent
        initAgent();
    }

    /**
     * 初始化Agent
     */
    private void initAgent() {
        logger.info("初始化增强版基金分析Agent: {}", AGENT_NAME);

        // 注册支持的输出模式
        addSupportedOutputSchema("structured_analysis");
        addSupportedOutputSchema("investment_advice");
        addSupportedOutputSchema("risk_report");

        // 初始化默认工具（如果有）
        // 注意：工具注册将在EnhancedAgentManager中自动完成
    }

    @Override
    protected AgentResult doProcessWithTools(String task, AgentContext context) throws Exception {
        logger.info("增强版基金分析Agent处理任务: {}", task);

        try {
            // 根据任务类型执行不同的处理逻辑
            if (task.contains("analyze") || task.contains("分析")) {
                return performEnhancedAnalysis(context);
            } else if (task.contains("generate") || task.contains("生成")) {
                return generateInvestmentAdvice(context);
            } else if (task.contains("assess") || task.contains("评估")) {
                return assessPortfolioRisk(context);
            } else if (task.contains("monitor") || task.contains("监控")) {
                return monitorPortfolio(context);
            } else {
                // 默认处理：执行增强分析
                return performEnhancedAnalysis(context);
            }
        } catch (Exception e) {
            logger.error("增强版基金分析Agent处理任务失败: {}", task, e);
            return buildErrorResult(e, 0);
        }
    }

    /**
     * 执行增强分析
     */
    private AgentResult performEnhancedAnalysis(AgentContext context) {
        long startTime = System.currentTimeMillis();
        logger.info("开始执行增强版基金分析");

        try {
            // 1. 收集和准备分析数据
            Map<String, Object> analysisData = prepareAnalysisData(context);

            // 2. 构建增强提示
            String enhancedPrompt = buildEnhancedPrompt(analysisData);

            // 3. 调用LLM进行结构化分析
            LlmProvider llmProvider = getLlmProvider();
            LlmRequest request = LlmRequest.builder()
                    .messages(List.of(Message.user(enhancedPrompt)))
                    .model("gpt-3.5-turbo")
                    .temperature(0.2) // 较低温度以获得更结构化的输出
                    .maxTokens(2000)
                    .build();

            var llmResponse = llmProvider.call(request);
            if (!llmResponse.isSuccess()) {
                throw new RuntimeException("LLM分析失败: " + llmResponse.getErrorMessage());
            }

            // 4. 解析和验证结构化输出
            Map<String, Object> structuredResult = outputParser.parseStructuredOutput(
                    llmResponse.getContent(), ANALYSIS_SCHEMA);

            // 5. 构建交易计划建议（如果需要）
            TradingPlan tradingPlan = null;
            if (structuredResult.containsKey("fundAnalyses")) {
                tradingPlan = generateTradingPlanSuggestions(structuredResult, analysisData);
            }

            // 6. 保存分析历史到记忆
            saveAnalysisToMemory(structuredResult, analysisData);

            long processingTime = System.currentTimeMillis() - startTime;

            // 7. 构建返回结果
            return AgentResult.builder()
                    .agentName(getName())
                    .status(AgentResult.Status.SUCCESS)
                    .content(structuredResult)
                    .confidence(calculateAnalysisConfidence(structuredResult))
                    .reasoning("基于增强版分析框架，整合市场、新闻、持仓数据")
                    .extraData(Map.of(
                            "structuredAnalysis", structuredResult,
                            "tradingPlanSuggestions", tradingPlan,
                            "dataSourcesUsed", Arrays.asList("market_data", "news_data", "fund_data", "portfolio_data"),
                            "processingTimeMs", processingTime,
                            "llmProvider", llmProvider.getProviderName()
                    ))
                    .processingTimeMs(processingTime)
                    .build();

        } catch (Exception e) {
            logger.error("增强版基金分析失败", e);
            return buildErrorResult(e, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 准备分析数据
     */
    private Map<String, Object> prepareAnalysisData(AgentContext context) {
        Map<String, Object> data = new HashMap<>();

        // 1. 获取市场环境数据
        MarketContext marketContext = extractMarketContext(context);
        data.put("marketContext", marketContext);
        data.put("marketSummary", marketContext != null ? marketContext.getMarketSummary() : "无市场数据");

        // 2. 获取新闻资讯数据
        NewsContext newsContext = extractNewsContext(context);
        data.put("newsContext", newsContext);
        data.put("newsSummary", newsContext != null ? newsContext.getBriefSummary() : "无新闻数据");

        // 3. 获取基金持仓数据
        List<Map<String, Object>> fundData = extractFundData(context);
        data.put("fundData", fundData);

        // 4. 计算账户整体情况
        Map<String, Object> portfolioSummary = calculatePortfolioSummary(fundData);
        data.put("portfolioSummary", portfolioSummary);

        // 5. 如果有工具调用器，可以获取额外数据
        if (toolCaller != null) {
            data.put("availableTools", toolCaller.getAvailableToolDescriptions());
        }

        return data;
    }

    /**
     * 构建增强提示
     */
    private String buildEnhancedPrompt(Map<String, Object> analysisData) {
        // 使用PromptBuilder构建结构化提示
        Map<String, Object> promptVariables = new HashMap<>();

        // 市场环境部分
        if (analysisData.get("marketContext") != null) {
            MarketContext marketContext = (MarketContext) analysisData.get("marketContext");
            promptVariables.put("market_context", formatMarketContext(marketContext));
        } else {
            promptVariables.put("market_context", "暂无最新的市场环境数据");
        }

        // 新闻资讯部分
        if (analysisData.get("newsContext") != null) {
            NewsContext newsContext = (NewsContext) analysisData.get("newsContext");
            promptVariables.put("news_context", formatNewsContext(newsContext));
        } else {
            promptVariables.put("news_context", "暂无相关的新闻资讯数据");
        }

        // 基金持仓部分
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fundData = (List<Map<String, Object>>) analysisData.get("fundData");
        promptVariables.put("fund_data", formatFundData(fundData));

        // 账户整体情况
        @SuppressWarnings("unchecked")
        Map<String, Object> portfolioSummary = (Map<String, Object>) analysisData.get("portfolioSummary");
        promptVariables.put("portfolio_summary", formatPortfolioSummary(portfolioSummary));

        // 构建完整提示
        return promptBuilder.buildPrompt(enhancedAnalysisTemplate, promptVariables);
    }

    /**
     * 提取市场环境数据
     */
    private MarketContext extractMarketContext(AgentContext context) {
        if (context != null && context.containsData("marketData")) {
            return (MarketContext) context.getData("marketData");
        }

        // 如果没有提供，尝试从其他Agent获取或返回null
        logger.warn("未提供市场环境数据，使用默认值");
        return null;
    }

    /**
     * 提取新闻资讯数据
     */
    private NewsContext extractNewsContext(AgentContext context) {
        if (context != null && context.containsData("newsData")) {
            return (NewsContext) context.getData("newsData");
        }

        logger.warn("未提供新闻资讯数据，使用默认值");
        return null;
    }

    /**
     * 提取基金数据
     */
    private List<Map<String, Object>> extractFundData(AgentContext context) {
        if (context != null && context.containsData("fundData")) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> fundData = (List<Map<String, Object>>) context.getData("fundData");
            return fundData;
        }

        // 如果没有提供基金数据，返回空列表
        logger.warn("未提供基金数据，返回空列表");
        return Collections.emptyList();
    }

    /**
     * 计算账户整体情况
     */
    private Map<String, Object> calculatePortfolioSummary(List<Map<String, Object>> fundData) {
        Map<String, Object> summary = new HashMap<>();

        if (fundData == null || fundData.isEmpty()) {
            summary.put("totalAssets", BigDecimal.ZERO);
            summary.put("totalCost", BigDecimal.ZERO);
            summary.put("totalProfit", BigDecimal.ZERO);
            summary.put("totalProfitRate", BigDecimal.ZERO);
            summary.put("fundCount", 0);
            return summary;
        }

        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal totalProfit = BigDecimal.ZERO;

        for (Map<String, Object> fund : fundData) {
            BigDecimal holdAmount = convertToBigDecimal(fund.get("holdAmount"));
            BigDecimal costAmount = convertToBigDecimal(fund.get("costAmount"));
            BigDecimal profit = convertToBigDecimal(fund.get("profit"));

            totalAssets = totalAssets.add(holdAmount);
            totalCost = totalCost.add(costAmount);
            totalProfit = totalProfit.add(profit);
        }

        BigDecimal totalProfitRate = totalCost.compareTo(BigDecimal.ZERO) > 0 ?
                totalProfit.divide(totalCost, 4, BigDecimal.ROUND_HALF_UP).multiply(BigDecimal.valueOf(100)) :
                BigDecimal.ZERO;

        summary.put("totalAssets", totalAssets);
        summary.put("totalCost", totalCost);
        summary.put("totalProfit", totalProfit);
        summary.put("totalProfitRate", totalProfitRate);
        summary.put("fundCount", fundData.size());

        return summary;
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

        // 获取新闻摘要
        NewsContext.NewsSummary summary = newsContext.getSummary();
        if (summary != null) {
            sb.append("新闻数量: ").append(summary.getTotalNewsCount()).append("\n");
        } else {
            sb.append("新闻数量: 0\n");
        }

        // 获取情感分析
        NewsContext.SentimentAnalysis sentimentAnalysis = newsContext.getSentimentAnalysis();
        if (sentimentAnalysis != null) {
            sb.append("总体情感: ").append(sentimentAnalysis.getOverallSentiment()).append("\n");
            sb.append("情感分数: ").append(sentimentAnalysis.getOverallSentimentScore()).append("\n");
        } else {
            sb.append("总体情感: N/A\n");
            sb.append("情感分数: N/A\n");
        }

        // 获取关键主题
        if (summary != null && summary.getTopKeywords() != null && !summary.getTopKeywords().isEmpty()) {
            sb.append("关键主题: ").append(String.join(", ", summary.getTopKeywords())).append("\n");
        }

        // 获取简要摘要
        sb.append("新闻摘要: ").append(newsContext.getBriefSummary()).append("\n");

        return sb.toString();
    }

    /**
     * 格式化基金数据
     */
    private String formatFundData(List<Map<String, Object>> fundData) {
        if (fundData == null || fundData.isEmpty()) {
            return "暂无基金持仓数据";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fundData.size(); i++) {
            Map<String, Object> fund = fundData.get(i);
            sb.append("--- 基金 ").append(i + 1).append(" ---\n");
            sb.append("代码: ").append(fund.getOrDefault("fundCode", "未知")).append("\n");
            sb.append("名称: ").append(fund.getOrDefault("fundName", "未知")).append("\n");
            sb.append("净值: ").append(formatNumber(fund.get("netValue"))).append("\n");
            sb.append("涨跌幅: ").append(formatNumber(fund.get("changePercent"))).append("%\n");
            sb.append("风险评估: ").append(fund.getOrDefault("riskLevel", "未知")).append("\n");

            // 持仓信息
            sb.append("\n【持仓信息】\n");
            sb.append("持有份额: ").append(formatNumber(fund.get("holdShares"))).append(" 份\n");
            sb.append("持仓市值: ").append(formatNumber(fund.get("holdAmount"))).append(" 元\n");
            sb.append("投入成本: ").append(formatNumber(fund.get("costAmount"))).append(" 元\n");
            sb.append("持有收益: ").append(formatNumber(fund.get("profit"))).append(" 元\n");
            sb.append("持有收益率: ").append(formatNumber(fund.get("profitRate"))).append("%\n");
            sb.append("仓位占比: ").append(formatPosition(fund.get("position"))).append("%\n");
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 格式化账户整体情况
     */
    private String formatPortfolioSummary(Map<String, Object> portfolioSummary) {
        StringBuilder sb = new StringBuilder();
        sb.append("基金总数: ").append(portfolioSummary.getOrDefault("fundCount", 0)).append("\n");
        sb.append("总资产: ").append(formatNumber(portfolioSummary.get("totalAssets"))).append(" 元\n");
        sb.append("总投入: ").append(formatNumber(portfolioSummary.get("totalCost"))).append(" 元\n");
        sb.append("总收益: ").append(formatNumber(portfolioSummary.get("totalProfit"))).append(" 元\n");
        sb.append("总收益率: ").append(formatNumber(portfolioSummary.get("totalProfitRate"))).append("%\n");

        return sb.toString();
    }

    /**
     * 生成交易计划建议
     */
    private TradingPlan generateTradingPlanSuggestions(Map<String, Object> structuredResult, Map<String, Object> analysisData) {
        // 这里实现交易计划生成逻辑
        // 目前返回一个空的交易计划对象
        TradingPlan tradingPlan = new TradingPlan();
        tradingPlan.setPlanId("plan_" + System.currentTimeMillis());
        tradingPlan.setPlanName("基金投资调整建议");
        tradingPlan.setDescription("基于增强版基金分析生成的交易计划建议");

        // 使用REBALANCE_PLAN作为调仓计划类型
        tradingPlan.setPlanType(TradingPlan.PlanType.REBALANCE_PLAN);

        // 使用DRAFT状态作为建议状态
        tradingPlan.setStatus(TradingPlan.PlanStatus.DRAFT);

        tradingPlan.setCreatedAt(LocalDateTime.now());
        tradingPlan.setCreatedBy(getName());

        // 在metadata中存储置信度
        tradingPlan.addMetadata("confidence", 0.7);
        tradingPlan.addMetadata("analysisType", "enhanced_fund_analysis");
        tradingPlan.addMetadata("generatedTime", LocalDateTime.now().toString());

        // 可以从structuredResult中提取建议构建具体订单
        // 这里可以基于structuredResult中的fundAnalyses构建具体的TradeOrder

        return tradingPlan;
    }

    /**
     * 保存分析历史到记忆
     */
    private void saveAnalysisToMemory(Map<String, Object> structuredResult, Map<String, Object> analysisData) {
        if (memoryManager == null) {
            return;
        }

        try {
            // 构建记忆内容
            String memoryContent = objectMapper.writeValueAsString(structuredResult);
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("analysisType", "enhanced_fund_analysis");
            metadata.put("fundCount", analysisData.getOrDefault("fundCount", 0));
            metadata.put("confidence", structuredResult.get("confidenceScore"));

            // 保存到中期记忆
            memoryManager.storeMemory(
                    getName(),
                    memoryContent,
                    MemoryManager.MemoryType.MID_TERM,
                    0.7, // 重要性评分
                    metadata
            );

            logger.debug("分析结果已保存到记忆");
        } catch (Exception e) {
            logger.warn("保存分析结果到记忆失败", e);
        }
    }

    /**
     * 计算分析置信度
     */
    private Double calculateAnalysisConfidence(Map<String, Object> structuredResult) {
        try {
            Object confidenceObj = structuredResult.get("confidenceScore");
            if (confidenceObj instanceof Number) {
                return ((Number) confidenceObj).doubleValue();
            }
        } catch (Exception e) {
            logger.warn("无法从结构化结果中提取置信度", e);
        }

        // 默认置信度
        return 0.7;
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
     * 生成投资建议
     */
    private AgentResult generateInvestmentAdvice(AgentContext context) {
        // TODO: 实现生成具体投资建议的逻辑
        return performEnhancedAnalysis(context);
    }

    /**
     * 评估组合风险
     */
    private AgentResult assessPortfolioRisk(AgentContext context) {
        // TODO: 实现组合风险评估逻辑
        return performEnhancedAnalysis(context);
    }

    /**
     * 监控组合
     */
    private AgentResult monitorPortfolio(AgentContext context) {
        // TODO: 实现组合监控逻辑
        return performEnhancedAnalysis(context);
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
     * 辅助方法：格式化仓位
     */
    private String formatPosition(Object position) {
        if (position == null) return "0.00";
        try {
            double pos = Double.parseDouble(position.toString());
            if (pos >= 0 && pos <= 1) {
                return String.format("%.2f", pos * 100);
            }
            return String.format("%.2f", pos);
        } catch (Exception e) {
            return position.toString();
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
     * 构建错误结果（覆盖父类方法）
     */
    @Override
    protected AgentResult buildErrorResult(Exception e, long processingTimeMs) {
        return AgentResult.builder()
                .agentName(getName())
                .status(AgentResult.Status.ERROR)
                .content("增强版基金分析失败")
                .errorMessage(e.getMessage())
                .errorCode("ENHANCED_ANALYSIS_ERROR")
                .processingTimeMs(processingTimeMs)
                .build();
    }
}