package com.icon.agent.queue;

import com.icon.agent.collector.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Bounded, per-target blocking queue providing backpressure.
 *
 * When full: offer() returns false → collector pauses (only this target
 * affected).
 * Other targets have independent queues — no cross-target impact.
 */
public class RecordQueue {

    private static final Logger log = LoggerFactory.getLogger(RecordQueue.class);

    private final String targetId;
    private final BlockingQueue<Record> queue;
    private final int capacity;

    public RecordQueue(String targetId, int capacity) {
        this.targetId = targetId;
        this.capacity = capacity;
        this.queue = new ArrayBlockingQueue<>(capacity);
    }

    /**
     * Non-blocking offer. Returns false if full (caller should pause collection).
     */
    public boolean offer(Record record) {
        boolean accepted = queue.offer(record);
        if (!accepted) {
            log.warn("[{}] RecordQueue full ({}/{}), dropping offer — collector will back off",
                    targetId, queue.size(), capacity);
        }
        return accepted;
    }

    /**
     * Drain up to maxCount records, waiting up to timeoutMs for at least one.
     */
    public List<Record> drain(int maxCount, long timeoutMs) throws InterruptedException {
        List<Record> batch = new ArrayList<>(maxCount);
        Record first = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
        if (first != null) {
            batch.add(first);
            queue.drainTo(batch, maxCount - 1);
        }
        return batch;
    }

    /**
     * 단일 레코드를 꺼낸다. timeoutMs=0이면 논블로킹(없으면 null 즉시 반환).
     * [2026-02-25] 1번: BatchBuilder에서 바이트 기준 배치 조립 시 사용
     */
    public Record poll(long timeoutMs) throws InterruptedException {
        if (timeoutMs <= 0) {
            return queue.poll(); // 논블로킹: 없으면 null 즉시 반환
        }
        return queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Drain all remaining records without waiting (used during shutdown).
     */
    public List<Record> drainAll() {
        List<Record> all = new ArrayList<>(queue.size());
        queue.drainTo(all);
        return all;
    }

    public int size() {
        return queue.size();
    }

    public int capacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
