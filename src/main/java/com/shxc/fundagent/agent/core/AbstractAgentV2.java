package com.shxc.fundagent.agent.core;

import com.shxc.fundagent.agent.AbstractAgent;
import com.shxc.fundagent.agent.AgentStatus;
import com.shxc.fundagent.agent.capabilities.MemoryManager;
import com.shxc.fundagent.agent.capabilities.Tool;
import com.shxc.fundagent.agent.capabilities.ToolCaller;
import com.shxc.fundagent.agent.model.v2.AgentContext;
import com.shxc.fundagent.agent.model.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 抽象Agent v2基类
 * 提供v2功能的通用实现，同时保持与v1的兼容性
 */
public abstract class AbstractAgentV2 extends AbstractAgent implements AgentV2 {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 工具调用器
     */
    protected ToolCaller toolCaller;

    /**
     * 记忆管理器
     */
    protected MemoryManager memoryManager;

    /**
     * 可用工具列表
     */
    protected final List<Tool> availableTools = new ArrayList<>();

    /**
     * 支持的输出模式列表
     */
    protected final List<String> supportedOutputSchemas = new ArrayList<>();

    /**
     * Agent配置
     */
    protected AgentConfig config;

    /**
     * 构造函数
     */
    protected AbstractAgentV2(String name, String description, String[] capabilities, String[] supportedContextTypes) {
        super(name, description, capabilities, supportedContextTypes);
        initDefaultConfig();
        initDefaultOutputSchemas();
    }

    /**
     * 初始化默认配置
     */
    protected void initDefaultConfig() {
        this.config = new DefaultAgentConfig();
    }

    /**
     * 初始化默认输出模式
     */
    protected void initDefaultOutputSchemas() {
        supportedOutputSchemas.add("default");
        supportedOutputSchemas.add("json");
        supportedOutputSchemas.add("structured");
    }

    @Override
    public AgentResult processWithTools(String task, AgentContext context) {
        long startTime = System.currentTimeMillis();

        try {
            // 检查Agent状态
            if (!isAvailable()) {
                return buildUnavailableResult();
            }

            // 验证上下文
            ValidationResult validationResult = validateContext(context);
            if (!validationResult.isValid()) {
                return buildInvalidContextResult(context, validationResult);
            }

            // 设置状态为忙碌
            setStatus(AgentStatus.BUSY);

            // 实际处理（由子类实现）
            AgentResult result = doProcessWithTools(task, context);

            // 设置处理时间
            result.setProcessingTimeMs(System.currentTimeMillis() - startTime);
            result.setAgentName(getName());

            // 设置状态为就绪
            setStatus(AgentStatus.READY);

            logger.debug("Agent {} processed task with tools successfully: {}", getName(), task);
            return result;

        } catch (Exception e) {
            // 处理异常
            setStatus(AgentStatus.ERROR);
            logger.error("Agent {} failed to process task with tools: {}", getName(), task, e);

            return buildErrorResult(e, System.currentTimeMillis() - startTime);
        }
    }

    @Override
    public CompletableFuture<AgentResult> processWithToolsAsync(String task, AgentContext context) {
        return CompletableFuture.supplyAsync(() -> processWithTools(task, context), getAsyncExecutor());
    }

    @Override
    public List<Tool> getAvailableTools() {
        return new ArrayList<>(availableTools);
    }

    @Override
    public boolean supportsToolCalling() {
        return toolCaller != null && !availableTools.isEmpty();
    }

    @Override
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    @Override
    public boolean supportsMemoryManagement() {
        return memoryManager != null && memoryManager.isAvailable();
    }

    @Override
    public List<String> getSupportedOutputSchemas() {
        return new ArrayList<>(supportedOutputSchemas);
    }

    @Override
    public String getDefaultOutputSchema() {
        return config.getDefaultOutputSchema();
    }

