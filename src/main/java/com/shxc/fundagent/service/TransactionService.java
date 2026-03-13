package com.shxc.fundagent.service;

import com.shxc.fundagent.entity.FundTransactionRecord;
import com.shxc.fundagent.enums.TransactionStatus;
import com.shxc.fundagent.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 基金交易服务接口
 * 处理基金交易记录的管理、确认和持仓更新
 */
public interface TransactionService {

    /**
     * 创建购买交易记录
     * @param fundCode 基金代码
     * @param totalAmount 买入金额
     * @param transactionTime 交易时间
     * @param fee 手续费（可选）
     * @return 交易记录
     */
    FundTransactionRecord createBuyTransaction(String fundCode, BigDecimal totalAmount,
                                               LocalDateTime transactionTime, BigDecimal fee);

    /**
     * 创建赎回交易记录
     * @param fundCode 基金代码
     * @param amount 赎回份额
     * @param transactionTime 交易时间
     * @param fee 手续费（可选）
     * @return 交易记录
     */
    FundTransactionRecord createSellTransaction(String fundCode, BigDecimal amount,
                                                LocalDateTime transactionTime, BigDecimal fee);

    /**
     * 确认交易
     * @param transactionId 交易记录ID
     * @param confirmedAmount 实际确认份额（考虑手续费后）
     * @param actualPrice 实际成交价格
     * @return 确认后的交易记录
     */
    FundTransactionRecord confirmTransaction(Long transactionId, BigDecimal confirmedAmount, BigDecimal actualPrice);

    /**
     * 取消交易
     * @param transactionId 交易记录ID
     * @return 取消后的交易记录
     */
    FundTransactionRecord cancelTransaction(Long transactionId);

    /**
     * 结算赎回交易
     * @param transactionId 交易记录ID
     * @return 结算后的交易记录
     */
    FundTransactionRecord settleSellTransaction(Long transactionId);

    /**
     * 根据交易记录更新持仓信息
     * @param fundCode 基金代码
     * @return 更新后的持仓ID
     */
    Long updateHoldingFromTransactions(String fundCode);

    /**
     * 批量处理待确认交易（定时任务调用）
     * @return 处理成功的交易数量
     */
    int processPendingTransactions();

    /**
     * 计算基金的平均持仓成本
     * @param fundCode 基金代码
     * @return 平均成本价格
     */
    BigDecimal calculateAverageCost(String fundCode);

    /**
     * 计算基金的总持仓份额
     * @param fundCode 基金代码
     * @return 总持仓份额
     */
    BigDecimal calculateTotalHoldingAmount(String fundCode);

    /**
     * 计算基金的总持仓成本
     * @param fundCode 基金代码
     * @return 总持仓成本
     */
    BigDecimal calculateTotalHoldingCost(String fundCode);

    /**
     * 获取基金的交易历史
     * @param fundCode 基金代码
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 交易记录列表
     */
    List<FundTransactionRecord> getTransactionHistory(String fundCode, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取待确认的交易记录
     * @return 待确认交易记录列表
     */
    List<FundTransactionRecord> getPendingTransactions();

    /**
     * 根据持仓ID获取相关交易记录
     * @param holdingId 持仓ID
     * @return 交易记录列表
     */
    List<FundTransactionRecord> getTransactionsByHoldingId(Long holdingId);

    /**
     * 重新计算所有基金的持仓信息
     * @return 更新成功的基金数量
     */
    int recalculateAllHoldings();

    /**
     * 验证交易数据完整性
     * @param fundCode 基金代码
     * @return 验证结果
     */
    ValidationResult validateTransactionData(String fundCode);

    /**
     * 获取交易统计信息
     * @param fundCode 基金代码（可选，为null时统计所有基金）
     * @return 统计信息
     */
    TransactionStatistics getTransactionStatistics(String fundCode);

    /**
     * 判断是否交易日
     * @param day 日期
     * @return 是否交易日
     */
    boolean isTradeDay(LocalDate day);

    // ================ 内部数据类 ================

    /**
     * 交易统计信息
     */
    class TransactionStatistics {
        private int totalTransactions;
        private int buyTransactions;
        private int sellTransactions;
        private BigDecimal totalBuyAmount;
        private BigDecimal totalSellAmount;
        private BigDecimal totalFee;
        private BigDecimal averageBuyPrice;
        private BigDecimal averageSellPrice;
        private int pendingCount;
        private int confirmedCount;
        private int cancelledCount;

        // getters and setters
        public int getTotalTransactions() { return totalTransactions; }
        public void setTotalTransactions(int totalTransactions) { this.totalTransactions = totalTransactions; }
        public int getBuyTransactions() { return buyTransactions; }
        public void setBuyTransactions(int buyTransactions) { this.buyTransactions = buyTransactions; }
        public int getSellTransactions() { return sellTransactions; }
        public void setSellTransactions(int sellTransactions) { this.sellTransactions = sellTransactions; }
        public BigDecimal getTotalBuyAmount() { return totalBuyAmount; }
        public void setTotalBuyAmount(BigDecimal totalBuyAmount) { this.totalBuyAmount = totalBuyAmount; }
        public BigDecimal getTotalSellAmount() { return totalSellAmount; }
        public void setTotalSellAmount(BigDecimal totalSellAmount) { this.totalSellAmount = totalSellAmount; }
        public BigDecimal getTotalFee() { return totalFee; }
        public void setTotalFee(BigDecimal totalFee) { this.totalFee = totalFee; }
        public BigDecimal getAverageBuyPrice() { return averageBuyPrice; }
        public void setAverageBuyPrice(BigDecimal averageBuyPrice) { this.averageBuyPrice = averageBuyPrice; }
        public BigDecimal getAverageSellPrice() { return averageSellPrice; }
        public void setAverageSellPrice(BigDecimal averageSellPrice) { this.averageSellPrice = averageSellPrice; }
        public int getPendingCount() { return pendingCount; }
        public void setPendingCount(int pendingCount) { this.pendingCount = pendingCount; }
        public int getConfirmedCount() { return confirmedCount; }
        public void setConfirmedCount(int confirmedCount) { this.confirmedCount = confirmedCount; }
        public int getCancelledCount() { return cancelledCount; }
        public void setCancelledCount(int cancelledCount) { this.cancelledCount = cancelledCount; }
    }

    /**
     * 验证结果
     */
    class ValidationResult {
        private boolean isValid;
        private List<String> errors;
        private List<String> warnings;

        public boolean isValid() { return isValid; }
        public void setValid(boolean valid) { isValid = valid; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }
}