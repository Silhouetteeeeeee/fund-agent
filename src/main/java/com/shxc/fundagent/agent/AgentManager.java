package com.shxc.fundagent.agent;

import com.shxc.fundagent.agent.model.AgentResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Agent管理器
 * 负责注册、管理、分配Agent任务
 */
@Component
public class AgentManager {

    private static final Logger logger = LoggerFactory.getLogger(AgentManager.class);

    /**
     * 所有注册的Agent（按名称索引）
     */
    private final Map<String, Agent> agents = new ConcurrentHashMap<>();

    /**
     * Agent能力索引（能力 -> Agent列表）
     */
    private final Map<String, List<Agent>> capabilityIndex = new ConcurrentHashMap<>();

    /**
     * Agent上下文类型索引（上下文类型 -> Agent列表）
     */
    private final Map<String, List<Agent>> contextTypeIndex = new ConcurrentHashMap<>();

    /**
     * 注册Agent
     */
    public void registerAgent(Agent agent) {
        String name = agent.getName();

        if (agents.containsKey(name)) {
            logger.warn("Agent with name '{}' already registered, replacing", name);
        }

        agents.put(name, agent);
        updateIndexes(agent);

        logger.info("Agent registered: {} - {}", name, agent.getDescription());
    }

    /**
     * 注销Agent
     */
    public void unregisterAgent(String agentName) {
        Agent agent = agents.remove(agentName);
        if (agent != null) {
            removeFromIndexes(agent);
            logger.info("Agent unregistered: {}", agentName);
        }
    }

    /**
     * 获取所有Agent
     */
    public List<Agent> getAllAgents() {
        return new ArrayList<>(agents.values());
    }

    /**
     * 获取Agent
     */
    public Agent getAgent(String agentName) {
        return agents.get(agentName);
    }

    /**
     * 检查Agent是否存在
     */
    public boolean hasAgent(String agentName) {
        return agents.containsKey(agentName);
    }

