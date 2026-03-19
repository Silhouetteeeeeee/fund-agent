package com.shxc.fundagent.agent.capabilities.tools;

import com.shxc.fundagent.agent.capabilities.Tool;
import com.shxc.fundagent.agent.capabilities.ToolResult;
import com.shxc.fundagent.entity.NewsItem;
import com.shxc.fundagent.service.NewsCrawlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 新闻爬虫工具
 * 用于获取财经新闻和资讯
 *
 * 注意：这是一个框架实现，实际新闻采集需要集成外部新闻API或爬虫服务
 */
@Component
public class NewsCrawlerTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(NewsCrawlerTool.class);

    private static final String TOOL_NAME = "fetch_news";
    private static final String TOOL_DESCRIPTION = "获取财经新闻和资讯，支持按关键字、分类和时间范围筛选。";

    private static final String PARAMETER_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "queryType": {
              "type": "string",
              "enum": ["KEYWORD", "CATEGORY", "LATEST", "RECOMMENDED"],
              "description": "查询类型：KEYWORD(关键字搜索)、CATEGORY(分类查询)、LATEST(最新新闻)、RECOMMENDED(推荐新闻)"
            },
            "keywords": {
              "type": "array",
              "items": {"type": "string"},
              "description": "搜索关键字（查询类型为KEYWORD时必需）"
            },
            "category": {
              "type": "string",
              "enum": ["MACRO", "INDUSTRY", "COMPANY", "FUND", "STOCK", "BOND"],
              "description": "新闻分类：MACRO(宏观经济)、INDUSTRY(行业)、COMPANY(公司)、FUND(基金)、STOCK(股票)、BOND(债券)"
            },
            "startDate": {
              "type": "string",
              "format": "date",
              "description": "开始日期（格式：yyyy-MM-dd）"
            },
            "endDate": {
              "type": "string",
              "format": "date",
              "description": "结束日期（格式：yyyy-MM-dd）"
            },
            "limit": {
              "type": "integer",
              "minimum": 1,
              "maximum": 50,
              "description": "返回新闻数量限制，默认10"
            },
            "relatedFunds": {
              "type": "array",
              "items": {"type": "string"},
              "description": "相关基金代码列表，用于筛选与特定基金相关的新闻"
            }
          },
          "required": ["queryType"]
        }
        """;

    private final NewsCrawlerService newsCrawlerService;

    @Autowired
    public NewsCrawlerTool(NewsCrawlerService newsCrawlerService) {
        this.newsCrawlerService = newsCrawlerService;
    }

    @Override
    public String getName() {
        return TOOL_NAME;
    }

    @Override
    public String getDescription() {
        return TOOL_DESCRIPTION;
    }

    @Override
    public String getParameterSchema() {
        return PARAMETER_SCHEMA;
    }

    @Override
    public ToolResult execute(Map<String, Object> parameters) {
        try {
            String queryType = getRequiredStringParam(parameters, "queryType", "查询类型");
            logger.info("新闻爬虫工具执行中，参数: {}", parameters);

            // 根据查询类型调用不同的服务方法
            List<NewsItem> newsItems;
            switch (queryType.toUpperCase()) {
                case "KEYWORD":
                    newsItems = handleKeywordQuery(parameters);
                    break;
                case "CATEGORY":
                    newsItems = handleCategoryQuery(parameters);
                    break;
                case "LATEST":
                    newsItems = handleLatestQuery(parameters);
                    break;
                case "RECOMMENDED":
                    newsItems = handleRecommendedQuery(parameters);
                    break;
                default:
                    return ToolResult.error("不支持的查询类型: " + queryType, "INVALID_QUERY_TYPE");
            }

            // 转换NewsItem为简单Map格式返回
            List<Map<String, Object>> newsList = convertNewsItemsToMap(newsItems);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("queryType", queryType);
            result.put("newsCount", newsList.size());
            result.put("news", newsList);
            result.put("dataSource", "DATABASE");
            result.put("timestamp", new Date());

            return ToolResult.success(result, 0L);

        } catch (IllegalArgumentException e) {
            logger.warn("新闻查询参数错误: {}", e.getMessage());
            return ToolResult.error(e.getMessage(), "INVALID_PARAMETERS");
        } catch (Exception e) {
            logger.error("新闻查询失败", e);
            return ToolResult.error("新闻查询失败: " + e.getMessage(), "EXECUTION_ERROR");
        }
    }

    @Override
    public boolean isAvailable() {
        return newsCrawlerService != null && newsCrawlerService.isAvailable();
    }

    @Override
    public String getCategory() {
        return "news";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }


    // 辅助方法：获取必需的字符串参数
    private String getRequiredStringParam(Map<String, Object> parameters, String key, String paramName) {
        Object value = parameters.get(key);
        if (value == null) {
            throw new IllegalArgumentException("必需参数缺失: " + paramName + " (" + key + ")");
        }
        if (!(value instanceof String)) {
            throw new IllegalArgumentException("参数类型错误: " + paramName + " 必须是字符串类型");
        }
        String strValue = ((String) value).trim();
        if (strValue.isEmpty()) {
            throw new IllegalArgumentException("参数不能为空: " + paramName);
        }
        return strValue;
    }

    // 辅助方法：获取可选的字符串参数
    private String getOptionalStringParam(Map<String, Object> parameters, String key, String defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof String)) {
            logger.warn("参数 {} 类型错误，使用默认值", key);
            return defaultValue;
        }
        String strValue = ((String) value).trim();
        return strValue.isEmpty() ? defaultValue : strValue;
    }

    // 辅助方法：获取可选的整数参数
    private int getOptionalIntParam(Map<String, Object> parameters, String key, int defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            if (value instanceof Integer) {
                return (Integer) value;
            } else if (value instanceof Long) {
                return ((Long) value).intValue();
            } else if (value instanceof String) {
                String strValue = ((String) value).trim();
                if (strValue.isEmpty()) {
                    return defaultValue;
                }
                return Integer.parseInt(strValue);
            } else if (value instanceof Number) {
                return ((Number) value).intValue();
            }
        } catch (NumberFormatException e) {
            logger.warn("参数 {} 格式错误，使用默认值: {}", key, defaultValue);
        }

        return defaultValue;
    }

    // 处理关键字查询
    private List<NewsItem> handleKeywordQuery(Map<String, Object> parameters) {
        if (!parameters.containsKey("keywords")) {
            throw new IllegalArgumentException("关键字查询需要keywords参数");
        }

        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) parameters.get("keywords");
        if (keywords == null || keywords.isEmpty()) {
            throw new IllegalArgumentException("keywords参数不能为空");
        }

        // 使用第一个关键词进行搜索（简化实现）
        String keyword = keywords.get(0);
        String category = getOptionalStringParam(parameters, "category", null);
        LocalDateTime startTime = parseDateParam(parameters, "startDate");
        LocalDateTime endTime = parseDateParam(parameters, "endDate");
        int limit = getOptionalIntParam(parameters, "limit", 10);

        return newsCrawlerService.searchNews(keyword, category, startTime, endTime, limit);
    }

    // 处理分类查询
    private List<NewsItem> handleCategoryQuery(Map<String, Object> parameters) {
        if (!parameters.containsKey("category")) {
            throw new IllegalArgumentException("分类查询需要category参数");
        }

        String category = (String) parameters.get("category");
        LocalDateTime startTime = parseDateParam(parameters, "startDate");
        LocalDateTime endTime = parseDateParam(parameters, "endDate");
        int limit = getOptionalIntParam(parameters, "limit", 10);

        // 如果有关键词，使用关键词搜索，否则使用分类搜索
        if (parameters.containsKey("keywords")) {
            @SuppressWarnings("unchecked")
            List<String> keywords = (List<String>) parameters.get("keywords");
            if (keywords != null && !keywords.isEmpty()) {
                return newsCrawlerService.searchNews(keywords.get(0), category, startTime, endTime, limit);
            }
        }

        return newsCrawlerService.searchNews(null, category, startTime, endTime, limit);
    }

    // 处理最新新闻查询
    private List<NewsItem> handleLatestQuery(Map<String, Object> parameters) {
        String category = getOptionalStringParam(parameters, "category", null);
        int limit = getOptionalIntParam(parameters, "limit", 10);

        if (category != null) {
            return newsCrawlerService.searchNews(null, category, null, null, limit);
        } else {
            return newsCrawlerService.searchNews(null, null, null, null, limit);
        }
    }

    // 处理推荐新闻查询
    private List<NewsItem> handleRecommendedQuery(Map<String, Object> parameters) {
        // 推荐新闻：返回重要性高的新闻
        int limit = getOptionalIntParam(parameters, "limit", 10);
        List<NewsItem> allNews = newsCrawlerService.searchNews(null, null, null, null, limit * 2);

        // 按重要性级别排序
        allNews.sort((a, b) -> {
            Integer importanceA = a.getImportanceLevel() != null ? a.getImportanceLevel() : 0;
            Integer importanceB = b.getImportanceLevel() != null ? b.getImportanceLevel() : 0;
            return Integer.compare(importanceB, importanceA); // 降序排列
        });

        // 应用限制
        if (allNews.size() > limit) {
            return allNews.subList(0, limit);
        }

        return allNews;
    }

    // 转换NewsItem为Map格式
    private List<Map<String, Object>> convertNewsItemsToMap(List<NewsItem> newsItems) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (NewsItem newsItem : newsItems) {
            Map<String, Object> itemMap = new LinkedHashMap<>();
            itemMap.put("id", newsItem.getId());
            itemMap.put("title", newsItem.getTitle());
            itemMap.put("summary", newsItem.getSummary());
            itemMap.put("newsType", newsItem.getNewsType());
            itemMap.put("source", newsItem.getSource());
            itemMap.put("publishTime", newsItem.getPublishTime() != null ?
                newsItem.getPublishTime().toString() : null);
            itemMap.put("importanceLevel", newsItem.getImportanceLevel());
            itemMap.put("sentiment", newsItem.getSentiment());
            itemMap.put("sentimentScore", newsItem.getSentimentScore());
            itemMap.put("relatedFundCodes", newsItem.getRelatedFundCodes());
            itemMap.put("keywords", newsItem.getKeywords());
            itemMap.put("marketImpactScore", newsItem.getMarketImpactScore());

            result.add(itemMap);
        }

        return result;
    }

    // 解析日期参数
    private LocalDateTime parseDateParam(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            return null;
        }

        try {
            if (value instanceof String) {
                String strValue = ((String) value).trim();
                if (strValue.isEmpty()) {
                    return null;
                }
                // 尝试解析日期
                LocalDate date = LocalDate.parse(strValue, DateTimeFormatter.ISO_DATE);
                return date.atStartOfDay();
            }
        } catch (DateTimeParseException e) {
            logger.warn("无法解析日期参数 {}: {}", key, e.getMessage());
        }

        return null;
    }
}