    @Override
    public ValidationResult validateContext(AgentContext context) {
        if (context == null) {
            return ValidationResult.invalid("Agent context cannot be null", "CONTEXT_NULL");
        }

        // 检查必要的上下文数据
        if (context.getTask() == null || context.getTask().trim().isEmpty()) {
            return ValidationResult.invalid("Task description is required", "TASK_REQUIRED");
        }

        // 检查工具调用配置
        if (context.getToolConfig() != null && context.getToolConfig().isEnabled() && !supportsToolCalling()) {
            return ValidationResult.invalid("Tool calling is not supported by this agent", "TOOLS_NOT_SUPPORTED");
        }

        // 检查记忆配置
        if (context.getMemoryConfig() != null && context.getMemoryConfig().isEnabled() && !supportsMemoryManagement()) {
            return ValidationResult.invalid("Memory management is not supported by this agent", "MEMORY_NOT_SUPPORTED");
        }

        // 检查输出模式
        if (context.getOutputConfig() != null && context.getOutputConfig().getSchemaName() != null) {
            String schemaName = context.getOutputConfig().getSchemaName();
            if (!supportedOutputSchemas.contains(schemaName)) {
                return ValidationResult.invalid(
                        String.format("Output schema '%s' is not supported. Supported schemas: %s",
                                schemaName, supportedOutputSchemas),
                        "UNSUPPORTED_OUTPUT_SCHEMA"
                );
            }
        }

        return ValidationResult.valid();
    }

    @Override
    public AgentConfig getConfig() {
        return config;
    }

    /**
     * 设置工具调用器
     */
    public void setToolCaller(ToolCaller toolCaller) {
        this.toolCaller = toolCaller;
        logger.debug("Agent {} set tool caller", getName());
    }

    /**
     * 设置记忆管理器
     */
    public void setMemoryManager(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        logger.debug("Agent {} set memory manager", getName());
    }

    /**
     * 添加工具
     */
    public void addTool(Tool tool) {
        if (tool != null) {
            availableTools.add(tool);
            logger.debug("Agent {} added tool: {}", getName(), tool.getName());
        }
    }

    /**
     * 添加多个工具
     */
    public void addTools(Collection<Tool> tools) {
        if (tools != null) {
            availableTools.addAll(tools);
            logger.debug("Agent {} added {} tools", getName(), tools.size());
        }
    }

    /**
     * 添加支持的输出模式
     */
    public void addSupportedOutputSchema(String schema) {
        if (schema != null && !supportedOutputSchemas.contains(schema)) {
            supportedOutputSchemas.add(schema);
            logger.debug("Agent {} added output schema: {}", getName(), schema);
        }
    }

    /**
     * 设置配置
     */
    public void setConfig(AgentConfig config) {
        this.config = config;
        logger.debug("Agent {} configuration updated", getName());
    }

    /**
     * 获取指定名称的工具
     */
    protected Tool getToolByName(String toolName) {
        return availableTools.stream()
                .filter(tool -> tool.getName().equals(toolName))
                .findFirst()
                .orElse(null);
    }

    /**
     * 调用工具
     */
    protected com.shxc.fundagent.agent.capabilities.ToolResult callTool(String toolName, Map<String, Object> parameters) {
        if (toolCaller == null) {
            return com.shxc.fundagent.agent.capabilities.ToolResult.error(
                    "Tool caller is not available", "TOOL_CALLER_UNAVAILABLE"
            );
        }

        Tool tool = getToolByName(toolName);
        if (tool == null) {
            return com.shxc.fundagent.agent.capabilities.ToolResult.error(
                    String.format("Tool '%s' not found", toolName), "TOOL_NOT_FOUND"
            );
        }

        if (!tool.isAvailable()) {
            return com.shxc.fundagent.agent.capabilities.ToolResult.error(
                    String.format("Tool '%s' is not available", toolName), "TOOL_UNAVAILABLE"
            );
        }

        try {
            long startTime = System.currentTimeMillis();
            com.shxc.fundagent.agent.capabilities.ToolResult result = tool.execute(parameters);
            long executionTime = System.currentTimeMillis() - startTime;

            // 如果有执行时间信息，更新它
            if (result.getExecutionTimeMs() == 0) {
                // 使用反射或创建新的ToolResult，这里简化处理
                logger.debug("Tool '{}' executed in {} ms", toolName, executionTime);
            }

            return result;
        } catch (Exception e) {
            logger.error("Error executing tool '{}'", toolName, e);
            return com.shxc.fundagent.agent.capabilities.ToolResult.error(
                    String.format("Error executing tool '%s': %s", toolName, e.getMessage()),
                    "TOOL_EXECUTION_ERROR"
            );
        }
    }

