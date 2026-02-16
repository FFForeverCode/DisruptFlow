package com.chenxiaofei.disruptorflow.repository;

import com.chenxiaofei.disruptorflow.model.RetryDisruptorTask;
import com.chenxiaofei.disruptorflow.repository.mapper.RetryDisruptorTaskMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description:
 */
@Slf4j
@Repository
public class RetryDisruptorTaskRepositoryImpl implements RetryDisruptorTaskRepository{

    @Resource
    private RetryDisruptorTaskMapper retryDisruptorTaskMapper;
    @Override
    public RetryDisruptorTask selectByPrimaryId(Long id) {
        log.info("查询重试任务，id={}",id);
        return retryDisruptorTaskMapper.selectByPrimaryId(id);
    }

    @Override
    public int updateToDoing(Long id, Integer version) {
        log.info("更新任务状态为正在执行中，id={},version={}",id,version);
        return retryDisruptorTaskMapper.updateToDoing(id,version);
    }

    @Override
    public int updateToFinished(Long id, Integer version) {
        log.info("更新任务状态为完成，id={},version={}",id,version);
        return retryDisruptorTaskMapper.updateToFinished(id,version);
    }

    @Override
    public int incrementFailedCount(Long id, String remark, Integer version) {
        log.info("更新任务失败次数，id={},remark={},version={}",id,remark,version);
        return retryDisruptorTaskMapper.incrementFailedCount(id,remark,version);
    }

    @Override
    public int insertBatch(List<RetryDisruptorTask> tasks) {
        log.info("批量插入任务，size={}",tasks.size());
        return retryDisruptorTaskMapper.insertBatch(tasks);
    }
}
