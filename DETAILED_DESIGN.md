# 支付宝基金理财智能 Agent 系统详细设计文档

版本：V1.0
日期：2026-03-08
作者：Claude Code
基于需求文档：[DESIGN.md](DESIGN.md)

---

## 1. 引言

### 1.1 项目概述
本系统是一款面向个人投资者的基金理财智能 Agent 系统，针对支付宝全品类基金持仓进行自动化监控、数据分析、策略决策与风险提醒。系统不涉及自动交易、不获取用户隐私资金权限，仅通过公开数据 + 本地配置实现智能理财辅助。

### 1.2 设计目标
1. **功能完整性**：覆盖基金数据采集、收益计算、策略决策、消息推送、报告生成全流程
2. **系统稳定性**：支持每日定时执行，具备异常处理和重试机制
3. **扩展性**：模块化设计，便于新增数据源、策略规则或推送渠道
4. **安全性**：所有数据本地存储，不连接用户支付宝账户，保障隐私安全
5. **易用性**：配置简单，输出结果直观易懂

### 1.3 术语表
- **基金代码**：支付宝基金唯一标识，如 001210
- **单位净值**：基金每份的实际价值
- **实时估值**：交易时段根据持仓股票计算的估算净值
- **持仓成本**：用户购买基金时的平均成本价
- **收益率**：(当前估值 - 持仓成本) / 持仓成本 × 100%
- **Agent**：系统中的智能代理模块，负责特定功能的自动化处理

## 2. 系统架构设计

### 2.1 整体架构图
```
┌─────────────────────────────────────────────────────────────┐
│                   用户层 (User Layer)                        │
├─────────────────────────────────────────────────────────────┤
│                   消息推送 (微信/邮件/企业微信)                 │
├─────────────────────────────────────────────────────────────┤
│                   业务应用层 (Application Layer)              │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │定时任务  │  │策略决策  │  │收益计算  │  │报告生成  │  │
│  │调度模块  │  │Agent模块 │  │Agent模块 │  │Agent模块 │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
├─────────────────────────────────────────────────────────────┤
│                   业务逻辑层 (Business Layer)                 │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐  │
│  │基金数据  │  │持仓管理  │  │策略规则  │  │消息通知  │  │
│  │服务模块  │  │服务模块  │  │引擎模块  │  │服务模块  │  │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘  │
├─────────────────────────────────────────────────────────────┤
│                   数据访问层 (Data Access Layer)              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │                MySQL 数据库持久化                      │  │
│  └──────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                   外部数据层 (External Data Layer)           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │               公开基金数据API (天天基金网等)             │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 技术栈选型
| 组件 | 技术选型 | 版本 | 说明 |
|------|----------|------|------|
| 后端框架 | SpringBoot | 3.x | 快速构建微服务 |
| 数据库 | MySQL | 8.0+ | 关系型数据存储 |
| 定时任务 | Spring Scheduler / Quartz | - | 任务调度 |
| API调用 | RestTemplate / WebClient | - | 外部数据获取 |
| 消息推送 | 微信服务号API / 企业微信机器人 | - | 实时通知 |
| 构建工具 | Maven / Gradle | - | 项目管理 |
| 单元测试 | JUnit 5 + Mockito | - | 测试框架 |

### 2.3 部署架构
- **开发环境**：本地运行，连接测试数据库
- **生产环境**：云服务器（1核2G+），独立MySQL实例
- **部署方式**：SpringBoot Jar包部署，systemd服务管理
- **监控方案**：SpringBoot Actuator健康检查，日志文件监控

## 3. 模块详细设计

### 3.1 基金数据采集模块
**职责**：从公开API获取基金实时数据、历史数据、基础信息

**核心类设计**：
```java
// 基金数据服务接口
public interface FundDataService {
    FundRealTimeData getRealTimeData(String fundCode);      // 实时估值
    FundHistoryData getHistoryData(String fundCode, int days); // 历史数据
    FundBasicInfo getFundBasicInfo(String fundCode);       // 基金基本信息
}

// 数据采集定时任务
@Component
public class FundDataCollector {
    @Scheduled(cron = "0 0/10 9-15 * * MON-FRI")  // 交易日9:00-15:00每10分钟
    public void collectRealTimeData() { /* 采集实时数据 */ }

    @Scheduled(cron = "0 30 15 * * MON-FRI")      // 交易日15:30
    public void collectDailyClosingData() { /* 采集收盘数据 */ }
}

