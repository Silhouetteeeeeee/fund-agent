package com.shxc.fundagent.attribution;

import com.shxc.fundagent.attribution.model.*;
import com.shxc.fundagent.entity.FundDailyData;
import com.shxc.fundagent.entity.FundHolding;
import com.shxc.fundagent.entity.FundInfo;
import com.shxc.fundagent.repository.FundDailyDataRepository;
import com.shxc.fundagent.repository.FundHoldingRepository;
import com.shxc.fundagent.repository.FundInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Brinson归因分析服务
 * 实现单期和多期Brinson归因模型
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrinsonAttributionService {

    private final FundHoldingRepository fundHoldingRepository;
    private final FundInfoRepository fundInfoRepository;
    private final FundDailyDataRepository fundDailyDataRepository;

    // ========== 基准配置（可配置化） ==========
    
    /**
     * 默认基准配置 - 股债平衡型（60%股票 + 40%债券）
     * 可根据用户风险偏好调整
     */
    private static final Map<AssetClass, BigDecimal> DEFAULT_BENCHMARK = new HashMap<>();
    
    static {
        DEFAULT_BENCHMARK.put(AssetClass.EQUITY_LARGE, new BigDecimal("0.30"));
        DEFAULT_BENCHMARK.put(AssetClass.EQUITY_SMALL, new BigDecimal("0.10"));
        DEFAULT_BENCHMARK.put(AssetClass.EQUITY_GROWTH, new BigDecimal("0.10"));
        DEFAULT_BENCHMARK.put(AssetClass.EQUITY_VALUE, new BigDecimal("0.10"));
        DEFAULT_BENCHMARK.put(AssetClass.BOND, new BigDecimal("0.30"));
        DEFAULT_BENCHMARK.put(AssetClass.MONEY_MARKET, new BigDecimal("0.10"));
    }

    // ========== 核心归因计算方法 ==========

    /**
     * 执行Brinson归因分析
     * 
     * @param startDate 分析开始日期
     * @param endDate 分析结束日期
     * @param benchmark 基准配置（可选，使用默认基准）
     * @return 归因分析结果
     */
    public BrinsonAttributionResult performAttribution(LocalDate startDate, LocalDate endDate, 
                                                        Map<AssetClass, BigDecimal> benchmark) {
        long startTime = System.currentTimeMillis();
        log.info("开始Brinson归因分析: {} 至 {}", startDate, endDate);

        try {
            // 1. 获取分析期间的持仓数据
            List<FundHolding> holdings = fundHoldingRepository.findByStatus("ACTIVE");
            if (holdings.isEmpty()) {
                log.warn("没有活跃持仓，无法执行归因分析");
                return createEmptyResult(startDate, endDate);
            }

            // 2. 使用默认基准（如果未提供）
            if (benchmark == null) {
                benchmark = new HashMap<>(DEFAULT_BENCHMARK);
            }

            // 3. 计算组合和基准的收益
            BigDecimal portfolioReturn = calculatePortfolioReturn(holdings, startDate, endDate);
            BigDecimal benchmarkReturn = calculateBenchmarkReturn(benchmark, startDate, endDate);
            BigDecimal excessReturn = portfolioReturn.subtract(benchmarkReturn);

            // 4. 按资产类别分组计算归因
            List<AssetClassAttribution> assetClassAttributions = calculateAssetClassAttribution(
                    holdings, benchmark, startDate, endDate);

            // 5. 计算单只基金归因
            List<FundAttribution> fundAttributions = calculateFundAttribution(holdings, startDate, endDate);

            // 6. 汇总三效应
            BigDecimal totalAllocation = assetClassAttributions.stream()
                    .map(AssetClassAttribution::getAllocationEffect)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalSelection = assetClassAttributions.stream()
                    .map(AssetClassAttribution::getSelectionEffect)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalInteraction = assetClassAttributions.stream()
                    .map(AssetClassAttribution::getInteractionEffect)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal totalAttribution = totalAllocation.add(totalSelection).add(totalInteraction);
            BigDecimal residual = excessReturn.subtract(totalAttribution);

            // 7. 生成贡献度分析
            AttributionContribution contribution = analyzeContribution(assetClassAttributions, fundAttributions);

            // 8. 生成配置分析
            AllocationAnalysis allocation = analyzeAllocation(holdings, benchmark);

            long endTime = System.currentTimeMillis();

            // 9. 构建结果
            BrinsonAttributionResult result = BrinsonAttributionResult.builder()
                    .attributionId(generateAttributionId())
                    .analysisDate(LocalDate.now())
                    .startDate(startDate)
                    .endDate(endDate)
                    .portfolioReturn(portfolioReturn)
                    .benchmarkReturn(benchmarkReturn)
                    .excessReturn(excessReturn)
                    .allocationEffect(totalAllocation)
                    .selectionEffect(totalSelection)
                    .interactionEffect(totalInteraction)
                    .totalAttribution(totalAttribution)
                    .residual(residual)
                    .assetClassAttributions(assetClassAttributions)
                    .fundAttributions(fundAttributions)
                    .contributionAnalysis(contribution)
                    .allocationAnalysis(allocation)
                    .calculationTimeMs(endTime - startTime)
                    .build();

            log.info("Brinson归因分析完成，耗时 {}ms", endTime - startTime);
            log.info("归因结果: 组合收益 {:.2f}%, 基准收益 {:.2f}%, 超额收益 {:.2f}%",
                    portfolioReturn.multiply(new BigDecimal("100")),
                    benchmarkReturn.multiply(new BigDecimal("100")),
                    excessReturn.multiply(new BigDecimal("100")));

            return result;

        } catch (Exception e) {
            log.error("Brinson归因分析失败", e);
            throw new RuntimeException("归因分析失败: " + e.getMessage(), e);
        }
    }

    /**
     * 使用默认基准执行归因分析
     */
    public BrinsonAttributionResult performAttribution(LocalDate startDate, LocalDate endDate) {
        return performAttribution(startDate, endDate, null);
    }

    /**
     * 计算资产类别层面的归因
     */
    private List<AssetClassAttribution> calculateAssetClassAttribution(
            List<FundHolding> holdings, 
            Map<AssetClass, BigDecimal> benchmark,
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<AssetClassAttribution> attributions = new ArrayList<>();

        // 1. 按资产类别分组基金
        Map<AssetClass, List<FundHolding>> holdingsByAssetClass = holdings.stream()
                .collect(Collectors.groupingBy(h -> getAssetClass(h.getFundCode())));

        // 2. 计算组合总市值
        BigDecimal totalPortfolioValue = holdings.stream()
                .map(FundHolding::getHoldingValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalPortfolioValue.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("组合总市值为0，无法计算归因");
            return attributions;
        }

        // 3. 对每个资产类别计算归因
        for (AssetClass assetClass : AssetClass.values()) {
            List<FundHolding> classHoldings = holdingsByAssetClass.getOrDefault(assetClass, new ArrayList<>());
            
            // 计算组合权重
            BigDecimal classValue = classHoldings.stream()
                    .map(FundHolding::getHoldingValue)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal portfolioWeight = classValue.divide(totalPortfolioValue, 4, RoundingMode.HALF_UP);

            // 获取基准权重
            BigDecimal benchmarkWeight = benchmark.getOrDefault(assetClass, BigDecimal.ZERO);

            // 计算组合中该类资产的收益率
            BigDecimal portfolioReturn = calculateAssetClassReturn(classHoldings, startDate, endDate);

            // 计算基准中该类资产的收益率（使用同类平均或指数）
            BigDecimal benchmarkReturn = calculateBenchmarkAssetReturn(assetClass, startDate, endDate);

            // 计算Brinson三效应
            BigDecimal weightDiff = portfolioWeight.subtract(benchmarkWeight);
            BigDecimal returnDiff = portfolioReturn.subtract(benchmarkReturn);

            // Allocation Effect = (Wp - Wb) * Rb
            BigDecimal allocationEffect = weightDiff.multiply(benchmarkReturn);

            // Selection Effect = Wb * (Rp - Rb)
            BigDecimal selectionEffect = benchmarkWeight.multiply(returnDiff);

            // Interaction Effect = (Wp - Wb) * (Rp - Rb)
            BigDecimal interactionEffect = weightDiff.multiply(returnDiff);

            AssetClassAttribution attribution = AssetClassAttribution.builder()
                    .assetClass(assetClass)
                    .assetClassName(assetClass.getName())
                    .portfolioWeight(portfolioWeight)
                    .benchmarkWeight(benchmarkWeight)
                    .weightDifference(weightDiff)
                    .portfolioReturn(portfolioReturn)
                    .benchmarkReturn(benchmarkReturn)
                    .excessReturn(returnDiff)
                    .allocationEffect(allocationEffect)
                    .selectionEffect(selectionEffect)
                    .interactionEffect(interactionEffect)
                    .totalEffect(allocationEffect.add(selectionEffect).add(interactionEffect))
                    .contributionToPortfolio(portfolioWeight.multiply(portfolioReturn))
                    .contributionToBenchmark(benchmarkWeight.multiply(benchmarkReturn))
                    .contributionToExcess(weightDiff.multiply(returnDiff))
                    .build();

            attributions.add(attribution);
        }

        return attributions;
    }

    /**
     * 计算单只基金的归因
     */
    private List<FundAttribution> calculateFundAttribution(List<FundHolding> holdings, 
                                                            LocalDate startDate, 
                                                            LocalDate endDate) {
        List<FundAttribution> attributions = new ArrayList<>();

        BigDecimal totalValue = holdings.stream()
                .map(FundHolding::getHoldingValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        for (FundHolding holding : holdings) {
            String fundCode = holding.getFundCode();
            
            // 获取基金信息
            Optional<FundInfo> fundInfoOpt = fundInfoRepository.findByFundCode(fundCode);
            FundInfo fundInfo = fundInfoOpt.orElse(null);

            // 计算基金收益率
            BigDecimal fundReturn = calculateFundReturn(fundCode, startDate, endDate);

            // 计算权重
            BigDecimal weight = holding.getHoldingValue() != null && totalValue.compareTo(BigDecimal.ZERO) > 0
                    ? holding.getHoldingValue().divide(totalValue, 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 获取同类平均收益（简化处理，实际应该查询同类基金平均）
            BigDecimal categoryAvgReturn = getCategoryAverageReturn(fundCode, startDate, endDate);

            FundAttribution attribution = FundAttribution.builder()
                    .fundCode(fundCode)
                    .fundName(fundInfo != null ? fundInfo.getFundName() : fundCode)
                    .assetClass(getAssetClass(fundCode))
                    .fundType(fundInfo != null ? fundInfo.getFundType().name() : "未知")
                    .portfolioWeight(weight)
                    .holdingValue(holding.getHoldingValue())
                    .holdingCost(holding.getTotalCost())
                    .fundReturn(fundReturn)
                    .categoryAverageReturn(categoryAvgReturn)
                    .excessReturn(fundReturn.subtract(categoryAvgReturn))
                    .contributionToPortfolio(weight.multiply(fundReturn))
                    .build();

            attributions.add(attribution);
        }

        return attributions;
    }

    // ========== 辅助计算方法 ==========

    /**
     * 计算组合总收益（市值加权）
     */
    private BigDecimal calculatePortfolioReturn(List<FundHolding> holdings, 
                                                 LocalDate startDate, 
                                                 LocalDate endDate) {
        BigDecimal totalValue = holdings.stream()
                .map(FundHolding::getHoldingValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal weightedReturn = BigDecimal.ZERO;
        for (FundHolding holding : holdings) {
            BigDecimal weight = holding.getHoldingValue().divide(totalValue, 4, RoundingMode.HALF_UP);
            BigDecimal fundReturn = calculateFundReturn(holding.getFundCode(), startDate, endDate);
            weightedReturn = weightedReturn.add(weight.multiply(fundReturn));
        }

        return weightedReturn;
    }

    /**
     * 计算基准总收益
     */
    private BigDecimal calculateBenchmarkReturn(Map<AssetClass, BigDecimal> benchmark,
                                                 LocalDate startDate, 
                                                 LocalDate endDate) {
        BigDecimal totalReturn = BigDecimal.ZERO;
        
        for (Map.Entry<AssetClass, BigDecimal> entry : benchmark.entrySet()) {
            AssetClass assetClass = entry.getKey();
            BigDecimal weight = entry.getValue();
            BigDecimal assetReturn = calculateBenchmarkAssetReturn(assetClass, startDate, endDate);
            totalReturn = totalReturn.add(weight.multiply(assetReturn));
        }

        return totalReturn;
    }

    /**
     * 计算单只基金收益率
     */
    private BigDecimal calculateFundReturn(String fundCode, LocalDate startDate, LocalDate endDate) {
        try {
            Optional<FundDailyData> startData = fundDailyDataRepository
                    .findFirstByFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(fundCode, startDate);
            Optional<FundDailyData> endData = fundDailyDataRepository
                    .findFirstByFundCodeAndTradeDateLessThanEqualOrderByTradeDateDesc(fundCode, endDate);

            if (startData.isPresent() && endData.isPresent() 
                    && startData.get().getNetValue() != null 
                    && endData.get().getNetValue() != null) {
                
                BigDecimal startNav = startData.get().getNetValue();
                BigDecimal endNav = endData.get().getNetValue();
                
                return endNav.subtract(startNav).divide(startNav, 4, RoundingMode.HALF_UP);
            }
        } catch (Exception e) {
            log.warn("计算基金 {} 收益率失败: {}", fundCode, e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    /**
     * 计算资产类别收益率
     */
    private BigDecimal calculateAssetClassReturn(List<FundHolding> holdings, 
                                                  LocalDate startDate, 
                                                  LocalDate endDate) {
        if (holdings.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalValue = holdings.stream()
                .map(FundHolding::getHoldingValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal weightedReturn = BigDecimal.ZERO;
        for (FundHolding holding : holdings) {
            BigDecimal weight = holding.getHoldingValue().divide(totalValue, 4, RoundingMode.HALF_UP);
            BigDecimal fundReturn = calculateFundReturn(holding.getFundCode(), startDate, endDate);
            weightedReturn = weightedReturn.add(weight.multiply(fundReturn));
        }

        return weightedReturn;
    }

    /**
     * 计算基准资产类别收益率（使用代表性指数）
     */
    private BigDecimal calculateBenchmarkAssetReturn(AssetClass assetClass, 
                                                      LocalDate startDate, 
                                                      LocalDate endDate) {
        // 简化实现：使用沪深300代表股票，中证全债代表债券
        // 实际应该查询对应指数数据
        String indexCode;
        switch (assetClass) {
            case EQUITY_LARGE:
            case EQUITY_GROWTH:
            case EQUITY_VALUE:
                indexCode = "000300"; // 沪深300
                break;
            case EQUITY_SMALL:
                indexCode = "000905"; // 中证500
                break;
            case BOND:
                indexCode = "H11001"; // 中证全债
                break;
            case MONEY_MARKET:
                return new BigDecimal("0.015"); // 年化1.5%
            default:
                return BigDecimal.ZERO;
        }

        // 这里简化处理，实际应该查询指数数据
        // 返回模拟数据
        return new BigDecimal("0.05"); // 假设5%收益
    }

    /**
     * 获取基金资产类别
     */
    private AssetClass getAssetClass(String fundCode) {
        Optional<FundInfo> fundInfo = fundInfoRepository.findByFundCode(fundCode);
        if (fundInfo.isPresent()) {
            return AssetClass.fromFundType(fundInfo.get().getFundType().name());
        }
        return AssetClass.HYBRID;
    }

    /**
     * 获取同类平均收益（简化实现）
     */
    private BigDecimal getCategoryAverageReturn(String fundCode, LocalDate startDate, LocalDate endDate) {
        // 简化实现，实际应该查询同类基金平均收益
        return new BigDecimal("0.03"); // 假设3%
    }

    /**
     * 分析贡献度
     */
    private AttributionContribution analyzeContribution(List<AssetClassAttribution> assetAttributions,
                                                         List<FundAttribution> fundAttributions) {
        // 找出最大正负贡献
        AssetClassAttribution topPositive = assetAttributions.stream()
                .filter(a -> a.getTotalEffect() != null)
                .max(Comparator.comparing(AssetClassAttribution::getTotalEffect))
                .orElse(null);

        AssetClassAttribution topNegative = assetAttributions.stream()
                .filter(a -> a.getTotalEffect() != null)
                .min(Comparator.comparing(AssetClassAttribution::getTotalEffect))
                .orElse(null);

        FundAttribution topFund = fundAttributions.stream()
                .filter(f -> f.getContributionToPortfolio() != null)
                .max(Comparator.comparing(FundAttribution::getContributionToPortfolio))
                .orElse(null);

        FundAttribution bottomFund = fundAttributions.stream()
                .filter(f -> f.getContributionToPortfolio() != null)
                .min(Comparator.comparing(FundAttribution::getContributionToPortfolio))
                .orElse(null);

        return AttributionContribution.builder()
                .topPositiveAssetClass(topPositive != null ? topPositive.getAssetClass() : null)
                .topPositiveContribution(topPositive != null ? topPositive.getTotalEffect() : BigDecimal.ZERO)
                .topNegativeAssetClass(topNegative != null ? topNegative.getAssetClass() : null)
                .topNegativeContribution(topNegative != null ? topNegative.getTotalEffect() : BigDecimal.ZERO)
                .topPositiveFund(topFund != null ? topFund.getFundName() : null)
                .topPositiveFundCode(topFund != null ? topFund.getFundCode() : null)
                .topNegativeFund(bottomFund != null ? bottomFund.getFundName() : null)
                .topNegativeFundCode(bottomFund != null ? bottomFund.getFundCode() : null)
                .build();
    }

    /**
     * 分析资产配置
     */
    private AllocationAnalysis analyzeAllocation(List<FundHolding> holdings,
                                                  Map<AssetClass, BigDecimal> benchmark) {
        // 计算组合配置
        Map<AssetClass, BigDecimal> portfolioAllocation = calculatePortfolioAllocation(holdings);

        // 计算权益/固收比例
        BigDecimal portfolioEquity = portfolioAllocation.entrySet().stream()
                .filter(e -> e.getKey().isEquity())
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal portfolioFixedIncome = portfolioAllocation.entrySet().stream()
                .filter(e -> e.getKey().isFixedIncome())
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算基准比例
        BigDecimal benchmarkEquity = benchmark.entrySet().stream()
                .filter(e -> e.getKey().isEquity())
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal benchmarkFixedIncome = benchmark.entrySet().stream()
                .filter(e -> e.getKey().isFixedIncome())
                .map(Map.Entry::getValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 计算偏离
        BigDecimal equityDeviation = portfolioEquity.subtract(benchmarkEquity);

        // 集中度分析
        BigDecimal maxSingle = portfolioAllocation.values().stream()
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        boolean isOverConcentrated = maxSingle.compareTo(new BigDecimal("0.50")) > 0;

        return AllocationAnalysis.builder()
                .portfolioEquityRatio(portfolioEquity)
                .portfolioFixedIncomeRatio(portfolioFixedIncome)
                .portfolioOtherRatio(BigDecimal.ONE.subtract(portfolioEquity).subtract(portfolioFixedIncome))
                .benchmarkEquityRatio(benchmarkEquity)
                .benchmarkFixedIncomeRatio(benchmarkFixedIncome)
                .benchmarkOtherRatio(BigDecimal.ONE.subtract(benchmarkEquity).subtract(benchmarkFixedIncome))
                .equityDeviation(equityDeviation)
                .maxSingleAssetRatio(maxSingle)
                .isOverConcentrated(isOverConcentrated)
                .allocationWarning(isOverConcentrated ? "单一资产类别占比过高，建议分散配置" : null)
                .diversificationScore(calculateDiversificationScore(portfolioAllocation))
                .build();
    }

    /**
     * 计算组合配置结构
     */
    private Map<AssetClass, BigDecimal> calculatePortfolioAllocation(List<FundHolding> holdings) {
        BigDecimal totalValue = holdings.stream()
                .map(FundHolding::getHoldingValue)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<AssetClass, BigDecimal> allocation = new HashMap<>();
        
        if (totalValue.compareTo(BigDecimal.ZERO) == 0) {
            return allocation;
        }

        for (FundHolding holding : holdings) {
            AssetClass assetClass = getAssetClass(holding.getFundCode());
            BigDecimal value = holding.getHoldingValue() != null ? holding.getHoldingValue() : BigDecimal.ZERO;
            BigDecimal weight = value.divide(totalValue, 4, RoundingMode.HALF_UP);
            
            allocation.merge(assetClass, weight, BigDecimal::add);
        }

        return allocation;
    }

    /**
     * 计算分散化评分
     */
    private BigDecimal calculateDiversificationScore(Map<AssetClass, BigDecimal> allocation) {
        if (allocation.isEmpty()) {
            return BigDecimal.ZERO;
        }

        // 使用赫芬达尔指数计算集中度
        BigDecimal hhi = allocation.values().stream()
                .map(w -> w.multiply(w))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 转换为0-100的分散化评分
        // HHI范围是0-1，1表示完全集中，0表示完全分散
        // 评分 = (1 - HHI) * 100
        return BigDecimal.ONE.subtract(hhi).multiply(new BigDecimal("100"));
    }

    /**
     * 创建空结果
     */
    private BrinsonAttributionResult createEmptyResult(LocalDate startDate, LocalDate endDate) {
        return BrinsonAttributionResult.builder()
                .attributionId(generateAttributionId())
                .analysisDate(LocalDate.now())
                .startDate(startDate)
                .endDate(endDate)
                .portfolioReturn(BigDecimal.ZERO)
                .benchmarkReturn(BigDecimal.ZERO)
                .excessReturn(BigDecimal.ZERO)
                .allocationEffect(BigDecimal.ZERO)
                .selectionEffect(BigDecimal.ZERO)
                .interactionEffect(BigDecimal.ZERO)
                .totalAttribution(BigDecimal.ZERO)
                .residual(BigDecimal.ZERO)
                .assetClassAttributions(new ArrayList<>())
                .fundAttributions(new ArrayList<>())
                .build();
    }

    /**
     * 生成归因分析ID
     */
    private String generateAttributionId() {
        return "ATTR_" + System.currentTimeMillis();
    }
}
