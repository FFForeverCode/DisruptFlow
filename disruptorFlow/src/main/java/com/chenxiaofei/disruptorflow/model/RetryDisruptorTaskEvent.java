package com.chenxiaofei.disruptorflow.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RetryDisruptorTaskEvent {


    /**
     * 重试任务实体类
     */
    private RetryDisruptorTask retryDisruptorTask;

    /**
     * 是否需要检查是否已经完成
     */
    private Boolean shouldCheckUnfinished;

    public boolean isShouldCheckUnfinished() {
        if(shouldCheckUnfinished == null){
            return Boolean.TRUE;
        }
        return shouldCheckUnfinished;
    }
}