// 数据源适配器（支持多数据源）
public interface FundDataSource {
    String getSourceName();
    FundRealTimeData fetchRealTimeData(String fundCode);
}
```

**数据流程**：
1. 定时任务触发数据采集
2. 调用数据源适配器获取原始数据
3. 数据清洗、转换、验证
4. 持久化到数据库
5. 异常处理：网络异常重试3次，数据格式异常记录日志

### 3.2 持仓收益计算模块
**职责**：计算单只基金和整体持仓的收益情况

**核心类设计**：
```java
// 收益计算服务
@Service
public class YieldCalculationService {
    // 计算单只基金收益率
    public FundYield calculateFundYield(String fundCode, BigDecimal costPrice) {
        // 获取最新估值
        // 计算收益率: (currentValue - costPrice) / costPrice
        // 计算收益金额: (currentValue - costPrice) * holdingAmount
    }

    // 计算整体持仓收益
    public PortfolioYield calculatePortfolioYield(List<FundHolding> holdings) {
        // 汇总所有持仓基金
        // 计算总成本、总市值、总收益、整体收益率
    }
}

// 收益率计算器
@Component
public class YieldCalculator {
    public BigDecimal calculateYieldRate(BigDecimal current, BigDecimal cost) {
        return current.subtract(cost)
                     .divide(cost, 4, RoundingMode.HALF_UP)
                     .multiply(BigDecimal.valueOf(100));
    }
}
```

**计算公式**：
- 单只基金收益率 = (当前估值 - 持仓成本) ÷ 持仓成本 × 100%
- 单只基金收益金额 = (当前估值 - 持仓成本) × 持仓份额
- 整体持仓收益率 = Σ(单只基金收益金额) ÷ Σ(单只基金持仓成本) × 100%

### 3.3 策略决策Agent模块（核心）
**职责**：根据预设规则生成投资操作建议

**核心类设计**：
```java
// 策略规则接口
public interface StrategyRule {
    String getRuleName();
    StrategySuggestion evaluate(FundContext context);
    int getPriority(); // 规则优先级
}

// 策略规则引擎
@Service
public class StrategyEngine {
    private List<StrategyRule> rules;

    public StrategyDecision evaluateFund(String fundCode) {
        // 1. 获取基金上下文（收益率、波动率等）
        FundContext context = buildContext(fundCode);

        // 2. 按优先级执行所有规则
        List<StrategySuggestion> suggestions = rules.stream()
            .sorted(Comparator.comparingInt(StrategyRule::getPriority))
            .map(rule -> rule.evaluate(context))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        // 3. 综合决策（取最高优先级建议）
        return aggregateDecision(suggestions);
    }
}

// 具体规则实现
@Component
public class LowPriceBuyRule implements StrategyRule {
    @Override
    public StrategySuggestion evaluate(FundContext context) {
        if (context.getYieldRate() <= -15 ||
            context.getDailyChangeRate() <= -3) {
            return StrategySuggestion.BUY;
        }
        return null;
    }
}
```

**决策流程**：
1. 收集基金上下文信息（收益率、日涨跌幅、周涨跌幅等）
2. 按优先级顺序执行规则链
3. 规则命中则生成建议，停止后续低优先级规则
4. 记录决策日志，触发消息推送

### 3.4 定时任务调度模块
**职责**：管理系统中的定时任务执行

**核心类设计**：
```java
// 任务调度配置
@Configuration
@EnableScheduling
public class TaskSchedulerConfig {
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("fund-agent-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        return scheduler;
    }
}

// 任务执行器基类
public abstract class BaseScheduledTask {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    public abstract void execute();

    protected void executeWithRetry(Runnable task, int maxRetries) {
        int retryCount = 0;
        while (retryCount <= maxRetries) {
            try {
                task.run();
                return;
            } catch (Exception e) {
                retryCount++;
                if (retryCount > maxRetries) {
                    logger.error("任务执行失败，已达最大重试次数", e);
                    throw e;
                }
                logger.warn("任务执行失败，第{}次重试", retryCount, e);
                try {
                    Thread.sleep(5000 * retryCount); // 递增延迟
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("任务被中断", ie);
                }
            }
        }
    }
}
```

**任务调度表**：
| 任务名称 | 执行时间 | 频率 | 说明 |
|----------|----------|------|------|
| 实时数据采集 | 9:00-15:00 | 每10分钟 | 交易日实时估值 |
| 收盘数据采集 | 15:30 | 每个交易日 | 收盘净值确认 |
| 收益计算 | 15:35 | 每个交易日 | 计算当日收益 |
| 策略决策 | 15:40 | 每个交易日 | 生成操作建议 |
| 日报推送 | 16:00 | 每个交易日 | 发送理财日报 |
| 周报生成 | 周五 16:30 | 每周 | 生成周度报告 |

### 3.5 消息推送模块
**职责**：通过多种渠道发送通知和报告

**核心类设计**：
```java
// 消息推送接口
public interface MessageSender {
    boolean support(MessageType type);
    void send(Message message);
}

