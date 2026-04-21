package com.itmasters.icon.engine.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * [2026-03-13] DATABASE 데이터소스 설정 엔티티 (엔진 전용 읽기 뷰)
 * ds_database_config 테이블과 매핑.
 * JDBC 폴링 수집에 필요한 연결 정보 및 하이워터마크를 보관한다.
 */
@Entity
@Table(name = "ds_database_config")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EngineDsDatabaseConfigEntity {

    @Id
    @Column(name = "ds_database_config_id")
    private String dsDatabaseConfigId;

    @Column(name = "data_source_id", nullable = false)
    private String dataSourceId;

    @Column(name = "connection_name", length = 100)
    private String connectionName;

    @Column(name = "database_type", length = 50)
    private String databaseType;

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

    @Column(name = "main_query", columnDefinition = "TEXT")
    private String mainQuery;

    @Column(name = "incremental_column", length = 200)
    private String incrementalColumn;

    /** NUMBER | DATETIME */
    @Column(name = "incremental_column_type", length = 50)
    private String incrementalColumnType;

    // [2026-04-21] 증분 컬럼 초기값 — 첫 수집 시 lastProcessedValue 가 null 인 경우 사용
    @Column(name = "incremental_column_initial_value", length = 200)
    private String incrementalColumnInitialValue;

    @Column(name = "batch_size")
    private Integer batchSize;

    /** 마지막으로 처리된 incrementalColumn 값 (하이워터마크) */
    @Column(name = "last_processed_value", length = 200)
    private String lastProcessedValue;

    @Column(name = "last_query_time")
    private LocalDateTime lastQueryTime;

    @Column(name = "is_active")
    private Boolean isActive;

    // [2026-03-13] 에이전트 폴링 모드 - agent_id 설정 시 에이전트가 직접 JDBC 폴링 후 Push
    @Column(name = "agent_id", length = 100)
    private String agentId;

    /** 하이워터마크 갱신 */
    public void updateLastProcessedValue(String value) {
        this.lastProcessedValue = value;
        this.lastQueryTime = LocalDateTime.now();
    }
}
