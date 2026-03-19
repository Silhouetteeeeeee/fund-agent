package com.shxc.fundagent.agent.capabilities;

import java.util.Map;

/**
 * 工具接口
 * 定义Agent可以调用的工具函数
 */
public interface Tool {

    /**
     * 获取工具名称
     */
    String getName();

    /**
     * 获取工具描述（用于LLM理解工具功能）
     */
    String getDescription();

    /**
     * 获取参数模式（JSON Schema格式）
     * 描述工具接受的参数格式
     */
    String getParameterSchema();

    /**
     * 执行工具
     * @param parameters 工具参数
     * @return 工具执行结果
     */
    ToolResult execute(Map<String, Object> parameters);

    /**
     * 检查工具是否可用
     */
    boolean isAvailable();

    /**
     * 获取工具类别
     */
    default String getCategory() {
        return "general";
    }

    /**
     * 获取工具版本
     */
    default String getVersion() {
        return "1.0";
    }
}