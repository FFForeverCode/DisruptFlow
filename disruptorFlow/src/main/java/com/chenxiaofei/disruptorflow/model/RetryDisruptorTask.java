package com.chenxiaofei.disruptorflow.model;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description: 重试任务实体类
 */
@Data
public class RetryDisruptorTask implements Serializable {


    /**
     * id
     */
    private Long id;

    /**
     * 业务单号
     */
    private String bn;

    /**
     * 多任务生命周期
     */
    private String lifeCycle;

    /**
     * 任务处理器
     */
    private String handleProcessor;

    /**
     * 状态
     */
    private Byte state;

    /**
     * 失败次数
     */
    private Byte failCount;

    /**
     * 任务参数
     */
    private String taskParams;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * created
     */
    private Date createTime;

    /**
     * updated
     */
    private Date updateTime;

    /**
     * 备注
     */
    private String remark;

    private static final long serialVersionUID = 1L;

}
