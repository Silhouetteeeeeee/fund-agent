package com.shxc.fundagent.scheduling;

import com.shxc.fundagent.service.FundDataService;
import com.shxc.fundagent.service.ReportGenerationService;
import com.shxc.fundagent.service.YieldCalculationService;
import com.shxc.fundagent.strategy.StrategyDecisionEngine;
import com.shxc.fundagent.strategy.model.StrategyDecisionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 定时任务调度器测试类
 */
@ExtendWith(MockitoExtension.class)
class TaskSchedulerTest {

    @Mock
    private FundDataService fundDataService;

    @Mock
    private StrategyDecisionEngine strategyDecisionEngine;

    @Mock
    private YieldCalculationService yieldCalculationService;

    @Mock
    private ReportGenerationService reportGenerationService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private Cache cache;

    @InjectMocks
    private FundTaskScheduler taskScheduler;

    @BeforeEach
    void setUp() {
        // 基础设置（使用lenient避免不必要的存根警告）
        lenient().when(cacheManager.getCacheNames()).thenReturn(List.of("fundCache", "strategyCache"));
        lenient().when(cacheManager.getCache(anyString())).thenReturn(cache);
    }

    @Test
    void testCollectOpeningData_Success() {
        // 准备
        List<String> fundCodes = List.of("519674");
        when(fundDataService.batchGetRealTimeData(anyList())).thenReturn(null);

        // 执行（通过反射调用私有方法）
        invokePrivateMethod(taskScheduler, "collectOpeningData");

        // 验证
        verify(fundDataService, times(1)).batchGetRealTimeData(anyList());
    }

    @Test
    void testCollectClosingData_Success() {
        // 准备
        when(fundDataService.getRealTimeData(anyString())).thenReturn(null);

        // 执行
        invokePrivateMethod(taskScheduler, "collectClosingData");

        // 验证
        verify(fundDataService, atLeastOnce()).getRealTimeData(anyString());
    }

    @Test
    void testSyncWeekendData_Success() {
        // 准备
        when(fundDataService.getFundBasicInfo(anyString())).thenReturn(null);

        // 执行
        invokePrivateMethod(taskScheduler, "syncWeekendData");

        // 验证
        verify(fundDataService, atLeastOnce()).getFundBasicInfo(anyString());
    }

    @Test
    void testExecuteStrategyDecisions_Success() {
        // 准备
        StrategyDecisionResult mockResult = StrategyDecisionResult.builder().build();
        when(strategyDecisionEngine.decideForFund(anyString())).thenReturn(mockResult);

        // 执行
        invokePrivateMethod(taskScheduler, "executeStrategyDecisions");

        // 验证
        verify(strategyDecisionEngine, atLeastOnce()).decideForFund(anyString());
    }

    @Test
    void testCalculateHoldingYields_Success() {
        // 准备
        when(yieldCalculationService.calculateFundYield(anyString(), any())).thenReturn(null);

        // 执行
        invokePrivateMethod(taskScheduler, "calculateHoldingYields");

        // 验证
        verify(yieldCalculationService, atLeastOnce()).calculateFundYield(anyString(), any());
    }

    @Test
    void testGenerateDailyReport_Success() {
        // 准备
        when(reportGenerationService.generateDailyReport(anyString()))
                .thenReturn(new ReportGenerationService.ReportResult("DAILY_001", true, "成功", null));

        // 执行
        invokePrivateMethod(taskScheduler, "generateDailyReport");

        // 验证
        verify(reportGenerationService, times(1)).generateDailyReport(anyString());
    }

    @Test
    void testGenerateWeeklyReport_Success() {
        // 准备
        when(reportGenerationService.generateWeeklyReport(anyString()))
                .thenReturn(new ReportGenerationService.ReportResult("WEEKLY_001", true, "成功", null));

        // 执行
        invokePrivateMethod(taskScheduler, "generateWeeklyReport");

        // 验证
        verify(reportGenerationService, times(1)).generateWeeklyReport(anyString());
    }

    @Test
    void testGenerateMonthlyReport_Success() {
        // 准备
        when(reportGenerationService.generateMonthlyReport(anyString()))
                .thenReturn(new ReportGenerationService.ReportResult("MONTHLY_001", true, "成功", null));

        // 执行
        invokePrivateMethod(taskScheduler, "generateMonthlyReport");

        // 验证
        verify(reportGenerationService, times(1)).generateMonthlyReport(anyString());
    }

    @Test
    void testCleanupCache_Success() {
        // 执行
        invokePrivateMethod(taskScheduler, "cleanupCache");

        // 验证
        verify(cacheManager, atLeastOnce()).getCacheNames();
        verify(cache, atLeast(2)).clear();
        verify(strategyDecisionEngine, times(1)).clearCache(eq("all"));
    }

    @Test
    void testCheckTaskStatus_Success() {
        // 执行
        invokePrivateMethod(taskScheduler, "checkTaskStatus");

        // 验证：这个任务主要是日志记录，没有具体的业务逻辑
        assertNotNull(taskScheduler);
    }

    /**
     * 使用反射调用私有方法
     */
    private void invokePrivateMethod(Object object, String methodName) {
        try {
            var method = FundTaskScheduler.class.getDeclaredMethod(methodName);
            method.setAccessible(true);
            method.invoke(object);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke method: " + methodName, e);
        }
    }
}