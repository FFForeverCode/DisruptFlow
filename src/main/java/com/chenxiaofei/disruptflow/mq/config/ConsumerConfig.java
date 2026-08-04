//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.chenxiaofei.disruptflow.mq.config;

import com.chenxiaofei.disruptflow.mq.enums.DelayLevel;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.apache.rocketmq.spring.annotation.ConsumeMode;

public class ConsumerConfig {
    private String consumerGroup;
    private String topic;
    private String tags;
    private MessageModel messageModel;
    private ConsumeMode consumeMode;
    private DelayLevel reconsumeDelayLevel;
    private Integer reconsumeTimes;
    private Long consumeTimeout;

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

    public String getTags() {
        return this.tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public MessageModel getMessageModel() {
        return this.messageModel;
    }

    public void setMessageModel(String messageModel) {
        this.messageModel = MessageModel.valueOf(messageModel);
    }

    public void setMessageModel(MessageModel messageModel) {
        this.messageModel = messageModel;
    }

    public ConsumeMode getConsumeMode() {
        return this.consumeMode;
    }

    public void setConsumeMode(String consumeMode) {
        this.consumeMode = ConsumeMode.valueOf(consumeMode);
    }

    public void setConsumeMode(ConsumeMode consumeMode) {
        this.consumeMode = consumeMode;
    }

    public DelayLevel getReconsumeDelayLevel() {
        return this.reconsumeDelayLevel;
    }

    public void setReconsumeDelayLevel(String reconsumeDelayLevel) {
        this.reconsumeDelayLevel = DelayLevel.valueOf(reconsumeDelayLevel);
    }

    public void setReconsumeDelayLevel(DelayLevel reconsumeDelayLevel) {
        this.reconsumeDelayLevel = reconsumeDelayLevel;
    }

    public Integer getReconsumeTimes() {
        return this.reconsumeTimes;
    }

    public void setReconsumeTimes(Integer reconsumeTimes) {
        this.reconsumeTimes = reconsumeTimes;
    }

    public Long getConsumeTimeout() {
        return this.consumeTimeout;
    }

    public void setConsumeTimeout(Long consumeTimeout) {
        this.consumeTimeout = consumeTimeout;
    }
}