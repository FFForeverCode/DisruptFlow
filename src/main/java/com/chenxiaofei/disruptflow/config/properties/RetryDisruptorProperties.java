package com.chenxiaofei.disruptflow.config.properties;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 重试任务调度配置。
 */
@Data
@Validated
@ConfigurationProperties(prefix = "retry-disruptor")
public class RetryDisruptorProperties {

    @Min(1)
    private int failedCountLimit = 3;

    @Min(64)
    private int bufferSize = 1024;

    @Min(1)
    private int workerMultiplier = 2;
}

