package com.shxc.fundagent.service.impl;

import com.shxc.fundagent.entity.RegularInvestmentPlan;
import com.shxc.fundagent.enums.InvestmentFrequency;
import com.shxc.fundagent.enums.InvestmentPlanStatus;
import com.shxc.fundagent.repository.RegularInvestmentPlanRepository;
import com.shxc.fundagent.service.RegularInvestmentPlanService;
import com.shxc.fundagent.service.TransactionService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 定投计划服务实现类
 */
@Service
@Transactional
public class RegularInvestmentPlanServiceImpl implements RegularInvestmentPlanService {

    private static final Logger logger = LoggerFactory.getLogger(RegularInvestmentPlanServiceImpl.class);

    private final RegularInvestmentPlanRepository planRepository;
    private final TransactionService transactionService;

    @Autowired
    public RegularInvestmentPlanServiceImpl(RegularInvestmentPlanRepository planRepository,
                                            TransactionService transactionService) {
        this.planRepository = planRepository;
        this.transactionService = transactionService;
    }

    @Override
    public RegularInvestmentPlan createPlan(String fundCode, BigDecimal amount, String frequency,
                                            Integer investmentDay, LocalDate startDate, LocalDate endDate,
                                            BigDecimal feeRate, String remark) {
        // 验证参数
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("定投金额必须大于0");
        }

        if (startDate == null) {
            startDate = LocalDate.now();
        }

        // 解析频率
        InvestmentFrequency freqEnum;
        try {
            freqEnum = InvestmentFrequency.valueOf(frequency.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的定投频率: " + frequency);
        }

        // 验证investmentDay
        if (investmentDay != null) {
            switch (freqEnum) {
                case WEEKLY:
                    if (investmentDay < 1 || investmentDay > 7) {
                        throw new IllegalArgumentException("周定投的投资日必须在1-7之间（1=周一，7=周日）");
                    }
                    break;
                case MONTHLY:
                    if (investmentDay < 1 || investmentDay > 31) {
                        throw new IllegalArgumentException("月定投的投资日必须在1-31之间");
                    }
                    break;
                case QUARTERLY:
                    if (investmentDay < 1 || investmentDay > 3) {
                        throw new IllegalArgumentException("季度定投的投资日必须在1-3之间（表示季度内第几个月）");
                    }
                    break;
                case YEARLY:
                    if (investmentDay < 1 || investmentDay > 12) {
                        throw new IllegalArgumentException("年定投的投资日必须在1-12之间（表示月份）");
                    }
                    break;
                default:
                    // DAILY忽略investmentDay
                    break;
            }
        }

        // 创建计划
        RegularInvestmentPlan plan = new RegularInvestmentPlan();
        plan.setFundCode(fundCode);
        plan.setAmount(amount);
        plan.setFrequency(freqEnum);
        plan.setInvestmentDay(investmentDay);
        plan.setStartDate(startDate);
        plan.setEndDate(endDate);
        plan.setFeeRate(feeRate != null ? feeRate : BigDecimal.ZERO);
        plan.setRemark(remark);
        plan.setStatus(InvestmentPlanStatus.ACTIVE);

        // 计算下次执行日期
        plan.calculateNextExecutionDate();

        logger.info("创建定投计划: fundCode={}, amount={}, frequency={}", fundCode, amount, frequency);
        return planRepository.save(plan);
    }

    @Override
    public RegularInvestmentPlan updatePlan(Long planId, BigDecimal amount, String frequency,
                                            Integer investmentDay, LocalDate startDate, LocalDate endDate,
                                            BigDecimal feeRate, String remark) {
        Optional<RegularInvestmentPlan> optional = planRepository.findById(planId);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("定投计划不存在: " + planId);
        }

        RegularInvestmentPlan plan = optional.get();

        // 只能更新活跃或暂停的计划
        if (plan.getStatus() == InvestmentPlanStatus.COMPLETED ||
            plan.getStatus() == InvestmentPlanStatus.CANCELLED) {
            throw new IllegalStateException("无法更新已完成或已取消的计划");
        }

        // 更新字段
        if (amount != null && amount.compareTo(BigDecimal.ZERO) > 0) {
            plan.setAmount(amount);
        }

        if (frequency != null) {
            try {
                InvestmentFrequency freqEnum = InvestmentFrequency.valueOf(frequency.toUpperCase());
                plan.setFrequency(freqEnum);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("无效的定投频率: " + frequency);
            }
        }

        if (investmentDay != null) {
            plan.setInvestmentDay(investmentDay);
        }

        if (startDate != null) {
            plan.setStartDate(startDate);
        }

        if (endDate != null) {
            plan.setEndDate(endDate);
        }

        if (feeRate != null) {
            plan.setFeeRate(feeRate);
        }

        if (remark != null) {
            plan.setRemark(remark);
        }

        // 重新计算下次执行日期
        plan.calculateNextExecutionDate();

