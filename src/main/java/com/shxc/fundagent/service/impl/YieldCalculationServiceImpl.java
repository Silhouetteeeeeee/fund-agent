package com.shxc.fundagent.service.impl;

import com.shxc.fundagent.entity.FundDailyData;
import com.shxc.fundagent.entity.FundHolding;
import com.shxc.fundagent.entity.FundInfo;
import com.shxc.fundagent.enums.FundType;
import com.shxc.fundagent.repository.FundDailyDataRepository;
import com.shxc.fundagent.repository.FundHoldingRepository;
import com.shxc.fundagent.repository.FundInfoRepository;
import com.shxc.fundagent.service.FundDataService;
import com.shxc.fundagent.service.YieldCalculationService;
import com.shxc.fundagent.service.YieldCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 收益计算服务实现类
 */
@Service
@Slf4j
public class YieldCalculationServiceImpl implements YieldCalculationService {

    private final FundDataService fundDataService;
    private final FundInfoRepository fundInfoRepository;
    private final FundHoldingRepository fundHoldingRepository;
    private final FundDailyDataRepository fundDailyDataRepository;
    private final YieldCalculator yieldCalculator;

    // 无风险利率（假设为3%）
    private static final BigDecimal RISK_FREE_RATE = BigDecimal.valueOf(3.0);

    @Autowired
    public YieldCalculationServiceImpl(FundDataService fundDataService,
                                       FundInfoRepository fundInfoRepository,
                                       FundHoldingRepository fundHoldingRepository,
                                       FundDailyDataRepository fundDailyDataRepository,
                                       YieldCalculator yieldCalculator) {
        this.fundDataService = fundDataService;
        this.fundInfoRepository = fundInfoRepository;
        this.fundHoldingRepository = fundHoldingRepository;
        this.fundDailyDataRepository = fundDailyDataRepository;
        this.yieldCalculator = yieldCalculator;
        log.info("YieldCalculationService initialized");
    }

    @Override
    public FundYield calculateFundYield(String fundCode, BigDecimal specifiedCostPrice) {
        log.debug("Calculating yield for fund: {}", fundCode);

        FundYield fundYield = new FundYield();
        fundYield.setFundCode(fundCode);
        fundYield.setCalculationDate(LocalDate.now());

        try {
            // 1. 获取基金基本信息
            FundInfo fundInfo = fundDataService.getFundBasicInfo(fundCode);
            FundHolding holding = fundHoldingRepository.findAcitveHoldingByFundCode(fundCode);
            if (fundInfo == null) {
                log.warn("Fund info not found for: {}", fundCode);
                return fundYield;
            }
            fundYield.setFundName(fundInfo.getFundName());

            // 2. 获取当前价格
            BigDecimal currentPrice = fundDataService.getCurrentPrice(fundCode);
            fundYield.setCurrentPrice(currentPrice);
            if (currentPrice == null) {
                log.warn("Current Price isn null for fund: {}", fundCode);
                throw new Exception("当前价格为空");
            }

            // 3. 确定成本价格
            BigDecimal costPrice = specifiedCostPrice;
            if (costPrice == null) {
                // 使用平均持仓成本
                costPrice = holding.getCostPrice();
            }
            fundYield.setCostPrice(costPrice);

            // 4. 获取持仓信息
            if (costPrice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal yieldRate = yieldCalculator.calculateYieldRate(currentPrice, costPrice);
                fundYield.setYieldRate(yieldRate);
            } else {
                fundYield.setYieldRate(BigDecimal.ZERO);
            }
            // 5. 计算各项指标
            BigDecimal profitAmount = yieldCalculator.calculateProfitAmount(currentPrice, costPrice, holding.getHoldingAmount());
            BigDecimal holdingValue = yieldCalculator.calculateHoldingValue(currentPrice, holding.getHoldingAmount());
            fundYield.setProfitAmount(profitAmount);
            fundYield.setHoldingValue(holdingValue);
            fundYield.setTotalCost(holding.getTotalCost());

            // 6. 获取涨跌幅数据
            fundYield.setDailyChangeRate(fundDataService.getDailyChangeRate(fundCode));
            fundYield.setWeeklyChangeRate(fundDataService.getWeeklyChangeRate(fundCode));
            fundYield.setMonthlyChangeRate(fundDataService.getMonthlyChangeRate(fundCode));

            log.info("Calculated yield for fund {}: {}%", fundCode, fundYield.getYieldRate());
            return fundYield;

        } catch (Exception e) {
            log.error("Error calculating yield for fund: {}", fundCode, e);
            return fundYield;
        }
    }

    @Override
    public List<FundYield> batchCalculateFundYields(List<String> fundCodes) {
        log.debug("Batch calculating yields for {} funds", fundCodes.size());
        List<FundYield> results = new ArrayList<>();

        for (String fundCode : fundCodes) {
            try {
                FundYield fundYield = calculateFundYield(fundCode, null);
                results.add(fundYield);
            } catch (Exception e) {
                log.error("Error calculating yield for fund: {} in batch", fundCode, e);
                results.add(new FundYield()); // 添加空的占位符
            }
        }

        return results;
    }

