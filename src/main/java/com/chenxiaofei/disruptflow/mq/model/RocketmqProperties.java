package com.chenxiaofei.disruptflow.mq.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description: RocketMQ 配置属性
 */
@ConfigurationProperties(prefix = "rocketmq")
@Data
public class RocketmqProperties {

    /**
     * 是否启用 RocketMQ
     */
    private boolean enabled = true;

    /**
     * NameServer 地址
     */
    private String nameServer;

    /**
     * 生产者组名
     */
    private String producerGroup = "DEFAULT_PRODUCER_GROUP";

    /**
     * 消费者组名
     */
    private String consumerGroup = "DEFAULT_CONSUMER_GROUP";

    /**
     * 发送消息超时时间（毫秒）
     */
    private int sendMsgTimeout = 3000;

    /**
     * 同步发送失败重试次数
     */
    private int retryTimesWhenSendFailed = 2;

    /**
     * 异步发送失败重试次数
     */
    private int retryTimesWhenSendAsyncFailed = 2;

    /**
     * 消息最大大小（字节）
     */
    private int maxMessageSize = 1024 * 1024 * 4;

    /**
     * 消息体压缩阈值（字节）
     */
    private int compressMsgBodyOverHowmuch = 1024 * 4;

    /**
     * 发送失败时是否重试其他 Broker
     */
    private boolean retryAnotherBrokerWhenNotStoreOK = false;

    /**
     * 生产者配置
     */
    private Producer producer = new Producer();

    @Data
    public static class Producer {
        private String retryDisruptorTopic;
        private String retryDisruptorTags;
    }
}
