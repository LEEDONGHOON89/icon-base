package com.icon.agent.rpc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
// [2026-02-25] 4번: ObjectNode 제거 — serializeBatch() 스트리밍 직렬화로 대체
import com.icon.agent.audit.AuditLogger;
import com.icon.agent.batch.Batch;
import com.icon.agent.config.RpcConfig;
import com.icon.agent.spool.SpoolManager;
import com.icon.agent.spool.SpoolManager.SpoolEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.*;
import java.util.zip.GZIPOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
// [2026-02-25] Race Condition 수정: WebSocket 직렬화 락 추가
import java.util.concurrent.locks.ReentrantLock;

/**
 * 대상별 WebSocket RPC 클라이언트.
 *
 * [2026-02-25] 다음 두 가지 문제 수정:
 *   1. Race Condition: replaySpool()과 deliveryLoop()가 동시에 sendText() 호출 가능
 *      → wsSendLock(ReentrantLock)으로 sendText() 직렬화
 *   2. 파이프라이닝 없음: 배치 1건 전송 후 ACK 대기(최대 30초)해야 다음 배치 전송 가능
 *      → inflightSemaphore(MAX_INFLIGHT=4)와 비동기 ACK 처리로 동시 전송 지원
 *   3. 연결 끊김 시 ACK 타임아웃 지연: 최대 ackTimeoutMs(30초)까지 세마포어 미반환
 *      → cancelPendingAcks()로 미완료 ACK 즉시 실패 처리
 *   4. 스풀 재전송 중 Race: 재전송 중 일반 send()가 끼어들면 순서 보장 불가
 *      → spoolReplaying 플래그로 재전송 중 일반 send()를 스풀 우회 처리
 * [2026-03-05] GZIP 압축 전송 지원 추가.
 *   rpc.compress=true 설정 시 serializeBatch() 결과를 gzip()으로 압축하여
 *   sendBinary()로 전송 — 서버는 binary frame 수신 시 GZIP 해제 후 JSON 파싱.
 *
 * 전송 흐름:
 *   1. 배치 JSON을 WebSocket으로 전송 (최대 MAX_INFLIGHT건 동시 in-flight)
 *      rpc.compress=true  → GZIP 압축 후 binary frame 전송  [2026-03-05 추가]
 *      rpc.compress=false → text frame 전송 (기본값)
 *      서버 구분: text frame = JSON, binary frame = GZIP(JSON)
 *   2. batchId를 포함한 ACK 메시지 비동기 대기
 *   3. ACK 성공: 인플라이트 슬롯 반환
 *      ACK 실패/타임아웃: 스풀 저장 + 슬롯 반환
 *   연결 끊김: 미완료 ACK 즉시 실패 처리, 지수 백오프로 재연결
 */
public class RpcClient {

    private static final Logger log = LoggerFactory.getLogger(RpcClient.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    // [2026-02-25] 최대 동시 in-flight 배치 수 (파이프라이닝 상한)
    private static final int MAX_INFLIGHT = 4;

    private final String targetId;
    private final RpcConfig config;
    private final com.icon.agent.config.TargetConfig targetConfig;
    private final SSLContext sslContext;
    private final SpoolManager spoolManager;
    /** Optional: persist CONFIG_UPDATE to config.yaml */
    private final TargetConfigPersister configPersister;
    /** Optional: apply COLLECTORS_SYNC to config.yaml */
    private final CollectorsSyncCallback collectorsSyncCallback;

    // [2026-04-22] 수집기 초기화 콜백 — COLLECTOR_RESET 수신 시 TargetContext.resetCollector() 호출
    private final CollectorResetCallback collectorResetCallback;

    /** config.yaml 최상단 agentId (내부통제시스템 등록용). null이면 targetId를 agentId로 사용 */
    private final String globalAgentId;

    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);

    // [2026-02-25] 스풀 재전송 중 플래그: true이면 일반 send()를 스풀로 우회
    private final AtomicBoolean spoolReplaying = new AtomicBoolean(false);

    private volatile WebSocket webSocket = null;

    // ACK 추적: batchId → CompletableFuture<Boolean>
    private final ConcurrentHashMap<String, CompletableFuture<Boolean>> pendingAcks = new ConcurrentHashMap<>();