    @Override
    public PortfolioYield calculatePortfolioYield() {
        log.debug("Calculating portfolio yield");

        PortfolioYield portfolioYield = new PortfolioYield();
        portfolioYield.setCalculationDate(LocalDate.now());

        try {
            // 1. 获取所有活跃持仓
            List<FundHolding> activeHoldings = fundHoldingRepository.findAllActiveHoldings();
            portfolioYield.setHoldingCount(activeHoldings.size());

            if (activeHoldings.isEmpty()) {
                log.info("No active holdings found");
                return initializeEmptyPortfolioYield();
            }

            // 2. 按基金代码分组
            Map<String, List<FundHolding>> holdingsByFund = activeHoldings.stream()
                    .collect(Collectors.groupingBy(FundHolding::getFundCode));

            // 3. 计算每只基金的收益
            List<FundYield> fundYields = new ArrayList<>();
            BigDecimal totalCost = BigDecimal.ZERO;
            BigDecimal totalValue = BigDecimal.ZERO;
            BigDecimal totalProfit = BigDecimal.ZERO;
            int profitableCount = 0;
            int lossCount = 0;

            for (Map.Entry<String, List<FundHolding>> entry : holdingsByFund.entrySet()) {
                String fundCode = entry.getKey();
                List<FundHolding> fundHoldings = entry.getValue();

                // 计算该基金的总持仓
                BigDecimal fundTotalCost = BigDecimal.ZERO;
                BigDecimal fundTotalAmount = BigDecimal.ZERO;

                for (FundHolding holding : fundHoldings) {
                    fundTotalCost = fundTotalCost.add(holding.getTotalCost());
                    fundTotalAmount = fundTotalAmount.add(holding.getHoldingAmount());
                }

                // 获取当前价格
                BigDecimal currentPrice = fundDataService.getCurrentPrice(fundCode);

                // 计算基金收益
                if (fundTotalAmount.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal avgCost = fundTotalCost.divide(fundTotalAmount, 4, RoundingMode.HALF_UP);
                    BigDecimal yieldRate = yieldCalculator.calculateYieldRate(currentPrice, avgCost);
                    BigDecimal profitAmount = yieldCalculator.calculateProfitAmount(currentPrice, avgCost, fundTotalAmount);
                    BigDecimal holdingValue = yieldCalculator.calculateHoldingValue(currentPrice, fundTotalAmount);

                    // 更新统计
                    totalCost = totalCost.add(fundTotalCost);
                    totalValue = totalValue.add(holdingValue);
                    totalProfit = totalProfit.add(profitAmount);

                    if (profitAmount.compareTo(BigDecimal.ZERO) >= 0) {
                        profitableCount++;
                    } else {
                        lossCount++;
                    }

                    // 创建FundYield对象
                    FundYield fundYield = new FundYield();
                    fundYield.setFundCode(fundCode);
                    fundYield.setCurrentPrice(currentPrice);
                    fundYield.setCostPrice(avgCost);
                    fundYield.setYieldRate(yieldRate);
                    fundYield.setProfitAmount(profitAmount);
                    fundYield.setHoldingValue(holdingValue);
                    fundYield.setTotalCost(fundTotalCost);
                    fundYield.setCalculationDate(LocalDate.now());

                    // 获取基金名称
                    fundInfoRepository.findByFundCode(fundCode)
                            .ifPresent(fundInfo -> fundYield.setFundName(fundInfo.getFundName()));

                    fundYields.add(fundYield);
                }
            }

            // 4. 计算整体收益率
            BigDecimal totalYieldRate = totalCost.compareTo(BigDecimal.ZERO) > 0
                    ? totalProfit.divide(totalCost, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;

            // 5. 计算整体涨跌幅
            BigDecimal dailyChange = calculatePortfolioDailyChange(fundYields);
            BigDecimal weeklyChange = calculatePortfolioWeeklyChange();
            BigDecimal monthlyChange = calculatePortfolioMonthlyChange();

            // 6. 设置结果
            portfolioYield.setTotalCost(totalCost);
            portfolioYield.setTotalValue(totalValue);
            portfolioYield.setTotalProfit(totalProfit);
            portfolioYield.setTotalYieldRate(totalYieldRate);
            portfolioYield.setDailyChange(dailyChange);
            portfolioYield.setWeeklyChange(weeklyChange);
            portfolioYield.setMonthlyChange(monthlyChange);
            portfolioYield.setProfitableCount(profitableCount);
            portfolioYield.setLossCount(lossCount);
            portfolioYield.setFundYields(fundYields);

            log.info("Calculated portfolio yield: total cost={}, total value={}, yield={}%",
                    totalCost, totalValue, totalYieldRate);
            return portfolioYield;

        } catch (Exception e) {
            log.error("Error calculating portfolio yield", e);
            return initializeEmptyPortfolioYield();
        }
    }

    @Override
    public HoldingYield calculateHoldingYield(Long holdingId) {
        log.debug("Calculating yield for holding: {}", holdingId);

        HoldingYield holdingYield = new HoldingYield();
        holdingYield.setHoldingId(holdingId);
        holdingYield.setCalculationDate(LocalDate.now());

        try {
            // 1. 获取持仓记录
            Optional<FundHolding> holdingOpt = fundHoldingRepository.findById(holdingId);
            if (holdingOpt.isEmpty()) {
                log.warn("Holding not found: {}", holdingId);
                return holdingYield;
            }

            FundHolding holding = holdingOpt.get();
            holdingYield.setHolding(holding);

            // 2. 获取基金信息
            Optional<FundInfo> fundInfoOpt = fundInfoRepository.findByFundCode(holding.getFundCode());
            fundInfoOpt.ifPresent(holdingYield::setFundInfo);

            // 3. 获取当前价格
            BigDecimal currentPrice = fundDataService.getCurrentPrice(holding.getFundCode());
            holdingYield.setCurrentPrice(currentPrice);

            // 4. 计算收益
            BigDecimal yieldRate = holding.calculateYieldRate(currentPrice);
            BigDecimal profitAmount = holding.calculateProfit(currentPrice);
            BigDecimal holdingValue = yieldCalculator.calculateHoldingValue(currentPrice, holding.getHoldingAmount());

            holdingYield.setYieldRate(yieldRate);
            holdingYield.setProfitAmount(profitAmount);
            holdingYield.setHoldingValue(holdingValue);

            // 5. 计算持有天数
            int holdingDays = (int) ChronoUnit.DAYS.between(holding.getPurchaseDate(), LocalDate.now());
            holdingYield.setHoldingDays(holdingDays);

            // 6. 计算年化收益率
            BigDecimal annualizedYield = yieldCalculator.calculateAnnualizedYield(yieldRate, holdingDays);
            holdingYield.setAnnualizedYield(annualizedYield);

            log.info("Calculated yield for holding {}: {}% (annualized: {}%)",
                    holdingId, yieldRate, annualizedYield);
            return holdingYield;

        } catch (Exception e) {
            log.error("Error calculating yield for holding: {}", holdingId, e);
            return holdingYield;
        }
    }

    @Override
    public List<HistoricalYield> calculateHistoricalYields(String fundCode, int days) {
        log.debug("Calculating historical yields for fund: {}, days: {}", fundCode, days);

        List<HistoricalYield> historicalYields = new ArrayList<>();

        try {
            // 1. 获取历史数据
            List<FundDailyData> historicalData = fundDataService.getHistoryData(fundCode, days);
            if (historicalData.isEmpty()) {
                log.warn("No historical data found for fund: {}", fundCode);
                return historicalYields;
            }

            // 按日期排序
            historicalData.sort(Comparator.comparing(FundDailyData::getTradeDate));

            // 2. 计算历史收益率
            BigDecimal cumulativeReturn = BigDecimal.ONE;
            BigDecimal maxPrice = historicalData.get(0).getEffectivePrice();

            for (int i = 0; i < historicalData.size(); i++) {
                FundDailyData currentData = historicalData.get(i);
                HistoricalYield historicalYield = new HistoricalYield();

                historicalYield.setDate(currentData.getTradeDate());
                historicalYield.setPrice(currentData.getEffectivePrice());

                // 计算日收益率
                if (i > 0) {
                    FundDailyData prevData = historicalData.get(i - 1);
                    BigDecimal prevPrice = prevData.getEffectivePrice();
                    BigDecimal currentPrice = currentData.getEffectivePrice();

                    if (prevPrice.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal dailyReturn = currentPrice.subtract(prevPrice)
                                .divide(prevPrice, 6, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100));
                        historicalYield.setDailyReturn(dailyReturn);

                        // 更新累计收益率
                        BigDecimal dailyFactor = BigDecimal.ONE.add(
                                dailyReturn.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                        cumulativeReturn = cumulativeReturn.multiply(dailyFactor);
                    }
                } else {
                    historicalYield.setDailyReturn(BigDecimal.ZERO);
                }

                // 计算累计收益率
                BigDecimal cumulativeReturnPercent = cumulativeReturn.subtract(BigDecimal.ONE)
                        .multiply(BigDecimal.valueOf(100));
                historicalYield.setCumulativeReturn(cumulativeReturnPercent);

                // 计算回撤
                if (currentData.getEffectivePrice().compareTo(maxPrice) > 0) {
                    maxPrice = currentData.getEffectivePrice();
                }

                BigDecimal drawdown = maxPrice.subtract(currentData.getEffectivePrice())
                        .divide(maxPrice, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                historicalYield.setDrawdown(drawdown);

                historicalYields.add(historicalYield);
            }

            log.info("Calculated {} historical yields for fund: {}", historicalYields.size(), fundCode);
            return historicalYields;

        } catch (Exception e) {
            log.error("Error calculating historical yields for fund: {}", fundCode, e);
            return historicalYields;
        }
    }

    @Override
    public RiskMetrics calculateRiskMetrics(String fundCode, int days) {
        log.debug("Calculating risk metrics for fund: {}, days: {}", fundCode, days);

        RiskMetrics riskMetrics = new RiskMetrics();

        try {
            // 1. 获取历史收益率数据
            List<HistoricalYield> historicalYields = calculateHistoricalYields(fundCode, days);
            if (historicalYields.size() < 2) {
                log.warn("Insufficient data for risk metrics calculation: {}", fundCode);
                return riskMetrics;
            }

            // 提取日收益率
            List<BigDecimal> dailyReturns = historicalYields.stream()
                    .skip(1) // 跳过第一个（没有日收益率）
                    .map(HistoricalYield::getDailyReturn)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (dailyReturns.isEmpty()) {
                return riskMetrics;
            }

            // 2. 计算波动率
            BigDecimal volatility = yieldCalculator.calculateVolatility(dailyReturns);
            riskMetrics.setVolatility(volatility);

            // 3. 计算最大回撤
            List<BigDecimal> prices = historicalYields.stream()
                    .map(HistoricalYield::getPrice)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            BigDecimal maxDrawdown = yieldCalculator.calculateMaxDrawdown(prices);
            riskMetrics.setMaxDrawdown(maxDrawdown);

            // 4. 计算夏普比率
            // 计算平均收益率
            BigDecimal sumReturns = dailyReturns.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgReturn = sumReturns.divide(
                    BigDecimal.valueOf(dailyReturns.size()), 4, RoundingMode.HALF_UP);

            BigDecimal sharpeRatio = yieldCalculator.calculateSharpeRatio(avgReturn, RISK_FREE_RATE, volatility);
            riskMetrics.setSharpeRatio(sharpeRatio);

            // 5. 计算预期收益率（使用历史平均）
            riskMetrics.setExpectedReturn(avgReturn);

            // 6. 计算VaR（95%）
            // 简单实现：排序后取第5百分位数
            List<BigDecimal> sortedReturns = new ArrayList<>(dailyReturns);
            Collections.sort(sortedReturns);

            int varIndex = (int) Math.floor(sortedReturns.size() * 0.05);
            BigDecimal var95 = varIndex < sortedReturns.size() ? sortedReturns.get(varIndex) : BigDecimal.ZERO;
            riskMetrics.setVar95(var95);

            // 7. 计算偏度和峰度（简化实现）
            // 这里使用简单的近似计算
            riskMetrics.setSkewness(calculateSkewness(dailyReturns));
            riskMetrics.setKurtosis(calculateKurtosis(dailyReturns));

            log.info("Calculated risk metrics for fund {}: volatility={}%, maxDrawdown={}%, sharpe={}",
                    fundCode, volatility, maxDrawdown, sharpeRatio);
            return riskMetrics;

        } catch (Exception e) {
            log.error("Error calculating risk metrics for fund: {}", fundCode, e);
            return riskMetrics;
        }
    }

    @Override
    public AssetAllocationAnalysis analyzeAssetAllocation() {
        log.debug("Analyzing asset allocation");

        AssetAllocationAnalysis analysis = new AssetAllocationAnalysis();

        try {
            // 1. 获取所有活跃持仓
            List<FundHolding> activeHoldings = fundHoldingRepository.findAllActiveHoldings();
            if (activeHoldings.isEmpty()) {
                log.info("No active holdings for asset allocation analysis");
                return analysis;
            }

            // 2. 按基金代码分组，获取基金信息
            Map<String, BigDecimal> fundValues = new HashMap<>();
            BigDecimal totalValue = BigDecimal.ZERO;

            for (FundHolding holding : activeHoldings) {
                // 获取当前价格
                BigDecimal currentPrice = fundDataService.getCurrentPrice(holding.getFundCode());
                BigDecimal holdingValue = yieldCalculator.calculateHoldingValue(currentPrice, holding.getHoldingAmount());

                fundValues.put(holding.getFundCode(), holdingValue);
                totalValue = totalValue.add(holdingValue);
            }

            if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
                return analysis;
            }

            // 3. 按基金类型分配
            Map<String, BigDecimal> allocationByType = new HashMap<>();
            Map<String, BigDecimal> allocationByRisk = new HashMap<>();

            for (Map.Entry<String, BigDecimal> entry : fundValues.entrySet()) {
                String fundCode = entry.getKey();
                BigDecimal fundValue = entry.getValue();
                BigDecimal weight = fundValue.divide(totalValue, 4, RoundingMode.HALF_UP);

                // 获取基金信息
                Optional<FundInfo> fundInfoOpt = fundInfoRepository.findByFundCode(fundCode);
                if (fundInfoOpt.isPresent()) {
                    FundInfo fundInfo = fundInfoOpt.get();

                    // 按基金类型
                    String fundType = fundInfo.getFundType().name();
                    allocationByType.merge(fundType, weight, BigDecimal::add);

                    // 按风险等级
                    String riskLevel = fundInfo.getRiskLevelDescription();
                    allocationByRisk.merge(riskLevel, weight, BigDecimal::add);
                }
            }

            analysis.setAllocationByType(allocationByType);
            analysis.setAllocationByRisk(allocationByRisk);

            // 4. 计算集中度
            Map<String, BigDecimal> concentration = new HashMap<>();
            for (Map.Entry<String, BigDecimal> entry : fundValues.entrySet()) {
                String fundCode = entry.getKey();
                BigDecimal weight = entry.getValue().divide(totalValue, 4, RoundingMode.HALF_UP);
                concentration.put(fundCode, weight);
            }
            analysis.setConcentration(concentration);

            // 5. 计算分散化评分
            BigDecimal diversificationScore = calculateDiversificationScore(concentration);
            analysis.setDiversificationScore(diversificationScore);

            // 6. 生成配置建议
            List<String> recommendations = generateAllocationRecommendations(allocationByType, allocationByRisk);
            analysis.setRecommendations(recommendations);

            log.info("Asset allocation analysis completed: {} funds, total value={}",
                    fundValues.size(), totalValue);
            return analysis;

        } catch (Exception e) {
            log.error("Error analyzing asset allocation", e);
            return analysis;
        }
    }

    // 其他方法实现...

    @Override
    public YieldComparison compareYields(List<String> fundCodes, int days) {
        // 实现收益对比
        return new YieldComparison();
    }

    @Override
    public YieldTrend calculateYieldTrend(String fundCode, LocalDate startDate, LocalDate endDate) {
        // 实现收益趋势计算
        return new YieldTrend();
    }

    @Override
    public YieldSummary calculateYieldSummary(String fundCode) {
        // 实现收益汇总计算
        return new YieldSummary();
    }

    @Override
    @Transactional
    public BigDecimal updateHoldingValue(String fundCode) {
        log.debug("Updating holding value for fund: {}", fundCode);

        try {
            BigDecimal currentPrice = fundDataService.getCurrentPrice(fundCode);
            List<FundHolding> holdings = fundHoldingRepository.findByFundCodeAndStatus(fundCode, "ACTIVE");

            BigDecimal totalValue = BigDecimal.ZERO;

            for (FundHolding holding : holdings) {
                holding.updateHoldingValue(currentPrice);
                fundHoldingRepository.save(holding);
                totalValue = totalValue.add(holding.getHoldingValue());
            }

            log.info("Updated holding value for fund {}: {}", fundCode, totalValue);
            return totalValue;

        } catch (Exception e) {
            log.error("Error updating holding value for fund: {}", fundCode, e);
            return BigDecimal.ZERO;
        }
    }

    @Override
    @Transactional
    public int batchUpdateHoldingValues() {
        log.debug("Batch updating holding values");

        try {
            List<FundHolding> activeHoldings = fundHoldingRepository.findAllActiveHoldings();
            Map<String, BigDecimal> currentPrices = new HashMap<>();

            int updatedCount = 0;

            for (FundHolding holding : activeHoldings) {
                String fundCode = holding.getFundCode();

                // 获取当前价格（缓存以避免重复查询）
                BigDecimal currentPrice = currentPrices.get(fundCode);
                if (currentPrice == null) {
                    currentPrice = fundDataService.getCurrentPrice(fundCode);
                    currentPrices.put(fundCode, currentPrice);
                }

                // 更新持仓市值
                holding.updateHoldingValue(currentPrice);
                fundHoldingRepository.save(holding);
                updatedCount++;
            }

            log.info("Batch updated {} holding values", updatedCount);
            return updatedCount;

        } catch (Exception e) {
            log.error("Error in batch update holding values", e);
            return 0;
        }
    }

    @Override
    public List<BigDecimal> calculateProfitDistribution(BigDecimal totalAmount, List<BigDecimal> weights) {
        log.debug("Calculating profit distribution: totalAmount={}, weights={}", totalAmount, weights.size());

        List<BigDecimal> distribution = new ArrayList<>();

        try {
            // 验证权重
            if (weights == null || weights.isEmpty()) {
                return distribution;
            }

            BigDecimal totalWeight = weights.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (totalWeight.compareTo(BigDecimal.ZERO) == 0) {
                return distribution;
            }

            // 计算分配
            BigDecimal distributed = BigDecimal.ZERO;

            for (int i = 0; i < weights.size(); i++) {
                BigDecimal weight = weights.get(i);
                BigDecimal share = weight.divide(totalWeight, 6, RoundingMode.HALF_UP)
                        .multiply(totalAmount)
                        .setScale(2, RoundingMode.HALF_UP);

                // 最后一个项目处理余数
                if (i == weights.size() - 1) {
                    BigDecimal remainder = totalAmount.subtract(distributed.add(share));
                    share = share.add(remainder);
                }

                distribution.add(share);
                distributed = distributed.add(share);
            }

            log.info("Profit distribution calculated: {} shares", distribution.size());
            return distribution;

        } catch (Exception e) {
            log.error("Error calculating profit distribution", e);
            return distribution;
        }
    }

    @Override
    public ValidationResult validateYieldData(String fundCode) {
        log.debug("Validating yield data for fund: {}", fundCode);

        ValidationResult result = new ValidationResult();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<String> suggestions = new ArrayList<>();

        try {
            // 1. 检查基金是否存在
            Optional<FundInfo> fundInfoOpt = fundInfoRepository.findByFundCode(fundCode);
            if (fundInfoOpt.isEmpty()) {
                errors.add("基金不存在: " + fundCode);
                result.setValid(false);
                result.setErrors(errors);
                return result;
            }

            // 2. 检查是否有持仓数据
            List<FundHolding> holdings = fundHoldingRepository.findByFundCodeAndStatus(fundCode, "ACTIVE");
            if (holdings.isEmpty()) {
                warnings.add("基金没有活跃持仓: " + fundCode);
            }

            // 3. 检查价格数据
            BigDecimal currentPrice = fundDataService.getCurrentPrice(fundCode);
            if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
                warnings.add("当前价格不可用: " + fundCode);
            }

            // 4. 检查历史数据
            List<FundDailyData> recentData = fundDailyDataRepository
                    .findLatestByFundCode(fundCode, org.springframework.data.domain.Pageable.ofSize(5));
            if (recentData.size() < 3) {
                warnings.add("历史数据不足: " + fundCode);
            }

            // 5. 检查数据新鲜度
            if (!recentData.isEmpty()) {
                FundDailyData latestData = recentData.get(0);
                long daysSinceLastUpdate = ChronoUnit.DAYS.between(
                        latestData.getTradeDate(), LocalDate.now());

                if (daysSinceLastUpdate > 3) {
                    warnings.add("数据已过期: " + daysSinceLastUpdate + "天未更新");
                }
            }

            // 6. 生成建议
            if (holdings.size() > 1) {
                suggestions.add("考虑合并持仓以简化管理");
            }

            if (warnings.isEmpty() && errors.isEmpty()) {
                suggestions.add("数据验证通过，建议定期检查");
            }

            result.setValid(errors.isEmpty());
            result.setErrors(errors);
            result.setWarnings(warnings);
            result.setSuggestions(suggestions);

            log.info("Yield data validation for fund {}: valid={}, errors={}, warnings={}",
                    fundCode, result.isValid(), errors.size(), warnings.size());
            return result;

        } catch (Exception e) {
            log.error("Error validating yield data for fund: {}", fundCode, e);
            errors.add("验证过程出错: " + e.getMessage());
            result.setValid(false);
            result.setErrors(errors);
            return result;
        }
    }

