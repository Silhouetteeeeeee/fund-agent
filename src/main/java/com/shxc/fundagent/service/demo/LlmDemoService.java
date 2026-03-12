package com.shxc.fundagent.service.demo;

import com.shxc.fundagent.agent.AgentManager;
import com.shxc.fundagent.agent.model.AgentResult;
import com.shxc.fundagent.llm.LlmProvider;
import com.shxc.fundagent.llm.LlmProviderFactory;
import com.shxc.fundagent.llm.mock.MockLlmProvider;
import com.shxc.fundagent.llm.model.LlmRequest;
import com.shxc.fundagent.llm.model.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LLM和Agent演示服务
 * 展示如何使用LLM工具类和Agent框架
 */
@Service
public class LlmDemoService {

    private static final Logger logger = LoggerFactory.getLogger(LlmDemoService.class);

    @Autowired
    private LlmProviderFactory llmProviderFactory;

    @Autowired
    private AgentManager agentManager;

    /**
     * 演示基本的LLM调用
     */
    public Map<String, Object> demonstrateBasicLlmCall() {
        Map<String, Object> result = new HashMap<>();
        result.put("demo", "basic_llm_call");

        try {
            // 1. 注册一个Mock提供商用于演示
            MockLlmProvider mockProvider = new MockLlmProvider("demo-mock", "demo-model");
            mockProvider.setSmartReplies(true);
            mockProvider.setSimulatedDelayMs(300);

            llmProviderFactory.registerProvider(mockProvider);
            llmProviderFactory.setDefaultProvider("demo-mock");

            // 2. 创建简单的LLM请求
            Message userMessage = Message.user("请介绍一下什么是基金投资？");
            LlmRequest request = LlmRequest.builder()
                    .messages(List.of(userMessage))
                    .model("demo-model")
                    .temperature(0.7)
                    .maxTokens(500)
                    .build();

            // 3. 调用LLM
            LlmProvider provider = llmProviderFactory.getDefaultProvider();
            var llmResponse = provider.call(request);

            // 4. 收集结果
            result.put("success", llmResponse.isSuccess());
            result.put("response", llmResponse.getContent());
            result.put("provider", llmResponse.getProviderName());
            result.put("model", llmResponse.getModel());
            result.put("tokens", llmResponse.getTotalTokens());
            result.put("cost", llmResponse.getCost());
            result.put("responseTimeMs", llmResponse.getResponseTimeMs());

            logger.info("Basic LLM call demo completed successfully");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            logger.error("Basic LLM call demo failed", e);
        }

        return result;
    }

    /**
     * 演示多提供商管理
     */
    public Map<String, Object> demonstrateMultiProviderManagement() {
        Map<String, Object> result = new HashMap<>();
        result.put("demo", "multi_provider_management");

        try {
            // 1. 注册多个Mock提供商
            MockLlmProvider provider1 = new MockLlmProvider("mock-fast", "fast-model");
            provider1.setSimulatedDelayMs(100);
            provider1.setSuccessRate(0.99);

            MockLlmProvider provider2 = new MockLlmProvider("mock-smart", "smart-model");
            provider2.setSimulatedDelayMs(800);
            provider2.setSmartReplies(true);
            provider2.setSuccessRate(0.95);

            MockLlmProvider provider3 = new MockLlmProvider("mock-unreliable", "unreliable-model");
            provider3.setSimulatedDelayMs(200);
            provider3.setSuccessRate(0.7); // 较低成功率

            llmProviderFactory.registerProvider(provider1);
            llmProviderFactory.registerProvider(provider2);
            llmProviderFactory.registerProvider(provider3);

            // 2. 演示提供商统计
            Map<String, Object> stats = llmProviderFactory.getProviderStats();
            result.put("providerStats", stats);

            // 3. 演示获取可用提供商
            List<LlmProvider> availableProviders = llmProviderFactory.getAvailableProviders();
            result.put("availableProviderCount", availableProviders.size());

            // 4. 演示最佳提供商选择
            LlmProvider bestProvider = llmProviderFactory.getBestAvailableProvider();
            result.put("bestProvider", bestProvider.getProviderName());

            // 5. 演示切换默认提供商
            llmProviderFactory.setDefaultProvider("mock-smart");
            result.put("newDefaultProvider", "mock-smart");

            logger.info("Multi-provider management demo completed successfully");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            logger.error("Multi-provider management demo failed", e);
        }

        return result;
    }

