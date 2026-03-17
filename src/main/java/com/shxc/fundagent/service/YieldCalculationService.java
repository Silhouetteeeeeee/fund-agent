package com.shxc.fundagent.service;

import com.shxc.fundagent.entity.FundHolding;
import com.shxc.fundagent.entity.FundInfo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 收益计算服务接口
 * 提供基金收益计算和持仓分析功能
 */
public interface YieldCalculationService {


    /**
     * 计算单只基金收益率
     *
     * @param fundCode  基金代码
     * @param specifiedCostPrice 持仓成本（如果为null，使用平均持仓成本）
     * @oara calculationDate 计算日期
     * @return 收益率信息
     */
    FundYield calculateFundYield(String fundCode, BigDecimal specifiedCostPrice);

    /**
     * 批量计算基金收益率
     *
     * @param fundCodes 基金代码列表
     * @return 收益率信息列表
     */
    List<FundYield> batchCalculateFundYields(List<String> fundCodes);

    /**
     * 计算整体持仓收益
     *
     * @return 整体持仓收益信息
     */
    PortfolioYield calculatePortfolioYield();

    /**
     * 计算指定持仓记录的收益
     *
     * @param holdingId 持仓记录ID
     * @return 持仓收益信息
     */
    HoldingYield calculateHoldingYield(Long holdingId);

    /**
     * 计算基金历史收益率
     *
     * @param fundCode 基金代码
     * @param days     历史天数
     * @return 历史收益率列表
     */
    List<HistoricalYield> calculateHistoricalYields(String fundCode, int days);

    /**
     * 计算基金风险指标
     *
     * @param fundCode 基金代码
     * @param days     历史天数
     * @return 风险指标
     */
    RiskMetrics calculateRiskMetrics(String fundCode, int days);

    /**
     * 计算资产配置分析
     *
     * @return 资产配置分析结果
     */
    AssetAllocationAnalysis analyzeAssetAllocation();

    /**
     * 计算收益对比
     *
     * @param fundCodes 基金代码列表
     * @param days      对比天数
     * @return 收益对比结果
     */
    YieldComparison compareYields(List<String> fundCodes, int days);

    /**
     * 计算收益趋势
     *
     * @param fundCode 基金代码
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return 收益趋势数据
     */
    YieldTrend calculateYieldTrend(String fundCode, LocalDate startDate, LocalDate endDate);

    /**
     * 计算收益汇总报告
     *
     * @param fundCode 基金代码
     * @return 收益汇总报告
     */
    YieldSummary calculateYieldSummary(String fundCode);

    /**
     * 更新持仓市值
     *
     * @param fundCode 基金代码
     * @return 更新后的持仓市值
     */
    BigDecimal updateHoldingValue(String fundCode);

    /**
     * 批量更新持仓市值
     *
     * @return 更新成功的数量
     */
    int batchUpdateHoldingValues();

    /**
     * 计算收益分配
     *
     * @param totalAmount 总金额
     * @param weights     权重列表
     * @return 分配结果列表
     */
    List<BigDecimal> calculateProfitDistribution(BigDecimal totalAmount, List<BigDecimal> weights);

    /**
     * 验证收益计算数据
     *
     * @param fundCode 基金代码
     * @return 验证结果
     */
    ValidationResult validateYieldData(String fundCode);

    /**
     * 获取收益计算统计
     *
     * @return 统计信息
     */
    Map<String, Object> getYieldStatistics();

    /**
     * 重新计算所有持仓收益
     *
     * @return 重新计算的数量
     */
    int recalculateAllYields();

    /**
     * 导出收益数据
     *
     * @param fundCode 基金代码
     * @param format   导出格式：CSV, JSON, EXCEL
     * @return 导出数据
     */
    String exportYieldData(String fundCode, String format);

    // ================ 内部数据类 ================

    /**
     * 基金收益率信息
     */
    class FundYield {
        private String fundCode;
        private String fundName;
        private BigDecimal currentPrice;
        private BigDecimal costPrice;
        private BigDecimal yieldRate; // 百分比
        private BigDecimal profitAmount;
        private BigDecimal holdingValue;
        private BigDecimal totalCost;
        private BigDecimal dailyChangeRate;
        private BigDecimal weeklyChangeRate;
        private BigDecimal monthlyChangeRate;
        private LocalDate calculationDate;

