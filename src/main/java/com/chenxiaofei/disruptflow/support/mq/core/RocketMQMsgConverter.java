package com.chenxiaofei.disruptflow.support.mq.core;

import com.google.common.collect.Lists;
import org.springframework.messaging.converter.*;
import org.springframework.util.ClassUtils;

import java.util.List;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description: 消息转换器
 */
public class RocketMQMsgConverter {

    private static final boolean JACKSON_PRESENT;

    private static final boolean FASTJSON_PRESENT;

    static{
        ClassLoader classLoader = RocketMQMsgConverter.class.getClassLoader();
        JACKSON_PRESENT = ClassUtils.isPresent("com.fasterxml.jackson.databind,ObjectMapper", classLoader)
                && ClassUtils.isPresent("com.fasterxml.jackson.core.JsonGenerator",classLoader);

        FASTJSON_PRESENT = ClassUtils.isPresent("com.alibaba.fastjson.JSON",classLoader)
        && ClassUtils.isPresent("com.alibaba.fastjson.support.config.FastJsonbject",classLoader);
    }

    public final CompositeMessageConverter messageConverter;

    public RocketMQMsgConverter(){
        List<MessageConverter> messageConverters = Lists.newArrayList();
        ByteArrayMessageConverter byteArrayMessageConverter = new ByteArrayMessageConverter();
        byteArrayMessageConverter.setContentTypeResolver(null);
        messageConverters.add(byteArrayMessageConverter);
        messageConverters.add(new StringMessageConverter());
        if(JACKSON_PRESENT){
            messageConverters.add(new MappingJackson2MessageConverter());
        }

        if(FASTJSON_PRESENT){
            try{
                messageConverters.add((MessageConverter) ClassUtils.forName("com.alibaba.fastjson.support.spring.FastJsonMessageConverter",
                        ClassUtils.getDefaultClassLoader()).newInstance() );
            }catch (Exception e){

            }
        }
        this.messageConverter = new CompositeMessageConverter(messageConverters);
    }

    public MessageConverter getMessageConverter(){
        return this.messageConverter;
    }
}