    // [2026-02-25] WebSocket sendText() 직렬화 락 (Race Condition 방지)
    private final ReentrantLock wsSendLock = new ReentrantLock();

    // [2026-02-25] 동시 in-flight 배치 수 제한 세마포어 (파이프라이닝)
    private final Semaphore inflightSemaphore = new Semaphore(MAX_INFLIGHT);

    private final ThreadLocalRandom rng = ThreadLocalRandom.current();
    private int reconnectAttempt = 0;

    public RpcClient(String targetId,
            com.icon.agent.config.TargetConfig targetConfig,
            SSLContext sslContext,
            SpoolManager spoolManager) {
        this(targetId, targetConfig, sslContext, spoolManager, null, null);
    }

    public RpcClient(String targetId,
            com.icon.agent.config.TargetConfig targetConfig,
            SSLContext sslContext,
            SpoolManager spoolManager,
            TargetConfigPersister configPersister) {
        this(targetId, targetConfig, sslContext, spoolManager, configPersister, null);
    }

    public RpcClient(String targetId,
            com.icon.agent.config.TargetConfig targetConfig,
            SSLContext sslContext,
            SpoolManager spoolManager,
            TargetConfigPersister configPersister,
            CollectorsSyncCallback collectorsSyncCallback) {
        this(targetId, targetConfig, sslContext, spoolManager, configPersister, collectorsSyncCallback, null);
    }

    public RpcClient(String targetId,
            com.icon.agent.config.TargetConfig targetConfig,
            SSLContext sslContext,
            SpoolManager spoolManager,
            TargetConfigPersister configPersister,
            CollectorsSyncCallback collectorsSyncCallback,
            String globalAgentId) {
        this(targetId, targetConfig, sslContext, spoolManager, configPersister,
                collectorsSyncCallback, null, globalAgentId);
    }

    // [2026-04-22] CollectorResetCallback 추가 생성자
    public RpcClient(String targetId,
            com.icon.agent.config.TargetConfig targetConfig,
            SSLContext sslContext,
            SpoolManager spoolManager,
            TargetConfigPersister configPersister,
            CollectorsSyncCallback collectorsSyncCallback,
            CollectorResetCallback collectorResetCallback,
            String globalAgentId) {
        this.targetId = targetId;
        this.targetConfig = targetConfig;
        this.config = targetConfig.getRpc();
        this.sslContext = sslContext;
        this.spoolManager = spoolManager;
        this.configPersister = configPersister;
        this.collectorsSyncCallback = collectorsSyncCallback;
        this.collectorResetCallback = collectorResetCallback;
        this.globalAgentId = globalAgentId;
    }

    /** Callback to persist CONFIG_UPDATE to targets.json */
    @FunctionalInterface
    public interface TargetConfigPersister {
        // [2026-04-22] maxBatchesPerSecond 파라미터 추가
        void persist(String targetId, String rpcEndpoint, boolean compress,
                     String tlsKeystorePath, String tlsKeystorePassword,
                     String tlsTruststorePath, String tlsTruststorePassword,
                     int queueCapacity, int maxBatchSize, long maxBatchMs, long maxBatchBytes,
                     int maxBatchesPerSecond);
    }

    /** Callback to apply COLLECTORS_SYNC to config.yaml */
    @FunctionalInterface
    public interface CollectorsSyncCallback {
        void onCollectorsSync(String targetId, java.util.List<com.icon.agent.config.CollectorConfig> collectors);
    }

    // [2026-04-22] 수집기 초기화 콜백 — COLLECTOR_RESET 메시지 수신 시 호출
    @FunctionalInterface
    public interface CollectorResetCallback {
        void onCollectorReset(String targetId, String collectorId);
    }

    /** 연결 시작 (논블로킹) */
    public void connect() {
        running.set(true);
        scheduleConnect(0);
    }

