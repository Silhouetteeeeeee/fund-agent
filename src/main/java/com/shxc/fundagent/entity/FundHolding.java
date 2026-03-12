package com.shxc.fundagent.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 持仓信息实体
 * 对应详细设计文档中的 fund_holding 表
 */
@Entity
@Table(name = "fund_holding")
@Data
public class FundHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 基金代码，关联基金基础信息
     */
    @Column(name = "fund_code", nullable = false, length = 10)
    private String fundCode;

    /**
     * 持仓成本价
     */
    @Column(name = "cost_price", nullable = false, precision = 10, scale = 4)
    private BigDecimal costPrice;

    /**
     * 持仓份额
     */
    @Column(name = "holding_amount", nullable = false, precision = 16, scale = 2)
    private BigDecimal holdingAmount;

    /**
     * 持仓市值（冗余字段，便于查询）
     */
    @Column(name = "holding_value", precision = 16, scale = 2)
    private BigDecimal holdingValue;

    /**
     * 购买日期
     */
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    /**
     * 持仓状态：ACTIVE-持有中，SOLD-已卖出
     */
    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    /**
     * 卖出日期
     */
    @Column(name = "sell_date")
    private LocalDate sellDate;

    /**
     * 卖出价格
     */
    @Column(name = "sell_price", precision = 10, scale = 4)
    private BigDecimal sellPrice;

    /**
     * 卖出收益
     */
    @Column(name = "sell_profit", precision = 16, scale = 2)
    private BigDecimal sellProfit;

    /**
     * 备注信息
     */
    @Column(name = "remark", length = 200)
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
     * 关联的基金基础信息（非数据库字段，用于查询）
     */
    @Transient
    private FundInfo fundInfo;

    /**
     * 无参构造函数
     */
    public FundHolding() {
    }

    /**
     * 带基本参数的构造函数
     */
    public FundHolding(String fundCode, BigDecimal costPrice, BigDecimal holdingAmount, LocalDate purchaseDate) {
        this.fundCode = fundCode;
        this.costPrice = costPrice;
        this.holdingAmount = holdingAmount;
        this.purchaseDate = purchaseDate;
        this.status = "ACTIVE";
    }

    /**
     * 计算持仓成本总额
     */
    public BigDecimal getTotalCost() {
        if (costPrice == null || holdingAmount == null) {
            return BigDecimal.ZERO;
        }
        return costPrice.multiply(holdingAmount);
    }

    /**
     * 更新持仓市值
     * @param currentPrice 当前价格
     */
    public void updateHoldingValue(BigDecimal currentPrice) {
        if (currentPrice != null && holdingAmount != null && "ACTIVE".equals(status)) {
            this.holdingValue = currentPrice.multiply(holdingAmount);
        } else {
            this.holdingValue = BigDecimal.ZERO;
        }
    }

    /**
     * 计算持仓收益
     * @param currentPrice 当前价格
     * @return 收益金额
     */
    public BigDecimal calculateProfit(BigDecimal currentPrice) {
        if (currentPrice == null || costPrice == null || holdingAmount == null || !"ACTIVE".equals(status)) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(costPrice).multiply(holdingAmount);
    }

    /**
     * 计算持仓收益率
     * @param currentPrice 当前价格
     * @return 收益率（百分比）
     */
    public BigDecimal calculateYieldRate(BigDecimal currentPrice) {
        if (currentPrice == null || costPrice == null || costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return currentPrice.subtract(costPrice)
                .divide(costPrice, 4, java.math.RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    // ================ 兼容性方法（供StrategyDecisionResult使用） ================

    /**
     * 获取持仓份额（兼容性方法）
     */
    public BigDecimal getHoldShare() {
        return holdingAmount;
    }

    /**
     * 获取当前市值（兼容性方法）
     */
    public BigDecimal getCurrentValue() {
        return holdingValue;
    }

    /**
     * 获取持仓收益（兼容性方法，需要当前价格）
     * 注意：此方法需要当前价格，暂返回0
     */
    public BigDecimal getHoldProfit() {
        return BigDecimal.ZERO;
    }

    /**
     * 获取持仓收益率（兼容性方法，需要当前价格）
     * 注意：此方法需要当前价格，暂返回0
     */
    public BigDecimal getHoldProfitRate() {
        return BigDecimal.ZERO;
    }

    /**
     * 获取持仓天数（兼容性方法）
     */
    public Integer getHoldDays() {
        if (purchaseDate == null) {
            return 0;
        }
        return (int) java.time.temporal.ChronoUnit.DAYS.between(purchaseDate, LocalDate.now());
    }

    /**
     * 获取当前价格（兼容性方法）
     * 注意：FundHolding实体不存储当前价格，需要通过其他方式获取
     */
    public BigDecimal getCurrentPrice() {
        // 尝试通过holdingValue和holdingAmount计算
        if (holdingValue != null && holdingAmount != null && holdingAmount.compareTo(BigDecimal.ZERO) != 0) {
            return holdingValue.divide(holdingAmount, 4, java.math.RoundingMode.HALF_UP);
        }
        return null;
    }
}