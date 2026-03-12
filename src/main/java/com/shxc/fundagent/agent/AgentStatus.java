package com.shxc.fundagent.agent;

/**
 * Agent状态枚举
 */
public enum AgentStatus {
    /**
     * 就绪状态
     */
    READY("ready"),

    /**
     * 忙碌状态（正在处理任务）
     */
    BUSY("busy"),

    /**
     * 错误状态
     */
    ERROR("error"),

    /**
     * 离线状态
     */
    OFFLINE("offline"),

    /**
     * 初始化中
     */
    INITIALIZING("initializing"),

    /**
     * 训练中（如果需要）
     */
    TRAINING("training");

    private final String code;

    AgentStatus(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AgentStatus fromCode(String code) {
        for (AgentStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return OFFLINE;
    }
}