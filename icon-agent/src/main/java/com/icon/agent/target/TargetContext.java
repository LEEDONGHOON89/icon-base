package com.icon.agent.target;

import com.icon.agent.audit.AuditLogger;
import com.icon.agent.batch.Batch;
import com.icon.agent.batch.BatchBuilder;
import com.icon.agent.collector.FileCollector;
import com.icon.agent.collector.JdbcCollector;
import com.icon.agent.collector.Record;
import com.icon.agent.config.CollectorConfig;
import com.icon.agent.config.ConfigManager;
import com.icon.agent.config.FileCollectorConfig;
import com.icon.agent.config.JdbcCollectorConfig;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.icon.agent.config.RpcConfig;
import com.icon.agent.config.TargetConfig;
import com.icon.agent.config.TlsConfig;
import com.icon.agent.queue.RecordQueue;
import com.icon.agent.rpc.BatchRateLimiter;
import com.icon.agent.rpc.RpcClient;
import com.icon.agent.spool.SpoolManager;
import com.icon.agent.store.FilePositionStore;
import com.icon.agent.store.JdbcWatermarkStore;
import com.icon.agent.tls.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.net.ssl.SSLContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 대상별 컨텍스트. 모든 컴포넌트를 연결하고 전송 루프를 실행한다.
 * [2026-02-25] 타입 구분을 통한 통합 수집기 목록 지원으로 업데이트
 * [2026-02-25] TargetConfig 구조 변경에 맞게 collectors/배치/큐 접근 방식 수정
 * [2026-02-25] 수집기별 개별 FilePositionStore 사용 — 다중 FileCollector 간 덮어쓰기 방지
 * [2026-03-05] JdbcCollector 저장소를 FilePositionStore → JdbcWatermarkStore 로 분리.
 *              단일 jdbcCollector 필드 → List<JdbcCollector> jdbcCollectors 로 다중 인스턴스 지원.
 */
public class TargetContext {

    private static final Logger log = LoggerFactory.getLogger(TargetContext.class);

    private final String targetId;
    private final TargetConfig config;
    private final ConfigManager configManager;
    // [2026-04-21] CONFIG_UPDATE 후 targets.json 동기화 콜백 (targets.json 관리 target 전용)
    private final Runnable storeSync;

    private final List<FileCollector> fileCollectors = new ArrayList<>();
    private final List<JdbcCollector> jdbcCollectors = new ArrayList<>();
    // [2026-04-21] COLLECTORS_SYNC로 수신한 전체 수집기 목록 (활성·비활성 포함) — CLI 상태 조회용
    private volatile java.util.Map<String, CollectorConfig> knownCollectors = new java.util.LinkedHashMap<>();
    private RecordQueue queue;
    private BatchBuilder batchBuilder;
    private RpcClient rpcClient;
    private SpoolManager spoolManager;
    // 각 수집기가 개별 저장소를 소유:
    //   FileCollector  → FilePositionStore  (data/{targetId}/{collectorId}/positions.dat)
    //   JdbcCollector  → JdbcWatermarkStore (data/{targetId}/{collectorId}/watermark.dat)

    private final ExecutorService deliveryExecutor;
    private volatile boolean running = false;

    // [2026-04-21] 전송 속도 제한
    private BatchRateLimiter rateLimiter;

    public TargetContext(TargetConfig config) {
        this(config, null, null);
    }

    public TargetContext(TargetConfig config, ConfigManager configManager) {
        this(config, configManager, null);
    }

