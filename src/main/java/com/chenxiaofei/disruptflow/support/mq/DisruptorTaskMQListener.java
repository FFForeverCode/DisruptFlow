package com.chenxiaofei.disruptflow.support.mq;

import com.alibaba.fastjson.JSON;
import com.chenxiaofei.disruptorflow.domain.disruptor.impl.RetryDisruptorEventPusher;
import com.chenxiaofei.disruptorflow.model.RetryDisruptorTaskEvent;
import com.chenxiaofei.disruptorflow.support.mq.ann.RocketMQListener;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description:
 */
@Slf4j
@Service
@RocketMQListener(consumerPrefix = "rocketmq.consumer.retryDisruptorTask")
public class DisruptorTaskMQListener implements RocketMQConsumer<MessageExt>{


    @Resource
    private RetryDisruptorEventPusher retryDisruptorEventPusher;
    @Override
    public void onMessage(MessageExt message) {
        if(Objects.isNull(message) || Objects.isNull(message.getBody())){
            log.info("message is null and the body of message is null");
            return;
        }
        try{
            String msg = new String(message.getBody());
            log.info("收到RocketMQ消息,准备推送到Disruptor,topic={},tags={},msg={}",
                    message.getTopic(),message.getTags(),msg);
            RetryDisruptorTaskEvent retryDisruptorTaskEvent = JSON.parseObject(msg, RetryDisruptorTaskEvent.class);
            retryDisruptorEventPusher.pushEvent2Disruptor(retryDisruptorTaskEvent);
        }catch (Exception e){
            log.error("RocketMQ消息处理失败,topic={},tags={},msg={}",
                    message.getTopic(),message.getTags(),new String(message.getBody()),e);
            throw new RuntimeException("RocketMQ消息处理失败,topic="+
                    message.getTopic()+",tags="+message.getTags(),e);
        }
    }
}
