package com.chenxiaofei.disruptflow.model.enums;

import lombok.Getter;

import static com.chenxiaofei.disruptflow.model.enums.RetryDisruptorTaskEnum.*;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description: 生命周期枚举
 */
@Getter
public enum RetryDisruptorLifeCycleEnum {

    DEMO_TASKS("demo任务",DEMO_TASK,DEMO_TASK2),
    CANCEL_LIFE("订单取消",CANCEL_EXPRESS,CANCEL_DELIVER,CANCEL_SHOP_INVENTORY,CANCEL_WAREHOUSE_INVENTORY,
            CANCEL_REFUND,CANCEL_ORDER_DELIVER_EXCEPTION,CANCEL_GROUP_ORDER,CANCEL_ORDER_AFTERSALE_TASK,CANCEL_FREE_GOODS_COUPON),
    ;



    RetryDisruptorLifeCycleEnum(String desc,RetryDisruptorTaskEnum... taskProcessors){
        this.desc = desc;
        this.taskProcessors = taskProcessors;
    }
    private String desc;

    private RetryDisruptorTaskEnum[] taskProcessors;
}
