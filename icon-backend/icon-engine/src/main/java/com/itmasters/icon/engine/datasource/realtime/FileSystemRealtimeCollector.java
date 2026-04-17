package com.itmasters.icon.engine.datasource.realtime;

import com.itmasters.icon.common.domain.type.DataSourceType;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsFileSystemConfigEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineDataSourceRepository;
import com.itmasters.icon.engine.datasource.repository.FileSystemConfigRepository;
import com.itmasters.icon.engine.processor.prep.Prep1ExtractProcessor;
import com.itmasters.icon.engine.service.MainEngineService;
import com.itmasters.icon.engine.service.SingleIngestService;
import com.itmasters.icon.engine.service.dto.SingleRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Scheduled collector for FILE_SYSTEM_REALTIME data sources.
 *
 * 동작 방식:
 * - 기본 틱: poll-interval-ms(기본 1초)마다 전체 소스를 순회
 * - 소스별 실행 조건: 마지막 수집 시각 대비 scan_interval_minutes 경과 여부
 * - 경과 시에만 collectOne() 실행 → PREP-1 ~ SYNC-1 파이프라인 수행
 *
 * 설정:
 *   icon.engine.file-realtime.poll-interval-ms=1000   (틱 주기, 기본 1초)
 *   ds_file_system_config.scan_interval_minutes       (소스별 수집 주기, 기본 1분)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileSystemRealtimeCollector {

    /** scanIntervalMinutes 미설정 시 기본값 (분) */
    private static final int DEFAULT_SCAN_INTERVAL_MINUTES = 1;

    private final EngineDataSourceRepository engineDataSourceRepository;
    private final FileSystemConfigRepository fileSystemConfigRepository;
    private final Prep1ExtractProcessor prep1ExtractProcessor;
    private final MainEngineService mainEngineService;
    private final SingleIngestService singleIngestService;

    /** dataSourceId → 마지막 실행 시각 */
    private final Map<String, Instant> lastRunMap = new ConcurrentHashMap<>();

    /**
     * dataSourceId → 캐싱된 scanIntervalMinutes (분).
     * 수집 실행 시점에 DB에서 최신값으로 갱신된다.
     * 틱마다 DB를 조회하는 부하를 방지한다.
     */
    private final Map<String, Integer> intervalCache = new ConcurrentHashMap<>();

    /**
     * 기본 틱: poll-interval-ms(기본 1초)마다 전체 소스를 순회.
     * 각 소스의 scanIntervalMinutes 경과 여부를 확인 후 수집 실행.
     * fixedDelay: 이전 실행 완료 후 다음 틱을 스케줄하여 중복 실행 방지.
     */
    @Scheduled(fixedDelayString = "${icon.engine.file-realtime.poll-interval-ms:1000}")
    public void collect() {
        List<EngineDataSourceEntity> sources =
                engineDataSourceRepository.findActiveDataSourcesByType(
                        DataSourceType.FILE_SYSTEM_REALTIME);

        if (sources.isEmpty()) {
            return;
        }

        Instant now = Instant.now();

        for (EngineDataSourceEntity source : sources) {
            String dataSourceId = source.getDataSourceId();
            try {
                // 캐싱된 간격 사용 (미캐싱 시 DB 조회)
                int intervalMinutes = intervalCache.getOrDefault(dataSourceId, DEFAULT_SCAN_INTERVAL_MINUTES);
                long intervalMs = (long) intervalMinutes * 60 * 1000L;

                Instant lastRun = lastRunMap.get(dataSourceId);
                long elapsedMs = lastRun != null ? now.toEpochMilli() - lastRun.toEpochMilli() : intervalMs;

                if (elapsedMs < intervalMs) {
                    // log.debug("[{}] 수집 주기 미도달 - 간격={}분, 경과={}초",dataSourceId, intervalMinutes, elapsedMs / 1000);
                    continue;
                }

                // 수집 시점에 scanIntervalMinutes 최신값 갱신 (DB 조회)
                int freshInterval = loadScanIntervalMinutes(dataSourceId);
                intervalCache.put(dataSourceId, freshInterval);
                lastRunMap.put(dataSourceId, now);

                log.info("[{}] FILE_SYSTEM_REALTIME 수집 시작 - 간격={}분", dataSourceId, freshInterval);
                collectOne(dataSourceId);

            } catch (Exception e) {
                log.error("[{}] collection error (continuing next source): {}",
                        dataSourceId, e.getMessage(), e);
            }
        }
    }

    /**
     * DB에서 소스별 scanIntervalMinutes 조회.
     * 미설정이거나 0 이하인 경우 DEFAULT_SCAN_INTERVAL_MINUTES 반환.
     */
    private int loadScanIntervalMinutes(String dataSourceId) {
        Optional<EngineDsFileSystemConfigEntity> cfg =
                fileSystemConfigRepository.findActiveByDataSourceId(dataSourceId);
        if (cfg.isPresent()
                && cfg.get().getScanIntervalMinutes() != null
                && cfg.get().getScanIntervalMinutes() > 0) {
            return cfg.get().getScanIntervalMinutes();
        }
        return DEFAULT_SCAN_INTERVAL_MINUTES;
    }

    private void collectOne(String dataSourceId) {
        // ingestAndRun() 은 UI 시뮬레이션 전용 (row 직접 주입 방식)
        // 스케줄러는 데이터소스에서 직접 읽는 ingestFromDataSourceAndRun() 사용
        SingleRunResponse response = singleIngestService.ingestFromDataSourceAndRun(
                dataSourceId, "REALTIME_SCHEDULER");

        if (!response.isSuccess()) {
            log.warn("[{}] 파이프라인 실패: {}", dataSourceId, response.getErrorMessage());
            return;
        }

        log.info("[{}] pipeline complete - events: {}, sensors: {}, rules: {}, scenarios: {}",
                dataSourceId,
                response.getSavedEventStreams(),
                response.getSavedSensors(),
                response.getSavedRules(),
                response.getSavedScenarios());
    }
}
