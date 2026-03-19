package com.shxc.fundagent.agent.agents.impl.v2;

import com.shxc.fundagent.agent.core.AbstractAgentV2;
import com.shxc.fundagent.agent.capabilities.ToolResult;
import com.shxc.fundagent.agent.capabilities.MemoryManager.MemoryType;
import com.shxc.fundagent.agent.capabilities.tools.NewsCrawlerTool;
import com.shxc.fundagent.agent.model.v2.AgentContext;
import com.shxc.fundagent.agent.model.v2.NewsContext;
import com.shxc.fundagent.agent.model.AgentResult;
import com.shxc.fundagent.entity.NewsItem;
import com.shxc.fundagent.repository.NewsItemRepository;
import com.shxc.fundagent.service.NewsCrawlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 新闻资讯分析Agent
 * 功能：采集财经新闻，进行情感分析，提取关键事件，生成新闻摘要
 * 对应需求：FR-002 新闻资讯采集
 */
@Component
public class NewsAnalysisAgent extends AbstractAgentV2 {

    private static final Logger logger = LoggerFactory.getLogger(NewsAnalysisAgent.class);

    private static final String AGENT_NAME = "news_analysis_agent";
    private static final String AGENT_DESCRIPTION = "新闻资讯分析Agent，负责采集、分析和处理财经新闻资讯";

    private static final String[] CAPABILITIES = {
            "news_crawling",
            "sentiment_analysis",
            "event_extraction",
            "news_summarization",
            "impact_assessment"
    };

    private static final String[] SUPPORTED_CONTEXT_TYPES = {
            "news_crawling_request",
            "sentiment_analysis_request",
            "event_extraction_request",
            "news_summary_request"
    };

    private final NewsItemRepository newsItemRepository;
    private final NewsCrawlerTool newsCrawlerTool;
    private NewsCrawlerService newsCrawlerService; // 将在后续实现

    @Autowired
    public NewsAnalysisAgent(NewsItemRepository newsItemRepository, NewsCrawlerTool newsCrawlerTool) {
        super(AGENT_NAME, AGENT_DESCRIPTION, CAPABILITIES, SUPPORTED_CONTEXT_TYPES);
        this.newsItemRepository = newsItemRepository;
        this.newsCrawlerTool = newsCrawlerTool;

        // 初始化Agent
        initAgent();
    }

    /**
     * 设置NewsCrawlerService（可选注入）
     */
    @Autowired(required = false)
    public void setNewsCrawlerService(NewsCrawlerService newsCrawlerService) {
        this.newsCrawlerService = newsCrawlerService;
    }

    /**
     * 初始化Agent
     */
    private void initAgent() {
        logger.info("初始化新闻资讯分析Agent: {}", AGENT_NAME);

        // 设置默认输出模式
        addSupportedOutputSchema("news_summary");
        addSupportedOutputSchema("sentiment_analysis");
        addSupportedOutputSchema("event_timeline");
        addSupportedOutputSchema("investment_suggestions");

        // 注册Agent特有的工具（如果有）
        // 注意：通用的工具（如NewsCrawlerTool）已在EnhancedAgentManager中注册
    }

    @Override
    protected AgentResult doProcessWithTools(String task, AgentContext context) throws Exception {
        logger.info("新闻资讯分析Agent处理任务: {}", task);

        // 根据任务类型执行不同的处理逻辑
        try {
            if (task.contains("collect") || task.contains("crawl") || task.contains("采集")) {
                return collectNewsData(context);
            } else if (task.contains("analyze") || task.contains("sentiment") || task.contains("分析")) {
                return analyzeNewsSentiment(context);
            } else if (task.contains("extract") || task.contains("event") || task.contains("提取")) {
                return extractNewsEvents(context);
            } else if (task.contains("summarize") || task.contains("summary") || task.contains("摘要")) {
                return generateNewsSummary(context);
            } else if (task.contains("assess") || task.contains("impact") || task.contains("评估")) {
                return assessNewsImpact(context);
            } else if (task.contains("search") || task.contains("query") || task.contains("查询")) {
                return searchNews(context);
            } else {
                // 默认处理：采集和分析新闻
                return processDefaultNewsTask(context);
            }
        } catch (Exception e) {
            logger.error("新闻资讯分析Agent处理任务失败: {}", task, e);
            return buildErrorResult(e, 0);
        }
    }

