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

    // [2026-04-21] 증분 컬럼 초기값 — 첫 수집 시 lastProcessedValue 가 null 인 경우 사용
    @Column(name = "incremental_column_initial_value", length = 200)
    private String incrementalColumnInitialValue;

    // [2026-04-22] 보조 증분 컬럼 — 복합 키 기반 증분 수집 지원 (선택사항)
    @Column(name = "secondary_incremental_column", length = 200)
    private String secondaryIncrementalColumn;

    @Column(name = "secondary_incremental_column_type", length = 50)
    private String secondaryIncrementalColumnType;

    @Column(name = "secondary_incremental_column_initial_value", length = 200)
    private String secondaryIncrementalColumnInitialValue;

    @Column(name = "last_secondary_processed_value", length = 200)
    private String lastSecondaryProcessedValue;

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

    // [2026-04-21] 에이전트 폴링 설정 — NULL 시 기본값으로 대체
    @Column(name = "poll_interval_ms")
    private Long pollIntervalMs;          // NULL → 300000 (5분)

    @Column(name = "max_lines_per_poll")
    private Integer maxLinesPerPoll;      // NULL → 1000

    @Column(name = "max_record_bytes")
    private Integer maxRecordBytes;       // NULL → 524288 (512 KB)

    public void applyAgentLink(String agentId) {
        this.agentId = agentId;
    }

    // [2026-04-22] 수집 하이워터마크 초기화 — 처음부터 재수집 지원 (보조 컬럼 포함)
    public void resetWatermark() {
        this.lastProcessedValue = null;
        this.lastSecondaryProcessedValue = null;
        this.lastQueryTime = null;
    }

    // [2026-04-21] DataSourceEntity.activate()/deactivate() 와 동기화용
    public void activate()   { this.isActive = true; }
    public void deactivate() { this.isActive = false; }

    // [2026-04-21] 에이전트 폴링 설정 업데이트
    public void applyPollSettings(Long pollIntervalMs, Integer maxLinesPerPoll, Integer maxRecordBytes) {
        this.pollIntervalMs = pollIntervalMs;
        this.maxLinesPerPoll = maxLinesPerPoll;
        this.maxRecordBytes = maxRecordBytes;
    }

    // [2026-04-21] passwordEncrypted: null/blank 입력 시 기존 값 유지
    //              API 응답에 비밀번호가 포함되지 않으므로, 화면에서 재입력하지 않으면
    //              password 필드가 비어 있는 상태로 저장 요청이 오는 경우 기존 값을 보존해야 한다.
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
        // [2026-04-21] 비밀번호는 새 값이 입력된 경우에만 갱신 (미입력 시 기존 값 유지)
        if (passwordEncrypted != null && !passwordEncrypted.isBlank()) {
            this.passwordEncrypted = passwordEncrypted;
        }
        this.minPoolSize = minPoolSize;
        this.maxPoolSize = maxPoolSize;
        this.connectionTimeoutSeconds = connectionTimeoutSeconds;
        this.idleTimeoutSeconds = idleTimeoutSeconds;
    }

    // [2026-04-22] 보조 증분 컬럼 파라미터 추가
    public void applyIngestion(String mainQuery,
                               String incrementalColumn,
                               String incrementalColumnType,
                               String incrementalColumnInitialValue,
                               String secondaryIncrementalColumn,
                               String secondaryIncrementalColumnType,
                               String secondaryIncrementalColumnInitialValue,
                               Integer batchSize) {
        this.mainQuery = mainQuery;
        this.incrementalColumn = incrementalColumn;
        this.incrementalColumnType = incrementalColumnType;
        this.incrementalColumnInitialValue = incrementalColumnInitialValue;
        // [2026-04-22] 보조 증분 컬럼 — null/blank 입력 시 기존 하이워터마크도 초기화
        String prevSecondary = this.secondaryIncrementalColumn;
        this.secondaryIncrementalColumn = (secondaryIncrementalColumn != null && !secondaryIncrementalColumn.isBlank())
                ? secondaryIncrementalColumn : null;
        this.secondaryIncrementalColumnType = (secondaryIncrementalColumnType != null && !secondaryIncrementalColumnType.isBlank())
                ? secondaryIncrementalColumnType : null;
        this.secondaryIncrementalColumnInitialValue = (secondaryIncrementalColumnInitialValue != null && !secondaryIncrementalColumnInitialValue.isBlank())
                ? secondaryIncrementalColumnInitialValue : null;
        // 보조 컬럼이 제거된 경우 보조 하이워터마크 초기화
        if (prevSecondary != null && this.secondaryIncrementalColumn == null) {
            this.lastSecondaryProcessedValue = null;
        }
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
