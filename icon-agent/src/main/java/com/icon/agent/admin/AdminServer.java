package com.icon.agent.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.icon.agent.config.ConfigManager;
import com.icon.agent.config.RpcConfig;
import com.icon.agent.config.TargetConfig;
import com.icon.agent.config.TlsConfig;
import com.icon.agent.target.TargetManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * [2026-04-21] 에이전트 관리 HTTP API 서버 (localhost:8081 전용).
 *
 * CLI(agent-cli.ps1)가 이 서버에 HTTP 요청을 보내 대화형으로 target을 관리한다.
 * 외부 노출 차단: 127.0.0.1에만 바인딩.
 *
 * 엔드포인트:
 *   GET  /status           에이전트 상태
 *   GET  /targets          target 목록
 *   POST /targets          target 추가  body: {"id":"...","endpoint":"ws://..."}
 *   DELETE /targets/{id}   target 제거
 */
public class AdminServer {

    private static final Logger log = LoggerFactory.getLogger(AdminServer.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private final int port;
    private final TargetManager targetManager;
    private final ConfigManager configManager;
    private final long startedAt = System.currentTimeMillis();
    private HttpServer server;

    public AdminServer(int port, TargetManager targetManager, ConfigManager configManager) {
        this.port = port;
        this.targetManager = targetManager;
        this.configManager = configManager;
    }

    public void start() throws IOException {
        // [2026-04-21] 127.0.0.1 전용 바인딩 — 외부 접근 차단
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/status",  ex -> handle(ex, this::handleStatus));
        server.createContext("/targets", ex -> handle(ex, this::handleTargets));
        server.setExecutor(null);
        server.start();
        log.info("Admin server started on 127.0.0.1:{}", port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            log.info("Admin server stopped");
        }
    }

    // ── 라우팅 래퍼 ────────────────────────────────────────────────────────────

    @FunctionalInterface
    interface RouteHandler {
        void handle(HttpExchange ex) throws IOException;
    }

    private void handle(HttpExchange ex, RouteHandler handler) throws IOException {
        try {
            handler.handle(ex);
        } catch (Exception e) {
            log.error("Admin API 오류: {}", e.getMessage(), e);
            sendJson(ex, 500, Map.of("error", e.getMessage()));
        } finally {
            ex.close();
        }
    }

    // ── GET /status ────────────────────────────────────────────────────────────

    private void handleStatus(HttpExchange ex) throws IOException {
        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 405, Map.of("error", "Method Not Allowed")); return; }

