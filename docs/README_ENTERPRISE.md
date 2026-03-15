# DisruptFlow 企业级部署与运维手册

<div align="center">

![Java](https://img.shields.io/badge/Java-17%2B-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen) 
![RocketMQ](https://img.shields.io/badge/RocketMQ-4.x%2F5.x-blue) ![Disruptor](https://img.shields.io/badge/Disruptor-3.4.4-red)

**企业级高性能异步任务编排引擎 | 单机吞吐量 50万+ TPS**

</div>

---

## 📋 快速导航

- [环境要求](#环境要求)
- [一键部署](#一键部署)
- [配置详解](#配置详解)
- [监控告警](#监控告警)
- [性能调优](#性能调优)
- [生产检查清单](#生产检查清单)
- [应急手册](#应急手册)

---

## 🛠️ 环境要求

### 硬件配置

| 环境类型 | CPU | 内存 | 磁盘 | 网络 |
|----------|-----|------|------|------|
| 开发环境 | 4核+ | 8GB+ | 50GB SSD | 千兆 |
| 测试环境 | 8核+ | 16GB+ | 100GB SSD | 千兆 |
| **生产环境** | **16核+** | **32GB+** | **500GB SSD** | **万兆** |

### 软件版本

```bash
# 必须组件
Java: JDK 17 or higher
Maven: 3.6+
MySQL: 8.0.28+
RocketMQ: 4.9.4 or 5.x
Docker: 20.10+ (可选)

# 验证命令
java -version
mysql --version
mvn -version
```

### 网络端口

| 组件 | 端口 | 说明 | 防火墙 |
|------|------|------|--------|
| Spring Boot | 8080 | HTTP服务 | 必须开放 |
| RocketMQ NameServer | 9876 | 名称服务 | 必须开放 |
| RocketMQ Broker | 10911/10912 | 消息服务 | 必须开放 |
| MySQL | 3306 | 数据库 | 内网访问 |
| JMX | 9999 | 监控 | 内网访问 |

---

## 🚀 一键部署

### Docker Compose 方案（推荐）

#### 1. 创建部署目录

```bash
mkdir -p /opt/disruptflow/{app,config,logs,data}
cd /opt/disruptflow
```

#### 2. 创建 docker-compose.yml

```yaml
cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  # MySQL 数据库
  mysql:
    image: mysql:8.0.32
    container_name: disruptflow-mysql
    restart: always
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_ROOT_PASS:-DisruptFlow_2024}
      MYSQL_DATABASE: disrupt_flow
      MYSQL_USER: disruptflow
      MYSQL_PASSWORD: ${DB_PASS:-DisruptFlow_2024}
    ports:
      - "3306:3306"
    volumes:
      - ./data/mysql:/var/lib/mysql
      - ./init.sql:/docker-entrypoint-initdb.d/init.sql
    command: 
      --character-set-server=utf8mb4
      --collation-server=utf8mb4_unicode_ci
      --innodb_buffer_pool_size=2G
      --max_connections=1000
    networks:
      - disruptflow-net

  # RocketMQ NameServer
  rmqnamesrv:
    image: apache/rocketmq:4.9.4
    container_name: disruptflow-rmq-namesrv
    restart: always
    ports:
      - "9876:9876"
    volumes:
      - ./data/rmq/namesrv/logs:/home/rocketmq/logs
    command: sh mqnamesrv
    networks:
      - disruptflow-net

  # RocketMQ Broker
  rmqbroker:
    image: apache/rocketmq:4.9.4
    container_name: disruptflow-rmq-broker
    restart: always
    ports:
      - "10911:10911"
      - "10912:10912"
      - "10909:10909"
    environment:
      NAMESRV_ADDR: rmqnamesrv:9876
    volumes:
      - ./data/rmq/broker/logs:/home/rocketmq/logs
      - ./data/rmq/broker/store:/home/rocketmq/store
      - ./broker.conf:/home/rocketmq/rocketmq-4.9.4/conf/broker.conf
    command: sh mqbroker -c /home/rocketmq/rocketmq-4.9.4/conf/broker.conf
    depends_on:
      - rmqnamesrv
    networks:
      - disruptflow-net

  # DisruptFlow 应用
  app:
    image: disruptflow:latest  # 需要先构建镜像
    container_name: disruptflow-app
    restart: always
    ports:
      - "8080:8080"
      - "9999:9999"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - DB_PORT=3306
      - DB_NAME=disrupt_flow
      - DB_USERNAME=disruptflow
      - DB_PASSWORD=${DB_PASS:-DisruptFlow_2024}
      - ROCKETMQ_NAMESRV_ADDR=rmqnamesrv:9876
      - JVM_OPTS=-Xms4g -Xmx4g -XX:+UseG1GC -XX:MaxGCPauseMillis=100
    volumes:
      - ./logs:/app/logs
      - ./application-prod.yml:/app/config/application-prod.yml
    depends_on:
      - mysql
      - rmqnamesrv
      - rmqbroker
    networks:
      - disruptflow-net
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 8G
        reservations:
          cpus: '2'
          memory: 4G

networks:
  disruptflow-net:
    driver: bridge

volumes:
  mysql_data:
  rmq_namesrv_logs:
  rmq_broker_logs:
  rmq_broker_store:
EOF
```

#### 3. 初始化 SQL 文件

```bash
cat > init.sql << 'EOF'
-- 任务主表
CREATE TABLE IF NOT EXISTS retry_disruptor_task (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    task_type VARCHAR(100) NOT NULL COMMENT '任务类型',
    task_data TEXT NOT NULL COMMENT '任务数据(JSON)',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0-初始化 1-处理中 2-成功 3-失败 4-已死亡',
    retry_count INT NOT NULL DEFAULT 0 COMMENT '重试次数',
    max_retry_count INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    next_retry_time DATETIME DEFAULT NULL COMMENT '下次重试时间',
    error_message TEXT COMMENT '错误信息',
    trace_id VARCHAR(64) COMMENT '追踪ID',
    INDEX idx_status_next_retry (status, next_retry_time),
    INDEX idx_create_time (create_time),
    INDEX idx_trace_id (trace_id),
    INDEX idx_task_type_status (task_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='异步任务表';

-- 幂等记录表
CREATE TABLE IF NOT EXISTS idempotence_record (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    biz_key VARCHAR(200) NOT NULL UNIQUE COMMENT '业务防重键',
    expire_time DATETIME NOT NULL COMMENT '过期时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='幂等记录表';

-- 任务历史表 (可选)
CREATE TABLE IF NOT EXISTS retry_disruptor_task_history (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT UNSIGNED NOT NULL COMMENT '原任务ID',
    task_type VARCHAR(100) NOT NULL,
    task_data TEXT NOT NULL,
    status TINYINT NOT NULL,
    retry_count INT NOT NULL,
    error_message TEXT,
    complete_time DATETIME NOT NULL,
    INDEX idx_task_id (task_id),
    INDEX idx_complete_time (complete_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务历史表';
EOF
```

#### 4. Broker 配置

```bash
cat > broker.conf << 'EOF'
brokerClusterName=DisruptFlowCluster
brokerName=broker-a
brokerId=0
deleteWhen=04
fileReservedTime=48
brokerRole=ASYNC_MASTER
flushDiskType=ASYNC_FLUSH
namesrvAddr=rmqnamesrv:9876
brokerIP1=localhost
autoCreateTopicEnable=true
autoCreateSubscriptionGroup=true
EOF
```

#### 5. 启动服务

```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f app

# 查看健康状态
curl http://localhost:8080/actuator/health
```

---

## ⚙️ 配置详解

### JVM 参数（生产推荐）

```bash
JAVA_OPTS="-server
-Xms8g -Xmx8g
-XX:MetaspaceSize=512m -XX:MaxMetaspaceSize=512m
-XX:+UseG1GC -XX:MaxGCPauseMillis=100
-XX:+ParallelRefProcEnabled -XX:+UseStringDeduplication
-XX:+UnlockExperimentalVMOptions -XX:+UseContainerSupport
-Xloggc:/app/logs/gc.log
-XX:+PrintGCDetails -XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/app/logs/heapdump.hprof
-Dfile.encoding=UTF-8
-Dapp.name=DisruptFlow"
```

### 核心配置参数

#### Disruptor 配置

```yaml
retry:
  disruptor:
    ring-buffer-size: 1048576      # 2^20，高吞吐场景
    wait-strategy: BLOCKING         # 选项: BLOCKING, YIELDING, BUSY_SPIN
    consumer-thread-count: 32       # CPU 核心数 * 2
    batch-size: 100                 # 批量消费大小
```

#### RocketMQ 配置

```yaml
rocketmq:
  name-server: "10.0.0.1:9876;10.0.0.2:9876"  # 集群地址
  producer-group: DisruptFlow_PRODUCER
  consumer-group: DisruptFlow_CONSUMER
  send-msg-timeout: 3000
  retry-times-when-send-failed: 3
  consumer:
    consume-thread-max: 64
    max-reconsume-times: 16
```

#### 数据库配置

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 20
      maximum-pool-size: 100
      connection-timeout: 30000
      max-lifetime: 1800000
      leak-detection-threshold: 60000
```

---

## 📊 监控告警

### Prometheus 监控指标

```yaml
# 在 application.yml 中启用
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

**关键指标**

| 指标名称 | 类型 | 说明 | 告警阈值 |
|----------|------|------|----------|
| `disruptflow_task_submitted_total` | Counter | 任务提交数 | - |
| `disruptflow_task_failed_total` | Counter | 任务失败数 | > 100/5min |
| `disruptflow_disruptor_remaining_capacity` | Gauge | RingBuffer 剩余容量 | < 10000 |
| `process_cpu_usage` | Gauge | CPU 使用率 | > 80% |
| `jvm_memory_used_percent` | Gauge | JVM 内存使用率 | > 85% |

### Grafana 仪表盘

**导入 Dashboard ID: `1860` (Spring Boot)**

**自定义面板**

```promql
# 任务成功率
1 - (disruptflow_task_failed_total / disruptflow_task_submitted_total)

# 平均处理耗时
disruptflow_task_processing_duration_seconds_sum / 
disruptflow_task_processing_duration_seconds_count

# RocketMQ 发送延迟
histogram_quantile(0.95, disruptflow_rocketmq_send_duration_seconds_bucket)
```

### 告警规则

```yaml
# alert-rules.yml
groups:
- name: disruptflow
  rules:
  - alert: TaskProcessingFailure
    expr: rate(disruptflow_task_failed_total[5m]) > 10
    for: 5m
    labels:
      severity: warning
    annotations:
      summary: "任务处理失败率过高"
      description: "5分钟内失败率 > 10/秒"
  
  - alert: RingBufferFull
    expr: disruptflow_disruptor_remaining_capacity < 10000
    for: 2m
    labels:
      severity: critical
    annotations:
      summary: "RingBuffer 即将满"
      description: "剩余容量 < 10000"
```

---

## 🚀 性能调优

### 1. 压测基准

```bash
# 使用 JMeter 或自定义压测脚本
# 目标指标:
# - 单机吞吐量: 50万+ TPS
# - 平均延迟: < 1ms
# - P99 延迟: < 10ms

# 压测命令示例
java -jar disruptflow-benchmark.jar \
  --threads=32 \
  --duration=600 \
  --task-type=ORDER_PROCESS
```

### 2. 调优参数

#### 高吞吐场景

```yaml
# 牺牲部分延迟，提升吞吐量
retry:
  disruptor:
    ring-buffer-size: 2097152          # 2^21
    wait-strategy: BLOCKING
    consumer-thread-count: 64
    batch-size: 500

rocketmq:
  consumer:
    consume-thread-max: 128
```

#### 低延迟场景

```yaml
# 追求极致延迟
retry:
  disruptor:
    ring-buffer-size: 65536            # 2^16
    wait-strategy: YIELDING            # 或 BUSY_SPIN
    consumer-thread-count: 16
    batch-size: 10

rocketmq:
  send-msg-timeout: 1000
```

### 3. 系统参数

```bash
# /etc/sysctl.conf
net.core.somaxconn = 32768
net.ipv4.tcp_max_syn_backlog = 65536
fs.file-max = 1000000

# /etc/security/limits.conf
* soft nofile 1000000
* hard nofile 1000000
```

---

## ✅ 生产检查清单

### 部署前检查

- [ ] JDK 17 已安装，JAVA_HOME 配置正确
- [ ] MySQL 8.0+ 部署完成，字符集 utf8mb4
- [ ] RocketMQ 集群部署，至少 2 主 2 从
- [ ] 服务器时间同步（NTP）
- [ ] 文件句柄限制 >= 65536
- [ ] 防火墙端口已开放

### 配置检查

- [ ] 数据库连接信息正确
- [ ] RocketMQ NameServer 地址正确
- [ ] JVM 参数根据内存调整
- [ ] 线程池配置合理
- [ ] 告警 webhook 配置并测试
- [ ] 敏感信息加密存储

### 部署后验证

- [ ] 应用健康检查返回 "UP"
- [ ] 能正常提交测试任务
- [ ] 监控指标正常上报
- [ ] 告警功能测试通过
- [ ] 日志输出正常，无 ERROR

### 高可用配置

- [ ] 部署 >= 3 个实例
- [ ] 负载均衡配置健康检查
- [ ] MySQL 主从复制
- [ ] RocketMQ 集群模式
- [ ] 配置优雅下线

---

## 🚨 应急手册

### 场景 1: 服务无响应

```bash
# 1. 检查进程
ps -ef | grep disruptflow

# 2. 检查端口
netstat -tlnp | grep 8080

# 3. 查看线程状态
jstack $(pgrep -f disruptflow) > jstack.log

# 4. 查看内存
jmap -heap $(pgrep -f disruptflow)

# 5. 紧急重启
systemctl restart disruptflow
```

### 场景 2: 任务积压

```sql
-- 查看积压数量
SELECT status, COUNT(*) FROM retry_disruptor_task 
WHERE status IN (0, 1) GROUP BY status;

-- 查看失败任务
SELECT * FROM retry_disruptor_task 
WHERE status = 3 
ORDER BY retry_count DESC 
LIMIT 100;
```

### 场景 3: 数据库连接池耗尽

```bash
# 查看连接数
mysql> SHOW PROCESSLIST;

# 查看慢查询
mysql> SELECT * FROM information_schema.processlist 
WHERE TIME > 60;
```

### 场景 4: RocketMQ 消息堆积

```bash
# 查看消费延迟
mqadmin consumerProgress -g DisruptFlowConsumerGroup -n localhost:9876

# 重置消费位点（慎用）
mqadmin resetOffsetByTime -g DisruptFlowConsumerGroup -t disruptorFlowException -s now -n localhost:9876
```

### 紧急联系方式

- 技术负责人: [填写]
- DBA: [填写]
- 运维: [填写]
- 架构师: [填写]

---

## 📚 相关文档

- [RocketMQ 配置指南](ROCKETMQ_CONFIG_GUIDE.md)
- [开发手册](./DEVELOPMENT_GUIDE.md)
- [API 文档](./API_DOCUMENTATION.md)
- [架构设计](./ARCHITECTURE.md)

---

## 🤝 技术支持

- GitHub Issues: [https://github.com/FFForeverCode/DisruptFlow/issues](https://github.com/FFForeverCode/DisruptFlow/issues)
- 邮件支持: [填写企业邮箱]
- 企业微信: [填写群二维码]

---

<div align="center">

**⚠️ 生产环境部署前，请仔细阅读本手册并严格执行检查清单**

**🔒 确保所有敏感信息已加密，备份策略已配置**

**📊 监控告警已启用，应急方案已准备**

</div>
