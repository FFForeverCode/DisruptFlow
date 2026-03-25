# RetryDisruptorTaskConfig 配置详解

## 一、文件概述

`RetryDisruptorTaskConfig.java` 是 **DisruptFlow** 项目的核心配置类，负责初始化并配置基于 **LMAX Disruptor** 的高性能任务重试队列系统。

### 核心定位
- **配置文件路径**: `src/main/java/com/chenxiaofei/disruptflow/config/RetryDisruptorTaskConfig.java`
- **主要作用**: 构建高性能、可重试的任务处理流水线
- **技术栈**: Spring Boot + LMAX Disruptor + RocketMQ
- **设计模式**: 事件驱动、生产者-消费者、工作线程池

---

## 二、核心组件详解

### 1. Disruptor 环形队列配置

```java
@Bean
public Disruptor<RetryDisruptorTaskEvent> getDisruptor()
```

#### 关键参数

| 参数 | 值 | 说明 |
|------|-----|------|
| **缓冲区大小** | `128` | RingBuffer 容量，必须是 2 的幂次方 |
| **工作线程数** | `CPU 核心数 × 2` | 动态计算，充分利用多核性能 |
| **线程命名** | `retry_disruptor_task_thread_seq:N` | 便于监控和排查问题 |

#### 创建流程

```java
Disruptor<RetryDisruptorTaskEvent> disruptor = new Disruptor<>(
    RetryDisruptorTaskEvent::new,      // Event 工厂方法
    BUFF_SIZE,                         // 缓冲区大小
    r -> new Thread(r, THREAD_FACTORY_DESC + atomicInteger.getAndIncrement()) // 线程工厂
);
```

**设计亮点**:
- 使用 **Lambda 表达式**作为 Event 工厂，避免反射开销
- 自定义线程工厂，便于**线程追踪**和**性能监控**
- 缓冲区大小 128，平衡内存占用与吞吐量

---

### 2. 工作处理器（WorkerHandler）

```java
@Bean
public RetryTaskEventWorkerHandler getRetryTaskEventWorkerHandler()
```

**核心职责**:
- 从 RingBuffer 消费事件
- 执行具体的业务任务（调用 TaskProcessor）
- 管理任务状态与重试逻辑
- 处理并发和幂等性

**工作流程**:

```
1. 接收 RetryDisruptorTaskEvent
   ↓
2. 判断是否需检查任务状态（首次执行跳过）
   ↓
3. 查询数据库获取最新任务信息
   ↓
4. 检查任务是否已完成
   ↓
5. 检查失败次数是否超过阈值（默认3次）
   ↓
6. 更新任务状态为「执行中」（乐观锁）
   ↓
7. 调用具体的 TaskProcessor 执行业务逻辑
   ↓
8. 根据执行结果更新任务状态
   ↓
   ├─ 成功 → 更新为 FINISHED
   └─ 失败 → 增加失败计数，抛出异常触发重试
```

---

### 3. 异常处理器（ExceptionHandler）

```java
@Bean
public ExceptionHandler<? super RetryDisruptorTaskEvent> getRetryTaskEventExceptionHandler()
```

**核心职责**:
- 捕获工作线程中的未处理异常
- 实现**智能重试策略**与**降级机制**
- 集成 RocketMQ 实现延迟重试队列

**异常处理流程**:

```
发生异常
  ↓
查询任务最新状态
  ↓
设置 shouldCheckUnfinished = true
  ↓
发送到 RocketMQ 延迟队列
  ↓
根据失败次数选择延迟级别（1s → 5s → 10s → 30s → 1m...）
  ↓
MQ 消费者接收后重新推入 Disruptor
```

**延迟级别策略**:

```java
// 失败次数 + 1 作为延迟级别
DelayLevel.getLevel(retryDisruptorTask.getFailCount() + 1)
```

| 失败次数 | 延迟时间 | 级别 |
|---------|---------|------|
| 1 | 1秒 | D1 |
| 2 | 5秒 | D2 |
| 3 | 10秒 | D3 |
| 4 | 30秒 | D4 |
| 5 | 1分钟 | D5 |
| ... | ... | ... |
| 18 | 2小时 | D18 |

这种**指数退避**策略有效避免系统雪崩。

---

### 4. 事件清理处理器

```java
@Bean
public EventHandler<RetryDisruptorTaskEvent> disruptorClearEventHandler()
```

**作用**: 在事件处理完成后，清理 RingBuffer 中的事件对象，**防止内存泄漏**。

