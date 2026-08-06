package com.chenxiaofei.disruptflow.domain.processor.impl;

import com.chenxiaofei.disruptflow.domain.processor.TaskProcessor;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import org.springframework.stereotype.Service;

/**
 * Demo 任务处理器2。
 */
@Service("demoTaskProcessor2")
public class DemoTaskProcessor2 implements TaskProcessor {

    @Override
    public boolean execute(RetryDisruptorTask retryDisruptorTask) {
        return true;
    }
}

