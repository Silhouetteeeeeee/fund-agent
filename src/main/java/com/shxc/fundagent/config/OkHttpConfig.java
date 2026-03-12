package com.shxc.fundagent.config;

import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * OkHttp配置类
 * 用于配置HTTP客户端
 */
@Configuration
public class OkHttpConfig {

    /**
     * 创建默认的OkHttpClient Bean
     */
    @Bean
    public OkHttpClient okHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 创建用于长连接的OkHttpClient
     */
    @Bean(name = "longTimeoutOkHttpClient")
    public OkHttpClient longTimeoutOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 创建用于快速请求的OkHttpClient
     */
    @Bean(name = "fastOkHttpClient")
    public OkHttpClient fastOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
    }
}