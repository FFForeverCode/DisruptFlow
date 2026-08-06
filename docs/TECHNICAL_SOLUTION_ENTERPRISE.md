# DisruptFlow 企业级落地技术方案

## 1. 目标与范围

本方案面向“可在生产环境稳定运行”的核心诉求，聚焦以下能力：

- 高可用：服务具备优雅停机、健康探针、可观测指标。
- 高可靠：任务失败路径可追踪、可重试、可人工介入。
- 可治理：关键并发参数配置化、可按环境动态调整。
- 可运维：提供标准化 `dev/prod` 配置模板与上线检查项。

## 2. 现状痛点（改造前）

- 线程池参数通过 `static + @Value` 注入，配置无法生效，存在生产风险。
- Disruptor 固定 RingBuffer 与 Worker 数，无法按机器规格调优。
- 任务失败路径存在双重计数风险，且空事件/非法处理器缺少防御。
- 缺乏健康探针与统一管理端点，不利于 K8s/容器化治理。

## 3. 本次重构设计

### 3.1 配置治理（Configuration as Code）

- 新增 `async-push-disruptor-flow-executor` 类型化配置：
  - 文件：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/src/main/java/com/chenxiaofei/disruptflow/config/properties/AsyncExecutorProperties.java`
- 新增 `retry-disruptor` 类型化配置：
  - 文件：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/src/main/java/com/chenxiaofei/disruptflow/config/properties/RetryDisruptorProperties.java`
- 所有核心参数支持环境化配置（`application-dev.yml` 与 `application-prod.yml`）。

### 3.2 并发模型升级

- 线程池从手工 `ThreadPoolExecutor` 切换为 `ThreadPoolTaskExecutor`，并启用：
  - 拒绝策略 `CallerRunsPolicy`
  - 停机等待 `setWaitForTasksToCompleteOnShutdown(true)`
  - 终止超时 `30s`
- 文件：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/src/main/java/com/chenxiaofei/disruptflow/config/ThreadConfig.java`

### 3.3 Disruptor 生命周期治理

- RingBuffer 容量、Worker 并发度均支持配置化。
- Bean 增加 `destroyMethod = "shutdown"`，确保容器停止时优雅释放。
- 事件清理改为字段置空，避免“`event = null` 无效赋值”。
- 文件：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/src/main/java/com/chenxiaofei/disruptflow/config/RetryDisruptorTaskConfig.java`

### 3.4 任务执行可靠性增强

- 增加空事件防御与非法处理器兜底。
- 修复失败路径“双重失败计数”的一致性问题。
- 失败计数前主动读取最新版本，降低乐观锁冲突导致的丢更新风险。
- 超限告警发送失败改为记录日志并返回，避免无意义的重复重试风暴。
- 增加处理器注册中心，启动即校验 `任务类型 -> Bean` 映射，失败快速暴露。
- 文件：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/src/main/java/com/chenxiaofei/disruptflow/domain/disruptor/impl/RetryTaskEventWorkerHandler.java`

### 3.5 启动可用性与降级能力

- 提供默认 ERP 告警降级实现，避免未接入外部 ERP 时应用无法启动。
- 修正任务枚举与处理器 BeanName 的不一致问题，避免运行时路由失败。
- 文件：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/src/main/java/com/chenxiaofei/disruptflow/domain/erp/impl/ErpFallbackConfig.java`
- 文件：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/src/main/java/com/chenxiaofei/disruptflow/domain/processors/TaskProcessorRegistry.java`

### 3.6 可观测性与运维能力

- 引入 Actuator + Validation 依赖。
- 暴露端点：`health,info,metrics,prometheus`。
- 新增 Disruptor 健康探针：
  - 文件：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/src/main/java/com/chenxiaofei/disruptflow/support/health/DisruptorHealthIndicator.java`
- 新增任务关键指标：`disruptflow.task.success`、`disruptflow.task.failed`、`disruptflow.task.over_limit`。
- 启用优雅停机：`server.shutdown=graceful`。

### 3.7 MQ 生命周期治理

- 统一管理 RocketMQ Producer 与 Consumer Container 生命周期。
- 应用关闭时主动 stop+destroy 消费容器，减少连接泄漏与重复消费风险。
- 文件：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/src/main/java/com/chenxiaofei/disruptflow/config/RocketMQConfig.java`

## 4. 生产环境配置基线

新增 `application-prod.yml`，建议通过环境变量注入关键值：

- 数据库：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`
- MQ：`ROCKETMQ_NAMESRV_ADDR`、`RETRY_DISRUPTOR_TOPIC`
- 并发：`ASYNC_CORE_POOL_SIZE`、`ASYNC_MAX_POOL_SIZE`
- 重试：`RETRY_FAILED_COUNT_LIMIT`、`RETRY_DISRUPTOR_BUFFER_SIZE`

文件路径：

- `/Users/chenxiaofei.ropz/Desktop/DisruptFlow/src/main/resources/application-prod.yml`

## 5. 上线验收清单（Go-Live Checklist）

- 启动前检查
  - JDK 版本满足项目基线（建议 17+）。
  - `application-prod.yml` 的敏感信息均来自环境变量，不落盘明文。
- 运行时检查
  - `GET /actuator/health` 返回 `UP`。
  - `disruptorCursor` 指标可正常采集。
  - 线程池拒绝数与任务积压告警接入监控平台。
- 故障演练
  - 下游故障时任务进入延迟重试链路。
  - 超过最大重试后触发人工介入告警。

## 6. 后续演进建议（下一阶段）

- 增加 `retry_disruptor_task` DDL 与 Mapper XML 的自动化校验测试。
- 引入任务死信队列（DLQ）管理页面，支持人工重放。
- 增加分布式链路追踪（OpenTelemetry）并打通日志与指标。
- 将核心参数纳入配置中心并支持灰度发布策略。

## 7. 部署资产

- Docker 构建文件：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/deploy/docker/Dockerfile`
- K8s 清单：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/deploy/k8s/deployment.yaml`
- 部署说明：`/Users/chenxiaofei.ropz/Desktop/DisruptFlow/deploy/README.md`
