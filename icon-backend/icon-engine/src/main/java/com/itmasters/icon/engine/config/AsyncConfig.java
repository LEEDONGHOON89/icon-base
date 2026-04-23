package com.itmasters.icon.engine.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
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
// [2026-04-23] AsyncConfigurer 구현 — @Async 기본 실행기를 ingestTaskExecutor로 지정
//              기존: @Async 미지정 시 SimpleAsyncTaskExecutor(무제한 스레드) 폴백 → 스레드 1312개 생성 → CPU 97%
//              변경: ThreadPoolTaskExecutor(최대 스레드 제한) 기본 실행기로 설정
public class AsyncConfig implements AsyncConfigurer {

    // ── 에이전트 배치 수신 스레드 풀 설정 (application.properties) ─────────────
    // [2026-04-23] 에이전트 배치 파이프라인 전용 스레드 풀 설정값 외부화
    @Value("${icon.engine.async.ingest.core-pool-size:10}")
    private int ingestCorePoolSize;

    @Value("${icon.engine.async.ingest.max-pool-size:50}")
    private int ingestMaxPoolSize;

    @Value("${icon.engine.async.ingest.queue-capacity:500}")
    private int ingestQueueCapacity;

    // ── 관계 처리 스레드 풀 설정 (application.properties) ───────────────────────
    // [2026-04-23] 관계 처리 스레드 풀도 설정값 외부화
    @Value("${icon.engine.async.relation.core-pool-size:2}")
    private int relationCorePoolSize;

    @Value("${icon.engine.async.relation.max-pool-size:4}")
    private int relationMaxPoolSize;

    @Value("${icon.engine.async.relation.queue-capacity:100}")
    private int relationQueueCapacity;

    /**
     * [2026-04-23] 에이전트 배치 파이프라인 전용 스레드 풀
     *
     * SingleIngestService.ingestBatchAndRun() 에서 @Async("ingestTaskExecutor")로 사용.
     * 대량 배치 수신 시 스레드 폭증(SimpleAsyncTaskExecutor) 방지.
     * - core-pool-size : 평상시 유지 스레드 수
     * - max-pool-size  : 큐 포화 시 최대 확장 스레드 수
     * - queue-capacity : 스레드 포화 전 대기 큐 크기
     * - CallerRunsPolicy: 큐·풀 모두 포화 시 호출 스레드에서 직접 실행 (데이터 유실 방지)
     */
    @Bean(name = "ingestTaskExecutor")
    public Executor ingestTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(ingestCorePoolSize);
        executor.setMaxPoolSize(ingestMaxPoolSize);
        executor.setQueueCapacity(ingestQueueCapacity);
        executor.setThreadNamePrefix("ingest-async-");
        executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();

        log.info("ingestTaskExecutor 초기화 - corePoolSize: {}, maxPoolSize: {}, queueCapacity: {}",
                executor.getCorePoolSize(), executor.getMaxPoolSize(), ingestQueueCapacity);

        return executor;
    }

    /**
     * 엔티티 관계 처리용 스레드 풀
     *
     * PREP-4, PREP-5-A, PREP-5-B 프로세서가 사용
     */
    @Bean(name = "relationTaskExecutor")
    public Executor relationTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // [2026-04-23] 하드코딩 → application.properties 설정값으로 변경
        executor.setCorePoolSize(relationCorePoolSize);
        executor.setMaxPoolSize(relationMaxPoolSize);
        executor.setQueueCapacity(relationQueueCapacity);
        executor.setThreadNamePrefix("relation-async-");
        executor.setRejectedExecutionHandler((r, e) ->
            log.warn("관계 처리 작업 거부됨 - 큐가 가득 참"));
        executor.initialize();

        log.info("relationTaskExecutor 초기화 - corePoolSize: {}, maxPoolSize: {}",
            executor.getCorePoolSize(), executor.getMaxPoolSize());

        return executor;
    }

    /**
     * [2026-04-23] @Async 기본 실행기를 ingestTaskExecutor로 지정
     * executor 이름 미지정 @Async 호출 시 SimpleAsyncTaskExecutor 폴백 방지
     */
    @Override
    public Executor getAsyncExecutor() {
        return ingestTaskExecutor();
    }
}
