# RocketMQ 配置与使用指南

## 一、配置说明

### 1.1 基础配置

在 `application-dev.yml` 或 `application.properties` 中配置 RocketMQ：

```yaml
rocketmq:
  enabled: true  # 是否启用 RocketMQ
  name-server: 127.0.0.1:9876  # NameServer 地址，多个用分号分隔
  producer-group: DisruptFlowProducerGroup  # 生产者组名
  consumer-group: DisruptFlowConsumerGroup  # 消费者组名
  send-msg-timeout: 3000  # 发送消息超时时间（毫秒）
  retry-times-when-send-failed: 2  # 同步发送失败重试次数
  retry-times-when-send-async-failed: 2  # 异步发送失败重试次数
  max-message-size: 4194304  # 消息最大大小（字节，默认4MB）
  compress-msg-body-over-howmuch: 4096  # 消息体压缩阈值（字节，默认4KB）
  retry-another-broker-when-not-store-ok: false  # 发送失败时是否重试其他 Broker
  producer:
    retry-disruptor-topic: disruptorFlowException  # 重试 Topic
    retry-disruptor-tags: disruptorFlowException   # 重试 Tags
```

### 1.2 配置项详解

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `rocketmq.enabled` | 是否启用 RocketMQ | true |
| `rocketmq.name-server` | NameServer 地址，集群用分号分隔 | 127.0.0.1:9876 |
| `rocketmq.producer-group` | 生产者组名 | DisruptFlowProducerGroup |
| `rocketmq.consumer-group` | 消费者组名 | DisruptFlowConsumerGroup |
| `rocketmq.send-msg-timeout` | 发送消息超时时间（毫秒） | 3000 |
| `rocketmq.retry-times-when-send-failed` | 同步发送失败重试次数 | 2 |
| `rocketmq.retry-times-when-send-async-failed` | 异步发送失败重试次数 | 2 |
| `rocketmq.max-message-size` | 消息最大大小（字节） | 4194304 (4MB) |
| `rocketmq.compress-msg-body-over-howmuch` | 消息体压缩阈值（字节） | 4096 (4KB) |
| `rocketmq.retry-another-broker-when-not-store-ok` | 发送失败是否重试其他 Broker | false |

## 二、如何使用

### 2.1 发送消息

使用 `RocketMQProducer` 发送消息：

```java
@Service
public class YourService {
    
    @Autowired
    private RocketMQProducer rocketMQProducer;
    
    public void sendMessage() {
        MessageBuild messageBuild = MessageBuild.builder()
            .topic("your-topic")
            .tags("your-tags")
            .keys("your-message-key")
            .body("your message body")
            .delay(DelayLevel.LEVEL_1)  // 可选：延迟级别
            .build();
            
        SendResult result = rocketMQProducer.send(messageBuild);
        
        if (result.getSendStatus() == SendStatus.SEND_OK) {
            log.info("消息发送成功，msgId: {}", result.getMsgId());
        }
    }
    
    public void asyncSendMessage() {
        MessageBuild messageBuild = MessageBuild.builder()
            .topic("your-topic")
            .tags("your-tags")
            .keys("your-message-key")
            .body("your message body")
            .build();
            
        rocketMQProducer.asyncSend(messageBuild, new SendCallback() {
            @Override
            public void onSuccess(SendResult sendResult) {
                log.info("异步消息发送成功，msgId: {}", sendResult.getMsgId());
            }
            
            @Override
            public void onException(Throwable e) {
                log.error("异步消息发送失败", e);
            }
        });
    }
}
```

### 2.2 消费消息

实现 `RocketMQConsumer` 接口并使用 `@RocketMQListener` 注解：

```java
@Service
@RocketMQListener(
    consumerGroup = "YourConsumerGroup",  // 消费者组（可选）
    topic = "your-topic",
    tags = "your-tags",
    consumeMode = ConsumeMode.CONCURRENTLY,  // 并发消费（默认）
    messageModel = MessageModel.CLUSTERING,  // 集群模式（默认）
    reconsumeTimes = 5,  // 最大重试次数
    consumeTimeout = 10  // 消费超时时间（秒）
)
public class YourMessageConsumer implements RocketMQConsumer<YourMessageType> {
    
    @Override
    public void onMessage(YourMessageType message) {
        try {
            // 处理消息
            log.info("收到消息: {}", message);
            
            // 业务逻辑...
            
        } catch (Exception e) {
            log.error("消息处理失败", e);
            // 抛出异常会触发重试
            throw new RuntimeException("消息处理失败", e);
        }
    }
}
```

