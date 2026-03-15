package com.chenxiaofei.disruptflow.support.utils;

import org.slf4j.MDC;

/**
 * @author chenxiaofei
 * @description: RocketMQ 日志追踪工具类
 */
public class RocketMQLogTraceUtils {
    private static final String TRACE_ID_KEY = "traceId";
    
    /**
     * 设置追踪 ID
     */
    public static void setTraceId(String traceId) {
        if (traceId != null) {
            MDC.put(TRACE_ID_KEY, traceId);
        }
    }
    
    /**
     * 移除追踪 ID
     */
    public static void removeTraceId() {
        MDC.remove(TRACE_ID_KEY);
    }
    
    /**
     * 获取追踪 ID
     */
    public static String getTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }
}
