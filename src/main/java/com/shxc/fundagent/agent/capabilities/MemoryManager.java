package com.shxc.fundagent.agent.capabilities;

import com.shxc.fundagent.entity.AgentMemory;
import java.util.List;
import java.util.Map;

/**
 * 记忆管理器接口
 * 负责管理Agent的短期、中期和长期记忆
 */
public interface MemoryManager {

    /**
     * 存储记忆
     * @param agentName Agent名称
     * @param content 记忆内容
     * @param type 记忆类型
     * @param importance 重要性评分（0.0-1.0）
     * @param metadata 元数据
     */
    void storeMemory(String agentName, String content, MemoryType type, double importance, Map<String, Object> metadata);

    /**
     * 存储记忆（简化版）
     */
    default void storeMemory(String agentName, String content, MemoryType type) {
        storeMemory(agentName, content, type, 0.5, Map.of());
    }

    /**
     * 检索相关记忆
     * @param agentName Agent名称
     * @param query 检索查询
     * @param type 记忆类型（null表示检索所有类型）
     * @param limit 返回数量限制
     * @return 相关记忆列表
     */
    List<AgentMemory> retrieveMemories(String agentName, String query, MemoryType type, int limit);

    /**
     * 检索相关记忆（使用默认限制）
     */
    default List<AgentMemory> retrieveMemories(String agentName, String query, MemoryType type) {
        return retrieveMemories(agentName, query, type, 10);
    }

    /**
     * 检索相关记忆（检索所有类型）
     */
    default List<AgentMemory> retrieveMemories(String agentName, String query, int limit) {
        return retrieveMemories(agentName, query, null, limit);
    }

    /**
     * 清理过期记忆
     * @param agentName Agent名称
     * @param type 记忆类型（null表示清理所有类型）
     * @return 清理的记忆数量
     */
    int cleanupExpiredMemories(String agentName, MemoryType type);

    /**
     * 根据重要性清理记忆
     * @param agentName Agent名称
     * @param type 记忆类型
     * @param importanceThreshold 重要性阈值，低于此值的记忆将被清理
     * @return 清理的记忆数量
     */
    int cleanupByImportance(String agentName, MemoryType type, double importanceThreshold);

    /**
     * 获取记忆统计信息
     * @param agentName Agent名称
     * @return 统计信息
     */
    MemoryStats getMemoryStats(String agentName);

    /**
     * 检查记忆管理器是否可用
     */
    boolean isAvailable();

    /**
     * 获取支持的记忆类型
     */
    List<MemoryType> getSupportedMemoryTypes();

    /**
     * 记忆类型枚举
     */
    enum MemoryType {
        /** 短期记忆 - 存储时间短，检索速度快（如Redis） */
        SHORT_TERM,
        /** 中期记忆 - 存储时间中等，支持结构化查询（如数据库） */
        MID_TERM,
        /** 长期记忆 - 存储时间长，支持语义检索（如向量数据库） */
        LONG_TERM
    }

    /**
     * 记忆统计信息
     */
    class MemoryStats {
        private final String agentName;
        private final Map<MemoryType, Integer> countByType;
        private final int totalCount;
        private final long oldestMemoryAgeMs;
        private final long newestMemoryAgeMs;
        private final double averageImportance;

        public MemoryStats(String agentName, Map<MemoryType, Integer> countByType, int totalCount,
                           long oldestMemoryAgeMs, long newestMemoryAgeMs, double averageImportance) {
            this.agentName = agentName;
            this.countByType = countByType;
            this.totalCount = totalCount;
            this.oldestMemoryAgeMs = oldestMemoryAgeMs;
            this.newestMemoryAgeMs = newestMemoryAgeMs;
            this.averageImportance = averageImportance;
        }

        public String getAgentName() {
            return agentName;
        }

        public Map<MemoryType, Integer> getCountByType() {
            return countByType;
        }

        public int getTotalCount() {
            return totalCount;
        }

        public long getOldestMemoryAgeMs() {
            return oldestMemoryAgeMs;
        }

        public long getNewestMemoryAgeMs() {
            return newestMemoryAgeMs;
        }

        public double getAverageImportance() {
            return averageImportance;
        }

        public int getCountByType(MemoryType type) {
            return countByType.getOrDefault(type, 0);
        }
    }
}