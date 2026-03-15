package com.chenxiaofei.disruptflow.support.mq.model;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description:
 */
@ConfigurationProperties(prefix = "rocketmq.producer")
@Component
@Data
public class RocketmqProperties {

    private String retryDisruptorTopic;


    private String retryDisruptorTags;
}
