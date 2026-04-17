package com.icon.agent.target;

import com.icon.agent.batch.Batch;
import com.icon.agent.batch.BatchBuilder;
import com.icon.agent.collector.FileCollector;
import com.icon.agent.collector.JdbcCollector;
import com.icon.agent.collector.Record;
import com.icon.agent.config.CollectorConfig;
import com.icon.agent.config.ConfigManager;
import com.icon.agent.config.FileCollectorConfig;
import com.icon.agent.config.JdbcCollectorConfig;
import com.icon.agent.config.TargetConfig;
import com.icon.agent.queue.RecordQueue;
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

    private final List<FileCollector> fileCollectors = new ArrayList<>();
    private final List<JdbcCollector> jdbcCollectors = new ArrayList<>();
    private RecordQueue queue;
    private BatchBuilder batchBuilder;
    private RpcClient rpcClient;
    private SpoolManager spoolManager;
    // 각 수집기가 개별 저장소를 소유:
    //   FileCollector  → FilePositionStore  (data/{targetId}/{collectorId}/positions.dat)
    //   JdbcCollector  → JdbcWatermarkStore (data/{targetId}/{collectorId}/watermark.dat)

    private final ExecutorService deliveryExecutor;
    private volatile boolean running = false;

    public TargetContext(TargetConfig config) {
        this(config, null);
    }

    public TargetContext(TargetConfig config, ConfigManager configManager) {
        this.targetId = config.getId();
        this.config = config;
        this.configManager = configManager;
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

            // 스풀 초기화
            spoolManager = new SpoolManager(targetId);

            // RPC 연결 (config.yaml 반영을 위해 ConfigManager 전달)
            RpcClient.TargetConfigPersister persister = null;
            RpcClient.CollectorsSyncCallback collectorsSync = null;
            if (configManager != null) {
                persister = (tid, rpcEndpoint, compress, tlsKp, tlsKpw, tlsTp, tlsTpw,
                        qc, mbs, mbm, mbb) -> {
                    try {
                        configManager.updateTargetRpcAndSave(tid, rpcEndpoint, compress,
                                tlsKp, tlsKpw, tlsTp, tlsTpw, qc, mbs, mbm, mbb);
                    } catch (Exception e) {
                        log.error("[{}] config.yaml 저장 실패: {}", tid, e.getMessage());
                    }
                };
                collectorsSync = (tid, collectors) -> {
                    try {
                        configManager.applyCollectorsAndSave(tid, collectors);
                        applyCollectorsSync(collectors);
                    } catch (Exception e) {
                        log.error("[{}] config.yaml collectors 저장 실패: {}", tid, e.getMessage());
                    }
                };
            }
            String globalAgentId = (configManager != null && configManager.getConfig().getAgentId() != null && !configManager.getConfig().getAgentId().isBlank())
                    ? configManager.getConfig().getAgentId() : null;
            rpcClient = new RpcClient(targetId, config, ssl, spoolManager, persister, collectorsSync, globalAgentId);
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
                }
            }

            // [2026-02-25] 배치 설정을 TargetConfig에서 직접 참조
            // [2026-02-25] 1번: maxBatchBytes 추가 (건수+바이트 동시 적용)
            batchBuilder = new BatchBuilder(
                    targetId, queue,
                    config.getMaxBatchSize(),
                    config.getMaxBatchMs(),
                    config.getMaxBatchBytes());

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

        synchronized (this) {
            // 1) 제거·비활성화된 수집기 중지
            List<FileCollector> toStopFile = fileCollectors.stream()
                    .filter(f -> !enabledIds.contains(f.getId()))
                    .collect(Collectors.toList());
            toStopFile.forEach(f -> {
                f.stop();
                fileCollectors.remove(f);
                log.info("[{}] 파일 수집기 동기화로 중지: {}", targetId, f.getId());
            });

            List<JdbcCollector> toStopJdbc = jdbcCollectors.stream()
                    .filter(j -> !enabledIds.contains(j.getId()))
                    .collect(Collectors.toList());
            toStopJdbc.forEach(j -> {
                j.stop();
                jdbcCollectors.remove(j);
                log.info("[{}] JDBC 수집기 동기화로 중지: {}", targetId, j.getId());
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
        log.info("[{}] 전송 루프 시작", targetId);
        while (running) {
            try {
                Batch batch = batchBuilder.nextBatch();
                if (batch == null)
                    continue;
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

    public boolean isRpcConnected() {
        return rpcClient != null && rpcClient.isConnected();
    }

    public int getQueueDepth() {
        return queue != null ? queue.size() : 0;
    }

    public int getSpoolDepth() {
        return spoolManager != null ? spoolManager.depth() : 0;
    }
}
