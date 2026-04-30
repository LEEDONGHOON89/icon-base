package com.itmasters.icon.rpc.agent.application.dto;

import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentEntity;
import com.itmasters.icon.rpc.agent.domain.AgentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class AgentDto {

    @Getter
    @Builder
    public static class Info {
        private String agentId;
        private String hostname;
        private String ipAddress;
        private String agentVersion;
        private String osInfo;
        private String displayName;
        private String description;
        private AgentStatus status;
        private LocalDateTime firstConnectedAt;
        private LocalDateTime lastConnectedAt;
        private LocalDateTime lastDisconnectedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Info from(AgentEntity e) {
            return Info.builder()
                    .agentId(e.getAgentId())
                    .hostname(e.getHostname())
                    .ipAddress(e.getIpAddress())
                    .agentVersion(e.getAgentVersion())
                    .osInfo(e.getOsInfo())
                    .displayName(e.getDisplayName() != null ? e.getDisplayName() : e.getAgentId())
                    .description(e.getDescription())
                    .status(e.getStatus())
                    .firstConnectedAt(e.getFirstConnectedAt())
                    .lastConnectedAt(e.getLastConnectedAt())
                    .lastDisconnectedAt(e.getLastDisconnectedAt())
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    public static class UpdateRequest {
        private String displayName;
        private String description;
    }

    /** Handshake message sent by agent on first WebSocket connect */
    @Getter
    @lombok.Setter
    public static class HandshakeMessage {
        private String type;
        /** 에이전트 식별자 (config.yaml 최상단 agentId) */
        private String agentId;
        /** 대상(target) 식별자 (config.yaml targets[].id). 없으면 agentId와 동일하게 처리 */
        private String targetId;
        private String hostname;
        private String ipAddress;
        private String agentVersion;
        private String osInfo;
        // target config from agent config.yaml
        private String rpcEndpoint;
        private boolean compress;
        private String tlsKeystorePath;
        private String tlsKeystorePassword;
        private String tlsTruststorePath;
        private String tlsTruststorePassword;
        private int    queueCapacity       = 10000;
        private int    maxBatchSize        = 500;
        private long   maxBatchMs          = 5000;
        private long   maxBatchBytes       = 524288;
        // [2026-04-22] 초당 최대 배치 전송 수 (0 = 무제한)
        private int    maxBatchesPerSecond = 10;
    }

    @Getter
    @Builder
    public static class SessionInfo {
        private String sessionId;
        private String agentId;
        private String remoteAddress;
        private String agentVersion;
        private String status;
        private LocalDateTime connectedAt;
        private LocalDateTime lastHeartbeatAt;
        private LocalDateTime disconnectedAt;
        private String disconnectReason;
    }
}