    @Override
    public Map<String, Object> getYieldStatistics() {
        log.debug("Getting yield statistics");

        Map<String, Object> stats = new HashMap<>();

        try {
            // 1. 持仓统计
            List<FundHolding> activeHoldings = fundHoldingRepository.findAllActiveHoldings();
            int holdingCount = activeHoldings.size();

            // 2. 收益统计
            PortfolioYield portfolioYield = calculatePortfolioYield();
            int profitableCount = portfolioYield.getProfitableCount();
            int lossCount = portfolioYield.getLossCount();

            // 3. 资产统计
            BigDecimal totalCost = portfolioYield.getTotalCost();
            BigDecimal totalValue = portfolioYield.getTotalValue();
            BigDecimal totalProfit = portfolioYield.getTotalProfit();
            BigDecimal totalYieldRate = portfolioYield.getTotalYieldRate();

            // 4. 基金类型分布
            Map<String, Long> typeDistribution = new HashMap<>();
            for (FundHolding holding : activeHoldings) {
                fundInfoRepository.findByFundCode(holding.getFundCode())
                        .ifPresent(fundInfo -> {
                            String fundType = fundInfo.getFundType().name();
                            typeDistribution.merge(fundType, 1L, Long::sum);
                        });
            }

            // 5. 风险等级分布
            Map<String, Long> riskDistribution = new HashMap<>();
            for (FundHolding holding : activeHoldings) {
                fundInfoRepository.findByFundCode(holding.getFundCode())
                        .ifPresent(fundInfo -> {
                            String riskLevel = fundInfo.getRiskLevelDescription();
                            riskDistribution.merge(riskLevel, 1L, Long::sum);
                        });
            }

            // 组装统计信息
            stats.put("holdingCount", holdingCount);
            stats.put("profitableCount", profitableCount);
            stats.put("lossCount", lossCount);
            stats.put("totalCost", totalCost);
            stats.put("totalValue", totalValue);
            stats.put("totalProfit", totalProfit);
            stats.put("totalYieldRate", totalYieldRate);
            stats.put("typeDistribution", typeDistribution);
            stats.put("riskDistribution", riskDistribution);
            stats.put("calculationDate", LocalDate.now());
            stats.put("dataSource", "FundAgent System");

            log.info("Yield statistics generated: {} holdings, total yield={}%",
                    holdingCount, totalYieldRate);
            return stats;

        } catch (Exception e) {
            log.error("Error getting yield statistics", e);
            stats.put("error", e.getMessage());
            return stats;
        }
    }

