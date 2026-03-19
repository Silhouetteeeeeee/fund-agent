package com.shxc.fundagent.repository;

import com.shxc.fundagent.agent.capabilities.MemoryManager;
import com.shxc.fundagent.entity.AgentMemory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent记忆仓库接口
 */
@Repository
public interface AgentMemoryRepository extends JpaRepository<AgentMemory, Long> {

    /**
     * 根据Agent名称查找记忆
     */
    List<AgentMemory> findByAgentName(String agentName);

    /**
     * 根据Agent名称和记忆类型查找记忆
     */
    List<AgentMemory> findByAgentNameAndMemoryType(String agentName, MemoryManager.MemoryType memoryType);

    /**
     * 根据Agent名称、记忆类型和重要性阈值查找记忆
     */
    List<AgentMemory> findByAgentNameAndMemoryTypeAndImportanceScoreGreaterThanEqual(
            String agentName, MemoryManager.MemoryType memoryType, double importanceScore);

    /**
     * 根据Agent名称查找未过期的记忆
     */
    @Query("SELECT m FROM AgentMemory m WHERE m.agentName = :agentName " +
           "AND (m.expiresAt IS NULL OR m.expiresAt > :currentTime) " +
           "AND m.archived = false")
    List<AgentMemory> findActiveMemoriesByAgentName(
            @Param("agentName") String agentName,
            @Param("currentTime") LocalDateTime currentTime);

    /**
     * 查找已过期的记忆
     */
    @Query("SELECT m FROM AgentMemory m WHERE m.expiresAt IS NOT NULL AND m.expiresAt <= :currentTime")
    List<AgentMemory> findExpiredMemories(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 根据Agent名称查找重要性高的记忆
     */
    @Query("SELECT m FROM AgentMemory m WHERE m.agentName = :agentName " +
           "AND m.importanceScore >= :importanceThreshold " +
           "ORDER BY m.importanceScore DESC, m.createdAt DESC")
    List<AgentMemory> findImportantMemories(
            @Param("agentName") String agentName,
            @Param("importanceThreshold") double importanceScore,
            Pageable pageable);

    /**
     * 根据内容关键字搜索记忆（简化版，兼容MySQL）
     */
    @Query("SELECT m FROM AgentMemory m WHERE m.agentName = :agentName " +
           "AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY m.importanceScore DESC")
    List<AgentMemory> searchMemoriesByKeyword(
            @Param("agentName") String agentName,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 删除过期的记忆
     */
    @Modifying
    @Query("DELETE FROM AgentMemory m WHERE m.expiresAt IS NOT NULL AND m.expiresAt <= :currentTime")
    int deleteExpiredMemories(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 根据重要性阈值删除记忆
     */
    @Modifying
    @Query("DELETE FROM AgentMemory m WHERE m.agentName = :agentName " +
           "AND m.memoryType = :memoryType " +
           "AND m.importanceScore < :importanceThreshold")
    int deleteByImportanceThreshold(
            @Param("agentName") String agentName,
            @Param("memoryType") MemoryManager.MemoryType memoryType,
            @Param("importanceThreshold") double importanceThreshold);

    /**
     * 更新记忆的访问时间和访问次数
     */
    @Modifying
    @Query("UPDATE AgentMemory m SET m.lastAccessedAt = :accessTime, m.accessCount = m.accessCount + 1 " +
           "WHERE m.id = :id")
    int updateAccessInfo(@Param("id") Long id, @Param("accessTime") LocalDateTime accessTime);

    /**
     * 归档旧记忆
     */
    @Modifying
    @Query("UPDATE AgentMemory m SET m.archived = true " +
           "WHERE m.agentName = :agentName AND m.createdAt < :dateThreshold")
    int archiveOldMemories(@Param("agentName") String agentName, @Param("dateThreshold") LocalDateTime dateThreshold);

    /**
     * 统计Agent的记忆数量
     */
    @Query("SELECT COUNT(m) FROM AgentMemory m WHERE m.agentName = :agentName")
    long countByAgentName(@Param("agentName") String agentName);

    /**
     * 按类型统计记忆数量
     */
    @Query("SELECT m.memoryType, COUNT(m) FROM AgentMemory m WHERE m.agentName = :agentName GROUP BY m.memoryType")
    List<Object[]> countByAgentNameAndMemoryType(@Param("agentName") String agentName);

    @Query(value = """
        SELECT * FROM agent_memory m
        WHERE m.agent_name = :agentName 
        AND JSON_OVERLAPS(m.tags, CAST(:tagJson AS JSON))
        ORDER BY m.importance_score DESC, m.created_at DESC
        """, nativeQuery = true)
    List<AgentMemory> searchMemoriesByTag(
            @Param("agentName") String agentName,
            @Param("tagJson") String tagJson);
}
