package com.shxc.fundagent.service;

import com.shxc.fundagent.entity.RegularInvestmentPlan;
import com.shxc.fundagent.enums.InvestmentPlanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 定投计划服务接口
 */
public interface RegularInvestmentPlanService {

    /**
     * 创建定投计划
     */
    RegularInvestmentPlan createPlan(String fundCode, BigDecimal amount, String frequency,
                                     Integer investmentDay, LocalDate startDate, LocalDate endDate,
                                     BigDecimal feeRate, String remark);

    /**
     * 更新定投计划
     */
    RegularInvestmentPlan updatePlan(Long planId, BigDecimal amount, String frequency,
                                     Integer investmentDay, LocalDate startDate, LocalDate endDate,
                                     BigDecimal feeRate, String remark);

    /**
     * 暂停定投计划
     */
    RegularInvestmentPlan pausePlan(Long planId);

    /**
     * 恢复定投计划
     */
    RegularInvestmentPlan resumePlan(Long planId);

    /**
     * 取消定投计划
     */
    RegularInvestmentPlan cancelPlan(Long planId);

    /**
     * 执行定投计划（生成交易记录）
     * @return 生成的交易记录ID
     */
    Long executePlan(Long planId);

    /**
     * 批量执行今天到期的定投计划
     * @return 成功执行的计划数量
     */
    int executeDuePlans();

    /**
     * 根据ID获取定投计划
     */
    RegularInvestmentPlan getPlanById(Long planId);

    /**
     * 根据基金代码获取定投计划
     */
    List<RegularInvestmentPlan> getPlansByFundCode(String fundCode);

    /**
     * 根据状态获取定投计划
     */
    List<RegularInvestmentPlan> getPlansByStatus(InvestmentPlanStatus status);

    /**
     * 获取所有活跃的定投计划
     */
    List<RegularInvestmentPlan> getAllActivePlans();

    /**
     * 获取今天需要执行的定投计划
     */
    List<RegularInvestmentPlan> getPlansDueToday();

    /**
     * 检查并更新所有计划的下次执行日期
     */
    void refreshNextExecutionDates();

    /**
     * 计算定投计划的总投资金额（已执行次数 × 每次金额）
     */
    BigDecimal calculateTotalInvestedAmount(Long planId);

    /**
     * 获取定投计划的执行历史（关联的交易记录）
     */
    List<Long> getExecutionHistory(Long planId);
}