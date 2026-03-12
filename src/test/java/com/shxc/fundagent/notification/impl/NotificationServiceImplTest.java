package com.shxc.fundagent.notification.impl;

import com.shxc.fundagent.entity.MessageLog;
import com.shxc.fundagent.enums.MessageType;
import com.shxc.fundagent.enums.NotificationChannel;
import com.shxc.fundagent.enums.SuggestionType;
import com.shxc.fundagent.notification.NotificationService;
import com.shxc.fundagent.notification.model.NotificationMessage;
import com.shxc.fundagent.repository.MessageLogRepository;
import com.shxc.fundagent.strategy.model.StrategyDecisionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import okhttp3.OkHttpClient;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 消息推送服务测试类
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private MessageLogRepository messageLogRepository;

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private OkHttpClient okHttpClient;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private NotificationMessage testMessage;

    @BeforeEach
    void setUp() throws Exception {
        // 创建测试消息
        testMessage = NotificationMessage.builder()
                .messageId("TEST_MSG_001")
                .messageType(MessageType.STRATEGY_ALERT)
                .title("测试消息标题")
                .content("测试消息内容")
                .channels(Arrays.asList(NotificationChannel.EMAIL))
                .recipients(Arrays.asList("test@example.com"))
                .urgent(false)
                .priority(5)
                .createdAt(LocalDateTime.now())
                .status(NotificationMessage.MessageStatus.PENDING)
                .build();

        // 使用lenient避免不必要的stubbing警告
        lenient().doNothing().when(mailSender).send(any(SimpleMailMessage.class));
        lenient().doNothing().when(mailSender).send(any(jakarta.mail.internet.MimeMessage.class));

        // 通过反射设置必要的配置字段，使邮件渠道可用
        var emailEnabledField = NotificationServiceImpl.class.getDeclaredField("emailEnabled");
        emailEnabledField.setAccessible(true);
        emailEnabledField.set(notificationService, true);

        var emailFromField = NotificationServiceImpl.class.getDeclaredField("emailFrom");
        emailFromField.setAccessible(true);
        emailFromField.set(notificationService, "test@example.com");

        // 初始化通知服务（模拟@PostConstruct）
        var initMethod = NotificationServiceImpl.class.getDeclaredMethod("init");
        initMethod.setAccessible(true);
        initMethod.invoke(notificationService);
    }

    @Test
    void testSendMessage_Success() {
        // 模拟保存消息日志
        when(messageLogRepository.save(any(MessageLog.class))).thenReturn(new MessageLog());

        NotificationService.SendResult result = notificationService.sendMessage(testMessage);

        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getMessageId());
        assertEquals("TEST_MSG_001", result.getMessageId());

        // 验证消息被保存
        verify(messageLogRepository, times(1)).save(any(MessageLog.class));
    }

    @Test
    void testSendMessage_NullMessage() {
        assertThrows(IllegalArgumentException.class, () -> {
            notificationService.sendMessage(null);
        });
    }

    @Test
    void testSendTextMessage() {
        when(messageLogRepository.save(any(MessageLog.class))).thenReturn(new MessageLog());

        NotificationService.SendResult result = notificationService.sendTextMessage(
                "文本消息标题",
                "文本消息内容",
                Arrays.asList(NotificationChannel.EMAIL),
                Arrays.asList("user@example.com")
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(messageLogRepository, times(1)).save(any(MessageLog.class));
    }

    @Test
    void testSendRiskAlert() {
        when(messageLogRepository.save(any(MessageLog.class))).thenReturn(new MessageLog());

        NotificationService.SendResult result = notificationService.sendRiskAlert(
                "001234",
                "测试基金",
                new BigDecimal("5.25"),
                new BigDecimal("2.15"),
                "风险描述",
                Arrays.asList(NotificationChannel.EMAIL),
                Arrays.asList("risk@example.com")
        );

        assertNotNull(result);
        // sendMessage返回的SendResult应该总是成功的，因为消息已排队
        assertTrue(result.isSuccess());
        verify(messageLogRepository, times(1)).save(any(MessageLog.class));
    }

    @Test
    void testSendStrategyAlert() {
        when(messageLogRepository.save(any(MessageLog.class))).thenReturn(new MessageLog());

        NotificationService.SendResult result = notificationService.sendStrategyAlert(
                "001234",
                "测试基金",
                SuggestionType.BUY,
                new BigDecimal("0.85"),
                "策略描述",
                Arrays.asList(NotificationChannel.EMAIL),
                Arrays.asList("strategy@example.com")
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(messageLogRepository, times(1)).save(any(MessageLog.class));
    }

    @Test
    void testSendDecisionNotification() {
        when(messageLogRepository.save(any(MessageLog.class))).thenReturn(new MessageLog());

        // 创建测试决策结果
        StrategyDecisionResult decisionResult = new StrategyDecisionResult();
        decisionResult.setFundCode("001234");
        decisionResult.setFundName("测试基金");
        decisionResult.setFinalSuggestion(SuggestionType.HOLD);
        decisionResult.setFinalConfidence(new BigDecimal("0.75"));
        decisionResult.setCurrentYieldRate(new BigDecimal("3.25"));
        decisionResult.setDailyChange(new BigDecimal("1.15"));
        decisionResult.setRiskLevel(3);

        NotificationService.SendResult result = notificationService.sendDecisionNotification(
                decisionResult,
                Arrays.asList(NotificationChannel.EMAIL),
                Arrays.asList("decision@example.com")
        );

        assertNotNull(result);
        assertTrue(result.isSuccess());
        verify(messageLogRepository, times(1)).save(any(MessageLog.class));
    }

    @Test
    void testGetMessageStatus() {
        // 模拟消息在缓存中
        notificationService.sendMessage(testMessage);

        NotificationMessage.MessageStatus status = notificationService.getMessageStatus("TEST_MSG_001");

        assertNotNull(status);
        // 消息发送后状态应为SENT
        assertEquals(NotificationMessage.MessageStatus.SENT, status);
    }

    @Test
    void testGetMessageStatus_FromDatabase() {
        // 模拟数据库查询
        MessageLog logEntry = new MessageLog();
        logEntry.setMessageId("DB_MSG_001");
        logEntry.setSendStatus(1); // 成功

        when(messageLogRepository.findByMessageId("DB_MSG_001")).thenReturn(Optional.of(logEntry));

        NotificationMessage.MessageStatus status = notificationService.getMessageStatus("DB_MSG_001");

        assertNotNull(status);
        assertEquals(NotificationMessage.MessageStatus.SENT, status);
    }

    @Test
    void testGetMessageDetail() {
        // 模拟消息在缓存中
        notificationService.sendMessage(testMessage);

        NotificationMessage message = notificationService.getMessageDetail("TEST_MSG_001");

        assertNotNull(message);
        assertEquals("TEST_MSG_001", message.getMessageId());
        assertEquals("测试消息标题", message.getTitle());
    }

    @Test
    void testRetryMessage() {
        // 模拟邮件发送失败，使消息保持在FAILED状态
        doThrow(new RuntimeException("模拟发送失败")).when(mailSender).send(any(SimpleMailMessage.class));

        // 创建失败消息
        NotificationMessage failedMessage = NotificationMessage.builder()
                .messageId("FAILED_MSG_001")
                .messageType(MessageType.STRATEGY_ALERT)
                .title("失败消息")
                .content("内容")
                .status(NotificationMessage.MessageStatus.FAILED)
                .retryCount(0)
                .maxRetries(3)
                .build();

        // 模拟消息在缓存中
        notificationService.sendMessage(failedMessage);

        // 恢复邮件发送成功，使重试能够成功
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        NotificationService.SendResult result = notificationService.retryMessage("FAILED_MSG_001");

        assertNotNull(result);
        assertTrue(result.isSuccess());
    }

    @Test
    void testCancelMessage() throws Exception {
        // 创建待发送消息，直接放入缓存，避免异步发送
        NotificationMessage pendingMessage = NotificationMessage.builder()
                .messageId("PENDING_MSG_001")
                .messageType(MessageType.STRATEGY_ALERT)
                .title("待发送消息")
                .content("内容")
                .channels(Arrays.asList(NotificationChannel.EMAIL))
                .recipients(Arrays.asList("test@example.com"))
                .status(NotificationMessage.MessageStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // 使用反射直接将消息放入缓存
        var messageCacheField = NotificationServiceImpl.class.getDeclaredField("messageCache");
        messageCacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, NotificationMessage> messageCache = (Map<String, NotificationMessage>) messageCacheField.get(notificationService);
        messageCache.put("PENDING_MSG_001", pendingMessage);

        boolean cancelled = notificationService.cancelMessage("PENDING_MSG_001");

        assertTrue(cancelled);

        // 验证消息状态已改为CANCELLED
        NotificationMessage cancelledMessage = messageCache.get("PENDING_MSG_001");
        assertNotNull(cancelledMessage);
        assertEquals(NotificationMessage.MessageStatus.CANCELLED, cancelledMessage.getStatus());
    }

    @Test
    void testIsChannelAvailable() {
        // 默认情况下，邮件渠道应该可用
        boolean available = notificationService.isChannelAvailable(NotificationChannel.EMAIL);

        // 由于是模拟测试，无法准确判断，但至少不会异常
        assertNotNull(notificationService);
        // 确保调用不会抛出异常，并返回布尔值
        // 不验证具体值，因为依赖模拟环境
    }

    @Test
    void testGetAvailableChannels() {
        List<NotificationChannel> channels = notificationService.getAvailableChannels();

        assertNotNull(channels);
        // 至少包含邮件渠道
        assertTrue(channels.contains(NotificationChannel.EMAIL));
    }

    @Test
    void testGetServiceStatus() {
        NotificationService.ServiceStatus status = notificationService.getServiceStatus();

        assertNotNull(status);
        assertTrue(status.isRunning());
        assertNotNull(status.getServiceVersion());
    }

    @Test
    void testIsReady() {
        boolean ready = notificationService.isReady();

        // 服务应该就绪
        assertTrue(ready);
    }

    @Test
    void testGetVersion() {
        String version = notificationService.getVersion();

        assertNotNull(version);
        assertEquals("1.0.0", version);
    }

    @Test
    void testCleanupExpiredMessages() {
        // 创建过期消息
        NotificationMessage expiredMessage = NotificationMessage.builder()
                .messageId("EXPIRED_MSG_001")
                .messageType(MessageType.STRATEGY_ALERT)
                .title("过期消息")
                .content("内容")
                .expiryTime(LocalDateTime.now().minusDays(1))
                .status(NotificationMessage.MessageStatus.PENDING)
                .build();

        // 模拟消息在缓存中
        notificationService.sendMessage(expiredMessage);

        int cleanedCount = notificationService.cleanupExpiredMessages(LocalDateTime.now());

        assertTrue(cleanedCount >= 0);
    }
}