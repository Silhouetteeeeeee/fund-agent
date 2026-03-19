package com.shxc.fundagent.agent.capabilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示构建器
 * 负责构建结构化的LLM提示，支持变量替换和模板管理
 */
@Component
public class PromptBuilder {

    private static final Logger logger = LoggerFactory.getLogger(PromptBuilder.class);

    // 默认变量替换模式：{variable_name}
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

    /**
     * 构建提示
     * @param template 提示模板
     * @param variables 变量映射
     * @return 构建后的提示
     */
    public String buildPrompt(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            throw new IllegalArgumentException("模板不能为空");
        }

        if (variables == null || variables.isEmpty()) {
            return template;
        }

        // 使用StringBuilder进行替换
        StringBuilder result = new StringBuilder(template);

        // 查找并替换所有变量
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = variables.get(variableName);

            if (value != null) {
                // 替换所有出现的变量
                String replacement = value.toString();
                // 注意：需要处理特殊字符转义
                result = new StringBuilder(result.toString().replace("{" + variableName + "}", replacement));
            } else {
                logger.warn("提示模板中的变量 '{}' 未提供值，使用空字符串", variableName);
                result = new StringBuilder(result.toString().replace("{" + variableName + "}", ""));
            }
        }

        String builtPrompt = result.toString();
        logger.debug("提示构建完成，长度: {}", builtPrompt.length());

        return builtPrompt;
    }

    /**
     * 构建带有系统消息的完整提示
     */
    public String buildCompletePrompt(String systemMessage, String userTemplate, Map<String, Object> userVariables) {
        StringBuilder prompt = new StringBuilder();

        // 添加系统消息
        if (systemMessage != null && !systemMessage.isEmpty()) {
            prompt.append("=== 系统指令 ===\n")
                  .append(systemMessage)
                  .append("\n\n");
        }

        // 构建用户消息
        String userMessage = buildPrompt(userTemplate, userVariables);
        prompt.append("=== 用户请求 ===\n")
              .append(userMessage);

        return prompt.toString();
    }

    /**
     * 构建结构化输出提示
     */
    public String buildStructuredOutputPrompt(String taskDescription, String outputSchema, Map<String, Object> data) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请根据以下任务描述和数据，生成符合指定JSON Schema的输出。\n\n");
        prompt.append("=== 任务描述 ===\n");
        prompt.append(taskDescription).append("\n\n");

        prompt.append("=== 输入数据 ===\n");
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            prompt.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        prompt.append("\n");

        prompt.append("=== 输出格式要求 ===\n");
        prompt.append("请严格按照以下JSON Schema格式生成输出：\n");
        prompt.append(outputSchema).append("\n\n");

        prompt.append("=== 输出示例（仅供参考） ===\n");
        prompt.append("{\n");
        prompt.append("  \"summary\": \"分析总结...\",\n");
        prompt.append("  \"recommendations\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"action\": \"BUY\",\n");
        prompt.append("      \"confidence\": 0.85,\n");
        prompt.append("      \"reasoning\": \"推理过程...\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n\n");

        prompt.append("请直接输出JSON，不要包含额外的解释或标记。");

        return prompt.toString();
    }

    /**
     * 构建多步骤分析提示
     */
    public String buildMultiStepAnalysisPrompt(String analysisSteps, Map<String, Object> stepVariables) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("请按照以下步骤进行分析，并在最后提供综合结论：\n\n");

        // 构建步骤
        String[] steps = analysisSteps.split("\n");
        for (int i = 0; i < steps.length; i++) {
            String stepTemplate = steps[i];
            String builtStep = buildPrompt(stepTemplate, stepVariables);
            prompt.append("步骤 ").append(i + 1).append(": ").append(builtStep).append("\n");
        }

        prompt.append("\n=== 综合结论要求 ===\n");
        prompt.append("请基于以上分析步骤，提供一个综合结论，包括：\n");
        prompt.append("1. 主要发现\n");
        prompt.append("2. 关键建议\n");
        prompt.append("3. 风险评估\n");
        prompt.append("4. 下一步行动计划\n");

        return prompt.toString();
    }

    /**
     * 检查模板中的变量是否都已提供
     */
    public boolean validateTemplateVariables(String template, Map<String, Object> variables) {
        if (template == null || template.isEmpty()) {
            return true;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            String variableName = matcher.group(1);
            if (!variables.containsKey(variableName)) {
                logger.warn("模板变量 '{}' 未提供", variableName);
                return false;
            }
        }

        return true;
    }

    /**
     * 获取模板中的所有变量名
     */
    public java.util.List<String> extractTemplateVariables(String template) {
        java.util.List<String> variables = new java.util.ArrayList<>();

        if (template == null || template.isEmpty()) {
            return variables;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        while (matcher.find()) {
            variables.add(matcher.group(1));
        }

        return variables;
    }
}