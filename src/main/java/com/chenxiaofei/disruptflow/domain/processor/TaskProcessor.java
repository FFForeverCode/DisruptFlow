package com.chenxiaofei.disruptflow.domain.processor;


import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description:
 */
public interface TaskProcessor {

    boolean execute(RetryDisruptorTask retryDisruptorTask);
}
