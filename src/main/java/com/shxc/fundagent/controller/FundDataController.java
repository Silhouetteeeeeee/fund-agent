package com.shxc.fundagent.controller;

import com.shxc.fundagent.dto.response.ApiResponse;
import com.shxc.fundagent.entity.FundDailyData;
import com.shxc.fundagent.entity.FundInfo;
import com.shxc.fundagent.service.FundDataService;
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
 * 基金数据API控制器
 * 提供基金数据相关的RESTful接口
 */
@Slf4j
@RestController
@RequestMapping("/api/funds")
@RequiredArgsConstructor
public class FundDataController {

    private final FundDataService fundDataService;

    /**
     * 获取基金实时数据
     */
    @GetMapping("/{fundCode}/real-time")
    public ResponseEntity<FundDailyData> getRealTimeData(@PathVariable String fundCode) {
        log.info("获取基金实时数据，基金代码: {}", fundCode);
        FundDailyData data = fundDataService.getRealTimeData(fundCode);
        if (data == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(data);
    }

    /**
     * 批量获取基金实时数据
     */
    @PostMapping("/batch/real-time")
    public ResponseEntity<List<FundDailyData>> batchGetRealTimeData(@RequestBody List<String> fundCodes) {
        log.info("批量获取基金实时数据，数量: {}", fundCodes.size());
        List<FundDailyData> dataList = fundDataService.batchGetRealTimeData(fundCodes);
        return ResponseEntity.ok(dataList);
    }

    /**
     * 获取基金历史数据
     */
    @GetMapping("/{fundCode}/history")
    public ResponseEntity<List<FundDailyData>> getHistoryData(
            @PathVariable String fundCode,
            @RequestParam(defaultValue = "30") int days) {
        log.info("获取基金历史数据，基金代码: {}, 天数: {}", fundCode, days);
        List<FundDailyData> historyData = fundDataService.getHistoryData(fundCode, days);
        return ResponseEntity.ok(historyData);
    }

    /**
     * 获取基金基本信息
     */
    @GetMapping("/{fundCode}/basic-info")
    public ResponseEntity<FundInfo> getFundBasicInfo(@PathVariable String fundCode) {
        log.info("获取基金基本信息，基金代码: {}", fundCode);
        FundInfo fundInfo = fundDataService.getFundBasicInfo(fundCode);
        if (fundInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fundInfo);
    }

    /**
     * 同步基金数据
     */
    @PostMapping("/{fundCode}/sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncFundData(
            @PathVariable String fundCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("同步基金数据，基金代码: {}, 日期: {}", fundCode, date);
        LocalDate syncDate = date != null ? date : LocalDate.now();
        boolean success = fundDataService.syncFundData(fundCode, syncDate);

        Map<String, Object> data = Map.of(
            "success", success,
            "message", success ? "数据同步成功" : "数据同步失败",
            "fundCode", fundCode,
            "date", syncDate.toString()
        );

        if (success) {
            return ResponseEntity.ok(ApiResponse.success(data, "数据同步成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("数据同步失败", data));
        }
    }

    /**
     * 批量同步基金数据
     */
    @PostMapping("/batch/sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchSyncFundData(
            @RequestBody List<String> fundCodes,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        log.info("批量同步基金数据，数量: {}, 日期: {}", fundCodes.size(), date);
        LocalDate syncDate = date != null ? date : LocalDate.now();
        int successCount = fundDataService.batchSyncFundData(fundCodes, syncDate);

        Map<String, Object> data = Map.of(
            "success", true,
            "message", "批量同步完成",
            "total", fundCodes.size(),
            "successCount", successCount,
            "failureCount", fundCodes.size() - successCount,
            "date", syncDate.toString()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "批量同步完成"));
    }

    /**
     * 获取基金当前价格
     */
    @GetMapping("/{fundCode}/current-price")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentPrice(@PathVariable String fundCode) {
        log.info("获取基金当前价格，基金代码: {}", fundCode);
        BigDecimal price = fundDataService.getCurrentPrice(fundCode);

        if (price == null) {
            Map<String, Object> errorData = Map.of(
                "fundCode", fundCode,
                "message", "基金价格信息未找到"
            );
            return ResponseEntity.status(404).body(ApiResponse.notFound("基金价格信息未找到", errorData));
        }

        Map<String, Object> data = Map.of(
            "fundCode", fundCode,
            "currentPrice", price,
            "currency", "CNY"
        );

        return ResponseEntity.ok(ApiResponse.success(data, "价格获取成功"));
    }

    /**
     * 获取基金涨跌幅
     */
    @GetMapping("/{fundCode}/change-rate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getChangeRate(
            @PathVariable String fundCode,
            @RequestParam(required = false) String period) {
        log.info("获取基金涨跌幅，基金代码: {}, 周期: {}", fundCode, period);

        BigDecimal changeRate;
        if ("weekly".equalsIgnoreCase(period)) {
            changeRate = fundDataService.getWeeklyChangeRate(fundCode);
        } else if ("monthly".equalsIgnoreCase(period)) {
            changeRate = fundDataService.getMonthlyChangeRate(fundCode);
        } else {
            changeRate = fundDataService.getDailyChangeRate(fundCode);
        }

        if (changeRate == null) {
            Map<String, Object> errorData = Map.of(
                "fundCode", fundCode,
                "period", period != null ? period : "daily",
                "message", "涨跌幅信息未找到"
            );
            return ResponseEntity.status(404).body(ApiResponse.notFound("涨跌幅信息未找到", errorData));
        }

        Map<String, Object> data = Map.of(
            "fundCode", fundCode,
            "period", period != null ? period : "daily",
            "changeRate", changeRate,
            "unit", "%"
        );

        return ResponseEntity.ok(ApiResponse.success(data, "涨跌幅获取成功"));
    }

    /**
     * 验证基金代码
     */
    @GetMapping("/{fundCode}/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateFundCode(@PathVariable String fundCode) {
        log.info("验证基金代码: {}", fundCode);
        boolean isValid = fundDataService.validateFundCode(fundCode);

        Map<String, Object> data = Map.of(
            "fundCode", fundCode,
            "valid", isValid,
            "message", isValid ? "基金代码有效" : "基金代码无效"
        );

        return ResponseEntity.ok(ApiResponse.success(data, "基金代码验证完成"));
    }

    /**
     * 搜索基金
     */
    @GetMapping("/search")
    public ResponseEntity<List<FundInfo>> searchFunds(@RequestParam String keyword) {
        log.info("搜索基金，关键词: {}", keyword);
        List<FundInfo> funds = fundDataService.searchFunds(keyword);
        return ResponseEntity.ok(funds);
    }

    /**
     * 获取数据统计
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDataStatistics() {
        log.info("获取基金数据统计");
        Map<String, Object> statistics = fundDataService.getDataStatistics();
        return ResponseEntity.ok(ApiResponse.success(statistics, "数据统计获取成功"));
    }

    /**
     * 获取数据源健康状态
     */
    @GetMapping("/data-source-health")
    public ResponseEntity<ApiResponse<Map<String, String>>> getDataSourceHealthStatus() {
        log.info("获取数据源健康状态");
        Map<String, String> healthStatus = fundDataService.getDataSourceHealthStatus();
        return ResponseEntity.ok(ApiResponse.success(healthStatus, "数据源健康状态获取成功"));
    }

    /**
     * 手动触发数据采集
     */
    @PostMapping("/{fundCode}/collect")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerDataCollection(
            @PathVariable String fundCode,
            @RequestParam String dataType) {
        log.info("手动触发数据采集，基金代码: {}, 数据类型: {}", fundCode, dataType);
        boolean success = fundDataService.triggerDataCollection(fundCode, dataType);

        Map<String, Object> data = Map.of(
            "success", success,
            "message", success ? "数据采集已触发" : "数据采集触发失败",
            "fundCode", fundCode,
            "dataType", dataType
        );

        return ResponseEntity.ok(ApiResponse.success(data, success ? "数据采集已触发" : "数据采集触发失败"));
    }

    /**
     * 检查数据是否最新
     */
    @GetMapping("/{fundCode}/up-to-date")
    public ResponseEntity<ApiResponse<Map<String, Object>>> isDataUpToDate(@PathVariable String fundCode) {
        log.info("检查基金数据是否最新，基金代码: {}", fundCode);
        boolean upToDate = fundDataService.isDataUpToDate(fundCode);

        Map<String, Object> data = Map.of(
            "fundCode", fundCode,
            "upToDate", upToDate,
            "message", upToDate ? "数据是最新的" : "数据需要更新"
        );

        return ResponseEntity.ok(ApiResponse.success(data, "数据状态检查完成"));
    }

    /**
     * 清理过期数据
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanUpExpiredData(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate beforeDate) {
        log.info("清理过期数据，截止日期: {}", beforeDate);
        LocalDate cleanupDate = beforeDate != null ? beforeDate : LocalDate.now().minusMonths(6);
        int cleanedCount = fundDataService.cleanUpExpiredData(cleanupDate);

        Map<String, Object> data = Map.of(
            "success", true,
            "message", "过期数据清理完成",
            "beforeDate", cleanupDate.toString(),
            "cleanedCount", cleanedCount
        );

        return ResponseEntity.ok(ApiResponse.success(data, "过期数据清理完成"));
    }
}