    /**
     * 演示Agent框架使用
     */
    public Map<String, Object> demonstrateAgentFramework() {
        Map<String, Object> result = new HashMap<>();
        result.put("demo", "agent_framework");

        try {
            // 1. 准备LLM提供商（Agent需要使用）
            MockLlmProvider mockProvider = new MockLlmProvider("agent-mock", "agent-model");
            mockProvider.setSmartReplies(true);
            llmProviderFactory.registerProvider(mockProvider);

            // 2. 演示Agent统计（此时还没有Agent注册）
            Map<String, Object> agentStatsBefore = agentManager.getAgentStats();
            result.put("agentStatsBefore", agentStatsBefore);

            // 3. 创建一个简单的基金分析任务上下文
            Map<String, Object> fundContext = new HashMap<>();
            fundContext.put("fundCode", "000001");
            fundContext.put("fundName", "华夏成长混合");
            fundContext.put("netValue", "1.2345");
            fundContext.put("changePercent", "1.23");
            fundContext.put("riskLevel", "中风险");

            // 4. 演示自动选择Agent（此时没有Agent，应返回错误）
            AgentResult autoAgentResult = agentManager.processTask("分析基金投资价值", fundContext);
            result.put("autoAgentResultWithoutAgents", autoAgentResult);

            // 5. 演示Agent管理器功能
            result.put("hasAgents", !agentManager.getAllAgents().isEmpty());
            result.put("availableAgents", agentManager.getAvailableAgents().size());

            logger.info("Agent framework demo completed successfully");

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            logger.error("Agent framework demo failed", e);
        }

        return result;
    }

    /**
     * 演示完整的LLM+Agent工作流
     */
    public Map<String, Object> demonstrateCompleteWorkflow() {
        Map<String, Object> result = new HashMap<>();
        result.put("demo", "complete_workflow");
        result.put("steps", new String[] {
                "1. 初始化LLM提供商",
                "2. 配置Agent管理器",
                "3. 创建任务上下文",
                "4. 处理任务",
                "5. 分析结果"
        });

        try {
            // 步骤1: 初始化LLM提供商
            MockLlmProvider workflowProvider = new MockLlmProvider("workflow-mock", "workflow-model");
            workflowProvider.setSmartReplies(true);
            workflowProvider.setSimulatedDelayMs(200);
            llmProviderFactory.registerProvider(workflowProvider);
            llmProviderFactory.setDefaultProvider("workflow-mock");

            result.put("llmProviderInitialized", true);
            result.put("defaultProvider", llmProviderFactory.getDefaultProvider().getProviderName());

            // 步骤2: 演示直接LLM调用
            Message message = Message.user("用一句话介绍基金定投的优势");
            LlmRequest request = LlmRequest.builder()
                    .messages(List.of(message))
                    .maxTokens(200)
                    .build();

            LlmProvider provider = llmProviderFactory.getBestAvailableProvider();
            var llmResponse = provider.call(request);

            result.put("directLlmCall", Map.of(
                    "success", llmResponse.isSuccess(),
                    "response", llmResponse.getContent(),
                    "tokens", llmResponse.getTotalTokens(),
                    "responseTimeMs", llmResponse.getResponseTimeMs()
            ));

            // 步骤3: 演示成本估算
            double estimatedCost = provider.estimateCost(request);
            result.put("costEstimation", Map.of(
                    "estimatedTokens", provider.estimateTokens(request),
                    "estimatedCost", estimatedCost,
                    "actualCost", llmResponse.getCost()
            ));

            // 步骤4: 演示提供商状态管理
            result.put("providerStatus", Map.of(
                    "providerName", provider.getProviderName(),
                    "isAvailable", provider.isAvailable(),
                    "providerType", provider.getProviderType().name()
            ));

            logger.info("Complete workflow demo completed successfully");
            result.put("success", true);

        } catch (Exception e) {
            result.put("success", false);
            result.put("error", e.getMessage());
            logger.error("Complete workflow demo failed", e);
        }

        return result;
    }

    /**
     * 运行所有演示
     */
    public Map<String, Object> runAllDemos() {
        Map<String, Object> allResults = new HashMap<>();
        allResults.put("timestamp", System.currentTimeMillis());

        logger.info("Starting all LLM and Agent demos...");

        // 运行各个演示
        allResults.put("basicLlmDemo", demonstrateBasicLlmCall());
        allResults.put("multiProviderDemo", demonstrateMultiProviderManagement());
        allResults.put("agentFrameworkDemo", demonstrateAgentFramework());
        allResults.put("completeWorkflowDemo", demonstrateCompleteWorkflow());

        // 汇总结果
        boolean allSuccessful = true;
        for (Map.Entry<String, Object> entry : allResults.entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map<?, ?> demoResult = (Map<?, ?>) entry.getValue();
                if (demoResult.containsKey("success") && Boolean.FALSE.equals(demoResult.get("success"))) {
                    allSuccessful = false;
                    break;
                }
            }
        }

        allResults.put("allSuccessful", allSuccessful);
        logger.info("All demos completed. Overall success: {}", allSuccessful);

        return allResults;
    }

    /**
     * 清理演示资源
     */
    public void cleanup() {
        try {
            llmProviderFactory.shutdown();
            agentManager.shutdown();
            logger.info("Demo resources cleaned up");
        } catch (Exception e) {
            logger.error("Error cleaning up demo resources", e);
        }
    }
}