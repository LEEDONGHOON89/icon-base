package com.itmasters.icon.common.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 데이터 소스 타입
 * 데이터 소스의 타입과 메타데이터를 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum DataSourceType {
    DATABASE("DATABASE", "데이터베이스", "관계형 데이터베이스", "database"),
    FILE_SYSTEM("FILE_SYSTEM", "파일 시스템", "로컬 또는 네트워크 파일 시스템", "folder"),
    FILE_SYSTEM_REALTIME("FILE_SYSTEM_REALTIME", "파일 시스템 실시간", "로컬 또는 네트워크 파일 시스템 실시간 데이터", "folder"),
    LOG("LOG", "로그", "애플리케이션 로그 데이터", "file-text"),
    LOG_SERVER("LOG_SERVER", "로그 서버", "중앙 집중식 로그 서버", "server"),
    API("API", "API", "REST API 또는 웹 서비스", "globe"),
    MESSAGE_QUEUE("MESSAGE_QUEUE", "메시지 큐", "Kafka, RabbitMQ 등", "inbox"),
    CLOUD_STORAGE("CLOUD_STORAGE", "클라우드 스토리지", "AWS S3, Azure Blob 등", "cloud"),
    FTP("FTP", "FTP", "FTP/SFTP 서버", "document"),
    SYSLOG("SYSLOG", "Syslog", "Syslog 프로토콜", "signal"),
    ELASTIC_SEARCH("ELASTIC_SEARCH", "Elasticsearch", "Elasticsearch 클러스터", "search"),
    SPLUNK("SPLUNK", "Splunk", "Splunk 로그 분석 플랫폼", "chart");

    private final String value;
    private final String label;
    private final String description;
    private final String iconType;
    
    // Legacy support - for backward compatibility
    public String getDisplayName() {
        return label;
    }

    /**
     * 데이터베이스 타입인지 확인
     */
    public boolean isDatabaseType() {
        return this == DATABASE;
    }

    /**
     * 로그 관련 타입인지 확인
     */
    public boolean isLogType() {
        return this == LOG || this == LOG_SERVER || this == SYSLOG || this == SPLUNK || this == ELASTIC_SEARCH;
    }

    /**
     * 파일 기반 타입인지 확인
     */
    public boolean isFileBasedType() {
        return this == FILE_SYSTEM || this == FILE_SYSTEM_REALTIME || this == CLOUD_STORAGE || this == FTP;
    }

    /**
     * 실시간 스트리밍 타입인지 확인
     */
    public boolean isStreamingType() {
        return this == MESSAGE_QUEUE || this == SYSLOG || this == API;
    }
}