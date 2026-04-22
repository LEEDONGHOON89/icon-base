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

        // [2026-04-21] 방안2: 동일 agentId 활성 세션 존재 시 신규 연결 거부 (기존 에이전트 보호)
        long activeSessions = agentSessionJpaRepository.countByAgentIdAndStatus(
                agentId, AgentSessionEntity.STATUS_CONNECTED);
        if (activeSessions > 0) {
            String existingAddr = agentSessionJpaRepository
                    .findFirstByAgentIdAndStatusOrderByConnectedAtDesc(agentId, AgentSessionEntity.STATUS_CONNECTED)
                    .map(AgentSessionEntity::getRemoteAddress)
                    .orElse("unknown");
            log.warn("[RPC] agentId 중복 연결 거부 - agentId={}, 기존 세션 주소={}, 신규 요청 주소={}",
                    agentId, existingAddr, remoteAddress);
            throw new DuplicateAgentIdException(agentId, existingAddr);
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
                    msg.getQueueCapacity()       > 0  ? msg.getQueueCapacity()       : 10000,
                    msg.getMaxBatchSize()        > 0  ? msg.getMaxBatchSize()        : 500,
                    msg.getMaxBatchMs()          > 0  ? msg.getMaxBatchMs()          : 5000L,
                    msg.getMaxBatchBytes()       >= 0 ? msg.getMaxBatchBytes()       : 524288L,
                    // [2026-04-22] maxBatchesPerSecond 추가
                    msg.getMaxBatchesPerSecond() > 0  ? msg.getMaxBatchesPerSecond() : 10
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
