package com.chenxiaofei.disruptflow.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 异步推送线程池配置。
 */
@Data
@Validated
@ConfigurationProperties(prefix = "async-push-disruptor-flow-executor")
public class AsyncExecutorProperties {

    @Min(1)
    private int corePoolSize = 10;

    @Min(1)
    private int maxPoolSize = 16;

    @Min(0)
    private int keepAliveTime = 500;

    @Min(1)
    private int queueSize = 10;
}

