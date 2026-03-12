package com.shxc.fundagent.controller;

import com.shxc.fundagent.dto.response.ApiResponse;
import com.shxc.fundagent.enums.MessageType;
import com.shxc.fundagent.enums.NotificationChannel;
import com.shxc.fundagent.notification.NotificationService;
import com.shxc.fundagent.notification.model.NotificationMessage;
import com.shxc.fundagent.strategy.model.StrategyDecisionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 消息通知API控制器
 * 提供消息推送和通知管理相关的RESTful接口
 */
@Slf4j
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 发送单个消息
     */
    @PostMapping("/send")
    public ResponseEntity<NotificationService.SendResult> sendMessage(
            @RequestBody NotificationMessage message) {
        log.info("发送单个消息，消息类型: {}, 标题: {}", message.getMessageType(), message.getTitle());
        NotificationService.SendResult result = notificationService.sendMessage(message);
        return ResponseEntity.ok(result);
    }

    /**
     * 批量发送消息
     */
    @PostMapping("/batch-send")
    public ResponseEntity<List<NotificationService.SendResult>> sendMessages(
            @RequestBody List<NotificationMessage> messages) {
        log.info("批量发送消息，数量: {}", messages.size());
        List<NotificationService.SendResult> results = notificationService.sendMessages(messages);
        return ResponseEntity.ok(results);
    }

    /**
     * 发送文本消息
     */
    @PostMapping("/text")
    public ResponseEntity<NotificationService.SendResult> sendTextMessage(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam List<NotificationChannel> channels,
            @RequestParam List<String> recipients) {
        log.info("发送文本消息，标题: {}, 渠道数量: {}, 接收者数量: {}", title, channels.size(), recipients.size());
        NotificationService.SendResult result =
                notificationService.sendTextMessage(title, content, channels, recipients);
        return ResponseEntity.ok(result);
    }

    /**
     * 发送模板消息
     */
    @PostMapping("/template")
    public ResponseEntity<NotificationService.SendResult> sendTemplateMessage(
            @RequestParam String templateId,
            @RequestBody Map<String, Object> templateParams,
            @RequestParam List<NotificationChannel> channels,
            @RequestParam List<String> recipients) {
        log.info("发送模板消息，模板ID: {}, 渠道数量: {}, 接收者数量: {}", templateId, channels.size(), recipients.size());
        NotificationService.SendResult result =
                notificationService.sendTemplateMessage(templateId, templateParams, channels, recipients);
        return ResponseEntity.ok(result);
    }

    /**
     * 发送风险警报
     */
    @PostMapping("/risk-alert")
    public ResponseEntity<NotificationService.SendResult> sendRiskAlert(
            @RequestParam String fundCode,
            @RequestParam String fundName,
            @RequestParam BigDecimal yieldRate,
            @RequestParam BigDecimal dailyChange,
            @RequestParam String riskDescription,
            @RequestParam List<NotificationChannel> channels,
            @RequestParam List<String> recipients) {
        log.info("发送风险警报，基金代码: {}, 基金名称: {}", fundCode, fundName);
        NotificationService.SendResult result = notificationService.sendRiskAlert(
                fundCode, fundName, yieldRate, dailyChange, riskDescription, channels, recipients);
        return ResponseEntity.ok(result);
    }

    /**
     * 发送策略提醒
     */
    @PostMapping("/strategy-alert")
    public ResponseEntity<NotificationService.SendResult> sendStrategyAlert(
            @RequestParam String fundCode,
            @RequestParam String fundName,
            @RequestParam String suggestionType,
            @RequestParam BigDecimal confidence,
            @RequestParam String strategyDescription,
            @RequestParam List<NotificationChannel> channels,
            @RequestParam List<String> recipients) {
        log.info("发送策略提醒，基金代码: {}, 建议类型: {}", fundCode, suggestionType);
        // 将字符串转换为枚举，这里需要导入SuggestionType
        com.shxc.fundagent.enums.SuggestionType type =
                com.shxc.fundagent.enums.SuggestionType.valueOf(suggestionType.toUpperCase());
        NotificationService.SendResult result = notificationService.sendStrategyAlert(
                fundCode, fundName, type, confidence, strategyDescription, channels, recipients);
        return ResponseEntity.ok(result);
    }

    /**
     * 发送决策结果通知
     */
    @PostMapping("/decision-notification")
    public ResponseEntity<NotificationService.SendResult> sendDecisionNotification(
            @RequestBody StrategyDecisionResult decisionResult,
            @RequestParam List<NotificationChannel> channels,
            @RequestParam List<String> recipients) {
        log.info("发送决策结果通知，基金代码: {}", decisionResult.getFundCode());
        NotificationService.SendResult result = notificationService.sendDecisionNotification(
                decisionResult, channels, recipients);
        return ResponseEntity.ok(result);
    }

    /**
     * 发送日报
     */
    @PostMapping("/daily-report")
    public ResponseEntity<NotificationService.SendResult> sendDailyReport(
            @RequestParam String reportDate,
            @RequestBody Map<String, Object> reportData,
            @RequestParam List<NotificationChannel> channels,
            @RequestParam List<String> recipients) {
        log.info("发送日报，报告日期: {}", reportDate);
        NotificationService.SendResult result = notificationService.sendDailyReport(
                reportDate, reportData, channels, recipients);
        return ResponseEntity.ok(result);
    }

    /**
     * 发送系统告警
     */
    @PostMapping("/system-alert")
    public ResponseEntity<NotificationService.SendResult> sendSystemAlert(
            @RequestParam String systemName,
            @RequestParam String errorMessage,
            @RequestParam String severity,
            @RequestParam List<NotificationChannel> channels,
            @RequestParam List<String> recipients) {
        log.info("发送系统告警，系统名称: {}, 严重程度: {}", systemName, severity);
        NotificationService.SendResult result = notificationService.sendSystemAlert(
                systemName, errorMessage, severity, channels, recipients);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取消息状态
     */
    @GetMapping("/{messageId}/status")
    public ResponseEntity<NotificationMessage.MessageStatus> getMessageStatus(
            @PathVariable String messageId) {
        log.info("获取消息状态，消息ID: {}", messageId);
        NotificationMessage.MessageStatus status = notificationService.getMessageStatus(messageId);
        return ResponseEntity.ok(status);
    }

    /**
     * 获取消息详情
     */
    @GetMapping("/{messageId}")
    public ResponseEntity<NotificationMessage> getMessageDetail(@PathVariable String messageId) {
        log.info("获取消息详情，消息ID: {}", messageId);
        NotificationMessage message = notificationService.getMessageDetail(messageId);
        if (message == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(message);
    }

    /**
     * 重试发送失败的消息
     */
    @PostMapping("/{messageId}/retry")
    public ResponseEntity<NotificationService.SendResult> retryMessage(
            @PathVariable String messageId) {
        log.info("重试发送失败的消息，消息ID: {}", messageId);
        NotificationService.SendResult result = notificationService.retryMessage(messageId);
        return ResponseEntity.ok(result);
    }

    /**
     * 取消待发送的消息
     */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelMessage(@PathVariable String messageId) {
        log.info("取消待发送的消息，消息ID: {}", messageId);
        boolean success = notificationService.cancelMessage(messageId);

        Map<String, Object> data = Map.of(
            "success", success,
            "message", success ? "消息取消成功" : "消息取消失败",
            "messageId", messageId,
            "timestamp", System.currentTimeMillis()
        );

        if (success) {
            return ResponseEntity.ok(ApiResponse.success(data, "消息取消成功"));
        } else {
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("消息取消失败", data));
        }
    }

    /**
     * 获取待发送消息列表
     */
    @GetMapping("/pending")
    public ResponseEntity<List<NotificationMessage>> getPendingMessages(
            @RequestParam(required = false) String messageType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        log.info("获取待发送消息列表，消息类型: {}, 状态: {}, 限制数量: {}", messageType, status, limit);

        MessageType type = null;
        if (messageType != null) {
            try {
                type = MessageType.valueOf(messageType.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 保持为null
            }
        }

        NotificationMessage.MessageStatus messageStatus = null;
        if (status != null) {
            try {
                messageStatus = NotificationMessage.MessageStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 保持为null
            }
        }

        List<NotificationMessage> messages =
                notificationService.getPendingMessages(type, messageStatus, limit);
        return ResponseEntity.ok(messages);
    }

    /**
     * 清理过期消息
     */
    @DeleteMapping("/cleanup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cleanupExpiredMessages(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeTime) {
        log.info("清理过期消息，截止时间: {}", beforeTime);
        LocalDateTime cleanupTime = beforeTime != null ? beforeTime : LocalDateTime.now().minusMonths(3);
        int cleanedCount = notificationService.cleanupExpiredMessages(cleanupTime);

        Map<String, Object> data = Map.of(
            "success", true,
            "message", "过期消息清理完成",
            "beforeTime", cleanupTime,
            "cleanedCount", cleanedCount,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "过期消息清理完成"));
    }

    /**
     * 检查渠道是否可用
     */
    @GetMapping("/channels/{channel}/available")
    public ResponseEntity<ApiResponse<Map<String, Object>>> isChannelAvailable(@PathVariable String channel) {
        log.info("检查渠道是否可用，渠道: {}", channel);
        NotificationChannel channelEnum;
        try {
            channelEnum = NotificationChannel.valueOf(channel.toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorData = Map.of(
                "success", false,
                "message", "无效的渠道类型: " + channel,
                "availableChannels", NotificationChannel.values()
            );
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("无效的渠道类型", errorData));
        }

        boolean available = notificationService.isChannelAvailable(channelEnum);
        Map<String, Object> data = Map.of(
            "channel", channel,
            "available", available,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "渠道可用性检查完成"));
    }

    /**
     * 获取所有可用渠道
     */
    @GetMapping("/channels")
    public ResponseEntity<List<NotificationChannel>> getAvailableChannels() {
        log.info("获取所有可用渠道");
        List<NotificationChannel> channels = notificationService.getAvailableChannels();
        return ResponseEntity.ok(channels);
    }

    /**
     * 获取渠道统计信息
     */
    @GetMapping("/channels/{channel}/statistics")
    public ResponseEntity<NotificationService.ChannelStatistics> getChannelStatistics(
            @PathVariable String channel,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        log.info("获取渠道统计信息，渠道: {}, 开始时间: {}, 结束时间: {}", channel, startTime, endTime);
        NotificationChannel channelEnum;
        try {
            channelEnum = NotificationChannel.valueOf(channel.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }

        LocalDateTime effectiveStartTime = startTime != null ? startTime : LocalDateTime.now().minusDays(30);
        LocalDateTime effectiveEndTime = endTime != null ? endTime : LocalDateTime.now();

        NotificationService.ChannelStatistics statistics =
                notificationService.getChannelStatistics(channelEnum, effectiveStartTime, effectiveEndTime);
        return ResponseEntity.ok(statistics);
    }

    /**
     * 重新加载渠道配置
     */
    @PostMapping("/channels/{channel}/reload")
    public ResponseEntity<ApiResponse<Map<String, Object>>> reloadChannelConfig(@PathVariable String channel) {
        log.info("重新加载渠道配置，渠道: {}", channel);
        NotificationChannel channelEnum;
        try {
            channelEnum = NotificationChannel.valueOf(channel.toUpperCase());
        } catch (IllegalArgumentException e) {
            Map<String, Object> errorData = Map.of(
                "success", false,
                "message", "无效的渠道类型: " + channel
            );
            return ResponseEntity.badRequest().body(ApiResponse.badRequest("无效的渠道类型", errorData));
        }

        boolean success = notificationService.reloadChannelConfig(channelEnum);
        Map<String, Object> data = Map.of(
            "success", success,
            "message", success ? "渠道配置重新加载成功" : "渠道配置重新加载失败",
            "channel", channel,
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, success ? "渠道配置重新加载成功" : "渠道配置重新加载失败"));
    }

    /**
     * 获取服务状态
     */
    @GetMapping("/service-status")
    public ResponseEntity<NotificationService.ServiceStatus> getServiceStatus() {
        log.info("获取服务状态");
        NotificationService.ServiceStatus status = notificationService.getServiceStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * 检查服务是否就绪
     */
    @GetMapping("/ready")
    public ResponseEntity<ApiResponse<Map<String, Object>>> isReady() {
        log.info("检查服务是否就绪");
        boolean ready = notificationService.isReady();

        Map<String, Object> data = Map.of(
            "ready", ready,
            "message", ready ? "消息推送服务已就绪" : "消息推送服务未就绪",
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "服务就绪状态检查完成"));
    }

    /**
     * 获取服务版本
     */
    @GetMapping("/version")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getVersion() {
        log.info("获取服务版本");
        String version = notificationService.getVersion();

        Map<String, Object> data = Map.of(
            "version", version,
            "serviceName", "FundAgent Notification Service",
            "apiVersion", "1.0",
            "timestamp", System.currentTimeMillis()
        );

        return ResponseEntity.ok(ApiResponse.success(data, "服务版本获取成功"));
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        log.info("消息推送服务健康检查");
        try {
            notificationService.getServiceStatus();
            Map<String, Object> data = Map.of(
                "status", "UP",
                "service", "NotificationService",
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.ok(ApiResponse.success(data, "服务健康"));
        } catch (Exception e) {
            Map<String, Object> errorData = Map.of(
                "status", "DOWN",
                "service", "NotificationService",
                "error", e.getMessage(),
                "timestamp", System.currentTimeMillis()
            );
            return ResponseEntity.status(503).body(ApiResponse.of(503, "SERVICE_UNAVAILABLE", "服务异常", errorData));
        }
    }
}