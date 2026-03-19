package com.shxc.fundagent.controller;

import com.shxc.fundagent.attribution.BrinsonAttributionService;
import com.shxc.fundagent.attribution.model.*;
import com.shxc.fundagent.dto.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 归因分析控制器
 * 提供Brinson归因分析的RESTful接口
 */
@Slf4j
@RestController
@RequestMapping("/attribution")
@RequiredArgsConstructor
public class AttributionController {

    private final BrinsonAttributionService attributionService;

    /**
     * 执行Brinson归因分析
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 归因分析结果
     */
    @GetMapping("/brinson")
    public ApiResponse<BrinsonAttributionResult> performAttribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        log.info("收到归因分析请求: {} 至 {}", startDate, endDate);
        
        try {
            BrinsonAttributionResult result = attributionService.performAttribution(startDate, endDate);
            return ApiResponse.success(result);
        } catch (Exception e) {
            log.error("归因分析失败", e);
            return ApiResponse.error("归因分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取最近一个月的归因分析
     */
    @GetMapping("/brinson/recent")
    public ApiResponse<BrinsonAttributionResult> getRecentAttribution() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(1);
        
        return performAttribution(startDate, endDate);
    }

    /**
     * 获取最近一个季度的归因分析
     */
    @GetMapping("/brinson/quarter")
    public ApiResponse<BrinsonAttributionResult> getQuarterAttribution() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusMonths(3);
        
        return performAttribution(startDate, endDate);
    }

    /**
     * 获取最近一年的归因分析
     */
    @GetMapping("/brinson/year")
    public ApiResponse<BrinsonAttributionResult> getYearAttribution() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusYears(1);
        
