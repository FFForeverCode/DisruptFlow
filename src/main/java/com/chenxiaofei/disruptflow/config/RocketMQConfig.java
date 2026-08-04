package com.chenxiaofei.disruptflow.config;

import com.chenxiaofei.disruptflow.mq.RocketMQConsumer;
import com.chenxiaofei.disruptflow.mq.RocketMQListenerContainer;
import com.chenxiaofei.disruptflow.mq.RocketMQProducer;
import com.chenxiaofei.disruptflow.mq.ann.RocketMQListener;
import com.chenxiaofei.disruptflow.mq.enums.ConsumeMode;
import com.chenxiaofei.disruptflow.mq.exception.RocketMQConfigException;
import com.chenxiaofei.disruptflow.mq.model.RocketmqProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author chenxiaofei
 * @description: RocketMQ 配置类
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(RocketmqProperties.class)
@ConditionalOnProperty(value = "rocketmq.enabled", havingValue = "true", matchIfMissing = true)
public class RocketMQConfig implements ApplicationContextAware {

    @Autowired
    private RocketmqProperties rocketmqProperties;

    private ApplicationContext applicationContext;

    /**
     * 创建 RocketMQ 生产者
     */
    @Bean
    public DefaultMQProducer defaultMQProducer() {
        if (StringUtils.isEmpty(rocketmqProperties.getNameServer())) {
            throw new RocketMQConfigException("NameServer 地址未配置");
        }
        
        DefaultMQProducer producer = new DefaultMQProducer(rocketmqProperties.getProducerGroup());
        producer.setNamesrvAddr(rocketmqProperties.getNameServer());
        producer.setSendMsgTimeout(rocketmqProperties.getSendMsgTimeout());
        producer.setRetryTimesWhenSendFailed(rocketmqProperties.getRetryTimesWhenSendFailed());
        producer.setRetryTimesWhenSendAsyncFailed(rocketmqProperties.getRetryTimesWhenSendAsyncFailed());
        producer.setMaxMessageSize(rocketmqProperties.getMaxMessageSize());
        producer.setCompressMsgBodyOverHowmuch(rocketmqProperties.getCompressMsgBodyOverHowmuch());
        producer.setRetryAnotherBrokerWhenNotStoreOK(rocketmqProperties.isRetryAnotherBrokerWhenNotStoreOK());

        try {
            producer.start();
            log.info("RocketMQ Producer 启动成功, NameServer: {}", rocketmqProperties.getNameServer());
        } catch (Exception e) {
            log.error("RocketMQ Producer 启动失败", e);
            throw new RocketMQConfigException("RocketMQ Producer 启动失败", e);
        }

        return producer;
    }

    /**
     * 创建 RocketMQ 生产者服务
     */
    @Bean
    public RocketMQProducer rocketMQProducer(StandardEnvironment environment, DefaultMQProducer defaultMQProducer) {
        return new RocketMQProducer(environment, defaultMQProducer);
    }

    /**
     * 消息转换器
     */
    @Bean
    public MessageConverter rocketMQMessageConverter() {
        List<MessageConverter> messageConverters = new ArrayList<>();
        messageConverters.add(new MappingJackson2MessageConverter());
        return new CompositeMessageConverter(messageConverters);
    }

    /**
     * 扫描并注册所有 @RocketMQListener 注解的消费者
     */
    @Bean
    @DependsOn("defaultMQProducer")
    public void registerRocketMQListeners() throws Exception {
        Map<String, Object> beans = applicationContext.getBeansWithAnnotation(RocketMQListener.class);
        
        if (beans.isEmpty()) {
            log.info("未找到 RocketMQ 消费者");
            return;
        }

        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            String beanName = entry.getKey();
            Object bean = entry.getValue();
            
            Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
            RocketMQListener annotation = targetClass.getAnnotation(RocketMQListener.class);
            
            if (annotation == null || !(bean instanceof RocketMQConsumer)) {
                log.warn("Bean {} 不是 RocketMQConsumer 类型或缺少 @RocketMQListener 注解", beanName);
                continue;
            }

            try {
                registerListenerContainer(beanName, bean, annotation);
            } catch (Exception e) {
                log.error("注册 RocketMQ 消费者失败: {}", beanName, e);
                throw e;
            }
        }
    }

    /**
     * 注册单个消费者容器
     */
    private void registerListenerContainer(String beanName, Object bean, RocketMQListener annotation) throws Exception {
        RocketMQListenerContainer container = new RocketMQListenerContainer();
        
        String consumerGroup = getConsumerGroup(annotation);
        String topic = annotation.topic();
        String tags = annotation.tags();
        
        if (StringUtils.isEmpty(topic)) {
            throw new RocketMQConfigException("Topic 不能为空");
        }
        
        // 基础配置
        container.setNameServer(rocketmqProperties.getNameServer());
        container.setConsumerGroup(consumerGroup);
        container.setTopic(topic);
        container.setTags(StringUtils.isEmpty(tags) ? "*" : tags);
        
        // 消费配置
        container.setConsumeMode(convertConsumeMode(annotation.consumeMode()));
        container.setMessageModel(convertMessageModel(annotation.messageModel()));
        container.setReconsumeTimes(annotation.reconsumeTimes());
        container.setConsumeTimeout(annotation.consumeTimeout());
        
        // 消费者实例
        container.setRocketMQConsumer((RocketMQConsumer) bean);
        
        // 初始化并启动
        container.afterPropertiesSet();
        container.start();
        
        log.info("RocketMQ 消费者注册成功: group={}, topic={}, tags={}, bean={}", 
                consumerGroup, topic, tags, beanName);
    }

    /**
     * 获取消费者组名
     */
    private String getConsumerGroup(RocketMQListener annotation) {
        String consumerGroup = annotation.consumerGroup();
        String consumerPrefix = annotation.consumerPrefix();
        
        if (StringUtils.isNotEmpty(consumerGroup)) {
            return consumerGroup;
        }
        
        if (StringUtils.isNotEmpty(consumerPrefix)) {
            return consumerPrefix + "_CONSUMER_GROUP";
        }
        
        return rocketmqProperties.getConsumerGroup();
    }
    
    /**
     * 转换消费模式
     */
    private ConsumeMode convertConsumeMode(
            Object springConsumeMode) {
        if (springConsumeMode != null && springConsumeMode.toString().equals("ORDERLY")) {
            return ConsumeMode.ORDERLY;
        }
        return ConsumeMode.CONCURRENTLY;
    }
    
    /**
     * 转换消息模型
     */
    private org.apache.rocketmq.common.protocol.heartbeat.MessageModel convertMessageModel(
            Object springMessageModel) {
        if (springMessageModel != null && springMessageModel.toString().equals("BROADCASTING")) {
            return org.apache.rocketmq.common.protocol.heartbeat.MessageModel.BROADCASTING;
        }
        return org.apache.rocketmq.common.protocol.heartbeat.MessageModel.CLUSTERING;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