        // getters and setters...

        public String getFundCode() { return fundCode; }
        public void setFundCode(String fundCode) { this.fundCode = fundCode; }
        public String getFundName() { return fundName; }
        public void setFundName(String fundName) { this.fundName = fundName; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
        public BigDecimal getCostPrice() { return costPrice; }
        public void setCostPrice(BigDecimal costPrice) { this.costPrice = costPrice; }
        public BigDecimal getYieldRate() { return yieldRate; }
        public void setYieldRate(BigDecimal yieldRate) { this.yieldRate = yieldRate; }
        public BigDecimal getProfitAmount() { return profitAmount; }
        public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
        public BigDecimal getHoldingValue() { return holdingValue; }
        public void setHoldingValue(BigDecimal holdingValue) { this.holdingValue = holdingValue; }
        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
        public BigDecimal getDailyChangeRate() { return dailyChangeRate; }
        public void setDailyChangeRate(BigDecimal dailyChangeRate) { this.dailyChangeRate = dailyChangeRate; }
        public BigDecimal getWeeklyChangeRate() { return weeklyChangeRate; }
        public void setWeeklyChangeRate(BigDecimal weeklyChangeRate) { this.weeklyChangeRate = weeklyChangeRate; }
        public BigDecimal getMonthlyChangeRate() { return monthlyChangeRate; }
        public void setMonthlyChangeRate(BigDecimal monthlyChangeRate) { this.monthlyChangeRate = monthlyChangeRate; }
        public LocalDate getCalculationDate() { return calculationDate; }
        public void setCalculationDate(LocalDate calculationDate) { this.calculationDate = calculationDate; }
    }

    /**
     * 投资组合收益信息
     */
    class PortfolioYield {
        private BigDecimal totalCost;
        private BigDecimal totalValue;
        private BigDecimal totalProfit;
        private BigDecimal totalYieldRate;
        private BigDecimal dailyChange;
        private BigDecimal weeklyChange;
        private BigDecimal monthlyChange;
        private int holdingCount;
        private int profitableCount;
        private int lossCount;
        private List<FundYield> fundYields;
        private LocalDate calculationDate;

        // getters and setters...
        public BigDecimal getTotalCost() { return totalCost; }
        public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
        public BigDecimal getTotalValue() { return totalValue; }
        public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
        public BigDecimal getTotalProfit() { return totalProfit; }
        public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
        public BigDecimal getTotalYieldRate() { return totalYieldRate; }
        public void setTotalYieldRate(BigDecimal totalYieldRate) { this.totalYieldRate = totalYieldRate; }
        public BigDecimal getDailyChange() { return dailyChange; }
        public void setDailyChange(BigDecimal dailyChange) { this.dailyChange = dailyChange; }
        public BigDecimal getWeeklyChange() { return weeklyChange; }
        public void setWeeklyChange(BigDecimal weeklyChange) { this.weeklyChange = weeklyChange; }
        public BigDecimal getMonthlyChange() { return monthlyChange; }
        public void setMonthlyChange(BigDecimal monthlyChange) { this.monthlyChange = monthlyChange; }
        public int getHoldingCount() { return holdingCount; }
        public void setHoldingCount(int holdingCount) { this.holdingCount = holdingCount; }
        public int getProfitableCount() { return profitableCount; }
        public void setProfitableCount(int profitableCount) { this.profitableCount = profitableCount; }
        public int getLossCount() { return lossCount; }
        public void setLossCount(int lossCount) { this.lossCount = lossCount; }
        public List<FundYield> getFundYields() { return fundYields; }
        public void setFundYields(List<FundYield> fundYields) { this.fundYields = fundYields; }
        public LocalDate getCalculationDate() { return calculationDate; }
        public void setCalculationDate(LocalDate calculationDate) { this.calculationDate = calculationDate; }
    }