        return performAttribution(startDate, endDate);
    }

    /**
     * 获取归因分析摘要
     */
    @GetMapping("/brinson/summary")
    public ApiResponse<String> getAttributionSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            BrinsonAttributionResult result = attributionService.performAttribution(startDate, endDate);
            return ApiResponse.success(result.getSummary());
        } catch (Exception e) {
            log.error("获取归因摘要失败", e);
            return ApiResponse.error("获取归因摘要失败: " + e.getMessage());
        }
    }

    /**
     * 获取资产配置分析
     */
    @GetMapping("/allocation")
    public ApiResponse<AllocationAnalysis> getAllocationAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            BrinsonAttributionResult result = attributionService.performAttribution(startDate, endDate);
            return ApiResponse.success(result.getAllocationAnalysis());
        } catch (Exception e) {
            log.error("获取配置分析失败", e);
            return ApiResponse.error("获取配置分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取资产类别归因明细
     */
    @GetMapping("/asset-class")
    public ApiResponse<List<AssetClassAttribution>> getAssetClassAttribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            BrinsonAttributionResult result = attributionService.performAttribution(startDate, endDate);
            return ApiResponse.success(result.getAssetClassAttributions());
        } catch (Exception e) {
            log.error("获取资产类别归因失败", e);
            return ApiResponse.error("获取资产类别归因失败: " + e.getMessage());
        }
    }

    /**
     * 获取基金归因明细
     */
    @GetMapping("/funds")
    public ApiResponse<List<FundAttribution>> getFundAttribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            BrinsonAttributionResult result = attributionService.performAttribution(startDate, endDate);
            return ApiResponse.success(result.getFundAttributions());
        } catch (Exception e) {
            log.error("获取基金归因失败", e);
            return ApiResponse.error("获取基金归因失败: " + e.getMessage());
        }
    }

    /**
     * 获取归因贡献度分析
     */
    @GetMapping("/contribution")
    public ApiResponse<AttributionContribution> getContributionAnalysis(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            BrinsonAttributionResult result = attributionService.performAttribution(startDate, endDate);
            return ApiResponse.success(result.getContributionAnalysis());
        } catch (Exception e) {
            log.error("获取贡献度分析失败", e);
            return ApiResponse.error("获取贡献度分析失败: " + e.getMessage());
        }
    }

    /**
     * 获取归因分析报表数据（用于前端图表展示）
     */
    @GetMapping("/report")
    public ApiResponse<Map<String, Object>> getAttributionReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            BrinsonAttributionResult result = attributionService.performAttribution(startDate, endDate);
            
            Map<String, Object> report = new HashMap<>();
            
            // 基本信息
            report.put("analysisPeriod", Map.of(
                "startDate", result.getStartDate(),
                "endDate", result.getEndDate(),
                "analysisDate", result.getAnalysisDate()
            ));
            
            // 收益数据
            report.put("returns", Map.of(
                "portfolioReturn", result.getPortfolioReturn(),
                "benchmarkReturn", result.getBenchmarkReturn(),
                "excessReturn", result.getExcessReturn()
            ));
            
            // Brinson三效应
            report.put("brinsonEffects", Map.of(
                "allocationEffect", result.getAllocationEffect(),
                "selectionEffect", result.getSelectionEffect(),
                "interactionEffect", result.getInteractionEffect(),
                "allocationRatio", result.getAllocationEffectRatio(),
                "selectionRatio", result.getSelectionEffectRatio(),
                "interactionRatio", result.getInteractionEffectRatio()
            ));
            
            // 资产类别归因（用于饼图/柱状图）
            List<Map<String, Object>> assetClassData = result.getAssetClassAttributions().stream()
                .map(a -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("assetClass", a.getAssetClass().getName());
                    data.put("portfolioWeight", a.getPortfolioWeight());
                    data.put("benchmarkWeight", a.getBenchmarkWeight());
                    data.put("allocationEffect", a.getAllocationEffect());
                    data.put("selectionEffect", a.getSelectionEffect());
                    data.put("totalEffect", a.getTotalEffect());
                    return data;
                })
                .toList();
            report.put("assetClassAttributions", assetClassData);
            
            // 基金归因（Top 10）
            List<Map<String, Object>> fundData = result.getFundAttributions().stream()
                .sorted((f1, f2) -> f2.getContributionToPortfolio().compareTo(f1.getContributionToPortfolio()))
                .limit(10)
                .map(f -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("fundCode", f.getFundCode());
                    data.put("fundName", f.getFundName());
                    data.put("weight", f.getPortfolioWeight());
                    data.put("return", f.getFundReturn());
                    data.put("contribution", f.getContributionToPortfolio());
                    data.put("rating", f.getPerformanceRating());
                    return data;
                })
                .toList();
            report.put("fundAttributions", fundData);
            
            // 配置分析
            if (result.getAllocationAnalysis() != null) {
                AllocationAnalysis alloc = result.getAllocationAnalysis();
                report.put("allocation", Map.of(
                    "portfolioEquityRatio", alloc.getPortfolioEquityRatio(),
                    "benchmarkEquityRatio", alloc.getBenchmarkEquityRatio(),
                    "equityDeviation", alloc.getEquityDeviation(),
                    "diversificationScore", alloc.getDiversificationScore(),
                    "allocationStyle", alloc.getAllocationStyle()
                ));
            }
            
            // 贡献度分析
            if (result.getContributionAnalysis() != null) {
                AttributionContribution contrib = result.getContributionAnalysis();
                report.put("contribution", Map.of(
                    "topPositiveAssetClass", contrib.getTopPositiveAssetClass() != null ? 
                        contrib.getTopPositiveAssetClass().getName() : null,
                    "topPositiveContribution", contrib.getTopPositiveContribution(),
                    "topNegativeAssetClass", contrib.getTopNegativeAssetClass() != null ? 
                        contrib.getTopNegativeAssetClass().getName() : null,
                    "topNegativeContribution", contrib.getTopNegativeContribution()
                ));
            }
            
            return ApiResponse.success(report);
            
        } catch (Exception e) {
            log.error("获取归因报表失败", e);
            return ApiResponse.error("获取归因报表失败: " + e.getMessage());
        }
    }

    /**
     * 验证归因平衡性
     */
    @GetMapping("/validate")
    public ApiResponse<Map<String, Object>> validateAttribution(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        try {
            BrinsonAttributionResult result = attributionService.performAttribution(startDate, endDate);
            
            Map<String, Object> validation = new HashMap<>();
            validation.put("isBalanced", result.isBalanced());
            validation.put("excessReturn", result.getExcessReturn());
            validation.put("totalAttribution", result.getTotalAttribution());
            validation.put("residual", result.getResidual());
            validation.put("calculationTimeMs", result.getCalculationTimeMs());
            
            if (!result.isBalanced()) {
                validation.put("warning", "归因不平衡，残差过大，请检查数据质量");
            }
            
            return ApiResponse.success(validation);
            
        } catch (Exception e) {
            log.error("验证归因失败", e);
            return ApiResponse.error("验证归因失败: " + e.getMessage());
        }
    }
}
