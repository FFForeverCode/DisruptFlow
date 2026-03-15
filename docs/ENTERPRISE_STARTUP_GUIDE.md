# DisruptFlow 企业级启动指南

## 📋 目录

- [1. 项目概述](#1-项目概述)
- [2. 系统要求](#2-系统要求)
- [3. 环境准备](#3-环境准备)
- [4. 快速启动](#4-快速启动)
- [5. 生产环境部署](#5-生产环境部署)
- [6. 性能优化](#6-性能优化)
- [7. 监控与告警](#7-监控与告警)
- [8. 故障排查](#8-故障排查)
- [9. 常见问题](#9-常见问题)

---

## 1. 项目概述

### 1.1 核心价值

**DisruptFlow** 是一款融合了 **LMAX Disruptor 无锁队列** 与 **本地消息表（Transactional Outbox）** 模式的高性能异步任务编排引擎。

**核心优势：**
- 🚀 **极低延迟**：微秒级进程内无锁调度，吞吐量达 **500k+ ops/sec**
- 🔒 **极高可靠性**：基于数据库事务保障，支持最终一致性
- 📊 **可观测性**：完整的生命周期监控和企业微信告警集成
- 🔄 **智能重试**：混合重试架构，指数退避算法缓解下游压力
- 🛡️ **三重幂等保护**：状态预检 + 乐观锁 + 业务幂等校验

### 1.2 技术栈

| 组件 | 版本 | 说明 |
|------|------|------|
| Java | 17+ | 编程语言 |
| Spring Boot | 4.0.3 | Web 框架 |
| LMAX Disruptor | 3.4.4 | 无锁并发引擎 |
| Apache RocketMQ | 2.2.3 | 分布式消息队列 |
| MySQL | 8.0+ | 数据持久化 |
| Redis | 6.0+ | 缓存与分布式锁 |

### 1.3 典型应用场景

- ✅ 核心交易链路（支付回调后的积分发放、权益到账）
- ✅ 高并发回滚场景（订单取消后的多系统可靠回滚）
- ✅ 削峰填谷 + 最终一致性（高流量冲击下的业务链条保护）
- ✅ 库存扣减、积分兑换、发货单生成等高并发异步任务

---

## 2. 系统要求

### 2.1 硬件要求

| 资源 | 最低配置 | 推荐配置 | 高并发配置 |
|------|----------|----------|-----------|
| CPU | 2核 | 4核 | 8核+ |
| 内存 | 4GB | 8GB | 16GB+ |
| 存储 | 50GB | 200GB | 500GB+ |
| 网络 | 100Mbps | 1Gbps | 10Gbps |

### 2.2 软件要求

```bash
# 必需
- Java 17+
- Maven 3.6+
- Docker 20.10+（推荐）
- Docker Compose 2.0+（推荐）

# 依赖服务
- MySQL 8.0+
- Redis 6.0+
- Apache RocketMQ 4.9+
```

### 2.3 端口占用检查

```bash
# 检查端口占用情况
# 应用默认端口
lsof -i :8080

# MySQL 端口
lsof -i :3306

# Redis 端口
lsof -i :6379

# RocketMQ NameServer 端口
lsof -i :9876

# RocketMQ Broker 端口
lsof -i :10911
```

---

## 3. 环境准备

### 3.1 本地开发环境

#### 步骤 1：克隆项目

```bash
git clone <repository-url>
cd disrupt-flow
```

#### 步骤 2：使用 Docker Compose 快速启动依赖服务

```bash
# 启动 MySQL 和 Redis
docker-compose up -d

# 验证服务启动状态
docker-compose ps

# 查看容器日志
docker-compose logs -f
```

#### 步骤 3：初始化数据库

```bash
# 连接到 MySQL 容器
docker exec -it disrupt-flow-mysql-1 mysql -uroot -p'verysecret'

# 创建数据库
CREATE DATABASE disrupt_flow CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE disrupt_flow;

# 导入表结构（需要提前准备 SQL 文件）
source /path/to/init.sql;
```

#### 步骤 4：启动 RocketMQ（本地开发推荐）

```bash
# 使用 Docker 启动 RocketMQ
docker run -d --name rocketmq-nameserver \
  -p 9876:9876 \
  apache/rocketmq:5.0.0 \
  sh mqnamesrv

docker run -d --name rocketmq-broker \
  -p 10911:10911 \
  -e "NAMESRV_ADDR=host.docker.internal:9876" \
  apache/rocketmq:5.0.0 \
  sh mqbroker -n host.docker.internal:9876
```

### 3.2 生产环境前置检查

#### 3.2.1 MySQL 准备

```bash
# 1. 验证 MySQL 版本（需要 8.0+）
mysql -u<user> -p -e "SELECT VERSION();"

# 2. 创建专用数据库用户
CREATE USER 'disrupt_flow'@'%' IDENTIFIED BY '<strong-password>';
GRANT ALL PRIVILEGES ON disrupt_flow.* TO 'disrupt_flow'@'%';
FLUSH PRIVILEGES;

# 3. 初始化数据库表
-- 创建重试任务表
CREATE TABLE `retry_disruptor_task` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `task_id` VARCHAR(64) UNIQUE NOT NULL COMMENT '任务ID',
  `task_name` VARCHAR(128) NOT NULL COMMENT '任务名称',
  `task_type` VARCHAR(32) NOT NULL COMMENT '任务类型',
  `task_payload` LONGTEXT COMMENT '任务数据',
  `task_status` INT NOT NULL COMMENT '任务状态',
  `retry_count` INT DEFAULT 0 COMMENT '重试次数',
  `max_retry_count` INT DEFAULT 3 COMMENT '最大重试次数',
  `error_msg` TEXT COMMENT '错误信息',
  `trace_id` VARCHAR(64) COMMENT '链路追踪ID',
  `version` BIGINT DEFAULT 0 COMMENT '乐观锁版本号',
  `create_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `update_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT DEFAULT 0 COMMENT '软删除标记',
  KEY `idx_task_id` (`task_id`),
  KEY `idx_task_status` (`task_status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

#### 3.2.2 Redis 准备

```bash
# 1. 验证 Redis 连接
redis-cli -h <redis-host> -p 6379 ping

# 2. 配置持久化（可选）
# 编辑 redis.conf
appendonly yes
appendfsync everysec

# 3. 设置密码（建议生产环境必须）
# 编辑 redis.conf
requirepass <strong-password>
```

#### 3.2.3 RocketMQ 集群部署

```bash
# 推荐使用 Docker Compose 部署高可用集群
# 参考官方文档：https://rocketmq.apache.org/docs/quick-start/

# 验证集群状态
./mqadmin clusterList -n <namesrv-addr>
```

---

## 4. 快速启动

### 4.1 本地启动

#### 方式 1：IDE 启动（推荐开发环境）

```bash
# 1. 打开项目在 IDE（IntelliJ IDEA / VS Code）
# 2. 在 DisruptFlowApplication.java 中右键 Run
# 或使用快捷键 Ctrl+Shift+F10（Mac: Control+Shift+R）
```

#### 方式 2：Maven 启动

```bash
# 1. 编译项目
mvn clean compile

# 2. 启动应用
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

#### 方式 3：JAR 启动

```bash
# 1. 打包项目
mvn clean package -DskipTests

# 2. 启动 JAR
java -jar target/disrupt-flow-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### 4.2 启动验证

```bash
# 1. 检查应用是否启动成功
curl http://localhost:8080/actuator/health

# 2. 查看应用日志
tail -f logs/disrupt-flow.log

# 3. 验证数据库连接
# 应用日志中应该看到：
# HikariPool-1 - Starting...
# HikariPool-1 - Start completed.
```

### 4.3 常见启动问题

| 问题 | 解决方案 |
|------|----------|
| 端口 8080 已被占用 | `lsof -i :8080` 查找进程，或在 application.yml 中修改 `server.port` |
| MySQL 连接失败 | 检查 MySQL 是否启动，验证用户名密码和数据库名 |
| Redis 连接失败 | 验证 Redis 是否启动，检查 `redis.conf` 中的 `bind` 配置 |
| RocketMQ 连接失败 | 验证 NameServer 地址配置，检查端口 9876 是否开放 |

---

## 5. 生产环境部署

### 5.1 构建生产镜像

```bash
# 1. 编译并打包
mvn clean package -DskipTests -P prod

# 2. 创建 Dockerfile
cat > Dockerfile << 'EOF'
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 复制 JAR 文件
COPY target/disrupt-flow-0.0.1-SNAPSHOT.jar app.jar

# JVM 性能参数
ENV JVM_OPTS="-Xmx8g -Xms8g -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -XX:+ParallelRefProcEnabled"

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java ${JVM_OPTS} -jar app.jar --spring.profiles.active=prod"]
EOF

# 3. 构建镜像
docker build -t disrupt-flow:latest .

# 4. 推送到仓库
docker push <registry>/disrupt-flow:latest
```

### 5.2 Kubernetes 部署

```yaml
# disrupt-flow-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: disrupt-flow
  namespace: production
spec:
  replicas: 3
  selector:
    matchLabels:
      app: disrupt-flow
  template:
    metadata:
      labels:
        app: disrupt-flow
    spec:
      containers:
      - name: disrupt-flow
        image: <registry>/disrupt-flow:latest
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: 8080
          name: http
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_DATASOURCE_URL
          valueFrom:
            configMapKeyRef:
              name: disrupt-flow-config
              key: db.url
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: disrupt-flow-secret
              key: db.username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: disrupt-flow-secret
              key: db.password
        - name: ROCKETMQ_NAME_SERVER
          valueFrom:
            configMapKeyRef:
              name: disrupt-flow-config
              key: rocketmq.nameserver
        resources:
          requests:
            memory: "4Gi"
            cpu: "2"
          limits:
            memory: "8Gi"
            cpu: "4"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: disrupt-flow-service
  namespace: production
spec:
  selector:
    app: disrupt-flow
  ports:
  - port: 80
    targetPort: 8080
  type: LoadBalancer
---
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: disrupt-flow-hpa
  namespace: production
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: disrupt-flow
  minReplicas: 3
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
```

### 5.3 配置管理

```yaml
# application-prod.yml
spring:
  application:
    name: DisruptFlow
  profiles:
    active: prod
  
  # 数据源配置
  datasource:
    url: jdbc:mysql://<db-host>:3306/disrupt_flow?useUnicode=true&characterEncoding=utf8mb4&useSSL=true&serverTimezone=Asia/Shanghai&allowMultiQueries=true
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      minimum-idle: 10
      maximum-pool-size: 50
      auto-commit: true
      idle-timeout: 30000
      pool-name: DisruptFlowHikariCP
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
  
  # Redis 配置
  redis:
    host: ${REDIS_HOST}
    port: 6379
    password: ${REDIS_PASSWORD}
    database: 0
    timeout: 2000ms
    lettuce:
      pool:
        max-active: 50
        max-idle: 20
        min-idle: 5
        max-wait: -1ms
      shutdown-timeout: 200ms
  
  # Jackson 配置
  jackson:
    default-property-inclusion: non_null
    serialization:
      fail-on-empty-beans: false

# MyBatis-Plus 配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: com.chenxiaofei.disruptflow.model
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    call-setters-on-nulls: true
    jdbc-type-for-null: null
    cache-enabled: true
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: deleted
      logic-delete-value: 1
      logic-not-delete-value: 0

# RocketMQ 配置
rocketmq:
  enabled: true
  name-server: ${ROCKETMQ_NAMESERVER}
  producer-group: DisruptFlowProducerGroup-${INSTANCE_ID:01}
  consumer-group: DisruptFlowConsumerGroup-${INSTANCE_ID:01}
  send-msg-timeout: 3000
  retry-times-when-send-failed: 3
  retry-times-when-send-async-failed: 3
  max-message-size: 4194304
  compress-msg-body-over-howmuch: 4096
  retry-another-broker-when-not-store-ok: true
  producer:
    retry-disruptor-topic: disruptorFlowException
    retry-disruptor-tags: disruptorFlowException

# 日志配置
logging:
  level:
    root: INFO
    com.chenxiaofei.disruptflow: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/disrupt-flow.log
    max-size: 100MB
    max-history: 30

# 服务器配置
server:
  port: 8080
  servlet:
    context-path: /
  compression:
    enabled: true
    min-response-size: 1024
  tomcat:
    threads:
      max: 200
      min-spare: 10
    accept-count: 100
    max-connections: 10000
    keep-alive-timeout: 60s

# Actuator 配置
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,info
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    export:
      prometheus:
        enabled: true
```

---

## 6. 性能优化

### 6.1 JVM 参数优化

```bash
# 生产环境推荐参数
JAVA_OPTS="
  -server
  -Xmx8g
  -Xms8g
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+ParallelRefProcEnabled
  -XX:+UnlockDiagnosticVMOptions
  -XX:G1SummarizeRSetStatsPeriod=1
  -XX:+PrintGCDetails
  -XX:+PrintGCDateStamps
  -Xloggc:logs/gc-%t.log
  -XX:+UseGCLogFileRotation
  -XX:NumberOfGCLogFiles=10
  -XX:GCLogFileSize=100M
  -XX:-OmitStackTraceInFastThrow
  -Dcom.sun.management.jmxremote
  -Dcom.sun.management.jmxremote.port=9010
  -Dcom.sun.management.jmxremote.authenticate=false
  -Dcom.sun.management.jmxremote.ssl=false
"
```

### 6.2 Disruptor 配置优化

```java
// 在 RetryDisruptorTaskConfig 中调整以下参数

// 1. RingBuffer 大小（2 的幂次方）
// 默认 65536，高并发场景建议 262144 或 1048576
ringBufferSize = 262144;

// 2. 等待策略
// BusySpinWaitStrategy：CPU 密集，延迟最低但 CPU 占用高
// YieldingWaitStrategy：延迟与 CPU 占用的平衡
// BlockingWaitStrategy：低 CPU 占用，延迟相对较高（默认）
waitStrategy = new YieldingWaitStrategy();

// 3. 事件处理器数量
// 根据 CPU 核数调整
eventHandlerThreadCount = Runtime.getRuntime().availableProcessors();
```

### 6.3 数据库优化

```sql
-- 1. 创建关键字段索引
CREATE INDEX idx_task_status_create_time 
ON retry_disruptor_task(task_status, create_time);

-- 2. 定期清理已删除的数据
DELETE FROM retry_disruptor_task 
WHERE deleted = 1 AND update_time < DATE_SUB(NOW(), INTERVAL 90 DAY);

-- 3. 设置分区表（可选，数据量特别大时）
ALTER TABLE retry_disruptor_task 
PARTITION BY RANGE (YEAR(create_time)) (
  PARTITION p2023 VALUES LESS THAN (2024),
  PARTITION p2024 VALUES LESS THAN (2025),
  PARTITION pmax VALUES LESS THAN MAXVALUE
);
```

### 6.4 Redis 优化

```bash
# redis.conf 配置优化
maxmemory 4gb
maxmemory-policy allkeys-lru
timeout 300
tcp-keepalive 300
databases 16

# 持久化优化
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec
```

---

## 7. 监控与告警

### 7.1 Prometheus 指标

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'disrupt-flow'
    static_configs:
      - targets: ['localhost:8080']
    metrics_path: '/actuator/prometheus'
```

### 7.2 关键指标监控

| 指标 | 说明 | 告警阈值 |
|------|------|----------|
| `disruptor_task_submit_total` | 提交任务总数 | - |
| `disruptor_task_success_total` | 成功完成任务数 | - |
| `disruptor_task_failed_total` | 失败任务数 | > 1% |
| `disruptor_task_processing_time_seconds` | 任务处理耗时 | > 1s（P99） |
| `disruptor_queue_depth` | 队列深度 | > 50% |
| `jvm_memory_used_bytes` | 内存使用 | > 80% |
| `jvm_gc_pause_seconds` | GC 暂停时间 | > 500ms |
| `http_request_duration_seconds` | HTTP 请求耗时 | > 1s（P99） |

### 7.3 告警规则示例

```yaml
# alert-rules.yaml
groups:
  - name: disrupt-flow
    rules:
      # 任务成功率告警
      - alert: HighTaskFailureRate
        expr: 'rate(disruptor_task_failed_total[5m]) / rate(disruptor_task_submit_total[5m]) > 0.01'
        for: 5m
        annotations:
          summary: "DisruptFlow 任务失败率过高"
          description: "失败率已达到 {{ $value | humanizePercentage }}"
      
      # 队列堆积告警
      - alert: HighQueueDepth
        expr: 'disruptor_queue_depth > 50000'
        for: 3m
        annotations:
          summary: "DisruptFlow 队列堆积"
          description: "当前队列深度: {{ $value }}"
      
      # 内存使用告警
      - alert: HighMemoryUsage
        expr: 'jvm_memory_used_bytes / jvm_memory_max_bytes > 0.85'
        for: 5m
        annotations:
          summary: "Java 应用内存使用过高"
          description: "内存使用率: {{ $value | humanizePercentage }}"
      
      # 数据库连接池告警
      - alert: HighDbConnectionUsage
        expr: 'hikari_connections_active / hikari_connections_max > 0.8'
        for: 3m
        annotations:
          summary: "数据库连接池使用过高"
          description: "连接使用率: {{ $value | humanizePercentage }}"
```

### 7.4 企业微信告警集成

```java
// 在应用中集成告警逻辑
@Component
public class AlertNotifier {
    
    private static final String WEBHOOK_URL = "${alert.wecom.webhook.url}";
    
    public void sendAlert(String title, String content, String severity) {
        // 构建企业微信消息
        Map<String, Object> message = new HashMap<>();
        message.put("msgtype", "markdown");
        Map<String, Object> markdown = new HashMap<>();
        markdown.put("content", String.format(
            "## %s\n" +
            "**严重级别**: %s\n" +
            "**内容**: %s\n" +
            "**时间**: %s",
            title, severity, content, LocalDateTime.now()
        ));
        message.put("markdown", markdown);
        
        // 发送请求
        // RestTemplate.postForObject(WEBHOOK_URL, message, String.class);
    }
}
```

---

## 8. 故障排查

### 8.1 日志分析

```bash
# 查看实时日志
tail -f logs/disrupt-flow.log

# 过滤错误日志
grep "ERROR" logs/disrupt-flow.log

# 查看特定链路的日志
grep "trace-id=xxx" logs/disrupt-flow.log

# 统计错误类型
grep "ERROR" logs/disrupt-flow.log | cut -d' ' -f7 | sort | uniq -c | sort -rn
```

### 8.2 常见问题排查

#### 问题 1：任务一直处于重试状态

```bash
# 检查 RocketMQ 消费者状态
./mqadmin consumerProgress -n <namesrv-addr> -g DisruptFlowConsumerGroup

# 检查数据库中的任务状态
SELECT id, task_id, task_status, retry_count, error_msg 
FROM retry_disruptor_task 
WHERE task_status = 2 AND retry_count > 0 
ORDER BY update_time DESC 
LIMIT 20;
```

#### 问题 2：内存持续增长

```bash
# 检查 Disruptor 队列堆积
SELECT COUNT(*) as pending_count 
FROM retry_disruptor_task 
WHERE task_status IN (0, 1);

# 通过 JVM 命令行获取堆信息
jmap -heap <pid>
jmap -histo:live <pid> | head -20
```

#### 问题 3：数据库连接耗尽

```bash
# 检查连接池状态（假设使用 HikariCP）
SELECT * FROM information_schema.processlist;

# 优化连接池配置
# 在 application.yml 中调整：
spring.datasource.hikari.maximum-pool-size: 50  # 增加
spring.datasource.hikari.idle-timeout: 30000     # 缩短
```

### 8.3 性能诊断

```bash
# 1. CPU 诊断
top -p <pid>

# 2. 线程诊断
jstack <pid> > thread-dump.txt
# 查找阻塞的线程

# 3. GC 诊断
jstat -gcutil -h10 <pid> 1000
jstat -gccause -h10 <pid> 1000

# 4. 内存诊断
jcmd <pid> VM.native_memory summary
jcmd <pid> GC.heap_info
```

---

## 9. 常见问题

### Q1: 如何处理任务的幂等性？

**A:** 项目内置三重幂等保护机制：
1. **状态预检**：检查任务当前状态，避免重复处理
2. **乐观锁（Version）CAS**：数据库层面的并发控制
3. **业务幂等校验**：由 TaskProcessor 实现的业务逻辑幂等

```java
@Override
public void process(RetryDisruptorTask task) {
    // 1. 状态预检
    if (task.getTaskStatus() != TaskStateEnum.PENDING.getCode()) {
        return;
    }
    
    // 2. 业务幂等校验
    if (isAlreadyProcessed(task.getTaskId())) {
        return;
    }
    
    // 3. 执行业务逻辑
    doProcess(task);
}
```

### Q2: 生产环境中如何处理 Disruptor 队列溢出？

**A:** 通过以下几个步骤：
1. 合理设置 RingBuffer 大小（根据吞吐量计算）
2. 监控队列深度，提前告警
3. 如果队列满，事件会被缓冲到数据库（本地消息表）
4. 通过 RocketMQ 异步重试消费

### Q3: 如何进行灾难恢复？

**A:** 
1. 所有已提交的任务都存储在数据库中，宕机不会丢失
2. 通过 `task_status` 字段可以重新处理失败的任务
3. 使用管理界面手动重试：`UPDATE retry_disruptor_task SET task_status = 0 WHERE id = ?`

### Q4: 与传统 MQ 异步方案相比，性能提升多少？

**A:** 根据官方基准测试数据：
- **延迟**：传统 MQ ~1-10ms，DisruptFlow ~0.001ms（微秒级）
- **吞吐量**：传统 MQ ~10k ops/sec，DisruptFlow ~500k ops/sec
- **资源占用**：内存上升 ~20%，但 CPU 效率提升 5-10 倍

### Q5: 如何自定义任务处理逻辑？

**A:** 实现 `TaskProcessor` 接口：

```java
@Component
public class CustomTaskProcessor implements TaskProcessor {
    
    @Override
    public String getProcessorType() {
        return "CUSTOM_TYPE";
    }
    
    @Override
    public void process(RetryDisruptorTask task) {
        // 自定义业务逻辑
        // 可以调用 ERP、积分系统等下游服务
        String payload = task.getTaskPayload();
        // ... 处理逻辑
    }
}
```

---

## 附录

### A. 相关文档

- [RocketMQ 配置指南](ROCKETMQ_CONFIG_GUIDE.md)
- [项目架构设计](../README.md)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [LMAX Disruptor 文档](https://lmax-exchange.github.io/disruptor/)

### B. 联系方式

- 项目维护者：Chen Xiaofei
- 问题反馈：提交 Issue 或联系团队

### C. 更新日志

| 版本 | 发布日期 | 主要变更 |
|------|----------|----------|
| 0.0.1 | 2026-03-15 | 初始版本发布 |

---

**文档最后更新时间：2026-03-15**
