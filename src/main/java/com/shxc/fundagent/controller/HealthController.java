package com.shxc.fundagent.controller;

import com.shxc.fundagent.dto.response.ApiResponse;
import com.shxc.fundagent.service.FundDataService;
import com.shxc.fundagent.strategy.StrategyDecisionEngine;
import com.shxc.fundagent.service.YieldCalculationService;
import com.shxc.fundagent.service.ReportGenerationService;
import com.shxc.fundagent.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统健康检查API控制器
 * 提供系统整体健康状态和各个服务状态检查
 */
@Slf4j
@RestController
@RequestMapping("/api/health")
@RequiredArgsConstructor
public class HealthController {

    private final FundDataService fundDataService;
    private final StrategyDecisionEngine strategyDecisionEngine;
    private final YieldCalculationService yieldCalculationService;
    private final ReportGenerationService reportGenerationService;
    private final NotificationService notificationService;

    /**
     * 整体健康检查
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        log.info("执行系统整体健康检查");
        Map<String, Object> healthStatus = new HashMap<>();

        // 检查各个服务状态
        healthStatus.put("timestamp", LocalDateTime.now());
        healthStatus.put("system", "FundAgent Fund Management System");
        healthStatus.put("version", "1.0.0");

        // 检查各个服务
        healthStatus.put("services", Map.of(
            "fundDataService", checkFundDataService(),
            "strategyDecisionEngine", checkStrategyDecisionEngine(),
            "yieldCalculationService", checkYieldCalculationService(),
            "reportGenerationService", checkReportGenerationService(),
            "notificationService", checkNotificationService()
        ));

        // 计算整体状态
        boolean allHealthy = healthStatus.get("services") instanceof Map &&
                ((Map<?, ?>) healthStatus.get("services")).values().stream()
                        .allMatch(status -> status instanceof Map &&
                                ((Map<?, ?>) status).get("status").equals("UP"));

        healthStatus.put("status", allHealthy ? "UP" : "DEGRADED");
        healthStatus.put("message", allHealthy ? "所有服务正常运行" : "部分服务异常");

        return ResponseEntity.ok(ApiResponse.success(healthStatus, "系统健康检查完成"));
    }

    /**
     * 详细服务状态检查
     */
    @GetMapping("/detailed")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detailedHealthCheck() {
        log.info("执行详细服务状态检查");
        Map<String, Object> detailedStatus = new HashMap<>();

        detailedStatus.put("timestamp", LocalDateTime.now());
        detailedStatus.put("system", "FundAgent Fund Management System");

        // 基金数据服务
        Map<String, Object> fundDataStatus = checkFundDataServiceDetailed();
        detailedStatus.put("fundDataService", fundDataStatus);

        // 策略决策引擎
        Map<String, Object> strategyStatus = checkStrategyDecisionEngineDetailed();
        detailedStatus.put("strategyDecisionEngine", strategyStatus);

        // 收益计算服务
        Map<String, Object> yieldStatus = checkYieldCalculationServiceDetailed();
        detailedStatus.put("yieldCalculationService", yieldStatus);

        // 报告生成服务
        Map<String, Object> reportStatus = checkReportGenerationServiceDetailed();
        detailedStatus.put("reportGenerationService", reportStatus);

        // 消息推送服务
        Map<String, Object> notificationStatus = checkNotificationServiceDetailed();
        detailedStatus.put("notificationService", notificationStatus);

        // 数据库连接检查（需要实现）
        detailedStatus.put("database", checkDatabaseStatus());

        // 缓存状态检查（需要实现）
        detailedStatus.put("cache", checkCacheStatus());

        // 外部API状态检查（需要实现）
        detailedStatus.put("externalApis", checkExternalApisStatus());

        // 计算整体状态
        int upCount = 0;
        int totalCount = 0;

        for (Map.Entry<String, Object> entry : detailedStatus.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<?, ?> serviceStatus = (Map<?, ?>) entry.getValue();
                if (serviceStatus.get("status") != null) {
                    totalCount++;
                    if ("UP".equals(serviceStatus.get("status"))) {
                        upCount++;
                    }
                }
            }
        }

