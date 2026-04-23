package com.itmasters.icon.rpcserver.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.service.SingleIngestService;
import com.itmasters.icon.rpc.agent.application.dto.AgentDto;
import com.itmasters.icon.rpc.agent.application.dto.AgentTargetConfigDto;
import com.itmasters.icon.rpc.agent.application.service.AgentRegistrationService;
import com.itmasters.icon.rpc.agent.application.service.AgentTargetConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

/**
 * Agent WebSocket RPC handler.
 *
 * Responsibilities:
 *   - Auto-register agent on HANDSHAKE message
 *   - Receive batch data (JSON text or GZIP binary) and return ACK
 *   - Track session connect/disconnect -> agents, agent_sessions tables
 *
 * Protocol:
 *   Agent -> Server (HANDSHAKE): { "type": "HANDSHAKE", "agentId": "...", "hostname": "...", ... }
 *   Agent -> Server (batch):     { "batchId": "...", "targetId": "...", "records": [...] }
 *   Server -> Agent (ACK):       { "ack": true, "batchId": "..." }
 */
@Slf4j
@Component
public class AgentRpcWebSocketHandler extends AbstractWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final AgentRegistrationService registrationService;
    // [2026-03-12] 에이전트 배치 → 파이프라인 실행
    private final SingleIngestService singleIngestService;
    // [2026-04-21] HANDSHAKE 후 DB target config → CONFIG_UPDATE 자동 푸시
    private final AgentTargetConfigService targetConfigService;
    // [2026-04-21] HANDSHAKE_ACK 후 AgentConnectedEvent 발행 → AgentSnapshotService가 COLLECTORS_SYNC 푸시
    private final ApplicationEventPublisher eventPublisher;

    /** sessionId -> agentId */
    private final Map<String, String> sessionAgentMap = new ConcurrentHashMap<>();
    /** agentId -> active WebSocketSession (for config push) */
    private final Map<String, WebSocketSession> agentSessionMap = new ConcurrentHashMap<>();

    public AgentRpcWebSocketHandler(ObjectMapper objectMapper,
                                    AgentRegistrationService registrationService,
                                    SingleIngestService singleIngestService,
                                    AgentTargetConfigService targetConfigService,
                                    ApplicationEventPublisher eventPublisher) {
        this.objectMapper = objectMapper;
        this.registrationService = registrationService;
        this.singleIngestService = singleIngestService;
        this.targetConfigService = targetConfigService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[RPC] Agent connected - sessionId={}, remote={}",
                session.getId(), session.getRemoteAddress());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        processPayload(session, message.getPayload());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        // GZIP binary frame (compress=true)
        byte[] compressed = message.getPayload().array();
        String json = decompress(compressed);
        processPayload(session, json);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        String agentId = sessionAgentMap.remove(sessionId);
        if (agentId != null) {
            agentSessionMap.remove(agentId);
        }
        log.info("[RPC] Agent disconnected - sessionId={}, agentId={}, status={}",
                sessionId, agentId, status);
        registrationService.handleDisconnect(sessionId, status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[RPC] Transport error - sessionId={}: {}", session.getId(), exception.getMessage());
    }

    // -----------------------------------------------------------------------

    private void processPayload(WebSocketSession session, String payload) throws IOException {
        JsonNode root = objectMapper.readTree(payload);
        String type = root.path("type").asText("");
        log.info("[RPC] processPayload - sessionId={}, root={}", session.getId(), root);
        if ("HANDSHAKE".equals(type)) {
            handleHandshake(session, root);
        } else if ("HEARTBEAT".equals(type)) {
            registrationService.updateHeartbeat(session.getId());
            log.debug("[RPC] HEARTBEAT - sessionId={}", session.getId());
        } else {
            handleBatch(session, root);
        }
    }

    private void handleHandshake(WebSocketSession session, JsonNode root) throws IOException {
        AgentDto.HandshakeMessage msg = objectMapper.treeToValue(root, AgentDto.HandshakeMessage.class);

        String remoteAddress = session.getRemoteAddress() != null
                ? session.getRemoteAddress().toString() : "unknown";

        AgentDto.Info info = registrationService.processHandshake(msg, session.getId(), remoteAddress);
        sessionAgentMap.put(session.getId(), info.getAgentId());
        agentSessionMap.put(info.getAgentId(), session);

        String ack = objectMapper.writeValueAsString(Map.of(
                "type",    "HANDSHAKE_ACK",
                "agentId", info.getAgentId(),
                "status",  info.getStatus().name()
        ));
        session.sendMessage(new TextMessage(ack));
        log.info("[RPC] HANDSHAKE_ACK sent - agentId={}", info.getAgentId());

        // [2026-04-21] DB에 저장된 target config를 에이전트에 즉시 푸시
        //              에이전트 재기동 시 UI에서 변경한 설정이 자동으로 적용됨
        pushStoredConfigsOnHandshake(info.getAgentId());

        // [2026-04-21] AgentConnectedEvent 발행 → AgentSnapshotService.onAgentConnected()가
        //              COLLECTORS_SYNC를 에이전트에 자동 푸시 (수집기 목록 동기화)
        eventPublisher.publishEvent(new AgentConnectedEvent(info.getAgentId()));
    }

    private void pushStoredConfigsOnHandshake(String agentId) {
        try {
            java.util.List<AgentTargetConfigDto.Info> configs = targetConfigService.findAll(agentId);
            for (AgentTargetConfigDto.Info cfg : configs) {
                Map<String, Object> payload = buildConfigPayload(cfg);
                pushConfigUpdate(agentId, payload);
                log.info("[RPC] HANDSHAKE 후 CONFIG_UPDATE 자동 푸시 - agentId={}, targetId={}",
                        agentId, cfg.getTargetId());
            }
        } catch (Exception e) {
            log.warn("[RPC] HANDSHAKE 후 CONFIG_UPDATE 푸시 실패 - agentId={}: {}", agentId, e.getMessage());
        }
    }

    private void handleBatch(WebSocketSession session, JsonNode root) throws IOException {
        // Auto-register agent if not yet registered (fallback: no HANDSHAKE case)
        if (!sessionAgentMap.containsKey(session.getId())) {
            String targetId = root.path("targetId").asText(null);
            if (targetId != null && !targetId.isBlank()) {
                log.info("[RPC] Auto-registering agent from batch - targetId={}", targetId);
                AgentDto.HandshakeMessage fallback = buildFallbackHandshake(targetId, session);
                AgentDto.Info info = registrationService.processHandshake(
                        fallback, session.getId(),
                        session.getRemoteAddress() != null ? session.getRemoteAddress().toString() : "unknown");
                sessionAgentMap.put(session.getId(), info.getAgentId());
            }
        }

        String agentId = sessionAgentMap.getOrDefault(session.getId(), "agent");
        String batchId = root.has("batchId") ? root.get("batchId").asText("") : "";
        log.debug("[RPC] Batch received - sessionId={}, agentId={}, batchId={}", session.getId(), agentId, batchId);

        // [2026-03-12] records 파싱 → collectorId(="DS_"+dataSourceId) 기준으로 그룹화 → pipeline 실행
        JsonNode recordsNode = root.path("records");
        if (recordsNode.isArray() && recordsNode.size() > 0) {
            // collectorId → rows 그룹핑 (한 배치에 여러 collector의 records가 섞일 수 있음)
            Map<String, List<Map<String, Object>>> byDataSource = new LinkedHashMap<>();
            for (JsonNode rec : recordsNode) {
                String collectorId = rec.path("collectorId").asText(null);
                if (collectorId == null || !collectorId.startsWith("DS_")) {
                    log.warn("[RPC] record collectorId 없음 또는 DS_ 접두사 없음 - 스킵: {}", collectorId);
                    continue;
                }
                String dsId = collectorId.substring(3); // "DS_" 제거 → dataSourceId

                // record → Map<String, Object> 변환 (content + metadata 플랫화)
                Map<String, Object> row = new LinkedHashMap<>();
                Map<String, Object> contentMap = new LinkedHashMap<>();
                row.put("content",     rec.path("content").asText(""));
                row.put("sourceRef",   rec.path("sourceRef").asText(null));
                row.put("collectorId", collectorId);
                row.put("collectedAt", rec.path("collectedAt").asText(null));
                // metadata 필드 플랫화 (file, offset, format 등)
                JsonNode meta = rec.path("metadata");
                if (meta.isObject()) {
                    meta.fields().forEachRemaining(e -> row.put(e.getKey(), e.getValue().asText()));
                }
                try {
                    JsonNode contentNode = objectMapper.readTree(row.get("content").toString());

                    contentNode.fields().forEachRemaining(e -> contentMap.put(e.getKey(), e.getValue().asText()));
                } catch (Exception e) {
                    contentMap.put("line", row.get("content").toString());
                }
                byDataSource.computeIfAbsent(dsId, k -> new ArrayList<>()).add(contentMap);

            }

            // 각 dataSource 별로 파이프라인 비동기 실행
            for (Map.Entry<String, List<Map<String, Object>>> entry : byDataSource.entrySet()) {
                log.info("[RPC] 에이전트 배치 파이프라인 요청 - dataSourceId={}, records={}, agentId={}",
                        entry.getKey(), entry.getValue().size(), agentId);
                singleIngestService.ingestBatchAndRun(entry.getKey(), entry.getValue(), agentId);
            }
        }

        // ACK 응답
        String responseJson = objectMapper.writeValueAsString(Map.of(
                "ack",     true,
                "batchId", batchId
        ));
        session.sendMessage(new TextMessage(responseJson));
    }

    private AgentDto.HandshakeMessage buildFallbackHandshake(String targetId, WebSocketSession session) {
        AgentDto.HandshakeMessage msg = new AgentDto.HandshakeMessage();
        msg.setType("HANDSHAKE");
        msg.setAgentId(targetId);
        msg.setTargetId(targetId);
        msg.setAgentVersion("unknown");
        msg.setOsInfo("unknown");
        if (session.getRemoteAddress() != null) {
            msg.setIpAddress(session.getRemoteAddress().getAddress().getHostAddress());
            msg.setHostname(session.getRemoteAddress().getHostString());
        }
        return msg;
    }

    // [2026-04-21] AgentTargetConfigDto.Info → CONFIG_UPDATE payload 변환 (HANDSHAKE 자동 푸시 + 수동 푸시 공통)
    public static Map<String, Object> buildConfigPayload(AgentTargetConfigDto.Info cfg) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("targetConfigId",      cfg.getTargetConfigId());
        payload.put("rpcEndpoint",         cfg.getRpcEndpoint());
        payload.put("compress",            cfg.isCompress());
        payload.put("tlsKeystorePath",     cfg.getTlsKeystorePath());
        payload.put("tlsKeystorePassword", cfg.getTlsKeystorePassword());
        payload.put("tlsTruststorePath",   cfg.getTlsTruststorePath());
        payload.put("tlsTruststorePassword", cfg.getTlsTruststorePassword());
        payload.put("queueCapacity",       cfg.getQueueCapacity());
        payload.put("maxBatchSize",        cfg.getMaxBatchSize());
        payload.put("maxBatchMs",          cfg.getMaxBatchMs());
        payload.put("maxBatchBytes",       cfg.getMaxBatchBytes());
        return payload;
    }

    /**
     * Pushes CONFIG_UPDATE message to the connected agent.
     * Called from AgentTargetConfigController after saving changes.
     */
    public boolean pushConfigUpdate(String agentId, Map<String, Object> configPayload) {
        WebSocketSession session = agentSessionMap.get(agentId);
        if (session == null || !session.isOpen()) {
            log.warn("[RPC] Cannot push CONFIG_UPDATE - agent not connected: agentId={}", agentId);
            return false;
        }
        try {
            configPayload.put("type", "CONFIG_UPDATE");
            String json = objectMapper.writeValueAsString(configPayload);
            session.sendMessage(new TextMessage(json));
            log.info("[RPC] CONFIG_UPDATE pushed to agentId={}", agentId);
            return true;
        } catch (IOException e) {
            log.error("[RPC] Failed to push CONFIG_UPDATE to agentId={}: {}", agentId, e.getMessage());
            return false;
        }
    }

    public boolean isAgentConnected(String agentId) {
        WebSocketSession session = agentSessionMap.get(agentId);
        return session != null && session.isOpen();
    }

    /**
     * Pushes COLLECTORS_SYNC to the agent so it can update config.yaml collectors.
     */
    public boolean pushCollectorsSync(String agentId, String targetId, java.util.List<java.util.Map<String, Object>> collectors) {
        WebSocketSession session = agentSessionMap.get(agentId);
        if (session == null || !session.isOpen()) {
            log.warn("[RPC] Cannot push COLLECTORS_SYNC - agent not connected: agentId={}", agentId);
            return false;
        }
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("type", "COLLECTORS_SYNC");
            payload.put("targetId", targetId);
            payload.put("collectors", collectors);
            String json = objectMapper.writeValueAsString(payload);
            session.sendMessage(new TextMessage(json));
            log.info("[RPC] COLLECTORS_SYNC pushed to agentId={}, targetId={}, collectors={}", agentId, targetId, collectors.size());
            return true;
        } catch (IOException e) {
            log.error("[RPC] Failed to push COLLECTORS_SYNC to agentId={}: {}", agentId, e.getMessage());
            return false;
        }
    }

    private String decompress(byte[] compressed) throws IOException {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(compressed))) {
            return new String(gzip.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
