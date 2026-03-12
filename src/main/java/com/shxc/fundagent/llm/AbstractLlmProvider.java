package com.shxc.fundagent.llm;

import com.shxc.fundagent.llm.model.LlmRequest;
import com.shxc.fundagent.llm.model.LlmResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 抽象LLM提供商基类
 * 提供重试、超时、日志、监控等通用功能
 */
public abstract class AbstractLlmProvider implements LlmProvider {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 提供商名称
     */
    protected final String providerName;

    /**
     * 模型名称
     */
    protected final String modelName;

    /**
     * 提供商类型
     */
    protected final ProviderType providerType;

    /**
     * 默认超时时间（毫秒）
     */
    protected long defaultTimeoutMs = 30000L;

    /**
     * 异步执行线程池
     */
    protected ExecutorService asyncExecutor;

    /**
     * 是否可用标志
     */
    protected volatile boolean available = true;

    /**
     * 最后错误时间
     */
    protected volatile long lastErrorTime = 0;

    /**
     * 错误计数
     */
    protected volatile int errorCount = 0;

    /**
     * 最大错误次数后熔断
     */
    protected static final int MAX_ERROR_COUNT = 5;

    /**
     * 熔断恢复时间（毫秒）
     */
    protected static final long CIRCUIT_BREAKER_RECOVERY_MS = 60000L; // 1分钟

    protected AbstractLlmProvider(String providerName, String modelName, ProviderType providerType) {
        this.providerName = providerName;
        this.modelName = modelName;
        this.providerType = providerType;
        this.asyncExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public LlmResponse call(LlmRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 检查熔断器
            checkCircuitBreaker();

            // 应用默认超时
            if (request.getTimeoutMs() == null) {
                request.setTimeoutMs(defaultTimeoutMs);
            }

            // 实际调用（由子类实现）
            LlmResponse response = doCall(request);

            // 成功处理
            handleSuccess(response, System.currentTimeMillis() - startTime);

            return response;

        } catch (Exception e) {
            // 错误处理
            handleError(e, System.currentTimeMillis() - startTime);

            // 构建错误响应
            return buildErrorResponse(request, e);
        }
    }

    @Override
    public CompletableFuture<LlmResponse> callAsync(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> call(request), asyncExecutor);
    }

    @Override
    public String getProviderName() {
        return providerName;
    }

    @Override
    public String getModelName() {
        return modelName;
    }

    @Override
    public ProviderType getProviderType() {
        return providerType;
    }

    @Override
    public boolean isAvailable() {
        if (!available) {
            // 检查是否可以恢复
            long now = System.currentTimeMillis();
            if (now - lastErrorTime > CIRCUIT_BREAKER_RECOVERY_MS) {
                available = true;
                errorCount = 0;
                logger.info("Circuit breaker recovered for provider: {}", providerName);
            }
        }
        return available;
    }

    @Override
    public int estimateTokens(LlmRequest request) {
        // 简单估算：每个中文字符算1个token，每个英文字符算0.3个token
        if (request.getMessages() == null) {
            return 0;
        }

        int totalTokens = 0;
        if (request.getSystemPrompt() != null) {
            totalTokens += estimateTextTokens(request.getSystemPrompt());
        }

        for (var message : request.getMessages()) {
            if (message.getContent() != null) {
                totalTokens += estimateTextTokens(message.getContent());
            }
        }

        return totalTokens;
    }

    @Override
    public double estimateCost(LlmRequest request) {
        // 默认估算：0.001美元每千token（实际价格因提供商而异）
        int tokens = estimateTokens(request);
        return tokens * 0.001 / 1000;
    }

    /**
     * 设置默认超时时间
     */
    public void setDefaultTimeoutMs(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }


    /**
     * 设置异步执行器
     */
    public void setAsyncExecutor(ExecutorService asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * 实际调用LLM的具体实现（由子类实现）
     */
    protected abstract LlmResponse doCall(LlmRequest request) throws Exception;

    /**
     * 检查熔断器状态
     */
    protected void checkCircuitBreaker() {
        if (!available) {
            long now = System.currentTimeMillis();
            if (now - lastErrorTime > CIRCUIT_BREAKER_RECOVERY_MS) {
                available = true;
                errorCount = 0;
                logger.info("Circuit breaker recovered for provider: {}", providerName);
            } else {
                throw new RuntimeException("Circuit breaker open for provider: " + providerName);
            }
        }
    }

    /**
     * 处理成功调用
     */
    protected void handleSuccess(LlmResponse response, long elapsedTime) {
        response.setResponseTimeMs(elapsedTime);
        response.setProviderName(providerName);
        response.setModel(modelName);

        // 记录成功日志
        logger.debug("LLM call succeeded: provider={}, model={}, time={}ms",
                providerName, modelName, elapsedTime);

        // 重置错误计数
        if (errorCount > 0) {
            errorCount = 0;
        }
    }

    /**
     * 处理错误
     */
    protected void handleError(Exception e, long elapsedTime) {
        errorCount++;
        lastErrorTime = System.currentTimeMillis();

        logger.error("LLM call failed: provider={}, model={}, time={}ms, error={}",
                providerName, modelName, elapsedTime, e.getMessage(), e);

        // 如果错误次数过多，触发熔断
        if (errorCount >= MAX_ERROR_COUNT) {
            available = false;
            logger.warn("Circuit breaker opened for provider: {} due to {} consecutive errors",
                    providerName, errorCount);
        }
    }

    /**
     * 构建错误响应
     */
    protected LlmResponse buildErrorResponse(LlmRequest request, Exception e) {
        LlmResponse.Status status;
        String errorCode = "UNKNOWN_ERROR";

        if (e instanceof TimeoutException) {
            status = LlmResponse.Status.TIMEOUT;
            errorCode = "TIMEOUT";
        } else if (e instanceof RuntimeException && e.getMessage() != null &&
                   e.getMessage().contains("rate limit")) {
            status = LlmResponse.Status.RATE_LIMITED;
            errorCode = "RATE_LIMITED";
        } else if (!available) {
            status = LlmResponse.Status.PROVIDER_UNAVAILABLE;
            errorCode = "PROVIDER_UNAVAILABLE";
        } else {
            status = LlmResponse.Status.ERROR;
            errorCode = "API_ERROR";
        }

        return LlmResponse.builder()
                .status(status)
                .content("LLM service error: " + e.getMessage())
                .providerName(providerName)
                .model(modelName)
                .errorMessage(e.getMessage())
                .errorCode(errorCode)
                .build();
    }

    /**
     * 估算文本token数（简单实现）
     */
    private int estimateTextTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // 简单估算：中文为主的项目，每个字符算1个token
        // 实际应该使用更精确的tokenizer
        return text.length();
    }

    /**
     * 清理资源
     */
    public void shutdown() {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}