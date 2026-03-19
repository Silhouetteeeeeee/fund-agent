package com.shxc.fundagent.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 市场感知数据实体
 * 存储市场环境感知Agent采集的市场数据
 */
@Entity
@Table(name = "market_perception_data", indexes = {
    @Index(name = "idx_market_date", columnList = "marketDate"),
    @Index(name = "idx_data_source", columnList = "dataSource"),
    @Index(name = "idx_created_at", columnList = "createdAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MarketPerceptionData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 市场日期
     */
    @Column(name = "market_date", nullable = false)
    private LocalDate marketDate;

    /**
     * 数据采集时间
     */
    @Column(name = "collection_time", nullable = false)
    private LocalDateTime collectionTime;

    /**
     * 数据来源
     */
    @Column(name = "data_source", length = 50)
    private String dataSource;

    // ========== 市场整体状态 ==========

    /**
     * 市场状态
     */
    @Column(name = "market_status", length = 50)
    private String marketStatus;

    /**
     * 风险等级
     */
    @Column(name = "risk_level", length = 50)
    private String riskLevel;

    /**
     * 市场情绪分数 (0-100)
     */
    @Column(name = "sentiment_score")
    private BigDecimal sentimentScore;

    /**
     * 市场情绪等级
     */
    @Column(name = "sentiment_level", length = 50)
    private String sentimentLevel;

    /**
     * 市场温度 (0.0-1.0)
     */
    @Column(name = "market_temperature")
    private BigDecimal marketTemperature;

    // ========== 主要指数数据 ==========

    /**
     * 上证指数
     */
    @Column(name = "sh_index")
    private BigDecimal shIndex;

    /**
     * 上证指数涨跌幅
     */
    @Column(name = "sh_index_change_pct")
    private BigDecimal shIndexChangePct;

    /**
     * 深证成指
     */
    @Column(name = "sz_index")
    private BigDecimal szIndex;

    /**
     * 深证成指涨跌幅
     */
    @Column(name = "sz_index_change_pct")
    private BigDecimal szIndexChangePct;

    /**
     * 创业板指
     */
    @Column(name = "cy_index")
    private BigDecimal cyIndex;

    /**
     * 创业板指涨跌幅
     */
    @Column(name = "cy_index_change_pct")
    private BigDecimal cyIndexChangePct;

    /**
     * 沪深300
     */
    @Column(name = "hs300_index")
    private BigDecimal hs300Index;

    /**
     * 沪深300涨跌幅
     */
    @Column(name = "hs300_index_change_pct")
    private BigDecimal hs300IndexChangePct;

    // ========== 资金流向数据 ==========

    /**
     * 主力资金净流入（元）
     */
    @Column(name = "main_fund_inflow")
    private BigDecimal mainFundInflow;

    /**
     * 北向资金净流入（元）
     */
    @Column(name = "northbound_inflow")
    private BigDecimal northboundInflow;

    /**
     * 沪股通净流入（元）
     */
    @Column(name = "shanghai_inflow")
    private BigDecimal shanghaiInflow;

    /**
     * 深股通净流入（元）
     */
    @Column(name = "shenzhen_inflow")
    private BigDecimal shenzhenInflow;

    /**
     * 资金流向趋势
     */
    @Column(name = "fund_flow_trend", length = 20)
    private String fundFlowTrend;

    // ========== 板块数据（JSON格式存储） ==========

    /**
     * 行业板块表现（JSON）
     */
    @Column(name = "sector_performance", columnDefinition = "TEXT")
    private String sectorPerformance;

    /**
     * 概念板块表现（JSON）
     */
    @Column(name = "concept_performance", columnDefinition = "TEXT")
    private String conceptPerformance;

    // ========== 估值数据 ==========

    /**
     * 沪深300 PE
     */
    @Column(name = "hs300_pe")
    private BigDecimal hs300Pe;

    /**
     * 沪深300 PB
     */
    @Column(name = "hs300_pb")
    private BigDecimal hs300Pb;

    /**
     * 估值水平
     */
    @Column(name = "valuation_level", length = 20)
    private String valuationLevel;

    // ========== 技术指标 ==========

    /**
     * 市场趋势评分 (-1.0 到 1.0)
     */
    @Column(name = "trend_score")
    private BigDecimal trendScore;

    /**
     * 市场趋势强度
     */
    @Column(name = "trend_strength", length = 50)
    private String trendStrength;

    // ========== 预警信息 ==========

    /**
     * 是否有预警信号
     */
    @Column(name = "has_warning")
    private Boolean hasWarning;

    /**
     * 预警信号详情（JSON）
     */
    @Column(name = "warning_signals", columnDefinition = "TEXT")
    private String warningSignals;

    /**
     * 市场建议
     */
    @Column(name = "market_advice", length = 500)
    private String marketAdvice;

    // ========== 元数据 ==========

    /**
     * 采集的数据点数量
     */
    @Column(name = "data_points")
    private Integer dataPoints;

    /**
     * 处理耗时（毫秒）
     */
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    /**
     * 原始数据（JSON格式，用于调试）
     */
    @Column(name = "raw_data", columnDefinition = "LONGTEXT")
    private String rawData;

    // ========== 时间戳 ==========

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
