package com.itmasters.icon.engine.datasource.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsDatabaseConfigEntity;
import com.itmasters.icon.engine.datasource.repository.DatabaseConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.*;

/**
 * [2026-03-13] DATABASE 데이터소스 JDBC 폴링 서비스.
 *
 * 동작 방식:
 *  - agent_id 미설정: engine이 직접 JDBC 폴링 수행 (직접 모드)
 *  - agent_id 설정:   에이전트가 JDBC 폴링 후 Push (에이전트 모드) → 빈 목록 반환
 *
 * icon-agent의 JdbcCollector 로직을 참조하여 하이워터마크 기반 증분 수집.
 * mainQuery에 ? 플레이스홀더를 통해 lastProcessedValue를 바인딩한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatabaseService {

    private final DatabaseConfigRepository databaseConfigRepository;

    /**
     * dataSourceId 기반으로 DATABASE 증분 데이터 읽기.
     * 성공 시 lastProcessedValue(하이워터마크)를 DB에 갱신한다.
     *
     * @param dataSourceId 데이터소스 ID
     * @return 수집된 레코드 목록 (에이전트 모드 시 빈 목록)
     */
    @Transactional
    public List<Map<String, Object>> readIncrementalData(String dataSourceId) {
        EngineDsDatabaseConfigEntity config = databaseConfigRepository.findByDataSourceId(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "DATABASE 설정을 찾을 수 없습니다: " + dataSourceId));

        // [2026-03-13] 에이전트 모드: agent_id 설정 시 에이전트가 Push하므로 직접 읽기 스킵
        if (config.getAgentId() != null && !config.getAgentId().isBlank()) {
            log.info("[{}] 에이전트 폴링 모드 - 로컬 JDBC 읽기 스킵 (에이전트 push 대기)", dataSourceId);
            return List.of();
        }

        String query = config.getMainQuery();
        if (query == null || query.isBlank()) {
            log.warn("[{}] DATABASE mainQuery가 설정되지 않았습니다.", dataSourceId);
            return List.of();
        }

        String jdbcUrl = buildJdbcUrl(config);
        loadDriver(jdbcUrl);

        log.info("[{}] DATABASE 직접 폴링 시작 - incrementalColumn={}, lastValue={}",
                dataSourceId, config.getIncrementalColumn(), config.getLastProcessedValue());

        List<Map<String, Object>> records = new ArrayList<>();
        String newLastValue = config.getLastProcessedValue();

        try (Connection conn = DriverManager.getConnection(
                jdbcUrl, config.getUsername(), config.getPasswordEncrypted());
             PreparedStatement ps = conn.prepareStatement(query)) {

            // incrementalColumn 기반 하이워터마크 바인딩
            bindIncrementalParam(ps, 1, config.getLastProcessedValue(), config.getIncrementalColumnType());

            int batchSize = config.getBatchSize() != null && config.getBatchSize() > 0
                    ? config.getBatchSize() : 1000;
            ps.setMaxRows(batchSize);

            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                records.add(row);

                // 하이워터마크 갱신
                if (config.getIncrementalColumn() != null) {
                    String val = rs.getString(config.getIncrementalColumn());
                    if (val != null) newLastValue = val;
                }
            }
            rs.close();

            log.info("[{}] DATABASE 폴링 완료 - {}건, newLastValue={}",
                    dataSourceId, records.size(), newLastValue);

        } catch (SQLException e) {
            log.error("[{}] DATABASE 폴링 오류: {}", dataSourceId, e.getMessage(), e);
            throw new RuntimeException("DATABASE 폴링 실패: " + e.getMessage(), e);
        }

        // 하이워터마크 변경 시 DB 갱신
        if (!records.isEmpty() && !Objects.equals(newLastValue, config.getLastProcessedValue())) {
            config.updateLastProcessedValue(newLastValue);
            databaseConfigRepository.save(config);
        }

        return records;
    }

    /**
     * [2026-03-13] databaseType + host + port + databaseName 조합으로 JDBC URL 생성.
     * 지원 대상: MARIADB, POSTGRESQL, ORACLE
     * 새 DB 타입 추가 시 이 메서드의 switch에만 case를 추가하면 된다.
     */
    private String buildJdbcUrl(EngineDsDatabaseConfigEntity config) {
        String type = config.getDatabaseType() != null ? config.getDatabaseType().toUpperCase() : "";
        String host = config.getHost();
        int port = config.getPort() != null ? config.getPort() : 0;
        String db = config.getDatabaseName();
        String schema = config.getSchemaName();

        switch (type) {
            case "MARIADB":
                // MariaDB 기본 포트: 3306
                return String.format("jdbc:mariadb://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8", host, port, db);
            case "POSTGRESQL":
            case "POSTGRES":
                // PostgreSQL 기본 포트: 5432 / schema 설정 시 currentSchema 파라미터 사용
                if (schema != null && !schema.isBlank()) {
                    return String.format("jdbc:postgresql://%s:%d/%s?currentSchema=%s", host, port, db, schema);
                }
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, db);
            case "ORACLE":
                // Oracle 기본 포트: 1521 / SID 방식 사용
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, db);
            default:
                throw new IllegalArgumentException("지원하지 않는 databaseType: " + type
                        + " (지원: MARIADB, POSTGRESQL, ORACLE)");
        }
    }

    /**
     * incrementalColumnType에 따라 PreparedStatement 파라미터 바인딩.
     * icon-agent JdbcCollector.bindParameter()와 동일한 방식.
     */
    private void bindIncrementalParam(PreparedStatement ps, int idx, String value, String type)
            throws SQLException {
        if (value == null) {
            ps.setNull(idx, Types.VARCHAR);
            return;
        }
        String t = type != null ? type.toUpperCase() : "STRING";
        switch (t) {
            case "NUMBER":
                try {
                    ps.setLong(idx, Long.parseLong(value));
                } catch (NumberFormatException e) {
                    log.warn("NUMBER 파싱 실패, 문자열로 바인딩: {}", value);
                    ps.setString(idx, value);
                }
                break;
            case "DATETIME":
            case "TIMESTAMP":
                try {
                    ps.setTimestamp(idx, Timestamp.valueOf(value));
                } catch (IllegalArgumentException e) {
                    log.warn("TIMESTAMP 파싱 실패, 문자열로 바인딩: {}", value);
                    ps.setString(idx, value);
                }
                break;
            default:
                ps.setString(idx, value);
        }
    }

    /**
     * [2026-03-13] JDBC URL 접두사로 드라이버를 감지하여 명시 로드.
     * 지원 대상: MARIADB(org.mariadb.jdbc.Driver), POSTGRESQL, ORACLE
     * 새 드라이버 추가 시 이 메서드의 if-else에만 추가하면 된다.
     */
    private void loadDriver(String url) {
        if (url == null) return;
        String driverClass = null;
        if (url.startsWith("jdbc:mariadb:"))         driverClass = "org.mariadb.jdbc.Driver";
        else if (url.startsWith("jdbc:postgresql:")) driverClass = "org.postgresql.Driver";
        else if (url.startsWith("jdbc:oracle:"))     driverClass = "oracle.jdbc.OracleDriver";

        if (driverClass != null) {
            try {
                Class.forName(driverClass);
                log.debug("JDBC 드라이버 로드 완료: {}", driverClass);
            } catch (ClassNotFoundException e) {
                log.warn("JDBC 드라이버를 찾을 수 없습니다: {} — build.gradle 의존성 확인", driverClass);
            }
        }
    }
}