// 消息内容构建器
@Component
public class MessageBuilder {
    public Message buildDailyReport(List<FundYield> yields,
                                    List<StrategyDecision> decisions) {
        Message message = new Message();
        message.setTitle("基金理财日报 - " + LocalDate.now());
        message.setContent(formatDailyContent(yields, decisions));
        message.setType(MessageType.DAILY_REPORT);
        return message;
    }

    public Message buildAlert(FundAlert alert) {
        Message message = new Message();
        message.setTitle("【风险预警】" + alert.getFundName());
        message.setContent(formatAlertContent(alert));
        message.setType(MessageType.RISK_ALERT);
        message.setUrgent(true);
        return message;
    }
}

// 微信推送实现
@Component
public class WechatMessageSender implements MessageSender {
    @Override
    public boolean support(MessageType type) {
        return true; // 支持所有消息类型
    }

    @Override
    public void send(Message message) {
        // 调用微信服务号API或企业微信机器人
        // 格式化消息内容为微信所需格式
    }
}
```

**消息类型**：
1. **实时提醒**：策略触发立即发送（加仓、减仓、风险预警）
2. **理财日报**：每日收盘后发送收益汇总和操作建议
3. **周度报告**：周五发送本周收益统计和资产配置分析
4. **月度报告**：每月发送月度收益总结和下月展望

### 3.6 报告生成模块
**职责**：生成各种理财报告和数据分析

**核心类设计**：
```java
// 报告生成服务
@Service
public class ReportService {
    // 生成每日收益简报
    public DailyReport generateDailyReport() {
        // 1. 查询当日所有基金数据
        // 2. 计算各项统计指标
        // 3. 格式化报告内容（Markdown/HTML）
    }

    // 生成资产配置健康度分析
    public AssetHealthReport generateAssetHealthReport() {
        // 1. 分析基金类型分布
        // 2. 评估风险等级平衡性
        // 3. 检测过度集中风险
        // 4. 提供优化建议
    }

