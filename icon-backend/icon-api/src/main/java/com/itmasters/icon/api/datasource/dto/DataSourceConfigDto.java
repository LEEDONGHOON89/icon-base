package com.itmasters.icon.api.datasource.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataSourceConfigDto {
    private String type; // DATABASE | FILE_SYSTEM
    private FileSystem fileSystem;
    private Database database;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileSystem {
        private String dsFileSystemConfigId;
        private String dataSourceId;
        private String connectionName;
        private String watchDirectory;
        private String filePattern;
        private String fileEncoding;
        private String delimiter;
        private String quoteChar;
        private String escapeChar;
        private Boolean hasHeader;
        private Integer skipLines;
        private String processingStrategy;
        private Integer scanIntervalMinutes;
        private Boolean moveProcessedFiles;
        private String processedFilesDirectory;
        private Boolean isActive;
        private String connectionStatus;
        private String lastErrorMessage;
        // [2026-03-12] FILE_SYSTEM_REALTIME → 에이전트 연결 정보 (UI에서 에이전트 선택 시 설정)
        private String agentId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Database {
        private String dsDatabaseConfigId;
        private String dataSourceId;
        private String connectionName;
        private String databaseType;
        private String host;
        private Integer port;
        private String databaseName;
        private String schemaName;
        private String username;
        private String password; // write-only; mapped to password_encrypted
        private Integer minPoolSize;
        private Integer maxPoolSize;
        private Integer connectionTimeoutSeconds;
        private Integer idleTimeoutSeconds;
        private String mainQuery;
        private String incrementalColumn;
        private String incrementalColumnType;
        private Integer batchSize;
        private Boolean isActive;
        private String connectionStatus;
        private String lastErrorMessage;
        // [2026-03-13] DATABASE → 에이전트 연결 정보 (에이전트가 JDBC 폴링 후 Push하는 경우)
        private String agentId;
    }
}

