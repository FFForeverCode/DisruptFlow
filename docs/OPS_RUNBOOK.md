# DisruptFlow 生产运维 Runbook

## 1. 健康检查

- 应用健康：`GET /actuator/health`
- 就绪探针：`GET /actuator/health/readiness`
- 存活探针：`GET /actuator/health/liveness`
- 指标：`GET /actuator/prometheus`

## 2. 核心观测指标

- `disruptflow_task_success_total`
- `disruptflow_task_failed_total`
- `disruptflow_task_over_limit_total`

> 建议告警阈值：
> - 5 分钟内 `failed_total` 增量 > 100
> - 任意时间窗 `over_limit_total` 增量 > 0

## 3. 常见故障处置

### 3.1 消费失败率升高

1. 查看下游服务可用性与超时。
2. 检查 RocketMQ 积压与 NameServer 连通性。
3. 排查任务处理器是否抛出业务异常。

### 3.2 重试任务持续超限

1. 核查目标任务 `handle_processor` 对应处理器实现是否正确。
2. 对单一异常业务单号执行人工补偿。
3. 必要时临时下调流量并扩大重试间隔。

### 3.3 应用无法启动

1. 检查 JDK 版本是否 >= 17。
2. 检查 `DB_URL/DB_USERNAME/DB_PASSWORD` 是否正确。
3. 检查 `ROCKETMQ_NAMESRV_ADDR` 可连通。

## 4. 发布回滚

- 发布前：执行 CI 全量通过（测试 + 构建 + Docker Build）。
- 发布后：观察 10 分钟核心指标。
- 回滚策略：
  - K8s 使用 `kubectl rollout undo deployment/disruptflow -n disruptflow`
  - 回滚后立即验证 `/actuator/health` 与失败指标是否恢复。
