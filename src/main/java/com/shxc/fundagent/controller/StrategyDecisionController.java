package com.shxc.fundagent.controller;

import com.shxc.fundagent.dto.response.ApiResponse;
import com.shxc.fundagent.entity.FundHolding;
import com.shxc.fundagent.strategy.StrategyDecisionEngine;
import com.shxc.fundagent.strategy.model.StrategyDecisionResult;
import com.shxc.fundagent.strategy.model.StrategyRuleConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 策略决策API控制器
 * 提供策略决策相关的RESTful接口
 */
@Slf4j
@RestController
@RequestMapping("/api/strategy")
@RequiredArgsConstructor
public class StrategyDecisionController {

    private final StrategyDecisionEngine strategyDecisionEngine;

    /**
     * 对单只基金进行策略决策
     */
    @GetMapping("/decide/{fundCode}")
    public ResponseEntity<StrategyDecisionResult> decideForFund(@PathVariable String fundCode) {
        log.info("对基金进行策略决策，基金代码: {}", fundCode);
        StrategyDecisionResult result = strategyDecisionEngine.decideForFund(fundCode);
        return ResponseEntity.ok(result);
    }

    /**
     * 批量对多只基金进行策略决策
     */
    @PostMapping("/batch-decide/funds")
    public ResponseEntity<Map<String, StrategyDecisionResult>> decideForFunds(@RequestBody List<String> fundCodes) {
        log.info("批量对基金进行策略决策，数量: {}", fundCodes.size());
        Map<String, StrategyDecisionResult> results = strategyDecisionEngine.decideForFunds(fundCodes);
        return ResponseEntity.ok(results);
    }

    /**
     * 对持仓进行策略决策
     */
    @PostMapping("/decide/holding")
    public ResponseEntity<StrategyDecisionResult> decideForHolding(@RequestBody FundHolding holding) {
        log.info("对持仓进行策略决策，持仓ID: {}, 基金代码: {}", holding.getId(), holding.getFundCode());
        StrategyDecisionResult result = strategyDecisionEngine.decideForHolding(holding);
        return ResponseEntity.ok(result);
    }

    /**
     * 批量对多个持仓进行策略决策
     */
    @PostMapping("/batch-decide/holdings")
    public ResponseEntity<Map<Long, StrategyDecisionResult>> decideForHoldings(@RequestBody List<FundHolding> holdings) {
        log.info("批量对持仓进行策略决策，数量: {}", holdings.size());
        Map<Long, StrategyDecisionResult> results = strategyDecisionEngine.decideForHoldings(holdings);
        return ResponseEntity.ok(results);
    }

    /**
     * 获取所有策略规则
     */
    @GetMapping("/rules")
    public ResponseEntity<List<StrategyRuleConfig>> getAllStrategyRules() {
        log.info("获取所有策略规则");
        List<StrategyRuleConfig> rules = strategyDecisionEngine.getAllStrategyRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * 根据规则ID获取策略规则
     */
    @GetMapping("/rules/{ruleId}")
    public ResponseEntity<StrategyRuleConfig> getStrategyRule(@PathVariable String ruleId) {
        log.info("获取策略规则，规则ID: {}", ruleId);
        StrategyRuleConfig rule = strategyDecisionEngine.getStrategyRuleById(ruleId);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rule);
    }

    /**
     * 添加新的策略规则
     */
    @PostMapping("/rules")
    public ResponseEntity<StrategyRuleConfig> addStrategyRule(@RequestBody StrategyRuleConfig ruleConfig) {
        log.info("添加策略规则，规则名称: {}", ruleConfig.getRuleName());
        StrategyRuleConfig addedRule = strategyDecisionEngine.addStrategyRule(ruleConfig);
        return ResponseEntity.ok(addedRule);
    }

    /**
     * 更新策略规则
     */
    @PutMapping("/rules/{ruleId}")
    public ResponseEntity<StrategyRuleConfig> updateStrategyRule(
            @PathVariable String ruleId,
            @RequestBody StrategyRuleConfig ruleConfig) {
        log.info("更新策略规则，规则ID: {}", ruleId);
        // 确保规则ID一致
        if (!ruleId.equals(ruleConfig.getRuleId())) {
            return ResponseEntity.badRequest().build();
        }
        StrategyRuleConfig updatedRule = strategyDecisionEngine.updateStrategyRule(ruleConfig);
        return ResponseEntity.ok(updatedRule);
    }

