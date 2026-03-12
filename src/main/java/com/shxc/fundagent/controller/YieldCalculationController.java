package com.shxc.fundagent.controller;

import com.shxc.fundagent.dto.response.ApiResponse;
import com.shxc.fundagent.service.YieldCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 收益计算API控制器
 * 提供基金收益计算和分析相关的RESTful接口
 */
@Slf4j
@RestController
@RequestMapping("/api/yield")
@RequiredArgsConstructor
public class YieldCalculationController {

    private final YieldCalculationService yieldCalculationService;

    /**
     * 计算单只基金收益率
     */
    @GetMapping("/{fundCode}/calculate")
    public ResponseEntity<YieldCalculationService.FundYield> calculateFundYield(
            @PathVariable String fundCode,
            @RequestParam(required = false) BigDecimal costPrice) {
        log.info("计算基金收益率，基金代码: {}, 持仓成本: {}", fundCode, costPrice);
        YieldCalculationService.FundYield yield = yieldCalculationService.calculateFundYield(fundCode, costPrice);
        return ResponseEntity.ok(yield);
    }

    /**
     * 批量计算基金收益率
     */
    @PostMapping("/batch-calculate")
    public ResponseEntity<List<YieldCalculationService.FundYield>> batchCalculateFundYields(
            @RequestBody List<String> fundCodes) {
        log.info("批量计算基金收益率，数量: {}", fundCodes.size());
        List<YieldCalculationService.FundYield> yields = yieldCalculationService.batchCalculateFundYields(fundCodes);
        return ResponseEntity.ok(yields);
    }

    /**
     * 计算整体持仓收益
     */
    @GetMapping("/portfolio")
    public ResponseEntity<YieldCalculationService.PortfolioYield> calculatePortfolioYield() {
        log.info("计算整体持仓收益");
        YieldCalculationService.PortfolioYield portfolioYield = yieldCalculationService.calculatePortfolioYield();
        return ResponseEntity.ok(portfolioYield);
    }

    /**
     * 计算指定持仓记录的收益
     */
    @GetMapping("/holding/{holdingId}")
    public ResponseEntity<YieldCalculationService.HoldingYield> calculateHoldingYield(
            @PathVariable Long holdingId) {
        log.info("计算持仓收益，持仓ID: {}", holdingId);
        YieldCalculationService.HoldingYield holdingYield = yieldCalculationService.calculateHoldingYield(holdingId);
        return ResponseEntity.ok(holdingYield);
    }