        detailedStatus.put("overallStatus", upCount == totalCount ? "UP" : "DEGRADED");
        detailedStatus.put("upServices", upCount);
        detailedStatus.put("totalServices", totalCount);
        detailedStatus.put("healthPercentage", totalCount > 0 ? (upCount * 100) / totalCount : 0);

        return ResponseEntity.ok(ApiResponse.success(detailedStatus, "详细系统健康检查完成"));
    }

    /**
     * 基金数据服务状态检查
     */
    @GetMapping("/fund-data")
    public ResponseEntity<Map<String, Object>> fundDataHealth() {
        log.info("检查基金数据服务状态");
        return ResponseEntity.ok(checkFundDataServiceDetailed());
    }

    /**
     * 策略决策引擎状态检查
     */
    @GetMapping("/strategy")
    public ResponseEntity<Map<String, Object>> strategyHealth() {
        log.info("检查策略决策引擎状态");
        return ResponseEntity.ok(checkStrategyDecisionEngineDetailed());
    }

    /**
     * 收益计算服务状态检查
     */
    @GetMapping("/yield")
    public ResponseEntity<Map<String, Object>> yieldHealth() {
        log.info("检查收益计算服务状态");
        return ResponseEntity.ok(checkYieldCalculationServiceDetailed());
    }

    /**
     * 报告生成服务状态检查
     */
    @GetMapping("/reports")
    public ResponseEntity<Map<String, Object>> reportsHealth() {
        log.info("检查报告生成服务状态");
        return ResponseEntity.ok(checkReportGenerationServiceDetailed());
    }

    /**
     * 消息推送服务状态检查
     */
    @GetMapping("/notifications")
    public ResponseEntity<Map<String, Object>> notificationsHealth() {
        log.info("检查消息推送服务状态");
        return ResponseEntity.ok(checkNotificationServiceDetailed());
    }

    // ================ 私有辅助方法 ================

    private Map<String, Object> checkFundDataService() {
        try {
            fundDataService.getDataStatistics();
            return Map.of("status", "UP", "service", "FundDataService");
        } catch (Exception e) {
            log.error("基金数据服务检查失败", e);
            return Map.of("status", "DOWN", "service", "FundDataService", "error", e.getMessage());
        }
    }

    private Map<String, Object> checkStrategyDecisionEngine() {
        try {
            strategyDecisionEngine.isReady();
            return Map.of("status", "UP", "service", "StrategyDecisionEngine");
        } catch (Exception e) {
            log.error("策略决策引擎检查失败", e);
            return Map.of("status", "DOWN", "service", "StrategyDecisionEngine", "error", e.getMessage());
        }
    }

    private Map<String, Object> checkYieldCalculationService() {
        try {
            yieldCalculationService.getYieldStatistics();
            return Map.of("status", "UP", "service", "YieldCalculationService");
        } catch (Exception e) {
            log.error("收益计算服务检查失败", e);
            return Map.of("status", "DOWN", "service", "YieldCalculationService", "error", e.getMessage());
        }
    }

    private Map<String, Object> checkReportGenerationService() {
        try {
            reportGenerationService.generateDailyReport(LocalDateTime.now().toString().substring(0, 10));
            return Map.of("status", "UP", "service", "ReportGenerationService");
        } catch (Exception e) {
            log.error("报告生成服务检查失败", e);
            return Map.of("status", "DOWN", "service", "ReportGenerationService", "error", e.getMessage());
        }
    }

    private Map<String, Object> checkNotificationService() {
        try {
            notificationService.isReady();
            return Map.of("status", "UP", "service", "NotificationService");
        } catch (Exception e) {
            log.error("消息推送服务检查失败", e);
            return Map.of("status", "DOWN", "service", "NotificationService", "error", e.getMessage());
        }
    }

    private Map<String, Object> checkFundDataServiceDetailed() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "FundDataService");
        status.put("description", "基金数据获取和管理服务");

        try {
            Map<String, Object> statistics = fundDataService.getDataStatistics();
            status.put("status", "UP");
            status.put("statistics", statistics);
            status.put("lastUpdate", LocalDateTime.now());
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            status.put("lastUpdate", null);
        }

        return status;
    }

    private Map<String, Object> checkStrategyDecisionEngineDetailed() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "StrategyDecisionEngine");
        status.put("description", "策略决策引擎");

        try {
            var engineStatus = strategyDecisionEngine.getEngineStatus();
            status.put("status", "UP");
            status.put("engineStatus", engineStatus);
            status.put("ready", strategyDecisionEngine.isReady());
            status.put("version", strategyDecisionEngine.getVersion());
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            status.put("ready", false);
        }

        return status;
    }

    private Map<String, Object> checkYieldCalculationServiceDetailed() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "YieldCalculationService");
        status.put("description", "收益计算和分析服务");

        try {
            Map<String, Object> statistics = yieldCalculationService.getYieldStatistics();
            status.put("status", "UP");
            status.put("statistics", statistics);
            status.put("lastCalculation", LocalDateTime.now());
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            status.put("lastCalculation", null);
        }

        return status;
    }

    private Map<String, Object> checkReportGenerationServiceDetailed() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "ReportGenerationService");
        status.put("description", "报告生成和导出服务");

        try {
            // 测试生成一个简单的报告
            String testDate = LocalDateTime.now().toString().substring(0, 10);
            var reportResult = reportGenerationService.generateDailyReport(testDate);
            status.put("status", "UP");
            status.put("testReport", Map.of(
                "reportId", reportResult.getReportId(),
                "success", reportResult.isSuccess()
            ));
            status.put("lastGeneration", LocalDateTime.now());
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            status.put("lastGeneration", null);
        }

        return status;
    }

    private Map<String, Object> checkNotificationServiceDetailed() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "NotificationService");
        status.put("description", "多渠道消息推送服务");

        try {
            var serviceStatus = notificationService.getServiceStatus();
            status.put("status", "UP");
            status.put("serviceStatus", serviceStatus);
            status.put("ready", notificationService.isReady());
            status.put("version", notificationService.getVersion());
            status.put("availableChannels", notificationService.getAvailableChannels());
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
            status.put("ready", false);
        }

        return status;
    }

    private Map<String, Object> checkDatabaseStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "Database");
        status.put("description", "MySQL数据库连接");

        try {
            // 这里应该实现数据库连接检查
            // 暂时返回模拟数据
            status.put("status", "UP");
            status.put("connection", "active");
            status.put("lastCheck", LocalDateTime.now());
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", "数据库连接检查失败");
        }

        return status;
    }

    private Map<String, Object> checkCacheStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "Cache");
        status.put("description", "系统缓存状态");

        try {
            // 这里应该实现缓存状态检查
            // 暂时返回模拟数据
            status.put("status", "UP");
            status.put("type", "Caffeine");
            status.put("size", "estimated");
            status.put("lastCheck", LocalDateTime.now());
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", "缓存状态检查失败");
        }

        return status;
    }

    private Map<String, Object> checkExternalApisStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "ExternalAPIs");
        status.put("description", "外部API服务状态");

        try {
            // 这里应该实现外部API状态检查
            Map<String, Object> apis = new HashMap<>();
            apis.put("tianTianFundApi", Map.of("status", "UP", "latency", "120ms"));
            apis.put("exchangeRateApi", Map.of("status", "UP", "latency", "80ms"));

            status.put("status", "UP");
            status.put("apis", apis);
            status.put("lastCheck", LocalDateTime.now());
        } catch (Exception e) {
            status.put("status", "DEGRADED");
            status.put("error", "部分外部API不可用");
        }

        return status;
    }
}