package com.shxc.fundagent.llm;

/**
 * LLM提供商类型枚举
 */
public enum ProviderType {
    /**
     * OpenAI提供的模型（GPT系列）
     */
    OPENAI("openai"),

    /**
     * Anthropic提供的模型（Claude系列）
     */
    ANTHROPIC("anthropic"),

    /**
     * 本地部署的开源模型
     */
    LOCAL("local"),

    /**
     * 阿里云通义千问
     */
    ALIYUN_QWEN("aliyun_qwen"),

    /**
     * 百度文心一言
     */
    BAIDU_ERNIE("baidu_ernie"),

    /**
     * 腾讯混元
     */
    TENCENT_HUNYUAN("tencent_hunyuan"),

    /**
     * 智谱AI
     */
    ZHIPU_AI("zhipu_ai"),

    /**
     * 月之暗面（Kimi）
     */
    MOONSHOT("moonshot"),

    /**
     * 零一万物（Yi系列）
     */
    ZERO_ONE_WORLD("zero_one_world"),

    /**
     * 深度求索（DeepSeek）
     */
    DEEPSEEK("deepseek"),

    /**
     * 其他自定义提供商
     */
    CUSTOM("custom");

    private final String code;

    ProviderType(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /**
     * 根据编码获取枚举值
     */
    public static ProviderType fromCode(String code) {
        for (ProviderType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return CUSTOM;
    }
}