package com.shxc.fundagent.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 基金历史数据DTO（对应天天基金网历史净值API返回格式）
 */
@Data
public class FundHistoryDataDTO {

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
     * 历史净值数据
     */
    @JsonProperty("data")
    private List<HistoryDataItem> data;

    /**
     * 总记录数
     */
    @JsonProperty("total")
    private Integer total;

    @Data
    public static class HistoryDataItem {

        /**
         * 净值日期
         */
        @JsonProperty("FSRQ")
        private String date;

        /**
         * 单位净值
         */
        @JsonProperty("DWJZ")
        private String netValue;

        /**
         * 累计净值
         */
        @JsonProperty("LJJZ")
        private String accumulatedNetValue;

        /**
         * 日增长率
         */
        @JsonProperty("JZZZL")
        private String dailyGrowthRate;

        /**
         * 申购状态
         */
        @JsonProperty("SGZT")
        private String purchaseStatus;

        /**
         * 赎回状态
         */
        @JsonProperty("SHZT")
        private String redemptionStatus;

        /**
         * 分红送配
         */
        @JsonProperty("FHSP")
        private String dividend;

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
         * 获取累计净值（BigDecimal类型）
         */
        public BigDecimal getAccumulatedNetValueAsBigDecimal() {
            try {
                return new BigDecimal(accumulatedNetValue);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * 获取日增长率（BigDecimal类型，百分比）
         */
        public BigDecimal getDailyGrowthRateAsBigDecimal() {
            try {
                return new BigDecimal(dailyGrowthRate);
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * 获取日期（LocalDate类型）
         */
        public LocalDate getDateAsLocalDate() {
            try {
                return LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                return null;
            }
        }

        /**
         * 验证数据是否有效
         */
        public boolean isValid() {
            return getDateAsLocalDate() != null && getNetValueAsBigDecimal() != null;
        }
    }

    /**
     * 验证数据是否有效
     */
    public boolean isValid() {
        return fundCode != null && !fundCode.isEmpty() && data != null && !data.isEmpty();
    }

    /**
     * 获取有效数据项（过滤无效数据）
     */
    public List<HistoryDataItem> getValidDataItems() {
        return data.stream()
                .filter(HistoryDataItem::isValid)
                .toList();
    }
}