    // 生成操作建议汇总
    public SuggestionSummary generateSuggestionSummary() {
        // 按建议类型分组统计
        // 识别高频建议基金
        // 评估建议执行情况
    }
}
```

**报告内容结构**：
- **每日收益简报**：
  - 当日总收益/总收益率
  - 收益Top3/亏损Top3基金
  - 关键操作建议汇总
  - 明日关注点

- **周度报告**：
  - 本周收益趋势图
  - 周收益率统计
  - 操作建议执行回顾
  - 下周策略展望

## 4. 数据库设计

### 4.1 数据库表结构

#### 4.1.1 基金基础表 (fund_info)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 主键 |
| fund_code | VARCHAR(10) | NOT NULL UNIQUE | 基金代码 |
| fund_name | VARCHAR(100) | NOT NULL | 基金名称 |
| fund_type | VARCHAR(20) | NOT NULL | 基金类型（混合/股票/指数/债券） |
| risk_level | TINYINT | DEFAULT 3 | 风险等级 1-5（1最低，5最高） |
| fund_company | VARCHAR(50) | | 基金公司 |
| established_date | DATE | | 成立日期 |
| manager | VARCHAR(50) | | 基金经理 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- 唯一索引：`idx_fund_code (fund_code)`
- 普通索引：`idx_fund_type (fund_type)`

#### 4.1.2 持仓表 (fund_holding)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 主键 |
| fund_code | VARCHAR(10) | NOT NULL | 基金代码 |
| cost_price | DECIMAL(10,4) | NOT NULL | 持仓成本价 |
| holding_amount | DECIMAL(16,2) | NOT NULL | 持仓份额 |
| holding_value | DECIMAL(16,2) | | 持仓市值（冗余，便于查询） |
| purchase_date | DATE | NOT NULL | 购买日期 |
| remark | VARCHAR(200) | | 备注 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- 外键约束：`FOREIGN KEY (fund_code) REFERENCES fund_info(fund_code)`
- 联合索引：`idx_fund_holding (fund_code, purchase_date)`

#### 4.1.3 每日数据表 (fund_daily_data)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 主键 |
| fund_code | VARCHAR(10) | NOT NULL | 基金代码 |
| trade_date | DATE | NOT NULL | 交易日 |
| net_value | DECIMAL(10,4) | | 单位净值 |
| estimate_value | DECIMAL(10,4) | | 实时估值 |
| change_rate | DECIMAL(6,2) | | 日涨跌幅（%） |
| turnover_rate | DECIMAL(6,2) | | 换手率（%） |
| pe_ratio | DECIMAL(10,2) | | 市盈率 |
| pb_ratio | DECIMAL(10,2) | | 市净率 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- 唯一索引：`idx_fund_date (fund_code, trade_date)` 防止重复记录
- 时间索引：`idx_trade_date (trade_date)` 按时间范围查询

#### 4.1.4 策略日志表 (fund_strategy_log)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 主键 |
| fund_code | VARCHAR(10) | NOT NULL | 基金代码 |
| trade_date | DATE | NOT NULL | 交易日 |
| yield_rate | DECIMAL(6,2) | NOT NULL | 当日收益率（%） |
| daily_change | DECIMAL(6,2) | | 日涨跌幅（%） |
| weekly_change | DECIMAL(6,2) | | 周涨跌幅（%） |
| suggestion | VARCHAR(20) | NOT NULL | 操作建议（BUY/HOLD/SELL/CLEAR） |
| suggestion_reason | VARCHAR(200) | | 建议原因 |
| triggered_rule | VARCHAR(50) | | 触发规则名称 |
| is_notified | TINYINT(1) | DEFAULT 0 | 是否已通知（0否，1是） |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

**索引**：
- 联合索引：`idx_fund_strategy (fund_code, trade_date)`
- 建议索引：`idx_suggestion_date (suggestion, trade_date)` 统计各类建议

#### 4.1.5 消息推送记录表 (message_log)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 主键 |
| message_type | VARCHAR(20) | NOT NULL | 消息类型（DAILY_REPORT/RISK_ALERT等） |
| message_title | VARCHAR(100) | NOT NULL | 消息标题 |
| message_content | TEXT | NOT NULL | 消息内容 |
| recipient | VARCHAR(100) | | 接收者标识 |
| channel | VARCHAR(20) | NOT NULL | 推送渠道（WECHAT/EMAIL等） |
| send_status | TINYINT | NOT NULL | 发送状态（0失败，1成功） |
| error_message | VARCHAR(500) | | 错误信息 |
| send_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 发送时间 |

**索引**：
- 时间索引：`idx_send_time (send_time)` 按发送时间查询
- 状态索引：`idx_status_channel (send_status, channel)` 统计发送情况

#### 4.1.6 基金交易记录表 (fund_transaction_record)
| 字段名 | 类型 | 约束 | 说明 |
|--------|------|------|------|
| id | BIGINT | PRIMARY KEY AUTO_INCREMENT | 主键 |
| fund_code | VARCHAR(10) | NOT NULL | 基金代码 |
| transaction_type | VARCHAR(20) | NOT NULL | 交易类型（BUY/SELL） |
| amount | DECIMAL(16,2) | NOT NULL | 交易份额（正数） |
| price | DECIMAL(10,4) | NOT NULL | 交易价格（单位净值） |
| total_amount | DECIMAL(16,2) | NOT NULL | 交易金额（份额 × 价格） |
| fee | DECIMAL(10,2) | DEFAULT 0 | 手续费 |
| confirmed_amount | DECIMAL(16,2) | | 实际确认份额（考虑手续费后） |
| transaction_time | DATETIME | NOT NULL | 交易时间（用户提交时间） |
| estimated_confirm_time | DATETIME | | 预计确认时间（根据交易时间计算） |
| actual_confirm_time | DATETIME | | 实际确认时间 |
| status | VARCHAR(20) | NOT NULL | 交易状态（PENDING/CONFIRMED/CANCELLED/SETTLED） |
| holding_id | BIGINT | | 关联的持仓记录ID |
| remark | VARCHAR(200) | | 备注信息 |
| create_time | DATETIME | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| update_time | DATETIME | DEFAULT CURRENT_TIMESTAMP ON UPDATE | 更新时间 |

**索引**：
- 联合索引：`idx_fund_transaction (fund_code, transaction_time)` 按基金和时间查询
- 状态索引：`idx_status_confirm (status, estimated_confirm_time)` 处理待确认交易
- 持仓关联索引：`idx_holding_id (holding_id)` 关联持仓查询
- 外键约束：`FOREIGN KEY (fund_code) REFERENCES fund_info(fund_code)`
- 外键约束：`FOREIGN KEY (holding_id) REFERENCES fund_holding(id)`

**交易确认规则**：
1. **购买交易**：交易日15:00前提交的交易，按当日净值计算，T+1日确认份额；15:00后提交的交易，按下一交易日净值计算，T+2日确认份额
2. **赎回交易**：确认规则与购买相同，资金到账时间通常为T+1到T+3个工作日
3. **非交易日**：非交易日提交的交易，按下一交易日净值计算

**持仓计算逻辑**：
- **平均持仓成本** = Σ(购买交易确认金额) / Σ(购买交易确认份额)
- **当前持仓份额** = Σ(购买交易确认份额) - Σ(赎回交易确认份额)
- **持仓更新时间**：每次交易确认后自动重新计算对应基金的持仓信息

### 4.2 数据库优化建议
1. **分区策略**：`fund_daily_data`表按`trade_date`进行范围分区，每月一个分区
2. **归档策略**：超过2年的历史数据移动到归档表，减少主表数据量
3. **读写分离**：高频查询走从库，数据写入走主库
4. **连接池**：使用HikariCP连接池，配置合理的最大连接数

## 5. API接口设计

### 5.1 基金信息管理接口
| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询基金列表 | GET | `/api/funds` | 获取所有基金信息 |
| 添加基金 | POST | `/api/funds` | 新增基金到系统 |
| 更新基金信息 | PUT | `/api/funds/{code}` | 更新基金基本信息 |
| 删除基金 | DELETE | `/api/funds/{code}` | 从系统移除基金 |
| 基金详情 | GET | `/api/funds/{code}` | 获取基金详细信息 |

**请求示例**：
```json
POST /api/funds
{
  "fundCode": "001210",
  "fundName": "天弘中证银行指数A",
  "fundType": "INDEX",
  "riskLevel": 3
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "fundCode": "001210",
    "fundName": "天弘中证银行指数A",
    "fundType": "INDEX",
    "riskLevel": 3,
    "createTime": "2026-03-08 10:30:00"
  }
}
```

### 5.2 持仓管理接口
| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 查询持仓 | GET | `/api/holdings` | 获取所有持仓记录 |
| 添加持仓 | POST | `/api/holdings` | 新增持仓记录 |
| 更新持仓 | PUT | `/api/holdings/{id}` | 更新持仓信息 |
| 删除持仓 | DELETE | `/api/holdings/{id}` | 删除持仓记录 |
| 持仓统计 | GET | `/api/holdings/stats` | 持仓统计信息 |

**请求示例**：
```json
POST /api/holdings
{
  "fundCode": "001210",
  "costPrice": 1.2345,
  "holdingAmount": 1000.00,
  "purchaseDate": "2026-01-15"
}
```

### 5.3 收益查询接口
| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 单只基金收益 | GET | `/api/yield/fund/{code}` | 查询基金历史收益 |
| 整体持仓收益 | GET | `/api/yield/portfolio` | 查询整体持仓收益 |
| 收益趋势图 | GET | `/api/yield/trend` | 获取收益趋势数据 |
| 收益对比 | GET | `/api/yield/comparison` | 多基金收益对比 |

### 5.4 策略建议接口
| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 今日建议 | GET | `/api/suggestions/today` | 获取今日操作建议 |
| 历史建议 | GET | `/api/suggestions/history` | 查询历史建议记录 |
| 模拟决策 | POST | `/api/suggestions/simulate` | 模拟策略决策 |

### 5.5 系统管理接口
| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 系统状态 | GET | `/api/system/status` | 获取系统运行状态 |
| 任务执行记录 | GET | `/api/system/tasks` | 查询定时任务执行记录 |
| 手动触发任务 | POST | `/api/system/tasks/{name}/trigger` | 手动执行任务 |
| 数据同步 | POST | `/api/system/sync` | 手动同步基金数据 |

### 5.6 统一响应格式
```json
{
  "code": 200,           // 状态码 200成功，其他失败
  "message": "success",  // 提示信息
  "data": {},            // 响应数据
  "timestamp": 1234567890 // 时间戳
}
```

## 6. 策略决策引擎详细设计

### 6.1 规则优先级设计
| 优先级 | 规则名称 | 触发条件 | 建议操作 |
|--------|----------|----------|----------|
| 1 | 极端风险提醒 | 单日跌幅 ≥ 4% 或 单周跌幅 ≥ 8% | 风险预警（特殊消息） |
| 2 | 止盈清仓线 | 收益率 ≥ 30% | 清仓 |
| 3 | 高估减仓区 | 收益率 ≥ 20% | 减仓 |
| 4 | 低估加仓区 | 收益率 ≤ -15% 或 单日跌幅 ≥ 3% | 加仓 |
| 5 | 正常持有区 | -15% < 收益率 < +15% | 持有 |

**规则执行逻辑**：
1. 规则按优先级从高到低执行
2. 高优先级规则命中后，停止执行低优先级规则
3. 同一优先级内，所有规则都会执行，取并集结果
4. 支持规则权重配置，可调整规则影响力

### 6.2 基金上下文构建
策略决策需要综合以下信息：
```java
public class FundContext {
    private String fundCode;
    private String fundName;
    private BigDecimal currentPrice;      // 当前价格
    private BigDecimal costPrice;         // 持仓成本
    private BigDecimal yieldRate;         // 收益率（%）
    private BigDecimal dailyChangeRate;   // 日涨跌幅（%）
    private BigDecimal weeklyChangeRate;  // 周涨跌幅（%）
    private BigDecimal volatility;        // 波动率
    private BigDecimal maxDrawdown;       // 最大回撤
    private String fundType;              // 基金类型
    private Integer riskLevel;            // 风险等级
    private LocalDate purchaseDate;       // 购买日期
    private Integer holdingDays;          // 持有天数
    private Map<String, Object> extraData; // 扩展数据
}
```

### 6.3 规则配置化设计
支持通过配置文件动态调整规则参数：
```yaml
strategy:
  rules:
    extremeRisk:
      enabled: true
      priority: 1
      conditions:
        dailyChange: -4.0
        weeklyChange: -8.0
      action: "RISK_ALERT"

    profitTaking:
      enabled: true
      priority: 2
      conditions:
        yieldRate: 30.0
      action: "CLEAR"

    overvalued:
      enabled: true
      priority: 3
      conditions:
        yieldRate: 20.0
      action: "SELL"

    undervalued:
      enabled: true
      priority: 4
      conditions:
        yieldRate: -15.0
        dailyChange: -3.0
      action: "BUY"

    normal:
      enabled: true
      priority: 5
      conditions:
        yieldRateMin: -15.0
        yieldRateMax: 15.0
      action: "HOLD"
