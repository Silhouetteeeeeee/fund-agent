package com.shxc.fundagent.repository;

import com.shxc.fundagent.entity.NewsItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 新闻资讯数据访问接口
 */
@Repository
public interface NewsItemRepository extends JpaRepository<NewsItem, Long> {

    /**
     * 根据新闻类型查找新闻
     */
    List<NewsItem> findByNewsType(String newsType);

    /**
     * 根据新闻类型查找新闻（分页）
     */
    Page<NewsItem> findByNewsType(String newsType, Pageable pageable);

    /**
     * 根据发布时间范围查找新闻
     */
    List<NewsItem> findByPublishTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据发布时间范围查找新闻（分页）
     */
    Page<NewsItem> findByPublishTimeBetween(LocalDateTime startTime, LocalDateTime endTime, Pageable pageable);

    /**
     * 根据情感极性查找新闻
     */
    List<NewsItem> findBySentiment(String sentiment);

    /**
     * 根据重要性级别查找新闻
     */
    List<NewsItem> findByImportanceLevel(Integer importanceLevel);

    /**
     * 根据重要性级别查找新闻（大于等于指定级别）
     */
    List<NewsItem> findByImportanceLevelGreaterThanEqual(Integer minImportanceLevel);

    /**
     * 查找包含指定关键词的新闻
     * 使用JSON_CONTAINS函数（MySQL）
     */
    @Query(value = "SELECT * FROM news_item WHERE JSON_CONTAINS(keywords, :keyword)", nativeQuery = true)
    List<NewsItem> findByKeyword(@Param("keyword") String keyword);

    /**
     * 查找与指定基金相关的新闻
     * 使用JSON_CONTAINS函数（MySQL）
     */
    @Query(value = "SELECT * FROM news_item WHERE JSON_CONTAINS(related_fund_codes, :fundCode)", nativeQuery = true)
    List<NewsItem> findByRelatedFundCode(@Param("fundCode") String fundCode);

    /**
     * 查找与指定公司相关的新闻
     * 使用JSON_CONTAINS函数（MySQL）
     */
    @Query(value = "SELECT * FROM news_item WHERE JSON_CONTAINS(related_companies, :company)", nativeQuery = true)
    List<NewsItem> findByRelatedCompany(@Param("company") String company);

    /**
     * 根据新闻类型和发布时间范围查找新闻
     */
    List<NewsItem> findByNewsTypeAndPublishTimeBetween(String newsType, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据情感极性和发布时间范围查找新闻
     */
    List<NewsItem> findBySentimentAndPublishTimeBetween(String sentiment, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找最新的新闻（按发布时间倒序）
     */
    List<NewsItem> findTop10ByOrderByPublishTimeDesc();

    /**
     * 查找最重要的新闻（按重要性级别倒序，发布时间倒序）
     */
    List<NewsItem> findTop10ByOrderByImportanceLevelDescPublishTimeDesc();

    /**
     * 查找市场影响最大的新闻（按市场影响评分倒序）
     */
    List<NewsItem> findTop10ByOrderByMarketImpactScoreDescPublishTimeDesc();

    /**
     * 根据新闻类型查找最新的新闻
     */
    List<NewsItem> findTop10ByNewsTypeOrderByPublishTimeDesc(String newsType);

    /**
     * 统计指定时间范围内的新闻数量
     */
    Long countByPublishTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计指定新闻类型的数量
     */
    Long countByNewsType(String newsType);

    /**
     * 统计指定情感极性的新闻数量
     */
    Long countBySentiment(String sentiment);

    /**
     * 删除过期的新闻（早于指定时间）
     */
    void deleteByPublishTimeBefore(LocalDateTime cutoffTime);

    /**
     * 查找未分析的新闻（情感评分为空或为0）
     */
    List<NewsItem> findBySentimentIsNullOrSentimentScoreIsNullOrSentimentScore(BigDecimal zeroScore);

    /**
     * 批量更新新闻的情感分析结果
     */
    @Modifying
    @Query("UPDATE NewsItem n SET n.sentiment = :sentiment, n.sentimentScore = :sentimentScore, " +
           "n.marketImpactScore = :marketImpactScore, n.impactDirection = :impactDirection " +
           "WHERE n.id IN :ids")
    void updateSentimentAnalysis(@Param("ids") List<Long> ids,
                                 @Param("sentiment") String sentiment,
                                 @Param("sentimentScore") BigDecimal sentimentScore,
                                 @Param("marketImpactScore") BigDecimal marketImpactScore,
                                 @Param("impactDirection") String impactDirection);
}