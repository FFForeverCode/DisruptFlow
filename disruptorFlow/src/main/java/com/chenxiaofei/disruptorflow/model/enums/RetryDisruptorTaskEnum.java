package com.chenxiaofei.disruptorflow.model.enums;

import io.netty.util.internal.StringUtil;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description: 任务枚举
 */
@Getter
public enum RetryDisruptorTaskEnum {

    DEMO_TASK(1,"demo任务1","demoTaskProcessor1"),
    DEMO_TASK2(2,"demo任务2","demoTaskProcessor2"),
    ;


    private Integer value;

    private String desc;

    private String beanName;

    RetryDisruptorTaskEnum(Integer value, String desc, String beanName){
        this.value = value;
        this.desc = desc;
        this.beanName = beanName;
    }
    public static Optional<RetryDisruptorTaskEnum>findTaskProcessor(String beanName){
        return Stream.of(values())
                .filter(t ->  StringUtils.equals(t.getBeanName(),beanName))
                .findFirst();
    }
}