    private void scheduleConnect(long delayMs) {
        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS)
                .execute(this::doConnect);
    }

    private void doConnect() {
        if (!running.get())
            return;
        MDC.put("targetId", targetId);
        try {
            log.info("[{}] 연결 중: {}", targetId, config.getEndpoint());
            HttpClient client = HttpClient.newBuilder()
                    // .sslContext(sslContext)
                    
                    .build();

            WebSocket ws = client.newWebSocketBuilder()
                    .buildAsync(URI.create(config.getEndpoint()), new WsListener())
                    .get(30, TimeUnit.SECONDS);

            this.webSocket = ws;
            connected.set(true);
            reconnectAttempt = 0;
            log.info("[{}] WebSocket 연결 완료: {}", targetId, config.getEndpoint());
            // [2026-04-21] 감사 로그: 연결 성공
            AuditLogger.targetConnected(targetId, config.getEndpoint());

            // 재연결 후 스풀 재전송
            replaySpool();

        } catch (Exception e) {
            StringBuilder msg = new StringBuilder().append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
            Throwable cause = e.getCause();
            if (cause != null && cause != e) {
                msg.append(" (cause: ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage()).append(")");
            }
            log.error("[{}] WebSocket 연결 실패: {}", targetId, msg);
            if (log.isDebugEnabled()) {
                log.debug("[{}] 연결 실패 상세", targetId, e);
            }
            connected.set(false);
            handleReconnect();
        } finally {
            MDC.remove("targetId");
        }
    }

    /**
     * 배치를 전송한다. 미연결/스풀 재전송 중/슬롯 초과 시 스풀에 저장한다.
     * [2026-02-25] 비동기 ACK 처리 + 파이프라이닝 지원
     *              스풀 재전송 중(spoolReplaying=true)이면 스풀로 우회 → 전송 순서 보장
     */
    public boolean send(Batch batch) {
        if (!connected.get() || webSocket == null) {
            log.warn("[{}] 미연결 — 스풀 저장: {}", targetId, batch.getBatchId());
            spoolBatch(batch);
            return false;
        }

        // [2026-02-25] 스풀 재전송 중이면 일반 전송 우회 — 재전송 완료 후 순서 보장
        if (spoolReplaying.get()) {
            log.debug("[{}] 스풀 재전송 진행 중 — 스풀 저장 (재전송 완료 후 순서 유지): {}",
                    targetId, batch.getBatchId());
            spoolBatch(batch);
            return false;
        }

        // [2026-02-25] 인플라이트 슬롯 획득 (MAX_INFLIGHT 초과 시 최대 5초 대기)
        try {
            if (!inflightSemaphore.tryAcquire(5, TimeUnit.SECONDS)) {
                log.warn("[{}] 인플라이트 한도({}) 초과 — 스풀 저장: {}", targetId, MAX_INFLIGHT, batch.getBatchId());
                spoolBatch(batch);
                return false;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            spoolBatch(batch);
            return false;
        }

        // [2026-02-25] 중복 처리 방지 가드 (ACK 타임아웃과 전송 오류 동시 발생 대비)
        AtomicBoolean handled = new AtomicBoolean(false);
        CompletableFuture<Boolean> ackFuture = new CompletableFuture<>();
        pendingAcks.put(batch.getBatchId(), ackFuture);

        // [2026-02-25] 비동기 ACK 처리: ACK 타임아웃/NACK 시 스풀 저장 + 슬롯 반환
        ackFuture
            .completeOnTimeout(false, config.getAckTimeoutMs(), TimeUnit.MILLISECONDS)
            .thenAccept(acked -> {
                if (handled.compareAndSet(false, true)) {
                    inflightSemaphore.release();
                    if (!acked) {
                        pendingAcks.remove(batch.getBatchId());
                        log.warn("[{}] ACK 타임아웃/NACK — 스풀 저장: {}", targetId, batch.getBatchId());
                        spoolBatch(batch);
                    }
                }
            });

        // wsSendLock으로 sendText/sendBinary() 직렬화 — 동시 전송 호출 방지
        wsSendLock.lock();
        try {
            String json = serializeBatch(batch);
            CompletableFuture<WebSocket> sendFuture;
            // [2026-03-05] rpc.compress=true: GZIP 압축 후 binary frame 전송
            if (config.isCompress()) {
                byte[] compressed = gzip(json);
                sendFuture = webSocket.sendBinary(ByteBuffer.wrap(compressed), true);
                log.debug("[{}] 배치 전송(GZIP binary {}→{}bytes, ACK 비동기): {}",
                        targetId, json.length(), compressed.length, batch.getBatchId());
            } else {
                // [2026-03-05] rpc.compress=false: 기존 text frame 전송 유지
                sendFuture = webSocket.sendText(json, true);
                log.debug("[{}] 배치 전송 (text, ACK 비동기 대기 중): {}", targetId, batch.getBatchId());
            }
            sendFuture.exceptionally(ex -> {
                    // sendText/sendBinary 실패 시 즉시 처리 (타임아웃 핸들러 중복 방지)
                    if (handled.compareAndSet(false, true)) {
                        inflightSemaphore.release();
                        ackFuture.complete(false);
                        pendingAcks.remove(batch.getBatchId());
                        log.error("[{}] 전송 오류 — 스풀 저장: {} ({})",
                                targetId, batch.getBatchId(), ex.getMessage());
                        spoolBatch(batch);
                        connected.set(false);
                        handleReconnect();
                    }
                    return null;
                });
            return true;
        } catch (Exception e) {
            if (handled.compareAndSet(false, true)) {
                inflightSemaphore.release();
                ackFuture.complete(false);
                pendingAcks.remove(batch.getBatchId());
                spoolBatch(batch);
                connected.set(false);
                handleReconnect();
            }
            return false;
        } finally {
            wsSendLock.unlock();
        }
    }

    /**
     * 스풀 재전송 전용 메서드. 실패 시 재스풀하지 않는다.
     * [2026-02-25] 스풀 파일 순서 보장을 위해 동기 ACK 대기 방식 유지
     *              wsSendLock으로 일반 send()와 sendText() 직렬화
     *              ACK 대기는 락 밖에서 수행 — 대기 중 일반 배치 전송 허용
     */
    private boolean sendFromSpool(Batch batch) {
        if (!connected.get() || webSocket == null) {
            log.warn("[{}] 미연결 — 스풀 재전송 불가, 원본 파일 유지: {}", targetId, batch.getBatchId());
            return false;
        }

        CompletableFuture<Boolean> ackFuture = new CompletableFuture<>();
        pendingAcks.put(batch.getBatchId(), ackFuture);

        // sendText/sendBinary만 락으로 보호, ACK 대기는 락 밖에서 수행
        wsSendLock.lock();
        try {
            String json = serializeBatch(batch);
            if (config.isCompress()) {
                byte[] compressed = gzip(json);
                webSocket.sendBinary(ByteBuffer.wrap(compressed), true).get(10, TimeUnit.SECONDS);
            } else {
                webSocket.sendText(json, true).get(10, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            pendingAcks.remove(batch.getBatchId());
            ackFuture.complete(false);
            log.error("[{}] 스풀 재전송 전송 오류 — 원본 스풀 파일 유지: {} ({})",
                    targetId, batch.getBatchId(), e.getMessage());
            connected.set(false);
            handleReconnect();
            return false;
        } finally {
            // [2026-02-25] sendText 완료 즉시 락 해제 — ACK 대기 중 다른 재전송 불필요한 블로킹 방지
            wsSendLock.unlock();
        }

        // ACK 동기 대기 (락 밖에서 수행)
        try {
            boolean acked = ackFuture.get(config.getAckTimeoutMs(), TimeUnit.MILLISECONDS);
            if (acked) {
                log.debug("[{}] 스풀 재전송 ACK 수신: {}", targetId, batch.getBatchId());
            }
            return acked;
        } catch (TimeoutException e) {
            pendingAcks.remove(batch.getBatchId());
            log.warn("[{}] 스풀 재전송 ACK 타임아웃 — 원본 스풀 파일 유지: {}", targetId, batch.getBatchId());
            return false;
        } catch (Exception e) {
            pendingAcks.remove(batch.getBatchId());
            log.error("[{}] 스풀 재전송 ACK 대기 오류 — 원본 스풀 파일 유지: {} ({})",
                    targetId, batch.getBatchId(), e.getMessage());
            return false;
        }
    }

    /**
     * 배치를 JSON 문자열로 직렬화한다.
     * [2026-02-25] 4번: 스트리밍 직렬화로 중간 복사본 제거 — 메모리 사용량 감소
     *
     * 기존 방식 (메모리 3벌):
     *   Batch → ObjectNode(valueToTree 복사) → JSON String → sendText
     * 변경 방식 (메모리 ~1벌):
     *   Batch → StringWriter(JsonGenerator 스트리밍) → JSON String → sendText
     *
     * ObjectNode 중간 복사본을 제거하여 GC 부하와 메모리 피크 감소.
     * Java 표준 WebSocket API가 String을 요구하므로 최종 String 변환은 불가피.
     */
    private String serializeBatch(Batch batch) throws Exception {
        StringWriter sw = new StringWriter();
        try (com.fasterxml.jackson.core.JsonGenerator gen = JSON.createGenerator(sw)) {
            gen.writeStartObject();
            gen.writeStringField("batchId", batch.getBatchId());
            gen.writeStringField("targetId", batch.getTargetId());
            gen.writeNumberField("createdAt", batch.getCreatedAt());
            gen.writeFieldName("records");
            JSON.writeValue(gen, batch.getRecords());
            gen.writeEndObject();
        }
        return sw.toString();
    }

    /**
     * [2026-03-05] JSON 문자열을 GZIP으로 압축하여 byte[]로 반환한다.
     * send() / sendFromSpool()에서 rpc.compress=true 인 경우 호출.
     * 서버는 binary WebSocket frame 수신 시 GZIP 해제 후 JSON으로 파싱한다.
     *   text  frame → 비압축 JSON (compress=false)
     *   binary frame → GZIP(JSON) (compress=true)
     */
    private byte[] gzip(String json) throws Exception {
        byte[] input = json.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(input.length);
        try (GZIPOutputStream gzos = new GZIPOutputStream(bos)) {
            gzos.write(input);
        }
        return bos.toByteArray();
    }

    private void spoolBatch(Batch batch) {
        try {
            spoolManager.write(batch);
        } catch (Exception e) {
            log.error("[{}] 스풀 저장 실패: {} ({})", targetId, batch.getBatchId(), e.getMessage());
        }
    }

    /**
     * 스풀에 저장된 배치를 순서대로 재전송한다.
     * [2026-02-25] spoolReplaying 플래그로 일반 send()를 스풀 우회 → 전송 순서 보장
     *              재전송 중 일반 배치는 스풀에 저장되며, 다음 재연결 시 순서대로 재전송됨
     */
    private void replaySpool() {
        try {
            List<SpoolEntry> pending = spoolManager.listPending();
            if (pending.isEmpty())
                return;

            log.info("[{}] 스풀 재전송 시작: {}건", targetId, pending.size());

            // [2026-02-25] 재전송 중 일반 send()를 스풀 우회로 전환 (순서 보장)
            spoolReplaying.set(true);

            for (SpoolEntry entry : pending) {
                try {
                    Batch batch = spoolManager.read(entry);
                    boolean ok = sendFromSpool(batch);
                    if (ok) {
                        spoolManager.ack(entry);
                        log.debug("[{}] 스풀 파일 삭제 완료: {}", targetId, entry.getName());
                    } else {
                        log.warn("[{}] 스풀 재전송 실패, 다음 재연결 시 재시도: {}", targetId, entry.getName());
                        // [2026-02-25] 연결 끊김 시 나머지는 재연결 후 처리
                        if (!connected.get()) {
                            log.info("[{}] 연결 끊김 — 스풀 재전송 중단, 재연결 후 재시도", targetId);
                            break;
                        }
                    }
                } catch (Exception e) {
                    log.error("[{}] 스풀 항목 재전송 오류: {} ({})", targetId, entry.getName(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("[{}] 스풀 재전송 전체 오류: {}", targetId, e.getMessage());
        } finally {
            // [2026-02-25] 재전송 완료 — 일반 send() 재개
            spoolReplaying.set(false);
            log.info("[{}] 스풀 재전송 완료 — 일반 전송 재개", targetId);
        }
    }

    private void handleReconnect() {
        if (!running.get())
            return;

        // [2026-02-25] 미완료 ACK 즉시 실패 처리 → 세마포어 즉시 반환 (ackTimeoutMs 지연 방지)
        cancelPendingAcks();

        reconnectAttempt++;
        long delay = Math.min(
                config.getReconnectBaseMs() * (1L << Math.min(reconnectAttempt, 10)),
                config.getReconnectMaxMs());
        // ±20% jitter
        long jitter = (long) (delay * 0.2 * (rng.nextDouble() * 2 - 1));
        long actualDelay = Math.max(100, delay + jitter);
        log.info("[{}] {}ms 후 재연결 시도 ({}회차)", targetId, actualDelay, reconnectAttempt);
        scheduleConnect(actualDelay);
    }

    /**
     * 미완료 ACK를 모두 false로 즉시 완료 처리한다.
     * [2026-02-25] 연결 끊김 시 호출 — ackTimeoutMs(최대 30초) 대기 없이 세마포어 즉시 반환
     *              각 배치의 thenAccept 콜백이 실행되어 스풀 저장 + 슬롯 반환 처리됨
     */
    private void cancelPendingAcks() {
        int count = pendingAcks.size();
        if (count > 0) {
            log.warn("[{}] 연결 끊김 — 미완료 ACK {}건 즉시 실패 처리 (스풀 저장)", targetId, count);
            pendingAcks.forEach((batchId, future) -> future.complete(false));
            pendingAcks.clear();
        }
    }

    private void sendHandshake(WebSocket ws) {
        try {
            String hostname = "unknown";
            String ipAddress = "unknown";
            try {
                java.net.InetAddress addr = java.net.InetAddress.getLocalHost();
                hostname  = addr.getHostName();
                ipAddress = addr.getHostAddress();
            } catch (Exception ignored) {}

            String version = RpcClient.class.getPackage() != null
                    ? RpcClient.class.getPackage().getImplementationVersion() : null;
            if (version == null) version = "unknown";

            com.icon.agent.config.TlsConfig tls = config.getTls();

            StringWriter sw = new StringWriter();
            try (com.fasterxml.jackson.core.JsonGenerator gen = JSON.createGenerator(sw)) {
                gen.writeStartObject();
                gen.writeStringField("type",             "HANDSHAKE");
                gen.writeStringField("agentId",          globalAgentId != null && !globalAgentId.isBlank() ? globalAgentId : targetId);
                gen.writeStringField("targetId",         targetId);
                gen.writeStringField("hostname",         hostname);
                gen.writeStringField("ipAddress",        ipAddress);
                gen.writeStringField("agentVersion",     version);
                gen.writeStringField("osInfo",
                        System.getProperty("os.name", "unknown") + " " +
                        System.getProperty("os.version", ""));
                // target config from config.yaml
                gen.writeStringField("rpcEndpoint",      config.getEndpoint() != null ? config.getEndpoint() : "");
                gen.writeBooleanField("compress",        config.isCompress());
                gen.writeStringField("tlsKeystorePath",      tls != null && tls.getKeystorePath() != null  ? tls.getKeystorePath()  : "");
                gen.writeStringField("tlsKeystorePassword",  tls != null && tls.getKeystorePassword() != null ? tls.getKeystorePassword() : "");
                gen.writeStringField("tlsTruststorePath",    tls != null && tls.getTruststorePath() != null ? tls.getTruststorePath() : "");
                gen.writeStringField("tlsTruststorePassword",tls != null && tls.getTruststorePassword() != null ? tls.getTruststorePassword() : "");
                gen.writeNumberField("queueCapacity",       targetConfig.getQueueCapacity());
                gen.writeNumberField("maxBatchSize",        targetConfig.getMaxBatchSize());
                gen.writeNumberField("maxBatchMs",          targetConfig.getMaxBatchMs());
                gen.writeNumberField("maxBatchBytes",       targetConfig.getMaxBatchBytes());
                // [2026-04-22] maxBatchesPerSecond HANDSHAKE에 추가
                gen.writeNumberField("maxBatchesPerSecond", targetConfig.getMaxBatchesPerSecond());
                gen.writeEndObject();
            }
            ws.sendText(sw.toString(), true);
            log.info("[{}] HANDSHAKE 전송 완료 (endpoint={}, compress={}, maxBatchSize={})",
                    targetId, config.getEndpoint(), config.isCompress(), targetConfig.getMaxBatchSize());
        } catch (Exception e) {
            log.warn("[{}] HANDSHAKE 전송 실패: {}", targetId, e.getMessage());
        }
    }

    public void disconnect() {
        running.set(false);
        connected.set(false);
        // [2026-02-25] 종료 시 미완료 ACK 즉시 처리
        cancelPendingAcks();
        WebSocket ws = this.webSocket;
        if (ws != null && !ws.isInputClosed()) {
            ws.sendClose(WebSocket.NORMAL_CLOSURE, "shutdown").join();
        }
        log.info("[{}] RpcClient 연결 해제", targetId);
    }

    public boolean isConnected() {
        return connected.get();
    }

    // -----------------------------------------------------------------------
    // WebSocket Listener
    // -----------------------------------------------------------------------
    private class WsListener implements WebSocket.Listener {
        private final StringBuilder textBuffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket ws) {
            log.info("[{}] WebSocket 열림", targetId);
            ws.request(1);
            sendHandshake(ws);
        }

        @Override
        public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
            textBuffer.append(data);
            if (last) {
                handleMessage(textBuffer.toString());
                textBuffer.setLength(0);
            }
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onPing(WebSocket ws, ByteBuffer message) {
            ws.sendPong(message);
            ws.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) {
            log.warn("[{}] WebSocket 닫힘: {} {}", targetId, statusCode, reason);
            connected.set(false);
            // [2026-04-21] 감사 로그: 연결 끊김
            AuditLogger.targetDisconnected(targetId, statusCode + " " + reason);
            handleReconnect();
            return null;
        }

        @Override
        public void onError(WebSocket ws, Throwable error) {
            log.error("[{}] WebSocket 오류: {}", targetId, error.getMessage());
            connected.set(false);
            // [2026-04-21] 감사 로그: 오류로 인한 연결 끊김
            AuditLogger.targetDisconnected(targetId, "error: " + error.getMessage());
            handleReconnect();
        }

        private void handleMessage(String message) {
            try {
                JsonNode node = JSON.readTree(message);
                String type = node.path("type").asText("");

                if ("CONFIG_UPDATE".equals(type)) {
                    handleConfigUpdate(node);
                    return;
                }
                if ("COLLECTORS_SYNC".equals(type)) {
                    handleCollectorsSync(node);
                    return;
                }
                // [2026-04-22] 수집기 초기화 — 위치 파일 삭제 후 재시작
                if ("COLLECTOR_RESET".equals(type)) {
                    handleCollectorReset(node);
                    return;
                }
                // [2026-04-21] 서버가 agentId 중복을 감지하면 HANDSHAKE_NACK 반환 → 재연결 중단
                if ("HANDSHAKE_NACK".equals(type)) {
                    String reason = node.path("reason").asText("DUPLICATE_AGENT_ID");
                    log.error("[{}] HANDSHAKE_NACK 수신 (reason={}). " +
                            "agentId 중복 — data/agent.id 파일을 삭제하거나 config.yaml의 agentId를 고유하게 설정하세요.",
                            targetId, reason);
                    AuditLogger.targetDisconnected(targetId, "HANDSHAKE_NACK:" + reason);
                    running.set(false);  // 재연결 시도 중단
                    return;
                }

                // ACK: {"ack": true, "batchId": "..."} or {"ack": "Y", "batchId": "..."}
                if (node.has("ack") && node.has("batchId")) {
                    String batchId = node.get("batchId").asText();
                    JsonNode ackNode = node.get("ack");
                    boolean ack = ackNode.isBoolean() ? ackNode.asBoolean()
                            : "Y".equalsIgnoreCase(ackNode.asText()) || "true".equalsIgnoreCase(ackNode.asText());
                    CompletableFuture<Boolean> future = pendingAcks.remove(batchId);
                    if (future != null) {
                        future.complete(ack);
                    } else {
                        log.warn("[{}] 알 수 없는 batchId ACK 수신: {}", targetId, batchId);
                    }
                }
            } catch (Exception e) {
                log.error("[{}] 서버 메시지 파싱 오류: {}", targetId, e.getMessage());
            }
        }

        private void handleConfigUpdate(JsonNode node) {
            String rpcEndpoint   = node.path("rpcEndpoint").asText(null);
            boolean compress     = node.path("compress").asBoolean(false);
            String tlsKp         = node.has("tlsKeystorePath")     ? node.get("tlsKeystorePath").asText(null)     : null;
            String tlsKpw        = node.has("tlsKeystorePassword") ? node.get("tlsKeystorePassword").asText(null) : null;
            String tlsTp         = node.has("tlsTruststorePath")   ? node.get("tlsTruststorePath").asText(null)   : null;
            String tlsTpw        = node.has("tlsTruststorePassword") ? node.get("tlsTruststorePassword").asText(null) : null;
            int queueCapacity    = node.path("queueCapacity").asInt(0);
            int maxBatchSize     = node.path("maxBatchSize").asInt(0);
            long maxBatchMs      = node.path("maxBatchMs").asLong(0);
            long maxBatchBytes   = node.path("maxBatchBytes").asLong(-1);
            // [2026-04-22] maxBatchesPerSecond 추가 (-1 = 미지정, 변경 없음)
            int maxBatchesPerSecond = node.path("maxBatchesPerSecond").asInt(-1);

            log.info("[{}] CONFIG_UPDATE 수신 - endpoint={}, maxBatchSize={}, maxBatchMs={}, maxBatchBytes={}, maxBatchesPerSecond={}, compress={}",
                    targetId, rpcEndpoint, maxBatchSize, maxBatchMs, maxBatchBytes, maxBatchesPerSecond, compress);

            if (configPersister != null) {
                try {
                    configPersister.persist(targetId, rpcEndpoint, compress,
                            tlsKp, tlsKpw, tlsTp, tlsTpw,
                            queueCapacity, maxBatchSize, maxBatchMs, maxBatchBytes,
                            maxBatchesPerSecond);
                } catch (Exception e) {
                    log.error("[{}] targets.json 반영 실패: {}", targetId, e.getMessage());
                }
            }
        }

        // [2026-04-22] COLLECTOR_RESET 수신 처리
        private void handleCollectorReset(JsonNode node) {
            String resetCollectorId = node.path("collectorId").asText(null);
            if (resetCollectorId == null || resetCollectorId.isBlank()) {
                log.warn("[{}] COLLECTOR_RESET: collectorId 누락", targetId);
                return;
            }
            log.info("[{}] COLLECTOR_RESET 수신 - collectorId={}", targetId, resetCollectorId);
            if (collectorResetCallback != null) {
                try {
                    collectorResetCallback.onCollectorReset(targetId, resetCollectorId);
                } catch (Exception e) {
                    log.error("[{}] COLLECTOR_RESET 처리 실패 - collectorId={}: {}",
                            targetId, resetCollectorId, e.getMessage(), e);
                }
            } else {
                log.warn("[{}] CollectorResetCallback 미등록 — COLLECTOR_RESET 무시", targetId);
            }
        }

        private void handleCollectorsSync(JsonNode node) {
            String syncTargetId = node.path("targetId").asText(null);
            if (syncTargetId == null || syncTargetId.isBlank()) {
                log.warn("[{}] COLLECTORS_SYNC missing targetId", targetId);
                return;
            }
            JsonNode arr = node.get("collectors");
            if (arr == null || !arr.isArray()) {
                log.warn("[{}] COLLECTORS_SYNC missing or invalid collectors array", targetId);
                return;
            }
            java.util.List<com.icon.agent.config.CollectorConfig> list = new java.util.ArrayList<>();
            for (JsonNode item : arr) {
                try {
                    String collectorType = item.path("type").asText("");
                    if ("FILE".equalsIgnoreCase(collectorType)) {
                        com.icon.agent.config.FileCollectorConfig fc = JSON.treeToValue(item, com.icon.agent.config.FileCollectorConfig.class);
                        list.add(fc);
                    } else if ("JDBC".equalsIgnoreCase(collectorType)) {
                        com.icon.agent.config.JdbcCollectorConfig jc = JSON.treeToValue(item, com.icon.agent.config.JdbcCollectorConfig.class);
                        list.add(jc);
                    }
                } catch (Exception e) {
                    log.warn("[{}] COLLECTORS_SYNC item parse error: {}", targetId, e.getMessage());
                }
            }
            log.info("[{}] COLLECTORS_SYNC 수신 - targetId={}, collectors={}", targetId, syncTargetId, list.size());
            if (collectorsSyncCallback != null) {
                try {
                    collectorsSyncCallback.onCollectorsSync(syncTargetId, list);
                } catch (Exception e) {
                    log.error("[{}] config.yaml collectors 반영 실패: {}", targetId, e.getMessage());
                }
            }
        }
    }
}
