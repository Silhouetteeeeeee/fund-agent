package com.shxc.fundagent.llm.impl;

import com.shxc.fundagent.llm.ProviderType;
import com.shxc.fundagent.llm.config.LlmProperties;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModelName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * OpenAI提供商实现
 * 基于LangChain4j的OpenAiChatModel
 */
@Component
@ConditionalOnProperty(name = "ai.llm.providers.openai.enabled", havingValue = "true")
public class OpenAiProvider extends LangChain4jProvider {

    @Autowired
    public OpenAiProvider(LlmProperties llmProperties) {
        super("openai", OpenAiChatModelName.GPT_3_5_TURBO.toString(), ProviderType.OPENAI);

        // 从配置中获取OpenAI设置
        LlmProperties.ProviderProperties config = llmProperties.getProviders().get("openai");
        if (config == null) {
            throw new IllegalStateException("OpenAI configuration not found");
        }

        // 构建OpenAiChatModel
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModel() != null ? config.getModel() : OpenAiChatModelName.GPT_3_5_TURBO.toString())
                .temperature(config.getTemperature() != null ? config.getTemperature() : 0.7)
                .maxTokens(config.getMaxTokens() != null ? config.getMaxTokens() : 1000)
                .timeout(config.getTimeoutMs() != null ? Duration.ofMillis(config.getTimeoutMs()) : Duration.ofSeconds(30));

        // 设置基础URL（如果有，用于自托管或代理）
        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        }

        // 应用其他参数
        if (config.getParameters() != null) {
            applyAdditionalParameters(builder, config.getParameters());
        }

        this.chatModel = builder.build();

        logger.info("OpenAI provider initialized: model={}, baseUrl={}",
                config.getModel(), config.getBaseUrl());
    }

    /**
     * 应用额外的配置参数
     */
    private void applyAdditionalParameters(OpenAiChatModel.OpenAiChatModelBuilder builder, Map<String, Object> parameters) {
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            try {
                switch (key) {
                    case "topP":
                        if (value instanceof Number) {
                            builder.topP(((Number) value).doubleValue());
                        }
                        break;
                    case "frequencyPenalty":
                        if (value instanceof Number) {
                            builder.frequencyPenalty(((Number) value).doubleValue());
                        }
                        break;
                    case "presencePenalty":
                        if (value instanceof Number) {
                            builder.presencePenalty(((Number) value).doubleValue());
                        }
                        break;
                    case "logRequests":
                        if (value instanceof Boolean) {
                            builder.logRequests((Boolean) value);
                        }
                        break;
                    case "logResponses":
                        if (value instanceof Boolean) {
                            builder.logResponses((Boolean) value);
                        }
                        break;
                    case "maxRetries":
                        if (value instanceof Number) {
                            builder.maxRetries(((Number) value).intValue());
                        }
                        break;
                    default:
                        logger.debug("Unknown OpenAI parameter: {} = {}", key, value);
                }
            } catch (Exception e) {
                logger.warn("Failed to apply parameter {}: {}", key, e.getMessage());
            }
        }
    }

    @Override
    public double estimateCost(LlmRequest request) {
        // OpenAI成本计算
        // 实际成本取决于模型和token数量
        int tokens = estimateTokens(request);

        // 简单成本估算（基于GPT-3.5-turbo定价）
        // 输入: $0.50 per 1M tokens, 输出: $1.50 per 1M tokens
        double inputCostPerToken = 0.50 / 1_000_000;
        double outputCostPerToken = 1.50 / 1_000_000;

        // 假设1/3为输出token（简化估算）
        double estimatedOutputTokens = tokens * 0.33;
        double estimatedInputTokens = tokens - estimatedOutputTokens;

        return (estimatedInputTokens * inputCostPerToken) + (estimatedOutputTokens * outputCostPerToken);
    }

    @Override
    public boolean isAvailable() {
        // 额外检查：API密钥是否设置
        if (chatModel == null) {
            return false;
        }

        // 可以添加ping测试，但为了性能，暂时只检查模型是否存在
        return super.isAvailable();
    }
}