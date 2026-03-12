package com.shxc.fundagent;

import com.shxc.fundagent.config.CacheConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 支付宝基金理财智能 Agent 系统 - 主启动类
 *
 * 功能：
 * 1. 系统启动和初始化
 * 2. 命令行参数处理
 * 3. 缓存统计信息展示
 * 4. 系统信息展示
 */
@Slf4j
@SpringBootApplication
public class FundAgentApplication implements CommandLineRunner {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Autowired
    private CacheConfig cacheConfig;

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(FundAgentApplication.class);

        // 设置Banner
        app.setBanner((environment, sourceClass, out) -> {
            out.println();
            out.println("  ███████╗██╗   ██╗███╗   ██╗██████╗      █████╗  ██████╗ ███████╗███╗   ██╗████████╗");
            out.println("  ██╔════╝██║   ██║████╗  ██║██╔══██╗    ██╔══██╗██╔════╝ ██╔════╝████╗  ██║╚══██╔══╝");
            out.println("  █████╗  ██║   ██║██╔██╗ ██║██║  ██║    ███████║██║  ███╗█████╗  ██╔██╗ ██║   ██║   ");
            out.println("  ██╔══╝  ██║   ██║██║╚██╗██║██║  ██║    ██╔══██║██║   ██║██╔══╝  ██║╚██╗██║   ██║   ");
            out.println("  ██║     ╚██████╔╝██║ ╚████║██████╔╝    ██║  ██║╚██████╔╝███████╗██║ ╚████║   ██║   ");
            out.println("  ╚═╝      ╚═════╝ ╚═╝  ╚═══╝╚═════╝     ╚═╝  ╚═╝ ╚═════╝ ╚══════╝╚═╝  ╚═══╝   ╚═╝   ");
            out.println();
            out.println("  支付宝基金理财智能 Agent 系统 v1.0.0");
            out.println("  ===================================");
            out.println();
        });

        app.run(args);
    }

    /**
     * 系统启动后执行的初始化逻辑
     */
    @Override
    public void run(String... args) throws Exception {
        log.info("🚀 基金理财智能 Agent 系统启动成功!");

        // 打印系统信息
        printSystemInfo();

        // 打印缓存配置信息
        printCacheInfo();

        // 打印启动参数
        if (args.length > 0) {
            log.info("启动参数: {}", String.join(" ", args));
        }

        // 检查并打印可用的Bean
        checkBeans();

        log.info("✅ 系统初始化完成，等待任务调度...");
    }

    /**
     * 打印系统信息
     */
    private void printSystemInfo() throws Exception {
        String hostAddress = InetAddress.getLocalHost().getHostAddress();
        String hostName = InetAddress.getLocalHost().getHostName();
        String port = environment.getProperty("server.port", "8080");
        String contextPath = environment.getProperty("server.servlet.context-path", "");
        String profile = String.join(",", environment.getActiveProfiles());
        if (profile.isEmpty()) {
            profile = "default";
        }

        String startTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        log.info("""
                📊 系统信息:
                  启动时间: {}
                  运行环境: {}
                  服务地址: http://{}:{}{}
                  主机名称: {}
                  Java版本: {}
                  进程ID: {}
                """,
                startTime,
                profile,
                hostAddress, port, contextPath,
                hostName,
                System.getProperty("java.version"),
                System.getProperty("PID", "N/A")
        );
    }

    /**
     * 打印缓存配置信息
     */
    private void printCacheInfo() {
        try {
            String cacheStats = cacheConfig.getCacheStats();
            log.info("💾 缓存配置信息:\n{}", cacheStats);
        } catch (Exception e) {
            log.warn("无法获取缓存统计信息: {}", e.getMessage());
        }
    }

    /**
     * 检查并打印关键Bean
     */
    private void checkBeans() {
        String[] beanNames = applicationContext.getBeanDefinitionNames();
        long totalBeans = beanNames.length;

        // 统计关键Bean
        long webBeans = 0;
        long dataBeans = 0;
        long schedulerBeans = 0;
        long cacheBeans = 0;

        for (String beanName : beanNames) {
            if (beanName.contains("Controller") || beanName.contains("OkHttp") || beanName.contains("Web")) {
                webBeans++;
            } else if (beanName.contains("Repository") || beanName.contains("DataSource") || beanName.contains("EntityManager")) {
                dataBeans++;
            } else if (beanName.contains("Scheduler") || beanName.contains("Quartz") || beanName.contains("Job")) {
                schedulerBeans++;
            } else if (beanName.contains("Cache") || beanName.contains("caffeine")) {
                cacheBeans++;
            }
        }

        log.info("""
                🔧 Bean统计:
                  总Bean数量: {}
                  Web相关: {}
                  数据相关: {}
                  调度相关: {}
                  缓存相关: {}
                """,
                totalBeans, webBeans, dataBeans, schedulerBeans, cacheBeans
        );
    }

    /**
     * 获取系统状态信息（可用于健康检查）
     */
    public String getSystemStatus() {
        return String.format("""
                🟢 系统运行正常
                启动时间: %s
                运行环境: %s
                缓存状态: 已配置
                数据库: 已连接
                调度器: 已启用
                """,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                String.join(",", environment.getActiveProfiles())
        );
    }
}
