package com.chenxiaofei.disruptorflow.domain.disruptor.impl;

import com.chenxiaofei.disruptorflow.model.RetryDisruptorTaskEvent;
import com.lmax.disruptor.ExceptionHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description: event异常处理器
 */
@Slf4j
@Service
public class RetryTaskEventExceptionHandler implements ExceptionHandler<RetryDisruptorTaskEvent> {
    @Override
    public void handleEventException(Throwable ex, long sequence, RetryDisruptorTaskEvent event) {

    }

    @Override
    public void handleOnStartException(Throwable ex) {

    }

    @Override
    public void handleOnShutdownException(Throwable ex) {

    }
}
