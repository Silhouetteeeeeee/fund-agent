package com.shxc.fundagent.agent.manager;

import com.shxc.fundagent.agent.Agent;
import com.shxc.fundagent.agent.AgentManager;
import com.shxc.fundagent.agent.AgentStatus;
import com.shxc.fundagent.agent.core.AgentV2;
import com.shxc.fundagent.agent.capabilities.Tool;
import com.shxc.fundagent.agent.capabilities.ToolCaller;
import com.shxc.fundagent.agent.capabilities.MemoryManager;
import com.shxc.fundagent.agent.model.v2.AgentContext;
import com.shxc.fundagent.agent.model.AgentResult;
import jdk.jfr.Label;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 增强的Agent管理器
 * 支持v2 Agent功能，包括工具调用、记忆管理、Agent编排等
 */
@Component
public class EnhancedAgentManager extends AgentManager {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedAgentManager.class);

    /**
     * v2 Agent索引
     */
    private final Map<String, AgentV2> v2Agents = new ConcurrentHashMap<>();

    /**
     * Agent工具调用器
     */
    @Autowired(required = false)
    private ToolCaller toolCaller;

    /**
     * Agent记忆管理器
     */
    @Autowired(required = false)
    private MemoryManager memoryManager;

    /**
     * Agent编排器
     */
    @Autowired(required = false)
    @Lazy
    private AgentOrchestrator agentOrchestrator;

    /**
     * Agent能力详细索引
     */
    private final Map<String, List<AgentV2>> v2CapabilityIndex = new ConcurrentHashMap<>();

    /**
     * 注册Agent（增强版，支持v2 Agent）
     */
    @Override
    public void registerAgent(Agent agent) {
        super.registerAgent(agent);

        // 如果是v2 Agent，进行额外注册
        if (agent instanceof AgentV2) {
            registerV2Agent((AgentV2) agent);
        }
    }

    /**
     * 注册v2 Agent
     */
    public void registerV2Agent(AgentV2 agent) {
        String name = agent.getName();

        if (v2Agents.containsKey(name)) {
            logger.warn("V2 Agent with name '{}' already registered, replacing", name);
        }

        v2Agents.put(name, agent);

        // 初始化v2 Agent的工具和记忆管理器
        initializeV2Agent(agent);

        // 更新v2能力索引
        updateV2CapabilityIndex(agent);

        logger.info("V2 Agent registered: {} - {}", name, agent.getDescription());
    }

    /**
     * 注销Agent（增强版）
     */
    @Override
    public void unregisterAgent(String agentName) {
        super.unregisterAgent(agentName);

        // 如果是v2 Agent，进行额外清理
        AgentV2 v2Agent = v2Agents.remove(agentName);
        if (v2Agent != null) {
            removeFromV2Indexes(v2Agent);
            logger.info("V2 Agent unregistered: {}", agentName);
        }
    }

    /**
     * 获取v2 Agent
     */
    public AgentV2 getV2Agent(String agentName) {
        return v2Agents.get(agentName);
    }

    /**
     * 获取所有v2 Agent
     */
    public List<AgentV2> getAllV2Agents() {
        return new ArrayList<>(v2Agents.values());
    }

    /**
     * 获取可用v2 Agent
     */
    public List<AgentV2> getAvailableV2Agents() {
        return v2Agents.values().stream()
                .filter(Agent::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * 根据能力查找v2 Agent
     */
    public List<AgentV2> findV2AgentsByCapability(String capability) {
        List<AgentV2> agentList = v2CapabilityIndex.get(capability);
        if (agentList == null) {
            return Collections.emptyList();
        }

        // 返回可用Agent
        return agentList.stream()
                .filter(Agent::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * 处理任务（使用v2上下文）
     */
    public AgentResult processTaskWithContext(String agentName, String task, AgentContext context) {
        AgentV2 agent = getV2Agent(agentName);

        if (agent == null) {
            // 尝试获取普通Agent并转换为v2上下文
            Agent regularAgent = getAgent(agentName);
            if (regularAgent == null) {
                return buildAgentNotFoundResult(agentName);
            }

            // 普通Agent不支持v2上下文，降级处理
            return processTaskWithAgent(agentName, task, convertContextToMessage(context));
        }

        if (!agent.isAvailable()) {
            return buildAgentUnavailableResult(agentName);
        }

        // 验证上下文
        AgentV2.ValidationResult validationResult = agent.validateContext(context);
        if (!validationResult.isValid()) {
            return buildInvalidContextResult(agentName, validationResult);
        }

        return agent.processWithTools(task, context);
    }

    /**
     * 指定Agent处理任务（v1 Agent兼容）
     */
    public AgentResult processTaskWithAgent(String agentName, String task, String message) {
        Agent agent = getAgent(agentName);

        if (agent == null) {
            return buildAgentNotFoundResult(agentName);
        }

        if (!agent.isAvailable()) {
            return buildAgentUnavailableResult(agentName);
        }

        return agent.process(task, message);
    }

    /**
     * 异步处理任务（使用v2上下文）
     */
    public CompletableFuture<AgentResult> processTaskWithContextAsync(String agentName, String task, AgentContext context) {
        AgentV2 agent = getV2Agent(agentName);

        if (agent == null) {
            // 尝试获取普通Agent
            Agent regularAgent = getAgent(agentName);
            if (regularAgent == null) {
                return CompletableFuture.completedFuture(buildAgentNotFoundResult(agentName));
            }

            // 普通Agent不支持异步v2上下文，降级处理
            if (regularAgent.processAsync(task, convertContextToMessage(context)) != null) {
                return regularAgent.processAsync(task, convertContextToMessage(context));
            } else {
                return CompletableFuture.completedFuture(regularAgent.process(task, convertContextToMessage(context)));
            }
        }

        if (!agent.isAvailable()) {
            return CompletableFuture.completedFuture(buildAgentUnavailableResult(agentName));
        }

        // 验证上下文
        AgentV2.ValidationResult validationResult = agent.validateContext(context);
        if (!validationResult.isValid()) {
            return CompletableFuture.completedFuture(buildInvalidContextResult(agentName, validationResult));
        }

        return agent.processWithToolsAsync(task, context);
    }

    /**
     * 编排多个Agent处理复杂任务
     */
    public AgentResult orchestrateTask(String task, AgentContext context) {
        if (agentOrchestrator == null) {
            logger.warn("Agent orchestrator not available, using default agent selection");
            return processTaskWithDefaultAgent(task, context);
        }

        return agentOrchestrator.orchestrate(task, context);
    }

    /**
     * 获取支持工具调用的Agent
     */
    public List<AgentV2> getAgentsWithToolSupport() {
        return v2Agents.values().stream()
                .filter(AgentV2::supportsToolCalling)
                .filter(Agent::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * 获取支持记忆管理的Agent
     */
    public List<AgentV2> getAgentsWithMemorySupport() {
        return v2Agents.values().stream()
                .filter(AgentV2::supportsMemoryManagement)
                .filter(Agent::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * 获取Agent的工具列表
     */
    public List<Tool> getAgentTools(String agentName) {
        AgentV2 agent = getV2Agent(agentName);
        if (agent == null || !agent.supportsToolCalling()) {
            return Collections.emptyList();
        }

        return agent.getAvailableTools();
    }

    /**
     * 调用Agent的工具
     */
    public com.shxc.fundagent.agent.capabilities.ToolResult callAgentTool(String agentName, String toolName, Map<String, Object> parameters) {
        if (toolCaller == null) {
            return com.shxc.fundagent.agent.capabilities.ToolResult.error(
                    "Tool caller not available", "TOOL_CALLER_UNAVAILABLE"
            );
        }

        // 检查Agent是否有权限调用该工具
        AgentV2 agent = getV2Agent(agentName);
        if (agent != null) {
            List<Tool> agentTools = agent.getAvailableTools();
            boolean hasTool = agentTools.stream().anyMatch(tool -> tool.getName().equals(toolName));
            if (!hasTool) {
                return com.shxc.fundagent.agent.capabilities.ToolResult.error(
                        String.format("Agent '%s' does not have tool '%s'", agentName, toolName),
                        "AGENT_TOOL_NOT_FOUND"
                );
            }
        }

        return toolCaller.callTool(toolName, parameters);
    }

    /**
     * 获取v2 Agent状态统计
     */
    public Map<String, Object> getV2AgentStats() {
        Map<String, Object> stats = new HashMap<>();

        int totalV2Agents = v2Agents.size();
        int availableV2Agents = (int) v2Agents.values().stream()
                .filter(Agent::isAvailable)
                .count();

        stats.put("totalV2Agents", totalV2Agents);
        stats.put("availableV2Agents", availableV2Agents);
        stats.put("v2AgentNames", new ArrayList<>(v2Agents.keySet()));

        // 按能力统计
        Map<String, Integer> capabilityCount = new HashMap<>();
        for (AgentV2 agent : v2Agents.values()) {
            String[] capabilities = agent.getCapabilities();
            if (capabilities != null) {
                for (String capability : capabilities) {
                    capabilityCount.put(capability, capabilityCount.getOrDefault(capability, 0) + 1);
                }
            }
        }
        stats.put("capabilityCount", capabilityCount);

        // 按特性统计
        int toolSupportCount = (int) v2Agents.values().stream()
                .filter(AgentV2::supportsToolCalling)
                .count();
        int memorySupportCount = (int) v2Agents.values().stream()
                .filter(AgentV2::supportsMemoryManagement)
                .count();

        stats.put("toolSupportCount", toolSupportCount);
        stats.put("memorySupportCount", memorySupportCount);

        return stats;
    }

    /**
     * 设置工具调用器
     */
    public void setToolCaller(ToolCaller toolCaller) {
        this.toolCaller = toolCaller;
        logger.info("Tool caller set for EnhancedAgentManager");
    }

    /**
     * 设置记忆管理器
     */
    public void setMemoryManager(MemoryManager memoryManager) {
        this.memoryManager = memoryManager;
        logger.info("Memory manager set for EnhancedAgentManager");
    }

    /**
     * 设置Agent编排器
     */
    public void setAgentOrchestrator(AgentOrchestrator agentOrchestrator) {
        this.agentOrchestrator = agentOrchestrator;
        logger.info("Agent orchestrator set for EnhancedAgentManager");
    }

    /**
     * 初始化v2 Agent
     */
    private void initializeV2Agent(AgentV2 agent) {
        // 设置工具调用器
        if (toolCaller != null && agent instanceof com.shxc.fundagent.agent.core.AbstractAgentV2) {
            ((com.shxc.fundagent.agent.core.AbstractAgentV2) agent).setToolCaller(toolCaller);
        }

        // 设置记忆管理器
        if (memoryManager != null && agent instanceof com.shxc.fundagent.agent.core.AbstractAgentV2) {
            ((com.shxc.fundagent.agent.core.AbstractAgentV2) agent).setMemoryManager(memoryManager);
        }

        logger.debug("V2 Agent initialized: {}", agent.getName());
    }

    /**
     * 更新v2能力索引
     */
    private void updateV2CapabilityIndex(AgentV2 agent) {
        String[] capabilities = agent.getCapabilities();
        if (capabilities != null) {
            for (String capability : capabilities) {
                v2CapabilityIndex
                        .computeIfAbsent(capability, k -> new ArrayList<>())
                        .add(agent);
            }
        }
    }

    /**
     * 从v2索引中移除Agent
     */
    private void removeFromV2Indexes(AgentV2 agent) {
        // 从能力索引中移除
        for (List<AgentV2> agentList : v2CapabilityIndex.values()) {
            agentList.remove(agent);
        }

        // 清理空列表
        v2CapabilityIndex.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * 使用默认Agent处理任务
     */
    private AgentResult processTaskWithDefaultAgent(String task, AgentContext context) {
        // 尝试找到适合任务的Agent
        List<AgentV2> candidates = findV2AgentsByTask(task);

        if (candidates.isEmpty()) {
            return buildNoSuitableAgentResult(task);
        }

        // 选择第一个可用Agent
        AgentV2 selectedAgent = candidates.get(0);
        return selectedAgent.processWithTools(task, context);
    }

    /**
     * 根据任务查找v2 Agent
     */
    private List<AgentV2> findV2AgentsByTask(String task) {
        // 简单实现：根据任务关键词匹配Agent能力
        List<AgentV2> candidates = new ArrayList<>();

        for (AgentV2 agent : v2Agents.values()) {
            if (!agent.isAvailable()) {
                continue;
            }

            String[] capabilities = agent.getCapabilities();
            if (capabilities != null) {
                for (String capability : capabilities) {
                    if (task.toLowerCase().contains(capability.toLowerCase())) {
                        candidates.add(agent);
                        break;
                    }
                }
            }
        }

        return candidates;
    }

    /**
     * 将v2上下文转换为v1消息
     */
    private String convertContextToMessage(AgentContext context) {
        if (context == null) {
            return "";
        }

        // 简单转换：使用任务和消息
        StringBuilder message = new StringBuilder();
        if (context.getTask() != null) {
            message.append("Task: ").append(context.getTask()).append("\n");
        }
        if (context.getMessage() != null) {
            message.append("Message: ").append(context.getMessage());
        }

        // 添加数据摘要
        if (context.getData() != null && !context.getData().isEmpty()) {
            message.append("\nData: ").append(context.getData().size()).append(" items");
        }

        return message.toString();
    }

    /**
     * 构建Agent未找到结果
     */
    private AgentResult buildAgentNotFoundResult(String agentName) {
        return AgentResult.builder()
                .agentName(agentName)
                .status(AgentResult.Status.ERROR)
                .content("Agent not found")
                .errorMessage("Agent not found: " + agentName)
                .errorCode("AGENT_NOT_FOUND")
                .build();
    }

    /**
     * 构建Agent不可用结果
     */
    private AgentResult buildAgentUnavailableResult(String agentName) {
        return AgentResult.builder()
                .agentName(agentName)
                .status(AgentResult.Status.ERROR)
                .content("Agent not available")
                .errorMessage("Agent not available: " + agentName)
                .errorCode("AGENT_UNAVAILABLE")
                .build();
    }

    /**
     * 构建无效上下文结果
     */
    private AgentResult buildInvalidContextResult(String agentName, AgentV2.ValidationResult validationResult) {
        return AgentResult.builder()
                .agentName(agentName)
                .status(AgentResult.Status.INVALID_INPUT)
                .content("Invalid agent context")
                .errorMessage(validationResult.getErrorMessage())
                .errorCode(validationResult.getErrorCode())
                .build();
    }

    /**
     * 构建无合适Agent结果
     */
    private AgentResult buildNoSuitableAgentResult(String task) {
        return AgentResult.builder()
                .status(AgentResult.Status.ERROR)
                .content("No suitable agent found")
                .errorMessage("No agent available for task: " + task)
                .errorCode("NO_AGENT_AVAILABLE")
                .build();
    }
}