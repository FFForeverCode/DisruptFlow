package com.chenxiaofei.disruptflow.domain.processor.impl;


import com.chenxiaofei.disruptflow.domain.processor.TaskProcessor;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description:
 */
@Slf4j
@Service("demoTaskProcessor1")
public class DemoTaskProcessor1 implements TaskProcessor {
    @Override
    public boolean execute(RetryDisruptorTask retryDisruptorTask) {
        return true;
    }
}