### 2.3 延迟消息

使用 `DelayLevel` 枚举设置延迟级别：

```java
MessageBuild messageBuild = MessageBuild.builder()
    .topic("your-topic")
    .tags("your-tags")
    .keys("message-key")
    .body("delayed message")
    .delay(DelayLevel.LEVEL_3)  // 延迟 10 秒
    .build();
```

延迟级别对应表：

| 级别 | 延迟时间 |
|------|---------|
| LEVEL_1 | 1秒 |
| LEVEL_2 | 5秒 |
| LEVEL_3 | 10秒 |
| LEVEL_4 | 30秒 |
| LEVEL_5 | 1分钟 |
| LEVEL_6 | 2分钟 |
| LEVEL_7 | 3分钟 |
| LEVEL_8 | 4分钟 |
| LEVEL_9 | 5分钟 |
| LEVEL_10 | 6分钟 |
| LEVEL_11 | 7分钟 |
| LEVEL_12 | 8分钟 |
| LEVEL_13 | 9分钟 |
| LEVEL_14 | 10分钟 |
| LEVEL_15 | 20分钟 |
| LEVEL_16 | 30分钟 |
| LEVEL_17 | 1小时 |
| LEVEL_18 | 2小时 |

## 三、消费模式说明

### 3.1 ConsumeMode（消费模式）

- **CONCURRENTLY**（默认）：并发消费，多个线程同时消费不同消息，吞吐量高
- **ORDERLY**：顺序消费，同一个队列的消息按顺序消费，保证消息顺序性

### 3.2 MessageModel（消息模型）

- **CLUSTERING**（默认）：集群模式，一个消费者组内的消费者分摊消费消息
- **BROADCASTING**：广播模式，一个消费者组内的每个消费者都消费全部消息

## 四、重试机制

### 4.1 重试策略

当消息消费失败时，框架会自动重试：

1. **第一次消费失败**：立即重试（最多重试 `reconsumeTimes` 次）
2. **达到最大重试次数**：消息进入死信队列（DLQ），需要人工介入

### 4.2 配置重试次数

```java
@RocketMQListener(
    topic = "your-topic",
    reconsumeTimes = 5  // 最多重试 5 次
)
```

### 4.3 重试间隔

RocketMQ 默认的重试间隔：

- 第 1 次重试：10 秒
- 第 2 次重试：30 秒
- 第 3 次重试：1 分钟
- 第 4 次重试：2 分钟
- 第 5 次重试：3 分钟
- 第 6 次重试：4 分钟
- 第 7 次重试：5 分钟
- 第 8 次重试：6 分钟
- 第 9 次重试：7 分钟
- 第 10 次重试：8 分钟
- 第 11 次重试：9 分钟
- 第 12 次重试：10 分钟
- 第 13 次重试：20 分钟
- 第 14 次重试：30 分钟
- 第 15 次重试：1 小时
- 第 16 次重试：2 小时

## 五、最佳实践

### 5.1 消息 Key 设置

为每条消息设置唯一的 Key，方便排查问题：

```java
MessageBuild messageBuild = MessageBuild.builder()
    .topic("order-topic")
    .tags("order-create")
    .keys("ORDER_" + orderId)  // 使用业务唯一标识
    .body(messageBody)
    .build();
```

### 5.2 消费幂等性

消费端需要保证幂等性，防止消息重复消费：

```java
@Override
public void onMessage(OrderMessage message) {
    // 1. 查询是否已处理
    boolean processed = orderService.isProcessed(message.getMessageId());
    if (processed) {
        log.warn("消息已处理，跳过，msgId: {}", message.getMessageId());
        return;
    }
    
    // 2. 执行业务逻辑
    try {
        processOrder(message);
        
        // 3. 标记为已处理
        orderService.markAsProcessed(message.getMessageId());
    } catch (Exception e) {
        log.error("处理失败，将触发重试", e);
        throw e;
    }
}
```

### 5.3 异常处理

在消费方法中捕获异常并记录详细日志：

```java
@Override
public void onMessage(YourMessage message) {
    try {
        // 业务逻辑
    } catch (BusinessException e) {
        // 业务异常，记录日志但不重试
        log.error("业务处理失败，msgId: {}, error: {}", 
                 message.getMsgId(), e.getMessage());
    } catch (Exception e) {
        // 系统异常，触发重试
        log.error("系统异常，将触发重试，msgId: {}", 
                 message.getMsgId(), e);
        throw new RuntimeException(e);
    }
}
```

