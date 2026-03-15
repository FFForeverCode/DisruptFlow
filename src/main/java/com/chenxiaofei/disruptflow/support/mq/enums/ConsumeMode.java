package com.chenxiaofei.disruptflow.support.mq.enums;

/**
 * @author chenxiaofei
 * @description: 消费模式
 */
public enum ConsumeMode {
    /**
     * 顺序消费
     */
    ORDERLY,
    
    /**
     * 并发消费
     */
    CONCURRENTLY
}
