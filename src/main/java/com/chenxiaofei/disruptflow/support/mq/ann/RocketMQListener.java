package com.chenxiaofei.disruptflow.support.mq.ann;

import com.chenxiaofei.disruptflow.support.mq.enums.DelayLevel;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.MessageModel;

import java.lang.annotation.*;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description:
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RocketMQListener {
    String consumerPrefix() default "";


    String consumerGroup() default "";

    String topic() default "";

    String tags() default "";


    MessageModel messageModel() default MessageModel.CLUSTERING;

    ConsumeMode consumeMode() default ConsumeMode.CONCURRENTLY;

    DelayLevel delayLevel() default DelayLevel.NO;

    int reconsumeTimes() default 5;

    long consumeTimeout() default 10L;

}
