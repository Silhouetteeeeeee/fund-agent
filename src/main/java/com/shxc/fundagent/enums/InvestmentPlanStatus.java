package com.shxc.fundagent.enums;

/**
 * 定投计划状态枚举
 */
public enum InvestmentPlanStatus {
    /** 活跃 - 计划正在执行中 */
    ACTIVE,
    /** 暂停 - 计划已暂停，暂时不执行 */
    PAUSED,
    /** 已完成 - 计划已执行完成（达到结束日期或目标） */
    COMPLETED,
    /** 已取消 - 计划已取消 */
    CANCELLED
}