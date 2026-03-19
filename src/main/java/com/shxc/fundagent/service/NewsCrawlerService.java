package com.shxc.fundagent.service;

import com.shxc.fundagent.entity.NewsItem;
import com.shxc.fundagent.repository.NewsItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 新闻爬虫服务
 * 用于采集财经新闻数据
 */
@Service
public class NewsCrawlerService {

    private static final Logger logger = LoggerFactory.getLogger(NewsCrawlerService.class);

    private final NewsItemRepository newsItemRepository;

    @Autowired
    public NewsCrawlerService(NewsItemRepository newsItemRepository) {
        this.newsItemRepository = newsItemRepository;
    }

    /**
     * 采集新闻数据
     * @param categories 新闻分类
     * @param limit 采集数量限制
     * @param timeRange 时间范围
     * @return 新闻数据列表
     */
    @Transactional
    public List<NewsItem> crawlNews(List<String> categories, int limit, String timeRange) {
        logger.info("开始采集新闻数据，分类: {}, 限制: {}, 时间范围: {}", categories, limit, timeRange);

        try {
            // 在实际实现中，这里应该调用外部新闻API
            // 现在使用模拟数据，模拟从外部API获取新闻
            List<NewsItem> mockNewsItems = generateMockNewsItems(categories, limit);

            // 过滤重复新闻（基于标题和发布时间）
            List<NewsItem> filteredNews = filterDuplicateNews(mockNewsItems);

            // 保存到数据库
            List<NewsItem> savedItems = newsItemRepository.saveAll(filteredNews);
            logger.info("成功采集并保存 {} 条新闻数据", savedItems.size());

            return savedItems;

        } catch (Exception e) {
            logger.error("新闻采集失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 从外部新闻源采集新闻
     */
    @Transactional
    public List<NewsItem> crawlFromExternalSources(List<String> sources, int limit) {
        logger.info("从外部新闻源采集新闻，来源: {}, 限制: {}", sources, limit);

        // 在实际实现中，这里应该集成多个新闻源API
        // 目前返回空列表，表示没有实现外部集成
        logger.warn("外部新闻源集成尚未实现，返回空列表");
        return Collections.emptyList();
    }

    /**
     * 批量分析新闻情感
     */
    @Transactional
    public int analyzeNewsSentiment(List<Long> newsIds) {
        if (newsIds == null || newsIds.isEmpty()) {
            logger.info("没有需要分析情感的新闻");
            return 0;
        }

        logger.info("开始分析 {} 条新闻的情感", newsIds.size());
        int analyzedCount = 0;

        // 获取需要分析的新闻
        List<NewsItem> newsItems = newsItemRepository.findAllById(newsIds);

        for (NewsItem newsItem : newsItems) {
            try {
                // 简单的情感分析（模拟）
                // 在实际实现中，这里应该调用NLP服务或LLM
                analyzeSingleNewsSentiment(newsItem);

                // 更新新闻项
                newsItemRepository.save(newsItem);
                analyzedCount++;

            } catch (Exception e) {
                logger.error("分析新闻情感失败，新闻ID: {}", newsItem.getId(), e);
            }
        }

        logger.info("成功分析 {} 条新闻的情感", analyzedCount);
        return analyzedCount;
    }

    /**
     * 获取未分析的新闻
     */
    public List<NewsItem> getUnanalyzedNews(int limit) {
        // 查找情感评分为空或为0的新闻
        List<NewsItem> unanalyzedNews = newsItemRepository.findBySentimentIsNullOrSentimentScoreIsNullOrSentimentScore(BigDecimal.ZERO);

        if (unanalyzedNews.size() > limit) {
            return unanalyzedNews.subList(0, limit);
        }

        return unanalyzedNews;
    }

    /**
     * 根据条件搜索新闻
     */
    public List<NewsItem> searchNews(String keyword, String category, LocalDateTime startTime, LocalDateTime endTime, int limit) {
        List<NewsItem> results = new ArrayList<>();

        try {
            if (keyword != null && !keyword.trim().isEmpty()) {
                // 使用关键词搜索（模拟实现，实际应该使用全文搜索）
                results = newsItemRepository.findByKeyword(keyword);
            } else if (category != null && !category.trim().isEmpty()) {
                // 按分类搜索
                results = newsItemRepository.findByNewsType(category);
            } else if (startTime != null && endTime != null) {
                // 按时间范围搜索
                results = newsItemRepository.findByPublishTimeBetween(startTime, endTime);
            } else {
                // 返回最新新闻
                results = newsItemRepository.findTop10ByOrderByPublishTimeDesc();
            }

            // 应用限制
            if (results.size() > limit) {
                results = results.subList(0, limit);
            }

        } catch (Exception e) {
            logger.error("新闻搜索失败", e);
        }

        return results;
    }

    /**
     * 检查爬虫服务是否可用
     */
    public boolean isAvailable() {
        return newsItemRepository != null;
    }

    /**
     * 统计新闻数据
     */
    public Map<String, Object> getNewsStatistics() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // 统计总数量
            long totalCount = newsItemRepository.count();
            stats.put("totalCount", totalCount);

            // 按类型统计
            Map<String, Long> typeStats = new HashMap<>();
            String[] newsTypes = {"MACRO", "INDUSTRY", "COMPANY", "FUND", "STOCK", "BOND"};
            for (String type : newsTypes) {
                Long count = newsItemRepository.countByNewsType(type);
                typeStats.put(type, count != null ? count : 0L);
            }
            stats.put("typeStats", typeStats);

            // 按情感统计
            Map<String, Long> sentimentStats = new HashMap<>();
            String[] sentiments = {"POSITIVE", "NEGATIVE", "NEUTRAL"};
            for (String sentiment : sentiments) {
                Long count = newsItemRepository.countBySentiment(sentiment);
                sentimentStats.put(sentiment, count != null ? count : 0L);
            }
            stats.put("sentimentStats", sentimentStats);

            // 最新新闻时间
            List<NewsItem> latestNews = newsItemRepository.findTop10ByOrderByPublishTimeDesc();
            if (!latestNews.isEmpty()) {
                stats.put("latestNewsTime", latestNews.get(0).getPublishTime());
                stats.put("latestNewsCount", latestNews.size());
            }

        } catch (Exception e) {
            logger.error("获取新闻统计信息失败", e);
        }

        return stats;
    }

    // ================ 私有辅助方法 ================

    /**
     * 生成模拟新闻数据
     */
    private List<NewsItem> generateMockNewsItems(List<String> categories, int limit) {
        List<NewsItem> mockItems = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 模拟新闻数据
        String[][] mockNewsData = {
            // 标题, 类型, 摘要, 情感, 重要性, 关键词
            {"央行宣布降准0.25个百分点", "MACRO", "中国人民银行决定下调金融机构存款准备金率0.25个百分点", "POSITIVE", "9", "央行,降准,货币政策"},
            {"科技股集体上涨，AI板块领涨", "INDUSTRY", "今日科技股表现强势，人工智能相关板块涨幅超过3%", "POSITIVE", "7", "科技,AI,股票"},
            {"明星基金经理新发基金一日售罄", "FUND", "知名基金经理张伟新发混合型基金今日开售，募集规模50亿元", "POSITIVE", "8", "基金,募集,混合型"},
            {"腾讯公布财报，净利润增长15%", "COMPANY", "腾讯控股公布2025年第四季度财报，净利润同比增长15%", "POSITIVE", "8", "腾讯,财报,互联网"},
            {"A股三大指数全线收涨", "STOCK", "今日A股市场三大指数集体上涨，上证指数涨1.2%", "POSITIVE", "6", "A股,指数,大盘"},
            {"国债收益率小幅下行", "BOND", "今日国债市场表现平稳，10年期国债收益率下行2个基点", "NEUTRAL", "5", "国债,收益率,债券"},
            {"国际油价大幅波动", "MACRO", "受地缘政治因素影响，国际油价出现大幅波动", "NEGATIVE", "6", "油价,地缘政治,能源"},
            {"新能源汽车行业政策调整", "INDUSTRY", "政府部门发布新能源汽车行业最新政策调整", "NEUTRAL", "7", "新能源汽车,政策,行业"},
            {"某大型基金公司被处罚", "FUND", "因违规操作，某大型基金公司被监管部门处罚", "NEGATIVE", "8", "基金,处罚,监管"},
            {"房地产市场新政出台", "MACRO", "多个城市出台房地产市场新政策，稳定市场预期", "NEUTRAL", "7", "房地产,政策,市场"}
        };

        int count = 0;
        for (String[] data : mockNewsData) {
            if (count >= limit) break;

            // 检查分类过滤
            if (categories != null && !categories.isEmpty()) {
                if (!categories.contains(data[1])) {
                    continue;
                }
            }

            NewsItem newsItem = new NewsItem();
            newsItem.setTitle(data[0]);
            newsItem.setNewsType(data[1]);
            newsItem.setSummary(data[2]);
            newsItem.setSentiment(data[3]);
            newsItem.setImportanceLevel(Integer.parseInt(data[4]));

            // 设置相关字段
            newsItem.setSource("模拟数据源");
            newsItem.setSourceUrl("https://mock.news.example.com/article/" + (count + 1));
            newsItem.setAuthor("模拟作者");
            newsItem.setPublishTime(now.minusHours(count * 2)); // 模拟不同发布时间
            newsItem.setCrawlTime(LocalDateTime.now());

            // 设置关键词
            String[] keywords = data[5].split(",");
            newsItem.setKeywords(Arrays.asList(keywords));

            // 设置相关基金（模拟）
            List<String> relatedFunds = Arrays.asList("000001", "000002", "000003");
            newsItem.setRelatedFundCodes(relatedFunds);

            // 设置情感评分
            if ("POSITIVE".equals(data[3])) {
                newsItem.setSentimentScore(BigDecimal.valueOf(0.8));
                newsItem.setMarketImpactScore(BigDecimal.valueOf(0.6));
                newsItem.setImpactDirection("POSITIVE");
            } else if ("NEGATIVE".equals(data[3])) {
                newsItem.setSentimentScore(BigDecimal.valueOf(-0.7));
                newsItem.setMarketImpactScore(BigDecimal.valueOf(-0.5));
                newsItem.setImpactDirection("NEGATIVE");
            } else {
                newsItem.setSentimentScore(BigDecimal.valueOf(0.1));
                newsItem.setMarketImpactScore(BigDecimal.valueOf(0.2));
                newsItem.setImpactDirection("NEUTRAL");
            }

            mockItems.add(newsItem);
            count++;
        }

        return mockItems;
    }

    /**
     * 过滤重复新闻
     */
    private List<NewsItem> filterDuplicateNews(List<NewsItem> newsItems) {
        if (newsItems == null || newsItems.isEmpty()) {
            return newsItems;
        }

        // 获取最新的新闻标题用于去重
        List<NewsItem> latestNews = newsItemRepository.findTop10ByOrderByPublishTimeDesc();
        Set<String> existingTitles = latestNews.stream()
                .map(NewsItem::getTitle)
                .collect(Collectors.toSet());

        return newsItems.stream()
                .filter(item -> !existingTitles.contains(item.getTitle()))
                .collect(Collectors.toList());
    }

    /**
     * 分析单条新闻的情感
     */
    private void analyzeSingleNewsSentiment(NewsItem newsItem) {
        // 简单的基于关键词的情感分析（模拟）
        String content = newsItem.getTitle() + " " + newsItem.getSummary();
        content = content.toLowerCase();

        // 正面关键词
        List<String> positiveWords = Arrays.asList("上涨", "增长", "盈利", "利好", "降准", "复苏", "创新高", "突破", "优化", "升级");
        // 负面关键词
        List<String> negativeWords = Arrays.asList("下跌", "亏损", "下滑", "利空", "处罚", "风险", "危机", "破产", "违规", "下跌");

        int positiveCount = 0;
        int negativeCount = 0;

        for (String word : positiveWords) {
            if (content.contains(word.toLowerCase())) {
                positiveCount++;
            }
        }

        for (String word : negativeWords) {
            if (content.contains(word.toLowerCase())) {
                negativeCount++;
            }
        }

        // 计算情感得分
        BigDecimal sentimentScore;
        String sentiment;
        String impactDirection;

        if (positiveCount > negativeCount) {
            sentimentScore = BigDecimal.valueOf(0.5 + (positiveCount - negativeCount) * 0.1);
            sentiment = "POSITIVE";
            impactDirection = "POSITIVE";
        } else if (negativeCount > positiveCount) {
            sentimentScore = BigDecimal.valueOf(-0.5 - (negativeCount - positiveCount) * 0.1);
            sentiment = "NEGATIVE";
            impactDirection = "NEGATIVE";
        } else {
            sentimentScore = BigDecimal.valueOf(0.0);
            sentiment = "NEUTRAL";
            impactDirection = "NEUTRAL";
        }

        // 限制在-1.0到1.0之间
        sentimentScore = sentimentScore.min(BigDecimal.ONE).max(BigDecimal.ONE.negate());

        // 更新新闻项
        newsItem.setSentiment(sentiment);
        newsItem.setSentimentScore(sentimentScore);
        newsItem.setImpactDirection(impactDirection);

        // 根据重要性级别设置市场影响评分
        if (newsItem.getImportanceLevel() != null) {
            BigDecimal marketImpact = sentimentScore.multiply(BigDecimal.valueOf(newsItem.getImportanceLevel() / 10.0));
            newsItem.setMarketImpactScore(marketImpact);
        }
    }
}