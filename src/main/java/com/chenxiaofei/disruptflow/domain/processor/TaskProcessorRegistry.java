package com.chenxiaofei.disruptflow.domain.processors;

import com.chenxiaofei.disruptflow.model.enums.RetryDisruptorTaskEnum;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 任务处理器注册中心，负责运行时路由与启动校验。
 */
@Component
public class TaskProcessorRegistry {

    private final Map<String, TaskProcessor> taskProcessors;

    public TaskProcessorRegistry(Map<String, TaskProcessor> taskProcessors) {
        this.taskProcessors = taskProcessors;
    }

    @PostConstruct
    public void validateProcessorMapping() {
        for (RetryDisruptorTaskEnum taskEnum : RetryDisruptorTaskEnum.values()) {
            String beanName = taskEnum.getBeanName();
            if (!taskProcessors.containsKey(beanName)) {
                throw new IllegalStateException("任务处理器未注册: " + taskEnum.name() + ", beanName=" + beanName);
            }
        }
    }

    public TaskProcessor getProcessor(String taskType) {
        if (Objects.isNull(taskType) || taskType.isBlank()) {
            throw new IllegalArgumentException("taskType 不能为空");
        }
        RetryDisruptorTaskEnum taskEnum = RetryDisruptorTaskEnum.valueOf(taskType);
        TaskProcessor taskProcessor = taskProcessors.get(taskEnum.getBeanName());
        if (Objects.isNull(taskProcessor)) {
            throw new IllegalStateException("任务处理器不存在, taskType=" + taskType + ", beanName=" + taskEnum.getBeanName());
        }
        return taskProcessor;
    }
}