```

### 6.4 决策结果处理
```java
public class StrategyDecision {
    private String fundCode;
    private String fundName;
    private String suggestion;          // 建议操作
    private String reason;              // 建议原因
    private BigDecimal yieldRate;       // 当前收益率
    private List<String> triggeredRules; // 触发的规则
    private LocalDate decisionDate;     // 决策日期
    private boolean urgent;             // 是否紧急（需要立即通知）

    // 评估决策置信度（0-1）
    public BigDecimal getConfidence() {
        // 基于收益率幅度、规则优先级、数据新鲜度计算
    }
}
```

## 7. 定时任务设计

### 7.1 任务执行流程
```
开始
├─ 任务触发（时间/事件）
├─ 任务锁获取（防止重复执行）
├─ 任务执行
│   ├─ 前置检查（依赖条件）
│   ├─ 核心业务逻辑
│   ├─ 后置处理（清理/通知）
│   └─ 异常处理（重试/记录）
├─ 任务锁释放
├─ 执行结果记录
└─ 下游任务触发（如有）
```

### 7.2 任务依赖管理
```java
// 任务依赖配置
@Configuration
public class TaskDependencyConfig {
    @Bean
    public Task dailyTaskFlow() {
        return Task.builder()
            .name("dailyTaskFlow")
            .tasks(Arrays.asList(
                TaskStep.of("collectClosingData", "15:30", "交易日收盘数据采集"),
                TaskStep.of("calculateYield", "15:35", "收益计算",
                    Collections.singletonList("collectClosingData")),
                TaskStep.of("makeStrategyDecision", "15:40", "策略决策",
                    Collections.singletonList("calculateYield")),
                TaskStep.of("sendDailyReport", "16:00", "日报推送",
                    Collections.singletonList("makeStrategyDecision"))
            ))
            .build();
    }
}
```

### 7.3 任务监控与告警
1. **执行时间监控**：记录任务开始、结束时间，计算耗时
2. **成功率统计**：统计任务执行成功率，低于阈值告警
3. **资源使用监控**：监控任务执行时的CPU、内存使用
4. **异常告警**：任务连续失败3次发送告警通知

## 8. 消息推送设计

### 8.1 消息格式设计
```json
{
  "type": "DAILY_REPORT",            // 消息类型
  "title": "基金理财日报",           // 消息标题
  "content": "## 今日收益情况...",   // 消息内容（Markdown）
  "data": {                          // 结构化数据
    "totalYield": 5.23,
    "topGainers": [...],
    "suggestions": [...]
  },
  "recipients": ["wechat_user1"],    // 接收者列表
  "priority": "NORMAL",              // 优先级
  "sendTime": "2026-03-08 16:00:00", // 计划发送时间
  "expireTime": "2026-03-09 09:00:00" // 过期时间
}
```

### 8.2 多渠道适配
```java
// 消息发送器工厂
@Component
public class MessageSenderFactory {
    private final Map<String, MessageSender> senderMap;

