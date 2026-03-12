package com.shxc.fundagent.llm.model;

import java.util.List;
import java.util.Map;

/**
 * LLM请求参数
 */
public class LlmRequest {

    /**
     * 消息列表
     */
    private List<Message> messages;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 模型名称
     */
    private String model;

    /**
     * 温度参数（控制随机性，0-2之间）
     */
    private Double temperature;

    /**
     * 最大生成token数
     */
    private Integer maxTokens;

    /**
     * top_p参数（0-1之间）
     */
    private Double topP;

    /**
     * 频率惩罚（-2到2之间）
     */
    private Double frequencyPenalty;

    /**
     * 存在惩罚（-2到2之间）
     */
    private Double presencePenalty;

    /**
     * 是否流式输出
     */
    private Boolean stream;

    /**
     * 停止词列表
     */
    private List<String> stopSequences;

    /**
     * 扩展参数（不同提供商可能有特殊参数）
     */
    private Map<String, Object> extraParams;

    /**
     * 请求超时时间（毫秒）
     */
    private Long timeoutMs;

    public LlmRequest() {
    }

    public LlmRequest(List<Message> messages) {
        this.messages = messages;
    }

    // Builder模式
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<Message> messages;
        private String systemPrompt;
        private String model;
        private Double temperature = 0.7;
        private Integer maxTokens = 1000;
        private Double topP = 1.0;
        private Double frequencyPenalty = 0.0;
        private Double presencePenalty = 0.0;
        private Boolean stream = false;
        private List<String> stopSequences;
        private Map<String, Object> extraParams;
        private Long timeoutMs = 30000L;

        public Builder messages(List<Message> messages) {
            this.messages = messages;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder model(String model) {
            this.model = model;
            return this;
        }

        public Builder temperature(Double temperature) {
            this.temperature = temperature;
            return this;
        }

        public Builder maxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
            return this;
        }

        public Builder topP(Double topP) {
            this.topP = topP;
            return this;
        }

        public Builder frequencyPenalty(Double frequencyPenalty) {
            this.frequencyPenalty = frequencyPenalty;
            return this;
        }

        public Builder presencePenalty(Double presencePenalty) {
            this.presencePenalty = presencePenalty;
            return this;
        }

        public Builder stream(Boolean stream) {
            this.stream = stream;
            return this;
        }

        public Builder stopSequences(List<String> stopSequences) {
            this.stopSequences = stopSequences;
            return this;
        }

        public Builder extraParams(Map<String, Object> extraParams) {
            this.extraParams = extraParams;
            return this;
        }

        public Builder timeoutMs(Long timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public LlmRequest build() {
            LlmRequest request = new LlmRequest();
            request.setMessages(messages);
            request.setSystemPrompt(systemPrompt);
            request.setModel(model);
            request.setTemperature(temperature);
            request.setMaxTokens(maxTokens);
            request.setTopP(topP);
            request.setFrequencyPenalty(frequencyPenalty);
            request.setPresencePenalty(presencePenalty);
            request.setStream(stream);
            request.setStopSequences(stopSequences);
            request.setExtraParams(extraParams);
            request.setTimeoutMs(timeoutMs);
            return request;
        }
    }

    // Getters and Setters
    public List<Message> getMessages() {
        return messages;
    }

    public void setMessages(List<Message> messages) {
        this.messages = messages;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
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

    public Double getTopP() {
        return topP;
    }

    public void setTopP(Double topP) {
        this.topP = topP;
    }

    public Double getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public void setFrequencyPenalty(Double frequencyPenalty) {
        this.frequencyPenalty = frequencyPenalty;
    }

    public Double getPresencePenalty() {
        return presencePenalty;
    }

    public void setPresencePenalty(Double presencePenalty) {
        this.presencePenalty = presencePenalty;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public List<String> getStopSequences() {
        return stopSequences;
    }

    public void setStopSequences(List<String> stopSequences) {
        this.stopSequences = stopSequences;
    }

    public Map<String, Object> getExtraParams() {
        return extraParams;
    }

    public void setExtraParams(Map<String, Object> extraParams) {
        this.extraParams = extraParams;
    }

    public Long getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(Long timeoutMs) {
        this.timeoutMs = timeoutMs;
    }
}