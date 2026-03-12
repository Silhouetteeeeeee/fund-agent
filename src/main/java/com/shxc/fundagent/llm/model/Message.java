package com.shxc.fundagent.llm.model;

/**
 * LLM消息模型
 */
public class Message {

    /**
     * 消息角色类型
     */
    public enum Role {
        /**
         * 系统角色，用于设置系统提示
         */
        SYSTEM("system"),

        /**
         * 用户角色，用户输入
         */
        USER("user"),

        /**
         * 助手角色，AI回复
         */
        ASSISTANT("assistant"),

        /**
         * 工具角色，用于工具调用
         */
        TOOL("tool");

        private final String code;

        Role(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static Role fromCode(String code) {
            for (Role role : values()) {
                if (role.getCode().equals(code)) {
                    return role;
                }
            }
            return USER;
        }
    }

    /**
     * 消息角色
     */
    private Role role;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 消息名称（可选）
     */
    private String name;

    /**
     * 工具调用ID（用于工具角色）
     */
    private String toolCallId;

    public Message() {
    }

    public Message(Role role, String content) {
        this.role = role;
        this.content = content;
    }

    public Message(Role role, String content, String name) {
        this.role = role;
        this.content = content;
        this.name = name;
    }

    // Builder模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Role role;
        private String content;
        private String name;
        private String toolCallId;

        public Builder role(Role role) {
            this.role = role;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder toolCallId(String toolCallId) {
            this.toolCallId = toolCallId;
            return this;
        }

        public Message build() {
            Message message = new Message();
            message.setRole(role);
            message.setContent(content);
            message.setName(name);
            message.setToolCallId(toolCallId);
            return message;
        }
    }

    // 快捷创建方法
    public static Message system(String content) {
        return new Message(Role.SYSTEM, content);
    }

    public static Message user(String content) {
        return new Message(Role.USER, content);
    }

    public static Message user(String content, String name) {
        return new Message(Role.USER, content, name);
    }

    public static Message assistant(String content) {
        return new Message(Role.ASSISTANT, content);
    }

    public static Message tool(String content, String toolCallId) {
        Message message = new Message(Role.TOOL, content);
        message.setToolCallId(toolCallId);
        return message;
    }

    // Getters and Setters
    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToolCallId() {
        return toolCallId;
    }

    public void setToolCallId(String toolCallId) {
        this.toolCallId = toolCallId;
    }

    @Override
    public String toString() {
        return "Message{" +
                "role=" + role +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 100)) + "..." : "null") + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}