package com.shxc.fundagent.service.impl;

import com.shxc.fundagent.entity.FundDailyData;
import com.shxc.fundagent.entity.FundHolding;
import com.shxc.fundagent.entity.FundTransactionRecord;
import com.shxc.fundagent.enums.TransactionStatus;
import com.shxc.fundagent.enums.TransactionType;
import com.shxc.fundagent.repository.FundHoldingRepository;
import com.shxc.fundagent.repository.FundTransactionRecordRepository;
import com.shxc.fundagent.service.FundDataService;
import com.shxc.fundagent.service.HolidayCalendarService;
import com.shxc.fundagent.service.TransactionService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 基金交易服务实现类
 */
@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    private final FundTransactionRecordRepository transactionRecordRepository;
    private final FundHoldingRepository holdingRepository;
    private final HolidayCalendarService holidayCalendarService;
    private final FundDataService fundDataService;

    @Autowired
    public TransactionServiceImpl(FundTransactionRecordRepository transactionRecordRepository,
                                  FundHoldingRepository holdingRepository, HolidayCalendarService holidayCalendarService, FundDataService fundDataService) {
        this.transactionRecordRepository = transactionRecordRepository;
        this.holdingRepository = holdingRepository;
        this.holidayCalendarService = holidayCalendarService;
        this.fundDataService = fundDataService;
    }

    @Override
    public FundTransactionRecord createBuyTransaction(String fundCode, BigDecimal totalAmount,
                                                      LocalDateTime transactionTime, BigDecimal fee) {
        if (transactionTime == null) {
            transactionTime = LocalDateTime.now();
        }
        if (fee == null) {
            fee = BigDecimal.ZERO;
        }

        FundTransactionRecord transaction = new FundTransactionRecord();
        transaction.setFundCode(fundCode);
        transaction.setTransactionType(TransactionType.BUY);
        transaction.setTransactionTime(transactionTime);
        transaction.setFee(fee);
        transaction.setTotalAmount(totalAmount);
        calculateEstimatedConfirmTime(transaction);
        transaction.setStatus(TransactionStatus.PENDING);
        if (transaction.getEstimatedConfirmDate().isBefore(LocalDate.now())) {
            // 历史数据初始化
            List<FundDailyData> historyDataList = fundDataService.getHistoryData(fundCode, transaction.getEstimatedConfirmDate(), transaction.getEstimatedConfirmDate());
            if (historyDataList != null && historyDataList.size() !=0 ) {
                FundDailyData data = historyDataList.get(0);
                if (data.getNetValue() != null) {
                    transaction.setPrice(data.getNetValue());
                    transaction.setAmount(transaction.getTotalCost().divide(data.getNetValue(), 4, RoundingMode.HALF_UP));
                    transaction.setActualConfirmTime(LocalDateTime.now());
                    transaction.setStatus(TransactionStatus.CONFIRMED);
                }
            }
        }
        return transactionRecordRepository.save(transaction);
    }

    @Override
    public FundTransactionRecord createSellTransaction(String fundCode, BigDecimal amount,
                                                       LocalDateTime transactionTime, BigDecimal fee) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("赎回份额必须大于0");
        }
        if (transactionTime == null) {
            transactionTime = LocalDateTime.now();
        }
        if (fee == null) {
            fee = BigDecimal.ZERO;
        }

        // 检查是否有足够持仓份额可以卖出
        BigDecimal totalHolding = calculateTotalHoldingAmount(fundCode);
        if (totalHolding.compareTo(amount) < 0) {
            throw new IllegalArgumentException("持仓份额不足，无法卖出。当前持仓: " + totalHolding + ", 卖出请求: " + amount);
        }

        FundTransactionRecord transaction = new FundTransactionRecord();
        transaction.setFundCode(fundCode);
        transaction.setTransactionType(TransactionType.SELL);
        transaction.setAmount(amount);
        transaction.setTransactionTime(transactionTime);
        transaction.setFee(fee);
        transaction.setStatus(TransactionStatus.PENDING);
        calculateEstimatedConfirmTime(transaction);

        if (transaction.getEstimatedConfirmDate().isBefore(LocalDate.now())) {
            // 历史数据初始化
            List<FundDailyData> historyDataList = fundDataService.getHistoryData(fundCode, transaction.getEstimatedConfirmDate(), transaction.getEstimatedConfirmDate());
            if (historyDataList != null && historyDataList.size() !=0 ) {
                FundDailyData data = historyDataList.get(0);
                if (data.getNetValue() != null) {
                    transaction.setPrice(data.getNetValue());
                    transaction.setTotalAmount(amount.multiply(data.getNetValue()));
                    transaction.setActualConfirmTime(LocalDateTime.now());
                    transaction.setStatus(TransactionStatus.CONFIRMED);
                }
            }
        }

        return transactionRecordRepository.save(transaction);
    }

    public void calculateEstimatedConfirmTime(FundTransactionRecord t) {
        LocalDateTime transactionTime = t.getTransactionTime();
        if (transactionTime == null) {
            return;
        }

        LocalDate transactionDate = transactionTime.toLocalDate();
        LocalTime transactionTimeOfDay = transactionTime.toLocalTime();

        // 判断是否在15:00前
        boolean beforeCutoff = transactionTimeOfDay.isBefore(LocalTime.of(15, 0));

        // 如果是购买交易，确认时间通常是T+1或T+2
        LocalDate confirmDate = transactionDate;
        if (!beforeCutoff) {
            // 下一个交易日的净值确认
            confirmDate = transactionDate.plusDays(1);
        }
        while (!isTradeDay(confirmDate)) {
            confirmDate = confirmDate.plusDays(1);
        }
        // 设置预计确认时间为确认日的09:00（假设）
        t.setEstimatedConfirmDate(confirmDate);
    }

    @Override
    public FundTransactionRecord confirmTransaction(Long transactionId, BigDecimal confirmedAmount, BigDecimal actualPrice) {
        Optional<FundTransactionRecord> optional = transactionRecordRepository.findById(transactionId);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("交易记录不存在: " + transactionId);
        }

        FundTransactionRecord transaction = optional.get();
        transaction.confirmTransaction(confirmedAmount, actualPrice);

        // 如果交易确认，更新关联的持仓信息
        if (transaction.getTransactionType() == TransactionType.BUY) {
            updateHoldingFromTransactions(transaction.getFundCode());
        } else if (transaction.getTransactionType() == TransactionType.SELL) {
            // 对于卖出交易，需要更新持仓份额
            updateHoldingFromTransactions(transaction.getFundCode());
        }

        return transactionRecordRepository.save(transaction);
    }

    @Override
    public FundTransactionRecord cancelTransaction(Long transactionId) {
        Optional<FundTransactionRecord> optional = transactionRecordRepository.findById(transactionId);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("交易记录不存在: " + transactionId);
        }

        FundTransactionRecord transaction = optional.get();
        transaction.cancelTransaction();
        return transactionRecordRepository.save(transaction);
    }

    @Override
    public FundTransactionRecord settleSellTransaction(Long transactionId) {
        Optional<FundTransactionRecord> optional = transactionRecordRepository.findById(transactionId);
        if (optional.isEmpty()) {
            throw new IllegalArgumentException("交易记录不存在: " + transactionId);
        }

        FundTransactionRecord transaction = optional.get();
        transaction.settleTransaction();
        return transactionRecordRepository.save(transaction);
    }

    @Override
    public Long updateHoldingFromTransactions(String fundCode) {
        // 计算当前持仓份额和成本
        BigDecimal totalConfirmedBuyAmount = transactionRecordRepository
                .sumBuyConfirmedAmountByFundCode(fundCode)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalConfirmedSellAmount = transactionRecordRepository
                .sumSellConfirmedAmountByFundCode(fundCode)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalBuyAmount = transactionRecordRepository
                .sumBuyTotalAmountByFundCode(fundCode)
                .orElse(BigDecimal.ZERO);

        BigDecimal totalSellAmount = transactionRecordRepository
                .sumSellTotalAmountByFundCode(fundCode)
                .orElse(BigDecimal.ZERO);

        // 净持仓份额 = 总购买份额 - 总卖出份额
        BigDecimal netHoldingAmount = totalConfirmedBuyAmount.subtract(totalConfirmedSellAmount);

        // 净持仓成本 = 总购买金额 - 总卖出金额
        BigDecimal netHoldingCost = totalBuyAmount.subtract(totalSellAmount);

        // 平均成本价格 = 净持仓成本 / 净持仓份额 (如果份额>0)
        BigDecimal averageCostPrice = BigDecimal.ZERO;
        if (netHoldingAmount.compareTo(BigDecimal.ZERO) > 0) {
            averageCostPrice = netHoldingCost.divide(netHoldingAmount, 4, RoundingMode.HALF_UP);
        }

        // 查找或创建持仓记录
        List<FundHolding> holdings = holdingRepository.findByFundCodeAndStatus(fundCode, "ACTIVE");
        FundHolding holding;

        if (holdings.isEmpty()) {
            // 创建新持仓记录
            holding = new FundHolding();
            holding.setFundCode(fundCode);
            holding.setStatus("ACTIVE");
            holding.setPurchaseDate(LocalDate.now());
        } else {
            // 使用第一个活跃持仓记录（假设每个基金只有一个活跃持仓）
            holding = holdings.get(0);
        }

        // 更新持仓信息
        holding.setCostPrice(averageCostPrice);
        holding.setHoldingAmount(netHoldingAmount);

        // 如果有当前价格，可以更新持仓市值，但这里不提供当前价格
        // holding.updateHoldingValue(currentPrice);

        holdingRepository.save(holding);

        // 将已确认的交易记录关联到此持仓
        List<FundTransactionRecord> confirmedTransactions = transactionRecordRepository
                .findByFundCodeAndStatus(fundCode, TransactionStatus.CONFIRMED);

        for (FundTransactionRecord transaction : confirmedTransactions) {
            if (transaction.getHoldingId() == null) {
                transaction.setHoldingId(holding.getId());
                transactionRecordRepository.save(transaction);
            }
        }

        return holding.getId();
    }

    @Override
    public int processPendingTransactions() {
        LocalDate now = LocalDate.now();
        List<FundTransactionRecord> pendingTransactions = transactionRecordRepository
                .findPendingTransactionsForConfirmation(now);

        int processedCount = 0;

        for (FundTransactionRecord transaction : pendingTransactions) {
            try {
                // 模拟确认：实际应用中需要从基金公司获取确认数据
                // 这里使用预期价格和份额作为实际值（简化处理）
                BigDecimal confirmedAmount = transaction.getAmount();
                BigDecimal actualPrice = transaction.getPrice();

                confirmTransaction(transaction.getId(), confirmedAmount, actualPrice);
                processedCount++;

                logger.info("交易确认成功: transactionId={}, fundCode={}",
                        transaction.getId(), transaction.getFundCode());
            } catch (Exception e) {
                logger.error("交易确认失败: transactionId={}, fundCode={}",
                        transaction.getId(), transaction.getFundCode(), e);
            }
        }

        return processedCount;
    }

    @Override
    public BigDecimal calculateAverageCost(String fundCode) {
        Optional<BigDecimal> avgPrice = transactionRecordRepository
                .calculateAverageBuyPriceByFundCode(fundCode);
        return avgPrice.orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal calculateTotalHoldingAmount(String fundCode) {
        BigDecimal totalBuy = transactionRecordRepository
                .sumBuyConfirmedAmountByFundCode(fundCode)
                .orElse(BigDecimal.ZERO);
        BigDecimal totalSell = transactionRecordRepository
                .sumSellConfirmedAmountByFundCode(fundCode)
                .orElse(BigDecimal.ZERO);
        return totalBuy.subtract(totalSell);
    }

    @Override
    public BigDecimal calculateTotalHoldingCost(String fundCode) {
        BigDecimal totalBuy = transactionRecordRepository
                .sumBuyTotalAmountByFundCode(fundCode)
                .orElse(BigDecimal.ZERO);
        BigDecimal totalSell = transactionRecordRepository
                .sumSellTotalAmountByFundCode(fundCode)
                .orElse(BigDecimal.ZERO);
        return totalBuy.subtract(totalSell);
    }

    @Override
    public List<FundTransactionRecord> getTransactionHistory(String fundCode, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return transactionRecordRepository.findByFundCode(fundCode);
        }
        return transactionRecordRepository.findByTransactionTimeBetween(startTime, endTime);
    }

    @Override
    public List<FundTransactionRecord> getPendingTransactions() {
        return transactionRecordRepository.findByStatus(TransactionStatus.PENDING);
    }

    @Override
    public List<FundTransactionRecord> getTransactionsByHoldingId(Long holdingId) {
        return transactionRecordRepository.findByHoldingId(holdingId);
    }

    @Override
    public int recalculateAllHoldings() {
        // 获取所有有交易记录的基金代码
        List<FundTransactionRecord> allTransactions = transactionRecordRepository.findAll();
        List<String> fundCodes = allTransactions.stream()
                .map(FundTransactionRecord::getFundCode)
                .distinct()
                .toList();

        int updatedCount = 0;
        for (String fundCode : fundCodes) {
            try {
                updateHoldingFromTransactions(fundCode);
                updatedCount++;
            } catch (Exception e) {
                logger.error("重新计算持仓失败: fundCode={}", fundCode, e);
            }
        }

        return updatedCount;
    }

    @Override
    public ValidationResult validateTransactionData(String fundCode) {
        ValidationResult result = new ValidationResult();
        result.setValid(true);
        result.setErrors(new ArrayList<>());
        result.setWarnings(new ArrayList<>());

        // 检查是否有待确认交易
        boolean hasPending = transactionRecordRepository.existsPendingTransactionByFundCode(fundCode);
        if (hasPending) {
            result.getWarnings().add("存在待确认的交易记录");
        }

        // 检查持仓份额是否与交易记录一致
        BigDecimal calculatedHolding = calculateTotalHoldingAmount(fundCode);
        List<FundHolding> holdings = holdingRepository.findByFundCodeAndStatus(fundCode, "ACTIVE");

        if (!holdings.isEmpty()) {
            FundHolding holding = holdings.get(0);
            BigDecimal holdingAmount = holding.getHoldingAmount();

            if (holdingAmount != null && calculatedHolding.compareTo(holdingAmount) != 0) {
                result.getErrors().add(String.format("持仓份额不一致: 持仓表=%s, 交易计算=%s",
                        holdingAmount, calculatedHolding));
                result.setValid(false);
            }
        }

        // 检查是否有未关联持仓的交易记录
        List<FundTransactionRecord> unassociatedTransactions = transactionRecordRepository
                .findByFundCodeAndStatus(fundCode, TransactionStatus.CONFIRMED)
                .stream()
                .filter(t -> t.getHoldingId() == null)
                .toList();

        if (!unassociatedTransactions.isEmpty()) {
            result.getWarnings().add(String.format("有%d条已确认交易未关联持仓", unassociatedTransactions.size()));
        }

        return result;
    }

    @Override
    public TransactionStatistics getTransactionStatistics(String fundCode) {
        TransactionStatistics stats = new TransactionStatistics();

        List<FundTransactionRecord> transactions;
        if (fundCode == null) {
            transactions = transactionRecordRepository.findAll();
        } else {
            transactions = transactionRecordRepository.findByFundCode(fundCode);
        }

        long total = transactions.size();
        long buyCount = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.BUY)
                .count();
        long sellCount = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.SELL)
                .count();

        BigDecimal totalBuyAmount = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.BUY && t.getTotalAmount() != null)
                .map(FundTransactionRecord::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSellAmount = transactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.SELL && t.getTotalAmount() != null)
                .map(FundTransactionRecord::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalFee = transactions.stream()
                .filter(t -> t.getFee() != null)
                .map(FundTransactionRecord::getFee)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingCount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.PENDING)
                .count();

        long confirmedCount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.CONFIRMED)
                .count();

        long cancelledCount = transactions.stream()
                .filter(t -> t.getStatus() == TransactionStatus.CANCELLED)
                .count();

        // 计算平均价格
        BigDecimal avgBuyPrice = buyCount > 0 ?
                totalBuyAmount.divide(BigDecimal.valueOf(buyCount), 4, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        BigDecimal avgSellPrice = sellCount > 0 ?
                totalSellAmount.divide(BigDecimal.valueOf(sellCount), 4, RoundingMode.HALF_UP) :
                BigDecimal.ZERO;

        stats.setTotalTransactions((int) total);
        stats.setBuyTransactions((int) buyCount);
        stats.setSellTransactions((int) sellCount);
        stats.setTotalBuyAmount(totalBuyAmount);
        stats.setTotalSellAmount(totalSellAmount);
        stats.setTotalFee(totalFee);
        stats.setAverageBuyPrice(avgBuyPrice);
        stats.setAverageSellPrice(avgSellPrice);
        stats.setPendingCount((int) pendingCount);
        stats.setConfirmedCount((int) confirmedCount);
        stats.setCancelledCount((int) cancelledCount);

        return stats;
    }

    @Override
    public boolean isTradeDay(LocalDate day) {
        // 非节假日 非周六周日
        return holidayCalendarService.isTradeDay(day);
    }
}