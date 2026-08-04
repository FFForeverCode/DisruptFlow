package com.chenxiaofei.disruptflow.domain.processor.impl;

import com.alibaba.fastjson.JSON;
import com.chenxiaofei.disruptflow.domain.processor.TaskProcessor;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: chenxiaofei.ropz
 * @CreateDate: 2026/3/16 21:07
 * @Description:
 */
@Slf4j
@Service("orderCancelDeliver")
public class OrderCancelDeliver implements TaskProcessor {
    @Override
    public boolean execute(RetryDisruptorTask retryDisruptorTask) {
        log.info("运单取消处理器执行,task={}", JSON.toJSONString(retryDisruptorTask));
        return false;
    }
}
