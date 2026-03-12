package com.shxc.fundagent.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 基金实时数据DTO（对应天天基金网API返回格式）
 * API示例：https://fundgz.1234567.com.cn/js/001210.js?rt=1463558676000
 * 返回格式：jsonpgz({...});
 */
@Data
public class FundRealTimeDataDTO {

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
     * 单位净值
     */
    @JsonProperty("dwjz")
    private String netValue;

    /**
     * 估算净值
     */
    @JsonProperty("gsz")
    private String estimateValue;

    /**
     * 估算涨跌幅
     */
    @JsonProperty("gszzl")
    private String estimateChangeRate;

    /**
     * 估算时间
     */
    @JsonProperty("gztime")
    private String estimateTime;

    /**
     * 净值日期
     */
    @JsonProperty("jzrq")
    private String netValueDate;

    /**
     * 获取净值（BigDecimal类型）
     */
    public BigDecimal getNetValueAsBigDecimal() {
        try {
            return new BigDecimal(netValue);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取估算净值（BigDecimal类型）
     */
    public BigDecimal getEstimateValueAsBigDecimal() {
        try {
            return new BigDecimal(estimateValue);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取估算涨跌幅（BigDecimal类型，百分比）
     */
    public BigDecimal getEstimateChangeRateAsBigDecimal() {
        try {
            return new BigDecimal(estimateChangeRate);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取净值日期（LocalDateTime类型）
     */
    public LocalDate getNetValueDateAsLocalDate() {
        try {
            // 格式：2024-03-08
            return LocalDate.parse(netValueDate,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取估算时间（LocalDateTime类型）
     */
    public LocalDateTime getEstimateTimeAsLocalDateTime() {
        try {
            // 格式：2024-03-08 14:30
            return LocalDateTime.parse(estimateTime,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 验证数据是否有效
     */
    public boolean isValid() {
        return fundCode != null && !fundCode.isEmpty() &&
               (getNetValueAsBigDecimal() != null || getEstimateValueAsBigDecimal() != null);
    }

    /**
     * 获取数据质量描述
     */
    public String getDataQuality() {
        if (getNetValueAsBigDecimal() != null) {
            return "HIGH"; // 有净值数据，高质量
        } else if (getEstimateValueAsBigDecimal() != null) {
            return "MEDIUM"; // 只有估值数据，中等质量
        } else {
            return "LOW"; // 无有效数据，低质量
        }
    }
}