    /**
     * 计算基金历史收益率
     */
    @GetMapping("/{fundCode}/history")
    public ResponseEntity<List<YieldCalculationService.HistoricalYield>> calculateHistoricalYields(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "30") int days) {
        log.info("计算基金历史收益率，基金代码: {}, 天数: {}", fundCode, days);
        List<YieldCalculationService.HistoricalYield> historicalYields =
                yieldCalculationService.calculateHistoricalYields(fundCode, days);
        return ResponseEntity.ok(historicalYields);
    }

    /**
     * 计算基金风险指标
     */
    @GetMapping("/{fundCode}/risk-metrics")
    public ResponseEntity<YieldCalculationService.RiskMetrics> calculateRiskMetrics(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "90") int days) {
        log.info("计算基金风险指标，基金代码: {}, 天数: {}", fundCode, days);
        YieldCalculationService.RiskMetrics riskMetrics =
                yieldCalculationService.calculateRiskMetrics(fundCode, days);
        return ResponseEntity.ok(riskMetrics);
    }

    /**
     * 计算资产配置分析
     */
    @GetMapping("/asset-allocation")
    public ResponseEntity<YieldCalculationService.AssetAllocationAnalysis> analyzeAssetAllocation() {
        log.info("计算资产配置分析");
        YieldCalculationService.AssetAllocationAnalysis analysis =
                yieldCalculationService.analyzeAssetAllocation();
        return ResponseEntity.ok(analysis);
    }

    /**
     * 计算收益对比
     */
    @PostMapping("/compare")
    public ResponseEntity<YieldCalculationService.YieldComparison> compareYields(
            @RequestBody List<String> fundCodes,
            @RequestParam(defaultValue = "30") int days) {
        log.info("计算收益对比，基金数量: {}, 天数: {}", fundCodes.size(), days);
        YieldCalculationService.YieldComparison comparison =
                yieldCalculationService.compareYields(fundCodes, days);
        return ResponseEntity.ok(comparison);
    }

    /**
     * 计算收益趋势
     */
    @GetMapping("/{fundCode}/trend")
    public ResponseEntity<YieldCalculationService.YieldTrend> calculateYieldTrend(
            @PathVariable String fundCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        log.info("计算收益趋势，基金代码: {}, 开始日期: {}, 结束日期: {}", fundCode, startDate, endDate);
        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now().minusMonths(3);
        LocalDate effectiveEndDate = endDate != null ? endDate : LocalDate.now();

        YieldCalculationService.YieldTrend yieldTrend =
                yieldCalculationService.calculateYieldTrend(fundCode, effectiveStartDate, effectiveEndDate);
        return ResponseEntity.ok(yieldTrend);
    }

    /**
     * 计算收益汇总报告
     */
    @GetMapping("/{fundCode}/summary")
    public ResponseEntity<YieldCalculationService.YieldSummary> calculateYieldSummary(
            @PathVariable String fundCode) {
        log.info("计算收益汇总报告，基金代码: {}", fundCode);
        YieldCalculationService.YieldSummary yieldSummary =
                yieldCalculationService.calculateYieldSummary(fundCode);
        return ResponseEntity.ok(yieldSummary);
    }

    /**
     * 更新持仓市值
     */
    @PutMapping("/{fundCode}/holding-value")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateHoldingValue(@PathVariable String fundCode) {
        log.info("更新持仓市值，基金代码: {}", fundCode);
        BigDecimal newValue = yieldCalculationService.updateHoldingValue(fundCode);

        Map<String, Object> data = Map.of(
            "fundCode", fundCode,
            "holdingValue", newValue,
            "updated", newValue != null,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "持仓市值更新成功"));
    }

    /**
     * 批量更新持仓市值
     */
    @PutMapping("/batch-update-holding-values")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchUpdateHoldingValues() {
        log.info("批量更新持仓市值");
        int successCount = yieldCalculationService.batchUpdateHoldingValues();

        Map<String, Object> data = Map.of(
            "success", true,
            "message", "批量更新完成",
            "successCount", successCount,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "批量更新完成"));
    }

    /**
     * 计算收益分配
     */
    @PostMapping("/profit-distribution")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculateProfitDistribution(
            @RequestParam BigDecimal totalAmount,
            @RequestBody List<BigDecimal> weights) {
        log.info("计算收益分配，总金额: {}, 权重数量: {}", totalAmount, weights.size());
        List<BigDecimal> distribution =
                yieldCalculationService.calculateProfitDistribution(totalAmount, weights);

        Map<String, Object> data = Map.of(
            "totalAmount", totalAmount,
            "weights", weights,
            "distribution", distribution,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "收益分配计算完成"));
    }

    /**
     * 验证收益计算数据
     */
    @GetMapping("/{fundCode}/validate")
    public ResponseEntity<YieldCalculationService.ValidationResult> validateYieldData(
            @PathVariable String fundCode) {
        log.info("验证收益计算数据，基金代码: {}", fundCode);
        YieldCalculationService.ValidationResult validationResult =
                yieldCalculationService.validateYieldData(fundCode);
        return ResponseEntity.ok(validationResult);
    }

    /**
     * 获取收益计算统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getYieldStatistics() {
        log.info("获取收益计算统计");
        Map<String, Object> statistics = yieldCalculationService.getYieldStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics, "收益计算统计获取成功"));
    }

    /**
     * 重新计算所有持仓收益
     */
    @PostMapping("/recalculate-all")
    public ResponseEntity<ApiResponse<Map<String, Object>>> recalculateAllYields() {
        log.info("重新计算所有持仓收益");
        int recalculatedCount = yieldCalculationService.recalculateAllYields();

        Map<String, Object> data = Map.of(
            "success", true,
            "message", "重新计算完成",
            "recalculatedCount", recalculatedCount,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "重新计算完成"));
    }

    /**
     * 导出收益数据
     */
    @GetMapping("/{fundCode}/export")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportYieldData(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "JSON") String format) {
        log.info("导出收益数据，基金代码: {}, 格式: {}", fundCode, format);
        String exportData = yieldCalculationService.exportYieldData(fundCode, format);

        if (exportData == null) {
            Map<String, Object> errorData = Map.of(
                "success", false,
                "message", "导出失败",
                "fundCode", fundCode,
                "format", format
            );
            return ResponseEntity.badRequest().body(ApiResponse.of(400, "BAD_REQUEST", "导出失败", errorData));
        }

        Map<String, Object> data = Map.of(
            "success", true,
            "message", "导出成功",
            "fundCode", fundCode,
            "format", format,
            "data", exportData,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "导出成功"));
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        log.info("收益计算服务健康检查");
        try {
            yieldCalculationService.getYieldStatistics();
            Map<String, Object> data = Map.of(
                "status", "UP",
                "service", "YieldCalculationService",
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(ApiResponse.success(data, "服务健康"));
        } catch (Exception e) {
            Map<String, Object> errorData = Map.of(
                "status", "DOWN",
                "service", "YieldCalculationService",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.status(503).body(ApiResponse.of(503, "SERVICE_UNAVAILABLE", "服务异常", errorData));
        }
    }
}