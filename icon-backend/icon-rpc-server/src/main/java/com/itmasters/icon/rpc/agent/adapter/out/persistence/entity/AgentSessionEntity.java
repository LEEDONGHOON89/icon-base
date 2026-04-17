package com.itmasters.icon.rpc.agent.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_sessions",
        indexes = {
                @Index(name = "idx_sessions_agent_status", columnList = "agent_id, status"),
                @Index(name = "idx_sessions_connected_at", columnList = "connected_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentSessionEntity {

    @Id
    @Column(name = "session_id", length = 200)
    private String sessionId;

    @Column(name = "agent_id", length = 100, nullable = false)
    private String agentId;

    @Column(name = "remote_address", length = 200)
    private String remoteAddress;

    @Column(name = "agent_version", length = 50)
    private String agentVersion;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "connected_at", nullable = false)
    private LocalDateTime connectedAt;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    @Column(name = "disconnected_at")
    private LocalDateTime disconnectedAt;

    @Column(name = "disconnect_reason", length = 500)
    private String disconnectReason;

    public static final String STATUS_CONNECTED = "CONNECTED";
    public static final String STATUS_DISCONNECTED = "DISCONNECTED";

    public static AgentSessionEntity createConnected(String sessionId, String agentId,
                                                      String remoteAddress, String agentVersion) {
        AgentSessionEntity e = new AgentSessionEntity();
        e.sessionId = sessionId;
        e.agentId = agentId;
        e.remoteAddress = remoteAddress;
        e.agentVersion = agentVersion;
        e.status = STATUS_CONNECTED;
        e.connectedAt = LocalDateTime.now();
        return e;
    }

    public void disconnect(String reason) {
        this.status = STATUS_DISCONNECTED;
        this.disconnectedAt = LocalDateTime.now();
        this.disconnectReason = reason;
    }

    public void updateHeartbeat() {
        this.lastHeartbeatAt = LocalDateTime.now();
    }
}
