package com.shxc.fundagent.llm.impl;

import com.shxc.fundagent.llm.ProviderType;
import com.shxc.fundagent.llm.config.LlmProperties;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * DeepSeek LLM 提供商
 * 基于 OpenAI 兼容接口
 */
@Component
@ConditionalOnProperty(name = "ai.llm.providers.deepseek.enabled", havingValue = "true")
public class DeepSeekProvider extends LangChain4jProvider {

    @Autowired
    public DeepSeekProvider(LlmProperties llmProperties) {
        super("deepseek", "deepseek-chat", ProviderType.DEEPSEEK);

        // 从配置中获取 DeepSeek 设置
        LlmProperties.ProviderProperties config = llmProperties.getProviders().get("deepseek");
        if (config == null) {
            throw new IllegalStateException("DeepSeek configuration not found");
        }

        // 构建 OpenAiChatModel（DeepSeek 兼容 OpenAI API）
        OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModel() != null ? config.getModel() : "deepseek-chat")
                .temperature(config.getTemperature() != null ? config.getTemperature() : 0.3)
                .maxTokens(config.getMaxTokens() != null ? config.getMaxTokens() : 2000)
                .timeout(config.getTimeoutMs() != null ? Duration.ofMillis(config.getTimeoutMs()) : Duration.ofSeconds(30));

        // 设置 DeepSeek 的 Base URL
        if (config.getBaseUrl() != null && !config.getBaseUrl().isEmpty()) {
            builder.baseUrl(config.getBaseUrl());
        } else {
            builder.baseUrl("https://api.deepseek.com");
        }

        this.chatModel = builder.build();

        logger.info("DeepSeek provider initialized: model={}, baseUrl={}",
                config.getModel(), config.getBaseUrl());
    }
}
