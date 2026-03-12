package com.shxc.fundagent.repository;

import com.shxc.fundagent.entity.FundStrategyLog;
import com.shxc.fundagent.enums.SuggestionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 策略日志数据访问接口
 */
@Repository
public interface FundStrategyLogRepository extends JpaRepository<FundStrategyLog, Long> {

    /**
     * 根据基金代码和交易日查询
     */
    Optional<FundStrategyLog> findByFundCodeAndTradeDate(String fundCode, LocalDate tradeDate);

    /**
     * 根据基金代码查询策略日志
     */
    List<FundStrategyLog> findByFundCodeOrderByTradeDateDesc(String fundCode);

    /**
     * 根据建议类型查询
     */
    List<FundStrategyLog> findBySuggestion(SuggestionType suggestion);

    /**
     * 根据建议类型和日期范围查询
     */
    List<FundStrategyLog> findBySuggestionAndTradeDateBetween(SuggestionType suggestion,
                                                               LocalDate startDate, LocalDate endDate);

    /**
     * 查询指定交易日的策略日志
     */
    List<FundStrategyLog> findByTradeDate(LocalDate tradeDate);

    /**
     * 查询指定日期范围的策略日志
     */
    List<FundStrategyLog> findByTradeDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * 根据是否已通知查询
     */
    List<FundStrategyLog> findByIsNotified(Boolean isNotified);

    /**
     * 根据是否已执行查询
     */
    List<FundStrategyLog> findByIsExecuted(Boolean isExecuted);

    /**
     * 查询未通知的策略日志
     */
    @Query("SELECT l FROM FundStrategyLog l WHERE l.isNotified = false")
    List<FundStrategyLog> findUnnotifiedLogs();

    /**
     * 查询未执行的策略日志
     */
    @Query("SELECT l FROM FundStrategyLog l WHERE l.isExecuted = false")
    List<FundStrategyLog> findUnexecutedLogs();

    /**
     * 统计各类建议的数量
     */
    @Query("SELECT l.suggestion, COUNT(l) FROM FundStrategyLog l GROUP BY l.suggestion")
    List<Object[]> countBySuggestion();

    /**
     * 统计指定基金的建议类型分布
     */
    @Query("SELECT l.suggestion, COUNT(l) FROM FundStrategyLog l " +
           "WHERE l.fundCode = :fundCode GROUP BY l.suggestion")
    List<Object[]> countBySuggestionAndFundCode(@Param("fundCode") String fundCode);

    /**
     * 查询最新策略建议
     */
    @Query("SELECT l FROM FundStrategyLog l WHERE l.fundCode = :fundCode " +
           "ORDER BY l.tradeDate DESC")
    Page<FundStrategyLog> findLatestByFundCode(@Param("fundCode") String fundCode, Pageable pageable);

    /**
     * 查询收益率超过阈值的策略日志
     */
    @Query("SELECT l FROM FundStrategyLog l WHERE l.yieldRate >= :minYield " +
           "OR l.yieldRate <= :maxNegativeYield")
    List<FundStrategyLog> findByYieldThreshold(@Param("minYield") Double minYield,
                                               @Param("maxNegativeYield") Double maxNegativeYield);

    /**
     * 查询日涨跌幅超过阈值的策略日志
     */
    @Query("SELECT l FROM FundStrategyLog l WHERE l.dailyChange <= :negativeThreshold " +
           "OR l.dailyChange >= :positiveThreshold")
    List<FundStrategyLog> findByDailyChangeThreshold(@Param("negativeThreshold") Double negativeThreshold,
                                                     @Param("positiveThreshold") Double positiveThreshold);

    /**
     * 更新通知状态
     */
    @Query("UPDATE FundStrategyLog l SET l.isNotified = :isNotified, l.notifyTime = :notifyTime " +
           "WHERE l.id = :id")
    int updateNotifyStatus(@Param("id") Long id,
                          @Param("isNotified") Boolean isNotified,
                          @Param("notifyTime") java.time.LocalDateTime notifyTime);

    /**
     * 更新执行状态
     */
    @Query("UPDATE FundStrategyLog l SET l.isExecuted = :isExecuted, l.executeTime = :executeTime, " +
           "l.executeResult = :executeResult WHERE l.id = :id")
    int updateExecuteStatus(@Param("id") Long id,
                           @Param("isExecuted") Boolean isExecuted,
                           @Param("executeTime") java.time.LocalDateTime executeTime,
                           @Param("executeResult") String executeResult);

    /**
     * 检查指定基金和日期是否存在策略日志
     */
    boolean existsByFundCodeAndTradeDate(String fundCode, LocalDate tradeDate);
}