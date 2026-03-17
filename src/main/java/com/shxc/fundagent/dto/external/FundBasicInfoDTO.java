package com.shxc.fundagent.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 基金基本信息DTO（对应天天基金网基金详情API返回格式）
 */
@Data
public class FundBasicInfoDTO {

    /**
     * 基金代码
     */
    @JsonProperty("fundcode")
    private String fundCode;

    /**
     * 基金名称
     */
    @JsonProperty("name")
    private String name;

    /**
     * 基金类型
     */
    @JsonProperty("fundtype")
    private String fundType;

    /**
     * 基金公司
     */
    @JsonProperty("company")
    private String company;

    /**
     * 成立日期
     */
    @JsonProperty("establishdate")
    private String establishDate;

    /**
     * 基金规模（亿元）
     */
    @JsonProperty("scale")
    private String scale;

    /**
     * 基金经理
     */
    @JsonProperty("manager")
    private String manager;

    /**
     * 管理费率（%）
     */
    @JsonProperty("managementfee")
    private String managementFee;

    /**
     * 托管费率（%）
     */
    @JsonProperty("custodyfee")
    private String custodyFee;

    /**
     * 业绩比较基准
     */
    @JsonProperty("benchmark")
    private String benchmark;

    /**
     * 投资目标
     */
    @JsonProperty("investmenttarget")
    private String investmentTarget;

    /**
     * 投资范围
     */
    @JsonProperty("investmentscope")
    private String investmentScope;

    /**
     * 风险等级：1-低风险，2-中低风险，3-中风险，4-中高风险，5-高风险
     */
    @JsonProperty("risklevel")
    private String riskLevel;

    @JsonProperty( "syl_1n")
    private String syl1n;

    @JsonProperty("syl_6y")
    private String syl6y;

    @JsonProperty("syl_3y")
    private String syl3y;

    @JsonProperty("syl_1y")
    private String syl1y;

    /**
     * 获取成立日期（LocalDate类型）
     */
    public LocalDate getEstablishDateAsLocalDate() {
        try {
            return LocalDate.parse(establishDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取基金规模（BigDecimal类型，单位：亿元）
     */
    public BigDecimal getScaleAsBigDecimal() {
        try {
            // 移除"亿元"单位
            String scaleStr = scale.replace("亿元", "").trim();
            return new BigDecimal(scaleStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取管理费率（BigDecimal类型，百分比）
     */
    public BigDecimal getManagementFeeAsBigDecimal() {
        try {
            // 移除"%"单位
            String feeStr = managementFee.replace("%", "").trim();
            return new BigDecimal(feeStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取托管费率（BigDecimal类型，百分比）
     */
    public BigDecimal getCustodyFeeAsBigDecimal() {
        try {
            // 移除"%"单位
            String feeStr = custodyFee.replace("%", "").trim();
            return new BigDecimal(feeStr);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取风险等级（Integer类型）
     */
    public Integer getRiskLevelAsInteger() {
        try {
            if (riskLevel == null || riskLevel.isEmpty()) {
                return 3; // 默认中风险
            }

            // 根据中文描述转换为等级
            switch (riskLevel) {
                case "低风险": return 1;
                case "中低风险": return 2;
                case "中风险": return 3;
                case "中高风险": return 4;
                case "高风险": return 5;
                default: return 3; // 默认中风险
            }
        } catch (Exception e) {
            return 3; // 默认中风险
        }
    }

    /**
     * 根据基金类型字符串转换为枚举类型
     */
    public String getFundTypeNormalized(String fundName) {
        if (fundName == null) {
            return "OTHER";
        }

        // 优先级 1：指数型（包含"指数"或"中证"、"上证"等指数特征词）
        if (fundName.contains("指数") || fundName.contains("中证")
                || fundName.contains("上证") || fundName.contains("深证") || fundName.contains("ETF")) {
            return "INDEX";
        } else if (fundName.contains("股票")) {
            return "STOCK";
        } else if (fundName.contains("混合")) {
            return "MIXED";
        } else if (fundName.contains("债券")) {
            return "BOND";
        } else if (fundName.contains("货币")) {
            return "MONEY_MARKET";
        } else if (fundName.contains("QDII")) {
            return "QDII";
        } else {
            return "OTHER";
        }
    }

    /**
     * 验证数据是否有效
     */
    public boolean isValid() {
        return fundCode != null && !fundCode.isEmpty() && name != null && !name.isEmpty();
    }
}