    /**
     * 采集新闻数据
     */
    private AgentResult collectNewsData(AgentContext context) {
        long startTime = System.currentTimeMillis();
        logger.info("开始采集新闻数据");

        try {
            // 使用NewsCrawlerTool采集新闻
            Map<String, Object> toolParams = new HashMap<>();
            toolParams.put("categories", Arrays.asList("财经", "股票", "基金", "宏观经济"));
            toolParams.put("limit", 20);
            toolParams.put("timeRange", "today");

            ToolResult toolResult = newsCrawlerTool.execute(toolParams);

            if (!toolResult.isSuccess()) {
                return buildErrorResult(new RuntimeException("新闻采集失败: " + toolResult.getErrorMessage()), 0);
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> newsList = (List<Map<String, Object>>) toolResult.getData();

            int savedCount = 0;
            List<NewsItem> savedItems = new ArrayList<>();

            for (Map<String, Object> newsData : newsList) {
                try {
                    NewsItem newsItem = convertToNewsItem(newsData);
                    if (newsItem != null) {
                        // 检查是否已存在相同标题的新闻（避免重复）
                        List<NewsItem> existingNews = newsItemRepository.findTop10ByOrderByPublishTimeDesc();
                        final NewsItem finalNewsItem = newsItem; // 创建final副本用于lambda
                        boolean isDuplicate = existingNews.stream()
                                .anyMatch(item -> item.getTitle() != null &&
                                        item.getTitle().equals(finalNewsItem.getTitle()) &&
                                        item.getSource() != null &&
                                        item.getSource().equals(finalNewsItem.getSource()));

                        if (!isDuplicate) {
                            NewsItem savedNewsItem = newsItemRepository.save(newsItem);
                            savedItems.add(savedNewsItem);
                            savedCount++;
                        }
                    }
                } catch (Exception e) {
                    logger.warn("保存新闻项失败: {}", newsData.get("title"), e);
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 创建NewsContext
            NewsContext newsContext = createNewsContextFromItems(savedItems);

            // 存储到记忆
            storeMemory(
                String.format("采集了%d条新闻，保存了%d条。主要分类: %s",
                    newsList.size(), savedCount, getTopCategories(savedItems)),
                MemoryType.MID_TERM, 0.7,
                Map.of("label", "news_collection", "newsCount", savedCount));

            return buildSuccessResult(
                String.format("成功采集并保存了%d条新闻，耗时%dms", savedCount, duration),
                0.8,
                Map.of(
                    "newsCount", savedCount,
                    "durationMs", duration,
                    "newsContext", newsContext,
                    "newsItems", savedItems.stream()
                        .map(NewsItem::getBriefInfo)
                        .toList()
                ),
                "news_collection_report"
            );

        } catch (Exception e) {
            logger.error("采集新闻数据失败", e);
            return buildErrorResult(new RuntimeException("采集新闻数据失败: " + e.getMessage()), 0);
        }
    }

    /**
     * 分析新闻情感
     */
    private AgentResult analyzeNewsSentiment(AgentContext context) {
        long startTime = System.currentTimeMillis();
        logger.info("开始分析新闻情感");

        try {
            // 获取需要分析的新闻（最近24小时，未分析或情感评分为空）
            LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
            List<NewsItem> newsToAnalyze = newsItemRepository.findByPublishTimeBetween(yesterday, LocalDateTime.now());

            // 过滤出需要分析的新闻（情感为空或为0）
            List<NewsItem> unanalyzedNews = newsToAnalyze.stream()
                .filter(item -> item.getSentiment() == null ||
                        item.getSentimentScore() == null ||
                        item.getSentimentScore().compareTo(BigDecimal.ZERO) == 0)
                .limit(50) // 限制每次分析数量
                .toList();

            if (unanalyzedNews.isEmpty()) {
                return buildSuccessResult("没有需要情感分析的新闻", 0.9, Map.of(), "sentiment_analysis_report");
            }

            int analyzedCount = 0;
            Map<String, Integer> sentimentCount = new HashMap<>();

            for (NewsItem newsItem : unanalyzedNews) {
                try {
                    // 简单的情感分析逻辑（实际应使用NLP服务）
                    String sentiment = analyzeTextSentiment(newsItem.getTitle() + " " + newsItem.getSummary());
                    BigDecimal sentimentScore = calculateSentimentScore(sentiment);

                    newsItem.setSentiment(sentiment);
                    newsItem.setSentimentScore(sentimentScore);

                    // 根据情感和内容评估市场影响
                    if (newsItem.getMarketImpactScore() == null ||
                        newsItem.getMarketImpactScore().compareTo(BigDecimal.ZERO) == 0) {
                        BigDecimal impactScore = assessNewsImpactScore(newsItem);
                        newsItem.setMarketImpactScore(impactScore);
                        newsItem.setImpactDirection(impactScore.compareTo(BigDecimal.ZERO) > 0 ? "POSITIVE" :
                                                   impactScore.compareTo(BigDecimal.ZERO) < 0 ? "NEGATIVE" : "NEUTRAL");
                    }

                    newsItemRepository.save(newsItem);
                    analyzedCount++;

                    sentimentCount.merge(sentiment, 1, Integer::sum);

                } catch (Exception e) {
                    logger.warn("分析新闻情感失败: {}", newsItem.getTitle(), e);
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 创建情感分析报告
            Map<String, Object> analysisResult = new HashMap<>();
            analysisResult.put("analyzedCount", analyzedCount);
            analysisResult.put("totalNews", unanalyzedNews.size());
            analysisResult.put("sentimentDistribution", sentimentCount);
            analysisResult.put("overallSentiment", calculateOverallSentiment(sentimentCount));
            analysisResult.put("durationMs", duration);

            // 存储到记忆
            storeMemory(
                String.format("分析了%d条新闻的情感。情感分布: %s",
                    analyzedCount, sentimentCount),
                MemoryType.MID_TERM, 0.6,
                Map.of("label", "sentiment_analysis", "analyzedCount", analyzedCount, "sentimentCount", sentimentCount));

            return buildSuccessResult(
                String.format("成功分析了%d条新闻的情感，耗时%dms", analyzedCount, duration),
                0.75,
                analysisResult,
                "sentiment_analysis_report"
            );

        } catch (Exception e) {
            logger.error("分析新闻情感失败", e);
            return buildErrorResult(new RuntimeException("分析新闻情感失败: " + e.getMessage()), 0);
        }
    }

    /**
     * 提取新闻事件
     */
    private AgentResult extractNewsEvents(AgentContext context) {
        long startTime = System.currentTimeMillis();
        logger.info("开始提取新闻事件");

        try {
            // 获取最近的重要新闻（高重要性级别）
            List<NewsItem> importantNews = newsItemRepository.findByImportanceLevelGreaterThanEqual(5); // 5=HIGH及以上

            if (importantNews.isEmpty()) {
                return buildSuccessResult("没有重要新闻可以提取事件", 0.9, Map.of(), "event_extraction_report");
            }

            List<Map<String, Object>> extractedEvents = new ArrayList<>();
            int eventCount = 0;

            for (NewsItem newsItem : importantNews) {
                try {
                    // 简单的事件提取逻辑（实际应使用NLP服务）
                    Map<String, Object> event = extractEventFromNews(newsItem);
                    if (event != null && !event.isEmpty()) {
                        extractedEvents.add(event);
                        eventCount++;
                    }
                } catch (Exception e) {
                    logger.warn("提取新闻事件失败: {}", newsItem.getTitle(), e);
                }
            }

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 创建事件时间线
            List<Map<String, Object>> timelineEvents = createTimelineEvents(extractedEvents);

            // 存储到记忆
            storeMemory(
                String.format("从%d条重要新闻中提取了%d个事件", importantNews.size(), eventCount),
                MemoryType.MID_TERM, 0.7,
                Map.of("label", "event_extraction", "importantNewsCount", importantNews.size(), "eventCount", eventCount));

            return buildSuccessResult(
                String.format("成功提取了%d个新闻事件，耗时%dms", eventCount, duration),
                0.8,
                Map.of(
                    "eventCount", eventCount,
                    "extractedEvents", extractedEvents,
                    "timelineEvents", timelineEvents,
                    "durationMs", duration
                ),
                "event_extraction_report"
            );

        } catch (Exception e) {
            logger.error("提取新闻事件失败", e);
            return buildErrorResult(new RuntimeException("提取新闻事件失败: " + e.getMessage()), 0);
        }
    }

    /**
     * 生成新闻摘要
     */
    private AgentResult generateNewsSummary(AgentContext context) {
        long startTime = System.currentTimeMillis();
        logger.info("开始生成新闻摘要");

        try {
            // 获取最近24小时的新闻
            LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
            List<NewsItem> recentNews = newsItemRepository.findByPublishTimeBetween(yesterday, LocalDateTime.now());

            if (recentNews.isEmpty()) {
                return buildSuccessResult("没有最近的新闻可以生成摘要", 0.9, Map.of(), "news_summary_report");
            }

            // 创建NewsContext
            NewsContext newsContext = createNewsContextFromItems(recentNews);
            newsContext.updateSummary();

            // 分析热点话题
            List<NewsContext.Topic> hotTopics = identifyHotTopics(recentNews);

            // 生成投资建议
            List<NewsContext.InvestmentSuggestion> suggestions = generateInvestmentSuggestions(newsContext);

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            // 存储到记忆
            storeMemory(
                String.format("生成了新闻摘要，包含%d条新闻，识别了%d个热点话题",
                    recentNews.size(), hotTopics.size()),
                MemoryType.MID_TERM, 0.8,
                Map.of("label", "news_summary", "recentNewsCount", recentNews.size(), "hotTopicsCount", hotTopics.size()));

            return buildSuccessResult(
                String.format("成功生成新闻摘要，包含%d条新闻，%d个热点话题，耗时%dms",
                    recentNews.size(), hotTopics.size(), duration),
                0.85,
                Map.of(
                    "newsContext", newsContext,
                    "hotTopics", hotTopics,
                    "investmentSuggestions", suggestions,
                    "summary", newsContext.getBriefSummary(),
                    "durationMs", duration
                ),
                "news_summary_report"
            );

        } catch (Exception e) {
            logger.error("生成新闻摘要失败", e);
            return buildErrorResult(new RuntimeException("生成新闻摘要失败: " + e.getMessage()), 0);
        }
    }

    /**
     * 评估新闻影响
     */
    private AgentResult assessNewsImpact(AgentContext context) {
        // TODO: 实现新闻影响评估
        return buildSuccessResult("新闻影响评估功能待实现", 0.5, Map.of(), "impact_assessment_report");
    }

    /**
     * 搜索新闻
     */
    private AgentResult searchNews(AgentContext context) {
        // TODO: 实现新闻搜索功能
        return buildSuccessResult("新闻搜索功能待实现", 0.5, Map.of(), "news_search_report");
    }

    /**
     * 默认新闻处理任务
     */
    private AgentResult processDefaultNewsTask(AgentContext context) {
        logger.info("执行默认新闻处理任务");

        // 默认流程：采集 -> 分析 -> 摘要
        AgentResult collectionResult = collectNewsData(context);
        if (!collectionResult.isSuccess()) {
            return collectionResult;
        }

        AgentResult analysisResult = analyzeNewsSentiment(context);
        if (!analysisResult.isSuccess()) {
            return analysisResult;
        }

        AgentResult summaryResult = generateNewsSummary(context);

        // 合并结果
        return buildSuccessResult(
            "默认新闻处理流程完成：采集、情感分析、摘要生成",
            0.8,
            Map.of(
                "collection", collectionResult.getContent(),
                "sentimentAnalysis", analysisResult.getContent(),
                "summary", summaryResult.getContent()
            ),
            "default_news_processing_report"
        );
    }

    // ========== 辅助方法 ==========

    /**
     * 将采集的新闻数据转换为NewsItem实体
     */
    private NewsItem convertToNewsItem(Map<String, Object> newsData) {
        try {
            NewsItem newsItem = new NewsItem();

            newsItem.setTitle((String) newsData.getOrDefault("title", ""));
            newsItem.setSummary((String) newsData.getOrDefault("summary", ""));
            newsItem.setContent((String) newsData.getOrDefault("content", ""));
            newsItem.setSource((String) newsData.getOrDefault("source", "未知来源"));
            newsItem.setSourceUrl((String) newsData.getOrDefault("url", ""));
            newsItem.setAuthor((String) newsData.getOrDefault("author", ""));

            // 解析发布时间
            Object publishTimeObj = newsData.get("publishTime");
            if (publishTimeObj instanceof LocalDateTime) {
                newsItem.setPublishTime((LocalDateTime) publishTimeObj);
            } else if (publishTimeObj instanceof String) {
                // 简单解析（实际应使用更复杂的解析逻辑）
                newsItem.setPublishTime(LocalDateTime.now());
            } else {
                newsItem.setPublishTime(LocalDateTime.now());
            }

            // 设置新闻类型
            String category = (String) newsData.getOrDefault("category", "OTHER");
            newsItem.setNewsType(category.toUpperCase());

            // 设置重要性级别（基于简单规则）
            String title = newsItem.getTitle().toLowerCase();
            int importance = 3; // 默认中等

            if (title.contains("紧急") || title.contains("重大") || title.contains("突发") ||
                title.contains("危机") || title.contains("暴跌") || title.contains("暴涨")) {
                importance = 7; // 极高
            } else if (title.contains("重要") || title.contains("政策") || title.contains("利率") ||
                      title.contains("GDP") || title.contains("CPI") || title.contains("财报")) {
                importance = 5; // 高
            } else if (title.contains("分析") || title.contains("观点") || title.contains("评论")) {
                importance = 3; // 中等
            } else {
                importance = 1; // 低
            }

            newsItem.setImportanceLevel(importance);

            // 设置相关基金代码（如果有）
            @SuppressWarnings("unchecked")
            List<String> relatedFunds = (List<String>) newsData.getOrDefault("relatedFunds", new ArrayList<>());
            if (relatedFunds != null && !relatedFunds.isEmpty()) {
                newsItem.setRelatedFundCodes(relatedFunds);
            }

            // 设置关键词（从标题和摘要中提取简单关键词）
            List<String> keywords = extractKeywords(newsItem.getTitle() + " " + newsItem.getSummary());
            newsItem.setKeywords(keywords);

            newsItem.setCrawlTime(LocalDateTime.now());

            return newsItem;

        } catch (Exception e) {
            logger.warn("转换新闻数据失败: {}", newsData, e);
            return null;
        }
    }

    /**
     * 从新闻列表创建NewsContext
     */
    private NewsContext createNewsContextFromItems(List<NewsItem> newsItems) {
        NewsContext newsContext = new NewsContext();
        newsContext.setContextId("news_context_" + System.currentTimeMillis());
        newsContext.setCollectionTime(LocalDateTime.now());
        newsContext.setAnalysisTime(LocalDateTime.now());

        // 添加新闻项
        for (NewsItem item : newsItems) {
            newsContext.addNewsItem(item.toNewsContextItem());
        }

        // 更新摘要
        newsContext.updateSummary();

        return newsContext;
    }

    /**
     * 获取主要分类
     */
    private String getTopCategories(List<NewsItem> newsItems) {
        Map<String, Integer> categoryCount = new HashMap<>();
        for (NewsItem item : newsItems) {
            categoryCount.merge(item.getNewsType(), 1, Integer::sum);
        }

        return categoryCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(3)
                .map(entry -> entry.getKey() + "(" + entry.getValue() + ")")
                .reduce((a, b) -> a + ", " + b)
                .orElse("无");
    }

    /**
     * 分析文本情感（简单实现）
     */
    private String analyzeTextSentiment(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "NEUTRAL";
        }

        String lowerText = text.toLowerCase();

        // 正面关键词
        String[] positiveWords = {"上涨", "增长", "利好", "盈利", "收益", "突破", "创新高", "复苏", "扩张", "乐观"};
        // 负面关键词
        String[] negativeWords = {"下跌", "下滑", "亏损", "利空", "风险", "危机", "衰退", "收缩", "悲观", "暴跌"};

        int positiveCount = 0;
        int negativeCount = 0;

        for (String word : positiveWords) {
            if (lowerText.contains(word)) {
                positiveCount++;
            }
        }

        for (String word : negativeWords) {
            if (lowerText.contains(word)) {
                negativeCount++;
            }
        }

        if (positiveCount > negativeCount * 2) {
            return "VERY_POSITIVE";
        } else if (positiveCount > negativeCount) {
            return "POSITIVE";
        } else if (negativeCount > positiveCount * 2) {
            return "VERY_NEGATIVE";
        } else if (negativeCount > positiveCount) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }

    /**
     * 计算情感评分
     */
    private BigDecimal calculateSentimentScore(String sentiment) {
        switch (sentiment) {
            case "VERY_POSITIVE": return BigDecimal.valueOf(0.8);
            case "POSITIVE": return BigDecimal.valueOf(0.4);
            case "NEUTRAL": return BigDecimal.valueOf(0.0);
            case "NEGATIVE": return BigDecimal.valueOf(-0.4);
            case "VERY_NEGATIVE": return BigDecimal.valueOf(-0.8);
            default: return BigDecimal.ZERO;
        }
    }

    /**
     * 评估新闻影响评分
     */
    private BigDecimal assessNewsImpactScore(NewsItem newsItem) {
        BigDecimal baseScore = BigDecimal.ZERO;

        // 基于重要性级别
        if (newsItem.getImportanceLevel() != null) {
            int importance = newsItem.getImportanceLevel();
            if (importance >= 7) {
                baseScore = baseScore.add(BigDecimal.valueOf(0.3));
            } else if (importance >= 5) {
                baseScore = baseScore.add(BigDecimal.valueOf(0.2));
            } else if (importance >= 3) {
                baseScore = baseScore.add(BigDecimal.valueOf(0.1));
            }
        }

        // 基于情感评分
        if (newsItem.getSentimentScore() != null) {
            baseScore = baseScore.add(newsItem.getSentimentScore().multiply(BigDecimal.valueOf(0.5)));
        }

        // 基于相关基金数量
        if (newsItem.getRelatedFundCodes() != null) {
            int fundCount = newsItem.getRelatedFundCodes().size();
            if (fundCount > 5) {
                baseScore = baseScore.add(BigDecimal.valueOf(0.2));
            } else if (fundCount > 0) {
                baseScore = baseScore.add(BigDecimal.valueOf(0.1));
            }
        }

        // 限制在-1.0到1.0之间
        if (baseScore.compareTo(BigDecimal.ONE) > 0) {
            baseScore = BigDecimal.ONE;
        } else if (baseScore.compareTo(BigDecimal.ONE.negate()) < 0) {
            baseScore = BigDecimal.ONE.negate();
        }

        return baseScore;
    }

    /**
     * 计算整体情感
     */
    private String calculateOverallSentiment(Map<String, Integer> sentimentCount) {
        if (sentimentCount.isEmpty()) {
            return "NEUTRAL";
        }

        // 计算情感得分，忽略总计数，因为不需要它
        int positiveScore = sentimentCount.getOrDefault("VERY_POSITIVE", 0) * 2 +
                          sentimentCount.getOrDefault("POSITIVE", 0);
        int negativeScore = sentimentCount.getOrDefault("VERY_NEGATIVE", 0) * 2 +
                          sentimentCount.getOrDefault("NEGATIVE", 0);

        if (positiveScore > negativeScore * 2) {
            return "VERY_POSITIVE";
        } else if (positiveScore > negativeScore) {
            return "POSITIVE";
        } else if (negativeScore > positiveScore * 2) {
            return "VERY_NEGATIVE";
        } else if (negativeScore > positiveScore) {
            return "NEGATIVE";
        } else {
            return "NEUTRAL";
        }
    }

    /**
     * 从新闻中提取事件
     */
    private Map<String, Object> extractEventFromNews(NewsItem newsItem) {
        Map<String, Object> event = new HashMap<>();

        event.put("title", newsItem.getTitle());
        event.put("time", newsItem.getPublishTime());
        event.put("source", newsItem.getSource());
        event.put("importance", newsItem.getImportanceLevel());
        event.put("sentiment", newsItem.getSentiment());
        event.put("impact", newsItem.getMarketImpactScore());

        // 简单的事件类型识别
        String title = newsItem.getTitle().toLowerCase();
        if (title.contains("财报") || title.contains("业绩") || title.contains("盈利")) {
            event.put("type", "EARNINGS_REPORT");
        } else if (title.contains("政策") || title.contains("法规") || title.contains("监管")) {
            event.put("type", "POLICY_CHANGE");
        } else if (title.contains("并购") || title.contains("收购") || title.contains("合并")) {
            event.put("type", "MERGERS_ACQUISITIONS");
        } else if (title.contains("利率") || title.contains("加息") || title.contains("降息")) {
            event.put("type", "INTEREST_RATE");
        } else if (title.contains("数据") || title.contains("统计") || title.contains("指数")) {
            event.put("type", "ECONOMIC_DATA");
        } else {
            event.put("type", "GENERAL_NEWS");
        }

        return event;
    }

    /**
     * 创建时间线事件
     */
    private List<Map<String, Object>> createTimelineEvents(List<Map<String, Object>> extractedEvents) {
        return extractedEvents.stream()
                .sorted((e1, e2) -> {
                    LocalDateTime t1 = (LocalDateTime) e1.get("time");
                    LocalDateTime t2 = (LocalDateTime) e2.get("time");
                    return t2.compareTo(t1); // 降序
                })
                .map(event -> {
                    Map<String, Object> timelineEvent = new HashMap<>(event);
                    // 添加时间线特定字段
                    timelineEvent.put("timelineId", "event_" + System.currentTimeMillis() + "_" + event.hashCode());
                    return timelineEvent;
                })
                .toList();
    }

    /**
     * 识别热点话题
     */
    private List<NewsContext.Topic> identifyHotTopics(List<NewsItem> newsItems) {
        // 简单的热点话题识别（基于关键词频率）
        Map<String, Integer> keywordFrequency = new HashMap<>();

        for (NewsItem item : newsItems) {
            if (item.getKeywords() != null) {
                for (String keyword : item.getKeywords()) {
                    keywordFrequency.merge(keyword, 1, Integer::sum);
                }
            }
        }

        // 选择频率最高的前5个关键词作为热点话题
        return keywordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .map(entry -> {
                    NewsContext.Topic topic = new NewsContext.Topic();
                    topic.setTopicId("topic_" + entry.getKey().hashCode());
                    topic.setTopicName(entry.getKey());
                    topic.setHeatScore(BigDecimal.valueOf(entry.getValue() / (double) newsItems.size()));
                    // 收集相关新闻ID
                    List<String> relatedNewsIds = newsItems.stream()
                            .filter(item -> item.getKeywords() != null && item.getKeywords().contains(entry.getKey()))
                            .map(item -> String.valueOf(item.getId()))
                            .limit(10)
                            .toList();
                    topic.setRelatedNewsIds(relatedNewsIds);
                    return topic;
                })
                .toList();
    }

    /**
     * 生成投资建议
     */
    private List<NewsContext.InvestmentSuggestion> generateInvestmentSuggestions(NewsContext newsContext) {
        List<NewsContext.InvestmentSuggestion> suggestions = new ArrayList<>();

        // 简单的投资建议生成逻辑
        // 基于整体情感和市场影响

        BigDecimal overallSentimentScore = BigDecimal.ZERO;
        if (newsContext.getSentimentAnalysis() != null &&
            newsContext.getSentimentAnalysis().getOverallSentimentScore() != null) {
            overallSentimentScore = newsContext.getSentimentAnalysis().getOverallSentimentScore();
        }

        if (overallSentimentScore.compareTo(BigDecimal.valueOf(0.3)) > 0) {
            // 正面情感 -> 建议买入或持有
            NewsContext.InvestmentSuggestion suggestion = new NewsContext.InvestmentSuggestion();
            suggestion.setSuggestionId("suggestion_buy_" + System.currentTimeMillis());
            suggestion.setSuggestionType("BUY");
            suggestion.setDescription("市场情绪积极，建议关注优质资产");
            suggestion.setConfidence(BigDecimal.valueOf(0.7));
            suggestion.setRationale("基于新闻情感分析，市场整体呈现积极态势");
            suggestion.setTimeHorizon("SHORT_TERM");
            suggestions.add(suggestion);
        } else if (overallSentimentScore.compareTo(BigDecimal.valueOf(-0.3)) < 0) {
            // 负面情感 -> 建议谨慎或卖出
            NewsContext.InvestmentSuggestion suggestion = new NewsContext.InvestmentSuggestion();
            suggestion.setSuggestionId("suggestion_caution_" + System.currentTimeMillis());
            suggestion.setSuggestionType("HOLD");
            suggestion.setDescription("市场情绪谨慎，建议控制风险");
            suggestion.setConfidence(BigDecimal.valueOf(0.6));
            suggestion.setRationale("基于新闻情感分析，市场存在负面情绪");
            suggestion.setTimeHorizon("SHORT_TERM");
            suggestions.add(suggestion);
        }

        return suggestions;
    }

    /**
     * 提取关键词（简单实现）
     */
    private List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 中文关键词列表（财经领域）
        String[] financialKeywords = {
            "股票", "基金", "债券", "投资", "理财", "收益", "风险", "市场",
            "经济", "增长", "通胀", "利率", "政策", "监管", "财报", "业绩",
            "并购", "上市", "退市", "波动", "调整", "反弹", "下跌", "上涨",
            "央行", "证监会", "财政部", "宏观经济", "微观经济", "行业"
        };

        List<String> keywords = new ArrayList<>();
        String lowerText = text.toLowerCase();

        for (String keyword : financialKeywords) {
            if (lowerText.contains(keyword.toLowerCase())) {
                keywords.add(keyword);
            }
        }

        // 限制关键词数量
        return keywords.stream().distinct().limit(10).toList();
    }
}