package com.chenxiaofei.disruptflow.domain.disruptor.impl;

import com.alibaba.fastjson.JSON;
import com.chenxiaofei.disruptflow.config.properties.RetryDisruptorProperties;
import com.chenxiaofei.disruptflow.domain.erp.ErpService;
import com.chenxiaofei.disruptflow.domain.processors.TaskProcessor;
import com.chenxiaofei.disruptflow.domain.processors.TaskProcessorRegistry;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTaskEvent;
import com.chenxiaofei.disruptflow.model.enums.TaskStateEnum;
import com.chenxiaofei.disruptflow.repository.mapper.RetryDisruptorTaskMapper;
import com.chenxiaofei.disruptflow.support.utils.UserContext;
import io.micrometer.core.instrument.MeterRegistry;
import com.lmax.disruptor.WorkHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.Objects;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description: event工作线程
 */
@Slf4j
@Service
public class RetryTaskEventWorkerHandler  implements WorkHandler<RetryDisruptorTaskEvent> {


    @Resource
    private RetryDisruptorTaskMapper retryDisruptorTaskMapper;

    @Resource
    private ErpService erpService;

    @Resource
    private RetryDisruptorProperties retryDisruptorProperties;

    @Resource
    private TaskProcessorRegistry taskProcessorRegistry;

    @Resource
    private MeterRegistry meterRegistry;


    @Override
    public void onEvent(RetryDisruptorTaskEvent retryDisruptorTaskEvent) throws Exception {
        if (Objects.isNull(retryDisruptorTaskEvent) || Objects.isNull(retryDisruptorTaskEvent.getRetryDisruptorTask())) {
            log.warn("收到空任务事件,忽略执行,event={}", JSON.toJSONString(retryDisruptorTaskEvent));
            return;
        }
        //拉取event执行
        log.info("开始执行任务,taskEvent={}", JSON.toJSONString(retryDisruptorTaskEvent.getRetryDisruptorTask()));
        //获取processor
        RetryDisruptorTask retryDisruptorTask = retryDisruptorTaskEvent.getRetryDisruptorTask();
        String metricTaskType = Objects.isNull(retryDisruptorTask.getHandleProcessor())
                ? "UNKNOWN"
                : retryDisruptorTask.getHandleProcessor();
        //不需要检查是否需要检查是否执行完毕-第一次执行，直接执行即可
        if(!retryDisruptorTaskEvent.isShouldCheckUnfinished()){
            execute(retryDisruptorTask);
            return;
        }
        //检查task状态-是否完成、是否超过重试次数
        Long taskId = retryDisruptorTask.getId();
        retryDisruptorTask = retryDisruptorTaskMapper.selectByPrimaryId(taskId);
        if(Objects.isNull(retryDisruptorTask)){
            log.error("this task is null,not expected;任务不存在，id={}",taskId);
            throw new RuntimeException("this task is null,not expected;任务不存在，id:"+taskId);
        }
        //查询是否完成
        boolean isFinished = Objects.equals(retryDisruptorTask.getState(), TaskStateEnum.FINISHED.getValue());
        if(isFinished){
            log.info("任务已完成,task={}",JSON.toJSONString(retryDisruptorTask));
            return;
        }
        //检查是否超过最大重试次数
        if (isOverFailedCountLimit(retryDisruptorTask)) {
            meterRegistry.counter("disruptflow.task.over_limit", "taskType", metricTaskType)
                    .increment();

            try{
                erpService.sendMessage("任务重试失败超过最大次数,请人工干预,task="
                        + JSON.toJSONString(retryDisruptorTask), UserContext.getUserId());
            } catch (Exception e) {
                log.error("执行erp下游服务发送消息失败,task={}",JSON.toJSONString(retryDisruptorTask),e);
            }
            return;
        }
        //条件符合要求，执行任务
        int updatedCount = retryDisruptorTaskMapper.updateToDoing(retryDisruptorTask.getId(), retryDisruptorTask.getVersion());
        if(updatedCount < 1){
            log.error("更新任务状态为正在执行中失败，请检查，id={},version={}",
                    retryDisruptorTask.getId(),retryDisruptorTask.getVersion());
            return;
        }
        //执行任务操作
        execute(retryDisruptorTask);

    }

