package com.chenxiaofei.disruptflow.mq;

import com.chenxiaofei.disruptflow.mq.model.MessageBuild;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

import java.nio.charset.StandardCharsets;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description:
 */
@Slf4j
public class RocketMQProducer {


    private final DefaultMQProducer defaultMQProducer;

    public RocketMQProducer(DefaultMQProducer defaultMQProducer) {
        this.defaultMQProducer = defaultMQProducer;
    }

    public final SendResult send(MessageBuild messageBuild){
        return this.defaultSend(messageBuild,(String)null);
    }

    public final SendResult asyncSend(MessageBuild messageBuild, SendCallback sendCallback){
        try{
            Message message = this.converMessage(messageBuild);
            log.info("RocketMQ async 消息发送,message{}",this.toString(message));
            this.defaultMQProducer.send(message,sendCallback);
            return null;
        }catch (Exception e){
            throw new RuntimeException("RocketMQ 消息发送失败，topic:"+messageBuild.getTopic(),e);
        }
    }

    private final SendResult defaultSend(MessageBuild messageBuild, String hashKey) {
        try{
            Message message = this.converMessage(messageBuild);
            log.info("RocketMQ defaultSend 消息发送,message{}",this.toString(message));
            SendResult result;
            if(StringUtils.isNotBlank(hashKey)){
                result = this.defaultMQProducer.send(message,(mqs,msg,arg)->{
                    int value = arg.hashCode();
                    if(value < 0){
                        value = Math.abs(value);
                    }
                    value %= mqs.size();
                    return (MessageQueue)mqs.get(value);
                },hashKey);
            }else{
                result = this.defaultMQProducer.send(message);
            }
            log.info("RocketMQ 消息发送，result =msgId:{},SendResult {}",result.getMsgId(),result.getSendStatus());
            if(result.getSendStatus() != SendStatus.SEND_OK){
                log.error("RocketMQ 消息发送失败，result =msgId:{},SendResult {}",result.getMsgId(),result.getSendStatus());
            }
            return result;
        }catch (Exception e){
            throw new RuntimeException("RocketMQ 消息发送失败，topic:"+messageBuild.getTopic(),e);
        }
    }

    private Message converMessage(MessageBuild messageBuild) {
        Message message = new Message();
        message.setTopic(messageBuild.getTopic());
        message.setTags(messageBuild.getTags());
        message.setKeys(messageBuild.getKeys());
        message.setBody(messageBuild.getBody().getBytes(StandardCharsets.UTF_8));
        message.setDelayTimeLevel(messageBuild.getDelayLevel().level);
        return message;
    }

    public String toString(Message message){
        return String.format("Message [topic=%s, tags=%s, keys=%s, body=%s, delayLevel=%d]",
                message.getTopic(), message.getTags(), message.getKeys(),
                new String(message.getBody()), message.getDelayTimeLevel());
    }
}
