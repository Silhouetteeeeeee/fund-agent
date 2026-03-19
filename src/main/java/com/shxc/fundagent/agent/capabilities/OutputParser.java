package com.shxc.fundagent.agent.capabilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 输出解析器
 * 负责解析LLM输出，支持JSON Schema验证和结构化提取
 */
@Component
public class OutputParser {

    private static final Logger logger = LoggerFactory.getLogger(OutputParser.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // JSON提取模式
    private static final Pattern JSON_PATTERN = Pattern.compile(
            "```(?:json)?\\s*(.*?)\\s*```|\\{(?:[^{}]|\\{[^{}]*\\})*\\}",
            Pattern.DOTALL
    );

    /**
     * 解析结构化输出
     * @param llmOutput LLM输出文本
     * @param schemaJson JSON Schema定义
     * @return 解析后的结构化数据
     */
    public Map<String, Object> parseStructuredOutput(String llmOutput, String schemaJson) {
        long startTime = System.currentTimeMillis();
        logger.debug("开始解析结构化输出");

        try {
            // 1. 提取JSON部分
            String jsonContent = extractJsonContent(llmOutput);
            if (jsonContent == null || jsonContent.trim().isEmpty()) {
                throw new IllegalArgumentException("无法从LLM输出中提取有效的JSON内容");
            }

            // 2. 解析JSON
            JsonNode jsonNode = objectMapper.readTree(jsonContent);

            // 3. 基本验证（这里简化了Schema验证，实际应该使用JSON Schema验证库）
            if (!validateBasicStructure(jsonNode, schemaJson)) {
                logger.warn("输出不符合基本结构要求，尝试修复");
                jsonNode = attemptFixJsonStructure(jsonNode, schemaJson);
            }

            // 4. 转换为Map
            Map<String, Object> result = convertJsonNodeToMap(jsonNode);

            long processingTime = System.currentTimeMillis() - startTime;
            logger.debug("结构化输出解析完成，耗时: {}ms", processingTime);

            return result;

        } catch (Exception e) {
            logger.error("解析结构化输出失败", e);
            throw new RuntimeException("解析结构化输出失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提取JSON内容
     */
    private String extractJsonContent(String llmOutput) {
        if (llmOutput == null || llmOutput.trim().isEmpty()) {
            return null;
        }

        // 尝试匹配代码块中的JSON
        Matcher matcher = JSON_PATTERN.matcher(llmOutput);
        if (matcher.find()) {
            String potentialJson = matcher.group(1);
            if (potentialJson != null && !potentialJson.trim().isEmpty()) {
                return potentialJson.trim();
            }
            // 如果没有group(1)，可能是第二个模式匹配到了
            String matched = matcher.group(0);
            if (matched.startsWith("{")) {
                return matched.trim();
            }
        }

        // 如果没有找到代码块，尝试直接查找JSON对象
        int jsonStart = llmOutput.indexOf('{');
        int jsonEnd = llmOutput.lastIndexOf('}');

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            String extractedJson = llmOutput.substring(jsonStart, jsonEnd + 1);
            // 验证是否是有效的JSON
            try {
                objectMapper.readTree(extractedJson);
                return extractedJson.trim();
            } catch (Exception e) {
                logger.debug("提取的文本不是有效的JSON: {}", e.getMessage());
            }
        }

        // 如果都失败，返回原始文本（可能不是JSON）
        logger.warn("无法提取有效的JSON，返回原始文本（可能解析失败）");
        return llmOutput.trim();
    }

    /**
     * 基本结构验证
     */
    private boolean validateBasicStructure(JsonNode jsonNode, String schemaJson) {
        if (jsonNode == null || !jsonNode.isObject()) {
            return false;
        }

        try {
            // 简单验证：检查是否包含必需的字段
            // 这里可以扩展为完整的JSON Schema验证
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            JsonNode requiredFields = schemaNode.get("required");

            if (requiredFields != null && requiredFields.isArray()) {
                for (JsonNode field : requiredFields) {
                    String fieldName = field.asText();
                    if (!jsonNode.has(fieldName)) {
                        logger.warn("缺少必需字段: {}", fieldName);
                        return false;
                    }
                }
            }

            return true;

        } catch (Exception e) {
            logger.warn("Schema验证失败，跳过验证: {}", e.getMessage());
            return true; // 如果Schema解析失败，跳过验证
        }
    }

    /**
     * 尝试修复JSON结构
     */
    private JsonNode attemptFixJsonStructure(JsonNode jsonNode, String schemaJson) {
        try {
            JsonNode schemaNode = objectMapper.readTree(schemaJson);
            JsonNode requiredFields = schemaNode.get("required");

            if (requiredFields != null && requiredFields.isArray()) {
                ObjectNode objectNode = (ObjectNode) jsonNode;

                for (JsonNode field : requiredFields) {
                    String fieldName = field.asText();
                    if (!objectNode.has(fieldName)) {
                        // 添加缺失的必需字段（使用默认值）
                        JsonNode properties = schemaNode.get("properties");
                        if (properties != null && properties.has(fieldName)) {
                            JsonNode fieldSchema = properties.get(fieldName);
                            Object defaultValue = getDefaultValueForType(fieldSchema);
                            if (defaultValue != null) {
                                objectNode.set(fieldName, objectMapper.valueToTree(defaultValue));
                                logger.info("添加缺失的必需字段: {} = {}", fieldName, defaultValue);
                            }
                        }
                    }
                }

                return objectNode;
            }

        } catch (Exception e) {
            logger.warn("修复JSON结构失败: {}", e.getMessage());
        }

        return jsonNode;
    }

    /**
     * 根据Schema类型获取默认值
     */
    private Object getDefaultValueForType(JsonNode fieldSchema) {
        if (fieldSchema == null || !fieldSchema.isObject()) {
            return null;
        }

        JsonNode typeNode = fieldSchema.get("type");
        if (typeNode == null) {
            return null;
        }

        String type = typeNode.asText();
        switch (type) {
            case "string":
                return "";
            case "number":
            case "integer":
                return 0;
            case "boolean":
                return false;
            case "array":
                return new ArrayList<>();
            case "object":
                return new HashMap<>();
            default:
                return null;
        }
    }

    /**
     * 将JsonNode转换为Map
     */
    private Map<String, Object> convertJsonNodeToMap(JsonNode jsonNode) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (jsonNode == null || !jsonNode.isObject()) {
            return result;
        }

        jsonNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();

            result.put(key, convertJsonValue(value));
        });

        return result;
    }

