CREATE TABLE IF NOT EXISTS retry_disruptor_task (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    bn VARCHAR(128) NOT NULL COMMENT '业务单号',
    life_cycle VARCHAR(64) NOT NULL COMMENT '生命周期',
    handle_processor VARCHAR(128) NOT NULL COMMENT '处理器枚举名称',
    state TINYINT NOT NULL DEFAULT 0 COMMENT '状态:0-未完成,1-完成,2-无效,3-执行中',
    fail_count TINYINT NOT NULL DEFAULT 0 COMMENT '失败次数',
    task_params JSON NULL COMMENT '任务参数',
    version INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
    remark VARCHAR(1024) NULL COMMENT '备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_bn_life_cycle_processor (bn, life_cycle, handle_processor),
    KEY idx_state_fail_count (state, fail_count),
    KEY idx_update_time (update_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='重试任务表';
