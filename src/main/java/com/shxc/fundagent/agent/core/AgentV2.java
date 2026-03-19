package com.shxc.fundagent.agent.core;

import com.shxc.fundagent.agent.Agent;
import com.shxc.fundagent.agent.capabilities.Tool;
import com.shxc.fundagent.agent.capabilities.MemoryManager;
import com.shxc.fundagent.agent.model.v2.AgentContext;
import com.shxc.fundagent.agent.model.AgentResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Agent v2接口
 * 扩展Agent接口，支持工具调用、记忆管理、结构化输出等高级功能
 */
public interface AgentV2 extends Agent {

    /**
     * 使用工具处理任务（v2新增）
     * @param task 任务描述
     * @param context Agent上下文
     * @return 处理结果
     */
    AgentResult processWithTools(String task, AgentContext context);

    /**
     * 异步使用工具处理任务（v2新增）
     * @param task 任务描述
     * @param context Agent上下文
     * @return 异步处理结果
     */
    CompletableFuture<AgentResult> processWithToolsAsync(String task, AgentContext context);

    /**
     * 获取Agent可用的工具列表
     * @return 工具列表
     */
    List<Tool> getAvailableTools();

    /**
     * 检查是否支持工具调用
     * @return 是否支持工具调用
     */
    boolean supportsToolCalling();

    /**
     * 获取Agent的记忆管理器
     * @return 记忆管理器
     */
    MemoryManager getMemoryManager();

    /**
     * 检查是否支持记忆管理
     * @return 是否支持记忆管理
     */
    boolean supportsMemoryManagement();

    /**
     * 获取Agent支持的输出模式
     * @return 输出模式列表
     */
    List<String> getSupportedOutputSchemas();

    /**
     * 获取默认的输出模式名称
     * @return 默认输出模式名称
     */
    String getDefaultOutputSchema();

    /**
     * 验证上下文是否有效（v2增强版本）
     * @param context Agent上下文
     * @return 验证结果和错误信息（如果无效）
     */
    ValidationResult validateContext(AgentContext context);

    /**
     * 获取Agent版本
     * @return Agent版本
     */
    default String getVersion() {
        return "2.0";
    }

    /**
     * 获取Agent配置
     * @return Agent配置信息
     */
    AgentConfig getConfig();

    /**
     * 上下文验证结果
     */
    class ValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String errorCode;

        public ValidationResult(boolean valid) {
            this(valid, null, null);
        }

        public ValidationResult(boolean valid, String errorMessage, String errorCode) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }

        public boolean isValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public String getErrorCode() {
            return errorCode;
        }

        public static ValidationResult valid() {
            return new ValidationResult(true);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage, null);
        }

        public static ValidationResult invalid(String errorMessage, String errorCode) {
            return new ValidationResult(false, errorMessage, errorCode);
        }
    }

    /**
     * Agent配置
     */
    interface AgentConfig {
        /**
         * 是否启用工具调用
         */
        boolean isToolCallingEnabled();

        /**
         * 是否启用记忆管理
         */
        boolean isMemoryManagementEnabled();

        /**
         * 最大工具调用次数
         */
        int getMaxToolCalls();

        /**
         * 默认工具调用超时时间（毫秒）
         */
        long getDefaultToolTimeoutMs();

        /**
         * 是否要求工具调用确认
         */
        boolean isToolCallConfirmationRequired();

        /**
         * 记忆检索限制
         */
        int getMemoryRetrievalLimit();

        /**
         * 默认输出模式
         */
        String getDefaultOutputSchema();

        /**
         * 是否启用输出验证
         */
        boolean isOutputValidationEnabled();
    }
}