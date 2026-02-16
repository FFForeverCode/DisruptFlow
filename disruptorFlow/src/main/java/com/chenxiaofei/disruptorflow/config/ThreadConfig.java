package com.chenxiaofei.disruptorflow.config;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description:
 */
@Configuration
public class ThreadConfig {



    @Value("${asyncPushDisruptorFlowExecutor.corePoolSize}")
    private static int coreSize = 10;

    @Value("${asyncPushDisruptorFlowExecutor.MaxPoolSize}")
    private static int maxSize = 16;

    @Value("${asyncPushDisruptorFlowExecutor.keepAliveTime}")
    private static int keepAliveTime = 200;

    @Value("${asyncPushDisruptorFlowExecutor.queueSize}")
    private static int queueSize = 50;

    private static RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
    @Bean("asyncPushDisruptorFlowExecutor")
    public ThreadPoolExecutor getAsyncPushDisruptorFlowExecutor(){
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                coreSize,
                maxSize,
                keepAliveTime,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(queueSize),
                new ThreadFactory() {
                    private final AtomicInteger seq = new AtomicInteger(0);
                    @Override
                    public Thread newThread(@NonNull Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("asyncPushDisruptorFlowExecutor-thread-seq:{}"+seq.get());
                        seq.incrementAndGet();
                        return thread;
                    }
                },
                rejectedExecutionHandler
        );

        return threadPoolExecutor;
    }
}