        logger.info("更新定投计划: planId={}", planId);
        return planRepository.save(plan);
    }

    @Override
    public RegularInvestmentPlan pausePlan(Long planId) {
        Optional<RegularInvestmentPlan> optional = planRepository.findById(planId);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("定投计划不存在: " + planId);
        }

        RegularInvestmentPlan plan = optional.get();
        plan.pause();

        logger.info("暂停定投计划: planId={}", planId);
        return planRepository.save(plan);
    }

    @Override
    public RegularInvestmentPlan resumePlan(Long planId) {
        Optional<RegularInvestmentPlan> optional = planRepository.findById(planId);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("定投计划不存在: " + planId);
        }

        RegularInvestmentPlan plan = optional.get();
        plan.resume();

        logger.info("恢复定投计划: planId={}", planId);
        return planRepository.save(plan);
    }

    @Override
    public RegularInvestmentPlan cancelPlan(Long planId) {
        Optional<RegularInvestmentPlan> optional = planRepository.findById(planId);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("定投计划不存在: " + planId);
        }

        RegularInvestmentPlan plan = optional.get();
        plan.cancel();

        logger.info("取消定投计划: planId={}", planId);
        return planRepository.save(plan);
    }

    @Override
    public Long executePlan(Long planId) {
        Optional<RegularInvestmentPlan> optional = planRepository.findById(planId);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("定投计划不存在: " + planId);
        }

        RegularInvestmentPlan plan = optional.get();

        // 检查计划状态
        if (plan.getStatus() != InvestmentPlanStatus.ACTIVE) {
            throw new IllegalStateException("只有活跃状态的计划才能执行");
        }

        // 检查是否应该今天执行
        if (!plan.shouldExecuteToday()) {
            throw new IllegalStateException("计划今天不需要执行，下次执行日期: " + plan.getNextExecutionDate());
        }

        // 计算手续费
        BigDecimal fee = plan.calculateFeeAmount();

        // 调用交易服务创建购买交易
        // 使用当前时间作为交易时间
        LocalDateTime transactionTime = LocalDateTime.now();
        var transaction = transactionService.createBuyTransaction(
                plan.getFundCode(),
                plan.getAmount(),
                transactionTime,
                fee
        );

        // 更新计划的下次执行日期
        plan.updateAfterExecution();
        planRepository.save(plan);

        logger.info("执行定投计划成功: planId={}, fundCode={}, amount={}, transactionId={}",
                planId, plan.getFundCode(), plan.getAmount(), transaction.getId());

        return transaction.getId();
    }

    @Override
    public int executeDuePlans() {
        LocalDate today = LocalDate.now();
        List<RegularInvestmentPlan> duePlans = planRepository.findPlansForExecution(today);

        if (duePlans.isEmpty()) {
            logger.info("今天没有需要执行的定投计划");
            return 0;
        }

        int successCount = 0;
        int failCount = 0;

        logger.info("开始执行今天到期的定投计划，共{}个", duePlans.size());

        for (RegularInvestmentPlan plan : duePlans) {
            try {
                executePlan(plan.getId());
                successCount++;
                logger.debug("定投计划执行成功: planId={}, fundCode={}", plan.getId(), plan.getFundCode());
            } catch (Exception e) {
                failCount++;
                logger.error("定投计划执行失败: planId={}, fundCode={}", plan.getId(), plan.getFundCode(), e);
            }
        }

        logger.info("定投计划执行完成，成功: {}，失败: {}", successCount, failCount);
        return successCount;
    }

    @Override
    public RegularInvestmentPlan getPlanById(Long planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("定投计划不存在: " + planId));
    }

    @Override
    public List<RegularInvestmentPlan> getPlansByFundCode(String fundCode) {
        return planRepository.findByFundCode(fundCode);
    }

    @Override
    public List<RegularInvestmentPlan> getPlansByStatus(InvestmentPlanStatus status) {
        return planRepository.findByStatus(status);
    }

    @Override
    public List<RegularInvestmentPlan> getAllActivePlans() {
        return planRepository.findAllActivePlans();
    }

    @Override
    public List<RegularInvestmentPlan> getPlansDueToday() {
        LocalDate today = LocalDate.now();
        return planRepository.findPlansForExecution(today);
    }

    @Override
    public void refreshNextExecutionDates() {
        List<RegularInvestmentPlan> activePlans = planRepository.findAllActivePlans();
        int updatedCount = 0;

        for (RegularInvestmentPlan plan : activePlans) {
            // 如果下次执行日期为空或已过期，重新计算
            if (plan.getNextExecutionDate() == null || plan.getNextExecutionDate().isBefore(LocalDate.now())) {
                plan.calculateNextExecutionDate();
                planRepository.save(plan);
                updatedCount++;
            }
        }

        if (updatedCount > 0) {
            logger.info("刷新了{}个定投计划的下次执行日期", updatedCount);
        }
    }

    @Override
    public BigDecimal calculateTotalInvestedAmount(Long planId) {
        // TODO: 未来需要关联交易记录来计算总投资金额
        // 目前简化实现：返回0
        return BigDecimal.ZERO;
    }

    @Override
    public List<Long> getExecutionHistory(Long planId) {
        // TODO: 未来需要查询关联的交易记录
        // 目前返回空列表
        return List.of();
    }
}