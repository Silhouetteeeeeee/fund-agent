package com.shxc.fundagent.llm.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * LLM配置属性
 * 从application.yml读取配置
 */
@Component
@ConfigurationProperties(prefix = "ai.llm")
public class LlmProperties {

    /**
     * 是否启用LLM功能
     */
    private boolean enabled = false;

    /**
     * 默认提供商名称
     */
    private String defaultProvider = "openai";

    /**
     * 提供商配置
     */
    private Map<String, ProviderProperties> providers = new HashMap<>();

    /**
     * 成本控制配置
     */
    private CostControlProperties costControl = new CostControlProperties();

    /**
     * 弹性配置
     */
    private ResilienceProperties resilience = new ResilienceProperties();

    /**
     * 日志配置
     */
    private LogProperties log = new LogProperties();

    // Getters and Setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public Map<String, ProviderProperties> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, ProviderProperties> providers) {
        this.providers = providers;
    }

    public CostControlProperties getCostControl() {
        return costControl;
    }

    public void setCostControl(CostControlProperties costControl) {
        this.costControl = costControl;
    }

    public ResilienceProperties getResilience() {
        return resilience;
    }

    public void setResilience(ResilienceProperties resilience) {
        this.resilience = resilience;
    }

    public LogProperties getLog() {
        return log;
    }

    public void setLog(LogProperties log) {
        this.log = log;
    }

    /**
     * 提供商配置
     */
    public static class ProviderProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * API密钥（建议通过环境变量配置）
         */
        private String apiKey;

        /**
         * 模型名称
         */
        private String model;

        /**
         * 基础URL（用于自托管模型）
         */
        private String baseUrl;

        /**
         * 温度参数
         */
        private Double temperature = 0.7;

        /**
         * 最大token数
         */
        private Integer maxTokens = 1000;

        /**
         * 超时时间（毫秒）
         */
        private Long timeoutMs = 30000L;

        /**
         * 其他提供商特定参数
         */
        private Map<String, Object> parameters = new HashMap<>();

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(Long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }
    }

    /**
     * 成本控制配置
     */
    public static class CostControlProperties {
        /**
         * 是否启用成本控制
         */
        private boolean enabled = true;

        /**
         * 月度预算（美元）
         */
        private Double monthlyBudget = 100.0;

        /**
         * 每次分析最大成本（美元）
         */
        private Double maxCostPerAnalysis = 0.10;

        /**
         * 每日请求限制
         */
        private Integer dailyRequestLimit = 100;

        /**
         * 是否启用成本告警
         */
        private boolean enableCostAlerts = true;

        /**
         * 告警阈值（预算百分比）
         */
        private Double alertThreshold = 0.8; // 80%

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Double getMonthlyBudget() {
            return monthlyBudget;
        }

        public void setMonthlyBudget(Double monthlyBudget) {
            this.monthlyBudget = monthlyBudget;
        }

        public Double getMaxCostPerAnalysis() {
            return maxCostPerAnalysis;
        }

        public void setMaxCostPerAnalysis(Double maxCostPerAnalysis) {
            this.maxCostPerAnalysis = maxCostPerAnalysis;
        }

        public Integer getDailyRequestLimit() {
            return dailyRequestLimit;
        }

        public void setDailyRequestLimit(Integer dailyRequestLimit) {
            this.dailyRequestLimit = dailyRequestLimit;
        }

        public boolean isEnableCostAlerts() {
            return enableCostAlerts;
        }

        public void setEnableCostAlerts(boolean enableCostAlerts) {
            this.enableCostAlerts = enableCostAlerts;
        }

        public Double getAlertThreshold() {
            return alertThreshold;
        }

        public void setAlertThreshold(Double alertThreshold) {
            this.alertThreshold = alertThreshold;
        }
    }

    /**
     * 弹性配置
     */
    public static class ResilienceProperties {
        /**
         * 是否启用弹性机制
         */
        private boolean enabled = true;

        /**
         * 重试次数
         */
        private Integer retryAttempts = 2;

        /**
         * 重试间隔（毫秒）
         */
        private Long retryIntervalMs = 1000L;

        /**
         * 超时时间（毫秒）
         */
        private Long timeoutMs = 5000L;

        /**
         * 是否启用熔断器
         */
        private boolean circuitBreakerEnabled = true;

        /**
         * 熔断器错误阈值
         */
        private Integer circuitBreakerErrorThreshold = 5;

        /**
         * 熔断器恢复时间（毫秒）
         */
        private Long circuitBreakerRecoveryMs = 60000L;

        /**
         * 降级策略：是否回退到规则引擎
         */
        private boolean fallbackToRules = true;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public Integer getRetryAttempts() {
            return retryAttempts;
        }

        public void setRetryAttempts(Integer retryAttempts) {
            this.retryAttempts = retryAttempts;
        }

        public Long getRetryIntervalMs() {
            return retryIntervalMs;
        }

        public void setRetryIntervalMs(Long retryIntervalMs) {
            this.retryIntervalMs = retryIntervalMs;
        }

        public Long getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(Long timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public boolean isCircuitBreakerEnabled() {
            return circuitBreakerEnabled;
        }

        public void setCircuitBreakerEnabled(boolean circuitBreakerEnabled) {
            this.circuitBreakerEnabled = circuitBreakerEnabled;
        }

        public Integer getCircuitBreakerErrorThreshold() {
            return circuitBreakerErrorThreshold;
        }

        public void setCircuitBreakerErrorThreshold(Integer circuitBreakerErrorThreshold) {
            this.circuitBreakerErrorThreshold = circuitBreakerErrorThreshold;
        }

        public Long getCircuitBreakerRecoveryMs() {
            return circuitBreakerRecoveryMs;
        }

        public void setCircuitBreakerRecoveryMs(Long circuitBreakerRecoveryMs) {
            this.circuitBreakerRecoveryMs = circuitBreakerRecoveryMs;
        }

        public boolean isFallbackToRules() {
            return fallbackToRules;
        }

        public void setFallbackToRules(boolean fallbackToRules) {
            this.fallbackToRules = fallbackToRules;
        }
    }

    /**
     * 日志配置
     */
    public static class LogProperties {
        /**
         * 是否启用请求/响应日志
         */
        private boolean enabled = true;

        /**
         * 是否记录完整请求
         */
        private boolean logFullRequest = false;

        /**
         * 是否记录完整响应
         */
        private boolean logFullResponse = false;

        /**
         * 是否记录成本信息
         */
        private boolean logCost = true;

        /**
         * 是否记录性能指标
         */
        private boolean logPerformance = true;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isLogFullRequest() {
            return logFullRequest;
        }

        public void setLogFullRequest(boolean logFullRequest) {
            this.logFullRequest = logFullRequest;
        }

        public boolean isLogFullResponse() {
            return logFullResponse;
        }

        public void setLogFullResponse(boolean logFullResponse) {
            this.logFullResponse = logFullResponse;
        }

        public boolean isLogCost() {
            return logCost;
        }

        public void setLogCost(boolean logCost) {
            this.logCost = logCost;
        }

        public boolean isLogPerformance() {
            return logPerformance;
        }

        public void setLogPerformance(boolean logPerformance) {
            this.logPerformance = logPerformance;
        }
    }
}