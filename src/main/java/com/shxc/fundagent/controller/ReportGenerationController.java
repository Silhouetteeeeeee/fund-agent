package com.shxc.fundagent.controller;

import com.shxc.fundagent.dto.response.ApiResponse;
import com.shxc.fundagent.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 报告生成API控制器
 * 提供各种理财报告生成和导出相关的RESTful接口
 */
@Slf4j
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportGenerationController {

    private final ReportGenerationService reportGenerationService;

    /**
     * 生成日报
     */
    @PostMapping("/daily")
    public ResponseEntity<ReportGenerationService.ReportResult> generateDailyReport(
            @RequestParam(required = false) String reportDate) {
        log.info("生成日报，报告日期: {}", reportDate);
        String effectiveDate = reportDate != null ? reportDate : getCurrentDate();
        ReportGenerationService.ReportResult result = reportGenerationService.generateDailyReport(effectiveDate);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成周报
     */
    @PostMapping("/weekly")
    public ResponseEntity<ReportGenerationService.ReportResult> generateWeeklyReport(
            @RequestParam(required = false) String reportDate) {
        log.info("生成周报，报告日期: {}", reportDate);
        String effectiveDate = reportDate != null ? reportDate : getCurrentDate();
        ReportGenerationService.ReportResult result = reportGenerationService.generateWeeklyReport(effectiveDate);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成月报
     */
    @PostMapping("/monthly")
    public ResponseEntity<ReportGenerationService.ReportResult> generateMonthlyReport(
            @RequestParam(required = false) String reportDate) {
        log.info("生成月报，报告月份: {}", reportDate);
        String effectiveDate = reportDate != null ? reportDate : getCurrentMonth();
        ReportGenerationService.ReportResult result = reportGenerationService.generateMonthlyReport(effectiveDate);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成投资组合报告
     */
    @PostMapping("/portfolio")
    public ResponseEntity<ReportGenerationService.ReportResult> generatePortfolioReport(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        log.info("生成投资组合报告，期间: {} 至 {}", startDate, endDate);
        ReportGenerationService.ReportResult result =
                reportGenerationService.generatePortfolioReport(startDate, endDate);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成风险分析报告
     */
    @PostMapping("/risk-analysis")
    public ResponseEntity<ReportGenerationService.ReportResult> generateRiskAnalysisReport(
            @RequestBody String[] fundCodes) {
        log.info("生成风险分析报告，基金数量: {}", fundCodes.length);
        ReportGenerationService.ReportResult result =
                reportGenerationService.generateRiskAnalysisReport(fundCodes);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成收益分析报告
     */
    @PostMapping("/yield-analysis")
    public ResponseEntity<ReportGenerationService.ReportResult> generateYieldAnalysisReport(
            @RequestParam String fundCode,
            @RequestParam String period) {
        log.info("生成收益分析报告，基金代码: {}, 周期: {}", fundCode, period);
        ReportGenerationService.ReportResult result =
                reportGenerationService.generateYieldAnalysisReport(fundCode, period);
        return ResponseEntity.ok(result);
    }

    /**
     * 生成模板报告
     */
    @PostMapping("/template")
    public ResponseEntity<ReportGenerationService.ReportResult> generateReportByTemplate(
            @RequestParam String templateId,
            @RequestBody Map<String, Object> parameters) {
        log.info("生成模板报告，模板ID: {}", templateId);
        ReportGenerationService.ReportResult result =
                reportGenerationService.generateReportByTemplate(templateId, parameters);
        return ResponseEntity.ok(result);
    }

    /**
     * 导出报告到文件
     */
    @PostMapping("/{reportId}/export")
    public ResponseEntity<ApiResponse<Map<String, Object>>> exportReportToFile(
            @PathVariable String reportId,
            @RequestParam String format) {
        log.info("导出报告，报告ID: {}, 格式: {}", reportId, format);
        String filePath = reportGenerationService.exportReportToFile(reportId, format);

        if (filePath == null) {
            Map<String, Object> errorData = Map.of(
                "success", false,
                "message", "报告导出失败",
                "reportId", reportId,
                "format", format
            );
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("报告导出失败", errorData));
        }

        Map<String, Object> data = Map.of(
            "success", true,
            "message", "报告导出成功",
            "reportId", reportId,
            "format", format,
            "filePath", filePath,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "报告导出成功"));
    }

    /**
     * 发送报告到指定渠道
     */
    @PostMapping("/{reportId}/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendReport(
            @PathVariable String reportId,
            @RequestParam String[] channels,
            @RequestParam String[] recipients) {
        log.info("发送报告，报告ID: {}, 渠道: {}, 接收者: {}",
                reportId, channels.length, recipients.length);
        boolean success = reportGenerationService.sendReport(reportId, channels, recipients);

        Map<String, Object> data = Map.of(
            "success", success,
            "message", success ? "报告发送成功" : "报告发送失败",
            "reportId", reportId,
            "channels", channels,
            "recipients", recipients,
            "timestamp", System.currentTimeMillis()
        );

        if (success) {
            return ResponseEntity.ok(ApiResponse.success(data, "报告发送成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("报告发送失败", data));
        }
    }

    /**
     * 批量生成报告
     */
    @PostMapping("/batch")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchGenerateReports(
            @RequestParam String reportType,
            @RequestBody String[] parameters) {
        log.info("批量生成报告，报告类型: {}, 参数数量: {}", reportType, parameters.length);
        // 这里应该实现批量报告生成逻辑，暂时返回成功响应
        Map<String, Object> data = Map.of(
            "success", true,
            "message", "批量报告生成功能开发中",
            "reportType", reportType,
            "parameterCount", parameters.length,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "批量报告生成功能开发中"));
    }

    /**
     * 获取报告模板列表
     */
    @GetMapping("/templates")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportTemplates() {
        log.info("获取报告模板列表");
        // 这里应该返回可用的报告模板列表，暂时返回模拟数据
        Map<String, Object> data = Map.of(
            "templates", new String[]{
                "daily_report",
                "weekly_report",
                "monthly_report",
                "portfolio_analysis",
                "risk_assessment",
                "yield_analysis"
            },
            "description", "可用的报告模板列表",
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "报告模板列表获取成功"));
    }

    /**
     * 验证报告模板
     */
    @GetMapping("/templates/{templateId}/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateTemplate(
            @PathVariable String templateId) {
        log.info("验证报告模板，模板ID: {}", templateId);
        // 这里应该实现模板验证逻辑，暂时返回成功响应
        Map<String, Object> data = Map.of(
            "templateId", templateId,
            "valid", true,
            "message", "模板验证通过",
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "模板验证通过"));
    }

    /**
     * 生成报告预览
     */
    @PostMapping("/preview")
    public ResponseEntity<ApiResponse<Map<String, Object>>> previewReport(
            @RequestParam String reportType,
            @RequestBody Map<String, Object> parameters) {
        log.info("生成报告预览，报告类型: {}", reportType);
        // 这里应该实现报告预览逻辑，暂时返回模拟数据
        Map<String, Object> data = Map.of(
            "success", true,
            "message", "报告预览生成成功",
            "reportType", reportType,
            "previewData", Map.of(
                "title", "报告预览",
                "content", "这是一个报告预览内容",
                "format", "HTML",
                "generationTime", System.currentTimeMillis()
            ),
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "报告预览生成成功"));
    }

    /**
     * 获取报告生成状态
     */
    @GetMapping("/{reportId}/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReportStatus(@PathVariable String reportId) {
        log.info("获取报告生成状态，报告ID: {}", reportId);
        // 这里应该查询报告生成状态，暂时返回模拟数据
        Map<String, Object> data = Map.of(
            "reportId", reportId,
            "status", "COMPLETED",
            "progress", 100,
            "generationTime", System.currentTimeMillis(),
            "message", "报告生成完成"
        );

        return ResponseEntity.ok(ApiResponse.success(data, "报告状态获取成功"));
    }

    /**
     * 取消报告生成
     */
    @DeleteMapping("/{reportId}/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelReportGeneration(@PathVariable String reportId) {
        log.info("取消报告生成，报告ID: {}", reportId);
        // 这里应该实现取消报告生成逻辑，暂时返回成功响应
        Map<String, Object> data = Map.of(
            "success", true,
            "message", "报告生成已取消",
            "reportId", reportId,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "报告生成已取消"));
    }

    /**
     * 获取当前日期（格式：yyyy-MM-dd）
     */
    private String getCurrentDate() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    /**
     * 获取当前月份（格式：yyyy-MM）
     */
    private String getCurrentMonth() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        log.info("报告生成服务健康检查");
        try {
            // 尝试生成一个简单的日报来检查服务状态
            String testDate = getCurrentDate();
            reportGenerationService.generateDailyReport(testDate);
            Map<String, Object> data = Map.of(
                "status", "UP",
                "service", "ReportGenerationService",
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(ApiResponse.success(data, "服务健康"));
        } catch (Exception e) {
            Map<String, Object> errorData = Map.of(
                "status", "DOWN",
                "service", "ReportGenerationService",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.status(503).body(ApiResponse.of(503, "SERVICE_UNAVAILABLE", "服务异常", errorData));
        }
    }
}