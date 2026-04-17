package com.itmasters.icon.rpc.agent.adapter.in.ws;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.rpc.agent.application.dto.AgentDto;
import com.itmasters.icon.rpc.agent.application.service.AgentRegistrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RpcWebSocketHandler extends TextWebSocketHandler {

    private final AgentRegistrationService registrationService;
    private final ObjectMapper objectMapper;

    /** sessionId -> agentId (connected sessions) */
    private final Map<String, String> sessionAgentMap = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[RPC] WebSocket connected - sessionId={}, remote={}",
                session.getId(), session.getRemoteAddress());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        JsonNode root = objectMapper.readTree(payload);
        String type = root.path("type").asText();

        switch (type) {
            case "HANDSHAKE":
                handleHandshake(session, root);
                break;
            case "HEARTBEAT":
                handleHeartbeat(session);
                break;
            default:
                log.info("[RPC] Unknown message type: {} - sessionId={}", type, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        String agentId = sessionAgentMap.remove(sessionId);
        log.info("[RPC] WebSocket disconnected - sessionId={}, agentId={}, status={}",
                sessionId, agentId, status);
        registrationService.handleDisconnect(sessionId, status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[RPC] Transport error - sessionId={}: {}", session.getId(), exception.getMessage());
    }

    private void handleHandshake(WebSocketSession session, JsonNode root) throws Exception {
        AgentDto.HandshakeMessage msg = objectMapper.treeToValue(root, AgentDto.HandshakeMessage.class);

        String remoteAddress = session.getRemoteAddress() != null
                ? session.getRemoteAddress().toString()
                : "unknown";

        AgentDto.Info info = registrationService.processHandshake(msg, session.getId(), remoteAddress);
        sessionAgentMap.put(session.getId(), info.getAgentId());

        String ack = objectMapper.writeValueAsString(Map.of(
                "type", "HANDSHAKE_ACK",
                "agentId", info.getAgentId(),
                "status", info.getStatus().name()
        ));
        session.sendMessage(new TextMessage(ack));
        log.info("[RPC] HANDSHAKE_ACK sent - agentId={}", info.getAgentId());
    }

    private void handleHeartbeat(WebSocketSession session) {
        registrationService.updateHeartbeat(session.getId());
        log.debug("[RPC] HEARTBEAT - sessionId={}", session.getId());
    }

    public boolean isConnected(String agentId) {
        return sessionAgentMap.containsValue(agentId);
    }
}
