package com.shxc.fundagent.agent.capabilities.tools;

import com.shxc.fundagent.agent.capabilities.Tool;
import com.shxc.fundagent.agent.capabilities.ToolResult;
import com.shxc.fundagent.entity.FundTransactionRecord;
import com.shxc.fundagent.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 交易执行工具
 * 用于执行基金买入和卖出交易
 */
@Component
public class TransactionTool implements Tool {

    private static final Logger logger = LoggerFactory.getLogger(TransactionTool.class);

    private static final String TOOL_NAME = "execute_transaction";
    private static final String TOOL_DESCRIPTION = "执行基金买入或卖出交易。根据操作类型创建相应的交易记录。";

    private static final String PARAMETER_SCHEMA = """
        {
          "type": "object",
          "properties": {
            "operation": {
              "type": "string",
              "enum": ["BUY", "SELL"],
              "description": "交易操作类型：BUY表示买入，SELL表示卖出"
            },
            "fundCode": {
              "type": "string",
              "description": "基金代码"
            },
            "amount": {
              "type": "number",
              "description": "交易金额（买入时为总金额，卖出时为份额）"
            },
            "transactionTime": {
              "type": "string",
              "format": "date-time",
              "description": "交易时间（ISO 8601格式），可选，默认为当前时间"
            },
            "fee": {
              "type": "number",
              "description": "手续费，可选，默认为0"
            }
          },
          "required": ["operation", "fundCode", "amount"]
        }
        """;

    private final TransactionService transactionService;

    @Autowired
    public TransactionTool(TransactionService transactionService) {
        this.transactionService = transactionService;
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
            // 验证必需参数
            String operation = getRequiredStringParam(parameters, "operation", "操作类型");
            String fundCode = getRequiredStringParam(parameters, "fundCode", "基金代码");
            BigDecimal amount = getRequiredBigDecimalParam(parameters, "amount", "交易金额");

            // 解析可选参数
            LocalDateTime transactionTime = getOptionalDateTimeParam(parameters, "transactionTime");
            BigDecimal fee = getOptionalBigDecimalParam(parameters, "fee", BigDecimal.ZERO);

            // 执行交易
            FundTransactionRecord transaction;
            if ("BUY".equalsIgnoreCase(operation)) {
                transaction = transactionService.createBuyTransaction(fundCode, amount, transactionTime, fee);
                logger.info("买入交易创建成功: fundCode={}, amount={}, transactionId={}",
                    fundCode, amount, transaction.getId());
            } else if ("SELL".equalsIgnoreCase(operation)) {
                transaction = transactionService.createSellTransaction(fundCode, amount, transactionTime, fee);
                logger.info("卖出交易创建成功: fundCode={}, amount={}, transactionId={}",
                    fundCode, amount, transaction.getId());
            } else {
                return ToolResult.error(
                    "不支持的操作类型: " + operation + "，支持的操作类型: BUY, SELL",
                    "INVALID_OPERATION"
                );
            }

            // 构建成功结果
            return ToolResult.success(
                Map.of(
                    "transactionId", transaction.getId(),
                    "fundCode", transaction.getFundCode(),
                    "operation", operation,
                    "amount", amount,
                    "status", transaction.getStatus().name(),
                    "estimatedConfirmDate", transaction.getEstimatedConfirmDate().toString(),
                    "createdTime", LocalDateTime.now().toString()
                ),
                0L
            );

        } catch (IllegalArgumentException e) {
            logger.warn("交易执行参数错误: {}", e.getMessage());
            return ToolResult.error(e.getMessage(), "INVALID_PARAMETERS");
        } catch (Exception e) {
            logger.error("交易执行失败", e);
            return ToolResult.error("交易执行失败: " + e.getMessage(), "EXECUTION_ERROR");
        }
    }

    @Override
    public boolean isAvailable() {
        return transactionService != null;
    }

    @Override
    public String getCategory() {
        return "trading";
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

    // 辅助方法：获取必需的BigDecimal参数
    private BigDecimal getRequiredBigDecimalParam(Map<String, Object> parameters, String key, String paramName) {
        Object value = parameters.get(key);
        if (value == null) {
            throw new IllegalArgumentException("必需参数缺失: " + paramName + " (" + key + ")");
        }

        try {
            if (value instanceof Number) {
                if (value instanceof Integer) {
                    return new BigDecimal((Integer) value);
                } else if (value instanceof Long) {
                    return new BigDecimal((Long) value);
                } else if (value instanceof Double) {
                    return BigDecimal.valueOf((Double) value);
                } else if (value instanceof BigDecimal) {
                    return (BigDecimal) value;
                }
            } else if (value instanceof String) {
                return new BigDecimal(((String) value).trim());
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("参数格式错误: " + paramName + " 必须是有效的数字");
        }

        throw new IllegalArgumentException("参数类型错误: " + paramName + " 必须是数字类型");
    }

    // 辅助方法：获取可选的日期时间参数
    private LocalDateTime getOptionalDateTimeParam(Map<String, Object> parameters, String key) {
        Object value = parameters.get(key);
        if (value == null) {
            return LocalDateTime.now();
        }

        try {
            if (value instanceof String) {
                String strValue = ((String) value).trim();
                if (strValue.isEmpty()) {
                    return LocalDateTime.now();
                }
                // 尝试解析ISO 8601格式
                return LocalDateTime.parse(strValue, DateTimeFormatter.ISO_DATE_TIME);
            } else if (value instanceof LocalDateTime) {
                return (LocalDateTime) value;
            }
        } catch (Exception e) {
            logger.warn("无法解析交易时间参数，使用当前时间: {}", e.getMessage());
        }

        return LocalDateTime.now();
    }

    // 辅助方法：获取可选的BigDecimal参数
    private BigDecimal getOptionalBigDecimalParam(Map<String, Object> parameters, String key, BigDecimal defaultValue) {
        Object value = parameters.get(key);
        if (value == null) {
            return defaultValue;
        }

        try {
            if (value instanceof Number) {
                if (value instanceof Integer) {
                    return new BigDecimal((Integer) value);
                } else if (value instanceof Long) {
                    return new BigDecimal((Long) value);
                } else if (value instanceof Double) {
                    return BigDecimal.valueOf((Double) value);
                } else if (value instanceof BigDecimal) {
                    return (BigDecimal) value;
                }
            } else if (value instanceof String) {
                String strValue = ((String) value).trim();
                if (strValue.isEmpty()) {
                    return defaultValue;
                }
                return new BigDecimal(strValue);
            }
        } catch (NumberFormatException e) {
            logger.warn("无法解析可选参数 {}，使用默认值: {}", key, e.getMessage());
        }

        return defaultValue;
    }
}