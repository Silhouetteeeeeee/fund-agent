package com.shxc.fundagent.llm.mock;

import com.shxc.fundagent.llm.AbstractLlmProvider;
import com.shxc.fundagent.llm.ProviderType;
import com.shxc.fundagent.llm.model.LlmRequest;
import com.shxc.fundagent.llm.model.LlmResponse;
import com.shxc.fundagent.llm.model.Message;

import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Mock LLM提供商
 * 用于测试和开发，不实际调用API
 */
public class MockLlmProvider extends AbstractLlmProvider {

    private static final Random random = new Random();

    /**
     * 模拟延迟（毫秒）
     */
    private long simulatedDelayMs = 500L;

    /**
     * 成功率（0-1之间）
     */
    private double successRate = 0.95;

    /**
     * 是否启用智能回复
     */
    private boolean smartReplies = true;

    public MockLlmProvider() {
        super("mock", "mock-model", ProviderType.CUSTOM);
    }

    public MockLlmProvider(String providerName, String modelName) {
        super(providerName, modelName, ProviderType.CUSTOM);
    }

    @Override
    protected LlmResponse doCall(LlmRequest request) throws Exception {
        // 模拟网络延迟
        if (simulatedDelayMs > 0) {
            Thread.sleep(simulatedDelayMs);
        }

        // 模拟随机失败
        if (random.nextDouble() > successRate) {
            throw new RuntimeException("Mock LLM service temporarily unavailable");
        }

        // 生成模拟响应
        return generateMockResponse(request);
    }

    /**
     * 生成模拟响应
     */
    private LlmResponse generateMockResponse(LlmRequest request) {
        String responseContent;

        if (smartReplies && request.getMessages() != null && !request.getMessages().isEmpty()) {
            // 尝试生成智能回复
            responseContent = generateSmartReply(request);
        } else {
            // 生成通用回复
            responseContent = generateGenericReply();
        }

        // 随机生成token数量（基于内容长度）
        int promptTokens = estimateTokens(request);
        int completionTokens = responseContent.length() / 2; // 简单估算
        int totalTokens = promptTokens + completionTokens;

        // 计算模拟成本
        double cost = totalTokens * 0.001 / 1000; // 简单成本计算

        return LlmResponse.builder()
                .status(LlmResponse.Status.SUCCESS)
                .responseId(UUID.randomUUID().toString())
                .content(responseContent)
                .model(modelName)
                .providerName(providerName)
                .finishReason("stop")
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .cost(cost)
                .build();
    }

    /**
     * 生成智能回复
     */
    private String generateSmartReply(LlmRequest request) {
        List<Message> messages = request.getMessages();
        String lastMessage = "";

        // 获取最后一条用户消息
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message message = messages.get(i);
            if (message.getRole() == Message.Role.USER && message.getContent() != null) {
                lastMessage = message.getContent().toLowerCase();
                break;
            }
        }

        // 根据关键词生成回复
        if (lastMessage.contains("你好") || lastMessage.contains("hello") || lastMessage.contains("hi")) {
            return "你好！我是Mock LLM助手。这是一个模拟回复，用于测试LLM调用功能。";
        } else if (lastMessage.contains("基金") || lastMessage.contains("fund")) {
            return "根据分析，当前市场环境下，建议关注以下基金类型：\n" +
                   "1. 货币基金：流动性好，风险低\n" +
                   "2. 债券基金：收益稳定，适合稳健投资者\n" +
                   "3. 股票基金：长期增长潜力大，但波动性较高\n" +
                   "（注：此为模拟建议，不构成投资推荐）";
        } else if (lastMessage.contains("天气") || lastMessage.contains("weather")) {
            return "今天天气晴朗，温度适宜。建议户外活动。\n" +
                   "（模拟天气信息）";
        } else if (lastMessage.contains("帮助") || lastMessage.contains("help")) {
            return "我可以帮助您处理以下任务：\n" +
                   "1. 回答一般性问题\n" +
                   "2. 提供基金相关信息\n" +
                   "3. 进行简单的文本分析\n" +
                   "（这是Mock LLM的模拟功能）";
        } else if (lastMessage.contains("计算") || lastMessage.contains("calculate")) {
            return "计算结果：42（宇宙的答案）\n" +
                   "（模拟计算响应）";
        } else {
            return generateGenericReply();
        }
    }

    /**
     * 生成通用回复
     */
    private String generateGenericReply() {
        String[] genericReplies = {
            "这是一个模拟的LLM响应。在实际应用中，这里会是真实的AI回复。",
            "Mock LLM响应：这是一个用于开发和测试的模拟回复。",
            "您好！这是Mock LLM的回复。实际调用时会连接真实的LLM API。",
            "模拟响应：当前请求已成功处理。在真实环境中，这里会是AI生成的内容。",
            "这是一个测试响应，用于验证LLM调用框架的正确性。"
        };

        return genericReplies[random.nextInt(genericReplies.length)];
    }

    /**
     * 设置模拟延迟
     */
    public void setSimulatedDelayMs(long simulatedDelayMs) {
        this.simulatedDelayMs = simulatedDelayMs;
    }

    /**
     * 设置成功率
     */
    public void setSuccessRate(double successRate) {
        this.successRate = Math.max(0, Math.min(1, successRate));
    }

    /**
     * 设置是否启用智能回复
     */
    public void setSmartReplies(boolean smartReplies) {
        this.smartReplies = smartReplies;
    }

    @Override
    public int estimateTokens(LlmRequest request) {
        // Mock实现的token估算
        int tokens = 0;

        if (request.getSystemPrompt() != null) {
            tokens += request.getSystemPrompt().length();
        }

        if (request.getMessages() != null) {
            for (Message message : request.getMessages()) {
                if (message.getContent() != null) {
                    tokens += message.getContent().length();
                }
            }
        }

        // 加上一些固定开销
        tokens += 100;

        return tokens;
    }

    @Override
    public double estimateCost(LlmRequest request) {
        // Mock实现的成本估算
        int tokens = estimateTokens(request);
        return tokens * 0.001 / 1000 * 2; // 比默认稍高一点
    }
}