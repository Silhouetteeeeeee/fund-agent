package com.shxc.fundagent.llm.impl;

import com.shxc.fundagent.llm.AbstractLlmProvider;
import com.shxc.fundagent.llm.ProviderType;
import com.shxc.fundagent.llm.model.LlmRequest;
import com.shxc.fundagent.llm.model.LlmResponse;
import com.shxc.fundagent.llm.model.Message;
import com.shxc.fundagent.llm.model.ToolCall;
import dev.langchain4j.data.message.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 基于LangChain4j的LLM提供商基类
 * 包装LangChain4j的ChatLanguageModel
 */
public abstract class LangChain4jProvider extends AbstractLlmProvider {

    private static final Logger logger = LoggerFactory.getLogger(LangChain4jProvider.class);

    /**
     * LangChain4j聊天模型
     */
    protected ChatLanguageModel chatModel;

    /**
     * 是否启用详细日志
     */
    protected boolean verboseLogging = false;

    protected LangChain4jProvider(String providerName, String modelName, ProviderType providerType) {
        super(providerName, modelName, providerType);
    }

    @Override
    protected LlmResponse doCall(LlmRequest request) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            // 转换为LangChain4j消息
            List<ChatMessage> chatMessages = convertToLangChain4jMessages(request);

            if (verboseLogging) {
                logger.debug("Sending request to {}: {} messages, model={}",
                        providerName, chatMessages.size(), modelName);
            }

            // 调用LangChain4j模型
            Response<AiMessage> response = chatModel.generate(chatMessages);

            // 转换为通用响应
            LlmResponse llmResponse = convertToLlmResponse(response, request, System.currentTimeMillis() - startTime);

            if (verboseLogging) {
                logger.debug("Received response from {}: {} tokens, finishReason={}",
                        providerName, llmResponse.getTotalTokens(), llmResponse.getFinishReason());
            }

            return llmResponse;

        } catch (Exception e) {
            logger.error("LangChain4j call failed for provider {}: {}", providerName, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 将通用请求转换为LangChain4j消息
     */
    protected List<ChatMessage> convertToLangChain4jMessages(LlmRequest request) {
        List<ChatMessage> chatMessages = new ArrayList<>();

        // 添加系统提示（如果有）
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().trim().isEmpty()) {
            chatMessages.add(SystemMessage.from(request.getSystemPrompt()));
        }

        // 添加消息列表
        if (request.getMessages() != null) {
            for (Message message : request.getMessages()) {
                chatMessages.add(convertMessage(message));
            }
        }

        return chatMessages;
    }

    /**
     * 转换单个消息
     */
    protected ChatMessage convertMessage(Message message) {
        switch (message.getRole()) {
            case SYSTEM:
                return SystemMessage.from(message.getContent());

            case USER:
                if (message.getName() != null) {
                    // 带名称的用户消息
                    return UserMessage.from(message.getName(), message.getContent());
                } else {
                    return UserMessage.from(message.getContent());
                }

            case ASSISTANT:
                AiMessage aiMessage = AiMessage.from(message.getContent());

                // 如果有工具调用，需要处理（简化实现）
                // 实际应用中需要更完整的工具调用处理
                if (message.getToolCallId() != null) {
                    // 这里可以添加工具调用处理
                    logger.debug("Assistant message with tool call ID: {}", message.getToolCallId());
                }

                return aiMessage;

            case TOOL:
                // 工具消息
                if (message.getToolCallId() != null) {
                    return ToolExecutionResultMessage.from(message.getToolCallId(), message.getContent());
                } else {
                    logger.warn("Tool message without tool call ID, content: {}", message.getContent());
                    return ToolExecutionResultMessage.from("unknown", message.getContent());
                }

            default:
                logger.warn("Unknown message role: {}, treating as user message", message.getRole());
                return UserMessage.from(message.getContent());
        }
    }

    /**
     * 将LangChain4j响应转换为通用响应
     */
    protected LlmResponse convertToLlmResponse(Response<AiMessage> response, LlmRequest request, long responseTimeMs) {
        AiMessage aiMessage = response.content();
        String content = aiMessage.text();

        // 提取工具调用信息
        List<ToolCall> toolCalls = null;
        if (aiMessage.hasToolExecutionRequests()) {
            toolCalls = aiMessage.toolExecutionRequests().stream()
                    .map(toolRequest -> ToolCall.builder()
                            .id(UUID.randomUUID().toString()) // LangChain4j没有提供ID，生成一个
                            .type("function")
                            .name(toolRequest.name())
                            .arguments(toolRequest.arguments())
                            .build())
                    .collect(Collectors.toList());
        }

        // 提取token使用信息
        Integer promptTokens = null;
        Integer completionTokens = null;
        Integer totalTokens = null;

        if (response.tokenUsage() != null) {
            promptTokens = response.tokenUsage().inputTokenCount();
            completionTokens = response.tokenUsage().outputTokenCount();
            totalTokens = response.tokenUsage().totalTokenCount();
        }

        // 提取完成原因
        String finishReason = null;
        if (response.finishReason() != null) {
            finishReason = response.finishReason().toString();
        }

        // 计算成本
        double cost = estimateCost(request);

        return LlmResponse.builder()
                .status(LlmResponse.Status.SUCCESS)
                .responseId(UUID.randomUUID().toString())
                .content(content)
                .model(modelName)
                .providerName(providerName)
                .finishReason(finishReason)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .cost(cost)
                .responseTimeMs(responseTimeMs)
                .toolCalls(toolCalls)
                .build();
    }

    @Override
    public int estimateTokens(LlmRequest request) {
        // 如果有LangChain4j的tokenizer，可以使用它
        // 这里先使用父类的简单估算
        return super.estimateTokens(request);
    }

    @Override
    public double estimateCost(LlmRequest request) {
        // 根据提供商类型和模型计算成本
        // 这里使用默认实现，实际应根据提供商定价调整
        return super.estimateCost(request);
    }

    /**
     * 设置LangChain4j聊天模型
     */
    public void setChatModel(ChatLanguageModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 获取LangChain4j聊天模型
     */
    public ChatLanguageModel getChatModel() {
        return chatModel;
    }

    /**
     * 设置是否启用详细日志
     */
    public void setVerboseLogging(boolean verboseLogging) {
        this.verboseLogging = verboseLogging;
    }

    /**
     * 检查模型是否可用
     */
    @Override
    public boolean isAvailable() {
        // 基础检查
        if (!super.isAvailable()) {
            return false;
        }

        // 检查chatModel是否初始化
        if (chatModel == null) {
            logger.warn("ChatLanguageModel not initialized for provider: {}", providerName);
            return false;
        }

        return true;
    }

    /**
     * 清理资源
     */
    @Override
    public void shutdown() {
        super.shutdown();
        // LangChain4j模型通常不需要显式关闭，但如果有资源可以在这里清理
        chatModel = null;
        logger.debug("LangChain4j provider {} shutdown", providerName);
    }
}