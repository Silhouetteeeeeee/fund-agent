package com.shxc.fundagent.agent.capabilities.tools;

import com.shxc.fundagent.agent.capabilities.Tool;
import com.shxc.fundagent.agent.capabilities.ToolResult;
import com.shxc.fundagent.entity.FundDailyData;
import com.shxc.fundagent.entity.FundInfo;
import com.shxc.fundagent.enums.FundType;
import com.shxc.fundagent.repository.FundInfoRepository;
import com.shxc.fundagent.service.FundDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 市场数据工具
 * 用于查询基金实时数据、历史数据和基础信息
 */
@Component
public class MarketDataTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(MarketDataTool.class);

    private static final String TOOL_NAME = "query_market_data";
    private static final String TOOL_DESCRIPTION = "查询基金市场数据，包括实时净值、历史数据、基金基础信息等。";

    private static final String PARAMETER_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "queryType": {
              "type": "string",
              "enum": ["REAL_TIME", "HISTORY", "FUND_INFO", "FUND_LIST"],
              "description": "查询类型：REAL_TIME(实时数据)、HISTORY(历史数据)、FUND_INFO(基金信息)、FUND_LIST(基金列表)"
            },
            "fundCode": {
              "type": "string",
              "description": "基金代码（查询类型为FUND_LIST时可选）"
            },
            "startDate": {
              "type": "string",
              "format": "date",
              "description": "开始日期（查询历史数据时必需，格式：yyyy-MM-dd）"
            },
            "endDate": {
              "type": "string",
              "format": "date",
              "description": "结束日期（查询历史数据时必需，格式：yyyy-MM-dd）"
            },
            "fundType": {
              "type": "string",
              "description": "基金类型（查询基金列表时可选，如：STOCK, BOND, MIXED）"
            },
            "limit": {
              "type": "integer",
              "minimum": 1,
              "maximum": 100,
              "description": "返回记录数量限制（查询基金列表时可选，默认10）"
            }
          },
          "required": ["queryType"]
        }
        """;

    private final FundDataService fundDataService;
    private final FundInfoRepository fundInfoRepository;

    @Autowired
    public MarketDataTool(FundDataService fundDataService, FundInfoRepository fundInfoRepository) {
        this.fundDataService = fundDataService;
        this.fundInfoRepository = fundInfoRepository;
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

            switch (queryType.toUpperCase()) {
                case "REAL_TIME":
                    return handleRealTimeQuery(parameters);
                case "HISTORY":
                    return handleHistoryQuery(parameters);
                case "FUND_INFO":
                    return handleFundInfoQuery(parameters);
                case "FUND_LIST":
                    return handleFundListQuery(parameters);
                default:
                    return ToolResult.error(
                        "不支持的查询类型: " + queryType + "，支持的查询类型: REAL_TIME, HISTORY, FUND_INFO, FUND_LIST",
                        "INVALID_QUERY_TYPE"
                    );
            }

        } catch (IllegalArgumentException e) {
            logger.warn("市场数据查询参数错误: {}", e.getMessage());
            return ToolResult.error(e.getMessage(), "INVALID_PARAMETERS");
        } catch (Exception e) {
            logger.error("市场数据查询失败", e);
            return ToolResult.error("市场数据查询失败: " + e.getMessage(), "EXECUTION_ERROR");
        }
    }

    @Override
    public boolean isAvailable() {
        return fundDataService != null && fundInfoRepository != null;
    }

    @Override
    public String getCategory() {
        return "market_data";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    // 处理实时数据查询
    private ToolResult handleRealTimeQuery(Map<String, Object> parameters) {
        String fundCode = getRequiredStringParam(parameters, "fundCode", "基金代码");

        try {
            FundDailyData realTimeData = fundDataService.getRealTimeData(fundCode);

            if (realTimeData == null) {
                return ToolResult.error("无法获取基金实时数据: " + fundCode, "DATA_NOT_FOUND");
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fundCode", realTimeData.getFundCode());
            // 通过fundInfo获取基金名称
            String fundName = "未知基金";
            if (realTimeData.getFundCode() != null) {
                Optional<FundInfo> fundInfo = fundInfoRepository.findByFundCode(realTimeData.getFundCode());
                if (fundInfo.isPresent()) {
                    fundName = fundInfo.get().getFundName();
                }
            }
            result.put("fundName", fundName);
            result.put("netValue", realTimeData.getNetValue());
            result.put("accumulatedNetValue", realTimeData.getNetValue()); // 使用净值作为累计净值
            result.put("dailyReturnRate", realTimeData.getChangeRate());
            result.put("tradeDate", realTimeData.getTradeDate().toString());
            result.put("updateTime", realTimeData.getCreateTime() != null ?
                realTimeData.getCreateTime().toString() : null);
            result.put("dataSource", realTimeData.getDataSource());

            return ToolResult.success(result, 0L);

        } catch (Exception e) {
            logger.error("实时数据查询失败: fundCode={}", fundCode, e);
            return ToolResult.error("实时数据查询失败: " + e.getMessage(), "QUERY_ERROR");
        }
    }

    // 处理历史数据查询
    private ToolResult handleHistoryQuery(Map<String, Object> parameters) {
        String fundCode = getRequiredStringParam(parameters, "fundCode", "基金代码");
        LocalDate startDate = getRequiredDateParam(parameters, "startDate", "开始日期");
        LocalDate endDate = getRequiredDateParam(parameters, "endDate", "结束日期");

        // 验证日期范围
        if (startDate.isAfter(endDate)) {
            return ToolResult.error("开始日期不能晚于结束日期", "INVALID_DATE_RANGE");
        }

        try {
            List<FundDailyData> historyData = fundDataService.getHistoryData(fundCode, startDate, endDate);

            if (historyData == null || historyData.isEmpty()) {
                return ToolResult.success(
                    Map.of(
                        "fundCode", fundCode,
                        "startDate", startDate.toString(),
                        "endDate", endDate.toString(),
                        "dataCount", 0,
                        "historyData", Collections.emptyList()
                    ),
                    0L
                );
            }

            List<Map<String, Object>> historyList = new ArrayList<>();
            for (FundDailyData data : historyData) {
                Map<String, Object> dataMap = new LinkedHashMap<>();
                dataMap.put("tradeDate", data.getTradeDate().toString());
                dataMap.put("netValue", data.getNetValue());
                dataMap.put("accumulatedNetValue", data.getNetValue()); // 使用净值作为累计净值
                dataMap.put("dailyReturnRate", data.getChangeRate());
                dataMap.put("volume", data.getTurnover());
                historyList.add(dataMap);
            }

            // 计算统计信息
            BigDecimal maxNetValue = historyData.stream()
                .map(FundDailyData::getNetValue)
                .filter(Objects::nonNull)
                .max(BigDecimal::compareTo)
                .orElse(null);

            BigDecimal minNetValue = historyData.stream()
                .map(FundDailyData::getNetValue)
                .filter(Objects::nonNull)
                .min(BigDecimal::compareTo)
                .orElse(null);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fundCode", fundCode);
            result.put("startDate", startDate.toString());
            result.put("endDate", endDate.toString());
            result.put("dataCount", historyData.size());
            result.put("maxNetValue", maxNetValue);
            result.put("minNetValue", minNetValue);
            result.put("historyData", historyList);

            return ToolResult.success(result, 0L);

        } catch (Exception e) {
            logger.error("历史数据查询失败: fundCode={}, startDate={}, endDate={}",
                fundCode, startDate, endDate, e);
            return ToolResult.error("历史数据查询失败: " + e.getMessage(), "QUERY_ERROR");
        }
    }

    // 处理基金信息查询
    private ToolResult handleFundInfoQuery(Map<String, Object> parameters) {
        String fundCode = getRequiredStringParam(parameters, "fundCode", "基金代码");

        try {
            Optional<FundInfo> fundInfoOpt = fundInfoRepository.findByFundCode(fundCode);

            if (fundInfoOpt.isEmpty()) {
                return ToolResult.error("基金信息不存在: " + fundCode, "FUND_NOT_FOUND");
            }

            FundInfo fundInfo = fundInfoOpt.get();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fundCode", fundInfo.getFundCode());
            result.put("fundName", fundInfo.getFundName());
            result.put("fundType", fundInfo.getFundType());
            result.put("company", fundInfo.getFundCompany());
            result.put("establishDate", fundInfo.getEstablishedDate() != null ?
                fundInfo.getEstablishedDate().toString() : null);
            result.put("fundSize", fundInfo.getFundSize());
            result.put("benchmark", "N/A"); // FundInfo没有benchmark字段
            result.put("riskLevel", fundInfo.getRiskLevel());
            result.put("custodianBank", "N/A"); // FundInfo没有custodianBank字段
            result.put("status", fundInfo.getIsActive() != null ?
                (fundInfo.getIsActive() ? "ACTIVE" : "INACTIVE") : "UNKNOWN");
            result.put("updateTime", fundInfo.getUpdateTime() != null ?
                fundInfo.getUpdateTime().toString() : null);

            return ToolResult.success(result, 0L);

        } catch (Exception e) {
            logger.error("基金信息查询失败: fundCode={}", fundCode, e);
            return ToolResult.error("基金信息查询失败: " + e.getMessage(), "QUERY_ERROR");
        }
    }

    // 处理基金列表查询
    private ToolResult handleFundListQuery(Map<String, Object> parameters) {
        String fundType = getOptionalStringParam(parameters, "fundType", null);
        int limit = getOptionalIntParam(parameters, "limit", 10);

        try {
            List<FundInfo> fundList;
            if (fundType != null && !fundType.isEmpty()) {
                // 将字符串转换为FundType枚举
                FundType fundTypeEnum;
                try {
                    // 先尝试直接转换为枚举值（如"STOCK"、"BOND"等）
                    fundTypeEnum = FundType.valueOf(fundType.toUpperCase());
                } catch (IllegalArgumentException e) {
                    // 如果不是枚举名称，尝试使用显示名称
                    fundTypeEnum = FundType.fromDisplayName(fundType);
                }
                fundList = fundInfoRepository.findByFundType(fundTypeEnum);
            } else {
                fundList = fundInfoRepository.findAll();
            }

            // 应用限制
            if (fundList.size() > limit) {
                fundList = fundList.subList(0, Math.min(limit, fundList.size()));
            }

            List<Map<String, Object>> fundInfoList = new ArrayList<>();
            for (FundInfo fundInfo : fundList) {
                Map<String, Object> fundMap = new LinkedHashMap<>();
                fundMap.put("fundCode", fundInfo.getFundCode());
                fundMap.put("fundName", fundInfo.getFundName());
                fundMap.put("fundType", fundInfo.getFundType());
                fundMap.put("company", fundInfo.getFundCompany());
                fundMap.put("establishDate", fundInfo.getEstablishedDate() != null ?
                    fundInfo.getEstablishedDate().toString() : null);
                fundMap.put("fundSize", fundInfo.getFundSize());
                fundMap.put("riskLevel", fundInfo.getRiskLevel());
                fundInfoList.add(fundMap);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("queryType", fundType != null ? fundType : "ALL");
            result.put("limit", limit);
            result.put("totalCount", fundList.size());
            result.put("funds", fundInfoList);

            return ToolResult.success(result, 0L);

        } catch (Exception e) {
            logger.error("基金列表查询失败: fundType={}", fundType, e);
            return ToolResult.error("基金列表查询失败: " + e.getMessage(), "QUERY_ERROR");
        }
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

    // 辅助方法：获取必需的日期参数
    private LocalDate getRequiredDateParam(Map<String, Object> parameters, String key, String paramName) {
        Object value = parameters.get(key);
        if (value == null) {
            throw new IllegalArgumentException("必需参数缺失: " + paramName + " (" + key + ")");
        }

        try {
            if (value instanceof String) {
                String strValue = ((String) value).trim();
                if (strValue.isEmpty()) {
                    throw new IllegalArgumentException("参数不能为空: " + paramName);
                }
                return LocalDate.parse(strValue, DateTimeFormatter.ISO_DATE);
            } else if (value instanceof LocalDate) {
                return (LocalDate) value;
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("参数格式错误: " + paramName + " 必须是有效的日期格式 (yyyy-MM-dd)");
        }

        throw new IllegalArgumentException("参数类型错误: " + paramName + " 必须是日期类型");
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
}