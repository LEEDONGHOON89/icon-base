package com.itmasters.icon.api.datasource.adapter.out.persistence.entity;

import com.itmasters.icon.entity.Auditable;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 데이터베이스 데이터소스 연결 설정 엔티티
 * ds_database_config 테이블과 매핑
 */
@Entity
@Table(name = "ds_database_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DatabaseConfigEntity extends Auditable {

    @Id
    @Column(name = "ds_database_config_id")
    private String dsDatabaseConfigId;

    @Column(name = "data_source_id", nullable = false)
    private String dataSourceId;

    @Column(name = "connection_name", nullable = false, length = 100)
    private String connectionName;

    @Column(name = "database_type", length = 50)
    private String databaseType; // e.g., POSTGRES, MYSQL

    @Column(name = "host", length = 200)
    private String host;

    @Column(name = "port")
    private Integer port;

    @Column(name = "database_name", length = 200)
    private String databaseName;

    @Column(name = "schema_name", length = 200)
    private String schemaName;

    @Column(name = "username", length = 200)
    private String username;

    @Column(name = "password_encrypted", columnDefinition = "TEXT")
    private String passwordEncrypted;

    @Column(name = "min_pool_size")
    private Integer minPoolSize;

    @Column(name = "max_pool_size")
    private Integer maxPoolSize;

    @Column(name = "connection_timeout_seconds")
    private Integer connectionTimeoutSeconds;

    @Column(name = "idle_timeout_seconds")
    private Integer idleTimeoutSeconds;

    @Column(name = "main_query", columnDefinition = "TEXT")
    private String mainQuery;

    @Column(name = "incremental_column", length = 200)
    private String incrementalColumn;

    @Column(name = "incremental_column_type", length = 50)
    private String incrementalColumnType; // e.g., NUMBER, DATETIME

    @Column(name = "batch_size")
    private Integer batchSize;

    @Column(name = "last_processed_value", length = 200)
    private String lastProcessedValue;

    @Column(name = "last_query_time")
    private LocalDateTime lastQueryTime;

    @Column(name = "total_queries_executed")
    private Integer totalQueriesExecuted;

    @Column(name = "total_records_fetched")
    private Long totalRecordsFetched;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "connection_status", length = 20)
    private String connectionStatus = "IDLE";

    @Column(name = "last_error_message", columnDefinition = "TEXT")
    private String lastErrorMessage;

    @Column(name = "last_error_time")
    private LocalDateTime lastErrorTime;

    // [2026-03-13] 에이전트 폴링 모드 - agent_id 설정 시 에이전트가 JDBC 폴링 후 Push
    @Column(name = "agent_id", length = 100)
    private String agentId;

    public void applyAgentLink(String agentId) {
        this.agentId = agentId;
    }

    public void applyBasic(String connectionName,
                           String databaseType,
                           String host,
                           Integer port,
                           String databaseName,
                           String schemaName,
                           String username,
                           String passwordEncrypted,
                           Integer minPoolSize,
                           Integer maxPoolSize,
                           Integer connectionTimeoutSeconds,
                           Integer idleTimeoutSeconds) {
        this.connectionName = connectionName;
        this.databaseType = databaseType;
        this.host = host;
        this.port = port;
        this.databaseName = databaseName;
        this.schemaName = schemaName;
        this.username = username;
        this.passwordEncrypted = passwordEncrypted;
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    public void applyIngestion(String mainQuery,
                               String incrementalColumn,
                               String incrementalColumnType,
                               Integer batchSize) {
        this.mainQuery = mainQuery;
        this.incrementalColumn = incrementalColumn;
        this.incrementalColumnType = incrementalColumnType;
        this.batchSize = batchSize;
    }

    public void assignIdsIfNew(String dsDatabaseConfigId, String dataSourceId) {
        if (this.dsDatabaseConfigId == null) this.dsDatabaseConfigId = dsDatabaseConfigId;
        if (this.dataSourceId == null) this.dataSourceId = dataSourceId;
    }

    /**
     * 새 엔티티 인스턴스 생성 (JPA 보호 생성자 제한을 우회하는 정적 팩토리)
     */
    public static DatabaseConfigEntity create(String dsDatabaseConfigId, String dataSourceId) {
        DatabaseConfigEntity e = new DatabaseConfigEntity();
        e.dsDatabaseConfigId = dsDatabaseConfigId;
        e.dataSourceId = dataSourceId;
        return e;
    }
}
