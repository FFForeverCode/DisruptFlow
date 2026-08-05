package com.chenxiaofei.disruptflow.domain.erp.impl;

import com.chenxiaofei.disruptflow.domain.erp.ErpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * ERP 告警降级配置。
 */
@Configuration
public class ErpFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(ErpService.class)
    public ErpService erpServiceFallback() {
        return new LogOnlyErpService();
    }

    @Slf4j
    static class LogOnlyErpService implements ErpService {
        @Override
        public void sendMessage(String message, String userErpId) {
            log.warn("未配置 ERP 告警实现，降级日志通知。userErpId={}, message={}", userErpId, message);
        }
    }
}

