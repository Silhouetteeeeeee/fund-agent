package com.shxc.fundagent.entity;

import com.shxc.fundagent.agent.model.v2.NewsContext;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 新闻资讯实体
 * 存储财经新闻、市场资讯等信息
 */
@Entity
@Table(name = "news_item")
@Data
public class NewsItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 新闻类型
     */
    @Column(name = "news_type", nullable = false, length = 20)
    private String newsType;

    /**
     * 新闻标题
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * 新闻摘要
     */
    @Column(name = "summary", length = 2000)
    private String summary;

    /**
     * 新闻内容
     */
    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    /**
     * 新闻来源
     */
    @Column(name = "source", length = 100)
    private String source;

    /**
     * 发布时间
     */
    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    /**
     * 重要性级别
     */
    @Column(name = "importance_level")
    private Integer importanceLevel;

    /**
     * 情感极性
     */
    @Column(name = "sentiment", length = 20)
    private String sentiment;

    /**
     * 精确情感评分 (-1.0到1.0)
     */
    @Column(name = "sentiment_score")
    private BigDecimal sentimentScore = BigDecimal.ZERO;

    /**
     * 相关基金代码列表 (JSON格式)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_fund_codes", columnDefinition = "json")
    private List<String> relatedFundCodes = new ArrayList<>();

    /**
     * 相关公司列表 (JSON格式)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "related_companies", columnDefinition = "json")
    private List<String> relatedCompanies = new ArrayList<>();

    /**
     * 关键词列表 (JSON格式)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "keywords", columnDefinition = "json")
    private List<String> keywords = new ArrayList<>();

    /**
     * 爬取时间
     */
    @CreationTimestamp
    @Column(name = "crawl_time")
    private LocalDateTime crawlTime;

    /**
     * 市场影响评分 (-1.0到1.0)
     */
    @Column(name = "market_impact_score")
    private BigDecimal marketImpactScore = BigDecimal.ZERO;

    /**
     * 影响方向: POSITIVE, NEGATIVE, NEUTRAL
     */
    @Column(name = "impact_direction", length = 20)
    private String impactDirection;

    /**
     * 事件类型
     */
    @Column(name = "event_type", length = 50)
    private String eventType;

    /**
     * 事件开始时间
     */
    @Column(name = "event_start_time")
    private LocalDateTime eventStartTime;

    /**
     * 事件结束时间
     */
    @Column(name = "event_end_time")
    private LocalDateTime eventEndTime;

    /**
     * 地点
     */
    @Column(name = "location", length = 200)
    private String location;

    /**
     * 作者
     */
    @Column(name = "author", length = 100)
    private String author;

    /**
     * 来源URL
     */
    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    /**
     * 元数据 (JSON格式)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 转换到NewsContext中的NewsItem模型
     */
    public NewsContext.NewsItem toNewsContextItem() {
        NewsContext.NewsItem item = new NewsContext.NewsItem();
        item.setNewsId(String.valueOf(this.id));
        item.setTitle(this.title);
        item.setSummary(this.summary);
        item.setContent(this.content);

        // 转换新闻类型为NewsCategory
        try {
            item.setCategory(NewsContext.NewsCategory.valueOf(this.newsType));
        } catch (IllegalArgumentException e) {
            item.setCategory(NewsContext.NewsCategory.OTHER);
        }

        // 设置重要性级别
        if (this.importanceLevel != null) {
            for (NewsContext.ImportanceLevel level : NewsContext.ImportanceLevel.values()) {
                if (level.getScore() == this.importanceLevel) {
                    item.setImportance(level);
                    break;
                }
            }
        }

        // 设置情感极性
        if (this.sentiment != null) {
            try {
                item.setSentiment(NewsContext.SentimentPolarity.valueOf(this.sentiment));
            } catch (IllegalArgumentException e) {
                item.setSentiment(NewsContext.SentimentPolarity.NEUTRAL);
            }
        }

        item.setSentimentScore(this.sentimentScore);
        item.setSourceName(this.source);
        item.setSourceUrl(this.sourceUrl);
        item.setAuthor(this.author);
        item.setPublishTime(this.publishTime);
        item.setCrawlTime(this.crawlTime);

        // 设置相关实体
        if (this.relatedCompanies != null) {
            item.setRelatedCompanies(this.relatedCompanies);
        }
        if (this.relatedFundCodes != null) {
            item.setRelatedFunds(this.relatedFundCodes);
        }
        if (this.keywords != null) {
            item.setKeywords(this.keywords);
            // 为每个关键词设置默认权重
            this.keywords.forEach(keyword ->
                item.getKeywordWeights().put(keyword, BigDecimal.valueOf(1.0))
            );
        }

        item.setEventType(this.eventType);
        item.setEventStartTime(this.eventStartTime);
        item.setEventEndTime(this.eventEndTime);
        item.setLocation(this.location);
        item.setMarketImpactScore(this.marketImpactScore);
        item.setImpactDirection(this.impactDirection);

        // 设置来源可信度（基于来源名称简单判断）
        if (this.source != null) {
            if (this.source.contains("新华社") || this.source.contains("人民日报") ||
                this.source.contains("央视") || this.source.contains("证监会")) {
                item.setSourceCredibility(NewsContext.SourceCredibility.VERY_HIGH);
            } else if (this.source.contains("证券时报") || this.source.contains("中国证券报") ||
                      this.source.contains("上海证券报") || this.source.contains("经济日报")) {
                item.setSourceCredibility(NewsContext.SourceCredibility.HIGH);
            } else if (this.source.contains("新浪") || this.source.contains("搜狐") ||
                      this.source.contains("网易") || this.source.contains("腾讯")) {
                item.setSourceCredibility(NewsContext.SourceCredibility.MEDIUM);
            } else {
                item.setSourceCredibility(NewsContext.SourceCredibility.LOW);
            }
        }

        if (this.metadata != null) {
            item.setMetadata(this.metadata);
        }

        return item;
    }

    /**
     * 从NewsContext的NewsItem创建实体
     */
    public static NewsItem fromNewsContextItem(NewsContext.NewsItem contextItem) {
        NewsItem entity = new NewsItem();
        entity.setTitle(contextItem.getTitle());
        entity.setSummary(contextItem.getSummary());
        entity.setContent(contextItem.getContent());

        if (contextItem.getCategory() != null) {
            entity.setNewsType(contextItem.getCategory().name());
        }

        if (contextItem.getImportance() != null) {
            entity.setImportanceLevel(contextItem.getImportance().getScore());
        }

        if (contextItem.getSentiment() != null) {
            entity.setSentiment(contextItem.getSentiment().name());
        }

        entity.setSentimentScore(contextItem.getSentimentScore());
        entity.setSource(contextItem.getSourceName());
        entity.setSourceUrl(contextItem.getSourceUrl());
        entity.setAuthor(contextItem.getAuthor());
        entity.setPublishTime(contextItem.getPublishTime());
        entity.setCrawlTime(contextItem.getCrawlTime());

        if (contextItem.getRelatedCompanies() != null) {
            entity.setRelatedCompanies(contextItem.getRelatedCompanies());
        }

        if (contextItem.getRelatedFunds() != null) {
            entity.setRelatedFundCodes(contextItem.getRelatedFunds());
        }

        if (contextItem.getKeywords() != null) {
            entity.setKeywords(contextItem.getKeywords());
        }

        entity.setEventType(contextItem.getEventType());
        entity.setEventStartTime(contextItem.getEventStartTime());
        entity.setEventEndTime(contextItem.getEventEndTime());
        entity.setLocation(contextItem.getLocation());
        entity.setMarketImpactScore(contextItem.getMarketImpactScore());
        entity.setImpactDirection(contextItem.getImpactDirection());

        if (contextItem.getMetadata() != null) {
            entity.setMetadata(contextItem.getMetadata());
        }

        return entity;
    }

    /**
     * 获取简要信息
     */
    public String getBriefInfo() {
        return String.format("[%s] %s (来源: %s, 发布时间: %s, 情感: %s)",
                newsType, title, source,
                publishTime != null ? publishTime.toLocalDate().toString() : "未知",
                sentiment != null ? sentiment : "未知");
    }
}