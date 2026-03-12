package com.shxc.fundagent.llm;

import com.shxc.fundagent.llm.model.LlmRequest;
import com.shxc.fundagent.llm.model.LlmResponse;
import java.util.concurrent.CompletableFuture;

/**
 * 大语言模型提供商接口
 * 定义调用LLM的核心操作，支持同步和异步调用
 */
public interface LlmProvider {

    /**
     * 同步调用LLM
     * @param request LLM请求参数
     * @return LLM响应
     */
    LlmResponse call(LlmRequest request);

    /**
     * 异步调用LLM
     * @param request LLM请求参数
     * @return 异步响应的CompletableFuture
     */
    CompletableFuture<LlmResponse> callAsync(LlmRequest request);

    /**
     * 获取提供商名称
     * @return 提供商名称（如：openai、anthropic等）
     */
    String getProviderName();

    /**
     * 检查服务是否可用
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 获取模型名称
     * @return 模型名称
     */
    String getModelName();

    /**
     * 获取提供商类型
     * @return 提供商类型枚举
     */
    ProviderType getProviderType();

    /**
     * 估算请求的token数量
     * @param request LLM请求
     * @return token数量估算
     */
    int estimateTokens(LlmRequest request);

    /**
     * 估算请求成本（单位：美元）
     * @param request LLM请求
     * @return 成本估算
     */
    double estimateCost(LlmRequest request);
}