    /**
     * 持仓收益信息
     */
    class HoldingYield {
        private Long holdingId;
        private FundHolding holding;
        private FundInfo fundInfo;
        private BigDecimal currentPrice;
        private BigDecimal yieldRate;
        private BigDecimal profitAmount;
        private BigDecimal holdingValue;
        private int holdingDays;
        private BigDecimal annualizedYield;
        private LocalDate calculationDate;

        // getters and setters...
        public Long getHoldingId() { return holdingId; }
        public void setHoldingId(Long holdingId) { this.holdingId = holdingId; }
        public FundHolding getHolding() { return holding; }
        public void setHolding(FundHolding holding) { this.holding = holding; }
        public FundInfo getFundInfo() { return fundInfo; }
        public void setFundInfo(FundInfo fundInfo) { this.fundInfo = fundInfo; }
        public BigDecimal getCurrentPrice() { return currentPrice; }
        public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
        public BigDecimal getYieldRate() { return yieldRate; }
        public void setYieldRate(BigDecimal yieldRate) { this.yieldRate = yieldRate; }
        public BigDecimal getProfitAmount() { return profitAmount; }
        public void setProfitAmount(BigDecimal profitAmount) { this.profitAmount = profitAmount; }
        public BigDecimal getHoldingValue() { return holdingValue; }
        public void setHoldingValue(BigDecimal holdingValue) { this.holdingValue = holdingValue; }
        public int getHoldingDays() { return holdingDays; }
        public void setHoldingDays(int holdingDays) { this.holdingDays = holdingDays; }
        public BigDecimal getAnnualizedYield() { return annualizedYield; }
        public void setAnnualizedYield(BigDecimal annualizedYield) { this.annualizedYield = annualizedYield; }
        public LocalDate getCalculationDate() { return calculationDate; }
        public void setCalculationDate(LocalDate calculationDate) { this.calculationDate = calculationDate; }
    }

    /**
     * 历史收益率
     */
    class HistoricalYield {
        private LocalDate date;
        private BigDecimal price;
        private BigDecimal dailyReturn;
        private BigDecimal cumulativeReturn;
        private BigDecimal drawdown;

        // getters and setters...
        public LocalDate getDate() { return date; }
        public void setDate(LocalDate date) { this.date = date; }
        public BigDecimal getPrice() { return price; }
        public void setPrice(BigDecimal price) { this.price = price; }
        public BigDecimal getDailyReturn() { return dailyReturn; }
        public void setDailyReturn(BigDecimal dailyReturn) { this.dailyReturn = dailyReturn; }
        public BigDecimal getCumulativeReturn() { return cumulativeReturn; }
        public void setCumulativeReturn(BigDecimal cumulativeReturn) { this.cumulativeReturn = cumulativeReturn; }
        public BigDecimal getDrawdown() { return drawdown; }
        public void setDrawdown(BigDecimal drawdown) { this.drawdown = drawdown; }
    }

    /**
     * 风险指标
     */
    class RiskMetrics {
        private BigDecimal volatility;
        private BigDecimal maxDrawdown;
        private BigDecimal sharpeRatio;
        private BigDecimal beta;
        private BigDecimal alpha;
        private BigDecimal var95; // 95% VaR
        private BigDecimal expectedReturn;
        private BigDecimal skewness;
        private BigDecimal kurtosis;

        // getters and setters...
        public BigDecimal getVolatility() { return volatility; }
        public void setVolatility(BigDecimal volatility) { this.volatility = volatility; }
        public BigDecimal getMaxDrawdown() { return maxDrawdown; }
        public void setMaxDrawdown(BigDecimal maxDrawdown) { this.maxDrawdown = maxDrawdown; }
        public BigDecimal getSharpeRatio() { return sharpeRatio; }
        public void setSharpeRatio(BigDecimal sharpeRatio) { this.sharpeRatio = sharpeRatio; }
        public BigDecimal getBeta() { return beta; }
        public void setBeta(BigDecimal beta) { this.beta = beta; }
        public BigDecimal getAlpha() { return alpha; }
        public void setAlpha(BigDecimal alpha) { this.alpha = alpha; }
        public BigDecimal getVar95() { return var95; }
        public void setVar95(BigDecimal var95) { this.var95 = var95; }
        public BigDecimal getExpectedReturn() { return expectedReturn; }
        public void setExpectedReturn(BigDecimal expectedReturn) { this.expectedReturn = expectedReturn; }
        public BigDecimal getSkewness() { return skewness; }
        public void setSkewness(BigDecimal skewness) { this.skewness = skewness; }
        public BigDecimal getKurtosis() { return kurtosis; }
        public void setKurtosis(BigDecimal kurtosis) { this.kurtosis = kurtosis; }
    }

