package com.shxc.fundagent.llm.config;

import com.shxc.fundagent.llm.LlmProviderFactory;
import com.shxc.fundagent.llm.mock.MockLlmProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LLM配置类
 * 配置和初始化LLM提供商
 */
@Configuration
@ConditionalOnProperty(name = "ai.llm.enabled", havingValue = "true", matchIfMissing = false)
public class LlmConfig {

    private static final Logger logger = LoggerFactory.getLogger(LlmConfig.class);

    @Autowired
    private LlmProperties llmProperties;

    /**
     * 配置LLM提供商工厂
     */
    @Bean
    public LlmProviderFactory llmProviderFactory() {
        logger.info("Initializing LlmProviderFactory...");
        return new LlmProviderFactory();
    }

    /**
     * 注册Mock LLM提供商（用于开发和测试）
     */
    @Bean
    @ConditionalOnProperty(name = "ai.llm.providers.mock.enabled", havingValue = "true", matchIfMissing = true)
    public MockLlmProvider mockLlmProvider() {
        logger.info("Registering Mock LLM provider...");

        MockLlmProvider mockProvider = new MockLlmProvider();

        // 从配置中获取参数
        var mockConfig = llmProperties.getProviders().get("mock");
        if (mockConfig != null) {
            if (mockConfig.getModel() != null) {
                // 重新创建以使用配置的模型名称
                mockProvider = new MockLlmProvider("mock", mockConfig.getModel());
            }

            if (mockConfig.getParameters() != null) {
                var params = mockConfig.getParameters();
                if (params.containsKey("simulatedDelayMs")) {
                    mockProvider.setSimulatedDelayMs(Long.parseLong(params.get("simulatedDelayMs").toString()));
                }
                if (params.containsKey("successRate")) {
                    mockProvider.setSuccessRate(Double.parseDouble(params.get("successRate").toString()));
                }
                if (params.containsKey("smartReplies")) {
                    mockProvider.setSmartReplies(Boolean.parseBoolean(params.get("smartReplies").toString()));
                }
            }
        }

        return mockProvider;
    }

    /**
     * 初始化并注册所有LLM提供商
     * 自动注册所有LlmProvider类型的bean
     */
    @Bean
    public boolean initializeLlmProviders(LlmProviderFactory factory,
                                          java.util.List<com.shxc.fundagent.llm.LlmProvider> providers) {
        logger.info("Initializing LLM providers...");

        try {
            // 注册所有提供商bean
            for (com.shxc.fundagent.llm.LlmProvider provider : providers) {
                String providerName = provider.getProviderName();

                if (factory.hasProvider(providerName)) {
                    logger.debug("Provider '{}' already registered, skipping", providerName);
                } else {
                    factory.registerProvider(provider);
                    logger.info("Registered LLM provider: {} ({})", providerName, provider.getModelName());
                }
            }

            // 设置默认提供商
            String defaultProvider = llmProperties.getDefaultProvider();
            if (factory.hasProvider(defaultProvider)) {
                factory.setDefaultProvider(defaultProvider);
                logger.info("Default LLM provider set to: {}", defaultProvider);
            } else if (!factory.getAllProviders().isEmpty()) {
                // 使用第一个可用的提供商作为默认
                factory.setDefaultProvider(factory.getAllProviders().get(0).getProviderName());
                logger.info("Auto-selected default LLM provider: {}", factory.getAllProviders().get(0).getProviderName());
            }

            // 打印提供商统计
            var stats = factory.getProviderStats();
            logger.info("LLM providers initialized: total={}, available={}, default={}",
                    stats.get("totalProviders"),
                    stats.get("availableProviders"),
                    stats.get("defaultProvider"));

            return true;

        } catch (Exception e) {
            logger.error("Failed to initialize LLM providers", e);
            return false;
        }
    }

    /**
     * 配置成本监控器（可选）
     */
    @Bean
    @ConditionalOnProperty(name = "ai.llm.cost-control.enabled", havingValue = "true")
    public CostMonitor costMonitor(LlmProviderFactory factory) {
        logger.info("Initializing LLM cost monitor...");
        return new CostMonitor(factory, llmProperties.getCostControl());
    }

    /**
     * 成本监控器内部类
     */
    public static class CostMonitor {
        private final LlmProviderFactory factory;
        private final LlmProperties.CostControlProperties config;

        public CostMonitor(LlmProviderFactory factory, LlmProperties.CostControlProperties config) {
            this.factory = factory;
            this.config = config;
            logger.info("Cost monitor configured with monthly budget: ${}", config.getMonthlyBudget());
        }

        // 这里可以添加成本监控逻辑
        public double getEstimatedMonthlyCost() {
            // 模拟实现：返回0
            return 0.0;
        }

        public boolean isBudgetExceeded() {
            return false;
        }
    }
}