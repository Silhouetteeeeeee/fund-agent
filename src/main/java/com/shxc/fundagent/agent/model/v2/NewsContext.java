package com.shxc.fundagent.agent.model.v2;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 新闻资讯上下文
 * 包含财经新闻、市场资讯、情感分析等信息
 */
@Data
public class NewsContext {

    /**
     * 新闻分类
     */
    public enum NewsCategory {
        /** 宏观经济 */
        MACRO_ECONOMY,
        /** 行业动态 */
        INDUSTRY_NEWS,
        /** 公司新闻 */
        COMPANY_NEWS,
        /** 基金资讯 */
        FUND_NEWS,
        /** 股票市场 */
        STOCK_MARKET,
        /** 债券市场 */
        BOND_MARKET,
        /** 政策法规 */
        POLICY_REGULATION,
        /** 国际新闻 */
        INTERNATIONAL,
        /** 投资策略 */
        INVESTMENT_STRATEGY,
        /** 其他 */
        OTHER
    }

    /**
     * 新闻重要性级别
     */
    public enum ImportanceLevel {
        /** 低重要性 */
        LOW(1),
        /** 中等重要性 */
        MEDIUM(3),
        /** 高重要性 */
        HIGH(5),
        /** 极高重要性 */
        CRITICAL(7);

        private final int score;

        ImportanceLevel(int score) {
            this.score = score;
        }

        public int getScore() {
            return score;
        }
    }

    /**
     * 情感极性
     */
    public enum SentimentPolarity {
        /** 非常负面 */
        VERY_NEGATIVE(-0.8),
        /** 负面 */
        NEGATIVE(-0.4),
        /** 中性 */
        NEUTRAL(0.0),
        /** 正面 */
        POSITIVE(0.4),
        /** 非常正面 */
        VERY_POSITIVE(0.8);

        private final double score;

        SentimentPolarity(double score) {
            this.score = score;
        }

        public double getScore() {
            return score;
        }
    }

    /**
     * 新闻来源可信度
     */
    public enum SourceCredibility {
        /** 低可信度 */
        LOW(0.3),
        /** 中等可信度 */
        MEDIUM(0.6),
        /** 高可信度 */
        HIGH(0.8),
        /** 极高可信度 */
        VERY_HIGH(0.95);

        private final double score;

        SourceCredibility(double score) {
            this.score = score;
        }

        public double getScore() {
            return score;
        }
    }

    // 基本信息
    private String contextId;
    private LocalDateTime collectionTime;
    private LocalDateTime analysisTime;

    // 新闻摘要统计
    private NewsSummary summary;

    // 新闻列表
    private List<NewsItem> newsItems = new ArrayList<>();

    // 按分类分组的新闻
    private Map<NewsCategory, List<NewsItem>> newsByCategory = new HashMap<>();

    // 情感分析结果
    private SentimentAnalysis sentimentAnalysis;

    // 热点话题
    private List<Topic> hotTopics = new ArrayList<>();

    // 关键事件时间线
    private List<TimelineEvent> timelineEvents = new ArrayList<>();

    // 市场影响评估
    private MarketImpactAssessment marketImpact;

    // 投资建议
    private List<InvestmentSuggestion> investmentSuggestions = new ArrayList<>();

    // 数据来源和元数据
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 新闻项
     */
    @Data
    public static class NewsItem {
        private String newsId;
        private String title;
        private String summary;
        private String content;
        private NewsCategory category;
        private List<NewsCategory> subCategories = new ArrayList<>();
        private ImportanceLevel importance = ImportanceLevel.MEDIUM;
        private SentimentPolarity sentiment = SentimentPolarity.NEUTRAL;
        private BigDecimal sentimentScore = BigDecimal.ZERO; // 精确情感评分 (-1.0到1.0)

        // 来源信息
        private String sourceName;
        private String sourceUrl;
        private SourceCredibility sourceCredibility = SourceCredibility.MEDIUM;
        private String author;
        private LocalDateTime publishTime;
        private LocalDateTime crawlTime;

        // 相关实体
        private List<String> relatedCompanies = new ArrayList<>();
        private List<String> relatedFunds = new ArrayList<>();
        private List<String> relatedStocks = new ArrayList<>();
        private List<String> relatedIndustries = new ArrayList<>();

        // 关键词
        private List<String> keywords = new ArrayList<>();
        private Map<String, BigDecimal> keywordWeights = new HashMap<>();

        // 事件信息
        private String eventType;
        private LocalDateTime eventStartTime;
        private LocalDateTime eventEndTime;
        private String location;

        // 影响评估
        private BigDecimal marketImpactScore = BigDecimal.ZERO; // 市场影响评分 (-1.0到1.0)
        private String impactDirection; // POSITIVE, NEGATIVE, NEUTRAL
        private Map<String, Object> impactDetails = new HashMap<>();

        // 元数据
        private Map<String, Object> metadata = new HashMap<>();

        /**
         * 添加相关公司
         */
        public void addRelatedCompany(String company) {
            if (company != null && !company.trim().isEmpty()) {
                this.relatedCompanies.add(company);
            }
        }

