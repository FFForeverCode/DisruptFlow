package com.chenxiaofei.disruptflow.domain.processors.impl;

import com.alibaba.fastjson.JSON;
import com.chenxiaofei.disruptflow.domain.processors.TaskProcessor;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: chenxiaofei.ropz
 * @CreateDate: 2026/3/16 21:10
 * @Description:
 */
@Slf4j
@Service("orderCancelReturnWarehouseInventory")
public class OrderCancelReturnWarehouseInventory implements TaskProcessor {
    @Override
    public boolean execute(RetryDisruptorTask retryDisruptorTask) {
        log.info("退库存处理器执行,task={}", JSON.toJSONString(retryDisruptorTask));
        return false;
    }
}
