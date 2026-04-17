package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ds_file_system_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineDsFileSystemLogEntity {

    @Id
    @Column(name = "ds_file_system_log_id")
    private String id;

    @Column(name = "ds_file_system_config_id", nullable = false)
    private String configId;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private ProcessingStatus processingStatus;

    @Column(name = "error_message")
    private String errorMessage;

    public enum ProcessingStatus { SUCCESS, FAILED, SKIPPED }

    public static EngineDsFileSystemLogEntity of(String id, String configId, String filePath, String fileName,
                                                 Long fileSize, LocalDateTime processedAt, Integer recordsProcessed,
                                                 ProcessingStatus processingStatus, String errorMessage) {
        EngineDsFileSystemLogEntity e = new EngineDsFileSystemLogEntity();
        e.id = id;
        e.configId = configId;
        e.filePath = filePath;
        e.fileName = fileName;
        e.fileSize = fileSize;
        e.processedAt = processedAt;
        e.recordsProcessed = recordsProcessed;
        e.processingStatus = processingStatus;
        e.errorMessage = errorMessage;
        return e;
    }
}
