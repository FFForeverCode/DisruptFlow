package com.chenxiaofei.disruptflow.domain.transaction.impl;

import com.chenxiaofei.disruptorflow.domain.transaction.TransactionCallBackAction;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class TransactionCallBackService {


    /**
     * transaction 事物提交后回调调用执行
     * @param action 抽象方法
     */
    public void execute(TransactionCallBackAction action){

        if(TransactionSynchronizationManager.isActualTransactionActive()){
            TransactionSynchronizationManager
                    .registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            action.callback();
                        }
                    });
        }else {
            action.callback();
        }

    }
}
