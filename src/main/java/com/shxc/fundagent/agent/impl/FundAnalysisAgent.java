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
import java.util.concurrent.CompletableFuture;

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

    private String llmProvider = "deepseek";

    /**
     * 分析提示模板 - 多只基金版本
     */
    private String multiAnalysisPromptTemplate = "你是一个专业的基金分析师。请根据以下 {fundCount} 只基金的信息和持仓情况进行分析并给出投资建议。\n" +
            "\n" +
            "=== 基金列表及持仓 ===\n" +
            "{fundList}\n" +
            "\n" +
            "=== 整体账户情况 ===\n" +
            "总资产：{totalAssets} 元\n" +
            "总投入：{totalCost} 元\n" +
            "总收益：{totalProfit} 元\n" +
            "总收益率：{totalProfitRate}%\n" +
            "可用资金：{availableCash} 元\n" +
            "目标仓位：{targetPosition}%\n" +
            "\n" +
            "请提供以下分析：\n" +
            "1. **整体市场评估**：基于这些基金的表现，分析当前市场状况\n" +
            "2. **逐只基金分析**：对每只基金分别进行评估\n" +
            "   - 当前状态（表现优劣）\n" +
            "   - 持仓盈亏分析\n" +
            "   - 投资建议（买入/持有/卖出）及操作建议\n" +
            "   - 主要风险点\n" +
            "3. **组合分析**：\n" +
            "   - 当前仓位是否合理\n" +
            "   - 资产配置是否均衡\n" +
            "   - 是否需要调仓及如何调仓\n" +
            "4. **风险提示**：整体风险和个别风险提示\n" +
            "5. **未来展望**：短期和长期展望\n" +
            "\n" +
            "要求：\n" +
            "- 使用表格或结构化格式呈现，便于对比\n" +
            "- 突出表现最好和最差的基金\n" +
            "- 给出具体的操作建议（包括加仓/减仓金额）\n" +
            "- 用中文回复，保持专业且易于理解";


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
                "funds"          // 多只基金
            }
        );
    }

    @Override
    protected AgentResult doProcess(String task, String message) throws Exception {
        // 3. 调用LLM进行分析
        LlmProvider llmProvider = getLlmProvider();
        LlmRequest request = createLlmRequest(message);

        var llmResponse = llmProvider.call(request);

        if (!llmResponse.isSuccess()) {
            return buildErrorResult("LLM分析失败: " + llmResponse.getErrorMessage());
        }

        // 4. 解析和构建结果
        return buildAnalysisResult(llmResponse.getContent());
    }

    /**
     * 验证上下文并选择模板
     */
    private String validateAndSelectTemplate(Map<String, Object> context) {
        if (context.containsKey("funds")) {
            Object fundsObj = context.get("funds");
            if (!(fundsObj instanceof List)) {
                throw new IllegalArgumentException("funds 必须是 List 类型");
            }
            List<?> funds = (List<?>) fundsObj;
            if (funds.isEmpty()) {
                throw new IllegalArgumentException("funds 列表不能为空");
            }
            return multiAnalysisPromptTemplate;
        }
        throw new IllegalArgumentException("不能不包含funds");
    }

    /**
     * 构建多基金提示词
     */
    private String buildMultiFundPrompt(String prompt, Map<String, Object> context) {
        List<Map<String, Object>> funds = (List<Map<String, Object>>) context.get("funds");

        StringBuilder fundListBuilder = new StringBuilder();
        for (int i = 0; i < funds.size(); i++) {
            Map<String, Object> fund = funds.get(i);
            fundListBuilder.append("--- 基金 ").append(i + 1).append(" ---\n");
            fundListBuilder.append("基金代码：").append(fund.getOrDefault("fundCode", "未知")).append("\n");
            fundListBuilder.append("基金名称：").append(fund.getOrDefault("fundName", "未知")).append("\n");
            fundListBuilder.append("当前净值：").append(fund.getOrDefault("netValue", "未知")).append("\n");
            fundListBuilder.append("今日涨跌幅：").append(fund.getOrDefault("changePercent", "未知")).append("%\n");
            fundListBuilder.append("风险评估：").append(fund.getOrDefault("riskLevel", "未知")).append("\n");

            // 持仓信息
            fundListBuilder.append("\n【我的持仓】\n");
            fundListBuilder.append("持有份额：").append(formatNumber(fund.get("holdShares"))).append(" 份\n");
            fundListBuilder.append("持仓市值：").append(formatNumber(fund.get("holdAmount"))).append(" 元\n");
            fundListBuilder.append("持仓成本：").append(formatNumber(fund.get("avgCost"))).append(" 元\n");
            fundListBuilder.append("投入本金：").append(formatNumber(fund.get("costAmount"))).append(" 元\n");
            fundListBuilder.append("持有收益：").append(formatNumber(fund.get("profit"))).append(" 元\n");
            fundListBuilder.append("持有收益率：").append(formatNumber(fund.get("profitRate"))).append("%\n");
            fundListBuilder.append("仓位占比：").append(formatPosition(fund.get("position"))).append("%\n");
            fundListBuilder.append("持有天数：").append(formatNumber(fund.get("holdDays"))).append(" 天\n");

            if (fund.containsKey("yieldRate")) {
                fundListBuilder.append("今年以来收益：").append(formatNumber(fund.get("yieldRate"))).append("%\n");
            }
            if (fund.containsKey("holdIndustry")) {
                fundListBuilder.append("持仓行业：").append(fund.get("holdIndustry")).append("\n");
            }
            fundListBuilder.append("\n");
        }

        prompt = prompt.replace("{fundCount}", String.valueOf(funds.size()));
        prompt = prompt.replace("{fundList}", fundListBuilder.toString().trim());

        // 替换整体账户信息
        prompt = prompt.replace("{totalAssets}", formatNumber(context.get("totalAssets")));
        prompt = prompt.replace("{totalCost}", formatNumber(context.get("totalCost")));
        prompt = prompt.replace("{totalProfit}", formatNumber(context.get("totalProfit")));
        prompt = prompt.replace("{totalProfitRate}", formatNumber(context.get("totalProfitRate")));
        prompt = prompt.replace("{availableCash}", formatNumber(context.get("availableCash")));
        prompt = prompt.replace("{targetPosition}", formatPosition(context.get("targetPosition")));

        return prompt;
    }

    /**
     * 格式化数字（保留 2 位小数）
     */
    private String formatNumber(Object value) {
        if (value == null) return "0.00";
        try {
            return String.format("%.2f", Double.parseDouble(value.toString()));
        } catch (Exception e) {
            return value.toString();
        }
    }

    /**
     * 格式化仓位（转换为百分比）
     */
    private String formatPosition(Object position) {
        if (position == null) return "0.00";
        try {
            double pos = Double.parseDouble(position.toString());
            // 如果是 0-1 之间的小数，转换为百分比
            if (pos >= 0 && pos <= 1) {
                return String.format("%.2f", pos * 100);
            }
            return String.format("%.2f", pos);
        } catch (Exception e) {
            return position.toString();
        }
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
    private AgentResult buildAnalysisResult(String analysis) {
        // 构建结果对象
        Map<String, Object> resultContent = Map.of(
                "analysis", analysis,
                "timestamp", System.currentTimeMillis()
        );

        // 计算置信度（简单实现，实际应根据分析质量计算）

        return buildSuccessResult(resultContent, 1d, "使用LLM进行基金分析");
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

    public String getMultiAnalysisPromptTemplate() {
        return multiAnalysisPromptTemplate;
    }

    public void setMultiAnalysisPromptTemplate(String multiAnalysisPromptTemplate) {
        this.multiAnalysisPromptTemplate = multiAnalysisPromptTemplate;
    }

    @Override
    public CompletableFuture<AgentResult> processAsync(String task, String msg) {
        return null;
    }
}