package com.shxc.fundagent.entity;

import com.shxc.fundagent.agent.capabilities.MemoryManager;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Agent记忆实体
 * 存储Agent的中期和长期记忆
 */
@Entity
@Table(name = "agent_memory")
@Data
public class AgentMemory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Agent名称
     */
    @Column(name = "agent_name", nullable = false, length = 100)
    private String agentName;

    /**
     * 记忆类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false, length = 20)
    private MemoryManager.MemoryType memoryType;

    /**
     * 记忆内容
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * 重要性评分（0.0-1.0）
     */
    @Column(name = "importance_score")
    private Double importanceScore = 0.5;

    /**
     * 标签（用于分类和检索）
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "tags", columnDefinition = "json")
    private Map<String, String> tags = new HashMap<>();

    /**
     * 元数据
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "json")
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 过期时间（null表示永不过期）
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 最后访问时间
     */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;

    /**
     * 访问次数
     */
    @Column(name = "access_count")
    private Integer accessCount = 0;

    /**
     * 是否已归档
     */
    @Column(name = "is_archived")
    private Boolean archived = false;

    public AgentMemory() {
    }

    public AgentMemory(String agentName, MemoryManager.MemoryType memoryType, String content) {
        this.agentName = agentName;
        this.memoryType = memoryType;
        this.content = content;
    }

    public AgentMemory(String agentName, MemoryManager.MemoryType memoryType, String content,
                       double importanceScore, Map<String, String> tags, Map<String, Object> metadata) {
        this.agentName = agentName;
        this.memoryType = memoryType;
        this.content = content;
        this.importanceScore = importanceScore;
        this.tags = tags != null ? tags : new HashMap<>();
        this.metadata = metadata != null ? metadata : new HashMap<>();
    }

    /**
     * 记录访问
     */
    public void recordAccess() {
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount = (this.accessCount != null ? this.accessCount : 0) + 1;
    }

    /**
     * 添加标签
     */
    public void addTag(String key, String value) {
        if (this.tags == null) {
            this.tags = new HashMap<>();
        }
        this.tags.put(key, value);
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * 检查记忆是否已过期
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 设置过期时间（基于天数）
     */
    public void setExpiresInDays(int days) {
        if (days <= 0) {
            this.expiresAt = null;
        } else {
            this.expiresAt = LocalDateTime.now().plusDays(days);
        }
    }

    /**
     * 获取记忆年龄（分钟）
     */
    public long getAgeInMinutes() {
        if (createdAt == null) {
            return 0;
        }
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toMinutes();
    }

    /**
     * 获取记忆年龄（天）
     */
    public long getAgeInDays() {
        if (createdAt == null) {
            return 0;
        }
        return java.time.Duration.between(createdAt, LocalDateTime.now()).toDays();
    }
}