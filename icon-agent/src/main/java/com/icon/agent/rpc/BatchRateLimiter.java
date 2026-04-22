package com.icon.agent.rpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * [2026-04-21] 배치 전송 속도 제한 (Token Bucket).
 *
 * maxBatchesPerSecond > 0 이면 초당 허용 배치 수를 제한한다.
 * 버스트 없음 — 정확히 1/maxBatchesPerSecond 초 간격으로 토큰 발급.
 *
 * 설계 원칙:
 *   - 외부 의존성 없음 (Guava RateLimiter 미사용)
 *   - nanosecond 정밀도로 슬립 → JVM 스케줄러 오차 최소화
 *   - maxBatchesPerSecond = 0 이면 제한 없음 (acquire() 즉시 반환)
 *
 * 설정 예 (config.yaml):
 *   maxBatchesPerSecond: 50   # 초당 최대 50 배치 전송
 */
public class BatchRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(BatchRateLimiter.class);

    // [2026-04-22] CONFIG_UPDATE 시 런타임 반영을 위해 volatile non-final 로 변경
    private volatile int maxBatchesPerSecond;
    private volatile long intervalNs;   // 토큰 발급 간격 (nanoseconds)
    private long nextTokenNs;           // 다음 허용 시각 (System.nanoTime 기준)

    public BatchRateLimiter(int maxBatchesPerSecond) {
        this.maxBatchesPerSecond = maxBatchesPerSecond;
        this.intervalNs = maxBatchesPerSecond > 0
                ? 1_000_000_000L / maxBatchesPerSecond
                : 0L;
        this.nextTokenNs = System.nanoTime();

        if (maxBatchesPerSecond > 0) {
            log.info("BatchRateLimiter enabled: {} batches/sec (interval {}ms)",
                    maxBatchesPerSecond, intervalNs / 1_000_000);
        }
    }

    /** Rate Limit 설정 여부 */
    public boolean isEnabled() {
        return intervalNs > 0;
    }

    /**
     * 다음 토큰 발급 시각까지 대기한다.
     * maxBatchesPerSecond = 0 이면 즉시 반환.
     *
     * @throws InterruptedException 스레드 인터럽트 시
     */
    public void acquire() throws InterruptedException {
        if (intervalNs <= 0) return;

        long now = System.nanoTime();
        long waitNs = nextTokenNs - now;

        if (waitNs > 0) {
            TimeUnit.NANOSECONDS.sleep(waitNs);
        }

        // 다음 토큰 시각 갱신 — 누적 지연 방지를 위해 현재 시각 기준으로 재계산
        nextTokenNs = Math.max(System.nanoTime(), nextTokenNs) + intervalNs;
    }

    public int getMaxBatchesPerSecond() {
        return maxBatchesPerSecond;
    }

    /** [2026-04-22] CONFIG_UPDATE 수신 시 속도 제한 런타임 갱신. */
    public synchronized void update(int newMaxBatchesPerSecond) {
        this.maxBatchesPerSecond = newMaxBatchesPerSecond;
        this.intervalNs = newMaxBatchesPerSecond > 0
                ? 1_000_000_000L / newMaxBatchesPerSecond
                : 0L;
        this.nextTokenNs = System.nanoTime(); // 즉시 전송 허용 (burst 방지)
        log.info("BatchRateLimiter updated: {} batches/sec (interval {}ms)",
                newMaxBatchesPerSecond,
                newMaxBatchesPerSecond > 0 ? intervalNs / 1_000_000 : 0);
    }
}
