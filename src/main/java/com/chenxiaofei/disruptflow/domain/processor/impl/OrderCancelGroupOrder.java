package com.chenxiaofei.disruptflow.domain.processor.impl;

import com.alibaba.fastjson.JSON;
import com.chenxiaofei.disruptflow.domain.processor.TaskProcessor;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: chenxiaofei.ropz
 * @CreateDate: 2026/3/16 21:16
 * @Description:
 */
@Slf4j
@Service("orderCancelGroupOrder")
public class OrderCancelGroupOrder implements TaskProcessor {
    @Override
    public boolean execute(RetryDisruptorTask retryDisruptorTask) {
        log.info("取消拼团,task={}", JSON.toJSONString(retryDisruptorTask));
        return false;
    }
}
