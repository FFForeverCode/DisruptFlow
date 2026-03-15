package com.chenxiaofei.disruptflow.support.mq.core;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author chenxiaofei
 * @project disrupt-flow
 * @date 2026-03-15
 * @description:
 */
@Configuration
@ConditionalOnMissingBean({RocketMQMsgConverter.class})
public class MessageConverterConfig {
    @Bean
    public RocketMQMsgConverter rocketMQMsgConverter(){
        return new RocketMQMsgConverter();
    }
}
