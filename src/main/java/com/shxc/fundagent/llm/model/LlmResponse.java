package com.shxc.fundagent.llm.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

/**
 * LLM响应模型
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LlmResponse {

    /**
     * 响应状态枚举
     */
    public enum Status {
        /**
         * 成功
         */
        SUCCESS("success"),

        /**
         * 失败
         */
        ERROR("error"),

        /**
         * 超时
         */
        TIMEOUT("timeout"),

        /**
         * 限流
         */
        RATE_LIMITED("rate_limited"),

        /**
         * 提供商不可用
         */
        PROVIDER_UNAVAILABLE("provider_unavailable");

        private final String code;

        Status(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public static Status fromCode(String code) {
            for (Status status : values()) {
                if (status.getCode().equals(code)) {
                    return status;
                }
            }
            return ERROR;
        }
    }

    /**
     * 响应ID
     */
    private String responseId;

    /**
     * 响应状态
     */
    private Status status;

    /**
     * 响应内容
     */
    private String content;

    /**
     * 角色（通常为assistant）
     */
    private String role;

    /**
     * 使用的模型名称
     */
    private String model;

    /**
     * 提供商名称
     */
    private String providerName;

    /**
     * 完成原因
     */
    private String finishReason;

    /**
     * 提示token数
     */
    private Integer promptTokens;

    /**
     * 完成token数
     */
    private Integer completionTokens;

    /**
     * 总token数
     */
    private Integer totalTokens;

    /**
     * 请求成本
     */
    private Double cost;

    /**
     * 响应时间（毫秒）
     */
    private Long responseTimeMs;

    /**
     * 工具调用列表
     */
    private List<ToolCall> toolCalls;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 扩展数据
     */
    private Map<String, Object> extraData;

    /**
     * 请求ID（用于跟踪）
     */
    private String requestId;

    public LlmResponse() {
    }

    public LlmResponse(Status status, String content) {
        this.status = status;
        this.content = content;
        this.role = Message.Role.ASSISTANT.getCode();
    }

    // Builder模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String responseId;
        private Status status;
        private String content;
        private String role = Message.Role.ASSISTANT.getCode();
        private String model;
        private String providerName;
        private String finishReason;
        private Integer promptTokens;
        private Integer completionTokens;
        private Integer totalTokens;
        private Double cost;
        private Long responseTimeMs;
        private List<ToolCall> toolCalls;
        private String errorMessage;
        private String errorCode;
        private Map<String, Object> extraData;
        private String requestId;

        public Builder responseId(String responseId) {
            this.responseId = responseId;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder providerName(String providerName) {
            this.providerName = providerName;
            return this;
        }

        public Builder finishReason(String finishReason) {
            this.finishReason = finishReason;
            return this;
        }

        public Builder promptTokens(Integer promptTokens) {
            this.promptTokens = promptTokens;
            return this;
        }

        public Builder completionTokens(Integer completionTokens) {
            this.completionTokens = completionTokens;
            return this;
        }

        public Builder totalTokens(Integer totalTokens) {
            this.totalTokens = totalTokens;
            return this;
        }

        public Builder cost(Double cost) {
            this.cost = cost;
            return this;
        }

        public Builder responseTimeMs(Long responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
            return this;
        }

        public Builder toolCalls(List<ToolCall> toolCalls) {
            this.toolCalls = toolCalls;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder errorCode(String errorCode) {
            this.errorCode = errorCode;
            return this;
        }

        public Builder extraData(Map<String, Object> extraData) {
            this.extraData = extraData;
            return this;
        }

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public LlmResponse build() {
            LlmResponse response = new LlmResponse();
            response.setResponseId(responseId);
            response.setStatus(status);
            response.setContent(content);
            response.setRole(role);
            response.setModel(model);
            response.setProviderName(providerName);
            response.setFinishReason(finishReason);
            response.setPromptTokens(promptTokens);
            response.setCompletionTokens(completionTokens);
            response.setTotalTokens(totalTokens);
            response.setCost(cost);
            response.setResponseTimeMs(responseTimeMs);
            response.setToolCalls(toolCalls);
            response.setErrorMessage(errorMessage);
            response.setErrorCode(errorCode);
            response.setExtraData(extraData);
            response.setRequestId(requestId);
            return response;
        }
    }

    // 快捷方法
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean hasError() {
        return status != Status.SUCCESS;
    }

    public boolean hasToolCalls() {
        return toolCalls != null && !toolCalls.isEmpty();
    }

    // Getters and Setters
    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getFinishReason() {
        return finishReason;
    }

    public void setFinishReason(String finishReason) {
        this.finishReason = finishReason;
    }

    public Integer getPromptTokens() {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens) {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens() {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens) {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Double getCost() {
        return cost;
    }

    public void setCost(Double cost) {
        this.cost = cost;
    }

    public Long getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Long responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public List<ToolCall> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(List<ToolCall> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    @Override
    public String toString() {
        return "LlmResponse{" +
                "status=" + status +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 100)) + "..." : "null") + '\'' +
                ", model='" + model + '\'' +
                ", providerName='" + providerName + '\'' +
                '}';
    }
}