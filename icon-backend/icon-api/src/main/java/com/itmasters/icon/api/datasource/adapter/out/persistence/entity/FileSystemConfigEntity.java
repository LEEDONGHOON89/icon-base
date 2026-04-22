package com.itmasters.icon.api.datasource.adapter.out.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 파일 시스템 데이터소스 연결 설정 엔티티
 * ds_file_system_config 테이블과 매핑
 */
@Entity
@Table(name = "ds_file_system_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileSystemConfigEntity extends Auditable {

    @Id
    @Column(name = "ds_file_system_config_id")
    private String dsFileSystemConfigId;

    @Column(name = "data_source_id", nullable = false)
    private String dataSourceId;

    @Column(name = "connection_name", nullable = false, length = 100)
    private String connectionName;

    @Column(name = "watch_directory", nullable = false, length = 500)
    private String watchDirectory;

    @Column(name = "file_pattern", length = 100)
    private String filePattern = "*";

    @Column(name = "file_encoding", length = 20)
    private String fileEncoding = "UTF-8";

    @Column(name = "delimiter", length = 5)
    private String delimiter = ",";

    @Column(name = "quote_char", length = 1)
    private String quoteChar = "\"";

    @Column(name = "escape_char", length = 1)
    private String escapeChar;

    @Column(name = "has_header")
    private Boolean hasHeader = true;

    @Column(name = "skip_lines")
    private Integer skipLines = 0;

    @Column(name = "processing_strategy", length = 50)
    private String processingStrategy = "INCREMENTAL";

    @Column(name = "scan_interval_minutes")
    private Integer scanIntervalMinutes = 60;

    @Column(name = "move_processed_files")
    private Boolean moveProcessedFiles = false;

    @Column(name = "processed_files_directory", length = 500)
    private String processedFilesDirectory;

    @Column(name = "last_processed_file", length = 500)
    private String lastProcessedFile;

    @Column(name = "last_scan_time")
    private LocalDateTime lastScanTime;

    @Column(name = "total_files_processed")
    private Integer totalFilesProcessed = 0;

    @Column(name = "total_records_processed")
    private Long totalRecordsProcessed = 0L;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "connection_status", length = 20)
    private String connectionStatus = "IDLE";

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "last_error_time")
    private LocalDateTime lastErrorTime;

    // [2026-03-12] FILE_SYSTEM_REALTIME → 에이전트 연결 정보
    @Column(name = "agent_id", length = 100)
    private String agentId;

    // [2026-04-21] 에이전트 폴링 설정 — NULL 시 기본값으로 대체
    @Column(name = "poll_interval_ms")
    private Long pollIntervalMs;          // NULL → scanIntervalMinutes * 60000 변환값 사용

    @Column(name = "max_lines_per_poll")
    private Integer maxLinesPerPoll;      // NULL → 1000

    @Column(name = "max_record_bytes")
    private Integer maxRecordBytes;       // NULL → 524288 (512 KB)

    private FileSystemConfigEntity(String dsFileSystemConfigId, String dataSourceId, 
                                  String connectionName, String watchDirectory, String filePattern,
                                  String fileEncoding, String delimiter, Boolean hasHeader) {
        this.dsFileSystemConfigId = dsFileSystemConfigId;
        this.dataSourceId = dataSourceId;
        this.connectionName = connectionName;
        this.watchDirectory = watchDirectory;
        this.filePattern = filePattern;
        this.fileEncoding = fileEncoding;
        this.delimiter = delimiter;
        this.hasHeader = hasHeader;
    }

    /**
     * 파일 시스템 설정 생성
     */
    public static FileSystemConfigEntity of(String dsFileSystemConfigId, String dataSourceId, 
                                           String connectionName, String watchDirectory, String filePattern,
                                           String fileEncoding, String delimiter, Boolean hasHeader) {
        return new FileSystemConfigEntity(dsFileSystemConfigId, dataSourceId, connectionName, 
                                         watchDirectory, filePattern, fileEncoding, delimiter, hasHeader);
    }

    /**
     * FILE_SYSTEM 기본값 설정 생성 (배치, 60분 스캔 주기)
     */
    public static FileSystemConfigEntity ofDefault(String dsFileSystemConfigId, String dataSourceId, 
                                                  String connectionName, String watchDirectory) {
        return new FileSystemConfigEntity(dsFileSystemConfigId, dataSourceId, connectionName, 
                                         watchDirectory, "*.csv", "UTF-8", ",", true);
    }

    /**
     * FILE_SYSTEM_REALTIME 기본값 설정 생성 (실시간, 폴링 간격 5초)
     * scan_interval_minutes 필드를 초 단위로 재사용 (기본값 5초)
     */
    public static FileSystemConfigEntity ofDefaultRealtime(String dsFileSystemConfigId, String dataSourceId,
                                                           String connectionName, String watchDirectory) {
        FileSystemConfigEntity e = new FileSystemConfigEntity(dsFileSystemConfigId, dataSourceId,
                connectionName, watchDirectory, "*", "UTF-8", ",", false);
        e.processingStrategy = "REALTIME";
        e.scanIntervalMinutes = 5; // 폴링 간격 초 단위 (필드 재사용)
        return e;
    }

    // 업데이트 유틸 (테스트/관리 UI용) — 필요한 범위만 반영
    public void applyBasic(String connectionName,
                           String watchDirectory,
                           String filePattern,
                           String fileEncoding,
                           String delimiter,
                           String quoteChar,
                           String escapeChar,
                           Boolean hasHeader,
                           Integer skipLines) {
        this.connectionName = connectionName;
        this.watchDirectory = watchDirectory;
        this.filePattern = filePattern;
        this.fileEncoding = fileEncoding;
        this.delimiter = delimiter;
        this.quoteChar = quoteChar;
        this.escapeChar = escapeChar;
        this.hasHeader = hasHeader;
        this.skipLines = skipLines;
    }

    // [2026-03-12] 에이전트 연결 정보 설정 메서드
    public void applyAgentLink(String agentId) {
        this.agentId = agentId;
    }

    // [2026-04-21] 에이전트 폴링 설정 업데이트
    public void applyPollSettings(Long pollIntervalMs, Integer maxLinesPerPoll, Integer maxRecordBytes) {
        this.pollIntervalMs = pollIntervalMs;
        this.maxLinesPerPoll = maxLinesPerPoll;
        this.maxRecordBytes = maxRecordBytes;
    }

    public void applyAdvanced(String processingStrategy,
                              Integer scanIntervalMinutes,
                              Boolean moveProcessedFiles,
                              String processedFilesDirectory) {
        this.processingStrategy = processingStrategy;
        this.scanIntervalMinutes = scanIntervalMinutes;
        this.moveProcessedFiles = moveProcessedFiles;
        this.processedFilesDirectory = processedFilesDirectory;
    }

    /**
     * 활성화 상태 변경
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 비활성화 상태 변경
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 스캔 시간 업데이트
     */
    public void updateScanTime() {
        this.lastScanTime = LocalDateTime.now();
    }

    /**
     * 처리 완료 파일 업데이트
     */
    public void updateProcessedFile(String filePath, int recordCount) {
        this.lastProcessedFile = filePath;
        this.totalFilesProcessed = (this.totalFilesProcessed == null ? 0 : this.totalFilesProcessed) + 1;
        this.totalRecordsProcessed = (this.totalRecordsProcessed == null ? 0L : this.totalRecordsProcessed) + recordCount;
        this.lastScanTime = LocalDateTime.now();
    }

    /**
     * 에러 상태 업데이트
     */
    public void updateError(String errorMessage) {
        this.lastErrorMessage = errorMessage;
        this.lastErrorTime = LocalDateTime.now();
        this.connectionStatus = "ERROR";
    }

    /**
     * 연결 상태를 IDLE로 설정
     */
    public void setIdle() {
        this.connectionStatus = "IDLE";
    }
}
