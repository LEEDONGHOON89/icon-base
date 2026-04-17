package com.itmasters.icon.rpc.agent.application.service;

import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentEntity;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentSessionEntity;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentTargetConfigEntity;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentJpaRepository;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentSessionJpaRepository;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentTargetConfigJpaRepository;
import com.itmasters.icon.rpc.agent.application.dto.AgentDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentRegistrationService {

    private final AgentJpaRepository agentJpaRepository;
    private final AgentSessionJpaRepository agentSessionJpaRepository;
    private final AgentTargetConfigJpaRepository agentTargetConfigJpaRepository;

    /**
     * Processes agent handshake on WebSocket connect.
     * Auto-registers the agent if first connect, or updates existing record.
     */
    @Transactional
    public AgentDto.Info processHandshake(AgentDto.HandshakeMessage msg,
                                          String sessionId,
                                          String remoteAddress) {
        String agentId = msg.getAgentId();
        log.info("[RPC] Handshake received - agentId={}, version={}, ip={}",
                agentId, msg.getAgentVersion(), remoteAddress);

        // Disconnect any existing active sessions for this agent
        int disconnected = agentSessionJpaRepository.disconnectAllByAgentId(
                agentId, "new_connection");
        if (disconnected > 0) {
            log.info("[RPC] Disconnected {} previous sessions for agentId={}", disconnected, agentId);
        }

        // Upsert agent record
        AgentEntity agent = agentJpaRepository.findById(agentId)
                .map(existing -> {
                    existing.updateOnConnect(msg.getHostname(), remoteAddress,
                            msg.getAgentVersion(), msg.getOsInfo());
                    return existing;
                })
                .orElseGet(() -> AgentEntity.createNew(
                        agentId, msg.getHostname(), remoteAddress,
                        msg.getAgentVersion(), msg.getOsInfo()));
        agentJpaRepository.save(agent);

        // Create new session
        AgentSessionEntity session = AgentSessionEntity.createConnected(
                sessionId, agentId, remoteAddress, msg.getAgentVersion());
        agentSessionJpaRepository.save(session);

        // Auto-create agent_target_configs if not exists (first connect)
        // agent_id = 에이전트 식별자(agentId), target_id = 대상 식별자(targetId, 없으면 agentId)
        String targetIdForConfig = (msg.getTargetId() != null && !msg.getTargetId().isBlank())
                ? msg.getTargetId() : agentId;
        if (!agentTargetConfigJpaRepository.existsByAgentIdAndTargetId(agentId, targetIdForConfig)) {
            String configId = java.util.UUID.randomUUID().toString();
            AgentTargetConfigEntity targetConfig = AgentTargetConfigEntity.create(
                    configId, agentId, targetIdForConfig,
                    msg.getRpcEndpoint() != null ? msg.getRpcEndpoint() : "",
                    msg.isCompress(),
                    nullIfBlank(msg.getTlsKeystorePath()),
                    nullIfBlank(msg.getTlsKeystorePassword()),
                    nullIfBlank(msg.getTlsTruststorePath()),
                    nullIfBlank(msg.getTlsTruststorePassword()),
                    msg.getQueueCapacity() > 0  ? msg.getQueueCapacity()  : 10000,
                    msg.getMaxBatchSize()  > 0  ? msg.getMaxBatchSize()   : 500,
                    msg.getMaxBatchMs()    > 0  ? msg.getMaxBatchMs()     : 2000L,
                    msg.getMaxBatchBytes() >= 0 ? msg.getMaxBatchBytes()  : 1048576L
            );
            agentTargetConfigJpaRepository.save(targetConfig);
            log.info("[RPC] agent_target_configs auto-created for agentId={}", agentId);
        }

        log.info("[RPC] Agent registered/updated - agentId={}, status={}", agentId, agent.getStatus());
        return AgentDto.Info.from(agent);
    }

    /**
     * Marks the session and agent as disconnected.
     */
    @Transactional
    public void handleDisconnect(String sessionId, String reason) {
        agentSessionJpaRepository.findBySessionIdAndStatus(
                        sessionId, AgentSessionEntity.STATUS_CONNECTED)
                .ifPresent(session -> {
                    session.disconnect(reason);
                    agentSessionJpaRepository.save(session);

                    agentJpaRepository.findById(session.getAgentId())
                            .ifPresent(agent -> {
                                agent.updateOnDisconnect();
                                agentJpaRepository.save(agent);
                                log.info("[RPC] Agent disconnected - agentId={}, reason={}",
                                        agent.getAgentId(), reason);
                            });
                });
    }

    /**
     * Updates heartbeat timestamp for the session.
     */
    @Transactional
    public void updateHeartbeat(String sessionId) {
        agentSessionJpaRepository.findBySessionIdAndStatus(
                        sessionId, AgentSessionEntity.STATUS_CONNECTED)
                .ifPresent(session -> {
                    session.updateHeartbeat();
                    agentSessionJpaRepository.save(session);
                });
    }

    private static String nullIfBlank(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    @Transactional(readOnly = true)
    public List<AgentDto.SessionInfo> getSessions(String agentId, int limit) {
        return agentSessionJpaRepository
                .findByAgentIdOrderByConnectedAtDesc(agentId, PageRequest.of(0, limit))
                .stream()
                .map(s -> AgentDto.SessionInfo.builder()
                        .sessionId(s.getSessionId())
                        .agentId(s.getAgentId())
                        .remoteAddress(s.getRemoteAddress())
                        .agentVersion(s.getAgentVersion())
                        .status(s.getStatus())
                        .connectedAt(s.getConnectedAt())
                        .lastHeartbeatAt(s.getLastHeartbeatAt())
                        .disconnectedAt(s.getDisconnectedAt())
                        .disconnectReason(s.getDisconnectReason())
                        .build())
                .collect(Collectors.toList());
    }
}
