package com.shxc.fundagent.repository;

import com.shxc.fundagent.entity.RegularInvestmentPlan;
import com.shxc.fundagent.enums.InvestmentPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * 定投计划数据访问接口
 */
@Repository
public interface RegularInvestmentPlanRepository extends JpaRepository<RegularInvestmentPlan, Long> {

    /**
     * 根据基金代码查询定投计划
     */
    List<RegularInvestmentPlan> findByFundCode(String fundCode);

    /**
     * 根据状态查询定投计划
     */
    List<RegularInvestmentPlan> findByStatus(InvestmentPlanStatus status);

    /**
     * 根据基金代码和状态查询定投计划
     */
    List<RegularInvestmentPlan> findByFundCodeAndStatus(String fundCode, InvestmentPlanStatus status);

    /**
     * 查询今天需要执行的定投计划
     */
    @Query("SELECT p FROM RegularInvestmentPlan p WHERE p.status = com.shxc.fundagent.enums.InvestmentPlanStatus.ACTIVE " +
           "AND p.nextExecutionDate = :today")
    List<RegularInvestmentPlan> findPlansForExecution(@Param("today") LocalDate today);

    /**
     * 查询所有活跃的定投计划
     */
    @Query("SELECT p FROM RegularInvestmentPlan p WHERE p.status = com.shxc.fundagent.enums.InvestmentPlanStatus.ACTIVE")
    List<RegularInvestmentPlan> findAllActivePlans();

    /**
     * 查询指定日期范围内需要执行的定投计划
     */
    @Query("SELECT p FROM RegularInvestmentPlan p WHERE p.status = com.shxc.fundagent.enums.InvestmentPlanStatus.ACTIVE " +
           "AND p.nextExecutionDate BETWEEN :startDate AND :endDate")
    List<RegularInvestmentPlan> findPlansForExecutionBetween(@Param("startDate") LocalDate startDate,
                                                             @Param("endDate") LocalDate endDate);

    /**
     * 检查基金是否有活跃的定投计划
     */
    @Query("SELECT COUNT(p) > 0 FROM RegularInvestmentPlan p WHERE p.fundCode = :fundCode " +
           "AND p.status = com.shxc.fundagent.enums.InvestmentPlanStatus.ACTIVE")
    boolean existsActivePlanByFundCode(@Param("fundCode") String fundCode);

    /**
     * 根据下次执行日期查询定投计划
     */
    List<RegularInvestmentPlan> findByNextExecutionDate(LocalDate nextExecutionDate);

    /**
     * 根据下次执行日期和状态查询定投计划
     */
    List<RegularInvestmentPlan> findByNextExecutionDateAndStatus(LocalDate nextExecutionDate, InvestmentPlanStatus status);
}