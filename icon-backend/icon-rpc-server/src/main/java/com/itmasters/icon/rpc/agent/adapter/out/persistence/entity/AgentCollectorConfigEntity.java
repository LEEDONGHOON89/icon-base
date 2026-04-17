package com.itmasters.icon.rpc.agent.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "agent_collector_configs",
        indexes = @Index(name = "idx_collectors_target", columnList = "target_config_id"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AgentCollectorConfigEntity {

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Id
    @Column(name = "collector_config_id", length = 100)
    private String collectorConfigId;

    @Column(name = "target_config_id", length = 100, nullable = false)
    private String targetConfigId;

    @Column(name = "collector_type", length = 20, nullable = false)
    private String collectorType; // FILE | JDBC

    @Column(name = "name", length = 200, nullable = false)
    private String name;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "poll_interval_ms", nullable = false)
    private long pollIntervalMs = 1000;

    @Column(name = "max_lines_per_poll", nullable = false)
    private int maxLinesPerPoll = 1000;

    @Column(name = "max_record_bytes", nullable = false)
    private int maxRecordBytes = 524288;

    // [2026-03-13] 연결된 데이터소스 ID (in-memory 참조용, DB 컬럼 없음 — ds_file_system_config 직접 참조)
    @Transient
    private String dataSourceId;

    public static AgentCollectorConfigEntity create(String collectorConfigId, String targetConfigId,
                                                    String collectorType, String name, boolean enabled,
                                                    long pollIntervalMs, int maxLinesPerPoll, int maxRecordBytes) {
        AgentCollectorConfigEntity e = new AgentCollectorConfigEntity();
        e.collectorConfigId = collectorConfigId;
        e.targetConfigId = targetConfigId;
        e.collectorType = collectorType;
        e.name = name;
        e.enabled = enabled;
        e.pollIntervalMs = pollIntervalMs;
        e.maxLinesPerPoll = maxLinesPerPoll;
        e.maxRecordBytes = maxRecordBytes;
        return e;
    }

    public void update(String name, boolean enabled, long pollIntervalMs, int maxLinesPerPoll, int maxRecordBytes) {
        this.name = name;
        this.enabled = enabled;
        this.pollIntervalMs = pollIntervalMs;
        this.maxLinesPerPoll = maxLinesPerPoll;
        this.maxRecordBytes = maxRecordBytes;
    }
}
