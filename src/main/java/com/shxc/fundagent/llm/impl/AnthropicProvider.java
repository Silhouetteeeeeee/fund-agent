package com.shxc.fundagent.llm.impl;

import com.shxc.fundagent.llm.ProviderType;
import com.shxc.fundagent.llm.config.LlmProperties;
import com.shxc.fundagent.llm.model.LlmRequest;
import dev.langchain4j.model.anthropic.AnthropicChatModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;

/**
 * Anthropic提供商实现
 * 基于LangChain4j的AnthropicChatModel
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "ai.llm.providers.anthropic.enabled", havingValue = "true")
public class AnthropicProvider extends LangChain4jProvider {

    @Autowired
    public AnthropicProvider(LlmProperties llmProperties) {
        super("anthropic", "claude-3-sonnet-20240229", ProviderType.ANTHROPIC);

        // 从配置中获取Anthropic设置
        LlmProperties.ProviderProperties config = llmProperties.getProviders().get("anthropic");
        if (config == null) {
            throw new IllegalStateException("Anthropic configuration not found");
        }

        // 构建AnthropicChatModel
        AnthropicChatModel.AnthropicChatModelBuilder builder = AnthropicChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModel() != null ? config.getModel() : "claude-3-sonnet-20240229")
                .temperature(config.getTemperature() != null ? config.getTemperature() : 0.3)
                .maxTokens(config.getMaxTokens() != null ? config.getMaxTokens() : 1000)
                .timeout(config.getTimeoutMs() != null ? Duration.ofMillis(config.getTimeoutMs()) : Duration.ofSeconds(30));

        // 设置基础URL（如果有）
        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        }

        // 应用其他参数
        if (config.getParameters() != null) {
            applyAdditionalParameters(builder, config.getParameters());
        }

        this.chatModel = builder.build();

        log.info("Anthropic provider initialized: model={}", config.getModel());
    }

    /**
     * 应用额外的配置参数
     */
    private void applyAdditionalParameters(AnthropicChatModel.AnthropicChatModelBuilder builder, Map<String, Object> parameters) {
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
                    case "topK":
                        if (value instanceof Number) {
                            builder.topK(((Number) value).intValue());
                        }
                        break;
                    case "maxRetries":
                        if (value instanceof Number) {
                            builder.maxRetries(((Number) value).intValue());
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
                    default:
                        log.debug("Unknown Anthropic parameter: {} = {}", key, value);
                }
            } catch (Exception e) {
                log.warn("Failed to apply parameter {}: {}", key, e.getMessage());
            }
        }
    }

    @Override
    public double estimateCost(LlmRequest request) {
        // Anthropic成本计算
        // Claude 3 Sonnet: 输入 $3 per 1M tokens, 输出 $15 per 1M tokens
        int tokens = estimateTokens(request);

        // 简单成本估算
        double inputCostPerToken = 3.0 / 1_000_000;
        double outputCostPerToken = 15.0 / 1_000_000;

        // 假设1/3为输出token
        double estimatedOutputTokens = tokens * 0.33;
        double estimatedInputTokens = tokens - estimatedOutputTokens;

        return (estimatedInputTokens * inputCostPerToken) + (estimatedOutputTokens * outputCostPerToken);
    }

    @Override
    public int estimateTokens(LlmRequest request) {
        // Anthropic有自己的tokenization方式
        // 这里使用父类估算，实际应用中可以使用更精确的估算
        return super.estimateTokens(request);
    }

    @Override
    public boolean isAvailable() {
        if (chatModel == null) {
            return false;
        }

        // Anthropic可能需要额外的可用性检查
        return super.isAvailable();
    }
}