    /**
     * 检查是否达到最大重试次数
     * @param retryDisruptorTask
     * @return
     */
    private boolean isOverFailedCountLimit(RetryDisruptorTask retryDisruptorTask) {
        int failCount = Objects.isNull(retryDisruptorTask.getFailCount()) ? 0 : retryDisruptorTask.getFailCount();
        return failCount >= retryDisruptorProperties.getFailedCountLimit();
    }

    /**
     * 处理task
     * @param retryDisruptorTask retryDisruptorTask
     */
    private void execute(RetryDisruptorTask retryDisruptorTask) {
        boolean failureMarked = false;
        //获取对应的processors执行
        String handleProcessor = retryDisruptorTask.getHandleProcessor();
        String metricTaskType = Objects.isNull(handleProcessor) ? "UNKNOWN" : handleProcessor;
        TaskProcessor processor;
        try {
            processor = taskProcessorRegistry.getProcessor(handleProcessor);
        } catch (Exception e) {
            markTaskFailed(retryDisruptorTask, "无效处理器类型:" + handleProcessor);
            throw new RuntimeException("无效处理器类型:" + handleProcessor, e);
        }

        try{
            boolean result = processor.execute(retryDisruptorTask);
            if(result){
                RetryDisruptorTask latestTask = retryDisruptorTaskMapper.selectByPrimaryId(retryDisruptorTask.getId());
                int currentVersion = Objects.isNull(latestTask) || Objects.isNull(latestTask.getVersion())
                        ? Objects.requireNonNullElse(retryDisruptorTask.getVersion(), 0)
                        : latestTask.getVersion();
                int updatedCount = retryDisruptorTaskMapper.updateToFinished(
                        retryDisruptorTask.getId(),
                        currentVersion
                );
                if (updatedCount < 1) {
                    throw new RuntimeException("更新任务状态为完成失败,id=" + retryDisruptorTask.getId());
                }
                meterRegistry.counter("disruptflow.task.success", "taskType", metricTaskType).increment();
            }else{
                markTaskFailed(
                        retryDisruptorTask,
                        "任务执行失败，请检查"
                );
                failureMarked = true;
                meterRegistry.counter("disruptflow.task.failed", "taskType", metricTaskType).increment();
                throw new RuntimeException("任务执行失败,id=" + retryDisruptorTask.getId());
            }

        }catch (Exception e){
            log.error("task process failed,task={}",JSON.toJSONString(retryDisruptorTask),e);
            if (!failureMarked) {
                markTaskFailed(
                        retryDisruptorTask,
                        "任务执行失败，发生异常:" + e.getMessage()
                );
                meterRegistry.counter("disruptflow.task.failed", "taskType", metricTaskType).increment();
            }
            throw new RuntimeException(e);
        }
        log.info("task process successfully,task={}",JSON.toJSONString(retryDisruptorTask));

    }

    private void markTaskFailed(RetryDisruptorTask retryDisruptorTask, String reason) {
        String remark = reason + ",task=" + JSON.toJSONString(retryDisruptorTask);
        RetryDisruptorTask latestTask = retryDisruptorTaskMapper.selectByPrimaryId(retryDisruptorTask.getId());
        int currentVersion = Objects.isNull(latestTask) || Objects.isNull(latestTask.getVersion())
                ? Objects.requireNonNullElse(retryDisruptorTask.getVersion(), 0)
                : latestTask.getVersion();
        int updatedCount = retryDisruptorTaskMapper.incrementFailedCount(
                retryDisruptorTask.getId(),
                remark,
                currentVersion
        );
        if (updatedCount < 1) {
            log.error("更新任务失败次数失败,id={},remark={}", retryDisruptorTask.getId(), remark);
        }
    }
}
