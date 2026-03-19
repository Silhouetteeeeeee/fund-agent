package com.shxc.fundagent.agent.capabilities.impl;

import com.shxc.fundagent.agent.capabilities.MemoryManager;
import com.shxc.fundagent.entity.AgentMemory;
import com.shxc.fundagent.repository.AgentMemoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 基于数据库的记忆管理器实现
 * 支持短期、中期、长期记忆存储（目前主要使用中期记忆 - 数据库存储）
 */
@Component
public class DatabaseMemoryManager implements MemoryManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMemoryManager.class);

    private final AgentMemoryRepository agentMemoryRepository;

    @Autowired
    public DatabaseMemoryManager(AgentMemoryRepository agentMemoryRepository) {
        this.agentMemoryRepository = agentMemoryRepository;
    }

    @Override
    public void storeMemory(String agentName, String content, MemoryType type, double importance, Map<String, Object> metadata) {
        try {
            AgentMemory memory = new AgentMemory();
            memory.setAgentName(agentName);
            memory.setMemoryType(type);
            memory.setContent(content);
            memory.setImportanceScore(importance);
            memory.setCreatedAt(LocalDateTime.now());

            // 设置元数据
            if (metadata != null && !metadata.isEmpty()) {
                Map<String, Object> metadataMap = new HashMap<>(metadata);
                memory.setMetadata(metadataMap);
            }

            // 根据记忆类型设置不同的过期策略
            setExpirationPolicy(memory, type);

            AgentMemory savedMemory = agentMemoryRepository.save(memory);
            logger.debug("记忆存储成功: agent={}, type={}, id={}", agentName, type, savedMemory.getId());

        } catch (Exception e) {
            logger.error("记忆存储失败: agent={}, type={}", agentName, type, e);
            throw new RuntimeException("记忆存储失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<AgentMemory> retrieveMemories(String agentName, String query, MemoryType type, int limit) {
        try {
            List<AgentMemory> memories;

            if (query == null || query.trim().isEmpty()) {
                // 如果没有查询条件，返回最近的记忆
                if (type != null) {
                    memories = agentMemoryRepository.findByAgentNameAndMemoryType(agentName, type);
                } else {
                    memories = agentMemoryRepository.findByAgentName(agentName);
                }

                // 按创建时间降序排序，限制数量
                memories.sort(Comparator.comparing(AgentMemory::getCreatedAt).reversed());
                if (memories.size() > limit) {
                    memories = memories.subList(0, Math.min(limit, memories.size()));
                }

            } else {
                // 有关键字查询，使用搜索功能
                Pageable pageable = PageRequest.of(0, limit);
                memories = agentMemoryRepository.searchMemoriesByKeyword(agentName, query, pageable);

                // 如果按类型过滤
                if (type != null) {
                    memories = memories.stream()
                            .filter(memory -> memory.getMemoryType() == type)
                            .collect(Collectors.toList());
                }
            }

            // 更新访问信息
            updateAccessInfo(memories);

            logger.debug("记忆检索成功: agent={}, query={}, type={}, limit={}, found={}",
                    agentName, query, type, limit, memories.size());

            return memories;

        } catch (Exception e) {
            logger.error("记忆检索失败: agent={}, query={}, type={}", agentName, query, type, e);
            return Collections.emptyList();
        }
    }

    @Override
    public int cleanupExpiredMemories(String agentName, MemoryType type) {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<AgentMemory> expiredMemories = agentMemoryRepository.findExpiredMemories(now);

            // 按Agent名称和类型过滤
            if (agentName != null) {
                expiredMemories = expiredMemories.stream()
                        .filter(memory -> agentName.equals(memory.getAgentName()))
                        .collect(Collectors.toList());
            }

            if (type != null) {
                expiredMemories = expiredMemories.stream()
                        .filter(memory -> type == memory.getMemoryType())
                        .collect(Collectors.toList());
            }

            // 删除过期的记忆
            agentMemoryRepository.deleteAll(expiredMemories);

            logger.info("清理过期记忆完成: agent={}, type={}, cleaned={}",
                    agentName, type, expiredMemories.size());

            return expiredMemories.size();

        } catch (Exception e) {
            logger.error("清理过期记忆失败: agent={}, type={}", agentName, type, e);
            return 0;
        }
    }

    @Override
    public int cleanupByImportance(String agentName, MemoryType type, double importanceThreshold) {
        try {
            if (type == null) {
                logger.warn("清理按重要性需要指定记忆类型");
                return 0;
            }

            int deletedCount = agentMemoryRepository.deleteByImportanceThreshold(
                    agentName, type, importanceThreshold);

            logger.info("按重要性清理记忆完成: agent={}, type={}, threshold={}, cleaned={}",
                    agentName, type, importanceThreshold, deletedCount);

            return deletedCount;

        } catch (Exception e) {
            logger.error("按重要性清理记忆失败: agent={}, type={}, threshold={}",
                    agentName, type, importanceThreshold, e);
            return 0;
        }
    }

    @Override
    public MemoryStats getMemoryStats(String agentName) {
        try {
            // 先获取基本统计信息
            long totalCount = agentMemoryRepository.countByAgentName(agentName);

            // 按类型统计
            List<Object[]> typeCounts = agentMemoryRepository.countByAgentNameAndMemoryType(agentName);
            Map<MemoryType, Integer> countByType = new HashMap<>();

            for (Object[] result : typeCounts) {
                MemoryType memoryType = (MemoryType) result[0];
                Long count = (Long) result[1];
                countByType.put(memoryType, count.intValue());
            }

            // 获取时间范围
            List<AgentMemory> allMemories = agentMemoryRepository.findByAgentName(agentName);
            long oldestMemoryAgeMs = 0;
            long newestMemoryAgeMs = 0;
            double totalImportance = 0.0;
            int importanceCount = 0;

            if (!allMemories.isEmpty()) {
                LocalDateTime oldest = allMemories.stream()
                        .map(AgentMemory::getCreatedAt)
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());

                LocalDateTime newest = allMemories.stream()
                        .map(AgentMemory::getCreatedAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());

                oldestMemoryAgeMs = java.time.Duration.between(oldest, LocalDateTime.now()).toMillis();
                newestMemoryAgeMs = java.time.Duration.between(newest, LocalDateTime.now()).toMillis();

                // 计算平均重要性
                for (AgentMemory memory : allMemories) {
                    if (memory.getImportanceScore() != null) {
                        totalImportance += memory.getImportanceScore();
                        importanceCount++;
                    }
                }
            }

            double averageImportance = importanceCount > 0 ? totalImportance / importanceCount : 0.0;

            return new MemoryStats(
                    agentName,
                    countByType,
                    (int) totalCount,
                    oldestMemoryAgeMs,
                    newestMemoryAgeMs,
                    averageImportance
            );

        } catch (Exception e) {
            logger.error("获取记忆统计信息失败: agent={}", agentName, e);
            // 返回空统计信息
            return new MemoryStats(
                    agentName,
                    Map.of(),
                    0,
                    0,
                    0,
                    0.0
            );
        }
    }

    @Override
    public boolean isAvailable() {
        try {
            // 简单的可用性检查：尝试执行一个简单的查询
            agentMemoryRepository.count();
            return true;
        } catch (Exception e) {
            logger.warn("记忆管理器不可用: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<MemoryType> getSupportedMemoryTypes() {
        return Arrays.asList(
                MemoryType.SHORT_TERM,
                MemoryType.MID_TERM,
                MemoryType.LONG_TERM
        );
    }

    // 设置过期策略
    private void setExpirationPolicy(AgentMemory memory, MemoryType type) {
        LocalDateTime now = LocalDateTime.now();

        switch (type) {
            case SHORT_TERM:
                // 短期记忆：1小时后过期
                memory.setExpiresAt(now.plusHours(1));
                break;
            case MID_TERM:
                // 中期记忆：30天后过期
                memory.setExpiresAt(now.plusDays(30));
                break;
            case LONG_TERM:
                // 长期记忆：永不过期
                memory.setExpiresAt(null);
                break;
            default:
                memory.setExpiresAt(now.plusDays(7)); // 默认7天
        }
    }

    // 更新访问信息
    private void updateAccessInfo(List<AgentMemory> memories) {
        LocalDateTime now = LocalDateTime.now();

        for (AgentMemory memory : memories) {
            try {
                memory.recordAccess();
                agentMemoryRepository.updateAccessInfo(memory.getId(), now);
            } catch (Exception e) {
                logger.warn("更新记忆访问信息失败: id={}", memory.getId(), e);
            }
        }
    }

    /**
     * 归档旧记忆
     * @param agentName Agent名称
     * @param daysThreshold 天数阈值，早于此天数的记忆将被归档
     * @return 归档的记忆数量
     */
    public int archiveOldMemories(String agentName, int daysThreshold) {
        try {
            LocalDateTime dateThreshold = LocalDateTime.now().minusDays(daysThreshold);
            return agentMemoryRepository.archiveOldMemories(agentName, dateThreshold);
        } catch (Exception e) {
            logger.error("归档旧记忆失败: agent={}, days={}", agentName, daysThreshold, e);
            return 0;
        }
    }

    /**
     * 根据标签检索记忆
     * @param agentName Agent名称
     * @param tagKey 标签键
     * @param tagValue 标签值
     * @return 匹配的记忆列表
     */
    public List<AgentMemory> retrieveMemoriesByTag(String agentName, String tagKey, String tagValue) {
        try {
            // 构建JSON标签查询
            String tagJson = String.format("{\"%s\": \"%s\"}", tagKey, tagValue);
            return agentMemoryRepository.searchMemoriesByTag(agentName, tagJson);
        } catch (Exception e) {
            logger.error("按标签检索记忆失败: agent={}, tagKey={}, tagValue={}",
                    agentName, tagKey, tagValue, e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取重要性高的记忆
     * @param agentName Agent名称
     * @param importanceThreshold 重要性阈值
     * @param limit 数量限制
     * @return 重要性高的记忆列表
     */
    public List<AgentMemory> retrieveImportantMemories(String agentName, double importanceThreshold, int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            return agentMemoryRepository.findImportantMemories(agentName, importanceThreshold, pageable);
        } catch (Exception e) {
            logger.error("获取重要性记忆失败: agent={}, threshold={}", agentName, importanceThreshold, e);
            return Collections.emptyList();
        }
    }
}