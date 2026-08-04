package com.chenxiaofei.disruptflow.domain.processor.impl;

import com.alibaba.fastjson.JSON;
import com.chenxiaofei.disruptflow.domain.processor.TaskProcessor;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: chenxiaofei.ropz
 * @CreateDate: 2026/3/16 21:12
 * @Description:
 */
@Slf4j
@Service("orderCancelGenerateRefund")
public class OrderCancelGenerateRefund implements TaskProcessor {
    @Override
    public boolean execute(RetryDisruptorTask retryDisruptorTask) {
        log.info("生成退款处理器执行,task={}", JSON.toJSONString(retryDisruptorTask));
        return false;
    }
}
