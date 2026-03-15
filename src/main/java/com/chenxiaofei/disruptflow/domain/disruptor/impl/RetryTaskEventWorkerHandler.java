package com.chenxiaofei.disruptflow.domain.disruptor.impl;

import com.alibaba.fastjson.JSON;
import com.chenxiaofei.disruptorflow.domain.erp.ErpService;
import com.chenxiaofei.disruptorflow.domain.processors.TaskProcessor;
import com.chenxiaofei.disruptorflow.model.RetryDisruptorTask;
import com.chenxiaofei.disruptorflow.model.RetryDisruptorTaskEvent;
import com.chenxiaofei.disruptorflow.model.enums.RetryDisruptorTaskEnum;
import com.chenxiaofei.disruptorflow.model.enums.TaskStateEnum;
import com.chenxiaofei.disruptorflow.repository.mapper.RetryDisruptorTaskMapper;
import com.chenxiaofei.disruptorflow.support.utils.UserContext;
import com.lmax.disruptor.WorkHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description: event工作线程
 */
@Slf4j
@Service
public class RetryTaskEventWorkerHandler  implements WorkHandler<RetryDisruptorTaskEvent>, ApplicationContextAware {

    private ApplicationContext applicationContext;


    @Resource
    private RetryDisruptorTaskMapper retryDisruptorTaskMapper;

    @Resource
    private ErpService erpService;


    @Value("${retry.failed.count.limit}")
    private Integer failedCountLimit = 3;




    @Override
    public void onEvent(RetryDisruptorTaskEvent retryDisruptorTaskEvent) throws Exception {
        //拉取event执行
        log.info("开始执行任务,taskEvent={}", JSON.toJSONString(retryDisruptorTaskEvent.getRetryDisruptorTask()));
        //获取processor
        RetryDisruptorTask retryDisruptorTask = retryDisruptorTaskEvent.getRetryDisruptorTask();
        //不需要检查是否需要检查是否执行完毕-第一次执行，直接执行即可
        if(!retryDisruptorTaskEvent.getShouldCheckUnfinished()){
            execute(retryDisruptorTask);
            return;
        }
        //检查task状态-是否完成、是否超过重试次数
        retryDisruptorTask = retryDisruptorTaskMapper.selectByPrimaryId(retryDisruptorTask.getId());
        if(Objects.isNull(retryDisruptorTask)){
            log.error("this task is null,not expected;任务不存在，id={}",retryDisruptorTask.getId());
            throw new RuntimeException("this task is null,not expected;任务不存在，id:"+retryDisruptorTask.getId());
        }
        //查询是否完成
        boolean isFinished = Objects.equals(retryDisruptorTask.getState(), TaskStateEnum.FINISHED.getValue());
        if(isFinished){
            log.info("任务已完成,task={}",JSON.toJSONString(retryDisruptorTask));
            return;
        }
        //检查是否超过最大重试次数
        if (isOverFailedCountLimit(retryDisruptorTask)) {

            try{
                erpService.sendMessage("任务重试失败超过最大次数,请人工干预,task="
                        + JSON.toJSONString(retryDisruptorTask), UserContext.getUserId());
            } catch (Exception e) {
                log.error("执行erp下游服务发送消息失败,task={}",JSON.toJSONString(retryDisruptorTask),e);
                throw new RuntimeException(e);
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
        return retryDisruptorTask.getFailCount() >= failedCountLimit;
    }

    /**
     * 处理task
     * @param retryDisruptorTask retryDisruptorTask
     */
    private void execute(RetryDisruptorTask retryDisruptorTask) {
        //获取对应的processors执行
        String handleProcessor = retryDisruptorTask.getHandleProcessor();
        RetryDisruptorTaskEnum retryDisruptorTaskEnum = RetryDisruptorTaskEnum.valueOf(handleProcessor);
        String processorName = retryDisruptorTaskEnum.getBeanName();
        TaskProcessor processor = (TaskProcessor) applicationContext.getBean(processorName);
        if(Objects.isNull(processor)){
            log.error("processor is null,not expected;processor不存在，processorName={}",processorName);
            throw new RuntimeException("processor is null,not expected;processor不存在，" +
                    "processorName:" +processorName);
        }
        try{
            boolean result = processor.execute(retryDisruptorTask);
            if(result){
                log.info("task process successfully");
                int count = retryDisruptorTaskMapper.updateToFinished(
                        retryDisruptorTask.getId(),
                        retryDisruptorTask.getVersion()
                );
            }else{
                log.info("task process failed");
                int count = retryDisruptorTaskMapper.incrementFailedCount(
                        retryDisruptorTask.getId(),
                        "任务执行失败，请检查，task=" + JSON.toJSONString(retryDisruptorTask),
                        retryDisruptorTask.getVersion()
                );
                throw new RuntimeException("com.chenxiaofei.disruptorflow.domain.disruptor.impl" +
                        ".RetryTaskEventWorkerHandler.execute retryTask failed");
            }

        }catch (Exception e){
            log.error("task process failed,task={}",JSON.toJSONString(retryDisruptorTask),e);
            int count = retryDisruptorTaskMapper.incrementFailedCount(
                    retryDisruptorTask.getId(),
                    "任务执行失败，发生异常，异常信息:" + e.getMessage() + "，请检查，task=" + JSON.toJSONString(retryDisruptorTask),
                    retryDisruptorTask.getVersion()
            );
             throw new RuntimeException(e);
        }
        log.info("task process successfully,task={}",JSON.toJSONString(retryDisruptorTask));

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
