package com.shxc.fundagent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.TimeZone;

/**
 * Jackson配置类
 * 用于配置JSON序列化和反序列化
 */
@Configuration
public class JacksonConfig {

    /**
     * 创建ObjectMapper Bean
     */
    @Bean
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .featuresToDisable(
                        SerializationFeature.WRITE_DATES_AS_TIMESTAMPS,
                        SerializationFeature.FAIL_ON_EMPTY_BEANS
                )
                .featuresToEnable(
                        SerializationFeature.INDENT_OUTPUT,
                        SerializationFeature.WRITE_ENUMS_USING_TO_STRING
                )
                .modules(new JavaTimeModule())
                .dateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                .timeZone(TimeZone.getTimeZone(ZoneId.of("Asia/Shanghai")))
                .build();
    }

    /**
     * 创建用于API响应的ObjectMapper
     */
    @Bean(name = "apiObjectMapper")
    public ObjectMapper apiObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册Java时间模块
        mapper.registerModule(new JavaTimeModule());

        // 配置日期格式
        mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        mapper.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));

        // 禁用时间戳格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 美化输出
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // 枚举使用toString
        mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);

        // 空对象不抛异常
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return mapper;
    }

    /**
     * 创建用于日志的紧凑ObjectMapper
     */
    @Bean(name = "compactObjectMapper")
    public ObjectMapper compactObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // 注册Java时间模块
        mapper.registerModule(new JavaTimeModule());

        // 紧凑输出
        mapper.disable(SerializationFeature.INDENT_OUTPUT);

        // 禁用时间戳格式
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 空对象不抛异常
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return mapper;
    }
}