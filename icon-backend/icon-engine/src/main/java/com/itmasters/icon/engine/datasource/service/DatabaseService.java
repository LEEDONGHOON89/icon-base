package com.itmasters.icon.engine.datasource.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsDatabaseConfigEntity;
import com.itmasters.icon.engine.datasource.repository.DatabaseConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

        // [2026-04-21] 첫 수집(lastProcessedValue=null)이면 사용자 설정 초기값을 우선 사용
        String effectiveLastValue = config.getLastProcessedValue();
        if (effectiveLastValue == null && config.getIncrementalColumnInitialValue() != null
                && !config.getIncrementalColumnInitialValue().isBlank()) {
            effectiveLastValue = config.getIncrementalColumnInitialValue();
            log.info("[{}] 첫 수집 — 사용자 설정 초기값 사용: incrementalColumnInitialValue={}",
                    dataSourceId, effectiveLastValue);
        }

        log.info("[{}] DATABASE 직접 폴링 시작 - incrementalColumn={}, lastValue={}",
                dataSourceId, config.getIncrementalColumn(), effectiveLastValue);

        // [2026-04-22] maxLinesPerPoll / maxRecordBytes 백엔드 직접 수집에도 적용
        int maxLines = (config.getMaxLinesPerPoll() != null && config.getMaxLinesPerPoll() > 0)
                       ? config.getMaxLinesPerPoll() : 0; // 0 = batchSize 기준
        int maxBytes = (config.getMaxRecordBytes()  != null && config.getMaxRecordBytes()  > 0)
                       ? config.getMaxRecordBytes()  : 0; // 0 = 제한 없음

        List<Map<String, Object>> records = new ArrayList<>();
        String newLastValue = effectiveLastValue;

        try (Connection conn = DriverManager.getConnection(
                jdbcUrl, config.getUsername(), config.getPasswordEncrypted());
             PreparedStatement ps = conn.prepareStatement(query)) {

            // [2026-04-21] effectiveLastValue 사용 (초기값 또는 하이워터마크)
            bindIncrementalParam(ps, 1, effectiveLastValue, config.getIncrementalColumnType());

            // [2026-04-22] SQL 레벨 행 수 제한: maxLinesPerPoll 우선, 없으면 batchSize
            int batchSize = config.getBatchSize() != null && config.getBatchSize() > 0
                    ? config.getBatchSize() : 1000;
            ps.setMaxRows(maxLines > 0 ? maxLines : batchSize);

            ResultSet rs = ps.executeQuery();
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            // [2026-04-22] maxLinesPerPoll 자바 레벨 루프 제한 (setMaxRows 보완)
            while (rs.next() && (maxLines <= 0 || records.size() < maxLines)) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }

                // [2026-04-22] maxRecordBytes: 행 크기 추정 후 초과 시 문자열 값 잘라내기
                if (maxBytes > 0) {
                    truncateRowIfNeeded(row, maxBytes, dataSourceId);
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
     * [2026-04-21] incrementalColumnType에 따라 PreparedStatement 파라미터 바인딩.
     * icon-agent JdbcCollector.bindParameter()와 동일한 방식.
     *
     * 수정 내용:
     *  - null(초회 수집) 처리: SQL NULL 대신 타입별 sentinel 값 사용
     *    · TIMESTAMP/DATETIME → 1970-01-01 00:00:00 (에포크) → 전체 레코드 수집
     *    · NUMBER             → 0L                           → 전체 레코드 수집
     *    · STRING             → ""                           → 전체 레코드 수집
     *    (기존: setNull(Types.VARCHAR) → timestamp > character varying 타입 불일치 오류)
     *    (이전 수정: setNull(Types.TIMESTAMP) → 타입 오류는 해결되나 NULL 비교로 0건 수집)
     *  - TIMESTAMP 파싱 실패 fallback을 setString → setTimestamp(에포크)로 변경
     *  - ISO 8601, 날짜만 있는 형식 등 다양한 포맷 지원
     */
    private static final List<DateTimeFormatter> TIMESTAMP_FORMATTERS = List.of(
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"),
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,           // yyyy-MM-ddTHH:mm:ss
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    );

    /** 초회 수집 시 TIMESTAMP 타입의 하이워터마크 시작 기준값 (에포크: 1970-01-01 00:00:00) */
    private static final Timestamp EPOCH_TIMESTAMP = Timestamp.valueOf("1970-01-01 00:00:00");

    private void bindIncrementalParam(PreparedStatement ps, int idx, String value, String type)
            throws SQLException {
        // [2026-04-21] type이 null인 경우 값 형태로 자동 추론
        //              incremental_column_type 이 DB에 NULL로 저장된 레코드 대응
        String t = resolveColumnType(type, value);

        // [2026-04-21] null = 초회 수집: SQL NULL 대신 sentinel 값으로 전체 레코드 수집
        if (value == null) {
            switch (t) {
                case "NUMBER":
                    log.debug("[bindIncrementalParam] 초회 수집 — NUMBER sentinel: 0");
                    ps.setLong(idx, 0L);
                    break;
                case "DATETIME":
                case "TIMESTAMP":
                    log.debug("[bindIncrementalParam] 초회 수집 — TIMESTAMP sentinel: 1970-01-01 00:00:00");
                    ps.setTimestamp(idx, EPOCH_TIMESTAMP);
                    break;
                default:
                    log.debug("[bindIncrementalParam] 초회 수집 — STRING sentinel: ''");
                    ps.setString(idx, "");
            }
            return;
        }

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
                // [2026-04-21] 다양한 날짜 포맷 순서대로 시도; 모두 실패 시 에포크 타임스탬프 사용
                Timestamp ts = parseTimestamp(value);
                if (ts != null) {
                    ps.setTimestamp(idx, ts);
                } else {
                    log.warn("[bindIncrementalParam] TIMESTAMP 파싱 실패 — 에포크로 fallback: value='{}'", value);
                    ps.setTimestamp(idx, EPOCH_TIMESTAMP);
                }
                break;
            default:
                ps.setString(idx, value);
        }
    }

    /**
     * [2026-04-21] incrementalColumnType 자동 추론.
     * DB에 incremental_column_type 이 NULL로 저장된 경우에도 값 형태를 보고 적절한 타입으로 바인딩한다.
     * - 값이 타임스탬프 패턴이면 "TIMESTAMP"
     * - 값이 순수 숫자이면 "NUMBER"
     * - 그 외 "STRING"
     */
    private String resolveColumnType(String type, String value) {
        if (type != null && !type.isBlank()) {
            return type.toUpperCase();
        }
        if (value == null) return "STRING";
        // 숫자 판별
        try { Long.parseLong(value); return "NUMBER"; } catch (NumberFormatException ignored) {}
        // 타임스탬프 판별
        if (parseTimestamp(value) != null) return "TIMESTAMP";
        return "STRING";
    }

    /**
     * [2026-04-21] 여러 날짜 포맷을 순서대로 시도하여 Timestamp 변환.
     * 변환 가능한 포맷이 없으면 null 반환.
     */
    private Timestamp parseTimestamp(String value) {
        // 1) java.sql.Timestamp.valueOf() — "yyyy-MM-dd HH:mm:ss[.nnnnnnnnn]"
        try {
            return Timestamp.valueOf(value);
        } catch (IllegalArgumentException ignored) {
            // fall through
        }
        // 2) DateTimeFormatter 목록 순서대로 시도
        for (DateTimeFormatter fmt : TIMESTAMP_FORMATTERS) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(value, fmt);
                return Timestamp.valueOf(ldt);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    /**
     * [2026-04-22] 행의 총 바이트 크기가 maxBytes를 초과하면 각 문자열 값을 비율대로 잘라내고
     * [TRUNCATED] 마커를 추가한다. 숫자/날짜 등 비문자열 값은 변경하지 않는다.
     * icon-agent JdbcCollector.truncateIfNeeded()와 동일한 방식.
     */
    private void truncateRowIfNeeded(Map<String, Object> row, int maxBytes, String dataSourceId) {
        // 행 전체 직렬화 크기 추정
        String rowStr = row.toString();
        byte[] rowBytes = rowStr.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (rowBytes.length <= maxBytes) return;

        log.warn("[{}] DATABASE 레코드 크기 초과 — 잘라냄: {} bytes > {} bytes 제한",
                dataSourceId, rowBytes.length, maxBytes);

        // 문자열 값만 잘라내기 (비율 기준: maxBytes / 컬럼 수)
        int stringColCount = (int) row.values().stream()
                .filter(v -> v instanceof String).count();
        if (stringColCount == 0) return;

        int bytesPerCol = Math.max(64, maxBytes / stringColCount);
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (!(entry.getValue() instanceof String)) continue;
            String val = (String) entry.getValue();
            byte[] valBytes = val.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            if (valBytes.length > bytesPerCol) {
                int approxChars = (int) ((long) bytesPerCol * val.length() / valBytes.length) - 20;
                approxChars = Math.max(0, approxChars);
                entry.setValue(val.substring(0, approxChars)
                        + String.format("...[TRUNCATED: %d→%d bytes]", valBytes.length, bytesPerCol));
            }
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
