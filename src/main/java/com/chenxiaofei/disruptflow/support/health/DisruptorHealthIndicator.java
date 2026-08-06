package com.chenxiaofei.disruptflow.support.health;

import com.chenxiaofei.disruptflow.model.RetryDisruptorTaskEvent;
import com.lmax.disruptor.dsl.Disruptor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Disruptor 运行状态探针。
 */
@Component
@RequiredArgsConstructor
public class DisruptorHealthIndicator implements HealthIndicator {

    private final Disruptor<RetryDisruptorTaskEvent> disruptor;

    @Override
    public Health health() {
        long cursor = disruptor.getRingBuffer().getCursor();
        return Health.up()
                .withDetail("disruptorCursor", cursor)
                .build();
    }
}

