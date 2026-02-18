package com.chenxiaofei.disruptorflow.domain.erp;

/**
 * @author chenxiaofei
 * @project DisruptFlow
 * @date 2026-02-18
 * @description: erp 服务接口
 */
public interface ErpService {

    /**
     * 发送消息
     * @param message 消息
     * @param userErpId  用户erp id
     */
    void sendMessage(String message,String userErpId);
}
