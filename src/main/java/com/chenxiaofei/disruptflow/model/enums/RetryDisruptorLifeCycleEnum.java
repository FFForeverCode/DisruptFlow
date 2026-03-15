package com.chenxiaofei.disruptflow.model.enums;

import lombok.Getter;

import static com.chenxiaofei.disruptflow.model.enums.RetryDisruptorTaskEnum.DEMO_TASK;
import static com.chenxiaofei.disruptflow.model.enums.RetryDisruptorTaskEnum.DEMO_TASK2;
/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description: 生命周期枚举
 */
@Getter
public enum RetryDisruptorLifeCycleEnum {

    DEMO_TASKS("demo任务",DEMO_TASK,DEMO_TASK2),
    ;



    RetryDisruptorLifeCycleEnum(String desc,RetryDisruptorTaskEnum... taskProcessors){
        this.desc = desc;
        this.taskProcessors = taskProcessors;
    }
    private String desc;

    private RetryDisruptorTaskEnum[] taskProcessors;
}
