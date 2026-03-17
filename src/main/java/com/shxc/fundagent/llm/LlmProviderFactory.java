package com.shxc.fundagent.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLM提供商工厂
 * 负责创建、管理和提供LLM提供商实例
 */
@Component
public class LlmProviderFactory {

    private static final Logger logger = LoggerFactory.getLogger(LlmProviderFactory.class);

    /**
     * 所有注册的提供商（按名称索引）
     */
    private final Map<String, LlmProvider> providers = new ConcurrentHashMap<>();

    /**
     * 提供商配置（按名称索引）
     */
    private final Map<String, ProviderConfig> providerConfigs = new ConcurrentHashMap<>();

    /**
     * 默认提供商名称
     */
    private String defaultProviderName;

    /**
     * 注册LLM提供商
     */
    public void registerProvider(LlmProvider provider) {
        String providerName = provider.getProviderName();

        if (providers.containsKey(providerName)) {
            logger.warn("LLM提供商 '{}' 已注册，正在替换", providerName);
        }

        providers.put(providerName, provider);

        // 如果没有设置默认提供商，将第一个注册的设为默认
        if (defaultProviderName == null) {
            defaultProviderName = providerName;
            logger.info("设置默认LLM提供商为: {}", providerName);
        }

        logger.info("LLM提供商注册: {} ({})", providerName, provider.getModelName());
    }

    /**
     * 注销LLM提供商
     */
    public void unregisterProvider(String providerName) {
        LlmProvider provider = providers.remove(providerName);
        if (provider != null) {
            logger.info("LLM提供商注销: {}", providerName);

            // 如果注销的是默认提供商，需要重新选择默认
            if (providerName.equals(defaultProviderName)) {
                selectNewDefaultProvider();
            }
        }
    }

    /**
     * 获取LLM提供商
     */
    public LlmProvider getProvider(String providerName) {
        return providers.get(providerName);
    }

    /**
     * 获取默认LLM提供商
     */
    public LlmProvider getDefaultProvider() {
        if (defaultProviderName == null) {
            if (!providers.isEmpty()) {
                defaultProviderName = providers.keySet().iterator().next();
            } else {
                throw new IllegalStateException("No LLM providers registered");
            }
        }
        return providers.get(defaultProviderName);
    }

    /**
     * 设置默认LLM提供商
     */
    public void setDefaultProvider(String providerName) {
        if (!providers.containsKey(providerName)) {
            throw new IllegalArgumentException("Provider not registered: " + providerName);
        }
        this.defaultProviderName = providerName;
        logger.info("默认LLM提供商设置为: {}", providerName);
    }

    /**
     * 获取所有提供商
     */
    public List<LlmProvider> getAllProviders() {
        return new ArrayList<>(providers.values());
    }

    /**
     * 获取可用提供商列表
     */
    public List<LlmProvider> getAvailableProviders() {
        return providers.values().stream()
                .filter(LlmProvider::isAvailable)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 根据提供商类型获取提供商
     */
    public List<LlmProvider> getProvidersByType(ProviderType providerType) {
        return providers.values().stream()
                .filter(provider -> provider.getProviderType() == providerType)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取最佳可用提供商
     * 策略：按优先级选择，考虑可用性、成本等因素
     */
    public LlmProvider getBestAvailableProvider() {
        // 优先使用默认提供商
        LlmProvider defaultProvider = getDefaultProvider();
        if (defaultProvider != null && defaultProvider.isAvailable()) {
            return defaultProvider;
        }

        // 寻找其他可用提供商
        List<LlmProvider> availableProviders = getAvailableProviders();
        if (!availableProviders.isEmpty()) {
            // 简单策略：选择第一个可用的
            return availableProviders.get(0);
        }

        // 没有可用提供商
        throw new IllegalStateException("No available LLM providers");
    }

    /**
     * 获取提供商统计信息
     */
    public Map<String, Object> getProviderStats() {
        Map<String, Object> stats = new HashMap<>();

        int totalProviders = providers.size();
        int availableProviders = (int) providers.values().stream()
                .filter(LlmProvider::isAvailable)
                .count();

        stats.put("totalProviders", totalProviders);
        stats.put("availableProviders", availableProviders);
        stats.put("defaultProvider", defaultProviderName);
        stats.put("providerNames", new ArrayList<>(providers.keySet()));

        // 按提供商类型统计
        Map<ProviderType, Integer> typeCount = new HashMap<>();
        for (LlmProvider provider : providers.values()) {
            ProviderType type = provider.getProviderType();
            typeCount.put(type, typeCount.getOrDefault(type, 0) + 1);
        }
        stats.put("typeCount", typeCount);

        return stats;
    }

    /**
     * 添加提供商配置
     */
    public void addProviderConfig(String providerName, ProviderConfig config) {
        providerConfigs.put(providerName, config);
        logger.debug("已添加提供商配置: {}", providerName);
    }

    /**
     * 获取提供商配置
     */
    public ProviderConfig getProviderConfig(String providerName) {
        return providerConfigs.get(providerName);
    }

    /**
     * 检查提供商是否存在
     */
    public boolean hasProvider(String providerName) {
        return providers.containsKey(providerName);
    }

    /**
     * 清理所有提供商
     */
    public void shutdown() {
        for (LlmProvider provider : providers.values()) {
            if (provider instanceof AbstractLlmProvider) {
                ((AbstractLlmProvider) provider).shutdown();
            }
        }
        providers.clear();
        providerConfigs.clear();
        defaultProviderName = null;
        logger.info("LlmProviderFactory 关闭完成");
    }

    /**
     * 重新选择默认提供商
     */
    private void selectNewDefaultProvider() {
        if (!providers.isEmpty()) {
            defaultProviderName = providers.keySet().iterator().next();
            logger.info("自动选择新的默认LLM提供商: {}", defaultProviderName);
        } else {
            defaultProviderName = null;
            logger.warn("没有可用的LLM提供商用于默认选择");
        }
    }

    /**
     * 提供商配置类
     */
    public static class ProviderConfig {
        private boolean enabled = true;
        private double costPerToken = 0.001 / 1000; // 默认成本
        private int priority = 1; // 优先级，数字越小优先级越高
        private Map<String, Object> settings = new HashMap<>();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public double getCostPerToken() {
            return costPerToken;
        }

        public void setCostPerToken(double costPerToken) {
            this.costPerToken = costPerToken;
        }

        public int getPriority() {
            return priority;
        }

        public void setPriority(int priority) {
            this.priority = priority;
        }

        public Map<String, Object> getSettings() {
            return settings;
        }

        public void setSettings(Map<String, Object> settings) {
            this.settings = settings;
        }

        public void addSetting(String key, Object value) {
            settings.put(key, value);
        }

        public Object getSetting(String key) {
            return settings.get(key);
        }
    }
}