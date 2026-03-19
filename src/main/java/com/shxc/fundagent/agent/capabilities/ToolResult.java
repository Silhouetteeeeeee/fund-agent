package com.shxc.fundagent.agent.capabilities;

/**
 * 工具执行结果
 */
public class ToolResult {

    /**
     * 执行是否成功
     */
    private final boolean success;

    /**
     * 执行结果数据
     */
    private final Object data;

    /**
     * 错误信息（如果执行失败）
     */
    private final String errorMessage;

    /**
     * 错误代码（如果执行失败）
     */
    private final String errorCode;

    /**
     * 执行耗时（毫秒）
     */
    private final long executionTimeMs;

    private ToolResult(boolean success, Object data, String errorMessage, String errorCode, long executionTimeMs) {
        this.success = success;
        this.data = data;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.executionTimeMs = executionTimeMs;
    }

    /**
     * 创建成功结果
     */
    public static ToolResult success(Object data) {
        return success(data, 0);
    }

    /**
     * 创建成功结果（带执行时间）
     */
    public static ToolResult success(Object data, long executionTimeMs) {
        return new ToolResult(true, data, null, null, executionTimeMs);
    }

    /**
     * 创建失败结果
     */
    public static ToolResult error(String errorMessage) {
        return error(errorMessage, null);
    }

    /**
     * 创建失败结果（带错误代码）
     */
    public static ToolResult error(String errorMessage, String errorCode) {
        return error(errorMessage, errorCode, 0);
    }

    /**
     * 创建失败结果（带执行时间）
     */
    public static ToolResult error(String errorMessage, String errorCode, long executionTimeMs) {
        return new ToolResult(false, null, errorMessage, errorCode, executionTimeMs);
    }

    // Getters

    public boolean isSuccess() {
        return success;
    }

    public Object getData() {
        return data;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * 获取数据并转换为指定类型
     */
    @SuppressWarnings("unchecked")
    public <T> T getDataAs(Class<T> type) {
        if (data == null) {
            return null;
        }
        if (type.isInstance(data)) {
            return (T) data;
        }
        throw new ClassCastException("Cannot cast " + data.getClass() + " to " + type);
    }

    /**
     * 检查结果是否包含数据
     */
    public boolean hasData() {
        return data != null;
    }
}