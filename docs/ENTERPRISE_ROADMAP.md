# DisruptFlow 企业级落地路线图

## 阶段一：生产可运行（已落地）

- 配置中心化：线程池、Disruptor、重试参数全部可配置。
- 生命周期治理：Disruptor / RocketMQ Consumer 支持优雅关闭。
- 启动可用性：ERP 告警能力提供缺省降级实现。
- 基础观测：Actuator + Prometheus 端点 + 任务核心指标。
- 部署资产：提供 Dockerfile 与 Kubernetes 清单。

## 阶段二：生产可观测（建议 1~2 周）

- 接入 OpenTelemetry Trace，统一 TraceID 到日志/指标。
- 增加关键告警规则：
  - `disruptflow.task.failed` 突增告警
  - `disruptflow.task.over_limit` 非零告警
  - RocketMQ 消费堆积告警
- 完成 SLI/SLO 定义：成功率、延迟、重试转化率。

## 阶段三：生产可治理（建议 2~4 周）

- 引入配置中心（Nacos/Apollo）+ 灰度参数发布。
- 引入任务死信管理台（DLQ）与人工重放流程。
- 补充运维闭环：变更审计、回滚预案、容量基线。

## 阶段四：生产可审计（建议 4~6 周）

- 引入数据库变更工具（Flyway/Liquibase）。
- 建立任务全链路审计：谁触发、何时执行、为何失败。
- 建立合规机制：凭据托管、敏感字段脱敏、最小权限访问。

## 最终目标

形成“高吞吐 + 高可靠 + 可观测 + 可审计 + 可运维”的企业级异步任务平台能力，支持核心交易链路长期稳定运行。
