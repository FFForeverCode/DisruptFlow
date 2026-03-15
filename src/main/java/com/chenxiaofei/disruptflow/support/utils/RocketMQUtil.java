package com.chenxiaofei.disruptflow.support.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author chenxiaofei
 * @description: RocketMQ 工具类
 */
public class RocketMQUtil {
    private static final AtomicLong COUNTER = new AtomicLong(0);
    
    /**
     * 生成实例名称
     */
    public static String getInstanceName() {
        return String.format("%s@%d", getLocalhost(), COUNTER.incrementAndGet());
    }
    
    /**
     * 获取本机 IP
     */
    private static String getLocalhost() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }
}
