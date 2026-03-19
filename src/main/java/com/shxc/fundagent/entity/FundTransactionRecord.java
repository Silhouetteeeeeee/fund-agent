package com.shxc.fundagent.entity;

import com.shxc.fundagent.enums.TransactionStatus;
import com.shxc.fundagent.enums.TransactionType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 基金交易记录实体
 * 记录每笔基金交易（购买/赎回）的详细信息，用于计算持仓成本和更新持仓
 */
@Entity
@Table(name = "fund_transaction_record")
@Data
public class FundTransactionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 基金代码，关联基金基础信息
     */
    @Column(name = "fund_code", nullable = false, length = 10)
    private String fundCode;

    /**
     * 交易类型：购买/赎回
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    /**
     * 交易份额（正数）
     */
    @Column(name = "amount")
    private BigDecimal amount;

    /**
     * 交易价格（单位净值）
     */
    @Column(name = "price")
    private BigDecimal price;

    /**
     * 交易金额（份额 × 价格）
     */
    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    /**
     * 手续费
     */
    @Column(name = "fee")
    private BigDecimal fee = BigDecimal.ZERO;

    /**
     * 实际确认份额（考虑手续费后）
     */
    @Column(name = "confirmed_amount")
    private BigDecimal confirmedAmount;

    /**
     * 交易时间（用户提交时间）
     */
    @Column(name = "transaction_time", nullable = false)
    private LocalDateTime transactionTime;

    /**
     * 预计确认时间（根据交易时间计算）
     */
    @Column(name = "estimated_confirm_date")
    private LocalDate estimatedConfirmDate;

    /**
     * 实际确认时间
     */
    @Column(name = "actual_confirm_time")
    private LocalDateTime actualConfirmTime;

    /**
     * 交易状态
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    /**
     * 关联的持仓记录ID（如果已关联）
     */
    @Column(name = "holding_id")
    private Long holdingId;

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
     * 无参构造函数
     */
    public FundTransactionRecord() {
    }

    /**
     * 带基本参数的构造函数
     */
    public FundTransactionRecord(String fundCode, TransactionType transactionType,
                                 BigDecimal amount, BigDecimal price, LocalDateTime transactionTime) {
        this.fundCode = fundCode;
        this.transactionType = transactionType;
        this.amount = amount;
        this.price = price;
        this.transactionTime = transactionTime;
        this.totalAmount = amount.multiply(price);
    }

    /**
     * 确认交易
     * @param confirmedAmount 实际确认份额（考虑手续费后）
     * @param actualPrice 实际成交价格（可能与预期价格不同）
     */
    public void confirmTransaction(BigDecimal confirmedAmount, BigDecimal actualPrice) {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException("只有待确认状态的交易才能确认");
        }

        this.confirmedAmount = confirmedAmount;
        this.price = actualPrice;
        this.totalAmount = confirmedAmount.multiply(actualPrice);
        this.actualConfirmTime = LocalDateTime.now();
        this.status = TransactionStatus.CONFIRMED;
    }

    /**
     * 取消交易
     */
    public void cancelTransaction() {
        if (this.status != TransactionStatus.PENDING) {
            throw new IllegalStateException("只有待确认状态的交易才能取消");
        }

        this.status = TransactionStatus.CANCELLED;
    }

    /**
     * 结算交易（用于赎回交易）
     */
    public void settleTransaction() {
        if (this.status != TransactionStatus.CONFIRMED || this.transactionType != TransactionType.SELL) {
            throw new IllegalStateException("只有已确认的赎回交易才能结算");
        }

        this.status = TransactionStatus.SETTLED;
    }

    /**
     * 获取交易方向符号：购买为正，赎回为负
     */
    public int getTransactionSign() {
        return transactionType == TransactionType.BUY ? 1 : -1;
    }

    /**
     * 计算交易成本（考虑手续费）
     */
    public BigDecimal getTotalCost() {
        if (fee == null) {
            return totalAmount;
        }
        BigDecimal feeRate = fee.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal feeAmount = totalAmount.multiply(feeRate);
        return totalAmount.subtract(feeAmount);
    }

    /**
     * 关联持仓记录
     */
    public void associateWithHolding(Long holdingId) {
        this.holdingId = holdingId;
    }

}