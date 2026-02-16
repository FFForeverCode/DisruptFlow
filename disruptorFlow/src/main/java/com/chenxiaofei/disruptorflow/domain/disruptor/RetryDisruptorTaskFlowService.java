package com.chenxiaofei.disruptorflow.domain.disruptor;

import com.chenxiaofei.disruptorflow.model.RetryDisruptorTask;

import java.util.List;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description: disruptor执行器接口
 */
public interface RetryDisruptorTaskFlowService {

    /**
     * 推送retryDisruptorTasks
     * @param retryDisruptorTasks
     */
    void pushDisruptorFlow(List<RetryDisruptorTask> retryDisruptorTasks);
}
