package com.itmasters.icon.rpc.agent.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_target_configs",
        uniqueConstraints = @UniqueConstraint(name = "uq_agent_target", columnNames = {"agent_id", "target_id"}))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentTargetConfigEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Id
    @Column(name = "target_config_id", length = 100)
    private String targetConfigId;

    @Column(name = "agent_id", length = 100, nullable = false)
    private String agentId;

    @Column(name = "target_id", length = 100, nullable = false)
    private String targetId;

    @Column(name = "rpc_endpoint", length = 500, nullable = false)
    private String rpcEndpoint;

    @Column(name = "compress", nullable = false)
    private boolean compress = false;

    @Column(name = "tls_keystore_path", length = 500)
    private String tlsKeystorePath;

    @Column(name = "tls_keystore_password", length = 200)
    private String tlsKeystorePassword;

    @Column(name = "tls_truststore_path", length = 500)
    private String tlsTruststorePath;

    @Column(name = "tls_truststore_password", length = 200)
    private String tlsTruststorePassword;

    @Column(name = "queue_capacity", nullable = false)
    private int queueCapacity = 10000;

    @Column(name = "max_batch_size", nullable = false)
    private int maxBatchSize = 500;

    @Column(name = "max_batch_ms", nullable = false)
    private long maxBatchMs = 2000L;

    @Column(name = "max_batch_bytes", nullable = false)
    private long maxBatchBytes = 1048576L;

    // [2026-04-22] 초당 최대 배치 전송 수 (0 = 무제한)
    @Column(name = "max_batches_per_second", nullable = false)
    private int maxBatchesPerSecond = 10;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    public static AgentTargetConfigEntity create(String targetConfigId, String agentId, String targetId,
                                                  String rpcEndpoint, boolean compress,
                                                  String tlsKeystorePath, String tlsKeystorePassword,
                                                  String tlsTruststorePath, String tlsTruststorePassword,
                                                  int queueCapacity, int maxBatchSize,
                                                  long maxBatchMs, long maxBatchBytes,
                                                  // [2026-04-22] maxBatchesPerSecond 추가
                                                  int maxBatchesPerSecond) {
        AgentTargetConfigEntity e = new AgentTargetConfigEntity();
        e.targetConfigId = targetConfigId;
        e.agentId = agentId;
        e.targetId = targetId;
        e.rpcEndpoint = rpcEndpoint;
        e.compress = compress;
        e.tlsKeystorePath = tlsKeystorePath;
        e.tlsKeystorePassword = tlsKeystorePassword;
        e.tlsTruststorePath = tlsTruststorePath;
        e.tlsTruststorePassword = tlsTruststorePassword;
        e.queueCapacity = queueCapacity;
        e.maxBatchSize = maxBatchSize;
        e.maxBatchMs = maxBatchMs;
        e.maxBatchBytes = maxBatchBytes;
        e.maxBatchesPerSecond = maxBatchesPerSecond;
        e.isActive = true;
        return e;
    }

    public void update(String rpcEndpoint, boolean compress,
                       String tlsKeystorePath, String tlsKeystorePassword,
                       String tlsTruststorePath, String tlsTruststorePassword,
                       int queueCapacity, int maxBatchSize, long maxBatchMs, long maxBatchBytes,
                       // [2026-04-22] maxBatchesPerSecond 추가
                       int maxBatchesPerSecond) {
        this.rpcEndpoint = rpcEndpoint;
        this.compress = compress;
        this.tlsKeystorePath = tlsKeystorePath;
        this.tlsKeystorePassword = tlsKeystorePassword;
        this.tlsTruststorePath = tlsTruststorePath;
        this.tlsTruststorePassword = tlsTruststorePassword;
        this.queueCapacity = queueCapacity;
        this.maxBatchSize = maxBatchSize;
        this.maxBatchMs = maxBatchMs;
        this.maxBatchBytes = maxBatchBytes;
        this.maxBatchesPerSecond = maxBatchesPerSecond;
    }

    public void deactivate() { this.isActive = false; }
    public void activate()   { this.isActive = true; }
}