    // [2026-04-21] storeSync: targets.json 관리 target에서 CONFIG_UPDATE 시 동기화
    public TargetContext(TargetConfig config, ConfigManager configManager, Runnable storeSync) {
        this.targetId = config.getId();
        this.config = config;
        this.configManager = configManager;
        this.storeSync = storeSync;
        this.deliveryExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "delivery-" + targetId);
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        MDC.put("targetId", targetId);
        try {
            log.info("[{}] 대상 컨텍스트 초기화 중", targetId);

            // [2026-02-25] 수집기별 개별 FilePositionStore 사용으로 변경
            //              공유 positionStore 제거 — 각 수집기 생성 시 개별 store를 전달

            // [2026-02-25] 큐 설정을 TargetConfig에서 직접 참조
            queue = new RecordQueue(targetId, config.getQueueCapacity());

            // TLS 설정
            SSLContext ssl = SslContextFactory.build(config.getRpc().getTls());

            // [2026-04-21] 스풀 초기화 — 크기 제한 설정 전달
            spoolManager = new SpoolManager(
                    "spool", targetId,
                    config.getMaxSpoolFiles(),
                    config.getMaxSpoolSizeMb());

            // RPC 연결 (config.yaml 반영을 위해 ConfigManager 전달)
            RpcClient.TargetConfigPersister persister = null;
            RpcClient.CollectorsSyncCallback collectorsSync = null;
            // [2026-04-21] targets는 targets.json에서 관리 — config.yaml 저장 제거
            //              CONFIG_UPDATE: in-memory 갱신 + targets.json 동기화만 수행
            // [2026-04-22] maxBatchesPerSecond 파라미터 추가
            persister = (tid, rpcEndpoint, compress, tlsKp, tlsKpw, tlsTp, tlsTpw,
                    qc, mbs, mbm, mbb, mbps) -> {
                applyConfigUpdate(compress, tlsKp, tlsKpw, tlsTp, tlsTpw, qc, mbs, mbm, mbb, mbps);
                if (storeSync != null) storeSync.run();
            };
            // COLLECTORS_SYNC: 런타임 수집기 동기화만 수행 (collectors는 targets.json 미저장, 서버에서 복원)
            collectorsSync = (tid, collectors) -> applyCollectorsSync(collectors);
            // [2026-04-22] COLLECTOR_RESET: 수집기 위치 파일 삭제 후 재시작
            RpcClient.CollectorResetCallback resetCallback = (tid, collectorId) -> resetCollector(collectorId);
            String globalAgentId = (configManager != null
                    && configManager.getConfig().getAgentId() != null
                    && !configManager.getConfig().getAgentId().isBlank())
                    ? configManager.getConfig().getAgentId() : null;
            rpcClient = new RpcClient(targetId, config, ssl, spoolManager, persister, collectorsSync, resetCallback, globalAgentId);
            rpcClient.connect();

            // [2026-02-25] TargetConfig에서 직접 수집기 목록을 가져와 타입별로 처리
            for (CollectorConfig collector : config.getCollectors()) {
                if (!collector.isEnabled()) {
                    log.info("[{}] 수집기 비활성화됨, 건너뜀: {}", targetId, collector.getName());
                    continue;
                }

                if ("FILE".equals(collector.getType()) && collector instanceof FileCollectorConfig) {
                    FileCollectorConfig fConfig = (FileCollectorConfig) collector;
                    // [2026-02-25] 수집기별 개별 store 생성 — data/{targetId}/{collectorId}/positions.dat
                    //              다중 FileCollector가 동일 target에 속할 때 서로의 위치를 덮어쓰지 않도록 분리
                    FilePositionStore collectorStore = new FilePositionStore(targetId, fConfig.getId());
                    FileCollector fileCollector = new FileCollector(targetId, fConfig, queue, collectorStore);
                    fileCollector.start();
                    fileCollectors.add(fileCollector);
                    log.info("[{}] 파일 수집기 시작: {} → 위치 저장소: data/{}/{}/positions.dat",
                            targetId, fConfig.getName(), targetId, fConfig.getId());
                    // [2026-04-21] 감사 로그: 수집기 시작
                    AuditLogger.collectorStarted(targetId, fConfig.getId(), "FILE", fConfig.getName());
                } else if ("JDBC".equals(collector.getType()) && collector instanceof JdbcCollectorConfig) {
                    JdbcCollectorConfig jConfig = (JdbcCollectorConfig) collector;
                    // [2026-03-05] FilePositionStore → JdbcWatermarkStore 분리
                    //              저장 경로: data/{targetId}/{collectorId}/watermark.dat
                    JdbcWatermarkStore jdbcStore = new JdbcWatermarkStore(targetId, jConfig.getId());
                    JdbcCollector jc = new JdbcCollector(targetId, jConfig, queue, jdbcStore);
                    jc.start();
                    jdbcCollectors.add(jc);
                    log.info("[{}] JDBC 수집기 시작: {} → 워터마크 저장소: data/{}/{}/watermark.dat",
                            targetId, jConfig.getName(), targetId, jConfig.getId());
                    // [2026-04-21] 감사 로그: 수집기 시작
                    AuditLogger.collectorStarted(targetId, jConfig.getId(), "JDBC", jConfig.getName());
                }
            }

            // [2026-02-25] 배치 설정을 TargetConfig에서 직접 참조
            // [2026-02-25] 1번: maxBatchBytes 추가 (건수+바이트 동시 적용)
            batchBuilder = new BatchBuilder(
                    targetId, queue,
                    config.getMaxBatchSize(),
                    config.getMaxBatchMs(),
                    config.getMaxBatchBytes());

            // [2026-04-21] Rate Limiter 초기화
            rateLimiter = new BatchRateLimiter(config.getMaxBatchesPerSecond());

            running = true;

            // 전송 루프 시작
            deliveryExecutor.submit(this::deliveryLoop);
            log.info("[{}] 대상 컨텍스트 시작 완료", targetId);

        } catch (Exception e) {
            log.error("[{}] 대상 컨텍스트 시작 실패: {}", targetId, e.getMessage(), e);
        } finally {
            MDC.remove("targetId");
        }
    }

    /**
     * [2026-04-22] COLLECTOR_RESET 수신 시 호출.
     * 해당 수집기를 중지하고 위치 파일(positions.dat / watermark.dat)을 삭제한 뒤 재시작한다.
     * 재시작 후 수집기는 파일/DB 처음부터 재수집한다.
     */
    public synchronized void resetCollector(String collectorId) {
        log.info("[{}] 수집기 초기화 요청 - collectorId={}", targetId, collectorId);

        // 1. 실행 중인 수집기 중지
        fileCollectors.stream()
                .filter(f -> f.getId().equals(collectorId))
                .findFirst()
                .ifPresent(fc -> {
                    fc.stop();
                    fileCollectors.remove(fc);
                    log.info("[{}] 파일 수집기 중지 - collectorId={}", targetId, collectorId);
                });
        jdbcCollectors.stream()
                .filter(j -> j.getId().equals(collectorId))
                .findFirst()
                .ifPresent(jc -> {
                    jc.stop();
                    jdbcCollectors.remove(jc);
                    log.info("[{}] JDBC 수집기 중지 - collectorId={}", targetId, collectorId);
                });

        // 2. 위치 파일 삭제 (positions.dat 또는 watermark.dat)
        deletePositionFile(Path.of("data", targetId, collectorId, "positions.dat"));
        deletePositionFile(Path.of("data", targetId, collectorId, "watermark.dat"));

        // 3. 감사 로그
        AuditLogger.configChanged(targetId, "COLLECTOR_RESET", collectorId);

        // 4. knownCollectors에서 설정을 찾아 재시작 (start() 이후 COLLECTORS_SYNC를 받은 경우)
        CollectorConfig cfg = knownCollectors.get(collectorId);
        if (cfg != null && cfg.isEnabled()) {
            try {
                if ("FILE".equals(cfg.getType()) && cfg instanceof FileCollectorConfig fConfig) {
                    FilePositionStore store = new FilePositionStore(targetId, fConfig.getId());
                    FileCollector fc = new FileCollector(targetId, fConfig, queue, store);
                    fc.start();
                    fileCollectors.add(fc);
                    log.info("[{}] 파일 수집기 초기화 후 재시작 - collectorId={}", targetId, collectorId);
                    AuditLogger.collectorStarted(targetId, collectorId, "FILE", fConfig.getName());
                } else if ("JDBC".equals(cfg.getType()) && cfg instanceof JdbcCollectorConfig jConfig) {
                    JdbcWatermarkStore store = new JdbcWatermarkStore(targetId, jConfig.getId());
                    JdbcCollector jc = new JdbcCollector(targetId, jConfig, queue, store);
                    jc.start();
                    jdbcCollectors.add(jc);
                    log.info("[{}] JDBC 수집기 초기화 후 재시작 - collectorId={}", targetId, collectorId);
                    AuditLogger.collectorStarted(targetId, collectorId, "JDBC", jConfig.getName());
                }
            } catch (Exception e) {
                log.error("[{}] 수집기 초기화 후 재시작 실패 - collectorId={}: {}", targetId, collectorId, e.getMessage(), e);
            }
        } else {
            log.warn("[{}] 수집기 설정 없음 또는 비활성 — 재시작 생략, COLLECTORS_SYNC 대기: collectorId={}",
                    targetId, collectorId);
        }
    }

    private void deletePositionFile(Path path) {
        try {
            if (Files.deleteIfExists(path)) {
                log.info("[{}] 수집기 위치 파일 삭제 완료: {}", targetId, path);
            }
        } catch (IOException e) {
            log.error("[{}] 수집기 위치 파일 삭제 실패: {} - {}", targetId, path, e.getMessage());
        }
    }

    /**
     * COLLECTORS_SYNC 수신 시 호출. config.yaml 저장 후 런타임에 수집기를 동기화한다.
     *
     * 처리 규칙:
     * - 제거/비활성화된 수집기: 중지 후 목록에서 제거
     * - 이미 실행 중 + 설정 변경 가능성 있는 수집기: 중지 후 새 설정으로 재시작
     *   (hasHeader, fileFormat, delimiter, charset 등 파싱 설정 변경을 즉시 반영)
     * - 새로 추가/활성화된 수집기: 시작하여 목록에 추가
     *
     * 참고: FilePositionStore는 디스크 기반이므로 재시작 시 수집 offset이 보존된다.
     */
    public void applyCollectorsSync(List<CollectorConfig> newCollectors) {
        if (queue == null || spoolManager == null) {
            log.debug("[{}] 아직 start() 완료 전이라 수집기 동기화 건너뜀", targetId);
            return;
        }
        Set<String> enabledIds = newCollectors == null ? Set.of() : newCollectors.stream()
                .filter(CollectorConfig::isEnabled)
                .map(CollectorConfig::getId)
                .collect(Collectors.toSet());

        // [2026-04-21] 전체 수집기 목록(활성·비활성) 보존 — getCollectorInfos()에서 비활성 수집기도 표시
        if (newCollectors != null) {
            java.util.Map<String, CollectorConfig> updated = new java.util.LinkedHashMap<>();
            newCollectors.forEach(c -> updated.put(c.getId(), c));
            this.knownCollectors = updated;
        }

        synchronized (this) {
            // 1) 제거·비활성화된 수집기 중지
            List<FileCollector> toStopFile = fileCollectors.stream()
                    .filter(f -> !enabledIds.contains(f.getId()))
                    .collect(Collectors.toList());
            toStopFile.forEach(f -> {
                f.stop();
                fileCollectors.remove(f);
                log.info("[{}] 파일 수집기 동기화로 중지: {}", targetId, f.getId());
                // [2026-04-21] 감사 로그: 수집기 중지
                AuditLogger.collectorStopped(targetId, f.getId(), "FILE");
            });

            List<JdbcCollector> toStopJdbc = jdbcCollectors.stream()
                    .filter(j -> !enabledIds.contains(j.getId()))
                    .collect(Collectors.toList());
            toStopJdbc.forEach(j -> {
                j.stop();
                jdbcCollectors.remove(j);
                log.info("[{}] JDBC 수집기 동기화로 중지: {}", targetId, j.getId());
                // [2026-04-21] 감사 로그: 수집기 중지
                AuditLogger.collectorStopped(targetId, j.getId(), "JDBC");
            });

            // 2) 활성화된 수집기: 이미 실행 중이면 설정 갱신을 위해 중지 후 재시작,
            //    신규이면 그냥 시작
            if (newCollectors != null) {
                for (CollectorConfig c : newCollectors) {
                    if (!c.isEnabled()) continue;
                    String id = c.getId();

                    if ("FILE".equals(c.getType()) && c instanceof FileCollectorConfig fConfig) {
                        // 기존 실행 중인 수집기가 있으면 중지 (설정 변경 반영을 위해)
                        fileCollectors.stream()
                                .filter(f -> f.getId().equals(id))
                                .findFirst()
                                .ifPresent(old -> {
                                    old.stop();
                                    fileCollectors.remove(old);
                                    log.info("[{}] 파일 수집기 설정 갱신 위해 중지: {} (재시작 예정)", targetId, id);
                                });
                        // 새 설정으로 시작 (positions.dat 디스크에서 읽어 offset 보존)
                        try {
                            FilePositionStore store = new FilePositionStore(targetId, fConfig.getId());
                            FileCollector fc = new FileCollector(targetId, fConfig, queue, store);
                            fc.start();
                            fileCollectors.add(fc);
                            log.info("[{}] 파일 수집기 동기화로 (재)시작: {} (hasHeader={}, format={}) → data/{}/{}/positions.dat",
                                    targetId, fConfig.getName(), fConfig.isCsvHasHeader(),
                                    fConfig.getFormat(), targetId, fConfig.getId());
                            // [2026-04-21] 감사 로그: 수집기 시작 (동기화)
                            AuditLogger.collectorStarted(targetId, fConfig.getId(), "FILE", fConfig.getName());
                        } catch (Exception e) {
                            log.error("[{}] 파일 수집기 시작 실패: {} - {}", targetId, id, e.getMessage(), e);
                        }

                    } else if ("JDBC".equals(c.getType()) && c instanceof JdbcCollectorConfig jConfig) {
                        // JDBC: 기존 실행 중이면 중지 후 재시작
                        jdbcCollectors.stream()
                                .filter(j -> j.getId().equals(id))
                                .findFirst()
                                .ifPresent(old -> {
                                    old.stop();
                                    jdbcCollectors.remove(old);
                                    log.info("[{}] JDBC 수집기 설정 갱신 위해 중지: {} (재시작 예정)", targetId, id);
                                });
                        try {
                            JdbcWatermarkStore store = new JdbcWatermarkStore(targetId, jConfig.getId());
                            JdbcCollector jc = new JdbcCollector(targetId, jConfig, queue, store);
                            jc.start();
                            jdbcCollectors.add(jc);
                            log.info("[{}] JDBC 수집기 동기화로 (재)시작: {} → data/{}/{}/watermark.dat",
                                    targetId, jConfig.getName(), targetId, jConfig.getId());
                            // [2026-04-21] 감사 로그: 수집기 시작 (동기화)
                            AuditLogger.collectorStarted(targetId, jConfig.getId(), "JDBC", jConfig.getName());
                        } catch (Exception e) {
                            log.error("[{}] JDBC 수집기 시작 실패: {} - {}", targetId, id, e.getMessage(), e);
                        }
                    }
                }
            }
        }
    }

    private void deliveryLoop() {
        MDC.put("targetId", targetId);
        log.info("[{}] 전송 루프 시작 (rateLimiter={})",
                targetId, rateLimiter.isEnabled()
                        ? rateLimiter.getMaxBatchesPerSecond() + "/sec" : "unlimited");
        while (running) {
            try {
                Batch batch = batchBuilder.nextBatch();
                if (batch == null)
                    continue;

                // [2026-04-21] Rate Limit 적용 — 설정된 속도 초과 시 대기
                rateLimiter.acquire();

                rpcClient.send(batch);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("[{}] 전송 루프 오류 (계속 실행): {}", targetId, e.getMessage(), e);
            }
        }
        log.info("[{}] 전송 루프 중지", targetId);
        MDC.remove("targetId");
    }

    public void stop() {
        MDC.put("targetId", targetId);
        try {
            log.info("[{}] 대상 컨텍스트 중지 중", targetId);
            running = false;

            // 1. 수집기 중지
            for (FileCollector collector : fileCollectors) {
                collector.stop();
            }
            for (JdbcCollector jc : jdbcCollectors) {
                jc.stop();
            }

            // 2. 남은 큐 데이터를 스풀에 플러시
            flushQueueToSpool();

            // 3. 위치 정보 저장 — 각 수집기의 stop()에서 개별 store에 이미 저장됨
            //    [2026-02-25] 공유 positionStore 제거로 인해 이 블록 삭제
            //                 FileCollector.stop() → persistPositions() → 각 store에 저장

            // 4. RPC 연결 해제
            if (rpcClient != null)
                rpcClient.disconnect();

            // 5. 전송 실행기 종료
            deliveryExecutor.shutdownNow();
            if (!deliveryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("[{}] 전송 실행기가 정상적으로 종료되지 않았습니다", targetId);
            }

            log.info("[{}] 대상 컨텍스트 중지 완료", targetId);
        } catch (Exception e) {
            log.error("[{}] 중지 중 오류 발생: {}", targetId, e.getMessage(), e);
        } finally {
            MDC.remove("targetId");
        }
    }

    private void flushQueueToSpool() {
        if (queue == null || spoolManager == null)
            return;
        var remaining = queue.drainAll();
        if (remaining.isEmpty())
            return;
        log.info("[{}] 종료 시 남은 레코드 {}건을 스풀에 저장 중", targetId, remaining.size());
        // [2026-02-25] 배치 설정을 TargetConfig에서 직접 참조
        // [2026-02-25] 1번: 종료 시 스풀 저장도 maxBatchBytes 기준으로 분할
        int batchSize = config.getMaxBatchSize();
        long maxBytes = config.getMaxBatchBytes();
        List<Record> current = new ArrayList<>();
        long currentBytes = 0L;

        for (Record r : remaining) {
            long rBytes = (r.getContent() != null ? r.getContent().length() : 0L) + 256L;
            // 건수 또는 바이트 한도 도달 시 현재 배치 스풀 저장
            if (!current.isEmpty()
                    && (current.size() >= batchSize || (maxBytes > 0 && currentBytes + rBytes > maxBytes))) {
                try {
                    spoolManager.write(new Batch(targetId, current));
                } catch (Exception e) {
                    log.error("[{}] 종료 배치 스풀 저장 실패: {}", targetId, e.getMessage());
                }
                current = new ArrayList<>();
                currentBytes = 0L;
            }
            current.add(r);
            currentBytes += rBytes;
        }
        // 남은 레코드 저장
        if (!current.isEmpty()) {
            try {
                spoolManager.write(new Batch(targetId, current));
            } catch (Exception e) {
                log.error("[{}] 종료 배치 스풀 저장 실패: {}", targetId, e.getMessage());
            }
        }
    }

    public String getTargetId() {
        return targetId;
    }

    // [2026-04-21] AdminServer 목록 표시용
    public String getEndpoint() {
        return config.getRpc() != null ? config.getRpc().getEndpoint() : "";
    }

    public TargetConfig getConfig() {
        return config;
    }

    public boolean isRpcConnected() {
        return rpcClient != null && rpcClient.isConnected();
    }

    public int getQueueDepth() {
        return queue != null ? queue.size() : 0;
    }

    public int getSpoolDepth() {
        return spoolManager != null ? spoolManager.depth() : 0;
    }

    // [2026-04-21] 수집기 상태·설정 목록 반환 — AdminServer /targets/{id}/collectors 용
    //              knownCollectors(활성·비활성 전체) 기반으로 조회, 실제 실행 상태를 overlay
    public synchronized List<java.util.Map<String, Object>> getCollectorInfos() {
        // 실행 중인 수집기 ID 집합
        java.util.Set<String> runningFileIds = fileCollectors.stream()
                .filter(com.icon.agent.collector.FileCollector::isRunning)
                .map(com.icon.agent.collector.FileCollector::getId)
                .collect(Collectors.toSet());
        java.util.Set<String> runningJdbcIds = jdbcCollectors.stream()
                .filter(com.icon.agent.collector.JdbcCollector::isRunning)
                .map(com.icon.agent.collector.JdbcCollector::getId)
                .collect(Collectors.toSet());

        List<java.util.Map<String, Object>> result = new ArrayList<>();
        for (CollectorConfig c : knownCollectors.values()) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("id",      c.getId());
            m.put("name",    c.getName());
            m.put("type",    c.getType());
            m.put("enabled", c.isEnabled());
            m.put("pollIntervalMs", c.getPollIntervalMs());
            if ("FILE".equals(c.getType()) && c instanceof com.icon.agent.config.FileCollectorConfig fc) {
                m.put("running",     runningFileIds.contains(c.getId()));
                m.put("directory",   fc.getDirectory());
                m.put("filePattern", fc.getFileNamePattern());
                m.put("format",      fc.getFormat());
            } else if ("JDBC".equals(c.getType()) && c instanceof com.icon.agent.config.JdbcCollectorConfig jc) {
                m.put("running",     runningJdbcIds.contains(c.getId()));
                m.put("url",         jc.getUrl());
                m.put("query",       jc.getQuery());
            } else {
                m.put("running", false);
            }
            result.add(m);
        }
        // knownCollectors가 비어있으면 기존 fileCollectors/jdbcCollectors fallback (COLLECTORS_SYNC 미수신 시)
        if (result.isEmpty()) {
            for (com.icon.agent.collector.FileCollector fc : fileCollectors) {
                com.icon.agent.config.FileCollectorConfig c = fc.getConfig();
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", c.getId()); m.put("name", c.getName()); m.put("type", "FILE");
                m.put("running", fc.isRunning()); m.put("enabled", c.isEnabled());
                m.put("pollIntervalMs", c.getPollIntervalMs());
                m.put("directory", c.getDirectory()); m.put("filePattern", c.getFileNamePattern()); m.put("format", c.getFormat());
                result.add(m);
            }
            for (com.icon.agent.collector.JdbcCollector jc : jdbcCollectors) {
                com.icon.agent.config.JdbcCollectorConfig c = jc.getConfig();
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", c.getId()); m.put("name", c.getName()); m.put("type", "JDBC");
                m.put("running", jc.isRunning()); m.put("enabled", c.isEnabled());
                m.put("pollIntervalMs", c.getPollIntervalMs());
                m.put("url", c.getUrl()); m.put("query", c.getQuery());
                result.add(m);
            }
        }
        return result;
    }

    public int getCollectorCount() {
        return fileCollectors.size() + jdbcCollectors.size();
    }

    // [2026-04-21] CONFIG_UPDATE 수신 시 in-memory TargetConfig 필드 동기화
    //              config.yaml에 없는 targets.json target도 서버 설정 변경을 반영하기 위해 필요
    // [2026-04-22] maxBatchesPerSecond 파라미터 추가 + BatchBuilder/BatchRateLimiter 런타임 갱신
    private void applyConfigUpdate(boolean compress,
            String tlsKp, String tlsKpw, String tlsTp, String tlsTpw,
            int qc, int mbs, long mbm, long mbb, int mbps) {
        RpcConfig rpc = config.getRpc();
        if (rpc == null) return;
        rpc.setCompress(compress);
        if (tlsKp != null || tlsKpw != null || tlsTp != null || tlsTpw != null) {
            TlsConfig tls = rpc.getTls();
            if (tls == null) { tls = new TlsConfig(); rpc.setTls(tls); }
            if (tlsKp  != null) tls.setKeystorePath(tlsKp.isEmpty()  ? null : tlsKp);
            if (tlsKpw != null) tls.setKeystorePassword(tlsKpw.isEmpty() ? null : tlsKpw);
            if (tlsTp  != null) tls.setTruststorePath(tlsTp.isEmpty() ? null : tlsTp);
            if (tlsTpw != null) tls.setTruststorePassword(tlsTpw.isEmpty() ? null : tlsTpw);
        }
        if (qc   > 0)  config.setQueueCapacity(qc);
        if (mbs  > 0)  config.setMaxBatchSize(mbs);
        if (mbm  > 0)  config.setMaxBatchMs(mbm);
        if (mbb >= 0)  config.setMaxBatchBytes(mbb);
        // [2026-04-22] maxBatchesPerSecond: -1은 미지정(변경 없음)
        if (mbps >= 0) config.setMaxBatchesPerSecond(mbps);

        // [2026-04-22] BatchBuilder/BatchRateLimiter에 변경 값 즉시 반영 (재시작 없이 적용)
        if (batchBuilder != null && (mbs > 0 || mbm > 0 || mbb >= 0)) {
            int newSize  = mbs  > 0  ? mbs  : config.getMaxBatchSize();
            long newMs   = mbm  > 0  ? mbm  : config.getMaxBatchMs();
            long newBytes = mbb >= 0 ? mbb  : config.getMaxBatchBytes();
            batchBuilder.updateBatchSettings(newSize, newMs, newBytes);
        }
        if (rateLimiter != null && mbps >= 0) {
            rateLimiter.update(mbps);
        }

        log.info("[{}] CONFIG_UPDATE 적용 완료 — maxBatchSize={}, maxBatchMs={}, maxBatchBytes={}, maxBatchesPerSecond={}",
                targetId, config.getMaxBatchSize(), config.getMaxBatchMs(),
                config.getMaxBatchBytes(), config.getMaxBatchesPerSecond());
    }
}
