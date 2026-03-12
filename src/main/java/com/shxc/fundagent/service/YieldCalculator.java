package com.shxc.fundagent.service;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 收益率计算器
 * 提供各种收益计算方法的工具类
 */
@Component
public class YieldCalculator {

    /**
     * 计算收益率
     *
     * @param currentPrice 当前价格
     * @param costPrice    成本价格
     * @return 收益率（百分比），如 5.25 表示 5.25%
     */
    public BigDecimal calculateYieldRate(BigDecimal currentPrice, BigDecimal costPrice) {
        if (currentPrice == null || costPrice == null) {
            return BigDecimal.ZERO;
        }

        if (costPrice.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO; // 避免除零错误
        }

        try {
            return currentPrice.subtract(costPrice)
                    .divide(costPrice, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算收益金额
     *
     * @param currentPrice 当前价格
     * @param costPrice    成本价格
     * @param amount       数量/份额
     * @return 收益金额
     */
    public BigDecimal calculateProfitAmount(BigDecimal currentPrice, BigDecimal costPrice,
                                           BigDecimal amount) {
        if (currentPrice == null || costPrice == null || amount == null) {
            return BigDecimal.ZERO;
        }

        try {
            return currentPrice.subtract(costPrice)
                    .multiply(amount)
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算持仓市值
     *
     * @param currentPrice 当前价格
     * @param amount       数量/份额
     * @return 持仓市值
     */
    public BigDecimal calculateHoldingValue(BigDecimal currentPrice, BigDecimal amount) {
        if (currentPrice == null || amount == null) {
            return BigDecimal.ZERO;
        }

        try {
            return currentPrice.multiply(amount)
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算持仓成本总额
     *
     * @param costPrice 成本价格
     * @param amount    数量/份额
     * @return 成本总额
     */
    public BigDecimal calculateTotalCost(BigDecimal costPrice, BigDecimal amount) {
        if (costPrice == null || amount == null) {
            return BigDecimal.ZERO;
        }

        try {
            return costPrice.multiply(amount)
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算年化收益率
     *
     * @param totalYield     总收益率（百分比）
     * @param holdingDays    持有天数
     * @return 年化收益率（百分比）
     */
    public BigDecimal calculateAnnualizedYield(BigDecimal totalYield, int holdingDays) {
        if (totalYield == null || holdingDays <= 0) {
            return BigDecimal.ZERO;
        }

        try {
            // 年化收益率 = (1 + 总收益率)^(365/持有天数) - 1
            BigDecimal dailyYield = totalYield.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
            BigDecimal onePlusYield = BigDecimal.ONE.add(dailyYield);

            BigDecimal exponent = BigDecimal.valueOf(365)
                    .divide(BigDecimal.valueOf(holdingDays), 6, RoundingMode.HALF_UP);

            // 使用对数计算幂运算：a^b = exp(b * ln(a))
            double result = Math.exp(exponent.doubleValue() * Math.log(onePlusYield.doubleValue()));

            return BigDecimal.valueOf(result - 1)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算最大回撤
     *
     * @param prices 价格序列（按时间顺序）
     * @return 最大回撤（百分比）
     */
    public BigDecimal calculateMaxDrawdown(List<BigDecimal> prices) {
        if (prices == null || prices.size() < 2) {
            return BigDecimal.ZERO;
        }

        try {
            BigDecimal maxPrice = prices.get(0);
            BigDecimal maxDrawdown = BigDecimal.ZERO;

            for (BigDecimal price : prices) {
                if (price.compareTo(maxPrice) > 0) {
                    maxPrice = price;
                }

                BigDecimal drawdown = maxPrice.subtract(price)
                        .divide(maxPrice, 6, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));

                if (drawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = drawdown;
                }
            }

            return maxDrawdown.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算波动率（标准差）
     *
     * @param returns 收益率序列（百分比）
     * @return 波动率（百分比）
     */
    public BigDecimal calculateVolatility(List<BigDecimal> returns) {
        if (returns == null || returns.size() < 2) {
            return BigDecimal.ZERO;
        }

        try {
            // 计算平均值
            BigDecimal sum = returns.stream()
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal mean = sum.divide(BigDecimal.valueOf(returns.size()), 6, RoundingMode.HALF_UP);

            // 计算方差
            BigDecimal variance = BigDecimal.ZERO;
            for (BigDecimal ret : returns) {
                BigDecimal diff = ret.subtract(mean);
                variance = variance.add(diff.multiply(diff));
            }
            variance = variance.divide(BigDecimal.valueOf(returns.size() - 1), 6, RoundingMode.HALF_UP);

            // 计算标准差（波动率）
            double stdDev = Math.sqrt(variance.doubleValue());

            return BigDecimal.valueOf(stdDev)
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算夏普比率
     *
     * @param averageReturn 平均收益率（百分比）
     * @param riskFreeRate  无风险利率（百分比）
     * @param volatility    波动率（百分比）
     * @return 夏普比率
     */
    public BigDecimal calculateSharpeRatio(BigDecimal averageReturn, BigDecimal riskFreeRate,
                                          BigDecimal volatility) {
        if (averageReturn == null || riskFreeRate == null || volatility == null) {
            return BigDecimal.ZERO;
        }

        if (volatility.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO; // 避免除零错误
        }

        try {
            BigDecimal excessReturn = averageReturn.subtract(riskFreeRate);
            return excessReturn.divide(volatility, 4, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算投资组合收益率（加权平均）
     *
     * @param yields    各资产收益率列表（百分比）
     * @param weights   各资产权重列表（总和应为1）
     * @return 投资组合收益率（百分比）
     */
    public BigDecimal calculatePortfolioYield(List<BigDecimal> yields, List<BigDecimal> weights) {
        if (yields == null || weights == null || yields.size() != weights.size() || yields.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            BigDecimal portfolioYield = BigDecimal.ZERO;

            for (int i = 0; i < yields.size(); i++) {
                BigDecimal assetYield = yields.get(i);
                BigDecimal weight = weights.get(i);

                BigDecimal contribution = assetYield.multiply(weight);
                portfolioYield = portfolioYield.add(contribution);
            }

            return portfolioYield.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 计算累计收益率
     *
     * @param dailyReturns 日收益率序列（百分比）
     * @return 累计收益率（百分比）
     */
    public BigDecimal calculateCumulativeReturn(List<BigDecimal> dailyReturns) {
        if (dailyReturns == null || dailyReturns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            BigDecimal cumulative = BigDecimal.ONE;

            for (BigDecimal dailyReturn : dailyReturns) {
                // 将百分比转换为小数：1 + 收益率/100
                BigDecimal dailyFactor = BigDecimal.ONE.add(
                        dailyReturn.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
                cumulative = cumulative.multiply(dailyFactor);
            }

            // 转换回百分比：(累计值 - 1) * 100
            return cumulative.subtract(BigDecimal.ONE)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * 格式化收益率显示
     *
     * @param yield 收益率（百分比）
     * @return 格式化字符串，如 "+5.25%" 或 "-2.10%"
     */
    public String formatYield(BigDecimal yield) {
        if (yield == null) {
            return "0.00%";
        }

        String sign = yield.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        return String.format("%s%.2f%%", sign, yield);
    }

    /**
     * 格式化金额显示
     *
     * @param amount 金额
     * @return 格式化字符串，如 "¥1,234.56" 或 "-¥567.89"
     */
    public String formatAmount(BigDecimal amount) {
        if (amount == null) {
            return "¥0.00";
        }

        String sign = amount.compareTo(BigDecimal.ZERO) >= 0 ? "" : "-";
        BigDecimal absAmount = amount.abs();

        // 添加千分位分隔符
        java.text.DecimalFormat formatter = new java.text.DecimalFormat("###,###.##");
        return String.format("%s¥%s", sign, formatter.format(absAmount));
    }
}