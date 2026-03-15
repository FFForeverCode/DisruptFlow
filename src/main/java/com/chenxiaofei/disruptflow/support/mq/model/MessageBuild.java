package com.chenxiaofei.disruptflow.support.mq.model;

import com.chenxiaofei.disruptflow.support.mq.enums.DelayLevel;
import lombok.Builder;
import lombok.Getter;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description:
 */

@Getter
@Builder
public class MessageBuild {

    private String topic;

    private String tags;

    private String keys;

    private DelayLevel delayLevel;

    private String body;

    public MessageBuild(){
        this.delayLevel= DelayLevel.NO;
    }

    public MessageBuild topic(String topic) {
        this.topic = topic;
        return this;
    }
    public MessageBuild tags(String tags) {
        this.tags = tags;
        return this;
    }
    public MessageBuild keys(String keys) {
        this.keys = keys;
        return this;
    }
    public MessageBuild delay(DelayLevel delayLevel) {
        this.delayLevel = delayLevel;
        return this;
    }
    public MessageBuild body(String body) {
        this.body = body;
        return this;
    }
}
