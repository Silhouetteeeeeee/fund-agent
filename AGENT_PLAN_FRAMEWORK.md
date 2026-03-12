# AI Agent集成框架 - 精简实现

基于原始AGENT_PLAN的精简框架，专注于LLM通用工具类和多Agent支持。

## 核心设计原则

1. **解耦设计**：LLM调用与具体提供商解耦，便于适配多种大模型
2. **弹性架构**：支持熔断、重试、降级等弹性机制
3. **多Agent协作**：支持多个Agent协同工作，自动任务分配
4. **配置驱动**：通过配置文件管理不同LLM提供商和Agent参数
5. **成本可控**：内置成本估算和监控机制

## 框架结构

```
src/main/java/com/shxc/fundagent/
├── llm/                          # LLM通用工具类
│   ├── LlmProvider.java          # LLM提供商接口
│   ├── ProviderType.java         # 提供商类型枚举
│   ├── AbstractLlmProvider.java  # 抽象LLM提供商基类
│   ├── LlmProviderFactory.java   # LLM提供商工厂
│   ├── config/                   # 配置类
│   │   ├── LlmProperties.java    # 配置属性
│   │   └── LlmConfig.java        # 配置类
│   ├── model/                    # 数据模型
│   │   ├── LlmRequest.java       # LLM请求
│   │   ├── LlmResponse.java      # LLM响应
│   │   ├── Message.java          # 消息模型
│   │   └── ToolCall.java         # 工具调用模型
│   └── mock/                     # Mock实现（测试用）
│       └── MockLlmProvider.java  # Mock LLM提供商
│
├── agent/                        # Agent框架
│   ├── Agent.java                # Agent接口
│   ├── AgentStatus.java          # Agent状态枚举
│   ├── AbstractAgent.java        # 抽象Agent基类
│   ├── AgentManager.java         # Agent管理器
│   ├── model/                    # Agent数据模型
│   │   └── AgentResult.java      # Agent处理结果
│   └── impl/                     # Agent实现示例
│       └── FundAnalysisAgent.java # 基金分析Agent
│
└── service/demo/                 # 示例服务
    └── LlmDemoService.java       # 演示服务类
```

## 核心功能

### 1. 多LLM提供商支持

框架支持多种LLM提供商，包括：
- OpenAI (GPT系列)
- Anthropic (Claude系列)
- 国内大模型（通义千问、文心一言、混元等）
- 本地开源模型
- 自定义提供商

**示例：注册和使用LLM提供商**

```java
// 注册Mock提供商
MockLlmProvider mockProvider = new MockLlmProvider("mock", "mock-model");
llmProviderFactory.registerProvider(mockProvider);

// 获取默认提供商
LlmProvider provider = llmProviderFactory.getDefaultProvider();

// 调用LLM
LlmRequest request = LlmRequest.builder()
        .messages(List.of(Message.user("你好")))
        .build();
LlmResponse response = provider.call(request);
```

### 2. 多Agent协作框架

支持注册多个Agent，根据任务自动选择最合适的Agent：

```java
// 注册Agent
FundAnalysisAgent fundAgent = new FundAnalysisAgent();
agentManager.registerAgent(fundAgent);

// 自动选择Agent处理任务
Map<String, Object> context = Map.of(
    "fundCode", "000001",
    "fundName", "华夏成长混合"
);
AgentResult result = agentManager.processTask("分析基金", context);

// 或指定Agent处理
AgentResult result2 = agentManager.processTaskWithAgent("fund-analysis-agent", "分析基金", context);
```

### 3. 弹性机制

- **熔断器**：连续失败后自动熔断，避免雪崩效应
- **重试机制**：可配置的重试策略
- **超时控制**：防止长时间阻塞
- **降级策略**：LLM服务不可用时回退到规则引擎

### 4. 成本控制

- Token数量估算
- 成本计算和监控
- 月度预算控制
- 成本告警

## 配置示例

`application.yml` 配置：

```yaml
ai:
  llm:
    enabled: true
    default-provider: mock

    providers:
      mock:
        enabled: true
        model: mock-model
        parameters:
          simulatedDelayMs: 300
          successRate: 0.95
          smartReplies: true

      openai:
        enabled: false  # 可后续启用
        api-key: ${OPENAI_API_KEY}
        model: gpt-3.5-turbo
        temperature: 0.3
        max-tokens: 1000
        timeout-ms: 30000

    cost-control:
      enabled: true
      monthly-budget: 100.00
      max-cost-per-analysis: 0.10
      daily-request-limit: 100
      enable-cost-alerts: true

    resilience:
      enabled: true
      retry-attempts: 2
      timeout-ms: 5000
      circuit-breaker-enabled: true
      fallback-to-rules: true
```

