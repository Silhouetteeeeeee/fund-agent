package com.shxc.fundagent.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理Controller层抛出的异常，返回标准化的错误响应
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理参数验证失败异常（@Validated注解触发）
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        log.warn("参数验证失败: {}", ex.getMessage());

        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "参数验证失败"
                ));

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "参数验证失败",
                "请求参数不符合要求",
                errors
        );
    }

    /**
     * 处理参数类型不匹配异常
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatchExceptions(
            MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型不匹配: {}", ex.getMessage());

        String errorMessage = String.format("参数 '%s' 类型不匹配，期望类型: %s, 实际值: %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "未知",
                ex.getValue());

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "参数类型不匹配",
                errorMessage,
                Map.of("parameter", ex.getName(), "value", String.valueOf(ex.getValue()))
        );
    }

    /**
     * 处理约束违反异常（@Valid注解触发）
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolationExceptions(
            ConstraintViolationException ex) {
        log.warn("约束违反异常: {}", ex.getMessage());

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "约束违反异常",
                "请求参数不符合约束条件",
                errors
        );
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex) {
        log.warn("资源未找到: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "资源未找到",
                ex.getMessage(),
                Map.of("resource", ex.getResourceName(), "identifier", ex.getIdentifier())
        );
    }

    /**
     * 处理业务逻辑异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(BusinessException ex) {
        log.warn("业务逻辑异常: {}", ex.getMessage());

        return buildErrorResponse(
                ex.getStatus() != null ? ex.getStatus() : HttpStatus.BAD_REQUEST,
                "业务逻辑异常",
                ex.getMessage(),
                ex.getDetails()
        );
    }

    /**
     * 处理服务不可用异常
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailableException(
            ServiceUnavailableException ex) {
        log.error("服务不可用异常: {}", ex.getMessage());

        return buildErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "服务不可用",
                ex.getMessage(),
                Map.of("service", ex.getServiceName(), "reason", ex.getReason())
        );
    }

    /**
     * 处理所有其他未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAllExceptions(Exception ex) {
        log.error("未捕获的异常: ", ex);

        // 生产环境中不应该暴露详细的错误信息
        String message = "系统内部错误，请稍后重试";
        Map<String, Object> details = new HashMap<>();
        details.put("exception", ex.getClass().getSimpleName());

        // 开发环境下可以返回更多信息
        if (isDevelopmentEnvironment()) {
            message = ex.getMessage();
            details.put("stackTrace", ex.getStackTrace());
        }

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "系统内部错误",
                message,
                details
        );
    }

    // ================ 构建标准错误响应 ================

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String error,
            String message,
            Map<String, ?> details) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        response.put("path", getCurrentRequestPath());

        if (details != null && !details.isEmpty()) {
            response.put("details", details);
        }

        // 添加错误ID用于日志追踪
        String errorId = generateErrorId();
        response.put("errorId", errorId);
        log.error("Error ID: {} - {}: {}", errorId, error, message);

        return new ResponseEntity<>(response, status);
    }

    private String getCurrentRequestPath() {
        // 这里可以通过RequestContextHolder获取当前请求路径
        // 简化实现：返回空字符串
        return "";
    }

    private String generateErrorId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean isDevelopmentEnvironment() {
        // 简化实现：总是返回false（生产环境）
        return false;
    }

    // ================ 自定义异常类 ================

    /**
     * 资源未找到异常
     */
    public static class ResourceNotFoundException extends RuntimeException {
        private final String resourceName;
        private final String identifier;

        public ResourceNotFoundException(String resourceName, String identifier) {
            super(String.format("%s not found with identifier: %s", resourceName, identifier));
            this.resourceName = resourceName;
            this.identifier = identifier;
        }

        public ResourceNotFoundException(String message, String resourceName, String identifier) {
            super(message);
            this.resourceName = resourceName;
            this.identifier = identifier;
        }

        public String getResourceName() {
            return resourceName;
        }

        public String getIdentifier() {
            return identifier;
        }
    }

    /**
     * 业务逻辑异常
     */
    public static class BusinessException extends RuntimeException {
        private final HttpStatus status;
        private final Map<String, Object> details;

        public BusinessException(String message) {
            super(message);
            this.status = HttpStatus.BAD_REQUEST;
            this.details = new HashMap<>();
        }

        public BusinessException(String message, HttpStatus status) {
            super(message);
            this.status = status;
            this.details = new HashMap<>();
        }

        public BusinessException(String message, Map<String, Object> details) {
            super(message);
            this.status = HttpStatus.BAD_REQUEST;
            this.details = details != null ? details : new HashMap<>();
        }

        public BusinessException(String message, HttpStatus status, Map<String, Object> details) {
            super(message);
            this.status = status;
            this.details = details != null ? details : new HashMap<>();
        }

        public HttpStatus getStatus() {
            return status;
        }

        public Map<String, Object> getDetails() {
            return details;
        }
    }

    /**
     * 服务不可用异常
     */
    public static class ServiceUnavailableException extends RuntimeException {
        private final String serviceName;
        private final String reason;

        public ServiceUnavailableException(String serviceName, String reason) {
            super(String.format("Service %s is unavailable: %s", serviceName, reason));
            this.serviceName = serviceName;
            this.reason = reason;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getReason() {
            return reason;
        }
    }
}