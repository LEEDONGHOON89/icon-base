package com.icon.agent.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 단일 파일 수집기 설정.
 * [2026-02-25] CollectorConfig 인터페이스 구현으로 업데이트
 */
public class FileCollectorConfig implements CollectorConfig {

    @JsonProperty("id")
    private String id;

    // [2026-02-25] 통합 수집기 목록 지원을 위한 type 필드 추가
    @JsonProperty("type")
    private String type = "FILE";

    @JsonProperty("name")
    private String name;

    @JsonProperty("enabled")
    private boolean enabled = true;

    /** 감시할 디렉토리. yaml의 'path'에 매핑 */
    @JsonProperty("path")
    private String directory;

    /** 감시할 파일명 또는 패턴. yaml의 'file'에 매핑 */
    @JsonProperty("file")
    private String fileNamePattern;

    /** 데이터 형식: LOG, CSV, JSON */
    @JsonProperty("format")
    private String format = "LOG";

    /** CSV 첫 행 헤더 포함 여부 (true=첫 행을 컬럼명으로 사용) */
    @JsonProperty("csvHasHeader")
    private boolean csvHasHeader = true;

    /** CSV 구분자 (선택사항) */
    @JsonProperty("csvDelimiter")
    private String csvDelimiter = ",";

    /** CSV 컬럼 목록 (csvHasHeader=false일 때 사용) */
    @JsonProperty("csvColumns")
    private String csvColumns;

    /** 새 데이터 폴링 주기 (밀리초) */
    @JsonProperty("pollIntervalMs")
    private long pollIntervalMs = 1000;

    /** 큐 점유를 방지하기 위한 폴링당 최대 읽기 라인 수 */
    @JsonProperty("maxLinesPerPoll")
    private int maxLinesPerPoll = 1000;

    /** 로그 파일 읽기 인코딩 */
    @JsonProperty("charset")
    private String charset = "UTF-8";

    // [2026-02-25] 3번: 단일 레코드 content 최대 바이트 크기 (0이면 제한 없음)
    // 초과 시 잘라내고 [TRUNCATED] 마커 추가
    /** 레코드 content 최대 바이트. 기본값: 512KB (0=제한 없음) */
    @JsonProperty("maxRecordBytes")
    private int maxRecordBytes = 524_288;

    // [2026-02-25] CollectorConfig 인터페이스 구현
    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    // [2026-02-25] type 필드 getter/setter 추가
    @Override
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getCsvDelimiter() {
        return csvDelimiter;
    }

    public void setCsvDelimiter(String csvDelimiter) {
        this.csvDelimiter = csvDelimiter;
    }

    public boolean isCsvHasHeader() {
        return csvHasHeader;
    }

    public void setCsvHasHeader(boolean csvHasHeader) {
        this.csvHasHeader = csvHasHeader;
    }

    public String getCsvColumns() {
        return csvColumns;
    }

    public void setCsvColumns(String csvColumns) {
        this.csvColumns = csvColumns;
    }

    // [2026-02-25] CollectorConfig 인터페이스 구현
    @Override
    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    // [2026-02-25] CollectorConfig 인터페이스 구현
    @Override
    public int getMaxLinesPerPoll() {
        return maxLinesPerPoll;
    }

    public void setMaxLinesPerPoll(int maxLinesPerPoll) {
        this.maxLinesPerPoll = maxLinesPerPoll;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    // [2026-02-25] 3번: maxRecordBytes getter/setter 추가
    @Override
    public int getMaxRecordBytes() {
        return maxRecordBytes;
    }

    public void setMaxRecordBytes(int maxRecordBytes) {
        this.maxRecordBytes = maxRecordBytes;
    }
}
