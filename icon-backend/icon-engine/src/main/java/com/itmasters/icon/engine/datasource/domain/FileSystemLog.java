package com.itmasters.icon.engine.datasource.domain;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 파일 시스템 처리 로그 도메인 모델
 */
@Getter
public class FileSystemLog {
    private final String processedFileId;
    private final String configId;
    private final String filePath;
    private final String fileName;
    private final Long fileSize;
    private final Integer recordsProcessed;
    private final ProcessingStatus status;
    private final String errorMessage;
    private final LocalDateTime processedAt;

    public enum ProcessingStatus {
        SUCCESS("성공"),
        FAILED("실패"),
        SKIPPED("스킵됨");

        private final String description;

        ProcessingStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private FileSystemLog(String processedFileId, String configId, String filePath, String fileName,
                         Long fileSize, Integer recordsProcessed, ProcessingStatus status,
                         String errorMessage, LocalDateTime processedAt) {
        this.processedFileId = processedFileId;
        this.configId = configId;
        this.filePath = filePath;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.recordsProcessed = recordsProcessed;
        this.status = status;
        this.errorMessage = errorMessage;
        this.processedAt = processedAt;
    }

    /**
     * 파일 시스템 로그 생성
     */
    public static FileSystemLog of(String processedFileId, String configId, String filePath, String fileName,
                                  Long fileSize, Integer recordsProcessed, ProcessingStatus status,
                                  String errorMessage, LocalDateTime processedAt) {
        return new FileSystemLog(processedFileId, configId, filePath, fileName, fileSize, recordsProcessed,
                                status, errorMessage, processedAt);
    }

    /**
     * 성공 로그 생성
     */
    public static FileSystemLog success(String processedFileId, String configId, String filePath, String fileName,
                                       Long fileSize, Integer recordsProcessed) {
        return new FileSystemLog(processedFileId, configId, filePath, fileName, fileSize, recordsProcessed,
                                ProcessingStatus.SUCCESS, null, LocalDateTime.now());
    }

    /**
     * 실패 로그 생성
     */
    public static FileSystemLog failed(String processedFileId, String configId, String filePath, String fileName,
                                      String errorMessage) {
        return new FileSystemLog(processedFileId, configId, filePath, fileName, null, null,
                                ProcessingStatus.FAILED, errorMessage, LocalDateTime.now());
    }

    /**
     * 스킵 로그 생성
     */
    public static FileSystemLog skipped(String processedFileId, String configId, String filePath, String fileName) {
        return new FileSystemLog(processedFileId, configId, filePath, fileName, null, null,
                                ProcessingStatus.SKIPPED, "Already processed", LocalDateTime.now());
    }

    /**
     * 성공 여부 확인
     */
    public boolean isSuccess() {
        return status == ProcessingStatus.SUCCESS;
    }

    /**
     * 실패 여부 확인
     */
    public boolean isFailed() {
        return status == ProcessingStatus.FAILED;
    }

    /**
     * 스킵 여부 확인
     */
    public boolean isSkipped() {
        return status == ProcessingStatus.SKIPPED;
    }
}