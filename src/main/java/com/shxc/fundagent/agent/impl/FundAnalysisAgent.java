package com.shxc.fundagent.agent.impl;

import com.shxc.fundagent.agent.AbstractAgent;
import com.shxc.fundagent.agent.model.AgentResult;
import com.shxc.fundagent.llm.LlmProvider;
import com.shxc.fundagent.llm.LlmProviderFactory;
import com.shxc.fundagent.llm.model.LlmRequest;
import com.shxc.fundagent.llm.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 基金分析Agent
 * 示例Agent实现，展示如何使用LLM进行基金分析
 */
@Component
public class FundAnalysisAgent extends AbstractAgent {

    @Autowired
    private LlmProviderFactory llmProviderFactory;

    /**
     * 默认LLM提供商名称
     */
    private String defaultLlmProvider = "mock";

    /**
     * 分析提示模板
     */
    private String analysisPromptTemplate = "你是一个专业的基金分析师。请分析以下基金信息并给出投资建议。\n" +
            "基金信息：\n" +
            "基金代码：{fundCode}\n" +
            "基金名称：{fundName}\n" +
            "净值：{netValue}\n" +
            "涨跌幅：{changePercent}%\n" +
            "风险评估：{riskLevel}\n" +
            "\n" +
            "请提供以下分析：\n" +
            "1. 基金当前状态评估\n" +
            "2. 投资建议（买入/持有/卖出）\n" +
            "3. 风险提示\n" +
            "4. 未来展望\n" +
            "\n" +
            "请用中文回复，保持专业且易于理解。";

    public FundAnalysisAgent() {
        super(
            "fund-analysis-agent",
            "基金分析智能代理，使用LLM分析基金信息并提供投资建议",
            new String[] {
                "fund-analysis",       // 基金分析
                "investment-advice",   // 投资建议
                "risk-assessment",     // 风险评估
                "market-analysis"      // 市场分析
            },
            new String[] {
                "fundCode",        // 基金代码
                "fundName",        // 基金名称
                "netValue",        // 净值
                "changePercent",   // 涨跌幅
                "riskLevel"        // 风险评估
            }
        );
    }

    @Override
    protected AgentResult doProcess(String task, Map<String, Object> context) throws Exception {
        // 1. 提取和验证上下文信息
        validateRequiredContext(context);

        // 2. 构建分析提示
        String prompt = buildAnalysisPrompt(context);

        // 3. 调用LLM进行分析
        LlmProvider llmProvider = getLlmProvider();
        LlmRequest request = createLlmRequest(prompt);

        var llmResponse = llmProvider.call(request);

        if (!llmResponse.isSuccess()) {
            return buildErrorResult("LLM分析失败: " + llmResponse.getErrorMessage());
        }

        // 4. 解析和构建结果
        return buildAnalysisResult(llmResponse.getContent(), context);
    }

    /**
     * 验证必需的上下文信息
     */
    private void validateRequiredContext(Map<String, Object> context) {
        // 基础验证已在父类中完成，这里可以添加特定验证
        if (!context.containsKey("fundCode") || !context.containsKey("fundName")) {
            throw new IllegalArgumentException("基金分析和必需的上下文信息：fundCode, fundName");
        }
    }

    /**
     * 构建分析提示
     */
    private String buildAnalysisPrompt(Map<String, Object> context) {
        String prompt = analysisPromptTemplate;

        // 替换模板中的变量
        for (Map.Entry<String, Object> entry : context.entrySet()) {
            String key = "{" + entry.getKey() + "}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "未知";
            prompt = prompt.replace(key, value);
        }

        return prompt;
    }

    /**
     * 获取LLM提供商
     */
    private LlmProvider getLlmProvider() {
        // 优先使用设置的LLM提供商
        if (llmProvider != null && llmProviderFactory.hasProvider(llmProvider)) {
            return llmProviderFactory.getProvider(llmProvider);
        }

        // 使用默认LLM提供商
        if (llmProviderFactory.hasProvider(defaultLlmProvider)) {
            return llmProviderFactory.getProvider(defaultLlmProvider);
        }

        // 使用最佳可用提供商
        return llmProviderFactory.getBestAvailableProvider();
    }

    /**
     * 创建LLM请求
     */
    private LlmRequest createLlmRequest(String prompt) {
        Message userMessage = Message.user(prompt);

        return LlmRequest.builder()
                .messages(List.of(userMessage))
                .model("gpt-3.5-turbo") // 模型名称，实际使用时会根据提供商调整
                .temperature(0.3)       // 较低的温度以获得更确定的输出
                .maxTokens(1500)        // 足够的token用于详细分析
                .build();
    }

    /**
     * 构建分析结果
     */
    private AgentResult buildAnalysisResult(String analysis, Map<String, Object> context) {
        // 解析分析内容（简单实现）
        String fundCode = context.get("fundCode").toString();
        String fundName = context.get("fundName").toString();

        // 构建结果对象
        Map<String, Object> resultContent = Map.of(
                "fundCode", fundCode,
                "fundName", fundName,
                "analysis", analysis,
                "timestamp", System.currentTimeMillis()
        );

        // 计算置信度（简单实现，实际应根据分析质量计算）
        double confidence = calculateConfidence(analysis);

        return buildSuccessResult(resultContent, confidence, "使用LLM进行基金分析");
    }

    /**
     * 计算置信度
     */
    private double calculateConfidence(String analysis) {
        // 简单置信度计算：基于分析长度和内容关键词
        double baseConfidence = 0.7;

        // 分析长度加分
        int length = analysis.length();
        if (length > 500) baseConfidence += 0.1;
        if (length > 1000) baseConfidence += 0.1;

        // 关键词加分
        String[] positiveKeywords = {"建议", "分析", "风险", "收益", "评估", "展望"};
        int keywordCount = 0;
        for (String keyword : positiveKeywords) {
            if (analysis.contains(keyword)) {
                keywordCount++;
            }
        }
        baseConfidence += keywordCount * 0.02;

        // 限制在0.5-0.95之间
        return Math.max(0.5, Math.min(0.95, baseConfidence));
    }

    /**
     * 构建错误结果
     */
    private AgentResult buildErrorResult(String errorMessage) {
        return AgentResult.builder()
                .agentName(getName())
                .status(AgentResult.Status.ERROR)
                .content("基金分析失败")
                .errorMessage(errorMessage)
                .errorCode("FUND_ANALYSIS_ERROR")
                .build();
    }

    /**
     * 设置默认LLM提供商
     */
    public void setDefaultLlmProvider(String defaultLlmProvider) {
        this.defaultLlmProvider = defaultLlmProvider;
    }

    /**
     * 设置分析提示模板
     */
    public void setAnalysisPromptTemplate(String analysisPromptTemplate) {
        this.analysisPromptTemplate = analysisPromptTemplate;
    }

    /**
     * 获取分析提示模板
     */
    public String getAnalysisPromptTemplate() {
        return analysisPromptTemplate;
    }
}