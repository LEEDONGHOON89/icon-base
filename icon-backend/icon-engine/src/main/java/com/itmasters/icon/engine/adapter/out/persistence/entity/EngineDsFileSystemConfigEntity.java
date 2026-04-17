package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ds_file_system_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineDsFileSystemConfigEntity {

    @Id
    @Column(name = "ds_file_system_config_id")
    private String id;

    @Column(name = "data_source_id", nullable = false)
    private String dataSourceId;

    @Column(name = "watch_directory", nullable = false)
    private String watchDirectory;

    @Column(name = "file_pattern")
    private String filePattern;

    @Column(name = "file_encoding")
    private String fileEncoding;

    @Column(name = "delimiter")
    private String delimiter;

    @Column(name = "has_header")
    private Boolean hasHeader;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "scan_interval_minutes")
    private Integer scanIntervalMinutes;

    // [2026-03-12] FILE_SYSTEM_REALTIME → 에이전트 수집 모드 여부
    @Column(name = "agent_id", length = 100)
    private String agentId;

    public static EngineDsFileSystemConfigEntity of(String id, String dataSourceId, String watchDirectory,
                                                    String filePattern, String fileEncoding, String delimiter,
                                                    Boolean hasHeader, Boolean isActive,
                                                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        EngineDsFileSystemConfigEntity e = new EngineDsFileSystemConfigEntity();
        e.id = id;
        e.dataSourceId = dataSourceId;
        e.watchDirectory = watchDirectory;
        e.filePattern = filePattern;
        e.fileEncoding = fileEncoding;
        e.delimiter = delimiter;
        e.hasHeader = hasHeader;
        e.isActive = isActive;
        e.createdAt = createdAt;
        e.updatedAt = updatedAt;
        return e;
    }
}
