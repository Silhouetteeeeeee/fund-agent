package com.shxc.fundagent.repository;

import com.shxc.fundagent.entity.FundDailyData;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 每日基金数据访问接口
 */
@Repository
public interface FundDailyDataRepository extends JpaRepository<FundDailyData, Long> {

    /**
     * 根据基金代码和交易日查询
     */
    Optional<FundDailyData> findByFundCodeAndTradeDate(String fundCode, LocalDate tradeDate);

    /**
     * 根据基金代码查询最新数据
     */
    @Query("SELECT d FROM FundDailyData d WHERE d.fundCode = :fundCode " +
           "ORDER BY d.tradeDate DESC LIMIT 1")
    Optional<FundDailyData> findLatestByFundCode(@Param("fundCode") String fundCode);

    /**
     * 根据基金代码查询历史数据（按日期倒序）
     */
    List<FundDailyData> findByFundCodeOrderByTradeDateDesc(String fundCode);

    /**
     * 根据基金代码和日期范围查询
     */
    List<FundDailyData> findByFundCodeAndTradeDateBetween(String fundCode, LocalDate startDate, LocalDate endDate);

    /**
     * 查询指定交易日的数据
     */
    List<FundDailyData> findByTradeDate(LocalDate tradeDate);

    /**
     * 查询指定交易日范围的基金数据
     */
    List<FundDailyData> findByTradeDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 查询指定基金的最新N条记录
     */
    @Query("SELECT d FROM FundDailyData d WHERE d.fundCode = :fundCode " +
           "ORDER BY d.tradeDate DESC")
    List<FundDailyData> findLatestByFundCode(@Param("fundCode") String fundCode, Pageable pageable);

    /**
     * 查询有净值数据的交易日
     */
    @Query("SELECT DISTINCT d.tradeDate FROM FundDailyData d WHERE d.netValue IS NOT NULL " +
           "ORDER BY d.tradeDate DESC")
    List<LocalDate> findTradeDatesWithNetValue(Pageable pageable);

    /**
     * 查询有估值数据的交易日
     */
    @Query("SELECT DISTINCT d.tradeDate FROM FundDailyData d WHERE d.estimateValue IS NOT NULL " +
           "ORDER BY d.tradeDate DESC")
    List<LocalDate> findTradeDatesWithEstimateValue(Pageable pageable);

    /**
     * 统计基金数据数量
     */
    @Query("SELECT COUNT(d) FROM FundDailyData d WHERE d.fundCode = :fundCode")
    long countByFundCode(@Param("fundCode") String fundCode);

    /**
     * 查询指定日期有数据的基金代码
     */
    @Query("SELECT DISTINCT d.fundCode FROM FundDailyData d WHERE d.tradeDate = :tradeDate")
    List<String> findFundCodesByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    /**
     * 查询指定日期有净值数据的基金代码
     */
    @Query("SELECT DISTINCT d.fundCode FROM FundDailyData d WHERE d.tradeDate = :tradeDate " +
           "AND d.netValue IS NOT NULL")
    List<String> findFundCodesWithNetValueByTradeDate(@Param("tradeDate") LocalDate tradeDate);

    /**
     * 删除指定日期之前的数据
     */
    @Query("DELETE FROM FundDailyData d WHERE d.tradeDate < :beforeDate")
    int deleteByTradeDateBefore(@Param("beforeDate") LocalDate beforeDate);

    /**
     * 检查指定基金和日期是否存在数据
     */
    boolean existsByFundCodeAndTradeDate(String fundCode, LocalDate tradeDate);

    /**
     * 查询最新交易日
     */
    @Query("SELECT MAX(d.tradeDate) FROM FundDailyData d")
    Optional<LocalDate> findLatestTradeDate();

}