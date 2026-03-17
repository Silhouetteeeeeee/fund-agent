package com.shxc.fundagent.agent;

import com.shxc.fundagent.agent.model.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 抽象Agent基类
 * 提供Agent的通用功能实现
 */
public abstract class AbstractAgent implements Agent {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Agent名称
     */
    protected final String name;

    /**
     * Agent描述
     */
    protected final String description;

    /**
     * Agent能力
     */
    protected final String[] capabilities;

    /**
     * 支持的上下文类型
     */
    protected final String[] supportedContextTypes;

    /**
     * Agent状态
     */
    protected volatile AgentStatus status = AgentStatus.READY;

    /**
     * LLM提供商名称
     */
    protected String llmProvider;

    /**
     * 异步执行器
     */
    protected ExecutorService asyncExecutor;

    /**
     * 是否启用
     */
    protected volatile boolean enabled = true;

    /**
     * 最大处理时间（毫秒）
     */
    protected long maxProcessingTimeMs = 30000L;

    protected AbstractAgent(String name, String description, String[] capabilities, String[] supportedContextTypes) {
        this.name = name;
        this.description = description;
        this.capabilities = capabilities;
        this.supportedContextTypes = supportedContextTypes;
        this.asyncExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public AgentResult process(String task, String msg) {
        long startTime = System.currentTimeMillis();

        try {
            // 检查Agent状态
            if (!isAvailable()) {
                return buildUnavailableResult();
            }

            // 设置状态为忙碌
            setStatus(AgentStatus.BUSY);

            // 实际处理（由子类实现）
            AgentResult result = doProcess(task, msg);

            // 设置处理时间
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            result.setAgentName(name);

            // 设置状态为就绪
            setStatus(AgentStatus.READY);

            logger.debug("Agent {} processed task successfully: {}", name, task);
            return result;

        } catch (Exception e) {
            // 处理异常
            setStatus(AgentStatus.ERROR);
            logger.error("Agent {} failed to process task: {}", name, task, e);

            return buildErrorResult(e, System.currentTimeMillis() - startTime);
        }
    }

    @Override
    public CompletableFuture<AgentResult> processAsync(String task, String msg) {
        return CompletableFuture.supplyAsync(() -> process(task, msg), asyncExecutor);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String[] getCapabilities() {
        return capabilities.clone();
    }

    @Override
    public boolean isAvailable() {
        return enabled && (status == AgentStatus.READY || status == AgentStatus.BUSY);
    }

    @Override
    public AgentStatus getStatus() {
        return status;
    }

    @Override
    public void reset() {
        setStatus(AgentStatus.READY);
        logger.info("Agent {} reset to READY state", name);
    }

    @Override
    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
        logger.debug("Agent {} set LLM provider to: {}", name, llmProvider);
    }

    @Override
    public String[] getSupportedContextTypes() {
        return supportedContextTypes.clone();
    }

    /**
     * 设置Agent状态
     */
    protected void setStatus(AgentStatus status) {
        this.status = status;
        logger.debug("Agent {} status changed to: {}", name, status);
    }

    /**
     * 启用/禁用Agent
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        logger.info("Agent {} {}", name, enabled ? "enabled" : "disabled");
    }

    /**
     * 设置最大处理时间
     */
    public void setMaxProcessingTimeMs(long maxProcessingTimeMs) {
        this.maxProcessingTimeMs = maxProcessingTimeMs;
    }

    /**
     * 设置异步执行器
     */
    public void setAsyncExecutor(ExecutorService asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
    }

    /**
     * 验证上下文是否有效
     */
    protected boolean validateContext(Map<String, Object> context) {
        if (context == null) {
            return true; // 允许空上下文
        }

        // 检查是否包含必需的上下文类型
        if (supportedContextTypes != null && supportedContextTypes.length > 0) {
            for (String requiredType : supportedContextTypes) {
                if (!context.containsKey(requiredType)) {
                    logger.warn("Agent {} requires context type: {}", name, requiredType);
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 构建不可用结果
     */
    protected AgentResult buildUnavailableResult() {
        return AgentResult.builder()
                .agentName(name)
                .status(AgentResult.Status.ERROR)
                .content("Agent is not available")
                .errorMessage("Agent " + name + " is not available")
                .errorCode("AGENT_UNAVAILABLE")
                .build();
    }

    /**
     * 构建无效上下文结果
     */
    protected AgentResult buildInvalidContextResult(Map<String, Object> context) {
        return AgentResult.builder()
                .agentName(name)
                .status(AgentResult.Status.INVALID_INPUT)
                .content("Invalid or missing context")
                .errorMessage("Agent " + name + " requires specific context types")
                .errorCode("INVALID_CONTEXT")
                .extraData(Map.of("requiredContextTypes", supportedContextTypes,
                                 "providedContextKeys", context != null ? context.keySet() : "null"))
                .build();
    }

    /**
     * 构建错误结果
     */
    protected AgentResult buildErrorResult(Exception e, long processingTimeMs) {
        return AgentResult.builder()
                .agentName(name)
                .status(AgentResult.Status.ERROR)
                .content("Agent processing failed")
                .errorMessage(e.getMessage())
                .errorCode("AGENT_PROCESSING_ERROR")
                .processingTimeMs(processingTimeMs)
                .build();
    }

    /**
     * 构建成功结果
     */
    protected AgentResult buildSuccessResult(Object content, Double confidence, String reasoning) {
        return AgentResult.builder()
                .agentName(name)
                .status(AgentResult.Status.SUCCESS)
                .content(content)
                .confidence(confidence)
                .reasoning(reasoning)
                .build();
    }

    /**
     * 构建需要人工干预的结果
     */
    protected AgentResult buildNeedsHumanInterventionResult(Object content, String reasoning, String suggestedAction) {
        return AgentResult.builder()
                .agentName(name)
                .status(AgentResult.Status.NEEDS_HUMAN_INTERVENTION)
                .content(content)
                .reasoning(reasoning)
                .suggestedAction(suggestedAction)
                .build();
    }

    /**
     * 清理资源
     */
    public void shutdown() {
        if (asyncExecutor != null && !asyncExecutor.isShutdown()) {
            asyncExecutor.shutdown();
        }
        logger.info("Agent {} shutdown completed", name);
    }

    /**
     * 检查Agent是否支持特定能力
     */
    public boolean hasCapability(String capability) {
        if (capabilities == null) {
            return false;
        }
        for (String cap : capabilities) {
            if (cap.equalsIgnoreCase(capability)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查Agent是否支持特定上下文类型
     */
    public boolean supportsContextType(String contextType) {
        if (supportedContextTypes == null) {
            return false;
        }
        for (String type : supportedContextTypes) {
            if (type.equalsIgnoreCase(contextType)) {
                return true;
            }
        }
        return false;
    }

    protected abstract AgentResult doProcess(String task, String message) throws Exception;

}