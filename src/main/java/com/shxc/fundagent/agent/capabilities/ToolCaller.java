package com.shxc.fundagent.agent.capabilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 工具调用器
 * 负责管理工具的注册、参数验证和执行
 */
@Component
public class ToolCaller {

    private static final Logger logger = LoggerFactory.getLogger(ToolCaller.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 注册的工具（按名称索引）
     */
    private final Map<String, Tool> tools = new ConcurrentHashMap<>();

    /**
     * 工具类别索引
     */
    private final Map<String, List<Tool>> toolsByCategory = new ConcurrentHashMap<>();

    /**
     * 工具调用统计
     */
    private final Map<String, ToolStats> toolStats = new ConcurrentHashMap<>();

    /**
     * 默认工具超时时间（毫秒）
     */
    private long defaultTimeoutMs = 30000L;

    /**
     * 是否启用参数验证
     */
    private boolean parameterValidationEnabled = true;

    /**
     * 注册工具
     */
    public void registerTool(Tool tool) {
        if (tool == null) {
            throw new IllegalArgumentException("Tool cannot be null");
        }

        String toolName = tool.getName();
        if (tools.containsKey(toolName)) {
            logger.warn("Tool with name '{}' already registered, replacing", toolName);
        }

        tools.put(toolName, tool);

        // 更新类别索引
        String category = tool.getCategory();
        toolsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(tool);

        // 初始化统计信息
        toolStats.put(toolName, new ToolStats(toolName));

        logger.info("Tool registered: {} - {}", toolName, tool.getDescription());
    }

    /**
     * 注册多个工具
     */
    public void registerTools(Collection<Tool> toolCollection) {
        if (toolCollection == null) {
            return;
        }

        for (Tool tool : toolCollection) {
            registerTool(tool);
        }
    }

    /**
     * 注销工具
     */
    public void unregisterTool(String toolName) {
        Tool tool = tools.remove(toolName);
        if (tool != null) {
            // 从类别索引中移除
            String category = tool.getCategory();
            List<Tool> categoryTools = toolsByCategory.get(category);
            if (categoryTools != null) {
                categoryTools.remove(tool);
                if (categoryTools.isEmpty()) {
                    toolsByCategory.remove(category);
                }
            }

            // 移除统计信息
            toolStats.remove(toolName);

            logger.info("Tool unregistered: {}", toolName);
        }
    }

    /**
     * 获取工具
     */
    public Tool getTool(String toolName) {
        return tools.get(toolName);
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String toolName) {
        return tools.containsKey(toolName);
    }

    /**
     * 获取所有工具
     */
    public List<Tool> getAllTools() {
        return new ArrayList<>(tools.values());
    }

    /**
     * 根据类别获取工具
     */
    public List<Tool> getToolsByCategory(String category) {
        List<Tool> categoryTools = toolsByCategory.get(category);
        return categoryTools != null ? new ArrayList<>(categoryTools) : Collections.emptyList();
    }

    /**
     * 获取所有类别
     */
    public Set<String> getAllCategories() {
        return new HashSet<>(toolsByCategory.keySet());
    }

    /**
     * 调用工具
     */
    public ToolResult callTool(String toolName, Map<String, Object> parameters) {
        long startTime = System.currentTimeMillis();
        ToolStats stats = toolStats.get(toolName);

        if (stats == null) {
            return ToolResult.error(
                    String.format("Tool '%s' not found", toolName),
                    "TOOL_NOT_FOUND",
                    System.currentTimeMillis() - startTime
            );
        }

        Tool tool = tools.get(toolName);
        if (tool == null) {
            stats.recordFailure();
            return ToolResult.error(
                    String.format("Tool '%s' not available", toolName),
                    "TOOL_UNAVAILABLE",
                    System.currentTimeMillis() - startTime
            );
        }

        if (!tool.isAvailable()) {
            stats.recordFailure();
            return ToolResult.error(
                    String.format("Tool '%s' is not available", toolName),
                    "TOOL_DISABLED",
                    System.currentTimeMillis() - startTime
            );
        }

        try {
            // 参数验证
            if (parameterValidationEnabled) {
                ValidationResult validationResult = validateParameters(tool, parameters);
                if (!validationResult.isValid()) {
                    stats.recordFailure();
                    return ToolResult.error(
                            String.format("Invalid parameters for tool '%s': %s", toolName, validationResult.getErrorMessage()),
                            "INVALID_PARAMETERS",
                            System.currentTimeMillis() - startTime
                    );
                }
            }

            // 执行工具
            ToolResult result = tool.execute(parameters);
            long executionTime = System.currentTimeMillis() - startTime;

            // 记录统计信息
            if (result.isSuccess()) {
                stats.recordSuccess(executionTime);
                logger.debug("Tool '{}' executed successfully in {} ms", toolName, executionTime);
            } else {
                stats.recordFailure();
                logger.warn("Tool '{}' execution failed: {}", toolName, result.getErrorMessage());
            }

            return result;

        } catch (Exception e) {
            stats.recordFailure();
            logger.error("Error executing tool '{}'", toolName, e);

            return ToolResult.error(
                    String.format("Error executing tool '%s': %s", toolName, e.getMessage()),
                    "EXECUTION_ERROR",
                    System.currentTimeMillis() - startTime
            );
        }
    }

    /**
     * 批量调用工具
     */
    public Map<String, ToolResult> callTools(Map<String, Map<String, Object>> toolCalls) {
        Map<String, ToolResult> results = new HashMap<>();

        for (Map.Entry<String, Map<String, Object>> entry : toolCalls.entrySet()) {
            String toolName = entry.getKey();
            Map<String, Object> parameters = entry.getValue();

            ToolResult result = callTool(toolName, parameters);
            results.put(toolName, result);
        }

        return results;
    }

    /**
     * 验证工具参数
     */
    private ValidationResult validateParameters(Tool tool, Map<String, Object> parameters) {
        // TODO: 实现基于JSON Schema的完整参数验证
        // 当前简化实现：检查必需参数
        String schema = tool.getParameterSchema();
        if (schema == null || schema.trim().isEmpty()) {
            return ValidationResult.valid(); // 无模式定义，跳过验证
        }

        try {
            // 这里可以集成JSON Schema验证库
            // 暂时返回验证通过
            return ValidationResult.valid();
        } catch (Exception e) {
            return ValidationResult.invalid(
                    String.format("Parameter validation error: %s", e.getMessage()),
                    "VALIDATION_ERROR"
            );
        }
    }

    /**
     * 获取工具描述列表（用于LLM）
     */
    public List<ToolDescription> getToolDescriptions() {
        return tools.values().stream()
                .map(tool -> new ToolDescription(
                        tool.getName(),
                        tool.getDescription(),
                        tool.getParameterSchema(),
                        tool.getCategory(),
                        tool.getVersion()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取可用工具描述列表
     */
    public List<ToolDescription> getAvailableToolDescriptions() {
        return tools.values().stream()
                .filter(Tool::isAvailable)
                .map(tool -> new ToolDescription(
                        tool.getName(),
                        tool.getDescription(),
                        tool.getParameterSchema(),
                        tool.getCategory(),
                        tool.getVersion()
                ))
                .collect(Collectors.toList());
    }

    /**
     * 获取工具统计信息
     */
    public ToolStats getToolStats(String toolName) {
        return toolStats.get(toolName);
    }

    /**
     * 获取所有工具统计信息
     */
    public Map<String, ToolStats> getAllToolStats() {
        return new HashMap<>(toolStats);
    }

    /**
     * 设置默认超时时间
     */
    public void setDefaultTimeoutMs(long defaultTimeoutMs) {
        this.defaultTimeoutMs = defaultTimeoutMs;
    }

    /**
     * 设置是否启用参数验证
     */
    public void setParameterValidationEnabled(boolean parameterValidationEnabled) {
        this.parameterValidationEnabled = parameterValidationEnabled;
    }

    /**
     * 清理统计信息
     */
    public void clearStats() {
        for (ToolStats stats : toolStats.values()) {
            stats.clear();
        }
        logger.info("Tool statistics cleared");
    }

    /**
     * 工具描述类
     */
    public static class ToolDescription {
        private final String name;
        private final String description;
        private final String parameterSchema;
        private final String category;
        private final String version;

        public ToolDescription(String name, String description, String parameterSchema, String category, String version) {
            this.name = name;
            this.description = description;
            this.parameterSchema = parameterSchema;
            this.category = category;
            this.version = version;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getParameterSchema() {
            return parameterSchema;
        }

        public String getCategory() {
            return category;
        }

        public String getVersion() {
            return version;
        }
    }

    /**
     * 工具统计信息
     */
    public static class ToolStats {
        private final String toolName;
        private final AtomicInteger successCount = new AtomicInteger(0);
        private final AtomicInteger failureCount = new AtomicInteger(0);
        private final AtomicInteger totalCalls = new AtomicInteger(0);
        private volatile long totalExecutionTimeMs = 0L;
        private volatile long lastCallTime = 0L;
        private volatile long lastSuccessTime = 0L;
        private volatile long lastFailureTime = 0L;

        public ToolStats(String toolName) {
            this.toolName = toolName;
        }

        public void recordSuccess(long executionTimeMs) {
            successCount.incrementAndGet();
            totalCalls.incrementAndGet();
            totalExecutionTimeMs += executionTimeMs;
            lastCallTime = System.currentTimeMillis();
            lastSuccessTime = lastCallTime;
        }

        public void recordFailure() {
            failureCount.incrementAndGet();
            totalCalls.incrementAndGet();
            lastCallTime = System.currentTimeMillis();
            lastFailureTime = lastCallTime;
        }

        public void clear() {
            successCount.set(0);
            failureCount.set(0);
            totalCalls.set(0);
            totalExecutionTimeMs = 0L;
            lastCallTime = 0L;
            lastSuccessTime = 0L;
            lastFailureTime = 0L;
        }

        public String getToolName() {
            return toolName;
        }

        public int getSuccessCount() {
            return successCount.get();
        }

        public int getFailureCount() {
            return failureCount.get();
        }

        public int getTotalCalls() {
            return totalCalls.get();
        }

        public double getSuccessRate() {
            int total = totalCalls.get();
            return total > 0 ? (double) successCount.get() / total : 0.0;
        }

        public long getTotalExecutionTimeMs() {
            return totalExecutionTimeMs;
        }

        public double getAverageExecutionTimeMs() {
            int successes = successCount.get();
            return successes > 0 ? (double) totalExecutionTimeMs / successes : 0.0;
        }

        public long getLastCallTime() {
            return lastCallTime;
        }

        public long getLastSuccessTime() {
            return lastSuccessTime;
        }

        public long getLastFailureTime() {
            return lastFailureTime;
        }

        public boolean isAvailable() {
            return true; // 假设工具总是可用，实际应该检查工具状态
        }
    }

    /**
     * 验证结果
     */
    private static class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String errorCode;

        private ValidationResult(boolean valid, String errorMessage, String errorCode) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true, null, null);
        }

        public static ValidationResult invalid(String errorMessage, String errorCode) {
            return new ValidationResult(false, errorMessage, errorCode);
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }
}