    @Override
    @Transactional
    public int recalculateAllYields() {
        log.info("Recalculating all yields");

        try {
            int recalculatedCount = 0;

            // 1. 更新所有持仓市值
            recalculatedCount += batchUpdateHoldingValues();

            // 2. 重新计算所有基金的收益（可选）
            // 这里可以根据需要实现具体的重新计算逻辑

            log.info("Recalculated {} yields", recalculatedCount);
            return recalculatedCount;

        } catch (Exception e) {
            log.error("Error recalculating all yields", e);
            return 0;
        }
    }

    @Override
    public String exportYieldData(String fundCode, String format) {
        log.debug("Exporting yield data for fund: {}, format: {}", fundCode, format);

        try {
            // 这里实现数据导出逻辑
            // 可以根据format参数导出为CSV、JSON、Excel等格式

            StringBuilder exportData = new StringBuilder();

            if ("CSV".equalsIgnoreCase(format)) {
                // CSV格式导出
                FundYield fundYield = calculateFundYield(fundCode, null);
                exportData.append("基金代码,基金名称,当前价格,成本价格,收益率(%),收益金额,持仓市值,总成本,计算日期\n");
                exportData.append(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        fundYield.getFundCode(),
                        fundYield.getFundName(),
                        fundYield.getCurrentPrice(),
                        fundYield.getCostPrice(),
                        fundYield.getYieldRate(),
                        fundYield.getProfitAmount(),
                        fundYield.getHoldingValue(),
                        fundYield.getTotalCost(),
                        fundYield.getCalculationDate()));
            } else if ("JSON".equalsIgnoreCase(format)) {
                // JSON格式导出
                FundYield fundYield = calculateFundYield(fundCode, null);
                // 这里可以使用Jackson等库生成JSON
                exportData.append("{\"fundCode\":\"").append(fundYield.getFundCode()).append("\",");
                exportData.append("\"yieldRate\":").append(fundYield.getYieldRate()).append("}");
            } else {
                throw new IllegalArgumentException("不支持的导出格式: " + format);
            }

            log.info("Exported yield data for fund {} in {} format", fundCode, format);
            return exportData.toString();

        } catch (Exception e) {
            log.error("Error exporting yield data for fund: {}", fundCode, e);
            return "导出失败: " + e.getMessage();
        }
    }

    // ================ 私有辅助方法 ================

    /**
     * 初始化空的投资组合收益
     */
    private PortfolioYield initializeEmptyPortfolioYield() {
        PortfolioYield emptyYield = new PortfolioYield();
        emptyYield.setTotalCost(BigDecimal.ZERO);
        emptyYield.setTotalValue(BigDecimal.ZERO);
        emptyYield.setTotalProfit(BigDecimal.ZERO);
        emptyYield.setTotalYieldRate(BigDecimal.ZERO);
        emptyYield.setDailyChange(BigDecimal.ZERO);
        emptyYield.setWeeklyChange(BigDecimal.ZERO);
        emptyYield.setMonthlyChange(BigDecimal.ZERO);
        emptyYield.setHoldingCount(0);
        emptyYield.setProfitableCount(0);
        emptyYield.setLossCount(0);
        emptyYield.setFundYields(new ArrayList<>());
        emptyYield.setCalculationDate(LocalDate.now());
        return emptyYield;
    }

    /**
     * 计算投资组合日涨跌幅
     */
    private BigDecimal calculatePortfolioDailyChange(List<FundYield> fundYields) {
        // 简化实现：计算加权平均日涨跌幅
        if (fundYields.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal weightedChange = BigDecimal.ZERO;

        for (FundYield fundYield : fundYields) {
            if (fundYield.getDailyChangeRate() != null && fundYield.getHoldingValue() != null) {
                BigDecimal weight = fundYield.getHoldingValue();
                weightedChange = weightedChange.add(
                        fundYield.getDailyChangeRate().multiply(weight));
                totalWeight = totalWeight.add(weight);
            }
        }

        return totalWeight.compareTo(BigDecimal.ZERO) > 0
                ? weightedChange.divide(totalWeight, 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
    }

    /**
     * 计算投资组合周涨跌幅
     */
    private BigDecimal calculatePortfolioWeeklyChange() {
        // 简化实现
        return BigDecimal.ZERO;
    }

    /**
     * 计算投资组合月涨跌幅
     */
    private BigDecimal calculatePortfolioMonthlyChange() {
        // 简化实现
        return BigDecimal.ZERO;
    }

    /**
     * 计算偏度
     */
    private BigDecimal calculateSkewness(List<BigDecimal> returns) {
        if (returns.size() < 3) {
            return BigDecimal.ZERO;
        }

        try {
            // 计算平均值和标准差
            BigDecimal sum = returns.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mean = sum.divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);

            BigDecimal variance = BigDecimal.ZERO;
            BigDecimal thirdMoment = BigDecimal.ZERO;

            for (BigDecimal ret : returns) {
                BigDecimal diff = ret.subtract(mean);
                variance = variance.add(diff.multiply(diff));
                thirdMoment = thirdMoment.add(diff.multiply(diff).multiply(diff));
            }

            variance = variance.divide(BigDecimal.valueOf(returns.size() - 1), 6, RoundingMode.HALF_UP);
            double stdDev = Math.sqrt(variance.doubleValue());

            if (stdDev == 0) {
                return BigDecimal.ZERO;
            }

            thirdMoment = thirdMoment.divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
            double skewness = thirdMoment.doubleValue() / Math.pow(stdDev, 3);

            return BigDecimal.valueOf(skewness).setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算峰度
     */
    private BigDecimal calculateKurtosis(List<BigDecimal> returns) {
        if (returns.size() < 4) {
            return BigDecimal.ZERO;
        }

        try {
            // 计算平均值和标准差
            BigDecimal sum = returns.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mean = sum.divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);

            BigDecimal variance = BigDecimal.ZERO;
            BigDecimal fourthMoment = BigDecimal.ZERO;

            for (BigDecimal ret : returns) {
                BigDecimal diff = ret.subtract(mean);
                variance = variance.add(diff.multiply(diff));
                fourthMoment = fourthMoment.add(
                        diff.multiply(diff).multiply(diff).multiply(diff));
            }

            variance = variance.divide(BigDecimal.valueOf(returns.size() - 1), 6, RoundingMode.HALF_UP);
            double stdDev = Math.sqrt(variance.doubleValue());

            if (stdDev == 0) {
                return BigDecimal.ZERO;
            }

            fourthMoment = fourthMoment.divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);
            double kurtosis = fourthMoment.doubleValue() / Math.pow(stdDev, 4) - 3; // 超额峰度

            return BigDecimal.valueOf(kurtosis).setScale(4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算分散化评分
     */
    private BigDecimal calculateDiversificationScore(Map<String, BigDecimal> concentration) {
        if (concentration.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 使用赫芬达尔-赫希曼指数（HHI）计算集中度
        BigDecimal hhi = BigDecimal.ZERO;
        for (BigDecimal weight : concentration.values()) {
            hhi = hhi.add(weight.multiply(weight));
        }

        // 转换为分散化评分：0-100，越高表示越分散
        BigDecimal maxHhi = BigDecimal.ONE; // 完全集中
        BigDecimal minHhi = BigDecimal.ONE.divide(
                BigDecimal.valueOf(concentration.size()), 6, RoundingMode.HALF_UP); // 完全分散

        if (hhi.compareTo(minHhi) < 0) {
            return BigDecimal.valueOf(100);
        }

        if (hhi.compareTo(maxHhi) >= 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal score = maxHhi.subtract(hhi)
                .divide(maxHhi.subtract(minHhi), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return score.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 生成配置建议
     */
    private List<String> generateAllocationRecommendations(
            Map<String, BigDecimal> allocationByType,
            Map<String, BigDecimal> allocationByRisk) {

        List<String> recommendations = new ArrayList<>();

        // 1. 检查股票型基金比例
        BigDecimal stockAllocation = allocationByType.getOrDefault("STOCK", BigDecimal.ZERO);
        if (stockAllocation.compareTo(BigDecimal.valueOf(0.7)) > 0) {
            recommendations.add("股票型基金比例过高（" + stockAllocation.multiply(BigDecimal.valueOf(100)) +
                    "%），建议适当降低以控制风险");
        } else if (stockAllocation.compareTo(BigDecimal.valueOf(0.3)) < 0) {
            recommendations.add("股票型基金比例较低（" + stockAllocation.multiply(BigDecimal.valueOf(100)) +
                    "%），可以考虑增加以提升长期收益");
        }

        // 2. 检查高风险资产比例
        BigDecimal highRiskAllocation = allocationByRisk.getOrDefault("高风险", BigDecimal.ZERO)
                .add(allocationByRisk.getOrDefault("中高风险", BigDecimal.ZERO));
        if (highRiskAllocation.compareTo(BigDecimal.valueOf(0.5)) > 0) {
            recommendations.add("中高风险资产比例较高（" + highRiskAllocation.multiply(BigDecimal.valueOf(100)) +
                    "%），建议平衡配置以降低波动");
        }

        // 3. 检查分散程度
        if (allocationByType.size() < 3) {
            recommendations.add("基金类型较少（" + allocationByType.size() + "种），建议增加不同类型的基金以提高分散性");
        }

        // 4. 默认建议
        if (recommendations.isEmpty()) {
            recommendations.add("当前资产配置较为合理，建议定期检视并根据市场变化调整");
        }

        return recommendations;
    }
}