    /**
     * 获取可用Agent列表
     */
    public List<Agent> getAvailableAgents() {
        return agents.values().stream()
                .filter(Agent::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * 根据能力查找Agent
     */
    public List<Agent> findAgentsByCapability(String capability) {
        List<Agent> agentList = capabilityIndex.get(capability);
        if (agentList == null) {
            return Collections.emptyList();
        }

        // 返回可用Agent
        return agentList.stream()
                .filter(Agent::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * 根据上下文类型查找Agent
     */
    public List<Agent> findAgentsByContextType(String contextType) {
        List<Agent> agentList = contextTypeIndex.get(contextType);
        if (agentList == null) {
            return Collections.emptyList();
        }

        // 返回可用Agent
        return agentList.stream()
                .filter(Agent::isAvailable)
                .collect(Collectors.toList());
    }

    /**
     * 根据任务描述自动选择Agent
     */
    public Agent selectAgentForTask(String task, Map<String, Object> context) {
        // 1. 根据上下文类型筛选
        Set<Agent> candidates = new HashSet<>();

        if (context != null && !context.isEmpty()) {
            for (String contextType : context.keySet()) {
                List<Agent> agentsWithContext = findAgentsByContextType(contextType);
                candidates.addAll(agentsWithContext);
            }
        }

        // 如果没有根据上下文找到，使用所有可用Agent
        if (candidates.isEmpty()) {
            candidates.addAll(getAvailableAgents());
        }

        // 2. 根据任务描述匹配能力（简单关键词匹配）
        List<Agent> scoredAgents = scoreAgentsByTask(candidates, task);

        if (scoredAgents.isEmpty()) {
            logger.warn("No suitable agent found for task: {}", task);
            return null;
        }

        // 3. 返回分数最高的Agent
        Agent selectedAgent = scoredAgents.get(0);
        logger.debug("Selected agent '{}' for task: {}", selectedAgent.getName(), task);
        return selectedAgent;
    }

//    /**
//     * 处理任务（自动选择Agent）
//     */
//    public AgentResult processTask(String task, Map<String, Object> context) {
//        Agent agent = selectAgentForTask(task, context);
//
//        if (agent == null) {
//            return AgentResult.builder()
//                    .status(AgentResult.Status.ERROR)
//                    .content("No suitable agent found")
//                    .errorMessage("No agent available for task: " + task)
//                    .errorCode("NO_AGENT_AVAILABLE")
//                    .build();
//        }
//
//        return agent.process(task, context);
//    }

//    /**
//     * 异步处理任务
//     */
//    public CompletableFuture<AgentResult> processTaskAsync(String task, Map<String, Object> context) {
//        Agent agent = selectAgentForTask(task, context);
//
//        if (agent == null) {
//            return CompletableFuture.completedFuture(
//                AgentResult.builder()
//                    .status(AgentResult.Status.ERROR)
//                    .content("No suitable agent found")
//                    .errorMessage("No agent available for task: " + task)
//                    .errorCode("NO_AGENT_AVAILABLE")
//                    .build()
//            );
//        }
//
//        return agent.processAsync(task, context);
//    }

//    /**
//     * 指定Agent处理任务
//     */
//    public AgentResult processTaskWithAgent(String agentName, String task, Map<String, Object> context) {
//        Agent agent = getAgent(agentName);
//
//        if (agent == null) {
//            return AgentResult.builder()
//                    .status(AgentResult.Status.ERROR)
//                    .content("Agent not found")
//                    .errorMessage("Agent not found: " + agentName)
//                    .errorCode("AGENT_NOT_FOUND")
//                    .build();
//        }
//
//        if (!agent.isAvailable()) {
//            return AgentResult.builder()
//                    .status(AgentResult.Status.ERROR)
//                    .content("Agent not available")
//                    .errorMessage("Agent not available: " + agentName)
//                    .errorCode("AGENT_UNAVAILABLE")
//                    .build();
//        }
//
//        return agent.process(task, context);
//    }

    /**
     * 获取Agent状态统计
     */
    public Map<String, Object> getAgentStats() {
        Map<String, Object> stats = new HashMap<>();

        int totalAgents = agents.size();
        int availableAgents = (int) agents.values().stream()
                .filter(Agent::isAvailable)
                .count();

        stats.put("totalAgents", totalAgents);
        stats.put("availableAgents", availableAgents);
        stats.put("agentNames", new ArrayList<>(agents.keySet()));

        // 按状态统计
        Map<AgentStatus, Integer> statusCount = new HashMap<>();
        for (Agent agent : agents.values()) {
            AgentStatus status = agent.getStatus();
            statusCount.put(status, statusCount.getOrDefault(status, 0) + 1);
        }
        stats.put("statusCount", statusCount);

        return stats;
    }

    /**
     * 更新索引
     */
    private void updateIndexes(Agent agent) {
        // 能力索引
        String[] capabilities = agent.getCapabilities();
        if (capabilities != null) {
            for (String capability : capabilities) {
                capabilityIndex
                        .computeIfAbsent(capability, k -> new ArrayList<>())
                        .add(agent);
            }
        }

        // 上下文类型索引
        String[] contextTypes = agent.getSupportedContextTypes();
        if (contextTypes != null) {
            for (String contextType : contextTypes) {
                contextTypeIndex
                        .computeIfAbsent(contextType, k -> new ArrayList<>())
                        .add(agent);
            }
        }
    }

    /**
     * 从索引中移除Agent
     */
    private void removeFromIndexes(Agent agent) {
        // 从能力索引中移除
        for (List<Agent> agentList : capabilityIndex.values()) {
            agentList.remove(agent);
        }

        // 从上下文类型索引中移除
        for (List<Agent> agentList : contextTypeIndex.values()) {
            agentList.remove(agent);
        }

        // 清理空列表
        capabilityIndex.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        contextTypeIndex.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    /**
     * 根据任务为Agent评分
     */
    private List<Agent> scoreAgentsByTask(Collection<Agent> agents, String task) {
        // 简单实现：根据Agent能力描述与任务关键词的匹配度评分
        // 实际应用中可以使用更复杂的NLP匹配算法

        List<AgentScore> scores = new ArrayList<>();

        for (Agent agent : agents) {
            int score = 0;
            String[] capabilities = agent.getCapabilities();

            if (capabilities != null) {
                for (String capability : capabilities) {
                    if (task.toLowerCase().contains(capability.toLowerCase())) {
                        score += 10; // 能力匹配加分
                    }
                }
            }

            // 考虑Agent描述
            String description = agent.getDescription();
            if (description != null) {
                String[] words = task.toLowerCase().split("\\s+");
                for (String word : words) {
                    if (word.length() > 3 && description.toLowerCase().contains(word)) {
                        score += 5; // 描述匹配加分
                    }
                }
            }

            scores.add(new AgentScore(agent, score));
        }

        // 按分数排序
        scores.sort((a, b) -> Integer.compare(b.score, a.score));

        return scores.stream()
                .filter(s -> s.score > 0) // 只返回有匹配分数的Agent
                .map(s -> s.agent)
                .collect(Collectors.toList());
    }

    /**
     * Agent评分包装类
     */
    private static class AgentScore {
        final Agent agent;
        final int score;

        AgentScore(Agent agent, int score) {
            this.agent = agent;
            this.score = score;
        }
    }

    /**
     * 清理所有Agent
     */
    public void shutdown() {
        for (Agent agent : agents.values()) {
            if (agent instanceof AbstractAgent) {
                ((AbstractAgent) agent).shutdown();
            }
        }
        agents.clear();
        capabilityIndex.clear();
        contextTypeIndex.clear();
        logger.info("AgentManager shutdown completed");
    }
}