    /**
     * 启用或禁用策略规则
     */
    @PatchMapping("/rules/{ruleId}/status")
    public ResponseEntity<StrategyRuleConfig> updateRuleStatus(
            @PathVariable String ruleId,
            @RequestParam boolean enabled) {
        log.info("更新策略规则状态，规则ID: {}, 启用: {}", ruleId, enabled);
        StrategyRuleConfig updatedRule = strategyDecisionEngine.updateRuleStatus(ruleId, enabled);
        return ResponseEntity.ok(updatedRule);
    }

    /**
     * 删除策略规则
     */
    @DeleteMapping("/rules/{ruleId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteStrategyRule(@PathVariable String ruleId) {
        log.info("删除策略规则，规则ID: {}", ruleId);
        boolean success = strategyDecisionEngine.deleteStrategyRule(ruleId);

        Map<String, Object> data = Map.of(
            "success", success,
            "message", success ? "策略规则删除成功" : "策略规则删除失败",
            "ruleId", ruleId
        );

        if (success) {
            return ResponseEntity.ok(ApiResponse.success(data, "策略规则删除成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("策略规则删除失败", data));
        }
    }

    /**
     * 重新加载策略规则
     */
    @PostMapping("/rules/reload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reloadStrategyRules() {
        log.info("重新加载策略规则");
        strategyDecisionEngine.reloadStrategyRules();

        Map<String, Object> data = Map.of(
            "success", true,
            "message", "策略规则重新加载完成",
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "策略规则重新加载完成"));
    }

    /**
     * 获取决策引擎状态
     */
    @GetMapping("/engine-status")
    public ResponseEntity<StrategyDecisionEngine.EngineStatus> getEngineStatus() {
        log.info("获取决策引擎状态");
        StrategyDecisionEngine.EngineStatus status = strategyDecisionEngine.getEngineStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 获取决策统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<StrategyDecisionEngine.DecisionStatistics> getDecisionStatistics() {
        log.info("获取决策统计信息");
        StrategyDecisionEngine.DecisionStatistics statistics = strategyDecisionEngine.getDecisionStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * 清理决策缓存
     */
    @DeleteMapping("/cache")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clearCache(@RequestParam(defaultValue = "all") String cacheType) {
        log.info("清理决策缓存，缓存类型: {}", cacheType);
        strategyDecisionEngine.clearCache(cacheType);

        Map<String, Object> data = Map.of(
            "success", true,
            "message", "缓存清理完成",
            "cacheType", cacheType,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "缓存清理完成"));
    }

    /**
     * 执行决策建议
     */
    @PostMapping("/execute")
    public ResponseEntity<StrategyDecisionEngine.ExecutionResult> executeDecision(
            @RequestBody StrategyDecisionResult decisionResult,
            @RequestParam(defaultValue = "false") boolean requireConfirmation) {
        log.info("执行决策建议，基金代码: {}, 建议类型: {}",
                decisionResult.getFundCode(), decisionResult.getFinalSuggestion());
        StrategyDecisionEngine.ExecutionResult executionResult =
                strategyDecisionEngine.executeDecision(decisionResult, requireConfirmation);
        return ResponseEntity.ok(executionResult);
    }

    /**
     * 批量执行决策建议
     */
    @PostMapping("/batch-execute")
    public ResponseEntity<List<StrategyDecisionEngine.ExecutionResult>> executeDecisions(
            @RequestBody List<StrategyDecisionResult> decisionResults,
            @RequestParam(defaultValue = "false") boolean requireConfirmation) {
        log.info("批量执行决策建议，数量: {}", decisionResults.size());
        List<StrategyDecisionEngine.ExecutionResult> executionResults =
                strategyDecisionEngine.executeDecisions(decisionResults, requireConfirmation);
        return ResponseEntity.ok(executionResults);
    }

    /**
     * 获取决策引擎版本
     */
    @GetMapping("/version")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersion() {
        log.info("获取决策引擎版本");
        String version = strategyDecisionEngine.getVersion();

        Map<String, Object> data = Map.of(
            "version", version,
            "engineName", "FundAgent Strategy Decision Engine",
            "apiVersion", "1.0"
        );

        return ResponseEntity.ok(ApiResponse.success(data, "引擎版本获取成功"));
    }

    /**
     * 检查决策引擎是否就绪
     */
    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<Map<String, Object>>> isReady() {
        log.info("检查决策引擎是否就绪");
        boolean ready = strategyDecisionEngine.isReady();

        Map<String, Object> data = Map.of(
            "ready", ready,
            "message", ready ? "决策引擎已就绪" : "决策引擎未就绪",
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "引擎就绪状态检查完成"));
    }
}