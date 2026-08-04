package com.chenxiaofei.disruptflow.domain.processor.impl;

import com.chenxiaofei.disruptflow.domain.processor.TaskProcessor;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: chenxiaofei.ropz
 * @CreateDate: 2026/3/16 21:09
 * @Description:
 */
@Slf4j
@Service("orderCancelReturnShopInventory")
public class OrderCancelReturnShopInventory implements TaskProcessor {
    @Override
    public boolean execute(RetryDisruptorTask retryDisruptorTask) {
        return false;
    }
}
