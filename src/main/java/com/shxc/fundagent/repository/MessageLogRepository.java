package com.shxc.fundagent.repository;

import com.shxc.fundagent.entity.MessageLog;
import com.shxc.fundagent.enums.MessageType;
import com.shxc.fundagent.enums.NotificationChannel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 消息推送记录数据访问接口
 */
@Repository
public interface MessageLogRepository extends JpaRepository<MessageLog, Long> {

    /**
     * 根据消息ID查询
     */
    Optional<MessageLog> findByMessageId(String messageId);

    /**
     * 根据消息类型查询
     */
    List<MessageLog> findByMessageType(MessageType messageType);

    /**
     * 根据推送渠道查询
     */
    List<MessageLog> findByChannel(NotificationChannel channel);

    /**
     * 根据发送状态查询
     */
    List<MessageLog> findBySendStatus(Integer sendStatus);

    /**
     * 根据接收者查询
     */
    List<MessageLog> findByRecipient(String recipient);

    /**
     * 查询发送失败的记录
     */
    @Query("SELECT m FROM MessageLog m WHERE m.sendStatus = 0 AND m.retryCount < m.maxRetries")
    List<MessageLog> findFailedMessages();

    /**
     * 查询未发送的记录
     */
    @Query("SELECT m FROM MessageLog m WHERE m.sendStatus = 2 AND " +
           "(m.scheduledTime IS NULL OR m.scheduledTime <= :now)")
    List<MessageLog> findPendingMessages(@Param("now") LocalDateTime now);

    /**
     * 查询已过期的记录
     */
    @Query("SELECT m FROM MessageLog m WHERE m.expireTime IS NOT NULL AND m.expireTime < :now")
    List<MessageLog> findExpiredMessages(@Param("now") LocalDateTime now);

    /**
     * 根据相关ID和类型查询
     */
    List<MessageLog> findByRelatedIdAndRelatedType(String relatedId, String relatedType);

    /**
     * 查询紧急消息
     */
    List<MessageLog> findByIsUrgentTrue();

    /**
     * 根据创建时间范围查询
     */
    List<MessageLog> findByCreateTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据发送时间范围查询
     */
    List<MessageLog> findBySendTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 统计各消息类型的数量
     */
    @Query("SELECT m.messageType, COUNT(m) FROM MessageLog m GROUP BY m.messageType")
    List<Object[]> countByMessageType();

    /**
     * 统计各渠道的发送成功率
     */
    @Query("SELECT m.channel, " +
           "SUM(CASE WHEN m.sendStatus = 1 THEN 1 ELSE 0 END) as successCount, " +
           "COUNT(m) as totalCount " +
           "FROM MessageLog m GROUP BY m.channel")
    List<Object[]> countSuccessRateByChannel();

    /**
     * 分页查询消息记录
     */
    Page<MessageLog> findAll(Pageable pageable);

    /**
     * 根据优先级查询
     */
    List<MessageLog> findByPriority(Integer priority);

    /**
     * 查询需要重试的消息
     */
//    @Query("SELECT m FROM MessageLog m WHERE m.sendStatus = 0 AND m.retryCount < m.maxRetries " +
//           "AND (m.lastRetryTime IS NULL OR m.lastRetryTime < :retryBefore)")
//    List<MessageLog> findMessagesForRetry(@Param("retryBefore") LocalDateTime retryBefore);

    /**
     * 批量更新发送状态
     */
    @Query("UPDATE MessageLog m SET m.sendStatus = :sendStatus, m.sendTime = :sendTime, " +
           "m.errorMessage = :errorMessage WHERE m.id IN :ids")
    int updateSendStatus(@Param("ids") List<Long> ids,
                        @Param("sendStatus") Integer sendStatus,
                        @Param("sendTime") LocalDateTime sendTime,
                        @Param("errorMessage") String errorMessage);

    /**
     * 删除过期消息
     */
    @Query("DELETE FROM MessageLog m WHERE m.expireTime IS NOT NULL AND m.expireTime < :now")
    int deleteExpiredMessages(@Param("now") LocalDateTime now);
}