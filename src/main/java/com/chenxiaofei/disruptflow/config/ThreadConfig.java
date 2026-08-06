package com.chenxiaofei.disruptflow.config;

import com.chenxiaofei.disruptflow.config.properties.AsyncExecutorProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步线程池配置。
 */
@Configuration
@RequiredArgsConstructor
public class ThreadConfig {

    private final AsyncExecutorProperties asyncExecutorProperties;

    @Bean("asyncPushDisruptorFlowExecutor")
    public ThreadPoolTaskExecutor asyncPushDisruptorFlowExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(asyncExecutorProperties.getCorePoolSize());
        taskExecutor.setMaxPoolSize(asyncExecutorProperties.getMaxPoolSize());
        taskExecutor.setKeepAliveSeconds(Math.max(1, asyncExecutorProperties.getKeepAliveTime() / 1000));
        taskExecutor.setQueueCapacity(asyncExecutorProperties.getQueueSize());
        taskExecutor.setThreadNamePrefix("async-push-disruptor-");
        taskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        taskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        taskExecutor.setAwaitTerminationSeconds(30);
        taskExecutor.initialize();
        return taskExecutor;
    }
}
