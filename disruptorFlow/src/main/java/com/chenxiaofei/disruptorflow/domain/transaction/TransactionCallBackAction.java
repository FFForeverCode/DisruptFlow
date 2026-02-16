package com.chenxiaofei.disruptorflow.domain.transaction;

/**
 * @author chenxiaofei
 * @description: 事务回调接口
 */
public interface TransactionCallBackAction {

    /**
     * 回调接口
     */
    void callback();
}
