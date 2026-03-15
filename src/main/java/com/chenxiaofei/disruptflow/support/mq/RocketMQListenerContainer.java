//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.chenxiaofei.disruptflow.support.mq;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.MethodParameter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SmartMessageConverter;
import org.springframework.messaging.support.MessageBuilder;
import com.chenxiaofei.disruptflow.support.mq.enums.ConsumeMode;
import com.chenxiaofei.disruptflow.support.utils.RocketMQLogTraceUtils;
import com.chenxiaofei.disruptflow.support.utils.RocketMQUtil;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;

public class RocketMQListenerContainer implements InitializingBean, DisposableBean, SmartLifecycle, ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(RocketMQListenerContainer.class);
    private ApplicationContext applicationContext;
    private String nameServer;
    private String consumerGroup;
    private String topic;
    private int delayLevelWhenNextConsume = 0;
    private long suspendCurrentQueueTimeMillis = 1000L;
    private int consumeThreadMax = 64;
    private String charset = "UTF-8";
    private MessageConverter messageConverter;
    private RocketMQConsumer rocketMQConsumer;
    private DefaultMQPushConsumer consumer;
    private Type messageType;
    private MethodParameter methodParameter;
    private boolean running;
    private String tags;
    private MessageModel messageModel;
    private ConsumeMode consumeMode;
    private long consumeTimeout;
    private String clientIp;
    private String consumerBeanName;
    private int reconsumeTimes = -1;

    public String getNameServer() {
        return this.nameServer;
    }

    public void setNameServer(String nameServer) {
        this.nameServer = nameServer;
    }

    public String getConsumerGroup() {
        return this.consumerGroup;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public String getTopic() {
        return this.topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getConsumeThreadMax() {
        return this.consumeThreadMax;
    }

    public String getCharset() {
        return this.charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public MessageConverter getMessageConverter() {
        return this.messageConverter;
    }

    public RocketMQListenerContainer setMessageConverter(MessageConverter messageConverter) {
        this.messageConverter = messageConverter;
        return this;
    }

    public void setRocketMQConsumer(RocketMQConsumer rocketMQConsumer) {
        this.rocketMQConsumer = rocketMQConsumer;
        this.consumerBeanName = rocketMQConsumer.getClass().getSimpleName().split("\\$\\$")[0];
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getTags() {
        return this.tags;
    }

    public DefaultMQPushConsumer getConsumer() {
        return this.consumer;
    }

    public void setConsumer(DefaultMQPushConsumer consumer) {
        this.consumer = consumer;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public void setDelayLevelWhenNextConsume(int delayLevelWhenNextConsume) {
        this.delayLevelWhenNextConsume = delayLevelWhenNextConsume;
    }

    public void setSuspendCurrentQueueTimeMillis(long suspendCurrentQueueTimeMillis) {
        this.suspendCurrentQueueTimeMillis = suspendCurrentQueueTimeMillis;
    }

    public void setConsumeThreadMax(int consumeThreadMax) {
        this.consumeThreadMax = consumeThreadMax;
    }

    public void setMessageType(Type messageType) {
        this.messageType = messageType;
    }

    public void setMethodParameter(MethodParameter methodParameter) {
        this.methodParameter = methodParameter;
    }

    public void setMessageModel(MessageModel messageModel) {
        this.messageModel = messageModel;
    }

    public void setConsumeMode(ConsumeMode consumeMode) {
        this.consumeMode = consumeMode;
    }

    public void setConsumeTimeout(long consumeTimeout) {
        this.consumeTimeout = consumeTimeout;
    }

    public void setConsumerBeanName(String consumerBeanName) {
        this.consumerBeanName = consumerBeanName;
    }

    public void setReconsumeTimes(int reconsumeTimes) {
        this.reconsumeTimes = reconsumeTimes;
    }

    public void destroy() {
        this.setRunning(false);
        if (Objects.nonNull(this.consumer)) {
            this.consumer.shutdown();
        }

        log.info("container destroyed, {}", this.toString());
    }

    public boolean isAutoStartup() {
        return true;
    }

    public void stop(Runnable callback) {
        this.stop();
        callback.run();
    }

    public void start() {
        if (this.isRunning()) {
            throw new IllegalStateException("container already running. " + this.toString());
        } else {
            try {
                this.consumer.start();
            } catch (MQClientException e) {
                throw new IllegalStateException("Failed to start RocketMQ push consumer", e);
            }

            this.setRunning(true);
            log.info("running container: {}", this.toString());
        }
    }

    public void stop() {
        if (this.isRunning()) {
            if (Objects.nonNull(this.consumer)) {
                this.consumer.shutdown();
            }

            this.setRunning(false);
        }

    }

    public boolean isRunning() {
        return this.running;
    }

    private void setRunning(boolean running) {
        this.running = running;
    }

    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    public void afterPropertiesSet() throws Exception {
        this.initRocketMQPushConsumer();
        this.messageType = this.getMessageType();
        this.methodParameter = this.getMethodParameter();
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private Object doConvertMessage(MessageExt messageExt) {
        if (Objects.equals(this.messageType, MessageExt.class)) {
            return messageExt;
        } else {
            String str = new String(messageExt.getBody(), Charset.forName(this.charset));
            if (Objects.equals(this.messageType, String.class)) {
                return str;
            } else {
                try {
                    return this.messageType instanceof Class ? this.getMessageConverter().fromMessage(MessageBuilder.withPayload(str).build(), (Class)this.messageType) : ((SmartMessageConverter)this.getMessageConverter()).fromMessage(MessageBuilder.withPayload(str).build(), (Class)((ParameterizedType)this.messageType).getRawType(), this.methodParameter);
                } catch (Exception e) {
                    log.info("convert failed. str:{}, msgType:{}", str, this.messageType);
                    throw new RuntimeException("cannot convert message to " + this.messageType, e);
                }
            }
        }
    }

    private MethodParameter getMethodParameter() {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(this.rocketMQConsumer);
        Type messageType = this.getMessageType();
        Class clazz = null;
        if (messageType instanceof ParameterizedType && this.messageConverter instanceof SmartMessageConverter) {
            clazz = (Class)((ParameterizedType)messageType).getRawType();
        } else {
            if (!(messageType instanceof Class)) {
                throw new RuntimeException("parameterType:" + messageType + " of onMessage method is not supported");
            }

            clazz = (Class)messageType;
        }

        try {
            Method method = targetClass.getMethod("onMessage", clazz);
            return new MethodParameter(method, 0);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException("parameterType:" + messageType + " of onMessage method is not supported");
        }
    }

    private Type getMessageType() {
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(this.rocketMQConsumer);

        Type matchedGenericInterface;
        for(matchedGenericInterface = null; Objects.nonNull(targetClass); targetClass = targetClass.getSuperclass()) {
            Type[] interfaces = targetClass.getGenericInterfaces();
            if (Objects.nonNull(interfaces)) {
                for(Type type : interfaces) {
                    if (type instanceof ParameterizedType && Objects.equals(((ParameterizedType)type).getRawType(), RocketMQConsumer.class)) {
                        matchedGenericInterface = type;
                        break;
                    }
                }
            }
        }

        if (Objects.isNull(matchedGenericInterface)) {
            return Object.class;
        } else {
            Type[] actualTypeArguments = ((ParameterizedType)matchedGenericInterface).getActualTypeArguments();
            if (Objects.nonNull(actualTypeArguments) && actualTypeArguments.length > 0) {
                return actualTypeArguments[0];
            } else {
                return Object.class;
            }
        }
    }

    private void initRocketMQPushConsumer() throws MQClientException {
        this.consumer = new DefaultMQPushConsumer(this.consumerGroup);
        this.consumer.setVipChannelEnabled(false);
        this.consumer.setInstanceName(RocketMQUtil.getInstanceName());
        this.consumer.setNamesrvAddr(this.nameServer);
        this.consumer.setConsumeThreadMax(this.consumeThreadMax);
        this.consumer.setConsumeTimeout(this.consumeTimeout);
        this.consumer.subscribe(this.topic, this.tags);
        this.consumer.setMessageModel(this.messageModel);
        this.consumer.setMaxReconsumeTimes(this.reconsumeTimes);
        if (StringUtils.isNotBlank(this.clientIp)) {
            this.consumer.setClientIP(this.clientIp);
        }

        switch (this.consumeMode) {
            case ORDERLY:
                this.consumer.setMessageListener(new DefaultMessageListenerOrderly());
                break;
            case CONCURRENTLY:
                this.consumer.setMessageListener(new DefaultMessageListenerConcurrently());
        }

    }

    public class DefaultMessageListenerOrderly implements MessageListenerOrderly {
        public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs, ConsumeOrderlyContext context) {
            for(MessageExt messageExt : msgs) {
                ConsumeOrderlyStatus var6;
                try {
                    RocketMQLogTraceUtils.setTraceId(messageExt.getMsgId());
                    RocketMQListenerContainer.log.info("{}: 收到消息, msgId: {}, topic: {}, tags: {}, keys: {}, 第{}次消费", new Object[]{RocketMQListenerContainer.this.consumerBeanName, messageExt.getMsgId(), messageExt.getTopic(), messageExt.getTags(), messageExt.getKeys(), messageExt.getReconsumeTimes() + 1});
                    long now = System.currentTimeMillis();
                    RocketMQListenerContainer.this.rocketMQConsumer.onMessage(RocketMQListenerContainer.this.doConvertMessage(messageExt));
                    long costTime = System.currentTimeMillis() - now;
                    RocketMQListenerContainer.log.info("{}: 消息消费完成, msgId: {}, cost: {}ms", new Object[]{RocketMQListenerContainer.this.consumerBeanName, messageExt.getMsgId(), costTime});
                    continue;
                } catch (Exception e) {
                    RocketMQListenerContainer.log.error("{}: 消息消费失败. msgId: {}, topic: {}, error: {}", new Object[]{RocketMQListenerContainer.this.consumerBeanName, messageExt.getMsgId(), messageExt.getTopic(), e.getMessage(), e});
                    context.setSuspendCurrentQueueTimeMillis(RocketMQListenerContainer.this.suspendCurrentQueueTimeMillis);
                    var6 = ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
                } finally {
                    RocketMQLogTraceUtils.removeTraceId();
                }

                return var6;
            }

            return ConsumeOrderlyStatus.SUCCESS;
        }
    }

    public class DefaultMessageListenerConcurrently implements MessageListenerConcurrently {
        public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
            for(MessageExt messageExt : msgs) {
                RocketMQLogTraceUtils.setTraceId(messageExt.getMsgId());
                RocketMQListenerContainer.log.info("{}: 收到消息, msgId: {}, topic: {}, tags: {}, keys: {}, 第{}次消费", new Object[]{RocketMQListenerContainer.this.consumerBeanName, messageExt.getMsgId(), messageExt.getTopic(), messageExt.getTags(), messageExt.getKeys(), messageExt.getReconsumeTimes() + 1});

                ConsumeConcurrentlyStatus var6;
                try {
                    long now = System.currentTimeMillis();
                    RocketMQListenerContainer.this.rocketMQConsumer.onMessage(RocketMQListenerContainer.this.doConvertMessage(messageExt));
                    long costTime = System.currentTimeMillis() - now;
                    RocketMQListenerContainer.log.info("{}: 消息消费完成, msgId: {}, cost: {}ms", new Object[]{RocketMQListenerContainer.this.consumerBeanName, messageExt.getMsgId(), costTime});
                    continue;
                } catch (Exception e) {
                    RocketMQListenerContainer.log.error("{}: 消息消费失败, msgId: {}, topic: {}, error: {}", new Object[]{RocketMQListenerContainer.this.consumerBeanName, messageExt.getMsgId(), messageExt.getTopic(), e.getMessage(), e});
                    context.setDelayLevelWhenNextConsume(RocketMQListenerContainer.this.delayLevelWhenNextConsume);
                    var6 = ConsumeConcurrentlyStatus.RECONSUME_LATER;
                } finally {
                    RocketMQLogTraceUtils.removeTraceId();
                }

                return var6;
            }

            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        }
    }
}