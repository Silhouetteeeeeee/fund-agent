package com.shxc.fundagent.agent.model.v2;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent上下文对象
 * v2版本支持更丰富的上下文信息
 */
public class AgentContext {

    /**
     * 任务描述
     */
    private String task;

    /**
     * 原始消息（兼容v1）
     */
    private String message;

    /**
     * 扩展上下文数据
     */
    private Map<String, Object> data;

    /**
     * 工具调用配置
     */
    private ToolConfig toolConfig;

    /**
     * 记忆检索配置
     */
    private MemoryConfig memoryConfig;

    /**
     * 输出格式配置
     */
    private OutputConfig outputConfig;

    public AgentContext() {
        this.data = new HashMap<>();
    }

    public AgentContext(String task, String message) {
        this();
        this.task = task;
        this.message = message;
    }

    public static AgentContextBuilder builder() {
        return new AgentContextBuilder();
    }

    // Getters and setters
    public String getTask() {
        return task;
    }

    public void setTask(String task) {
        this.task = task;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public ToolConfig getToolConfig() {
        return toolConfig;
    }

    public void setToolConfig(ToolConfig toolConfig) {
        this.toolConfig = toolConfig;
    }

    public MemoryConfig getMemoryConfig() {
        return memoryConfig;
    }

    public void setMemoryConfig(MemoryConfig memoryConfig) {
        this.memoryConfig = memoryConfig;
    }

    public OutputConfig getOutputConfig() {
        return outputConfig;
    }

    public void setOutputConfig(OutputConfig outputConfig) {
        this.outputConfig = outputConfig;
    }

    /**
     * 添加数据项
     */
    public AgentContext addData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

    /**
     * 获取数据项
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key) {
        return (T) this.data.get(key);
    }

    /**
     * 获取数据项，如果不存在则返回默认值
     */
    @SuppressWarnings("unchecked")
    public <T> T getData(String key, T defaultValue) {
        return this.data.containsKey(key) ? (T) this.data.get(key) : defaultValue;
    }

    /**
     * 检查是否包含数据项
     */
    public boolean containsData(String key) {
        return this.data.containsKey(key);
    }

    /**
     * 工具配置
     */
    public static class ToolConfig {
        private boolean enabled = true;
        private int maxToolCalls = 5;
        private boolean requireConfirmation = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMaxToolCalls() {
            return maxToolCalls;
        }

        public void setMaxToolCalls(int maxToolCalls) {
            this.maxToolCalls = maxToolCalls;
        }

        public boolean isRequireConfirmation() {
            return requireConfirmation;
        }

        public void setRequireConfirmation(boolean requireConfirmation) {
            this.requireConfirmation = requireConfirmation;
        }
    }

    /**
     * 记忆配置
     */
    public static class MemoryConfig {
        private boolean enabled = true;
        private int memoryLimit = 10; // 检索的记忆数量限制
        private double relevanceThreshold = 0.5; // 相关性阈值

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getMemoryLimit() {
            return memoryLimit;
        }

        public void setMemoryLimit(int memoryLimit) {
            this.memoryLimit = memoryLimit;
        }

        public double getRelevanceThreshold() {
            return relevanceThreshold;
        }

        public void setRelevanceThreshold(double relevanceThreshold) {
            this.relevanceThreshold = relevanceThreshold;
        }
    }

    /**
     * 输出配置
     */
    public static class OutputConfig {
        private boolean structured = true;
        private String schemaName = "default";
        private boolean validate = true;

        public boolean isStructured() {
            return structured;
        }

        public void setStructured(boolean structured) {
            this.structured = structured;
        }

        public String getSchemaName() {
            return schemaName;
        }

        public void setSchemaName(String schemaName) {
            this.schemaName = schemaName;
        }

        public boolean isValidate() {
            return validate;
        }

        public void setValidate(boolean validate) {
            this.validate = validate;
        }
    }

    /**
     * Builder模式支持
     */
    public static class AgentContextBuilder {
        private final AgentContext context;

        public AgentContextBuilder() {
            this.context = new AgentContext();
        }

        public AgentContextBuilder task(String task) {
            this.context.task = task;
            return this;
        }

        public AgentContextBuilder message(String message) {
            this.context.message = message;
            return this;
        }

        public AgentContextBuilder addData(String key, Object value) {
            this.context.addData(key, value);
            return this;
        }

        public AgentContextBuilder data(Map<String, Object> data) {
            this.context.data = data;
            return this;
        }

        public AgentContextBuilder toolConfig(ToolConfig toolConfig) {
            this.context.toolConfig = toolConfig;
            return this;
        }

        public AgentContextBuilder memoryConfig(MemoryConfig memoryConfig) {
            this.context.memoryConfig = memoryConfig;
            return this;
        }

        public AgentContextBuilder outputConfig(OutputConfig outputConfig) {
            this.context.outputConfig = outputConfig;
            return this;
        }

        public AgentContext build() {
            return this.context;
        }
    }
}