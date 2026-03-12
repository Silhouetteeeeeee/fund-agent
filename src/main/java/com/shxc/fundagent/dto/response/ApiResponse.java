package com.shxc.fundagent.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 统一的API响应类
 * 用于包装所有API的返回结果，提供一致的响应格式
 *
 * @param <T> 数据负载的类型
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    /**
     * 状态码
     */
    private int code;

    /**
     * 状态描述
     */
    private String status;

    /**
     * 响应消息
     */
    private String message;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private LocalDateTime timestamp;

    /**
     * 成功响应（无数据）
     *
     * @return 成功的API响应
     */
    public static <T> ApiResponse<T> success() {
        return success(null, "操作成功");
    }

    /**
     * 成功响应（带数据）
     *
     * @param data 响应数据
     * @return 成功的API响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "操作成功");
    }

    /**
     * 成功响应（带数据和自定义消息）
     *
     * @param data    响应数据
     * @param message 自定义消息
     * @return 成功的API响应
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .code(200)
                .status("SUCCESS")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 客户端错误响应（400 Bad Request）
     *
     * @param message 错误消息
     * @return 客户端错误的API响应
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return badRequest(message, null);
    }

    /**
     * 客户端错误响应（400 Bad Request）带数据
     *
     * @param message 错误消息
     * @param data    错误数据
     * @return 客户端错误的API响应
     */
    public static <T> ApiResponse<T> badRequest(String message, T data) {
        return ApiResponse.<T>builder()
                .code(400)
                .status("BAD_REQUEST")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 未授权响应（401 Unauthorized）
     *
     * @param message 错误消息
     * @return 未授权的API响应
     */
    public static <T> ApiResponse<T> unauthorized(String message) {
        return unauthorized(message, null);
    }

    /**
     * 未授权响应（401 Unauthorized）带数据
     *
     * @param message 错误消息
     * @param data    错误数据
     * @return 未授权的API响应
     */
    public static <T> ApiResponse<T> unauthorized(String message, T data) {
        return ApiResponse.<T>builder()
                .code(401)
                .status("UNAUTHORIZED")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 禁止访问响应（403 Forbidden）
     *
     * @param message 错误消息
     * @return 禁止访问的API响应
     */
    public static <T> ApiResponse<T> forbidden(String message) {
        return forbidden(message, null);
    }

    /**
     * 禁止访问响应（403 Forbidden）带数据
     *
     * @param message 错误消息
     * @param data    错误数据
     * @return 禁止访问的API响应
     */
    public static <T> ApiResponse<T> forbidden(String message, T data) {
        return ApiResponse.<T>builder()
                .code(403)
                .status("FORBIDDEN")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 资源不存在响应（404 Not Found）
     *
     * @param message 错误消息
     * @return 资源不存在的API响应
     */
    public static <T> ApiResponse<T> notFound(String message) {
        return notFound(message, null);
    }

    /**
     * 资源不存在响应（404 Not Found）带数据
     *
     * @param message 错误消息
     * @param data    错误数据
     * @return 资源不存在的API响应
     */
    public static <T> ApiResponse<T> notFound(String message, T data) {
        return ApiResponse.<T>builder()
                .code(404)
                .status("NOT_FOUND")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 服务器错误响应（500 Internal Server Error）
     *
     * @param message 错误消息
     * @return 服务器错误的API响应
     */
    public static <T> ApiResponse<T> error(String message) {
        return error(message, null);
    }

    /**
     * 服务器错误响应（500 Internal Server Error）带数据
     *
     * @param message 错误消息
     * @param data    错误数据
     * @return 服务器错误的API响应
     */
    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .code(500)
                .status("INTERNAL_SERVER_ERROR")
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 自定义状态码响应
     *
     * @param code    状态码
     * @param status  状态描述
     * @param message 消息
     * @param data    数据
     * @return 自定义的API响应
     */
    public static <T> ApiResponse<T> of(int code, String status, String message, T data) {
        return ApiResponse.<T>builder()
                .code(code)
                .status(status)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 创建响应（使用Spring的HttpStatus）
     *
     * @param httpStatus HTTP状态码
     * @param message    消息
     * @param data       数据
     * @return API响应
     */
    public static <T> ApiResponse<T> of(org.springframework.http.HttpStatus httpStatus, String message, T data) {
        return ApiResponse.<T>builder()
                .code(httpStatus.value())
                .status(httpStatus.name())
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }
}