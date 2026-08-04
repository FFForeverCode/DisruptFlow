package com.chenxiaofei.disruptflow.controller;

import com.chenxiaofei.disruptflow.common.result.ApiResponse;
import com.chenxiaofei.disruptflow.domain.disruptor.RetryDisruptorTaskFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * DisruptFlow 任务管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/disrupt-flow")
@RequiredArgsConstructor
public class DisruptFlowController {

    private final RetryDisruptorTaskFlowService disruptorTaskFlowService;

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("DisruptFlow is running");
    }

    /**
     * 提交异步任务
     */
    @PostMapping("/tasks")
    public ApiResponse<Map<String, Object>> submitTask(@RequestBody Map<String, Object> taskParams) {
        try {
            disruptorTaskFlowService.pushDisruptorFlow(taskParams);
            return ApiResponse.success(Map.of("status", "submitted"));
        } catch (Exception e) {
            log.error("提交任务失败", e);
            return ApiResponse.error("任务提交失败: " + e.getMessage());
        }
    }
}
