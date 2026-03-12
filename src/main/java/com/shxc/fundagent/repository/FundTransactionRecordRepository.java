package com.shxc.fundagent.repository;

import com.shxc.fundagent.entity.FundTransactionRecord;
import com.shxc.fundagent.enums.TransactionStatus;
import com.shxc.fundagent.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基金交易记录数据访问接口
 */
@Repository
public interface FundTransactionRecordRepository extends JpaRepository<FundTransactionRecord, Long> {

    /**
     * 根据基金代码查询交易记录
     */
    List<FundTransactionRecord> findByFundCode(String fundCode);

    @Query("select t from FundTransactionRecord t where t.fundCode = :fundCode and t.estimatedConfirmDate <= current date " +
            "order by t.estimatedConfirmDate desc")
    List<FundTransactionRecord> findActiveTransactionRecord(String fundCode);

    /**
     * 根据基金代码和交易类型查询交易记录
     */
    List<FundTransactionRecord> findByFundCodeAndTransactionType(String fundCode, TransactionType transactionType);

    /**
     * 根据基金代码和状态查询交易记录
     */
    List<FundTransactionRecord> findByFundCodeAndStatus(String fundCode, TransactionStatus status);

    /**
     * 根据交易状态查询交易记录
     */
    List<FundTransactionRecord> findByStatus(TransactionStatus status);

    /**
     * 根据交易时间范围查询交易记录
     */
    List<FundTransactionRecord> findByTransactionTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据预计确认时间查询待确认的交易记录
     */
    List<FundTransactionRecord> findByEstimatedConfirmDateBeforeAndStatus(LocalDateTime time, TransactionStatus status);

    /**
     * 查询指定持仓关联的交易记录
     */
    List<FundTransactionRecord> findByHoldingId(Long holdingId);

    /**
     * 统计指定基金的交易总金额（购买）
     */
    @Query("SELECT SUM(t.totalAmount) FROM FundTransactionRecord t WHERE t.fundCode = :fundCode " +
           "AND t.transactionType = com.shxc.fundagent.enums.TransactionType.BUY " +
           "AND t.status = com.shxc.fundagent.enums.TransactionStatus.CONFIRMED")
    Optional<BigDecimal> sumBuyTotalAmountByFundCode(@Param("fundCode") String fundCode);

    /**
     * 统计指定基金的交易总份额（购买）
     */
    @Query("SELECT SUM(t.confirmedAmount) FROM FundTransactionRecord t WHERE t.fundCode = :fundCode " +
           "AND t.transactionType = com.shxc.fundagent.enums.TransactionType.BUY " +
           "AND t.status = com.shxc.fundagent.enums.TransactionStatus.CONFIRMED")
    Optional<BigDecimal> sumBuyConfirmedAmountByFundCode(@Param("fundCode") String fundCode);

    /**
     * 统计指定基金的交易总金额（赎回）
     */
    @Query("SELECT SUM(t.totalAmount) FROM FundTransactionRecord t WHERE t.fundCode = :fundCode " +
           "AND t.transactionType = com.shxc.fundagent.enums.TransactionType.SELL " +
           "AND t.status = com.shxc.fundagent.enums.TransactionStatus.CONFIRMED")
    Optional<BigDecimal> sumSellTotalAmountByFundCode(@Param("fundCode") String fundCode);

    /**
     * 统计指定基金的交易总份额（赎回）
     */
    @Query("SELECT SUM(t.confirmedAmount) FROM FundTransactionRecord t WHERE t.fundCode = :fundCode " +
           "AND t.transactionType = com.shxc.fundagent.enums.TransactionType.SELL " +
           "AND t.status = com.shxc.fundagent.enums.TransactionStatus.CONFIRMED")
    Optional<BigDecimal> sumSellConfirmedAmountByFundCode(@Param("fundCode") String fundCode);

    /**
     * 计算指定基金的平均购买成本
     * 平均成本 = 总购买金额 / 总确认份额
     */
    @Query("SELECT SUM(t.totalAmount) / SUM(t.confirmedAmount) FROM FundTransactionRecord t " +
           "WHERE t.fundCode = :fundCode " +
           "AND t.transactionType = com.shxc.fundagent.enums.TransactionType.BUY " +
           "AND t.status = com.shxc.fundagent.enums.TransactionStatus.CONFIRMED")
    Optional<BigDecimal> calculateAverageBuyPriceByFundCode(@Param("fundCode") String fundCode);

    /**
     * 查询需要确认的交易记录（预计确认时间已到且状态为待确认）
     */
    @Query("SELECT t FROM FundTransactionRecord t WHERE t.estimatedConfirmDate <= :currentTime " +
           "AND t.status = com.shxc.fundagent.enums.TransactionStatus.PENDING")
    List<FundTransactionRecord> findPendingTransactionsForConfirmation(@Param("currentTime") LocalDate currentTime);

    /**
     * 查询指定日期范围内的交易记录
     */
    @Query("SELECT t FROM FundTransactionRecord t WHERE DATE(t.transactionTime) BETWEEN :startDate AND :endDate")
    List<FundTransactionRecord> findByTransactionDateBetween(@Param("startDate") java.time.LocalDate startDate,
                                                             @Param("endDate") java.time.LocalDate endDate);

    /**
     * 统计指定基金的总交易次数
     */
    @Query("SELECT COUNT(t) FROM FundTransactionRecord t WHERE t.fundCode = :fundCode " +
           "AND t.status = com.shxc.fundagent.enums.TransactionStatus.CONFIRMED")
    Long countConfirmedTransactionsByFundCode(@Param("fundCode") String fundCode);

    /**
     * 查询最新交易记录
     */
    @Query("SELECT t FROM FundTransactionRecord t WHERE t.fundCode = :fundCode " +
           "ORDER BY t.transactionTime DESC LIMIT 1")
    Optional<FundTransactionRecord> findLatestTransactionByFundCode(@Param("fundCode") String fundCode);

    /**
     * 查询指定持仓的成本明细（购买交易）
     */
    @Query("SELECT t FROM FundTransactionRecord t WHERE t.holdingId = :holdingId " +
           "AND t.transactionType = com.shxc.fundagent.enums.TransactionType.BUY " +
           "AND t.status = com.shxc.fundagent.enums.TransactionStatus.CONFIRMED " +
           "ORDER BY t.transactionTime")
    List<FundTransactionRecord> findBuyTransactionsByHoldingId(@Param("holdingId") Long holdingId);

    /**
     * 检查基金是否有未确认的交易
     */
    @Query("SELECT COUNT(t) > 0 FROM FundTransactionRecord t WHERE t.fundCode = :fundCode " +
           "AND t.status = com.shxc.fundagent.enums.TransactionStatus.PENDING")
    boolean existsPendingTransactionByFundCode(@Param("fundCode") String fundCode);
}