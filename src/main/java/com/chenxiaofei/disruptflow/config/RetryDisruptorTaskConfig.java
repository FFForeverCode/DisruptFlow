package com.chenxiaofei.disruptflow.config;

import com.chenxiaofei.disruptflow.domain.disruptor.impl.RetryTaskEventExceptionHandler;
import com.chenxiaofei.disruptflow.domain.disruptor.impl.RetryTaskEventWorkerHandler;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTaskEvent;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description: disruptor队列配置类
 */
@Configuration
public class RetryDisruptorTaskConfig {


    private static final String THREAD_FACTORY_DESC = "retry_disruptor_task_thread_seq:";

    private static final int BUFF_SIZE = 128;

    private static final int WORKER_SIZE = Runtime.getRuntime().availableProcessors() * 2;




    @Bean
    public RetryTaskEventWorkerHandler getRetryTaskEventWorkerHandler(){
        return new RetryTaskEventWorkerHandler();
    }

    @Bean
    public ExceptionHandler<? super RetryDisruptorTaskEvent> getRetryTaskEventExceptionHandler(){
        return new RetryTaskEventExceptionHandler();
    }

    @Bean
    public EventHandler<RetryDisruptorTaskEvent> disruptorClearEventHandler(){
        return (event,l,b)->{
            event = null;
        };
    }

    @Bean
    public Disruptor<RetryDisruptorTaskEvent> getDisruptor(){
        AtomicInteger atomicInteger = new AtomicInteger(0);
        Disruptor<RetryDisruptorTaskEvent> retryDisruptorTaskEventDisruptor = new Disruptor<>(
                RetryDisruptorTaskEvent::new,
                BUFF_SIZE,
                r -> {
                    return new Thread(r, THREAD_FACTORY_DESC + atomicInteger.getAndIncrement());
                }
        );
        RetryTaskEventWorkerHandler[] workerHandlers = new RetryTaskEventWorkerHandler[WORKER_SIZE];
        for (int i = 0; i < WORKER_SIZE; i++) {
            workerHandlers[i] = getRetryTaskEventWorkerHandler();
        }
        retryDisruptorTaskEventDisruptor.setDefaultExceptionHandler(getRetryTaskEventExceptionHandler());
        retryDisruptorTaskEventDisruptor.handleEventsWithWorkerPool(workerHandlers)
                .then(disruptorClearEventHandler());
        retryDisruptorTaskEventDisruptor.start();
        return retryDisruptorTaskEventDisruptor;
    }
}