    /**
     * 检索相关记忆
     */
    protected List<com.shxc.fundagent.entity.AgentMemory> retrieveMemories(String query, MemoryManager.MemoryType type, int limit) {
        if (memoryManager == null || !memoryManager.isAvailable()) {
            return Collections.emptyList();
        }

        try {
            return memoryManager.retrieveMemories(getName(), query, type, limit);
        } catch (Exception e) {
            logger.error("Error retrieving memories for agent '{}'", getName(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 存储记忆
     */
    protected void storeMemory(String content, MemoryManager.MemoryType type, double importance, Map<String, Object> metadata) {
        if (memoryManager != null && memoryManager.isAvailable()) {
            try {
                memoryManager.storeMemory(getName(), content, type, importance, metadata);
                logger.debug("Agent '{}' stored memory of type {}", getName(), type);
            } catch (Exception e) {
                logger.error("Error storing memory for agent '{}'", getName(), e);
            }
        }
    }

    /**
     * 构建无效上下文结果
     */
    protected AgentResult buildInvalidContextResult(AgentContext context, ValidationResult validationResult) {
        return AgentResult.builder()
                .agentName(getName())
                .status(AgentResult.Status.INVALID_INPUT)
                .content("Invalid agent context")
                .errorMessage(validationResult.getErrorMessage())
                .errorCode(validationResult.getErrorCode())
                .extraData(Map.of(
                        "contextTask", context != null ? context.getTask() : "null",
                        "contextDataKeys", context != null ? context.getData().keySet() : "null"
                ))
                .build();
    }

    /**
     * 构建成功结果（v2扩展）
     */
    protected AgentResult buildSuccessResult(String content, double confidence, Map<String, Object> extraData, String schema) {
        AgentResult.Builder builder = AgentResult.builder()
                .agentName(getName())
                .status(AgentResult.Status.SUCCESS)
                .content(content)
                .confidence(confidence);

        if (extraData != null && !extraData.isEmpty()) {
            builder.extraData(extraData);
        }

        // 将schema作为reasoning或存储在extraData中
        if (schema != null) {
            builder.reasoning("Generated with schema: " + schema);
            // 同时存储在extraData中
            Map<String, Object> finalExtraData = extraData != null ? new HashMap<>(extraData) : new HashMap<>();
            finalExtraData.put("outputSchema", schema);
            builder.extraData(finalExtraData);
        }

        return builder.build();
    }

    /**
     * 获取异步执行器
     */
    protected ExecutorService getAsyncExecutor() {
        return asyncExecutor;
    }

    /**
     * 实现父类的抽象方法（v1兼容）
     * 将v1任务转换为v2上下文处理
     */
    @Override
    protected AgentResult doProcess(String task, String message) throws Exception {
        // 创建简化的AgentContext
        AgentContext context = new AgentContext();
        context.setTask(task);
        context.setMessage(message);

        // 对于v1调用，使用默认配置
        AgentContext.ToolConfig toolConfig = new AgentContext.ToolConfig();
        toolConfig.setEnabled(false);
        context.setToolConfig(toolConfig);

        AgentContext.MemoryConfig memoryConfig = new AgentContext.MemoryConfig();
        memoryConfig.setEnabled(false);
        context.setMemoryConfig(memoryConfig);

        // 调用v2处理逻辑
        return doProcessWithTools(task, context);
    }

    /**
     * 实际处理逻辑（由子类实现）
     */
    protected abstract AgentResult doProcessWithTools(String task, AgentContext context) throws Exception;

    /**
     * 默认Agent配置实现
     */
    protected static class DefaultAgentConfig implements AgentConfig {
        @Override
        public boolean isToolCallingEnabled() {
            return true;
        }

        @Override
        public boolean isMemoryManagementEnabled() {
            return true;
        }

        @Override
        public int getMaxToolCalls() {
            return 5;
        }

        @Override
        public long getDefaultToolTimeoutMs() {
            return 30000L; // 30秒
        }

        @Override
        public boolean isToolCallConfirmationRequired() {
            return false;
        }

        @Override
        public int getMemoryRetrievalLimit() {
            return 10;
        }

        @Override
        public String getDefaultOutputSchema() {
            return "structured";
        }

        @Override
        public boolean isOutputValidationEnabled() {
            return true;
        }
    }
}