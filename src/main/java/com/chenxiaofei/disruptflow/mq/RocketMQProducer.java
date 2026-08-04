package com.chenxiaofei.disruptflow.mq;

import com.chenxiaofei.disruptflow.mq.model.MessageBuild;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;
import org.springframework.util.ClassUtils;

import java.util.Objects;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description:
 */
@Slf4j
@Service
public class RocketMQProducer implements InitializingBean, DisposableBean {


    private DefaultMQProducer defaultMQProducer;

    private Environment environment;

    public RocketMQProducer(Environment environment, DefaultMQProducer defaultMQProducer) {
        this.environment = environment;
        this.defaultMQProducer = defaultMQProducer;
    }
    @Override
    public void destroy() throws Exception {
        if (Objects.nonNull(defaultMQProducer)) {
            this.defaultMQProducer.shutdown();
            log.info("RocketMQProducer shutdown success");
        }
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
        message.setBody(messageBuild.getBody().getBytes());
        message.setDelayTimeLevel(messageBuild.getDelayLevel().level);
        return message;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        ClassLoader classLoader = RocketMQProducer.class.getClassLoader();
        if(Objects.nonNull(this.defaultMQProducer) && !ClassUtils.isPresent(
                "org.apache.rocketmq.spring.core.RocketMQTemplate",classLoader)){
            log.info("RocketMQTemplate is not exist,please check,default-MQ-Producer will not start");
            this.defaultMQProducer.start();
        }
    }

    public String toString(Message message){
        return String.format("Message [topic=%s, tags=%s, keys=%s, body=%s, delayLevel=%d]",
                message.getTopic(), message.getTags(), message.getKeys(),
                new String(message.getBody()), message.getDelayTimeLevel());
    }
}