## 使用步骤

### 1. 添加依赖

在 `pom.xml` 中添加所需依赖（根据实际使用的LLM SDK）：

```xml
<!-- 根据需要使用相应的LLM SDK -->
<!-- <dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>0.31.0</version>
</dependency> -->
```

### 2. 配置LLM提供商

创建具体的LLM提供商实现，继承 `AbstractLlmProvider`：

```java
@Component
public class OpenAiProvider extends AbstractLlmProvider {
    public OpenAiProvider() {
        super("openai", "gpt-3.5-turbo", ProviderType.OPENAI);
    }

    @Override
    protected LlmResponse doCall(LlmRequest request) throws Exception {
        // 实现OpenAI API调用
        // 使用OpenAI SDK调用API
        // 转换响应为LlmResponse格式
    }
}
```

### 3. 创建自定义Agent

继承 `AbstractAgent` 实现业务逻辑：

```java
@Component
public class CustomAgent extends AbstractAgent {
    public CustomAgent() {
        super("custom-agent", "自定义Agent描述",
              new String[]{"capability1", "capability2"},
              new String[]{"contextType1", "contextType2"});
    }

    @Override
    protected AgentResult doProcess(String task, Map<String, Object> context) {
        // 实现业务逻辑
        // 可以调用LLM、访问数据库、调用外部API等
        return buildSuccessResult(result, 0.9, "处理完成");
    }
}
```

### 4. 集成到现有系统

在决策引擎中集成AI增强：

```java
@Service
public class EnhancedDecisionEngine {
    @Autowired
    private AgentManager agentManager;

    public DecisionResult makeDecision(FundData data) {
        // 原有规则决策
        DecisionResult ruleResult = executeRuleEngine(data);

        // AI增强分析
        Map<String, Object> context = buildAnalysisContext(data);
        AgentResult aiResult = agentManager.processTask("基金决策增强分析", context);

        // 融合决策
        return fuseDecisions(ruleResult, aiResult);
    }
}
```

## 扩展点

### 1. 添加新的LLM提供商

1. 实现 `LlmProvider` 接口或继承 `AbstractLlmProvider`
2. 在 `ProviderType` 中添加新的枚举值（可选）
3. 在 `LlmConfig` 中注册提供商

### 2. 创建新的Agent类型

1. 继承 `AbstractAgent` 类
2. 定义Agent的能力和上下文需求
3. 实现 `doProcess` 方法
4. 在Spring中注册为Bean

### 3. 自定义Agent选择策略

重写 `AgentManager.selectAgentForTask()` 方法实现自定义选择逻辑。

## 演示代码

运行演示服务查看框架功能：

```java
@Autowired
private LlmDemoService demoService;

// 运行所有演示
Map<String, Object> results = demoService.runAllDemos();
```

## 后续开发建议

### 短期（1-2周）
1. 集成真实的LLM SDK（如OpenAI、Anthropic）
2. 实现具体的业务Agent（基金分析、风险评估等）
3. 完善监控和日志

### 中期（2-4周）
1. 实现Agent间的协作和通信
2. 添加工具调用支持（Function Calling）
3. 实现记忆和上下文管理

### 长期（1-2月）
1. 集成向量数据库进行知识增强
2. 实现工作流引擎（多个Agent协同完成任务）
3. 添加强化学习能力

## 注意事项

1. **API密钥安全**：使用环境变量或密钥管理服务存储API密钥
2. **成本控制**：在生产环境启用成本监控，设置预算限制
3. **错误处理**：确保有适当的降级和错误处理机制
4. **性能监控**：监控响应时间和成功率指标
5. **合规性**：遵循相关法律法规和平台政策

---

**框架优势**：
- 高度解耦，易于扩展新的LLM提供商
- 内置弹性机制，提高系统稳定性
- 支持多Agent协作，适应复杂业务场景
- 配置驱动，便于管理和调整

**适用场景**：
- 基金理财智能决策系统
- 客户服务聊天机器人
- 文档分析和总结
- 数据分析和报告生成
- 工作流自动化和决策支持