    /**
     * 转换JsonNode值为Java对象
     */
    private Object convertJsonValue(JsonNode node) {
        if (node == null) {
            return null;
        }

        if (node.isNull()) {
            return null;
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isNumber()) {
            if (node.isInt()) {
                return node.asInt();
            } else if (node.isLong()) {
                return node.asLong();
            } else if (node.isDouble()) {
                return node.asDouble();
            } else {
                return new BigDecimal(node.asText());
            }
        } else if (node.isBoolean()) {
            return node.asBoolean();
        } else if (node.isArray()) {
            List<Object> list = new ArrayList<>();
            node.forEach(element -> list.add(convertJsonValue(element)));
            return list;
        } else if (node.isObject()) {
            return convertJsonNodeToMap(node);
        }

        return node.asText();
    }

    /**
     * 解析简单文本输出
     */
    public String parseTextOutput(String llmOutput) {
        if (llmOutput == null) {
            return "";
        }

        // 清理输出：移除代码标记等
        String cleaned = llmOutput.replaceAll("```[\\w]*", "").trim();

        // 提取主要内容（如果有多部分）
        String[] parts = cleaned.split("\n\n");
        if (parts.length > 1) {
            // 尝试找到最长的部分作为主要内容
            String mainContent = parts[0];
            for (String part : parts) {
                if (part.length() > mainContent.length()) {
                    mainContent = part;
                }
            }
            return mainContent.trim();
        }

        return cleaned;
    }

    /**
     * 解析列表输出
     */
    public List<String> parseListOutput(String llmOutput, String itemPrefix) {
        List<String> items = new ArrayList<>();

        if (llmOutput == null || llmOutput.isEmpty()) {
            return items;
        }

        String[] lines = llmOutput.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty()) {
                // 移除项目符号或编号
                if (itemPrefix != null && line.startsWith(itemPrefix)) {
                    line = line.substring(itemPrefix.length()).trim();
                }
                // 移除常见的列表标记
                line = line.replaceAll("^[-•*]\\s*", "").trim();
                if (!line.isEmpty()) {
                    items.add(line);
                }
            }
        }

        return items;
    }

    /**
     * 解析键值对输出
     */
    public Map<String, String> parseKeyValueOutput(String llmOutput) {
        Map<String, String> result = new LinkedHashMap<>();

        if (llmOutput == null || llmOutput.isEmpty()) {
            return result;
        }

        String[] lines = llmOutput.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                continue;
            }

            // 匹配键值对：key: value 或 key = value
            String[] parts = line.split("[:=]", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                result.put(key, value);
            }
        }

        return result;
    }

    /**
     * 验证输出是否符合预期格式
     */
    public ValidationResult validateOutput(String llmOutput, String expectedFormat) {
        ValidationResult result = new ValidationResult();

        if (llmOutput == null || llmOutput.isEmpty()) {
            result.setValid(false);
            result.setErrorMessage("输出为空");
            return result;
        }

        try {
            // 根据expectedFormat进行验证
            if ("JSON".equalsIgnoreCase(expectedFormat)) {
                objectMapper.readTree(llmOutput);
                result.setValid(true);
            } else if ("TEXT".equalsIgnoreCase(expectedFormat)) {
                // 文本格式验证：检查是否有内容
                if (llmOutput.trim().length() > 10) { // 简单验证
                    result.setValid(true);
                } else {
                    result.setValid(false);
                    result.setErrorMessage("文本内容太短");
                }
            } else {
                // 未知格式，假设有效
                result.setValid(true);
                result.setWarning("未知格式，跳过验证");
            }

        } catch (Exception e) {
            result.setValid(false);
            result.setErrorMessage("格式验证失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 计算输出质量评分
     */
    public double calculateOutputQuality(String llmOutput, String expectedFormat) {
        double score = 0.5; // 基础分

        if (llmOutput == null || llmOutput.isEmpty()) {
            return 0.0;
        }

        // 长度评分
        int length = llmOutput.length();
        if (length > 100) score += 0.1;
        if (length > 500) score += 0.1;
        if (length > 1000) score += 0.1;

        // 格式评分
        ValidationResult validation = validateOutput(llmOutput, expectedFormat);
        if (validation.isValid()) {
            score += 0.2;
        }

        // 结构化程度评分（检查是否有JSON或结构化标记）
        if (llmOutput.contains("{") && llmOutput.contains("}")) {
            score += 0.1;
        }

        // 限制在0-1之间
        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * 验证结果类
     */
    public static class ValidationResult {
        private boolean valid;
        private String errorMessage;
        private String warning;

        public boolean isValid() {
            return valid;
        }

        public void setValid(boolean valid) {
            this.valid = valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }

        public String getWarning() {
            return warning;
        }

        public void setWarning(String warning) {
            this.warning = warning;
        }
    }
}