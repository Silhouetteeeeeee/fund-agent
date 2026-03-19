package com.shxc.fundagent.entity;

import com.shxc.fundagent.enums.InvestmentFrequency;
import com.shxc.fundagent.enums.InvestmentPlanStatus;
import com.shxc.fundagent.utils.TradeDayUtils;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 定投计划实体
 * 记录定期投资计划的配置信息
 */
@Entity
@Table(name = "regular_investment_plan")
@Data
public class RegularInvestmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 基金代码，关联基金基础信息
     */
    @Column(name = "fund_code", nullable = false, length = 10)
    private String fundCode;

    /**
     * 定投金额（每次投资的金额）
     */
    @Column(name = "amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal amount;

    /**
     * 定投频率
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private InvestmentFrequency frequency;

    /**
     * 定投日
     * - 对于月定投：表示每月几号（1-31）
     * - 对于周定投：表示星期几（1-7，1=周一，7=周日）
     * - 对于日定投：忽略此字段
     * - 对于季度定投：表示季度内第几个月（1-3）
     * - 对于年定投：表示年内第几个月（1-12）
     */
    @Column(name = "investment_day")
    private Integer investmentDay;

    /**
     * 计划开始日期
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * 计划结束日期（可选，为空表示无限期）
     */
    @Column(name = "end_date")
    private LocalDate endDate;

    /**
     * 下次执行日期
     */
    @Column(name = "next_execution_date")
    private LocalDate nextExecutionDate;

    /**
     * 状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvestmentPlanStatus status = InvestmentPlanStatus.ACTIVE;

    /**
     * 手续费率（百分比，如0.15表示0.15%）
     */
    @Column(name = "fee_rate", precision = 5, scale = 3)
    private BigDecimal feeRate = BigDecimal.ZERO;

    /**
     * 备注信息
     */
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    /**
     * 无参构造函数
     */
    public RegularInvestmentPlan() {
    }

    /**
     * 带基本参数的构造函数
     */
    public RegularInvestmentPlan(String fundCode, BigDecimal amount, InvestmentFrequency frequency,
                                 LocalDate startDate) {
        this.fundCode = fundCode;
        this.amount = amount;
        this.frequency = frequency;
        this.startDate = startDate;
        this.status = InvestmentPlanStatus.ACTIVE;
        calculateNextExecutionDate();
    }

    /**
     * 计算下次执行日期
     * 基于当前日期和定投频率
     */
    public void calculateNextExecutionDate() {
        if (status != InvestmentPlanStatus.ACTIVE) {
            this.nextExecutionDate = null;
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate nextDate = startDate;

        // 如果开始日期在未来，直接使用开始日期
        if (startDate.isAfter(today)) {
            this.nextExecutionDate = startDate;
            return;
        }

        // 根据频率计算下次执行日期
        switch (frequency) {
            case DAILY:
                nextDate = TradeDayUtils.findNextTradeDay(today.plusDays(1), frequency);
                break;
            case WEEKLY:
                if (investmentDay != null && investmentDay >= 1 && investmentDay <= 7) {
                    // 计算下一个指定星期几
                    int currentDayOfWeek = today.getDayOfWeek().getValue(); // 1=周一，7=周日
                    int daysToAdd = investmentDay - currentDayOfWeek;
                    if (daysToAdd <= 0) {
                        daysToAdd += 7;
                    }
                    nextDate = TradeDayUtils.findNextTradeDay(today.plusDays(daysToAdd), frequency);
                } else {
                    // 默认每周一
                    nextDate = today.plusDays((8 - today.getDayOfWeek().getValue()) % 7);
                    nextDate = TradeDayUtils.findNextTradeDay(nextDate, frequency);
                }
                break;
            case MONTHLY:
                if (investmentDay != null && investmentDay >= 1 && investmentDay <= 31) {
                    nextDate = today.withDayOfMonth(1).plusMonths(1);
                    // 确保日期有效（如2月31日）
                    int dayOfMonth = Math.min(investmentDay, nextDate.lengthOfMonth());
                    nextDate = nextDate.withDayOfMonth(dayOfMonth);
                    // 如果计算出的日期在今天或之前，再加一个月
                    if (!nextDate.isAfter(today)) {
                        nextDate = nextDate.plusMonths(1);
                        dayOfMonth = Math.min(investmentDay, nextDate.lengthOfMonth());
                        nextDate = nextDate.withDayOfMonth(dayOfMonth);
                    }
                } else {
                    // 默认每月1号
                    nextDate = today.withDayOfMonth(1).plusMonths(1);
                    if (!nextDate.isAfter(today)) {
                        nextDate = nextDate.plusMonths(1);
                    }
                }
                break;
            case QUARTERLY:
                // 季度定投：每3个月一次
                nextDate = today.withDayOfMonth(1).plusMonths(3);
                if (investmentDay != null && investmentDay >= 1 && investmentDay <= 31) {
                    int dayOfMonth = Math.min(investmentDay, nextDate.lengthOfMonth());
                    nextDate = nextDate.withDayOfMonth(dayOfMonth);
                }
                if (!nextDate.isAfter(today)) {
                    nextDate = nextDate.plusMonths(3);
                }
                break;
            case YEARLY:
                // 年定投：每年一次
                nextDate = today.plusYears(1);
                if (investmentDay != null && investmentDay >= 1 && investmentDay <= 12) {
                    nextDate = nextDate.withMonth(investmentDay).withDayOfMonth(1);
                    if (!nextDate.isAfter(today)) {
                        nextDate = nextDate.plusYears(1);
                    }
                }
                break;
        }

        // 检查是否超过结束日期
        if (endDate != null && nextDate.isAfter(endDate)) {
            this.nextExecutionDate = null;
            this.status = InvestmentPlanStatus.COMPLETED;
        } else {
            this.nextExecutionDate = nextDate;
        }
    }

    /**
     * 检查今天是否需要执行定投
     */
    public boolean shouldExecuteToday() {
        if (status != InvestmentPlanStatus.ACTIVE) {
            return false;
        }

        LocalDate today = LocalDate.now();

        // 检查是否在开始日期之后
        if (today.isBefore(startDate)) {
            return false;
        }

        // 检查是否已过结束日期
        if (endDate != null && today.isAfter(endDate)) {
            this.status = InvestmentPlanStatus.COMPLETED;
            return false;
        }

        // 检查下次执行日期是否为今天
        if (nextExecutionDate != null && nextExecutionDate.equals(today)) {
            return true;
        }

        // 如果没有设置下次执行日期，重新计算
        if (nextExecutionDate == null) {
            calculateNextExecutionDate();
            return nextExecutionDate != null && nextExecutionDate.equals(today);
        }

        return false;
    }

    /**
     * 执行定投后更新下次执行日期
     */
    public void updateAfterExecution() {
        if (nextExecutionDate == null) {
            calculateNextExecutionDate();
            return;
        }

        LocalDate lastExecutionDate = nextExecutionDate;
        calculateNextExecutionDate();

        // 如果下次执行日期与上次相同（可能因为假期跳过），继续计算下一次
        while (nextExecutionDate != null && nextExecutionDate.equals(lastExecutionDate)) {
            switch (frequency) {
                case DAILY:
                    nextExecutionDate = nextExecutionDate.plusDays(1);
                    break;
                case WEEKLY:
                    nextExecutionDate = nextExecutionDate.plusWeeks(1);
                    break;
                case MONTHLY:
                    nextExecutionDate = nextExecutionDate.plusMonths(1);
                    break;
                case QUARTERLY:
                    nextExecutionDate = nextExecutionDate.plusMonths(3);
                    break;
                case YEARLY:
                    nextExecutionDate = nextExecutionDate.plusYears(1);
                    break;
            }

            // 检查是否超过结束日期
            if (endDate != null && nextExecutionDate.isAfter(endDate)) {
                nextExecutionDate = null;
                status = InvestmentPlanStatus.COMPLETED;
                break;
            }
        }
    }

    /**
     * 暂停计划
     */
    public void pause() {
        this.status = InvestmentPlanStatus.PAUSED;
        this.nextExecutionDate = null;
    }

    /**
     * 恢复计划
     */
    public void resume() {
        if (this.status == InvestmentPlanStatus.PAUSED) {
            this.status = InvestmentPlanStatus.ACTIVE;
            calculateNextExecutionDate();
        }
    }

    /**
     * 取消计划
     */
    public void cancel() {
        this.status = InvestmentPlanStatus.CANCELLED;
        this.nextExecutionDate = null;
    }

    /**
     * 计算手续费金额
     */
    public BigDecimal calculateFeeAmount() {
        if (feeRate == null || feeRate.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(feeRate).divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }
}