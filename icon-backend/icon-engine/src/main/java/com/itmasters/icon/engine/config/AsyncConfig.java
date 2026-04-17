package com.itmasters.icon.engine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 처리 설정
 *
 * 엔티티 관계 추출 등 실시간 탐지와 분리된 작업을 비동기로 처리
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 엔티티 관계 처리용 스레드 풀
     *
     * PREP-4, PREP-5-A, PREP-5-B 프로세서가 사용
     */
    @Bean(name = "relationTaskExecutor")
    public Executor relationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("relation-async-");
        executor.setRejectedExecutionHandler((r, e) ->
            log.warn("관계 처리 작업 거부됨 - 큐가 가득 참"));
        executor.initialize();

        log.info("relationTaskExecutor 초기화 - corePoolSize: {}, maxPoolSize: {}",
            executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }
}
