package com.chenxiaofei.disruptflow.domain.disruptor.impl;

import com.chenxiaofei.disruptflow.domain.disruptor.RetryDisruptorTaskFlowService;
import com.chenxiaofei.disruptflow.domain.transaction.impl.TransactionCallBackService;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTaskEvent;
import com.chenxiaofei.disruptflow.repository.RetryDisruptorTaskRepository;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description: 任务推送具体实现
 */
@Slf4j
@Service
public class RetryDisruptorTaskFlowServiceImpl implements RetryDisruptorTaskFlowService {


    @Resource
    private RetryDisruptorEventPusher retryDisruptorEventPusher;

    @Resource
    private RetryDisruptorTaskRepository retryDisruptorTaskRepository;

    @Resource
    private TransactionCallBackService transactionCallBackService;


    @Override
    @Async("asyncPushDisruptorFlowExecutor")
    public void pushDisruptorFlow(List<RetryDisruptorTask> retryDisruptorTasks) {
        if(CollectionUtils.isEmpty(retryDisruptorTasks)){
            log.warn("任务列表为空,无需推送");
            return;
        }
        log.info("准备推送任务，size={}",retryDisruptorTasks.size());
        //持久化消息体
        int count = retryDisruptorTaskRepository.insertBatch(retryDisruptorTasks);
        if(count <= 0){
            log.info("插入任务失败 com.chenxiaofei.disruptorflow.domain.disruptor.impl.RetryDisruptorTaskFlowServiceImpl.pushDisruptorFlow " +
                    "size={}",retryDisruptorTasks.size());
            return;
        }
        transactionCallBackService.execute(()->{
            log.info("插入任务表事务执行成功，执行推送回调");
            retryDisruptorTasks.stream()
                    .map(retryDisruptorTask -> new RetryDisruptorTaskEvent(retryDisruptorTask,true))
                    .forEach(retryDisruptorEventPusher::pushEvent2Disruptor);
        });
    }
}
