package com.chenxiaofei.disruptflow.repository;

import com.chenxiaofei.disruptorflow.model.RetryDisruptorTask;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description:
 */

public interface RetryDisruptorTaskRepository {


    RetryDisruptorTask selectByPrimaryId(Long id);


    int updateToDoing(Long id, Integer version);

    int updateToFinished(Long id,Integer version);


    int incrementFailedCount(Long id,
                             String remark,
                             Integer version
    );

    int insertBatch(List<RetryDisruptorTask> tasks);



}