    /**
     * 资产配置分析
     */
    class AssetAllocationAnalysis {
        private Map<String, BigDecimal> allocationByType; // 按基金类型分配
        private Map<String, BigDecimal> allocationByRisk; // 按风险等级分配
        private Map<String, BigDecimal> concentration; // 集中度分析
        private BigDecimal diversificationScore; // 分散化评分
        private List<String> recommendations; // 配置建议

        // getters and setters...
        public Map<String, BigDecimal> getAllocationByType() { return allocationByType; }
        public void setAllocationByType(Map<String, BigDecimal> allocationByType) { this.allocationByType = allocationByType; }
        public Map<String, BigDecimal> getAllocationByRisk() { return allocationByRisk; }
        public void setAllocationByRisk(Map<String, BigDecimal> allocationByRisk) { this.allocationByRisk = allocationByRisk; }
        public Map<String, BigDecimal> getConcentration() { return concentration; }
        public void setConcentration(Map<String, BigDecimal> concentration) { this.concentration = concentration; }
        public BigDecimal getDiversificationScore() { return diversificationScore; }
        public void setDiversificationScore(BigDecimal diversificationScore) { this.diversificationScore = diversificationScore; }
        public List<String> getRecommendations() { return recommendations; }
        public void setRecommendations(List<String> recommendations) { this.recommendations = recommendations; }
    }

    /**
     * 收益对比
     */
    class YieldComparison {
        private List<String> fundCodes;
        private Map<String, BigDecimal> yields;
        private Map<String, BigDecimal> volatilities;
        private Map<String, BigDecimal> sharpeRatios;
        private Map<String, BigDecimal> maxDrawdowns;
        private String bestPerformer;
        private String worstPerformer;
        private LocalDate comparisonDate;

        // getters and setters...
        public List<String> getFundCodes() { return fundCodes; }
        public void setFundCodes(List<String> fundCodes) { this.fundCodes = fundCodes; }
        public Map<String, BigDecimal> getYields() { return yields; }
        public void setYields(Map<String, BigDecimal> yields) { this.yields = yields; }
        public Map<String, BigDecimal> getVolatilities() { return volatilities; }
        public void setVolatilities(Map<String, BigDecimal> volatilities) { this.volatilities = volatilities; }
        public Map<String, BigDecimal> getSharpeRatios() { return sharpeRatios; }
        public void setSharpeRatios(Map<String, BigDecimal> sharpeRatios) { this.sharpeRatios = sharpeRatios; }
        public Map<String, BigDecimal> getMaxDrawdowns() { return maxDrawdowns; }
        public void setMaxDrawdowns(Map<String, BigDecimal> maxDrawdowns) { this.maxDrawdowns = maxDrawdowns; }
        public String getBestPerformer() { return bestPerformer; }
        public void setBestPerformer(String bestPerformer) { this.bestPerformer = bestPerformer; }
        public String getWorstPerformer() { return worstPerformer; }
        public void setWorstPerformer(String worstPerformer) { this.worstPerformer = worstPerformer; }
        public LocalDate getComparisonDate() { return comparisonDate; }
        public void setComparisonDate(LocalDate comparisonDate) { this.comparisonDate = comparisonDate; }
    }

    /**
     * 收益趋势
     */
    class YieldTrend {
        private String fundCode;
        private List<HistoricalYield> historicalYields;
        private BigDecimal trendSlope; // 趋势斜率
        private BigDecimal rSquared; // R平方值
        private boolean isUpwardTrend;
        private LocalDate startDate;
        private LocalDate endDate;

