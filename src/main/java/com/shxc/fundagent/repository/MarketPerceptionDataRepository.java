package com.shxc.fundagent.repository;

import com.shxc.fundagent.entity.MarketPerceptionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 市场感知数据Repository
 */
@Repository
public interface MarketPerceptionDataRepository extends JpaRepository<MarketPerceptionData, Long> {

    /**
     * 根据市场日期查询
     */
    Optional<MarketPerceptionData> findByMarketDate(LocalDate marketDate);

    /**
     * 查询日期范围内的数据
     */
    List<MarketPerceptionData> findByMarketDateBetweenOrderByMarketDateDesc(LocalDate startDate, LocalDate endDate);

    /**
     * 查询最新的市场感知数据
     */
    Optional<MarketPerceptionData> findTopByOrderByMarketDateDesc();

    /**
     * 根据数据来源查询
     */
    List<MarketPerceptionData> findByDataSourceOrderByMarketDateDesc(String dataSource);

    /**
     * 检查指定日期是否存在数据
     */
    boolean existsByMarketDate(LocalDate marketDate);

    /**
     * 查询有预警的数据
     */
    List<MarketPerceptionData> findByHasWarningTrueOrderByMarketDateDesc();

    /**
     * 查询情绪分数大于指定值的数据
     */
    @Query("SELECT m FROM MarketPerceptionData m WHERE m.sentimentScore >= :score ORDER BY m.marketDate DESC")
    List<MarketPerceptionData> findBySentimentScoreGreaterThanEqual(@Param("score") Double score);

    /**
     * 查询最新的N条数据（用于趋势分析）
     */
    @Query(value = "SELECT * FROM market_perception_data ORDER BY market_date DESC LIMIT :limit", nativeQuery = true)
    List<MarketPerceptionData> findLatestN(@Param("limit") int limit);
}