    public MessageSender getSender(String channel) {
        MessageSender sender = senderMap.get(channel);
        if (sender == null) {
            throw new IllegalArgumentException("不支持的渠道: " + channel);
        }
        return sender;
    }
}

// 渠道配置
message:
  channels:
    wechat:
      enabled: true
      type: "WECHAT_OFFICIAL_ACCOUNT"  # 微信服务号
      appId: "${wechat.appId}"
      appSecret: "${wechat.appSecret}"

    wecom:
      enabled: true
      type: "WECOM_ROBOT"              # 企业微信机器人
      webhook: "${wecom.webhook}"

    email:
      enabled: true
      type: "EMAIL"
      smtpHost: "${email.smtpHost}"
      smtpPort: 465
      username: "${email.username}"
      password: "${email.password}"
```

### 8.3 推送策略
1. **实时推送**：风险预警、紧急建议立即发送
2. **批量推送**：日报、周报按预定时间发送
3. **失败重试**：发送失败后重试3次，间隔递增
4. **去重机制**：相同内容30分钟内不重复发送
5. **流量控制**：限制每分钟最大发送量，避免被风控

## 9. 安全设计

### 9.1 数据安全
1. **数据本地化**：所有用户数据存储在本地数据库，不上传云端
2. **数据加密**：敏感配置信息（API密钥等）加密存储
3. **访问控制**：API接口增加简单认证机制（API Key）
4. **操作日志**：记录所有关键操作日志，便于审计

### 9.2 隐私保护
1. **无资金权限**：系统不连接支付宝账户，无法进行资金操作
2. **数据最小化**：仅收集必要的基金数据，不收集用户身份信息
3. **数据匿名化**：统计报告中使用聚合数据，不暴露具体持仓细节

### 9.3 系统安全
1. **输入验证**：所有API接口进行参数校验
2. **SQL注入防护**：使用PreparedStatement或ORM框架
3. **XSS防护**：输出内容进行HTML转义
4. **CSRF防护**：API接口增加Token验证（可选）

## 10. 性能与容错设计

### 10.1 性能优化
1. **数据库优化**：
   - 合理使用索引，避免全表扫描
   - 查询字段最小化，避免SELECT *
   - 批量操作使用批量插入/更新

2. **缓存策略**：
   ```java
   // 基金基础信息缓存（变化频率低）
   @Cacheable(value = "fundInfo", key = "#fundCode")
   public FundBasicInfo getFundBasicInfo(String fundCode) {
       return fundInfoMapper.selectByCode(fundCode);
   }

   // 实时数据缓存（短期有效）
   @Cacheable(value = "fundRealTime", key = "#fundCode",
              unless = "#result == null")
   public FundRealTimeData getRealTimeData(String fundCode) {
       // 获取数据
   }
   ```

3. **异步处理**：
   - 消息推送异步执行，不影响主流程
   - 报告生成使用异步任务
   - 日志记录使用异步写入

### 10.2 容错机制
1. **外部API容错**：
   - 设置超时时间（HTTP请求30秒）
   - 失败重试机制（最多3次）
   - 熔断降级（连续失败后暂时禁用）
   - 备用数据源（主数据源失败时使用备用源）

2. **任务执行容错**：
   ```java
   @Scheduled(cron = "0 30 15 * * MON-FRI")
   public void collectDailyData() {
       try {
           // 任务执行逻辑
       } catch (Exception e) {
           logger.error("任务执行失败", e);
           // 记录失败状态
           // 发送告警通知
           // 尝试补偿措施
       }
   }
   ```

3. **数据一致性**：
   - 关键操作使用数据库事务
   - 分布式任务使用乐观锁防重
   - 数据校验机制，防止脏数据

### 10.3 监控告警
1. **健康检查**：提供`/health`端点，检查数据库连接、外部API状态
2. **指标收集**：收集任务执行时间、成功率、API响应时间等指标
3. **日志收集**：结构化日志，便于分析和排查问题
4. **告警规则**：设置关键指标阈值，触发告警通知

## 11. 部署与配置

### 11.1 环境要求
- **操作系统**：Linux (CentOS 7+/Ubuntu 18.04+) 或 Windows Server
- **Java环境**：JDK 11+
- **数据库**：MySQL 8.0+
- **内存**：至少2GB可用内存
- **磁盘空间**：至少10GB可用空间

### 11.2 部署步骤
1. **环境准备**：
   ```bash
   # 安装JDK
   sudo apt install openjdk-11-jdk

   # 安装MySQL
   sudo apt install mysql-server

   # 创建数据库和用户
   mysql> CREATE DATABASE fund_agent DEFAULT CHARSET utf8mb4;
   mysql> CREATE USER 'fund_agent'@'localhost' IDENTIFIED BY 'password';
   mysql> GRANT ALL PRIVILEGES ON fund_agent.* TO 'fund_agent'@'localhost';
   ```

2. **应用部署**：
   ```bash
   # 下载应用JAR包
   wget https://example.com/fund-agent-1.0.0.jar

   # 创建配置文件
   mkdir -p /etc/fund-agent
   vi /etc/fund-agent/application.yml

   # 创建systemd服务
   vi /etc/systemd/system/fund-agent.service
   ```

3. **配置文件示例**：
   ```yaml
   spring:
     datasource:
       url: jdbc:mysql://localhost:3306/fund_agent
       username: fund_agent
       password: ${DB_PASSWORD}
       driver-class-name: com.mysql.cj.jdbc.Driver

     jpa:
       hibernate:
         ddl-auto: update
       show-sql: false

   fund:
     data:
       source: "tianTianFund"  # 天天基金网
       apiBaseUrl: "https://fundgz.1234567.com.cn"

     strategy:
       rules:
         extremeRisk:
           dailyChange: -4.0
           weeklyChange: -8.0

     notification:
       wechat:
         enabled: true
         appId: ${WECHAT_APP_ID}
         appSecret: ${WECHAT_APP_SECRET}

   logging:
     level:
       com.example.fundagent: DEBUG
     file:
       name: /var/log/fund-agent/application.log
   ```

4. **启动服务**：
   ```bash
   # 加载服务配置
   sudo systemctl daemon-reload

   # 启动服务
   sudo systemctl start fund-agent

   # 设置开机自启
   sudo systemctl enable fund-agent

   # 查看服务状态
   sudo systemctl status fund-agent
   ```

### 11.3 配置管理
1. **环境区分**：支持`application-dev.yml`、`application-prod.yml`
2. **敏感信息**：密码、API密钥使用环境变量注入
3. **动态配置**：支持配置热更新（需要重启生效）

## 12. 测试策略

### 12.1 单元测试
```java
@SpringBootTest
class FundDataServiceTest {
    @MockBean
    private FundDataSource dataSource;