```java
(event, l, b) -> {
    event = null;  // 显式置空，帮助 GC
}
```

**执行顺序**:
```
WorkerHandlers 并行处理 → 清理处理器 → RingBuffer 槽位释放
```

---

## 三、整体架构设计

### 数据处理流程图

```
┌─────────────────────────────────────────────────────────────────────┐
│                        业务系统（生产者）                             │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ 1. 创建任务
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    DisruptorEventPusher                             │
│              （推送事件到 Disruptor）                               │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ 2. publishEvent()
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                    RingBuffer（环形队列）                           │
│  ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐  │
│  │ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │...│   │   │   │   │   │127│  │
│  └───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┴───┘  │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ 3. 事件消费
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│              WorkerPool（多工作线程并行处理）                        │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                 │
│  │ Worker-1    │  │ Worker-2    │  │ Worker-3    │                 │
│  │ ↳ 执行任务  │  │ ↳ 执行任务  │  │ ↳ 执行任务  │ ...              │
│  └─────────────┘  └─────────────┘  └─────────────┘                 │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               │ 4. 成功 → 更新状态 FINISHED
                               │ 5. 失败 → 抛出异常
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│              ExceptionHandler（异常捕获与重试）                      │
│  • 记录失败次数                                              │
│  • 发送到 RocketMQ 延迟队列                                │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ 6. 延迟消息
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                       RocketMQ 延迟队列                             │
│      1s   5s   10s   30s   1m   2m   3m   ...   2h               │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ 7. 时间到期
                               ↓
┌─────────────────────────────────────────────────────────────────────┐
│                DisruptorTaskMQListener                              │
│            （MQ 消费者重新推送事件）                                │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ 8. 重新推入 Disruptor
                               ↓
                    [回到步骤 3，形成重试闭环]
```

---

## 四、配置参数说明

### 1. 静态常量

```java
// 线程名称前缀
private static final String THREAD_FACTORY_DESC = "retry_disruptor_task_thread_seq:";

// RingBuffer 大小（必须是 2 的幂）
private static final int BUFF_SIZE = 128;

// 工作线程数
private static final int WORKER_SIZE = Runtime.getRuntime().availableProcessors() * 2;
```

### 2. 可配置项（application.yml）

```yaml
retry:
  failed:
    count:
      limit: 3    # 最大重试次数，默认 3 次
```

在 `RetryTaskEventWorkerHandler` 中引用：
```java
@Value("${retry.failed.count.limit}")
private Integer failedCountLimit = 3;
```

---

## 五、关键技术特性

### 1. 高性能设计

- **无锁并发**: Disruptor 使用 CAS 操作，避免锁竞争
- **缓存行填充**: 解决伪共享问题，提升性能
- **预分配内存**: 事件对象提前创建，减少 GC 压力
- **批处理**: 支持批量消费，提高吞吐量

### 2. 可靠性保障

- **持久化存储**: 任务状态保存在数据库
- **幂等性控制**: 通过版本号实现乐观锁
- **异常重试**: 多级延迟重试策略
- **人工干预**: 超过最大重试次数发送通知

### 3. 可扩展性

- **动态任务类型**: 通过 `RetryDisruptorTaskEnum` 扩展新任务
- **策略模式**: 不同任务实现 `TaskProcessor` 接口
- **线程池自适应**: 根据 CPU 核心数自动调整

### 4. 可观测性

- **详细日志**: 记录任务全生命周期
- **线程命名**: 便于监控工具识别
- **异常追踪**: 完整的错误堆栈和上下文信息
- **RocketMQ 监控**: 消息发送成功/失败回调

---

## 六、任务状态流转

```
CREATED（已创建）
    ↓
DOING（执行中） ← 乐观锁更新
    ↓
    ├─ 成功 → FINISHED（已完成）
    └─ 失败 →
          ↓
       failCount + 1
          ↓
       判断是否超过阈值
          ├─ 未超过 → 重新入队重试
          └─ 超过 → 发送 ERP 通知（人工干预）
```

---

## 七、使用示例

### 1. 创建新任务

```java
@Autowired
private RetryDisruptorEventPusher eventPusher;

public void createTask() {
    RetryDisruptorTask task = new RetryDisruptorTask();
    task.setBn("ORDER_20260001");
    task.setLifeCycle("ORDER_CANCEL");
    task.setHandleProcessor("CANCEL_EXPRESS");  // 使用枚举值
    task.setState(TaskStateEnum.CREATED.getValue());
    task.setFailCount((byte) 0);
    task.setTaskParams("{\"orderId\": 1001}");
    
    // 保存到数据库
    retryDisruptorTaskMapper.insert(task);
    
    // 推送至 Disruptor
    RetryDisruptorTaskEvent event = new RetryDisruptorTaskEvent();
    event.setRetryDisruptorTask(task);
    event.setShouldCheckUnfinished(false);  // 首次执行无需检查
    
    eventPusher.pushEvent2Disruptor(event);
}
```

