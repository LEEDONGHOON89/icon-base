package com.itmasters.icon.engine.datasource.database;

import com.itmasters.icon.common.domain.type.DataSourceType;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsDatabaseConfigEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineDataSourceRepository;
import com.itmasters.icon.engine.datasource.repository.DatabaseConfigRepository;
import com.itmasters.icon.engine.service.SingleIngestService;
import com.itmasters.icon.engine.service.dto.SingleRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [2026-03-13] DATABASE 타입 데이터소스 주기적 폴링 수집기.
 *
 * FileSystemRealtimeCollector와 동일한 구조.
 * ds_database_config.batch_size 기반으로 하이워터마크 증분 수집한다.
 *
 * 새 데이터소스 타입 추가 시 이 클래스를 참조하여 동일 패턴으로 확장하면 된다.
 *
 * 설정:
 *   icon.engine.database.poll-interval-ms=5000  (틱 주기, 기본 5초)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseCollector {

    private static final int DEFAULT_POLL_INTERVAL_MINUTES = 5;

    private final EngineDataSourceRepository engineDataSourceRepository;
    private final DatabaseConfigRepository databaseConfigRepository;
    private final SingleIngestService singleIngestService;

    /** dataSourceId → 마지막 실행 시각 */
    private final Map<String, Instant> lastRunMap = new ConcurrentHashMap<>();

    /** dataSourceId → 캐싱된 pollIntervalMinutes (수집 시점 DB 갱신) */
    private final Map<String, Integer> intervalCache = new ConcurrentHashMap<>();

    /**
     * 틱마다 활성 DATABASE 소스를 순회하여 폴링 주기 도달 시 파이프라인 실행.
     * agent_id가 설정된 소스는 에이전트 Push 모드이므로 스킵.
     */
    @Scheduled(fixedDelayString = "${icon.engine.database.poll-interval-ms:5000}")
    public void collect() {
        List<EngineDataSourceEntity> sources =
                engineDataSourceRepository.findActiveDataSourcesByType(DataSourceType.DATABASE);

        if (sources.isEmpty()) return;

        Instant now = Instant.now();

        for (EngineDataSourceEntity source : sources) {
            String dataSourceId = source.getDataSourceId();
            try {
                int intervalMinutes = intervalCache.getOrDefault(dataSourceId, DEFAULT_POLL_INTERVAL_MINUTES);
                long intervalMs = (long) intervalMinutes * 60 * 1000L;

                Instant lastRun = lastRunMap.get(dataSourceId);
                long elapsedMs = lastRun != null ? now.toEpochMilli() - lastRun.toEpochMilli() : intervalMs;

                if (elapsedMs < intervalMs) continue;

                // 수집 시점에 최신 설정 조회 (agentId, batchSize 등)
                int freshInterval = loadPollIntervalMinutes(dataSourceId);
                intervalCache.put(dataSourceId, freshInterval);
                lastRunMap.put(dataSourceId, now);

                log.info("[{}] DATABASE 폴링 수집 시작 - 주기={}분", dataSourceId, freshInterval);
                collectOne(dataSourceId);

            } catch (Exception e) {
                log.error("[{}] DATABASE 수집 오류: {}", dataSourceId, e.getMessage(), e);
            }
        }
    }

    /**
     * DB에서 pollIntervalMinutes 조회.
     * 미설정이거나 batchSize 기반으로 결정 — 현재는 DEFAULT 반환.
     * 향후 ds_database_config에 poll_interval_minutes 컬럼 추가 시 이 메서드만 수정.
     */
    private int loadPollIntervalMinutes(String dataSourceId) {
        return databaseConfigRepository.findByDataSourceId(dataSourceId)
                .map(cfg -> DEFAULT_POLL_INTERVAL_MINUTES)
                .orElse(DEFAULT_POLL_INTERVAL_MINUTES);
    }

    private void collectOne(String dataSourceId) {
        SingleRunResponse response = singleIngestService.ingestFromDataSourceAndRun(
                dataSourceId, "DATABASE_SCHEDULER");

        if (!response.isSuccess()) {
            log.warn("[{}] DATABASE 파이프라인 실패: {}", dataSourceId, response.getErrorMessage());
            return;
        }

        log.info("[{}] DATABASE pipeline complete - events={}, sensors={}, rules={}, scenarios={}",
                dataSourceId,
                response.getSavedEventStreams(),
                response.getSavedSensors(),
                response.getSavedRules(),
                response.getSavedScenarios());
    }
}