        // getters and setters...
        public String getFundCode() { return fundCode; }
        public void setFundCode(String fundCode) { this.fundCode = fundCode; }
        public List<HistoricalYield> getHistoricalYields() { return historicalYields; }
        public void setHistoricalYields(List<HistoricalYield> historicalYields) { this.historicalYields = historicalYields; }
        public BigDecimal getTrendSlope() { return trendSlope; }
        public void setTrendSlope(BigDecimal trendSlope) { this.trendSlope = trendSlope; }
        public BigDecimal getRSquared() { return rSquared; }
        public void setRSquared(BigDecimal rSquared) { this.rSquared = rSquared; }
        public boolean isUpwardTrend() { return isUpwardTrend; }
        public void setUpwardTrend(boolean upwardTrend) { isUpwardTrend = upwardTrend; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    }

    /**
     * 收益汇总
     */
    class YieldSummary {
        private String fundCode;
        private BigDecimal totalInvestment;
        private BigDecimal currentValue;
        private BigDecimal totalProfit;
        private BigDecimal totalYield;
        private BigDecimal annualizedYield;
        private BigDecimal bestDailyReturn;
        private BigDecimal worstDailyReturn;
        private BigDecimal averageDailyReturn;
        private BigDecimal volatility;
        private BigDecimal sharpeRatio;
        private LocalDate startDate;
        private LocalDate endDate;
        private int tradingDays;

        // getters and setters...
        public String getFundCode() { return fundCode; }
        public void setFundCode(String fundCode) { this.fundCode = fundCode; }
        public BigDecimal getTotalInvestment() { return totalInvestment; }
        public void setTotalInvestment(BigDecimal totalInvestment) { this.totalInvestment = totalInvestment; }
        public BigDecimal getCurrentValue() { return currentValue; }
        public void setCurrentValue(BigDecimal currentValue) { this.currentValue = currentValue; }
        public BigDecimal getTotalProfit() { return totalProfit; }
        public void setTotalProfit(BigDecimal totalProfit) { this.totalProfit = totalProfit; }
        public BigDecimal getTotalYield() { return totalYield; }
        public void setTotalYield(BigDecimal totalYield) { this.totalYield = totalYield; }
        public BigDecimal getAnnualizedYield() { return annualizedYield; }
        public void setAnnualizedYield(BigDecimal annualizedYield) { this.annualizedYield = annualizedYield; }
        public BigDecimal getBestDailyReturn() { return bestDailyReturn; }
        public void setBestDailyReturn(BigDecimal bestDailyReturn) { this.bestDailyReturn = bestDailyReturn; }
        public BigDecimal getWorstDailyReturn() { return worstDailyReturn; }
        public void setWorstDailyReturn(BigDecimal worstDailyReturn) { this.worstDailyReturn = worstDailyReturn; }
        public BigDecimal getAverageDailyReturn() { return averageDailyReturn; }
        public void setAverageDailyReturn(BigDecimal averageDailyReturn) { this.averageDailyReturn = averageDailyReturn; }
        public BigDecimal getVolatility() { return volatility; }
        public void setVolatility(BigDecimal volatility) { this.volatility = volatility; }
        public BigDecimal getSharpeRatio() { return sharpeRatio; }
        public void setSharpeRatio(BigDecimal sharpeRatio) { this.sharpeRatio = sharpeRatio; }
        public LocalDate getStartDate() { return startDate; }
        public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
        public LocalDate getEndDate() { return endDate; }
        public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
        public int getTradingDays() { return tradingDays; }
        public void setTradingDays(int tradingDays) { this.tradingDays = tradingDays; }
    }

    /**
     * 验证结果
     */
    class ValidationResult {
        private boolean isValid;
        private List<String> errors;
        private List<String> warnings;
        private List<String> suggestions;

        // getters and setters...
        public boolean isValid() { return isValid; }
        public void setValid(boolean valid) { isValid = valid; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
        public List<String> getSuggestions() { return suggestions; }
        public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    }
}