package com.chenxiaofei.disruptorflow.domain.disruptor.impl;

import com.chenxiaofei.disruptorflow.model.RetryDisruptorTaskEvent;
import com.lmax.disruptor.dsl.Disruptor;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-16
 * @description: event 推送服务
 */
@Service
public class RetryDisruptorEventPusher {

    @Resource
    private Disruptor<RetryDisruptorTaskEvent>disruptor;

    /**
     * 将event推送至环形队列中
     * @param event
     */
    public void pushEvent2Disruptor(RetryDisruptorTaskEvent event){
        disruptor.publishEvent(
                (eventObj,seq)->{
                    eventObj.setRetryDisruptorTask(event.getRetryDisruptorTask());
                    eventObj.setShouldCheckUnfinished(event.isShouldCheckUnfinished());
                }
        );
    }
}
