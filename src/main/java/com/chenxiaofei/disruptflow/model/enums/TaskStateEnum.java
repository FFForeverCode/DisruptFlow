package com.chenxiaofei.disruptflow.model.enums;

import lombok.Getter;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description: 任务状态枚举
 */
@Getter
public enum TaskStateEnum {

    UN_FINISHED(0,"未完成"),
    FINISHED(1,"完成"),
    INVALID(2,"无效"),
    DOING(3,"执行中"),
    ;


    /**
     * state value
     */
    private Integer value;

    /**
     * state desc
     */
    private String desc;

    TaskStateEnum(Integer value, String desc){
        this.value = value;
        this.desc = desc;
    }



}
