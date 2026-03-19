package com.shxc.fundagent.scheduling;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shxc.fundagent.agent.agents.impl.v2.MarketPerceptionAgent;
import com.shxc.fundagent.agent.model.AgentResult;
import com.shxc.fundagent.agent.model.v2.AgentContext;
import com.shxc.fundagent.agent.model.v2.MarketContext;
import com.shxc.fundagent.entity.MarketPerceptionData;
import com.shxc.fundagent.repository.MarketPerceptionDataRepository;
import com.shxc.fundagent.service.MarketPerceptionDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 市场环境感知定时任务测试类
 */
@ExtendWith(MockitoExtension.class)
class MarketPerceptionTaskTest {

    @Mock
    private MarketPerceptionAgent marketPerceptionAgent;

    @Mock
    private MarketPerceptionDataService marketPerceptionDataService;

    @Mock
    private MarketPerceptionDataRepository marketPerceptionDataRepository;

    @InjectMocks
    private FundTaskScheduler fundTaskScheduler;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    void testCollectMarketPerceptionData_Success() {
        // 准备 - 创建模拟的AgentResult
        AgentResult mockResult = createMockAgentResult();
        
        when(marketPerceptionAgent.processWithTools(anyString(), any(AgentContext.class)))
                .thenReturn(mockResult);

        // 创建模拟的保存后数据
        MarketPerceptionData savedData = createMockMarketPerceptionData();
        when(marketPerceptionDataService.saveMarketPerceptionData(any(AgentResult.class)))
                .thenReturn(savedData);

        when(marketPerceptionDataService.getTodayMarketSummary())
                .thenReturn("📊 今日市场概况 (2026-03-19)\n市场情绪: 乐观 (75/100)");

        // 执行 - 通过反射调用私有方法
        invokePrivateMethod(fundTaskScheduler, "collectMarketPerceptionData");

        // 验证
        verify(marketPerceptionAgent, times(1)).processWithTools(eq("collect-market-data"), any(AgentContext.class));
        verify(marketPerceptionDataService, times(1)).saveMarketPerceptionData(any(AgentResult.class));
        verify(marketPerceptionDataService, times(1)).getTodayMarketSummary();
    }

    @Test
    void testCollectMarketPerceptionData_AgentReturnsFailure() {
        // 准备 - Agent执行失败
        AgentResult failedResult = AgentResult.builder()
                .status(AgentResult.Status.ERROR)
                .errorMessage("数据采集失败")
                .build();
        
        when(marketPerceptionAgent.processWithTools(anyString(), any(AgentContext.class)))
                .thenReturn(failedResult);

        // 执行
        invokePrivateMethod(fundTaskScheduler, "collectMarketPerceptionData");

        // 验证 - 失败时不应该保存数据
        verify(marketPerceptionAgent, times(1)).processWithTools(anyString(), any(AgentContext.class));
        verify(marketPerceptionDataService, never()).saveMarketPerceptionData(any());
    }

    @Test
    void testCollectMarketPerceptionData_AgentThrowsException() {
        // 准备 - Agent抛出异常
        when(marketPerceptionAgent.processWithTools(anyString(), any(AgentContext.class)))
                .thenThrow(new RuntimeException("网络连接失败"));

        // 执行 - 不应该抛出异常，而是捕获并记录
        assertDoesNotThrow(() -> invokePrivateMethod(fundTaskScheduler, "collectMarketPerceptionData"));

        // 验证
        verify(marketPerceptionAgent, times(1)).processWithTools(anyString(), any(AgentContext.class));
        verify(marketPerceptionDataService, never()).saveMarketPerceptionData(any());
    }

