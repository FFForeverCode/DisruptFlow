package com.chenxiaofei.disruptflow.domain.processors;

import com.chenxiaofei.disruptorflow.model.RetryDisruptorTask;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description:
 */
public interface TaskProcessor {

    boolean execute(RetryDisruptorTask retryDisruptorTask);
}