### 2. 实现自定义任务处理器

```java
@Component("orderCancelExpress")
public class OrderCancelExpressProcessor implements TaskProcessor {
    
    @Override
    public boolean execute(RetryDisruptorTask task) {
        try {
            // 解析参数
            JSONObject params = JSON.parseObject(task.getTaskParams());
            Long orderId = params.getLong("orderId");
            
            // 执行业务逻辑
            return expressService.cancelOrder(orderId);
        } catch (Exception e) {
            log.error("取消运单失败, taskId={}", task.getId(), e);
            return false;  // 返回 false 触发重试
        }
    }
}
```

### 3. 添加新任务类型

在 `RetryDisruptorTaskEnum.java` 中添加：

```java
NEW_TASK_TYPE(12, "新任务类型", "newTaskProcessor")
```

然后实现对应的 Processor Bean：

```java
@Component("newTaskProcessor")
public class NewTaskProcessor implements TaskProcessor {
    @Override
    public boolean execute(RetryDisruptorTask task) {
        // 业务逻辑
        return true;
    }
}
```

---

## 八、性能调优建议

### 1. RingBuffer 大小调整

```java
// 根据业务场景调整
private static final int BUFF_SIZE = 1024;  // 高并发场景
// 或
private static final int BUFF_SIZE = 64;    // 低延迟场景
```

### 2. 工作线程数调整

```java
// IO 密集型任务
private static final int WORKER_SIZE = Runtime.getRuntime().availableProcessors() * 4;

// CPU 密集型任务
private static final int WORKER_SIZE = Runtime.getRuntime().availableProcessors() + 1;
```

### 3. 重试策略优化

```yaml
retry:
  failed:
    count:
      limit: 5    # 根据业务重要性调整
```

---

## 九、监控与运维

### 1. 日志关键字

- `开始执行任务` - 任务开始
- `任务已完成` - 任务成功
- `任务执行失败` - 任务失败
- `任务重试失败超过最大次数` - 需要人工介入
- `异常event处理成功` - 重试机制触发

### 2. 数据库监控

```sql
-- 查询待处理任务
SELECT * FROM retry_disruptor_task WHERE state = 0;

-- 查询失败次数过多的任务
SELECT * FROM retry_disruptor_task WHERE fail_count >= 2;

-- 统计各状态任务数
SELECT state, COUNT(*) FROM retry_disruptor_task GROUP BY state;
```

### 3. RocketMQ 监控

关注 Topic: `${rocketmq.producer.retryDisruptorTopic}`

---

## 十、注意事项

### ⚠️ 重要提醒

1. **任务幂等性**: 所有 TaskProcessor 实现必须保证幂等性，重试不会导致业务数据异常
2. **异常处理**: 不要吞掉异常，应该抛出让异常处理器捕获
3. **数据库版本号**: 更新操作必须使用 version 乐观锁，防止并发问题
4. **线程安全**: WorkHandler 是多线程并发执行，注意共享变量的线程安全
5. **日志规范**: 记录 taskId 便于链路追踪
6. **参数序列化**: taskParams 使用 JSON 字符串，注意特殊字符转义

### ❌ 常见陷阱

- 不要在 Processor 中执行长时间阻塞操作（考虑异步化）
- 避免在事件处理中发起远程调用而没有超时机制
- 不要修改 RetryDisruptorTaskEvent 对象的状态（事件会重复使用）
- 忘记设置 shouldCheckUnfinished 导致重复执行

---

## 十一、扩展阅读

- [LMAX Disruptor 官方文档](https://lmax-exchange.github.io/disruptor/)
- [DisruptFlow 项目架构设计](./ARCHITECTURE.md)
- [RocketMQ 延迟消息](./ROCKETMQ_CONFIG_GUIDE.md)
- [TaskProcessor 接口规范](./TASK_PROCESSOR_GUIDE.md)

---

## 十二、作者信息

- **作者**: chenxiaofei
- **创建日期**: 2026-02-18
- **项目**: DisruptFlow
- **最后更新**: 2026-03-25

---

**文档版本**: v1.0
**维护状态**: 活跃维护中