### 5.4 线程池配置

根据业务需求调整线程池配置：

```yaml
async-push-disruptor-flow-executor:
  core-pool-size: 10      # 核心线程数
  max-pool-size: 16       # 最大线程数
  keep-alive-time: 500    # 线程空闲时间（毫秒）
  queue-size: 10          # 队列大小
```

## 六、监控与运维

### 6.1 日志追踪

框架自动为每条消息生成 TraceId，可以通过日志追踪完整的消息处理链路：

```
2024-01-01 12:00:00.123 [Thread-1] INFO  [7F0000010A1B18B4AAC27C1D2D1F0000] YourConsumer - 收到消息, msgId: C0A800010A1B18B4AAC27C1D2D1F0000
2024-01-01 12:00:00.234 [Thread-1] INFO  [7F0000010A1B18B4AAC27C1D2D1F0000] YourConsumer - 消息处理完成, cost: 111ms
```

### 6.2 死信队列

当消息达到最大重试次数后，会进入死信队列（DLQ），Topic 命名为：`%DLQ%YourConsumerGroup`。

需要定期检查死信队列，处理异常消息。

### 6.3 告警配置

在 `application.yml` 中配置告警：

```yaml
# 告警配置示例
alert:
  enabled: true
  webhook: "https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=your-key"
  retry-threshold: 3  # 重试次数阈值
```

## 七、常见问题

### Q1: 消费者没有收到消息？

1. 检查 NameServer 地址是否正确
2. 检查 Topic 和 Tags 是否匹配
3. 检查消费者组名是否正确
4. 查看日志是否有异常

### Q2: 消息发送失败？

1. 检查 RocketMQ 服务是否正常
2. 检查 NameServer 地址配置
3. 检查网络连接
4. 查看发送超时配置

### Q3: 如何保证消息顺序？

使用顺序消费模式：

```java
@RocketMQListener(
    topic = "your-topic",
    consumeMode = ConsumeMode.ORDERLY,  // 顺序消费
    messageModel = MessageModel.CLUSTERING
)
```

注意：顺序消费会降低吞吐量，仅在需要保证消息顺序时使用。

### Q4: 如何处理大量消息？

1. 使用并发消费模式（默认）
2. 增加消费者实例（集群部署）
3. 调整线程池配置
4. 考虑使用批量消费（需自定义实现）

## 八、集成示例

### 8.1 与 DisruptFlow 集成

```java
@Service
public class OrderService {
    
    @Autowired
    private RocketMQProducer rocketMQProducer;
    
    @Transactional
    public void createOrder(Order order) {
        // 1. 保存订单到数据库
        orderMapper.insert(order);
        
        // 2. 发送订单创建消息
        MessageBuild messageBuild = MessageBuild.builder()
            .topic("order-topic")
            .tags("order-create")
            .keys("ORDER_" + order.getId())
            .body(JSON.toJSONString(order))
            .build();
            
        rocketMQProducer.send(messageBuild);
        
        // 3. 消息和业务在同一个事务中，保证一致性
    }
}

@Service
@RocketMQListener(
    topic = "order-topic",
    tags = "order-create",
    consumeMode = ConsumeMode.CONCURRENTLY
)
public class OrderCreateConsumer implements RocketMQConsumer<String> {
    
    @Override
    public void onMessage(String message) {
        Order order = JSON.parseObject(message, Order.class);
        
        // 处理订单创建后的逻辑
        // 如：发送通知、更新库存、计算积分等
    }
}
```

## 九、性能优化建议

1. **批量发送**：将多条小消息合并为一条大消息发送
2. **异步发送**：使用 `asyncSend` 方法提高发送吞吐量
3. **合理设置线程池**：根据 CPU 核心数和业务复杂度调整
4. **消息压缩**：对于大消息，框架会自动压缩（超过 4KB）
5. **消费批量处理**：在消费端批量处理消息，减少数据库交互次数

## 十、相关类说明

- **RocketMQProducer**：消息发送服务
- **RocketMQConsumer**：消息消费接口
- **RocketMQListener**：消费者注解
- **RocketMQListenerContainer**：消费者容器（自动管理）
- **RocketmqProperties**：配置属性类
- **MessageBuild**：消息构建器
- **DelayLevel**：延迟级别枚举
- **ConsumeMode**：消费模式枚举

## 十一、版本兼容

- RocketMQ 4.x/5.x
- Spring Boot 2.7.x
- JDK 8+
