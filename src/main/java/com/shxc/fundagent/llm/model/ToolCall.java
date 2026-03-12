package com.shxc.fundagent.llm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * 工具调用模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ToolCall {

    /**
     * 工具调用ID
     */
    private String id;

    /**
     * 工具类型
     */
    private String type;

    /**
     * 工具名称
     */
    private String name;

    /**
     * 函数调用参数（JSON格式）
     */
    private Map<String, Object> arguments;

    /**
     * 工具调用输出（用于工具角色消息）
     */
    private String output;

    /**
     * 工具调用时间戳
     */
    private Long timestamp;

    public ToolCall() {
    }

    public ToolCall(String id, String type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }

    // Builder模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String type;
        private String name;
        private Map<String, Object> arguments;
        private String output;
        private Long timestamp;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder arguments(Map<String, Object> arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder output(String output) {
            this.output = output;
            return this;
        }

        public Builder timestamp(Long timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ToolCall build() {
            ToolCall toolCall = new ToolCall();
            toolCall.setId(id);
            toolCall.setType(type);
            toolCall.setName(name);
            toolCall.setArguments(arguments);
            toolCall.setOutput(output);
            toolCall.setTimestamp(timestamp);
            return toolCall;
        }
    }

    // 快捷创建方法
    public static ToolCall function(String id, String name, Map<String, Object> arguments) {
        ToolCall toolCall = new ToolCall(id, "function", name);
        toolCall.setArguments(arguments);
        return toolCall;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "ToolCall{" +
                "id='" + id + '\'' +
                ", type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", arguments=" + (arguments != null ? arguments.toString() : "null") +
                '}';
    }
}