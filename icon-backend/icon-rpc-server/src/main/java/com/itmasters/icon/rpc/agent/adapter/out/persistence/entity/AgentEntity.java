package com.itmasters.icon.rpc.agent.adapter.out.persistence.entity;

import com.itmasters.icon.rpc.agent.domain.AgentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "agents")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Id
    @Column(name = "agent_id", length = 100)
    private String agentId;

    @Column(name = "hostname", length = 300)
    private String hostname;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "agent_version", length = 50)
    private String agentVersion;

    @Column(name = "os_info", length = 200)
    private String osInfo;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "description", length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 30, nullable = false)
    private AgentStatus status;

    @Column(name = "first_connected_at", nullable = false, updatable = false)
    private LocalDateTime firstConnectedAt;

    @Column(name = "last_connected_at")
    private LocalDateTime lastConnectedAt;

    @Column(name = "last_disconnected_at")
    private LocalDateTime lastDisconnectedAt;

    public static AgentEntity createNew(String agentId, String hostname, String ipAddress,
                                        String agentVersion, String osInfo) {
        AgentEntity e = new AgentEntity();
        e.agentId = agentId;
        e.hostname = hostname;
        e.ipAddress = ipAddress;
        e.agentVersion = agentVersion;
        e.osInfo = osInfo;
        e.status = AgentStatus.PENDING_CONFIG;
        e.firstConnectedAt = LocalDateTime.now();
        e.lastConnectedAt = LocalDateTime.now();
        return e;
    }

    public void updateOnConnect(String hostname, String ipAddress, String agentVersion, String osInfo) {
        this.hostname = hostname;
        this.ipAddress = ipAddress;
        this.agentVersion = agentVersion;
        this.osInfo = osInfo;
        this.status = AgentStatus.ACTIVE;
        this.lastConnectedAt = LocalDateTime.now();
    }

    public void updateOnDisconnect() {
        this.status = AgentStatus.DISCONNECTED;
        this.lastDisconnectedAt = LocalDateTime.now();
    }

    public void updateMeta(String displayName, String description) {
        if (displayName != null) this.displayName = displayName;
        if (description != null) this.description = description;
    }

    public void deactivate() {
        this.status = AgentStatus.INACTIVE;
    }
}
