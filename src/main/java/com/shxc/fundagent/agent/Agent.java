package com.shxc.fundagent.agent;

import com.shxc.fundagent.agent.model.AgentResult;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Agent接口
 * 定义智能代理的核心行为
 */
public interface Agent {

    /**
     * 处理任务
     * @param task 任务描述
     * @param context 上下文信息
     * @return 处理结果
     */
    AgentResult process(String task, Map<String, Object> context);

    /**
     * 异步处理任务
     * @param task 任务描述
     * @param context 上下文信息
     * @return 异步处理结果
     */
    CompletableFuture<AgentResult> processAsync(String task, Map<String, Object> context);

    /**
     * 获取Agent名称
     * @return Agent名称
     */
    String getName();

    /**
     * 获取Agent描述
     * @return Agent描述
     */
    String getDescription();

    /**
     * 获取Agent能力描述
     * @return 能力描述
     */
    String[] getCapabilities();

    /**
     * 检查Agent是否可用
     * @return 是否可用
     */
    boolean isAvailable();

    /**
     * 获取Agent状态
     * @return Agent状态
     */
    AgentStatus getStatus();

    /**
     * 重置Agent状态
     */
    void reset();

    /**
     * 设置LLM提供商（如果Agent需要调用LLM）
     * @param llmProvider LLM提供商名称
     */
    void setLlmProvider(String llmProvider);

    /**
     * 获取支持的上下文类型
     * @return 上下文类型列表
     */
    String[] getSupportedContextTypes();
}