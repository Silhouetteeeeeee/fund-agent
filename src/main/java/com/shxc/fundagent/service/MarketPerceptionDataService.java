package com.shxc.fundagent.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shxc.fundagent.agent.model.AgentResult;
import com.shxc.fundagent.agent.model.v2.MarketContext;
import com.shxc.fundagent.entity.MarketPerceptionData;
import com.shxc.fundagent.repository.MarketPerceptionDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 市场感知数据服务
 * 负责存储和查询市场环境感知数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MarketPerceptionDataService {

    private final MarketPerceptionDataRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * 保存市场感知数据
     *
     * @param result Agent执行结果
     * @return 保存的实体
     */
    @Transactional
    public MarketPerceptionData saveMarketPerceptionData(AgentResult result) {
        if (result == null || !result.isSuccess()) {
            log.warn("Agent结果为空或执行失败，无法保存市场感知数据");
            return null;
        }

        try {
            // 从AgentResult中提取数据
            Map<String, Object> extraData = result.getExtraData();
            if (extraData == null) {
                log.warn("Agent结果中无额外数据");
                return null;
            }

            MarketContext marketContext = (MarketContext) extraData.get("marketContext");
            if (marketContext == null) {
                log.warn("Agent结果中无市场上下文数据");
                return null;
            }

            // 构建实体
            MarketPerceptionData data = buildMarketPerceptionData(marketContext, extraData);

            // 检查是否已存在同一天的记录
            Optional<MarketPerceptionData> existing = repository.findByMarketDate(data.getMarketDate());
            if (existing.isPresent()) {
                // 更新现有记录
                data.setId(existing.get().getId());
                log.info("更新市场感知数据: {}", data.getMarketDate());
            } else {
                log.info("保存新的市场感知数据: {}", data.getMarketDate());
            }

            return repository.save(data);

        } catch (Exception e) {
            log.error("保存市场感知数据失败", e);
            return null;
        }
    }

    /**
     * 构建市场感知数据实体
     */
    private MarketPerceptionData buildMarketPerceptionData(MarketContext context, Map<String, Object> extraData) {
        MarketPerceptionData.MarketPerceptionDataBuilder builder = MarketPerceptionData.builder();

        // 基本信息
        builder.marketDate(context.getMarketDate() != null ? context.getMarketDate() : LocalDate.now());
        builder.collectionTime(context.getTimestamp() != null ? context.getTimestamp() : LocalDateTime.now());
        builder.dataSource((String) extraData.get("dataSource"));

        // 市场整体状态
        builder.marketStatus(context.getMarketStatus() != null ? context.getMarketStatus().name() : null);
        builder.riskLevel(context.getRiskLevel() != null ? context.getRiskLevel().name() : null);
        builder.sentimentScore(context.getSentimentScore());
        builder.sentimentLevel(context.getSentimentLevel());
        builder.marketTemperature(context.getMarketTemperature());

        // 主要指数数据
        if (context.getIndexData() != null) {
            context.getIndexData().forEach((code, indexData) -> {
                if (code.contains("000001")) {
                    builder.shIndex(indexData.getCurrentValue());
                    builder.shIndexChangePct(indexData.getChangePercent());
                } else if (code.contains("399001")) {
                    builder.szIndex(indexData.getCurrentValue());
                    builder.szIndexChangePct(indexData.getChangePercent());
                } else if (code.contains("399006")) {
                    builder.cyIndex(indexData.getCurrentValue());
                    builder.cyIndexChangePct(indexData.getChangePercent());
                } else if (code.contains("000300")) {
                    builder.hs300Index(indexData.getCurrentValue());
                    builder.hs300IndexChangePct(indexData.getChangePercent());
                }
            });
        }

        // 资金流向数据
        if (context.getFundFlowData() != null) {
            MarketContext.FundFlowData fundFlow = context.getFundFlowData();
            builder.mainFundInflow(fundFlow.getMainFundInflow());
            builder.fundFlowTrend(fundFlow.getFundFlowTrend());
        }

        // 北向资金数据
        if (context.getNorthboundFlowData() != null) {
            MarketContext.NorthboundFlowData northbound = context.getNorthboundFlowData();
            builder.northboundInflow(northbound.getTotalInflow());
            builder.shanghaiInflow(northbound.getShanghaiInflow());
            builder.shenzhenInflow(northbound.getShenzhenInflow());
        }

        // 板块数据（JSON格式）
        try {
            if (context.getSectorPerformance() != null) {
                builder.sectorPerformance(objectMapper.writeValueAsString(context.getSectorPerformance()));
            }
            if (context.getConceptPerformance() != null) {
                builder.conceptPerformance(objectMapper.writeValueAsString(context.getConceptPerformance()));
            }
        } catch (JsonProcessingException e) {
            log.error("序列化板块数据失败", e);
        }

        // 估值数据
        if (context.getValuationData() != null) {
            MarketContext.ValuationData valuation = context.getValuationData();
            builder.hs300Pe(valuation.getPeRatio());
            builder.hs300Pb(valuation.getPbRatio());
            builder.valuationLevel(valuation.getValuationLevel());
        }

        // 技术指标
        if (context.getTechnicalIndicators() != null) {
            MarketContext.TechnicalIndicators tech = context.getTechnicalIndicators();
            builder.trendScore(tech.getTrendScore());
            builder.trendStrength(tech.getTrendStrength());
        }

        // 预警信息
        builder.hasWarning(context.getWarningSignals() != null && !context.getWarningSignals().isEmpty());
        try {
            if (context.getWarningSignals() != null && !context.getWarningSignals().isEmpty()) {
                builder.warningSignals(objectMapper.writeValueAsString(context.getWarningSignals()));
            }
        } catch (JsonProcessingException e) {
            log.error("序列化预警信号失败", e);
        }

        builder.marketAdvice(context.getMarketAdvice());

        // 元数据
        builder.dataPoints((Integer) extraData.get("dataPoints"));
        builder.processingTimeMs((Long) extraData.get("processingTimeMs"));

        // 原始数据（用于调试）
        try {
            builder.rawData(objectMapper.writeValueAsString(context));
        } catch (JsonProcessingException e) {
            log.error("序列化原始数据失败", e);
        }

        return builder.build();
    }

    /**
     * 根据日期查询市场感知数据
     */
    public Optional<MarketPerceptionData> findByMarketDate(LocalDate marketDate) {
        return repository.findByMarketDate(marketDate);
    }

    /**
     * 查询最新的市场感知数据
     */
    public Optional<MarketPerceptionData> findLatest() {
        return repository.findTopByOrderByMarketDateDesc();
    }

    /**
     * 查询最近N天的数据
     */
    public List<MarketPerceptionData> findLatestN(int n) {
        return repository.findLatestN(n);
    }

    /**
     * 查询日期范围内的数据
     */
    public List<MarketPerceptionData> findByDateRange(LocalDate startDate, LocalDate endDate) {
        return repository.findByMarketDateBetweenOrderByMarketDateDesc(startDate, endDate);
    }

    /**
     * 检查指定日期是否存在数据
     */
    public boolean existsByMarketDate(LocalDate marketDate) {
        return repository.existsByMarketDate(marketDate);
    }

    /**
     * 获取市场情绪趋势（最近N天）
     */
    public List<MarketPerceptionData> getSentimentTrend(int days) {
        return repository.findLatestN(days);
    }

    /**
     * 获取今日市场情绪摘要
     */
    public String getTodayMarketSummary() {
        Optional<MarketPerceptionData> today = findLatest();
        if (today.isEmpty()) {
            return "今日市场数据尚未采集";
        }

        MarketPerceptionData data = today.get();
        StringBuilder sb = new StringBuilder();
        sb.append("📊 今日市场概况 (").append(data.getMarketDate()).append(")\n");
        sb.append("市场情绪: ").append(data.getSentimentLevel())
          .append(" (").append(data.getSentimentScore()).append("/100)\n");
        sb.append("市场温度: ").append(data.getMarketTemperature()).append("\n");
        sb.append("上证指数: ").append(data.getShIndex())
          .append(" (").append(data.getShIndexChangePct() != null ? 
                  data.getShIndexChangePct().multiply(new BigDecimal("100")) : "0").append("%)\n");

        if (data.getNorthboundInflow() != null) {
            sb.append("北向资金: ").append(formatAmount(data.getNorthboundInflow())).append("\n");
        }

        if (data.getHasWarning() != null && data.getHasWarning()) {
            sb.append("⚠️ 存在市场预警，请注意风险\n");
        }

        return sb.toString();
    }

    /**
     * 格式化金额（转换为亿/万）
     */
    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        BigDecimal absAmount = amount.abs();
        if (absAmount.compareTo(new BigDecimal("100000000")) >= 0) {
            return amount.divide(new BigDecimal("100000000"), 2, BigDecimal.ROUND_HALF_UP) + "亿";
        } else if (absAmount.compareTo(new BigDecimal("10000")) >= 0) {
            return amount.divide(new BigDecimal("10000"), 2, BigDecimal.ROUND_HALF_UP) + "万";
        }
        return amount.toString();
    }
}
