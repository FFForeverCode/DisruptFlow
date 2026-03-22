package com.chenxiaofei.disruptflow.domain.processors.impl;

import com.alibaba.fastjson.JSON;
import com.chenxiaofei.disruptflow.domain.processors.TaskProcessor;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: chenxiaofei.ropz
 * @CreateDate: 2026/3/16 21:15
 * @Description:
 */
@Slf4j
@Service("orderCancelProcessOrderException")
public class OrderCancelProcessOrderException implements TaskProcessor {
    @Override
    public boolean execute(RetryDisruptorTask retryDisruptorTask) {
        log.info("处理订单异常处理器执行,task={}", JSON.toJSONString(retryDisruptorTask));
        return false;
    }
}
