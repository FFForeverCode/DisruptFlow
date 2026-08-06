package com.chenxiaofei.disruptflow.config;

import com.chenxiaofei.disruptflow.config.properties.RetryDisruptorProperties;
import com.chenxiaofei.disruptflow.domain.disruptor.impl.RetryTaskEventExceptionHandler;
import com.chenxiaofei.disruptflow.domain.disruptor.impl.RetryTaskEventWorkerHandler;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTaskEvent;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Disruptor 队列配置。
 */
@Configuration
@RequiredArgsConstructor
public class RetryDisruptorTaskConfig {

    private static final String THREAD_FACTORY_DESC = "retry-disruptor-task-";

    private final RetryDisruptorProperties retryDisruptorProperties;

    @Bean
    public EventHandler<RetryDisruptorTaskEvent> disruptorClearEventHandler() {
        return (event, l, b) -> {
            event.setRetryDisruptorTask(null);
            event.setShouldCheckUnfinished(null);
        };
    }

    @Bean(destroyMethod = "shutdown")
    public Disruptor<RetryDisruptorTaskEvent> disruptor(
            RetryTaskEventExceptionHandler exceptionHandler,
            RetryTaskEventWorkerHandler retryTaskEventWorkerHandler
    ) {
        AtomicInteger sequence = new AtomicInteger(0);
        int workerSize = Math.max(1,
                Runtime.getRuntime().availableProcessors() * retryDisruptorProperties.getWorkerMultiplier());
        Disruptor<RetryDisruptorTaskEvent> retryDisruptorTaskEventDisruptor = new Disruptor<>(
                (EventFactory) RetryDisruptorTaskEvent::new,
                retryDisruptorProperties.getBufferSize(),
                (Executor) runnable -> new Thread(runnable, THREAD_FACTORY_DESC + sequence.getAndIncrement()),
                ProducerType.MULTI,
                new com.lmax.disruptor.BlockingWaitStrategy()
        );

        RetryTaskEventWorkerHandler[] workerHandlers = new RetryTaskEventWorkerHandler[workerSize];
        for (int index = 0; index < workerSize; index++) {
            workerHandlers[index] = retryTaskEventWorkerHandler;
        }

        retryDisruptorTaskEventDisruptor.setDefaultExceptionHandler(exceptionHandler);
        retryDisruptorTaskEventDisruptor.handleEventsWithWorkerPool(workerHandlers)
                .then(disruptorClearEventHandler());
        retryDisruptorTaskEventDisruptor.start();
        return retryDisruptorTaskEventDisruptor;
    }
}
