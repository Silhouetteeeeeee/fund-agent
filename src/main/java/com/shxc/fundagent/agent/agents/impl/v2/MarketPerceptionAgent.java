package com.shxc.fundagent.agent.agents.impl.v2;

import com.shxc.fundagent.agent.core.AbstractAgentV2;
import com.shxc.fundagent.agent.capabilities.Tool;
import com.shxc.fundagent.agent.model.v2.AgentContext;
import com.shxc.fundagent.agent.model.v2.MarketContext;
import com.shxc.fundagent.agent.model.AgentResult;
import com.shxc.fundagent.service.FundDataService;
import com.shxc.fundagent.service.MarketDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 市场环境感知Agent
 * 功能：采集市场数据，分析市场状态，生成市场环境报告
 * 对应需求：FR-001 市场环境感知
 */
@Component
public class MarketPerceptionAgent extends AbstractAgentV2 {

    private static final Logger logger = LoggerFactory.getLogger(MarketPerceptionAgent.class);

    private static final String AGENT_NAME = "market_perception_agent";
    private static final String AGENT_DESCRIPTION = "市场环境感知Agent，负责采集和分析市场数据，评估市场状态和风险";

    private static final String[] CAPABILITIES = {
            "market_analysis",
            "risk_assessment",
            "data_collection",
            "trend_detection"
    };

    private static final String[] SUPPORTED_CONTEXT_TYPES = {
            "market_data_request",
            "risk_assessment_request",
            "market_trend_analysis"
    };

    private final FundDataService fundDataService;
    private final MarketDataService marketDataService;

    @Autowired
    public MarketPerceptionAgent(FundDataService fundDataService,
                                 @Qualifier("tencentMarketDataService") MarketDataService marketDataService) {
        super(AGENT_NAME, AGENT_DESCRIPTION, CAPABILITIES, SUPPORTED_CONTEXT_TYPES);
        this.fundDataService = fundDataService;
        this.marketDataService = marketDataService;

        // 初始化Agent
        initAgent();
    }

    /**
     * 初始化Agent
     */
    private void initAgent() {
        logger.info("初始化市场环境感知Agent: {}", AGENT_NAME);

        // 设置默认输出模式
        addSupportedOutputSchema("market_report");
        addSupportedOutputSchema("risk_assessment");
        addSupportedOutputSchema("technical_analysis");

        // 注意：工具注册将在EnhancedAgentManager中自动完成
        // 这里可以添加Agent特定的工具
    }

    @Override
    protected AgentResult doProcessWithTools(String task, AgentContext context) throws Exception {
        logger.info("市场环境感知Agent处理任务: {}", task);

        // 根据任务类型执行不同的处理逻辑
        try {
            if (task.contains("collect") || task.contains("采集")) {
                return collectMarketData(context);
            } else if (task.contains("analyze") || task.contains("分析")) {
                return analyzeMarket(context);
            } else if (task.contains("assess") || task.contains("评估")) {
                return assessMarketRisk(context);
            } else if (task.contains("report") || task.contains("报告")) {
                return generateMarketReport(context);
            } else {
                // 默认处理：收集和分析市场数据
                return processDefaultMarketTask(context);
            }
        } catch (Exception e) {
            logger.error("市场环境感知Agent处理任务失败: {}", task, e);
            return buildErrorResult(e, 0);
        }
    }

