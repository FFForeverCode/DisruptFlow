package com.chenxiaofei.disruptflow.mq;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description:
 */
public interface RocketMQConsumer <T>{
    void onMessage(T var);
}
