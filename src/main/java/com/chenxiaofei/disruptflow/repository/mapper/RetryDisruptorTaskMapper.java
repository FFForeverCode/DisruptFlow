package com.chenxiaofei.disruptflow.repository.mapper;

import com.chenxiaofei.disruptorflow.model.RetryDisruptorTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description:
 */
@Mapper
public interface RetryDisruptorTaskMapper {

    RetryDisruptorTask selectByPrimaryId(Long id);


    int updateToDoing(@Param("id") Long id,@Param("version") Integer version);

    int updateToFinished(@Param("id") Long id,@Param("version") Integer version);


    int incrementFailedCount(@Param("id") Long id,
                             @Param("remark") String remark,
                             @Param("version") Integer version
    );

    int insertBatch(List<RetryDisruptorTask> tasks);
}
