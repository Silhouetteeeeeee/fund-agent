package com.shxc.fundagent.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 使用Caffeine作为本地缓存
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 基金基础信息缓存配置（24小时）
     */
    @Bean(name = "fundInfoCache")
    public Caffeine<Object, Object> fundInfoCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(24, TimeUnit.HOURS)
                .recordStats();
    }

    /**
     * 实时数据缓存配置（10分钟）
     */
    @Bean(name = "realTimeDataCache")
    public Caffeine<Object, Object> realTimeDataCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * 历史数据缓存配置（7天）
     */
    @Bean(name = "historyDataCache")
    public Caffeine<Object, Object> historyDataCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(7, TimeUnit.DAYS)
                .recordStats();
    }

    /**
     * 策略决策缓存配置（1小时）
     */
    @Bean(name = "strategyCache")
    public Caffeine<Object, Object> strategyCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .recordStats();
    }

    /**
     * 收益率计算缓存配置（30分钟）
     */
    @Bean(name = "yieldCache")
    public Caffeine<Object, Object> yieldCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(300)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .recordStats();
    }

    /**
     * 创建缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        // 设置默认缓存配置
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .recordStats());

        // 注册特定缓存
        cacheManager.registerCustomCache("fundInfo", fundInfoCacheBuilder().build());
        cacheManager.registerCustomCache("realTimeData", realTimeDataCacheBuilder().build());
        cacheManager.registerCustomCache("historyData", historyDataCacheBuilder().build());
        cacheManager.registerCustomCache("strategy", strategyCacheBuilder().build());
        cacheManager.registerCustomCache("yield", yieldCacheBuilder().build());

        return cacheManager;
    }

    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== 缓存统计信息 ===\n");

        CaffeineCacheManager cacheManager = (CaffeineCacheManager) cacheManager();
        String[] cacheNames = cacheManager.getCacheNames().toArray(new String[0]);

        for (String cacheName : cacheNames) {
            com.github.benmanes.caffeine.cache.Cache<Object, Object> cache =
                    (com.github.benmanes.caffeine.cache.Cache<Object, Object>)
                            cacheManager.getCache(cacheName).getNativeCache();

            com.github.benmanes.caffeine.cache.stats.CacheStats cacheStats = cache.stats();
            stats.append(String.format("缓存名称: %s\n", cacheName));
            stats.append(String.format("  请求次数: %d\n", cacheStats.requestCount()));
            stats.append(String.format("  命中次数: %d\n", cacheStats.hitCount()));
            stats.append(String.format("  命中率: %.2f%%\n", cacheStats.hitRate() * 100));
            stats.append(String.format("  未命中次数: %d\n", cacheStats.missCount()));
            stats.append(String.format("  加载成功次数: %d\n", cacheStats.loadSuccessCount()));
            stats.append(String.format("  加载失败次数: %d\n", cacheStats.loadFailureCount()));
            stats.append(String.format("  总加载时间: %d ns\n", cacheStats.totalLoadTime()));
            stats.append(String.format("  回收次数: %d\n", cacheStats.evictionCount()));
            stats.append("---\n");
        }

        return stats.toString();
    }
}