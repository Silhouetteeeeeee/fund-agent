package com.shxc.fundagent.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日基金数据实体
 * 对应详细设计文档中的 fund_daily_data 表
 */
@Entity
@Table(name = "fund_daily_data", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"fund_code", "trade_date"})
})
@Data
public class FundDailyData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 基金代码
     */
    @Column(name = "fund_code", nullable = false, length = 10)
    private String fundCode;

    /**
     * 交易日
     */
    @Column(name = "trade_date", nullable = false)
    private LocalDate tradeDate;

    /**
     * 单位净值
     */
    @Column(name = "net_value", precision = 10, scale = 4)
    private BigDecimal netValue;

    /**
     * 实时估值
     */
    @Column(name = "estimate_value", precision = 10, scale = 4)
    private BigDecimal estimateValue;

    /**
     * 日涨跌幅（%）
     */
    @Column(name = "change_rate", precision = 6, scale = 2)
    private BigDecimal changeRate;

    /**
     * 成交额（万元）
     */
    @Column(name = "turnover", precision = 16, scale = 2)
    private BigDecimal turnover;

    /**
     * 换手率（%）
     */
    @Column(name = "turnover_rate", precision = 6, scale = 2)
    private BigDecimal turnoverRate;

    /**
     * 市盈率
     */
    @Column(name = "pe_ratio", precision = 10, scale = 2)
    private BigDecimal peRatio;

    /**
     * 市净率
     */
    @Column(name = "pb_ratio", precision = 10, scale = 2)
    private BigDecimal pbRatio;

    /**
     * 净值日期
     */
    @Column(name = "nav_date")
    private LocalDate navDate;

    /**
     * 数据来源
     */
    @Column(name = "data_source", length = 50)
    private String dataSource = "tianTianFund";

    /**
     * 数据质量：HIGH-高质量，MEDIUM-中等，LOW-低质量
     */
    @Column(name = "data_quality", length = 10)
    private String dataQuality = "MEDIUM";

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;

    /**
     * 无参构造函数
     */
    public FundDailyData() {
    }

    /**
     * 带基本参数的构造函数
     */
    public FundDailyData(String fundCode, LocalDate tradeDate) {
        this.fundCode = fundCode;
        this.tradeDate = tradeDate;
    }

    /**
     * 获取有效价格（优先使用净值，其次使用估值）
     */
    public BigDecimal getEffectivePrice() {
        if (netValue != null && netValue.compareTo(BigDecimal.ZERO) > 0) {
            return netValue;
        } else if (estimateValue != null && estimateValue.compareTo(BigDecimal.ZERO) > 0) {
            return estimateValue;
        }
        return null;
    }

    /**
     * 判断是否为交易日数据
     */
    public boolean isTradingDayData() {
        return tradeDate != null && getEffectivePrice().compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 获取数据质量描述
     */
    public String getDataQualityDescription() {
        if (dataQuality == null) {
            return "未知";
        }
        switch (dataQuality) {
            case "HIGH": return "高质量";
            case "MEDIUM": return "中等";
            case "LOW": return "低质量";
            default: return "未知";
        }
    }

    /**
     * 判断是否为收盘数据（有净值）
     */
    public boolean isClosingData() {
        return netValue != null && netValue.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * 判断是否为实时估值数据
     */
    public boolean isEstimateData() {
        return estimateValue != null && estimateValue.compareTo(BigDecimal.ZERO) > 0;
    }

    // ================ 兼容性方法（供其他代码使用） ================

    /**
     * 获取日涨跌幅（兼容性方法）
     */
    public BigDecimal getDailyChangeRate() {
        return changeRate;
    }

    /**
     * 设置日涨跌幅（兼容性方法）
     */
    public void setDailyChangeRate(BigDecimal dailyChangeRate) {
        this.changeRate = dailyChangeRate;
    }

    /**
     * 获取收益率（兼容性方法）
     */
    public BigDecimal getYieldRate() {
        return changeRate; // 假设changeRate就是收益率
    }

    /**
     * 设置收益率（兼容性方法）
     */
    public void setYieldRate(BigDecimal yieldRate) {
        this.changeRate = yieldRate;
    }

    /**
     * 获取净值（兼容性方法）
     */
    public BigDecimal getNetValue() {
        return netValue;
    }

    /**
     * 设置净值（兼容性方法）
     */
    public void setNetValue(BigDecimal netValue) {
        this.netValue = netValue;
    }
}