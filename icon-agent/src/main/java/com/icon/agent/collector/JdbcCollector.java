package com.icon.agent.collector;

import com.icon.agent.config.JdbcCollectorConfig;
import com.icon.agent.queue.RecordQueue;
import com.icon.agent.store.JdbcWatermarkRecord;
import com.icon.agent.store.JdbcWatermarkStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Map;
import java.util.concurrent.*;

/**
 * JDBC 수집기. 하이워터마크 기반으로 데이터베이스 테이블을 폴링한다.
 *
 * [2026-03-05] FilePositionStore + Map<String,PositionRecord> 구조에서
 *              JdbcWatermarkStore + JdbcWatermarkRecord 구조로 전환.
 *              워터마크는 watermark.dat 에 독립 저장 — FilePositionStore와 완전 분리.
 *              스레드명에 config.getId() 추가 — 다중 인스턴스 시 고유성 보장.
 */
public class JdbcCollector {

    private static final Logger log = LoggerFactory.getLogger(JdbcCollector.class);

    private final String targetId;
    private final JdbcCollectorConfig config;
    private final RecordQueue queue;
    private final JdbcWatermarkStore watermarkStore;

    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    public JdbcCollector(String targetId,
            JdbcCollectorConfig config,
            RecordQueue queue,
            JdbcWatermarkStore watermarkStore) throws Exception {
        this.targetId = targetId;
        this.config = config;
        this.queue = queue;
        this.watermarkStore = watermarkStore;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "jdbc-collector-" + targetId + "-" + config.getId());
            t.setDaemon(true);
            return t;
        });

        // ShadowJar fat jar 환경에서 ServiceLoader가 드라이버를 못 찾는 문제 대비
        // URL에서 DB 종류를 감지하여 Class.forName()으로 명시 로드
        loadDriver(config.getUrl());
    }

    /**
     * JDBC URL 접두사로 드라이버 클래스를 감지하여 명시적으로 로드한다.
     * ShadowJar fat jar 환경에서 DriverManager SPI 자동 등록이 실패하는 경우 대비.
     * [2026-03-13] 지원 대상: MARIADB, POSTGRESQL, ORACLE
     *   jdbc:mariadb://     → org.mariadb.jdbc.Driver
     *   jdbc:postgresql://  → org.postgresql.Driver
     *   jdbc:oracle:thin:   → oracle.jdbc.OracleDriver
     * 새 DB 타입 추가 시 이 메서드의 if-else에만 추가하면 된다.
     */
    private void loadDriver(String url) {
        if (url == null) return;
        String driverClass = null;
        if (url.startsWith("jdbc:mariadb:")) {
            driverClass = "org.mariadb.jdbc.Driver";
        } else if (url.startsWith("jdbc:postgresql:")) {
            driverClass = "org.postgresql.Driver";
        } else if (url.startsWith("jdbc:oracle:")) {
            driverClass = "oracle.jdbc.OracleDriver";
        }
        if (driverClass != null) {
            try {
                Class.forName(driverClass);
                log.info("[{}] JDBC driver loaded: {}", targetId, driverClass);
            } catch (ClassNotFoundException e) {
                log.error("[{}] JDBC driver not found: {} — check build.gradle dependencies", targetId, driverClass);
            }
        }
    }

    public void start() {
        if (!config.isEnabled()) {
            log.info("[{}] JDBC collector disabled", targetId);
            return;
        }
        running = true;
        scheduler.scheduleWithFixedDelay(this::poll,
                0,
                config.getPollIntervalMs(),
                TimeUnit.MILLISECONDS);
        log.info("[{}] JdbcCollector started, poll={}ms", targetId, config.getPollIntervalMs());
    }

    /** 수집기 ID (동적 추가/제거 시 식별용) */
    public String getId() {
        return config.getId();
    }

    public void stop() {
        running = false;
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[{}] JdbcCollector stopped", targetId);
    }

    private void poll() {
        if (!running) return;
        MDC.put("targetId", targetId);
        try {
            // field1 / field2 : 각 ? 위치의 추적 컬럼명, 바인딩 타입, 초기값
            String trackingCol1  = config.getField1();
            String bindType1     = config.getField1Type();
            String initialValue1 = config.getField1Value();

            String trackingCol2  = config.getField2();
            String bindType2     = config.getField2Type();
            String initialValue2 = config.getField2Value();

            boolean hasField2 = trackingCol2 != null && !trackingCol2.isEmpty();

            // [2026-03-05] JdbcWatermarkStore.load()로 watermark 조회 (구: pos.getJdbcLastValue())
            JdbcWatermarkRecord wm = watermarkStore.load();
            String lastValue1 = (wm != null && wm.getValue1() != null) ? wm.getValue1() : initialValue1;
            String lastValue2 = (wm != null && wm.getValue2() != null) ? wm.getValue2() : initialValue2;

            try (Connection conn = DriverManager.getConnection(
                    config.getUrl(), config.getUsername(), config.getPassword());
                    PreparedStatement ps = conn.prepareStatement(config.getQuery())) {

                // ? 위치 1 바인딩
                bindParameter(ps, 1, lastValue1, bindType1);
                // ? 위치 2 바인딩 (field2가 설정된 경우에만)
                if (hasField2) {
                    bindParameter(ps, 2, lastValue2, bindType2);
                }

                log.info("[{}] JDBC poll — field1={} ({}), field2={}, sql = {}",
                        targetId, lastValue1, bindType1, hasField2 ? lastValue2 + " (" + bindType2 + ")" : "없음", config.getQuery());

                ResultSet rs = ps.executeQuery();
                ResultSetMetaData meta = rs.getMetaData();
                int cols = meta.getColumnCount();
                String newLastValue1 = lastValue1;
                String newLastValue2 = lastValue2;
                int count = 0;

                while (rs.next()) {
                    StringBuilder sb = new StringBuilder("{");
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1)
                            sb.append(",");
                        sb.append('"').append(meta.getColumnLabel(i)).append('"')
                                .append(":\"").append(rs.getString(i)).append('"');
                    }
                    sb.append("}");

                    // field1 하이워터마크 갱신
                    if (trackingCol1 != null) {
                        String tv = rs.getString(trackingCol1);
                        if (tv != null) newLastValue1 = tv;
                    }
                    // field2 하이워터마크 갱신 (설정된 경우)
                    if (hasField2) {
                        String tv = rs.getString(trackingCol2);
                        if (tv != null) newLastValue2 = tv;
                    }

                    // [2026-02-25] 3번: 레코드 크기 제한 — maxRecordBytes 초과 시 잘라내기
                    String content = truncateIfNeeded(sb.toString(), config.getQuery());

                    if (!queue.offer(new Record(targetId, config.getId(), Record.Source.JDBC,
                            config.getQuery(), content,
                            Map.of("trackingColumn", trackingCol1 != null ? trackingCol1 : "")))) {
                        log.warn("[{}] Queue full — stopping JDBC poll cycle", targetId);
                        break;
                    }
                    count++;
                }
                rs.close();

                // [2026-03-05] watermark 변경 시 JdbcWatermarkStore.save()로 즉시 저장
                //              (구: positionStore.update(posKey, pos, positions))
                boolean changed1 = newLastValue1 != null && !newLastValue1.equals(lastValue1);
                boolean changed2 = hasField2 && newLastValue2 != null && !newLastValue2.equals(lastValue2);
                if (changed1 || changed2) {
                    JdbcWatermarkRecord updated = new JdbcWatermarkRecord(
                            changed1 ? newLastValue1 : lastValue1,
                            hasField2 ? (changed2 ? newLastValue2 : lastValue2) : null);
                    watermarkStore.save(updated);
                    log.debug("[{}] JDBC collected {} row(s), watermark1={}, watermark2={}",
                            targetId, count, newLastValue1, hasField2 ? newLastValue2 : "-");
                }
            }
        } catch (Exception e) {
            log.error("[{}] JDBC poll error: {}", targetId, e.getMessage(), e);
        } finally {
            MDC.remove("targetId");
        }
    }

    /**
     * trackingColumnType에 따라 PreparedStatement 파라미터를 적절한 타입으로 바인딩한다.
     * - TIMESTAMP : setTimestamp() — PostgreSQL/Oracle timestamp 컬럼 타입 오류 방지
     * - NUMBER    : setLong()     — 숫자형 시퀀스/ID 컬럼
     * - STRING    : setString()   — 기본값 (MySQL varchar 등)
     */
    private void bindParameter(PreparedStatement ps, int idx, String value, String type)
            throws java.sql.SQLException {
        switch (type) {
            case "TIMESTAMP":
                try {
                    ps.setTimestamp(idx, java.sql.Timestamp.valueOf(value));
                } catch (IllegalArgumentException e) {
                    // 파싱 실패 시 문자열 그대로 바인딩 (하위 호환)
                    log.warn("[{}] TIMESTAMP 파싱 실패, 문자열로 바인딩: {}", targetId, value);
                    ps.setString(idx, value);
                }
                break;
            case "NUMBER":
                try {
                    ps.setLong(idx, Long.parseLong(value));
                } catch (NumberFormatException e) {
                    log.warn("[{}] NUMBER 파싱 실패, 문자열로 바인딩: {}", targetId, value);
                    ps.setString(idx, value);
                }
                break;
            default: // STRING
                ps.setString(idx, value);
                break;
        }
    }

    /**
     * content가 maxRecordBytes를 초과하면 잘라내고 [TRUNCATED] 마커를 추가한다.
     * [2026-02-25] 3번: JDBC BLOB/CLOB 등 대용량 컬럼 대비 레코드 크기 제한
     *              maxRecordBytes=0이면 제한 없음
     */
    private String truncateIfNeeded(String content, String source) {
        int limit = config.getMaxRecordBytes();
        if (limit <= 0 || content == null) {
            return content;
        }
        byte[] utf8Bytes = content.getBytes(StandardCharsets.UTF_8);
        if (utf8Bytes.length <= limit) {
            return content;
        }
        int approxChars = (int) ((long) limit * content.length() / utf8Bytes.length) - 60;
        approxChars = Math.max(0, approxChars);
        String marker = String.format("...[TRUNCATED: %d→%d bytes]", utf8Bytes.length, limit);
        log.warn("[{}] JDBC 레코드 크기 초과 — 잘라냄: {} ({} bytes > {} bytes 제한)",
                targetId, source, utf8Bytes.length, limit);
        return content.substring(0, approxChars) + marker;
    }
}
