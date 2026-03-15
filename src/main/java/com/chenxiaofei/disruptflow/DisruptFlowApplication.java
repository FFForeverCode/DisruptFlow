package com.chenxiaofei.disruptflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableAutoConfiguration
@EnableConfigurationProperties
@SpringBootApplication
public class DisruptFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisruptFlowApplication.class, args);
    }

}
