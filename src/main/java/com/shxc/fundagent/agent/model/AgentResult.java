package com.shxc.fundagent.agent.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;

/**
 * Agent处理结果
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AgentResult {

    /**
     * 结果状态枚举
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
         * 需要人工干预
         */
        NEEDS_HUMAN_INTERVENTION("needs_human_intervention"),

        /**
         * 部分成功
         */
        PARTIAL_SUCCESS("partial_success"),

        /**
         * 无效输入
         */
        INVALID_INPUT("invalid_input"),

        /**
         * 超时
         */
        TIMEOUT("timeout");

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
     * 结果ID
     */
    private String resultId;

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * Agent名称
     */
    private String agentName;

    /**
     * 结果状态
     */
    private Status status;

    /**
     * 结果内容（可以是字符串、JSON对象等）
     */
    private Object content;

    /**
     * 置信度（0-1之间）
     */
    private Double confidence;

    /**
     * 推理过程
     */
    private String reasoning;

    /**
     * 建议操作
     */
    private String suggestedAction;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 错误代码
     */
    private String errorCode;

    /**
     * 处理时间（毫秒）
     */
    private Long processingTimeMs;

    /**
     * 扩展数据
     */
    private Map<String, Object> extraData;

    /**
     * 是否最终结果
     */
    private Boolean isFinal = true;

    public AgentResult() {
    }

    public AgentResult(Status status, Object content) {
        this.status = status;
        this.content = content;
    }

    // Builder模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String resultId;
        private String taskId;
        private String agentName;
        private Status status;
        private Object content;
        private Double confidence;
        private String reasoning;
        private String suggestedAction;
        private String errorMessage;
        private String errorCode;
        private Long processingTimeMs;
        private Map<String, Object> extraData;
        private Boolean isFinal = true;

        public Builder resultId(String resultId) {
            this.resultId = resultId;
            return this;
        }

        public Builder taskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public Builder agentName(String agentName) {
            this.agentName = agentName;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder content(Object content) {
            this.content = content;
            return this;
        }

        public Builder confidence(Double confidence) {
            this.confidence = confidence;
            return this;
        }

        public Builder reasoning(String reasoning) {
            this.reasoning = reasoning;
            return this;
        }

        public Builder suggestedAction(String suggestedAction) {
            this.suggestedAction = suggestedAction;
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

        public Builder processingTimeMs(Long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public Builder extraData(Map<String, Object> extraData) {
            this.extraData = extraData;
            return this;
        }

        public Builder isFinal(Boolean isFinal) {
            this.isFinal = isFinal;
            return this;
        }

        public AgentResult build() {
            AgentResult result = new AgentResult();
            result.setResultId(resultId);
            result.setTaskId(taskId);
            result.setAgentName(agentName);
            result.setStatus(status);
            result.setContent(content);
            result.setConfidence(confidence);
            result.setReasoning(reasoning);
            result.setSuggestedAction(suggestedAction);
            result.setErrorMessage(errorMessage);
            result.setErrorCode(errorCode);
            result.setProcessingTimeMs(processingTimeMs);
            result.setExtraData(extraData);
            result.setIsFinal(isFinal);
            return result;
        }
    }

    // 快捷方法
    public boolean isSuccess() {
        return status == Status.SUCCESS || status == Status.PARTIAL_SUCCESS;
    }

    public boolean hasError() {
        return status == Status.ERROR || status == Status.TIMEOUT || status == Status.INVALID_INPUT;
    }

    public boolean needsHumanIntervention() {
        return status == Status.NEEDS_HUMAN_INTERVENTION;
    }

    // Getters and Setters
    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Object getContent() {
        return content;
    }

    public void setContent(Object content) {
        this.content = content;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
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

    public Long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(Long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public Map<String, Object> getExtraData() {
        return extraData;
    }

    public void setExtraData(Map<String, Object> extraData) {
        this.extraData = extraData;
    }

    public Boolean getIsFinal() {
        return isFinal;
    }

    public void setIsFinal(Boolean isFinal) {
        this.isFinal = isFinal;
    }

    @Override
    public String toString() {
        return "AgentResult{" +
                "agentName='" + agentName + '\'' +
                ", status=" + status +
                ", content=" + (content != null ? content.getClass().getSimpleName() : "null") +
                ", confidence=" + confidence +
                '}';
    }
}