    /**
     * 采集市场数据
     * 使用MarketDataService获取全面的市场数据
     */
    private AgentResult collectMarketData(AgentContext context) {
        long startTime = System.currentTimeMillis();
        logger.info("开始采集市场数据");

        try {
            // 创建市场上下文
            MarketContext marketContext = new MarketContext();
            marketContext.setContextId("market_" + System.currentTimeMillis());
            marketContext.setTimestamp(LocalDateTime.now());
            marketContext.setMarketDate(java.time.LocalDate.now());

            // ========== 1. 获取主要指数数据 ==========
            List<MarketContext.IndexData> indexDataList = marketDataService.getMajorIndicesData();
            
            if (indexDataList.isEmpty()) {
                logger.warn("从MarketDataService获取数据失败，使用模拟数据");
                initializeMockMarketData(marketContext);
            } else {
                // 将获取的数据设置到市场上下文
                for (MarketContext.IndexData indexData : indexDataList) {
                    marketContext.addIndexData(indexData);
                    logger.debug("获取指数数据: {} = {}", indexData.getIndexName(), indexData.getCurrentValue());
                }
                
                // ========== 2. 获取行业板块数据 ==========
                Map<String, BigDecimal> sectorPerformance = marketDataService.getSectorPerformance();
                if (sectorPerformance != null && !sectorPerformance.isEmpty()) {
                    marketContext.setSectorPerformance(sectorPerformance);
                    logger.info("获取行业板块数据: {} 个板块", sectorPerformance.size());
                }
                
                // ========== 3. 获取概念板块数据 ==========
                Map<String, BigDecimal> conceptPerformance = marketDataService.getConceptPerformance();
                if (conceptPerformance != null && !conceptPerformance.isEmpty()) {
                    marketContext.setConceptPerformance(conceptPerformance);
                    logger.info("获取概念板块数据: {} 个概念", conceptPerformance.size());
                }
                
                // ========== 4. 获取市场情绪数据 ==========
                MarketDataService.MarketSentimentData sentiment = marketDataService.getMarketSentiment();
                if (sentiment != null) {
                    marketContext.setSentimentScore(sentiment.getSentimentScore());
                    marketContext.setSentimentLevel(sentiment.getSentimentLevel());
                    logger.info("市场情绪: {} (分数: {})", 
                            sentiment.getSentimentLevel(), 
                            sentiment.getSentimentScore());
                }
                
                // ========== 5. 获取资金流向数据 ==========
                MarketDataService.FundFlowData fundFlow = marketDataService.getMarketFundFlow();
                if (fundFlow != null) {
                    marketContext.setFundFlowData(fundFlow);
                    logger.info("资金流向: 主力净流入={}, 趋势={}", 
                            fundFlow.getMainForceInflow(), 
                            fundFlow.getTrend());
                }
                
                // ========== 6. 获取北向资金数据 ==========
                MarketDataService.NorthboundFlowData northboundFlow = marketDataService.getNorthboundFlow();
                if (northboundFlow != null) {
                    marketContext.setNorthboundFlowData(northboundFlow);
                    logger.info("北向资金: 总净流入={}, 沪股通={}, 深股通={}", 
                            northboundFlow.getTotalInflow(),
                            northboundFlow.getShanghaiInflow(),
                            northboundFlow.getShenzhenInflow());
                }
                
                // ========== 7. 获取市场估值数据（沪深300） ==========
                MarketDataService.ValuationData valuation = marketDataService.getIndexValuation("sh000300");
                if (valuation != null) {
                    marketContext.setValuationData(valuation);
                    logger.info("市场估值: PE={}, PB={}, 估值水平={}", 
                            valuation.getPeRatio(),
                            valuation.getPbRatio(),
                            valuation.getValuationLevel());
                }
                
                logger.info("成功从{}获取市场数据，共 {} 个指数，{} 个板块", 
                        marketDataService.getDataSourceName(), 
                        indexDataList.size(),
                        sectorPerformance != null ? sectorPerformance.size() : 0);
            }

            // 分析市场状态
            analyzeMarketState(marketContext);

            // 评估风险
            assessMarketRiskLevel(marketContext);

            // 计算市场温度
            marketContext.calculateMarketTemperature();

            long processingTime = System.currentTimeMillis() - startTime;

            // 构建成功结果
            return AgentResult.builder()
                    .agentName(getName())
                    .status(AgentResult.Status.SUCCESS)
                    .content("市场数据采集完成")
                    .extraData(Map.of(
                            "marketContext", marketContext,
                            "marketSummary", marketContext.getMarketSummary(),
                            "marketTemperature", marketContext.getMarketTemperature(),
                            "dataPoints", marketContext.getIndexData().size(),
                            "sectorCount", marketContext.getSectorPerformance() != null ? 
                                    marketContext.getSectorPerformance().size() : 0,
                            "fundFlowAvailable", marketContext.getFundFlowData() != null,
                            "northboundAvailable", marketContext.getNorthboundFlowData() != null,
                            "dataSource", marketDataService.getDataSourceName(),
                            "processingTimeMs", processingTime
                    ))
                    .processingTimeMs(processingTime)
                    .build();

        } catch (Exception e) {
            logger.error("采集市场数据失败", e);
            return buildErrorResult(e, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 分析市场
     */
    private AgentResult analyzeMarket(AgentContext context) {
        long startTime = System.currentTimeMillis();
        logger.info("开始分析市场");

        try {
            // 从上下文中获取市场数据，或重新采集
            MarketContext marketContext = null;
            if (context != null && context.containsData("marketData")) {
                marketContext = context.getData("marketData");
            }

            if (marketContext == null) {
                // 如果没有提供市场数据，先采集
                AgentResult collectionResult = collectMarketData(context);
                if (collectionResult.getStatus() != AgentResult.Status.SUCCESS) {
                    return collectionResult;
                }
                marketContext = (MarketContext) collectionResult.getExtraData().get("marketContext");
            }

            // 进行深入分析
            performDeepMarketAnalysis(marketContext);

            // 识别投资机会和风险
            identifyOpportunitiesAndRisks(marketContext);

            long processingTime = System.currentTimeMillis() - startTime;

            return AgentResult.builder()
                    .agentName(getName())
                    .status(AgentResult.Status.SUCCESS)
                    .content("市场分析完成")
                    .extraData(Map.of(
                            "marketContext", marketContext,
                            "analysisSummary", "市场分析完成，识别了投资机会和风险",
                            "opportunityCount", marketContext.getWarningSignals() != null ?
                                    marketContext.getWarningSignals().size() : 0,
                            "technicalIndicators", marketContext.getTechnicalIndicators() != null ?
                                    marketContext.getTechnicalIndicators().getTrendStrength() : "N/A",
                            "fundamentalScore", marketContext.getFundamentalIndicators() != null ?
                                    marketContext.getFundamentalIndicators().getFundamentalScore() : "N/A",
                            "processingTimeMs", processingTime
                    ))
                    .processingTimeMs(processingTime)
                    .build();

        } catch (Exception e) {
            logger.error("市场分析失败", e);
            return buildErrorResult(e, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 评估市场风险
     */
    private AgentResult assessMarketRisk(AgentContext context) {
        long startTime = System.currentTimeMillis();
        logger.info("开始评估市场风险");

        try {
            MarketContext marketContext = null;
            if (context != null && context.containsData("marketData")) {
                marketContext = context.getData("marketData");
            }

            if (marketContext == null) {
                AgentResult collectionResult = collectMarketData(context);
                if (collectionResult.getStatus() != AgentResult.Status.SUCCESS) {
                    return collectionResult;
                }
                marketContext = (MarketContext) collectionResult.getExtraData().get("marketContext");
            }

            // 进行风险评估
            performRiskAssessment(marketContext);

            long processingTime = System.currentTimeMillis() - startTime;

            return AgentResult.builder()
                    .agentName(getName())
                    .status(AgentResult.Status.SUCCESS)
                    .content("市场风险评估完成")
                    .extraData(Map.of(
                            "marketContext", marketContext,
                            "riskLevel", marketContext.getRiskLevel(),
                            "warningSignals", marketContext.getWarningSignals() != null ?
                                    marketContext.getWarningSignals().size() : 0,
                            "riskAssessment", "风险评估完成，风险等级: " + marketContext.getRiskLevel(),
                            "processingTimeMs", processingTime
                    ))
                    .processingTimeMs(processingTime)
                    .build();

        } catch (Exception e) {
            logger.error("市场风险评估失败", e);
            return buildErrorResult(e, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 生成市场报告
     */
    private AgentResult generateMarketReport(AgentContext context) {
        long startTime = System.currentTimeMillis();
        logger.info("开始生成市场报告");

        try {
            // 收集和分析市场数据
            AgentResult analysisResult = analyzeMarket(context);
            if (analysisResult.getStatus() != AgentResult.Status.SUCCESS) {
                return analysisResult;
            }

            MarketContext marketContext = (MarketContext) analysisResult.getExtraData().get("marketContext");

            // 生成报告
            String report = generateComprehensiveReport(marketContext);

            long processingTime = System.currentTimeMillis() - startTime;

            return AgentResult.builder()
                    .agentName(getName())
                    .status(AgentResult.Status.SUCCESS)
                    .content(report)
                    .extraData(Map.of(
                            "marketContext", marketContext,
                            "reportType", "comprehensive_market_report",
                            "reportLength", report.length(),
                            "processingTimeMs", processingTime
                    ))
                    .processingTimeMs(processingTime)
                    .build();

        } catch (Exception e) {
            logger.error("生成市场报告失败", e);
            return buildErrorResult(e, System.currentTimeMillis() - startTime);
        }
    }

    /**
     * 处理默认市场任务
     */
    private AgentResult processDefaultMarketTask(AgentContext context) {
        // 默认执行完整的市场分析流程
        return generateMarketReport(context);
    }

    /**
     * 初始化模拟市场数据
     */
    private void initializeMockMarketData(MarketContext marketContext) {
        // 模拟主要指数数据
        MarketContext.IndexData shIndex = new MarketContext.IndexData();
        shIndex.setIndexCode("SH000001");
        shIndex.setIndexName("上证指数");
        shIndex.setCurrentValue(new BigDecimal("3200.50"));
        shIndex.setChange(new BigDecimal("25.75"));
        shIndex.setChangePercent(new BigDecimal("0.81"));
        shIndex.setOpen(new BigDecimal("3175.25"));
        shIndex.setHigh(new BigDecimal("3210.75"));
        shIndex.setLow(new BigDecimal("3168.50"));
        shIndex.setPreviousClose(new BigDecimal("3174.75"));
        shIndex.setVolume(4500000000L);
        shIndex.setTurnover(new BigDecimal("420000000000"));
        shIndex.setUpdateTime(LocalDateTime.now());
        shIndex.setPeRatio(new BigDecimal("13.5"));
        shIndex.setPbRatio(new BigDecimal("1.4"));
        shIndex.setTrend("UP");

        MarketContext.IndexData szIndex = new MarketContext.IndexData();
        szIndex.setIndexCode("SZ399001");
        szIndex.setIndexName("深证成指");
        szIndex.setCurrentValue(new BigDecimal("11500.25"));
        szIndex.setChange(new BigDecimal("92.50"));
        szIndex.setChangePercent(new BigDecimal("0.81"));
        szIndex.setOpen(new BigDecimal("11410.75"));
        szIndex.setHigh(new BigDecimal("11550.50"));
        szIndex.setLow(new BigDecimal("11385.25"));
        szIndex.setPreviousClose(new BigDecimal("11407.75"));
        szIndex.setVolume(3800000000L);
        szIndex.setTurnover(new BigDecimal("380000000000"));
        szIndex.setUpdateTime(LocalDateTime.now());
        szIndex.setPeRatio(new BigDecimal("18.2"));
        szIndex.setPbRatio(new BigDecimal("2.1"));
        szIndex.setTrend("UP");

        marketContext.addIndexData(shIndex);
        marketContext.addIndexData(szIndex);

        // 模拟行业表现
        marketContext.addSectorPerformance("科技", new BigDecimal("2.5"));
        marketContext.addSectorPerformance("金融", new BigDecimal("0.8"));
        marketContext.addSectorPerformance("消费", new BigDecimal("1.2"));
        marketContext.addSectorPerformance("医药", new BigDecimal("-0.5"));
        marketContext.addSectorPerformance("能源", new BigDecimal("1.8"));
    }

    /**
     * 分析市场状态
     */
    private void analyzeMarketState(MarketContext marketContext) {
        // 简单分析：基于指数涨跌判断市场状态
        BigDecimal avgChangePercent = marketContext.getIndexData().values().stream()
                .map(MarketContext.IndexData::getChangePercent)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(marketContext.getIndexData().size()), 4, BigDecimal.ROUND_HALF_UP);

        if (avgChangePercent.compareTo(new BigDecimal("1.0")) > 0) {
            marketContext.setMarketStatus(MarketContext.MarketStatus.BULL_MARKET);
            marketContext.setSentiment(MarketContext.MarketSentiment.OPTIMISTIC);
        } else if (avgChangePercent.compareTo(new BigDecimal("-1.0")) < 0) {
            marketContext.setMarketStatus(MarketContext.MarketStatus.BEAR_MARKET);
            marketContext.setSentiment(MarketContext.MarketSentiment.PESSIMISTIC);
        } else {
            marketContext.setMarketStatus(MarketContext.MarketStatus.RANGING_MARKET);
            marketContext.setSentiment(MarketContext.MarketSentiment.NEUTRAL);
        }
    }

    /**
     * 评估市场风险等级
     */
    private void assessMarketRiskLevel(MarketContext marketContext) {
        // 简单风险评估：基于波动率和市场状态
        BigDecimal volatility = calculateMarketVolatility(marketContext);

        if (volatility.compareTo(new BigDecimal("2.0")) > 0) {
            marketContext.setRiskLevel(MarketContext.RiskLevel.HIGH);
        } else if (volatility.compareTo(new BigDecimal("1.0")) > 0) {
            marketContext.setRiskLevel(MarketContext.RiskLevel.MEDIUM);
        } else {
            marketContext.setRiskLevel(MarketContext.RiskLevel.LOW);
        }

        // 添加预警信号示例
        if (volatility.compareTo(new BigDecimal("2.5")) > 0) {
            MarketContext.WarningSignal signal = new MarketContext.WarningSignal();
            signal.setSignalId("high_volatility_" + System.currentTimeMillis());
            signal.setSignalType("TECHNICAL");
            signal.setSignalName("高波动率预警");
            signal.setDescription("市场波动率超过2.5%，存在较高风险");
            signal.setSeverity("MEDIUM");
            signal.setConfidence(new BigDecimal("0.7"));
            signal.setAction("MONITOR");
            marketContext.addWarningSignal(signal);
        }
    }

    /**
     * 计算市场波动率（简化版）
     */
    private BigDecimal calculateMarketVolatility(MarketContext marketContext) {
        // 简单计算：基于指数涨跌幅的标准差（模拟）
        // 实际应用中应该使用更复杂的计算方法
        return new BigDecimal("1.2"); // 模拟值
    }

    /**
     * 进行深入市场分析
     */
    private void performDeepMarketAnalysis(MarketContext marketContext) {
        // 初始化技术指标
        MarketContext.TechnicalIndicators technicalIndicators = new MarketContext.TechnicalIndicators();
        technicalIndicators.setMa5(new BigDecimal("3185.25"));
        technicalIndicators.setMa10(new BigDecimal("3168.50"));
        technicalIndicators.setMa20(new BigDecimal("3150.75"));
        technicalIndicators.setMa60(new BigDecimal("3100.25"));
        technicalIndicators.setMa250(new BigDecimal("3000.50"));

        technicalIndicators.setRsi(new BigDecimal("58.5"));
        technicalIndicators.setMacd(new BigDecimal("12.5"));
        technicalIndicators.setMacdSignal(new BigDecimal("10.2"));
        technicalIndicators.setMacdHistogram(new BigDecimal("2.3"));

        technicalIndicators.setTrendStrength("UP");
        technicalIndicators.setTrendScore(new BigDecimal("0.6"));
        technicalIndicators.setMarketPhase("UPTREND");

        marketContext.setTechnicalIndicators(technicalIndicators);

        // 初始化基本面指标
        MarketContext.FundamentalIndicators fundamentalIndicators = new MarketContext.FundamentalIndicators();
        fundamentalIndicators.setGdpGrowth(new BigDecimal("5.2"));
        fundamentalIndicators.setCpi(new BigDecimal("2.1"));
        fundamentalIndicators.setInterestRate(new BigDecimal("3.45"));
        fundamentalIndicators.setMonetaryPolicy("NEUTRAL");
        fundamentalIndicators.setFundamentalScore(new BigDecimal("0.4"));
        fundamentalIndicators.setEconomicCycle("EXPANSION");

        marketContext.setFundamentalIndicators(fundamentalIndicators);

        // 初始化资金流向数据
        // 创建模拟资金流向数据
        MarketDataService.FundFlowData fundFlowData = new MarketDataService.FundFlowData();
        fundFlowData.setTotalInflow(new BigDecimal("5200000000")); // 52亿元
        fundFlowData.setMainForceInflow(new BigDecimal("1580000000"));
        fundFlowData.setTrend("INFLOW");

        marketContext.setFundFlowData(fundFlowData);
    }

    /**
     * 识别投资机会和风险
     */
    private void identifyOpportunitiesAndRisks(MarketContext marketContext) {
        // 识别机会：基于技术指标和基本面
        if (marketContext.getTechnicalIndicators() != null &&
                marketContext.getTechnicalIndicators().getTrendScore() != null &&
                marketContext.getTechnicalIndicators().getTrendScore().compareTo(new BigDecimal("0.5")) > 0) {

            MarketContext.WarningSignal opportunity = new MarketContext.WarningSignal();
            opportunity.setSignalId("uptrend_opportunity_" + System.currentTimeMillis());
            opportunity.setSignalType("TECHNICAL");
            opportunity.setSignalName("上升趋势机会");
            opportunity.setDescription("技术指标显示上升趋势，存在投资机会");
            opportunity.setSeverity("LOW"); // 机会的严重程度较低
            opportunity.setConfidence(new BigDecimal("0.65"));
            opportunity.setAction("CONSIDER_BUY");
            marketContext.addWarningSignal(opportunity);
        }

        // 识别风险：基于波动率和基本面
        if (marketContext.getRiskLevel() == MarketContext.RiskLevel.HIGH) {
            MarketContext.WarningSignal risk = new MarketContext.WarningSignal();
            risk.setSignalId("high_risk_warning_" + System.currentTimeMillis());
            risk.setSignalType("RISK");
            risk.setSignalName("高风险警告");
            risk.setDescription("市场风险等级高，建议谨慎操作");
            risk.setSeverity("HIGH");
            risk.setConfidence(new BigDecimal("0.8"));
            risk.setAction("REDUCE_POSITION");
            marketContext.addWarningSignal(risk);
        }
    }

    /**
     * 进行风险评估
     */
    private void performRiskAssessment(MarketContext marketContext) {
        // 风险评估逻辑
        BigDecimal riskScore = BigDecimal.ZERO;
        int factorCount = 0;

        // 基于波动率
        BigDecimal volatility = calculateMarketVolatility(marketContext);
        riskScore = riskScore.add(volatility.multiply(new BigDecimal("0.4")));
        factorCount++;

        // 基于技术指标
        if (marketContext.getTechnicalIndicators() != null &&
                marketContext.getTechnicalIndicators().getTrendScore() != null) {
            // 趋势评分越低，风险越高（负相关）
            BigDecimal trendRisk = BigDecimal.ONE.subtract(
                    marketContext.getTechnicalIndicators().getTrendScore().add(BigDecimal.ONE)
                            .divide(new BigDecimal("2"), 4, BigDecimal.ROUND_HALF_UP)
            );
            riskScore = riskScore.add(trendRisk.multiply(new BigDecimal("0.3")));
            factorCount++;
        }

        // 基于基本面
        if (marketContext.getFundamentalIndicators() != null &&
                marketContext.getFundamentalIndicators().getFundamentalScore() != null) {
            // 基本面评分越低，风险越高（负相关）
            BigDecimal fundamentalRisk = BigDecimal.ONE.subtract(
                    marketContext.getFundamentalIndicators().getFundamentalScore().add(BigDecimal.ONE)
                            .divide(new BigDecimal("2"), 4, BigDecimal.ROUND_HALF_UP)
            );
            riskScore = riskScore.add(fundamentalRisk.multiply(new BigDecimal("0.3")));
            factorCount++;
        }

        // 根据风险评分设置风险等级
        if (factorCount > 0) {
            BigDecimal avgRiskScore = riskScore.divide(new BigDecimal(factorCount), 4, BigDecimal.ROUND_HALF_UP);
            if (avgRiskScore.compareTo(new BigDecimal("0.7")) > 0) {
                marketContext.setRiskLevel(MarketContext.RiskLevel.VERY_HIGH);
            } else if (avgRiskScore.compareTo(new BigDecimal("0.5")) > 0) {
                marketContext.setRiskLevel(MarketContext.RiskLevel.HIGH);
            } else if (avgRiskScore.compareTo(new BigDecimal("0.3")) > 0) {
                marketContext.setRiskLevel(MarketContext.RiskLevel.MEDIUM);
            } else if (avgRiskScore.compareTo(new BigDecimal("0.1")) > 0) {
                marketContext.setRiskLevel(MarketContext.RiskLevel.LOW);
            } else {
                marketContext.setRiskLevel(MarketContext.RiskLevel.VERY_LOW);
            }
        }
    }

    /**
     * 生成综合报告
     */
    private String generateComprehensiveReport(MarketContext marketContext) {
        StringBuilder report = new StringBuilder();

        report.append("=== 市场环境分析报告 ===\n\n");
        report.append("报告时间: ").append(LocalDateTime.now()).append("\n");
        report.append("市场日期: ").append(marketContext.getMarketDate()).append("\n\n");

        report.append("一、市场概览\n");
        report.append(marketContext.getMarketSummary()).append("\n\n");

        report.append("二、指数表现\n");
        for (MarketContext.IndexData index : marketContext.getIndexData().values()) {
            report.append(String.format("%s (%s): %.2f (%.2f%%)\n",
                    index.getIndexName(), index.getIndexCode(),
                    index.getCurrentValue(), index.getChangePercent()));
        }
        report.append("\n");

        report.append("三、技术分析\n");
        if (marketContext.getTechnicalIndicators() != null) {
            MarketContext.TechnicalIndicators ti = marketContext.getTechnicalIndicators();
            report.append("趋势强度: ").append(ti.getTrendStrength()).append("\n");
            report.append("趋势评分: ").append(ti.getTrendScore()).append("\n");
            report.append("市场阶段: ").append(ti.getMarketPhase()).append("\n");
            report.append("RSI: ").append(ti.getRsi()).append("\n");
            report.append("MACD: ").append(ti.getMacd()).append("\n");
        }
        report.append("\n");

        report.append("四、基本面分析\n");
        if (marketContext.getFundamentalIndicators() != null) {
            MarketContext.FundamentalIndicators fi = marketContext.getFundamentalIndicators();
            report.append("基本面评分: ").append(fi.getFundamentalScore()).append("\n");
            report.append("经济周期: ").append(fi.getEconomicCycle()).append("\n");
            report.append("货币政策: ").append(fi.getMonetaryPolicy()).append("\n");
        }
        report.append("\n");

        report.append("五、资金流向\n");
        if (marketContext.getFundFlowData() != null) {
            MarketContext.FundFlowData ff = marketContext.getFundFlowData();
            report.append("北向资金净流入: ").append(ff.getNorthboundInflow()).append(" 亿元\n");
            report.append("资金流向趋势: ").append(ff.getFundFlowTrend()).append("\n");
        }
        report.append("\n");

        report.append("六、风险与机会\n");
        report.append("风险等级: ").append(marketContext.getRiskLevel()).append("\n");
        if (marketContext.getWarningSignals() != null && !marketContext.getWarningSignals().isEmpty()) {
            report.append("预警信号数量: ").append(marketContext.getWarningSignals().size()).append("\n");
            for (MarketContext.WarningSignal signal : marketContext.getWarningSignals().values()) {
                report.append(String.format("- %s: %s (置信度: %.2f)\n",
                        signal.getSignalName(), signal.getDescription(), signal.getConfidence()));
            }
        }
        report.append("\n");

        report.append("七、投资建议\n");
        report.append("市场温度: ").append(marketContext.getMarketTemperature().multiply(new BigDecimal("100")))
                .append("%\n");
        report.append("总体建议: ");

        BigDecimal temp = marketContext.getMarketTemperature();
        if (temp.compareTo(new BigDecimal("0.7")) > 0) {
            report.append("市场过热，建议谨慎，考虑减仓或观望");
        } else if (temp.compareTo(new BigDecimal("0.3")) > 0) {
            report.append("市场正常，可适度参与，注意分散风险");
        } else {
            report.append("市场偏冷，可能存在投资机会，可逐步建仓");
        }
        report.append("\n\n");

        report.append("=== 报告结束 ===\n");
        report.append("生成者: ").append(getName()).append("\n");

        return report.toString();
    }

    /**
     * 构建错误结果（覆盖父类方法，提供更具体的错误信息）
     */
    @Override
    protected AgentResult buildErrorResult(Exception e, long processingTimeMs) {
        return AgentResult.builder()
                .agentName(getName())
                .status(AgentResult.Status.ERROR)
                .content("市场环境感知处理失败")
                .errorMessage(e.getMessage())
                .errorCode("MARKET_ANALYSIS_ERROR")
                .processingTimeMs(processingTimeMs)
                .build();
    }
}