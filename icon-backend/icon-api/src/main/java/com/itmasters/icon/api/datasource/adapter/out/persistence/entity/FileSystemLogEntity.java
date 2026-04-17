package com.itmasters.icon.api.datasource.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 파일 시스템 처리 로그 엔티티
 * ds_file_system_log 테이블과 매핑
 * 로그성 데이터이므로 Auditable 상속하지 않고 regDt 필드만 사용
 */
@Entity
@Table(name = "ds_file_system_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileSystemLogEntity {

    @Id
    @Column(name = "ds_file_system_log_id")
    private String dsFileSystemLogId;

    @Column(name = "ds_file_system_config_id", nullable = false)
    private String dsFileSystemConfigId;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_name", nullable = false, length = 500)
    private String fileName;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "file_modified_time")
    private LocalDateTime fileModifiedTime;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "records_processed")
    private Integer recordsProcessed;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    private ProcessingStatus processingStatus;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public enum ProcessingStatus {
        SUCCESS,    // 성공
        FAILED,     // 실패
        SKIPPED     // 스킵됨 (이미 처리된 파일)
    }

    private FileSystemLogEntity(String dsFileSystemLogId, String dsFileSystemConfigId,
                               String filePath, String fileName, Long fileSize,
                               Integer recordsProcessed, ProcessingStatus processingStatus, String errorMessage) {
        this.dsFileSystemLogId = dsFileSystemLogId;
        this.dsFileSystemConfigId = dsFileSystemConfigId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.recordsProcessed = recordsProcessed;
        this.processingStatus = processingStatus;
        this.errorMessage = errorMessage;
        this.processedAt = LocalDateTime.now();
    }

    /**
     * 성공 로그 생성
     */
    public static FileSystemLogEntity success(String dsFileSystemLogId, String dsFileSystemConfigId,
                                             String filePath, String fileName, Long fileSize,
                                             Integer recordsProcessed) {
        return new FileSystemLogEntity(dsFileSystemLogId, dsFileSystemConfigId, filePath, fileName,
                                      fileSize, recordsProcessed, ProcessingStatus.SUCCESS, null);
    }

    /**
     * 실패 로그 생성
     */
    public static FileSystemLogEntity failed(String dsFileSystemLogId, String dsFileSystemConfigId,
                                            String filePath, String fileName, String errorMessage) {
        return new FileSystemLogEntity(dsFileSystemLogId, dsFileSystemConfigId, filePath, fileName,
                                      null, null, ProcessingStatus.FAILED, errorMessage);
    }

    /**
     * 스킵 로그 생성 (이미 처리된 파일)
     */
    public static FileSystemLogEntity skipped(String dsFileSystemLogId, String dsFileSystemConfigId,
                                             String filePath, String fileName) {
        return new FileSystemLogEntity(dsFileSystemLogId, dsFileSystemConfigId, filePath, fileName,
                                      null, null, ProcessingStatus.SKIPPED, "Already processed");
    }
}