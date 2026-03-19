package com.shxc.fundagent.controller;

import com.shxc.fundagent.service.MarketPerceptionDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器 - 用于手动触发定时任务进行测试
 */
@RestController
@RequestMapping("/test")
public class TestController {

    private static final Logger log = LoggerFactory.getLogger(TestController.class);

    @Autowired
    private com.shxc.fundagent.scheduling.FundTaskScheduler fundTaskScheduler;

    @Autowired
    private MarketPerceptionDataService marketPerceptionDataService;

    /**
     * 手动触发市场环境感知任务
     */
    @PostMapping("/trigger-market-perception")
    public ResponseEntity<Map<String, Object>> triggerMarketPerceptionTask() {
        Map<String, Object> result = new HashMap<>();
        try {
            log.info("手动触发市场环境感知任务...");
            
            // 通过反射调用私有方法
            var method = com.shxc.fundagent.scheduling.FundTaskScheduler.class.getDeclaredMethod("collectMarketPerceptionData");
            method.setAccessible(true);
            method.invoke(fundTaskScheduler);
            
            result.put("success", true);
            result.put("message", "市场环境感知任务已触发");
            
            // 获取今日市场摘要
            String summary = marketPerceptionDataService.getTodayMarketSummary();
            result.put("summary", summary);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("触发市场环境感知任务失败", e);
            result.put("success", false);
            result.put("message", "任务执行失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }

    /**
     * 获取今日市场感知数据
     */
    @GetMapping("/market-perception/today")
    public ResponseEntity<Map<String, Object>> getTodayMarketPerception() {
        Map<String, Object> result = new HashMap<>();
        try {
            String summary = marketPerceptionDataService.getTodayMarketSummary();
            boolean hasData = !"今日市场数据尚未采集".equals(summary);
            
            result.put("success", true);
            result.put("hasData", hasData);
            result.put("summary", summary);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取今日市场感知数据失败", e);
            result.put("success", false);
            result.put("message", "获取数据失败: " + e.getMessage());
            return ResponseEntity.internalServerError().body(result);
        }
    }
}