        long uptimeSec = (System.currentTimeMillis() - startedAt) / 1000;
        String agentId = configManager != null ? configManager.getConfig().getAgentId() : "unknown";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",    "RUNNING");
        body.put("agentId",   agentId);
        body.put("uptimeSec", uptimeSec);
        body.put("targets",   targetManager.getContexts().size());
        sendJson(ex, 200, body);
    }

    // ── /targets ───────────────────────────────────────────────────────────────

    private void handleTargets(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase();
        String path   = ex.getRequestURI().getPath();       // /targets or /targets/{id} or /targets/{id}/collectors

        // [2026-04-21] /targets/{id}/collectors 처리
        if (path.endsWith("/collectors")) {
            String withoutSuffix = path.substring("/targets/".length(), path.length() - "/collectors".length());
            if (!withoutSuffix.isEmpty()) {
                if ("GET".equals(method)) { handleListCollectors(ex, withoutSuffix); }
                else { sendJson(ex, 405, Map.of("error", "Method Not Allowed")); }
                return;
            }
        }

        boolean hasId = path.length() > "/targets/".length();
        if (hasId) {
            String id = path.substring("/targets/".length()).trim();
            switch (method) {
                case "GET"    -> handleGetTarget(ex, id);
                case "DELETE" -> handleRemoveTarget(ex, id);
                default       -> sendJson(ex, 405, Map.of("error", "Method Not Allowed"));
            }
            return;
        }

        switch (method) {
            case "GET"  -> handleListTargets(ex);
            case "POST" -> handleAddTarget(ex);
            default     -> sendJson(ex, 405, Map.of("error", "Method Not Allowed"));
        }
    }

    private void handleListTargets(HttpExchange ex) throws IOException {
        List<Map<String, Object>> list = targetManager.getContexts().stream()
                .map(ctx -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id",          ctx.getTargetId());
                    m.put("endpoint",    ctx.getEndpoint());
                    m.put("connected",   ctx.isRpcConnected());
                    m.put("queueDepth",  ctx.getQueueDepth());
                    m.put("spoolDepth",  ctx.getSpoolDepth());
                    return m;
                })
                .toList();
        sendJson(ex, 200, list);
    }

    // [2026-04-21] GET /targets/{id} — target 전체 설정 조회 (target show 용)
    private void handleGetTarget(HttpExchange ex, String id) throws IOException {
        var ctx = targetManager.getContexts().stream()
                .filter(c -> c.getTargetId().equals(id))
                .findFirst().orElse(null);
        if (ctx == null) { sendJson(ex, 404, Map.of("error", "target '" + id + "' not found")); return; }

        var tc  = ctx.getConfig();
        var rpc = tc.getRpc();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                 tc.getId());
        m.put("endpoint",           rpc != null ? rpc.getEndpoint() : "");
        m.put("compress",           rpc != null && rpc.isCompress());
        m.put("reconnectBaseMs",    rpc != null ? rpc.getReconnectBaseMs() : 1000);
        m.put("reconnectMaxMs",     rpc != null ? rpc.getReconnectMaxMs()  : 60000);
        m.put("ackTimeoutMs",       rpc != null ? rpc.getAckTimeoutMs()    : 30000);
        m.put("queueCapacity",      tc.getQueueCapacity());
        m.put("maxBatchSize",       tc.getMaxBatchSize());
        m.put("maxBatchMs",         tc.getMaxBatchMs());
        m.put("maxBatchBytes",      tc.getMaxBatchBytes());
        m.put("maxSpoolFiles",      tc.getMaxSpoolFiles());
        m.put("maxSpoolSizeMb",     tc.getMaxSpoolSizeMb());
        m.put("maxBatchesPerSecond",tc.getMaxBatchesPerSecond());
        if (rpc != null && rpc.getTls() != null) {
            var tls = rpc.getTls();
            Map<String, Object> tlsMap = new LinkedHashMap<>();
            tlsMap.put("insecureTrustAll", tls.isInsecureTrustAll());
            if (tls.getKeystorePath()   != null) tlsMap.put("keystorePath",       tls.getKeystorePath());
            if (tls.getTruststorePath() != null) tlsMap.put("truststorePath",     tls.getTruststorePath());
            m.put("tls", tlsMap);
        }
        sendJson(ex, 200, m);
    }

    // [2026-04-21] GET /targets/{id}/collectors — 수집기 상태 및 설정 조회
    private void handleListCollectors(HttpExchange ex, String id) throws IOException {
        var ctx = targetManager.getContexts().stream()
                .filter(c -> c.getTargetId().equals(id))
                .findFirst().orElse(null);
        if (ctx == null) { sendJson(ex, 404, Map.of("error", "target '" + id + "' not found")); return; }
        sendJson(ex, 200, ctx.getCollectorInfos());
    }

    // [2026-04-21] TLS/배치/큐/스풀/속도제한 전체 파라미터 수신
    @SuppressWarnings("unchecked")
    private void handleAddTarget(HttpExchange ex) throws IOException {
        String bodyStr = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> req = JSON.readValue(bodyStr, Map.class);

        String id       = (String) req.get("id");
        String endpoint = (String) req.get("endpoint");
        if (id == null || id.isBlank() || endpoint == null || endpoint.isBlank()) {
            sendJson(ex, 400, Map.of("error", "id and endpoint are required")); return;
        }

        RpcConfig rpc = new RpcConfig();
        rpc.setEndpoint(endpoint);
        rpc.setCompress(Boolean.TRUE.equals(req.get("compress")));
        if (req.get("reconnectBaseMs") instanceof Number n) rpc.setReconnectBaseMs(n.longValue());
        if (req.get("reconnectMaxMs")  instanceof Number n) rpc.setReconnectMaxMs(n.longValue());
        if (req.get("ackTimeoutMs")    instanceof Number n) rpc.setAckTimeoutMs(n.longValue());

        // TLS 설정 (선택)
        Map<String, Object> tlsReq = (Map<String, Object>) req.get("tls");
        if (tlsReq != null) {
            TlsConfig tls = new TlsConfig();
            if (tlsReq.get("keystorePath")       instanceof String s) tls.setKeystorePath(s);
            if (tlsReq.get("keystorePassword")   instanceof String s) tls.setKeystorePassword(s);
            if (tlsReq.get("truststorePath")     instanceof String s) tls.setTruststorePath(s);
            if (tlsReq.get("truststorePassword") instanceof String s) tls.setTruststorePassword(s);
            if (tlsReq.get("keystoreType")       instanceof String s) tls.setKeystoreType(s);
            tls.setInsecureTrustAll(Boolean.TRUE.equals(tlsReq.get("insecureTrustAll")));
            rpc.setTls(tls);
        }

        TargetConfig tc = new TargetConfig();
        tc.setId(id);
        tc.setRpc(rpc);
        if (req.get("queueCapacity")      instanceof Number n) tc.setQueueCapacity(n.intValue());
        if (req.get("maxBatchSize")        instanceof Number n) tc.setMaxBatchSize(n.intValue());
        if (req.get("maxBatchMs")          instanceof Number n) tc.setMaxBatchMs(n.longValue());
        if (req.get("maxBatchBytes")       instanceof Number n) tc.setMaxBatchBytes(n.longValue());
        if (req.get("maxSpoolFiles")       instanceof Number n) tc.setMaxSpoolFiles(n.intValue());
        if (req.get("maxSpoolSizeMb")      instanceof Number n) tc.setMaxSpoolSizeMb(n.longValue());
        if (req.get("maxBatchesPerSecond") instanceof Number n) tc.setMaxBatchesPerSecond(n.intValue());

        // storeSync: CONFIG_UPDATE 수신 시에도 targets.json 갱신
        Runnable storeSync = () -> TargetStore.save(targetManager.getManagedTargetConfigs());
        String result = targetManager.addTarget(tc, configManager, storeSync);
        if ("already-exists".equals(result)) {
            sendJson(ex, 409, Map.of("error", "target '" + id + "' already exists")); return;
        }

        TargetStore.save(targetManager.getManagedTargetConfigs());
        sendJson(ex, 200, Map.of("ok", true, "id", id, "endpoint", endpoint));
    }

    private void handleRemoveTarget(HttpExchange ex, String id) throws IOException {
        if (!"DELETE".equalsIgnoreCase(ex.getRequestMethod())) { sendJson(ex, 405, Map.of("error", "Method Not Allowed")); return; }

        boolean removed = targetManager.removeTarget(id);
        if (!removed) {
            sendJson(ex, 404, Map.of("error", "target '" + id + "' 를 찾을 수 없습니다")); return;
        }

        TargetStore.save(targetManager.getManagedTargetConfigs());
        sendJson(ex, 200, Map.of("ok", true, "id", id));
    }

    // ── 공통 응답 ──────────────────────────────────────────────────────────────

    private void sendJson(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = JSON.writerWithDefaultPrettyPrinter().writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
