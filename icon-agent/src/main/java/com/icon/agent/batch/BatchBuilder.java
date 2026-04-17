package com.icon.agent.batch;

import com.icon.agent.collector.Record;
import com.icon.agent.queue.RecordQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * RecordQueue에서 레코드를 꺼내 Batch를 조립한다.
 *
 * 플러시 트리거 (셋 중 먼저 도달하는 조건):
 *   1. records >= maxBatchSize  (건수 기준)
 *   2. 누적 bytes >= maxBatchBytes (바이트 기준, 0이면 비활성)  ← [2026-02-25] 1번 추가
 *   3. 대기 시간 >= maxBatchMs  (시간 기준)
 *
 * [2026-02-25] 1번: maxBatchBytes 추가 — 레코드 크기에 무관하게 배치 크기 예측 가능
 *              바이트 한도 초과 시 해당 레코드를 pendingRecord에 이월 → 다음 배치 첫 번째로 사용
 */
public class BatchBuilder {

    private static final Logger log = LoggerFactory.getLogger(BatchBuilder.class);

    private final String targetId;
    private final RecordQueue queue;
    private final int maxBatchSize;
    private final long maxBatchMs;
    // [2026-02-25] 1번: 배치 최대 바이트 크기 (0이면 제한 없음)
    private final long maxBatchBytes;

    // [2026-02-25] 1번: 바이트 한도 초과로 다음 배치에 이월된 레코드 버퍼
    private Record pendingRecord = null;

    public BatchBuilder(String targetId,
            RecordQueue queue,
            int maxBatchSize,
            long maxBatchMs,
            long maxBatchBytes) {
        this.targetId = targetId;
        this.queue = queue;
        this.maxBatchSize = maxBatchSize;
        this.maxBatchMs = maxBatchMs;
        this.maxBatchBytes = maxBatchBytes;
    }

    /**
     * 블로킹 호출: maxBatchMs까지 대기하다가 배치가 준비되면 반환한다.
     * 인터럽트 시에만 null을 반환한다.
     *
     * [2026-02-25] 1번: maxBatchBytes 적용 — 바이트 한도 초과 시 배치 즉시 반환
     *              초과 레코드는 pendingRecord에 저장 → 다음 nextBatch() 호출 시 첫 번째 레코드로 사용
     */
    public Batch nextBatch() throws InterruptedException {
        List<Record> records = new ArrayList<>(Math.min(maxBatchSize, 64));
        long accumulatedBytes = 0L;
        long deadline = System.currentTimeMillis() + maxBatchMs;

        // [2026-02-25] 이전 배치에서 이월된 레코드가 있으면 먼저 추가
        if (pendingRecord != null) {
            records.add(pendingRecord);
            accumulatedBytes += estimateBytes(pendingRecord);
            pendingRecord = null;
        }

        // 첫 번째 레코드 대기: 이월 레코드가 없으면 최대 maxBatchMs까지 블로킹
        if (records.isEmpty()) {
            long waitMs = Math.max(1L, deadline - System.currentTimeMillis());
            Record r = queue.poll(waitMs);
            if (r == null) {
                return null; // 타임아웃까지 레코드 없음
            }
            records.add(r);
            accumulatedBytes += estimateBytes(r);
        }

        // 추가 레코드를 논블로킹으로 최대한 수집 (건수/바이트 한도 확인)
        while (records.size() < maxBatchSize) {
            Record r = queue.poll(0L); // 논블로킹
            if (r == null) {
                break; // 큐 비어 있음 — 시간 기반 플러시
            }
            long rBytes = estimateBytes(r);
            // [2026-02-25] 바이트 한도 초과 시 현재 레코드를 다음 배치로 이월
            if (maxBatchBytes > 0 && accumulatedBytes + rBytes > maxBatchBytes) {
                pendingRecord = r; // 다음 nextBatch()에서 사용
                log.debug("[{}] 배치 바이트 한도 도달({}bytes) — 레코드 이월, 현재 배치: {}건",
                        targetId, maxBatchBytes, records.size());
                break;
            }
            records.add(r);
            accumulatedBytes += rBytes;
        }

        Batch batch = new Batch(targetId, records);
        log.debug("[{}] 배치 조립 완료: {}건 / 약 {}bytes → batchId={}",
                targetId, batch.size(), accumulatedBytes, batch.getBatchId());
        return batch;
    }

    /**
     * 레코드 크기를 빠르게 추정한다 (정확한 직렬화 없이).
     * content 문자 수 + JSON 메타데이터 오버헤드(256B) 합산.
     * [2026-02-25] 1번: 문자 수 기준 추정 (UTF-8 ASCII는 1:1, 한글은 최대 3배 오차)
     *              소프트 한도이므로 추정치로 충분
     */
    private long estimateBytes(Record record) {
        long contentLen = (record.getContent() != null) ? record.getContent().length() : 0L;
        return contentLen + 256L; // 256: targetId, source, sourceRef, metadata 등 고정 오버헤드
    }
}