    @Test
    void testCollectMarketPerceptionData_ServiceReturnsNull() {
        // 准备 - Agent成功但保存返回null
        AgentResult mockResult = createMockAgentResult();
        
        when(marketPerceptionAgent.processWithTools(anyString(), any(AgentContext.class)))
                .thenReturn(mockResult);
        when(marketPerceptionDataService.saveMarketPerceptionData(any(AgentResult.class)))
                .thenReturn(null);

        // 执行
        invokePrivateMethod(fundTaskScheduler, "collectMarketPerceptionData");

        // 验证
        verify(marketPerceptionAgent, times(1)).processWithTools(anyString(), any(AgentContext.class));
        verify(marketPerceptionDataService, times(1)).saveMarketPerceptionData(any(AgentResult.class));
        // 当savedData为null时，不应该调用getTodayMarketSummary
        verify(marketPerceptionDataService, never()).getTodayMarketSummary();
    }

    @Test
    void testMarketPerceptionDataService_SaveSuccess() {
        // 准备
        MarketPerceptionDataService service = new MarketPerceptionDataService(
                marketPerceptionDataRepository, objectMapper);
        
        AgentResult mockResult = createMockAgentResult();
        MarketPerceptionData mockData = createMockMarketPerceptionData();
        
        when(marketPerceptionDataRepository.findByMarketDate(any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(marketPerceptionDataRepository.save(any(MarketPerceptionData.class)))
                .thenReturn(mockData);

        // 执行
        MarketPerceptionData result = service.saveMarketPerceptionData(mockResult);

        // 验证
        assertNotNull(result);
        assertEquals(LocalDate.now(), result.getMarketDate());
        verify(marketPerceptionDataRepository, times(1)).save(any(MarketPerceptionData.class));
    }

    @Test
    void testMarketPerceptionDataService_SaveNullResult() {
        // 准备
        MarketPerceptionDataService service = new MarketPerceptionDataService(
                marketPerceptionDataRepository, objectMapper);

        // 执行
        MarketPerceptionData result = service.saveMarketPerceptionData(null);

        // 验证
        assertNull(result);
        verify(marketPerceptionDataRepository, never()).save(any());
    }

    @Test
    void testMarketPerceptionDataService_SaveFailedResult() {
        // 准备
        MarketPerceptionDataService service = new MarketPerceptionDataService(
                marketPerceptionDataRepository, objectMapper);
        
        AgentResult failedResult = AgentResult.builder()
                .status(AgentResult.Status.ERROR)
                .build();

        // 执行
        MarketPerceptionData result = service.saveMarketPerceptionData(failedResult);

        // 验证
        assertNull(result);
        verify(marketPerceptionDataRepository, never()).save(any());
    }

    @Test
    void testMarketPerceptionDataService_UpdateExistingData() {
        // 准备
        MarketPerceptionDataService service = new MarketPerceptionDataService(
                marketPerceptionDataRepository, objectMapper);
        
        AgentResult mockResult = createMockAgentResult();
        MarketPerceptionData existingData = createMockMarketPerceptionData();
        existingData.setId(1L);
        
        when(marketPerceptionDataRepository.findByMarketDate(any(LocalDate.class)))
                .thenReturn(Optional.of(existingData));
        when(marketPerceptionDataRepository.save(any(MarketPerceptionData.class)))
                .thenReturn(existingData);

        // 执行
        MarketPerceptionData result = service.saveMarketPerceptionData(mockResult);

        // 验证 - 应该更新现有记录
        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(marketPerceptionDataRepository, times(1)).save(any(MarketPerceptionData.class));
    }

    @Test
    void testMarketPerceptionDataService_GetTodayMarketSummary() {
        // 准备
        MarketPerceptionDataService service = new MarketPerceptionDataService(
                marketPerceptionDataRepository, objectMapper);
        
        MarketPerceptionData todayData = createMockMarketPerceptionData();
        when(marketPerceptionDataRepository.findTopByOrderByMarketDateDesc())
                .thenReturn(Optional.of(todayData));

        // 执行
        String summary = service.getTodayMarketSummary();

        // 验证
        assertNotNull(summary);
        assertTrue(summary.contains("今日市场概况"));
        assertTrue(summary.contains("市场情绪"));
    }

    @Test
    void testMarketPerceptionDataService_GetTodayMarketSummary_NoData() {
        // 准备
        MarketPerceptionDataService service = new MarketPerceptionDataService(
                marketPerceptionDataRepository, objectMapper);
        
        when(marketPerceptionDataRepository.findTopByOrderByMarketDateDesc())
                .thenReturn(Optional.empty());

        // 执行
        String summary = service.getTodayMarketSummary();

        // 验证
        assertEquals("今日市场数据尚未采集", summary);
    }

    @Test
    void testRepository_FindByMarketDate() {
        // 准备
        MarketPerceptionData mockData = createMockMarketPerceptionData();
        when(marketPerceptionDataRepository.findByMarketDate(any(LocalDate.class)))
                .thenReturn(Optional.of(mockData));

        // 执行
        Optional<MarketPerceptionData> result = marketPerceptionDataRepository.findByMarketDate(LocalDate.now());

        // 验证
        assertTrue(result.isPresent());
        assertEquals(LocalDate.now(), result.get().getMarketDate());
    }

    @Test
    void testRepository_ExistsByMarketDate() {
        // 准备
        when(marketPerceptionDataRepository.existsByMarketDate(any(LocalDate.class)))
                .thenReturn(true);

        // 执行
        boolean exists = marketPerceptionDataRepository.existsByMarketDate(LocalDate.now());

        // 验证
        assertTrue(exists);
    }

    /**
     * 创建模拟的AgentResult
     */
    private AgentResult createMockAgentResult() {
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("marketContext", createMockMarketContext());
        extraData.put("dataSource", "tencent");
        extraData.put("dataPoints", 10);
        extraData.put("processingTimeMs", 1500L);

        return AgentResult.builder()
                .status(AgentResult.Status.SUCCESS)
                .content("市场数据采集成功")
                .extraData(extraData)
                .build();
    }

    /**
     * 创建模拟的MarketContext
     */
    private MarketContext createMockMarketContext() {
        MarketContext context = new MarketContext();
        context.setMarketDate(LocalDate.now());
        context.setTimestamp(LocalDateTime.now());
        context.setMarketStatus(MarketContext.MarketStatus.BULL_MARKET);
        context.setRiskLevel(MarketContext.RiskLevel.MEDIUM);
        context.setSentimentScore(new BigDecimal("75.5"));
        context.setSentimentLevel("乐观");
        context.setMarketTemperature(new BigDecimal("0.75"));

        // 设置指数数据
        Map<String, MarketContext.IndexData> indexData = new HashMap<>();
        MarketContext.IndexData shIndex = new MarketContext.IndexData();
        shIndex.setCurrentValue(new BigDecimal("3050.50"));
        shIndex.setChangePercent(new BigDecimal("0.015"));
        indexData.put("sh000001", shIndex);
        context.setIndexData(indexData);

        return context;
    }

    /**
     * 创建模拟的MarketPerceptionData实体
     */
    private MarketPerceptionData createMockMarketPerceptionData() {
        return MarketPerceptionData.builder()
                .id(1L)
                .marketDate(LocalDate.now())
                .collectionTime(LocalDateTime.now())
                .dataSource("tencent")
                .marketStatus("BULL_MARKET")
                .riskLevel("MEDIUM")
                .sentimentScore(new BigDecimal("75.5"))
                .sentimentLevel("乐观")
                .marketTemperature(new BigDecimal("0.75"))
                .shIndex(new BigDecimal("3050.50"))
                .shIndexChangePct(new BigDecimal("0.015"))
                .northboundInflow(new BigDecimal("200000000"))
                .hasWarning(false)
                .marketAdvice("适度参与，控制仓位")
                .dataPoints(10)
                .processingTimeMs(1500L)
                .build();
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
