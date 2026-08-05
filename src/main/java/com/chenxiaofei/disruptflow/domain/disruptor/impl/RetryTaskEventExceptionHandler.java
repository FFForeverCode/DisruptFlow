package com.chenxiaofei.disruptflow.domain.disruptor.impl;


import com.alibaba.fastjson.JSON;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTask;
import com.chenxiaofei.disruptflow.model.RetryDisruptorTaskEvent;
import com.chenxiaofei.disruptflow.repository.mapper.RetryDisruptorTaskMapper;
import com.chenxiaofei.disruptflow.support.mq.RocketMQProducer;
import com.chenxiaofei.disruptflow.support.mq.enums.DelayLevel;
import com.chenxiaofei.disruptflow.support.mq.model.MessageBuild;
import com.chenxiaofei.disruptflow.support.mq.model.RocketmqProperties;
import com.lmax.disruptor.ExceptionHandler;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.springframework.stereotype.Service;
import java.util.Objects;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description: event异常处理器
 */
@Slf4j
@Service
public class RetryTaskEventExceptionHandler implements ExceptionHandler<RetryDisruptorTaskEvent> {



    @Resource
    private RocketmqProperties rocketMQProperties;
    @Resource
    private RetryDisruptorTaskMapper retryDisruptorTaskMapper;

    @Resource
    private RocketMQProducer rocketMqProducer;
    @Override
    public void handleEventException(Throwable ex, long sequence, RetryDisruptorTaskEvent event) {
        log.info("执行任务发生异常,进行异常处理,sequence={},event={}",sequence,event);
        if(Objects.isNull(event) || Objects.isNull(event.getRetryDisruptorTask())){
            log.warn("event或task为null,无法进行异常处理,sequence={}",sequence);
            return;
        }
        RetryDisruptorTask retryDisruptorTask = retryDisruptorTaskMapper.selectByPrimaryId(event.getRetryDisruptorTask()
                .getId());
        if(Objects.isNull(retryDisruptorTask)){
            log.warn("task不存在,无法进行异常处理,sequence={},taskId={}",sequence,event.getRetryDisruptorTask().getId());
            throw new RuntimeException("task不存在,无法进行异常处理,sequence="
                    +sequence+",taskId="+event.getRetryDisruptorTask().getId());
        }
        event.setRetryDisruptorTask(retryDisruptorTask);
        event.setShouldCheckUnfinished(true);
        int failCount = Objects.isNull(retryDisruptorTask.getFailCount()) ? 0 : retryDisruptorTask.getFailCount();
        rocketMqProducer.asyncSend(
                MessageBuild.builder()
                        .topic(rocketMQProperties.getProducer().getRetryDisruptorTopic())
                        .tags(rocketMQProperties.getProducer().getRetryDisruptorTags())
                        .body(JSON.toJSONString(event))
                        .delayLevel(DelayLevel.getLevel(failCount + 1))
                        .build(),
                new SendCallback() {
                    @Override
                    public void onSuccess(SendResult sendResult) {
                        log.info("com.chenxiaofei.disruptorflow.domain.disruptor.impl.RetryTaskEventExceptionHandler.handleEventException" +
                                "异常event处理成功 event{}",event);
                    }

                    @Override
                    public void onException(Throwable throwable) {
                        log.error("com.chenxiaofei.disruptorflow.domain.disruptor.impl.RetryTaskEventExceptionHandler.handleEventException" +
                                "异常event处理失败 event{} message{}",event,throwable.getMessage());

                    }
                }
        );

    }

    @Override
    public void handleOnStartException(Throwable ex) {

    }

    @Override
    public void handleOnShutdownException(Throwable ex) {

    }
}