        /**
         * 添加相关基金
         */
        public void addRelatedFund(String fundCode) {
            if (fundCode != null && !fundCode.trim().isEmpty()) {
                this.relatedFunds.add(fundCode);
            }
        }

        /**
         * 添加关键词
         */
        public void addKeyword(String keyword, BigDecimal weight) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                this.keywords.add(keyword);
                if (weight != null) {
                    this.keywordWeights.put(keyword, weight);
                }
            }
        }

        /**
         * 获取新闻简要信息
         */
        public String getBriefInfo() {
            return String.format("[%s] %s (来源: %s, 重要性: %s, 情感: %s)",
                    category, title, sourceName, importance, sentiment);
        }
    }

    /**
     * 新闻摘要统计
     */
    @Data
    public static class NewsSummary {
        private int totalNewsCount;
        private Map<NewsCategory, Integer> countByCategory = new HashMap<>();
        private Map<ImportanceLevel, Integer> countByImportance = new HashMap<>();
        private Map<SentimentPolarity, Integer> countBySentiment = new HashMap<>();

        private LocalDateTime earliestNewsTime;
        private LocalDateTime latestNewsTime;

        // 关键词统计
        private Map<String, Integer> keywordFrequency = new HashMap<>();
        private List<String> topKeywords = new ArrayList<>();

        // 来源统计
        private Map<String, Integer> countBySource = new HashMap<>();
        private List<String> topSources = new ArrayList<>();

        // 更新统计信息
        public void updateSummary(List<NewsItem> newsItems) {
            this.totalNewsCount = newsItems.size();
            this.countByCategory.clear();
            this.countByImportance.clear();
            this.countBySentiment.clear();
            this.keywordFrequency.clear();
            this.countBySource.clear();

            for (NewsItem news : newsItems) {
                // 按分类统计
                countByCategory.merge(news.getCategory(), 1, Integer::sum);

                // 按重要性统计
                countByImportance.merge(news.getImportance(), 1, Integer::sum);

                // 按情感统计
                countBySentiment.merge(news.getSentiment(), 1, Integer::sum);

                // 关键词频率统计
                if (news.getKeywords() != null) {
                    for (String keyword : news.getKeywords()) {
                        keywordFrequency.merge(keyword, 1, Integer::sum);
                    }
                }

                // 来源统计
                if (news.getSourceName() != null) {
                    countBySource.merge(news.getSourceName(), 1, Integer::sum);
                }

                // 更新最早和最晚时间
                if (news.getPublishTime() != null) {
                    if (earliestNewsTime == null || news.getPublishTime().isBefore(earliestNewsTime)) {
                        earliestNewsTime = news.getPublishTime();
                    }
                    if (latestNewsTime == null || news.getPublishTime().isAfter(latestNewsTime)) {
                        latestNewsTime = news.getPublishTime();
                    }
                }
            }

            // 计算热门关键词（前10）
            topKeywords = keywordFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .map(Map.Entry::getKey)
                    .toList();

            // 计算热门来源（前5）
            topSources = countBySource.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .toList();
        }
    }

    /**
     * 情感分析结果
     */
    @Data
    public static class SentimentAnalysis {
        // 总体情感
        private BigDecimal overallSentimentScore = BigDecimal.ZERO;
        private SentimentPolarity overallSentiment = SentimentPolarity.NEUTRAL;

        // 按分类的情感分析
        private Map<NewsCategory, BigDecimal> sentimentByCategory = new HashMap<>();

        // 情感趋势
        private List<SentimentTrendPoint> sentimentTrend = new ArrayList<>();

        // 情感关键词
        private Map<String, BigDecimal> positiveKeywords = new HashMap<>();
        private Map<String, BigDecimal> negativeKeywords = new HashMap<>();

        // 情感强度
        private BigDecimal sentimentIntensity = BigDecimal.ZERO; // 情感强度 (0.0-1.0)
        private BigDecimal sentimentVolatility = BigDecimal.ZERO; // 情感波动性 (0.0-1.0)

        /**
         * 情感趋势点
         */
        @Data
        public static class SentimentTrendPoint {
            private LocalDateTime time;
            private BigDecimal sentimentScore;
            private int newsCount;
        }
    }

    /**
     * 热点话题
     */
    @Data
    public static class Topic {
        private String topicId;
        private String topicName;
        private List<String> keywords = new ArrayList<>();
        private BigDecimal heatScore = BigDecimal.ZERO; // 热度评分 (0.0-1.0)
        private BigDecimal sentimentScore = BigDecimal.ZERO; // 话题情感评分 (-1.0到1.0)
        private List<String> relatedNewsIds = new ArrayList<>();
        private List<String> relatedEntities = new ArrayList<>();
        private LocalDateTime firstAppeared;
        private LocalDateTime lastUpdated;
        private String trend; // 趋势：RISING, STABLE, DECLINING

        // 话题发展脉络
        private List<TopicDevelopment> developments = new ArrayList<>();

        /**
         * 话题发展节点
         */
        @Data
        public static class TopicDevelopment {
            private LocalDateTime time;
            private String event;
            private BigDecimal impactScore = BigDecimal.ZERO;
        }
    }

    /**
     * 时间线事件
     */
    @Data
    public static class TimelineEvent {
        private String eventId;
        private String eventName;
        private String description;
        private LocalDateTime eventTime;
        private ImportanceLevel importance;
        private SentimentPolarity sentiment;
        private List<String> relatedNewsIds = new ArrayList<>();
        private List<String> relatedEntities = new ArrayList<>();
        private BigDecimal marketImpactScore = BigDecimal.ZERO;
    }

    /**
     * 市场影响评估
     */
    @Data
    public static class MarketImpactAssessment {
        private BigDecimal overallImpactScore = BigDecimal.ZERO;
        private String overallImpactDirection; // POSITIVE, NEGATIVE, NEUTRAL

        // 按市场板块的影响
        private Map<String, BigDecimal> impactBySector = new HashMap<>();

        // 按资产类别的影响
        private Map<String, BigDecimal> impactByAssetClass = new HashMap<>();

        // 影响时间范围
        private String impactDuration; // SHORT_TERM, MEDIUM_TERM, LONG_TERM

        // 风险评估
        private String riskLevel; // LOW, MEDIUM, HIGH
        private List<String> riskFactors = new ArrayList<>();

        // 机会识别
        private List<InvestmentOpportunity> opportunities = new ArrayList<>();

        /**
         * 投资机会
         */
        @Data
        public static class InvestmentOpportunity {
            private String opportunityId;
            private String description;
            private String assetClass; // STOCK, BOND, FUND, COMMODITY
            private List<String> relatedAssets = new ArrayList<>();
            private BigDecimal potentialReturn = BigDecimal.ZERO;
            private BigDecimal riskLevel = BigDecimal.ZERO;
            private String timeHorizon; // IMMEDIATE, SHORT_TERM, MEDIUM_TERM, LONG_TERM
            private BigDecimal confidence = BigDecimal.ZERO;
        }
    }

    /**
     * 投资建议
     */
    @Data
    public static class InvestmentSuggestion {
        private String suggestionId;
        private String suggestionType; // BUY, SELL, HOLD, ADJUST
        private String description;
        private List<String> targetAssets = new ArrayList<>();
        private BigDecimal confidence = BigDecimal.ZERO;
        private String rationale;
        private String timeHorizon;
        private BigDecimal expectedImpact = BigDecimal.ZERO;
        private List<String> supportingNewsIds = new ArrayList<>();
        private LocalDateTime suggestedTime;
    }

    /**
     * 添加新闻项
     */
    public void addNewsItem(NewsItem newsItem) {
        if (newsItem != null) {
            this.newsItems.add(newsItem);

            // 更新按分类的分组
            NewsCategory category = newsItem.getCategory();
            newsByCategory.computeIfAbsent(category, k -> new ArrayList<>()).add(newsItem);

            // 如果有子分类，也添加到相应分组
            if (newsItem.getSubCategories() != null) {
                for (NewsCategory subCategory : newsItem.getSubCategories()) {
                    newsByCategory.computeIfAbsent(subCategory, k -> new ArrayList<>()).add(newsItem);
                }
            }
        }
    }

    /**
     * 获取特定分类的新闻
     */
    public List<NewsItem> getNewsByCategory(NewsCategory category) {
        return newsByCategory.getOrDefault(category, new ArrayList<>());
    }

    /**
     * 获取特定重要性级别的新闻
     */
    public List<NewsItem> getNewsByImportance(ImportanceLevel importanceLevel) {
        return newsItems.stream()
                .filter(news -> news.getImportance() == importanceLevel)
                .toList();
    }

    /**
     * 获取特定情感的新闻
     */
    public List<NewsItem> getNewsBySentiment(SentimentPolarity sentiment) {
        return newsItems.stream()
                .filter(news -> news.getSentiment() == sentiment)
                .toList();
    }

    /**
     * 获取与特定基金相关的新闻
     */
    public List<NewsItem> getNewsRelatedToFund(String fundCode) {
        return newsItems.stream()
                .filter(news -> news.getRelatedFunds() != null && news.getRelatedFunds().contains(fundCode))
                .toList();
    }

    /**
     * 获取与特定公司相关的新闻
     */
    public List<NewsItem> getNewsRelatedToCompany(String company) {
        return newsItems.stream()
                .filter(news -> news.getRelatedCompanies() != null && news.getRelatedCompanies().contains(company))
                .toList();
    }

    /**
     * 更新新闻摘要统计
     */
    public void updateSummary() {
        if (summary == null) {
            summary = new NewsSummary();
        }
        summary.updateSummary(newsItems);
    }

    /**
     * 获取简要的新闻摘要
     */
    public String getBriefSummary() {
        if (summary == null) {
            updateSummary();
        }

        return String.format("新闻总数: %d, 分类: %s, 总体情感: %s, 热门关键词: %s",
                summary.getTotalNewsCount(),
                summary.getCountByCategory(),
                sentimentAnalysis != null ? sentimentAnalysis.getOverallSentiment() : "N/A",
                summary.getTopKeywords().stream().limit(3).toList());
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (key != null && value != null) {
            this.metadata.put(key, value);
        }
    }
}