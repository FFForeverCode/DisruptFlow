package com.chenxiaofei.disruptflow.domain.processors.impl;

import com.alibaba.fastjson.JSON;
import com.chenxiaofei.disruptflow.domain.processors.TaskProcessor;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @Author: chenxiaofei.ropz
 * @CreateDate: 2026/3/16 21:19
 * @Description:
 */
@Slf4j
@Service("orderCancelReturnFreeGoodActCouponTask")
public class OrderCancelReturnFreeGoodActCouponTask implements TaskProcessor {
    @Override
    public boolean execute(RetryDisruptorTask retryDisruptorTask) {
        log.info("取消订单返还优惠券,task={}", JSON.toJSONString(retryDisruptorTask));
        return false;
    }
}