    @Autowired
    private FundDataService fundDataService;

    @Test
    void testGetRealTimeData() {
        // 模拟数据源返回
        when(dataSource.fetchRealTimeData("001210"))
            .thenReturn(buildMockRealTimeData());

        // 调用服务
        FundRealTimeData result = fundDataService.getRealTimeData("001210");

        // 验证结果
        assertNotNull(result);
        assertEquals("001210", result.getFundCode());
    }
}
```

### 12.2 集成测试
1. **数据库集成测试**：测试数据层与数据库交互
2. **API集成测试**：测试REST接口功能
3. **外部服务集成测试**：测试与基金API、微信API的集成

### 12.3 端到端测试
1. **完整流程测试**：从数据采集到消息推送的全流程测试
2. **定时任务测试**：模拟定时任务执行场景
3. **容错测试**：模拟网络异常、服务不可用等异常场景

### 12.4 性能测试
1. **并发测试**：模拟多用户并发访问
2. **负载测试**：测试系统在正常和峰值负载下的表现
3. **压力测试**：测试系统极限承受能力

## 13. 附录

### 13.1 数据库初始化脚本
```sql
-- 创建数据库
CREATE DATABASE IF NOT EXISTS fund_agent DEFAULT CHARSET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE fund_agent;

-- 基金基础表
CREATE TABLE fund_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL UNIQUE COMMENT '基金代码',
    fund_name VARCHAR(100) NOT NULL COMMENT '基金名称',
    fund_type VARCHAR(20) NOT NULL COMMENT '基金类型',
    risk_level TINYINT DEFAULT 3 COMMENT '风险等级 1-5',
    -- ... 其他字段
    INDEX idx_fund_code (fund_code),
    INDEX idx_fund_type (fund_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='基金基础信息';

-- 持仓表
CREATE TABLE fund_holding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    fund_code VARCHAR(10) NOT NULL COMMENT '基金代码',
    cost_price DECIMAL(10,4) NOT NULL COMMENT '持仓成本',
    holding_amount DECIMAL(16,2) NOT NULL COMMENT '持仓份额',
    -- ... 其他字段
    FOREIGN KEY (fund_code) REFERENCES fund_info(fund_code),
    INDEX idx_fund_holding (fund_code, purchase_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='持仓记录';

-- 更多表创建语句...
```

### 13.2 常用基金API接口
1. **天天基金网实时数据**：
   ```
   GET https://fundgz.1234567.com.cn/js/{fundCode}.js?rt=1463558676000
   返回格式：jsonpgz({...});
   ```

2. **历史净值数据**：
   ```
   GET https://fundf10.eastmoney.com/F10DataApi.aspx?type=lsjz&code={fundCode}
   ```

3. **基金基本信息**：
   ```
   GET https://fund.eastmoney.com/pingzhongdata/{fundCode}.js
   ```

### 13.3 微信推送配置指南
1. **注册微信服务号**：前往微信公众平台注册服务号
2. **获取AppID和AppSecret**：在开发-基本配置中获取
3. **配置IP白名单**：添加服务器IP到IP白名单
4. **配置消息模板**：创建消息模板，获取模板ID
5. **用户关注**：用户关注服务号获取OpenID

---

## 修订记录
| 版本 | 日期 | 作者 | 修改说明 |
|------|------|------|----------|
| V1.0 | 2026-03-08 | Claude Code | 初始版本，基于DESIGN.md需求文档 |
| V1.1 | 2026-03-10 | Claude Code | 添加基金交易记录表设计，支持交易确认和持仓成本计算 |

---

**文档状态**：✅ 已完成
**下一步**：根据详细设计文档进行系统开发实现