package com.itmasters.icon.engine.datasource.domain;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 파일 시스템 설정 도메인 모델
 */
@Getter
public class FileSystemConfig {
    private final String id;
    private final String dataSourceId;
    private final String basePath;
    private final String filePattern;
    private final Boolean recursiveScan;
    private final String encoding;
    private final String delimiter;
    private final Boolean hasHeader;
    private final Boolean isActive;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private FileSystemConfig(String id, String dataSourceId, String basePath, String filePattern,
                           Boolean recursiveScan, String encoding, String delimiter, Boolean hasHeader,
                           Boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.dataSourceId = dataSourceId;
        this.basePath = basePath;
        this.filePattern = filePattern;
        this.recursiveScan = recursiveScan;
        this.encoding = encoding;
        this.delimiter = delimiter;
        this.hasHeader = hasHeader;
        this.isActive = isActive;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 파일 시스템 설정 생성
     */
    public static FileSystemConfig of(String id, String dataSourceId, String basePath, String filePattern,
                                     Boolean recursiveScan, String encoding, String delimiter, Boolean hasHeader,
                                     Boolean isActive, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new FileSystemConfig(id, dataSourceId, basePath, filePattern, recursiveScan,
                                   encoding, delimiter, hasHeader, isActive, createdAt, updatedAt);
    }

    /**
     * 기본값을 사용한 새 설정 생성
     */
    public static FileSystemConfig createNew(String id, String dataSourceId, String basePath) {
        return new FileSystemConfig(id, dataSourceId, basePath, "*.csv", false,
                                   "UTF-8", ",", true, true, LocalDateTime.now(), LocalDateTime.now());
    }

    /**
     * 파일 패턴과 일치하는지 확인
     */
    public boolean matchesFilePattern(String fileName) {
        if (filePattern == null || filePattern.equals("*.*") || filePattern.equals("*")) {
            return true;
        }
        
        // 간단한 와일드카드 패턴 매칭 구현
        String pattern = filePattern.replace("*", ".*");
        return fileName.matches(pattern);
    }

    /**
     * 활성화 여부 확인
     */
    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    /**
     * CSV 헤더 존재 여부
     */
    public boolean hasHeader() {
        return Boolean.TRUE.equals(hasHeader);
    }

    /**
     * 재귀 스캔 여부
     */
    public boolean isRecursiveScan() {
        return Boolean.TRUE.equals(recursiveScan);
    }
}