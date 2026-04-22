package com.itmasters.icon.api.datasource.application.service;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DatabaseConfigEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemConfigEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.DatabaseConfigJpaRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.FileSystemConfigJpaRepository;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentTargetConfigJpaRepository;
import com.itmasters.icon.rpcserver.agent.AgentConnectedEvent;
import com.itmasters.icon.rpcserver.agent.AgentRpcWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * [2026-04-21] 에이전트 수집기 스냅샷 서비스.
 *
 * 에이전트가 연결되면 해당 에이전트에 등록된 모든 수집기(FILE + JDBC)를
 * ds_file_system_config / ds_database_config 테이블에서 조회하여
 * COLLECTORS_SYNC 메시지로 한 번에 푸시한다.
 *
 * 기존: 수집기 저장 시 단일 수집기만 동기화
 * 변경: 에이전트 연결 시 + 저장 시 전체 스냅샷 동기화
 *
 * agent_collector_configs / agent_collector_file_configs / agent_collector_jdbc_configs 테이블 불필요 →
 * ds_file_system_config.agent_id / ds_database_config.agent_id 가 단일 진실 공급원(Single Source of Truth)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentSnapshotService {

    private final FileSystemConfigJpaRepository fileSystemConfigJpaRepository;
    private final DatabaseConfigJpaRepository databaseConfigJpaRepository;
    private final AgentTargetConfigJpaRepository agentTargetConfigJpaRepository;
    private final AgentRpcWebSocketHandler wsHandler;

    // ── 기본값 상수 ──────────────────────────────────────────────────────────
    private static final long DEFAULT_FILE_POLL_INTERVAL_MS  = 60_000L;   // 1분
    private static final long DEFAULT_JDBC_POLL_INTERVAL_MS  = 300_000L;  // 5분
    private static final int  DEFAULT_MAX_LINES_PER_POLL     = 1_000;
    private static final int  DEFAULT_MAX_RECORD_BYTES        = 524_288;   // 512 KB

    /**
     * [2026-04-21] 에이전트 HANDSHAKE 완료 이벤트 수신 — 전체 수집기 스냅샷 자동 푸시.
     * AgentRpcWebSocketHandler 가 HANDSHAKE_ACK 전송 후 발행하는 AgentConnectedEvent 를 수신한다.
     */
    @EventListener
    @Transactional(readOnly = true)
    public void onAgentConnected(AgentConnectedEvent event) {
        String agentId = event.getAgentId();
        log.info("[Snapshot] 에이전트 연결 이벤트 수신 - agentId={}, 스냅샷 푸시 시작", agentId);
        pushSnapshot(agentId);
    }

    /**
     * [2026-04-21] 해당 에이전트에 등록된 전체 수집기 스냅샷을 빌드하여 COLLECTORS_SYNC 메시지로 푸시.
     * <ul>
     *   <li>ds_file_system_config WHERE agent_id = agentId → FILE 수집기</li>
     *   <li>ds_database_config   WHERE agent_id = agentId → JDBC 수집기</li>
     * </ul>
     *
     * @param agentId 스냅샷을 푸시할 에이전트 ID
     */
    @Transactional(readOnly = true)
    public void pushSnapshot(String agentId) {
        var targetConfigOpt = agentTargetConfigJpaRepository.findByAgentId(agentId).stream().findFirst();
        // [2026-04-21] agent_target_configs 항목이 없으면 agentId를 targetId 대용으로 사용
        //              에이전트 측 handleCollectorsSync()는 targetId 일치 여부를 검사하지 않으므로 무방함
        String targetId = targetConfigOpt.isPresent()
                ? targetConfigOpt.get().getTargetId()
                : agentId;

        List<Map<String, Object>> collectors = buildSnapshot(agentId);

        log.info("[Snapshot] 스냅샷 빌드 완료 - agentId={}, targetId={}, collectors={}",
                agentId, targetId, collectors.size());

        boolean pushed = wsHandler.pushCollectorsSync(agentId, targetId, collectors);
        if (pushed) {
            log.info("[Snapshot] COLLECTORS_SYNC 전송 완료 - agentId={}, collectors={}", agentId, collectors.size());
        } else {
            log.warn("[Snapshot] COLLECTORS_SYNC 전송 실패 (에이전트 미연결) - agentId={}", agentId);
        }
    }

    /**
     * [2026-04-21] 에이전트의 전체 수집기 목록을 Map 형태로 반환 (스냅샷 조회 API 용).
     *
     * @param agentId 조회할 에이전트 ID
     * @return 수집기 Map 목록
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> buildSnapshot(String agentId) {
        List<Map<String, Object>> collectors = new ArrayList<>();

        // FILE 수집기
        List<FileSystemConfigEntity> fsList = fileSystemConfigJpaRepository.findAllByAgentId(agentId);
        for (FileSystemConfigEntity fs : fsList) {
            collectors.add(buildFileCollector(fs));
        }

        // JDBC 수집기
        List<DatabaseConfigEntity> dbList = databaseConfigJpaRepository.findAllByAgentId(agentId);
        for (DatabaseConfigEntity db : dbList) {
            collectors.add(buildJdbcCollector(db));
        }

        log.debug("[Snapshot] buildSnapshot - agentId={}, file={}, jdbc={}, total={}",
                agentId, fsList.size(), dbList.size(), collectors.size());
        return collectors;
    }

    // ── 수집기 빌더 ─────────────────────────────────────────────────────────

    /**
     * [2026-04-21] FILE 수집기 Map 빌드.
     * ds_file_system_config 엔티티 → agent COLLECTORS_SYNC payload.
     */
    private Map<String, Object> buildFileCollector(FileSystemConfigEntity fs) {
        String collectorId  = "DS_" + fs.getDataSourceId();
        String filePattern  = fs.getFilePattern()  != null ? fs.getFilePattern()  : "*";
        String fileFormat   = resolveFileFormat(filePattern);
        boolean hasHeader   = Boolean.TRUE.equals(fs.getHasHeader());
        String delimiter    = fs.getDelimiter()    != null ? fs.getDelimiter()    : ",";
        String charset      = fs.getFileEncoding() != null ? fs.getFileEncoding() : "UTF-8";
        String directory    = fs.getWatchDirectory()!= null? fs.getWatchDirectory(): "";

        // [2026-04-21] poll_interval_ms 설정 우선, 없으면 scan_interval_minutes 변환
        // [2026-04-22] scan_interval_minutes 단위 변경: 분 → 초. 변환식 * 60_000L → * 1_000L
        long pollIntervalMs;
        if (fs.getPollIntervalMs() != null && fs.getPollIntervalMs() > 0) {
            pollIntervalMs = fs.getPollIntervalMs();
        } else if (fs.getScanIntervalMinutes() != null && fs.getScanIntervalMinutes() > 0) {
            pollIntervalMs = (long) fs.getScanIntervalMinutes() * 1_000L;
        } else {
            pollIntervalMs = DEFAULT_FILE_POLL_INTERVAL_MS;
        }

        int maxLinesPerPoll = fs.getMaxLinesPerPoll() != null ? fs.getMaxLinesPerPoll() : DEFAULT_MAX_LINES_PER_POLL;
        int maxRecordBytes  = fs.getMaxRecordBytes()  != null ? fs.getMaxRecordBytes()  : DEFAULT_MAX_RECORD_BYTES;

        Map<String, Object> m = new HashMap<>();
        m.put("id",             collectorId);
        m.put("type",           "FILE");
        m.put("name",           fs.getConnectionName() != null ? fs.getConnectionName() : fs.getDataSourceId());
        m.put("enabled",        Boolean.TRUE.equals(fs.getIsActive()));
        m.put("pollIntervalMs", pollIntervalMs);
        m.put("maxLinesPerPoll",maxLinesPerPoll);
        m.put("maxRecordBytes", maxRecordBytes);
        m.put("path",           directory);
        m.put("file",           filePattern);
        m.put("format",         fileFormat);
        m.put("csvHasHeader",   hasHeader);
        m.put("csvDelimiter",   delimiter);
        m.put("charset",        charset);
        return m;
    }

    /**
     * [2026-04-21] JDBC 수집기 Map 빌드.
     * ds_database_config 엔티티 → agent COLLECTORS_SYNC payload.
     */
    private Map<String, Object> buildJdbcCollector(DatabaseConfigEntity db) {
        String collectorId  = "DS_" + db.getDataSourceId();
        String jdbcUrl      = buildJdbcUrl(db);

        // [2026-04-21] poll_interval_ms 설정 우선, 없으면 기본 5분
        long pollIntervalMs = (db.getPollIntervalMs() != null && db.getPollIntervalMs() > 0)
                ? db.getPollIntervalMs() : DEFAULT_JDBC_POLL_INTERVAL_MS;
        int maxLinesPerPoll = db.getMaxLinesPerPoll() != null ? db.getMaxLinesPerPoll() : DEFAULT_MAX_LINES_PER_POLL;
        int maxRecordBytes  = db.getMaxRecordBytes()  != null ? db.getMaxRecordBytes()  : DEFAULT_MAX_RECORD_BYTES;

        String initialValue = db.getIncrementalColumnInitialValue();

        Map<String, Object> m = new HashMap<>();
        m.put("id",              collectorId);
        m.put("type",            "JDBC");
        m.put("name",            db.getConnectionName() != null ? db.getConnectionName() : db.getDataSourceId());
        m.put("enabled",         Boolean.TRUE.equals(db.getIsActive()));
        m.put("pollIntervalMs",  pollIntervalMs);
        m.put("maxLinesPerPoll", maxLinesPerPoll);
        m.put("maxRecordBytes",  maxRecordBytes);
        m.put("url",             jdbcUrl);
        m.put("username",        db.getUsername());
        m.put("password",        db.getPasswordEncrypted());
        m.put("query",           db.getMainQuery());
        m.put("field1",          db.getIncrementalColumn());
        m.put("field1_type",     resolveIncrementalType(db.getIncrementalColumnType()));
        m.put("field1_value",    initialValue != null ? initialValue : "");
        return m;
    }

    // ── 헬퍼 메서드 ─────────────────────────────────────────────────────────

    /**
     * [2026-04-21] DatabaseConfigEntity 기반 JDBC URL 조합.
     * 지원 대상: MARIADB, POSTGRESQL, ORACLE
     */
    private String buildJdbcUrl(DatabaseConfigEntity db) {
        String type   = db.getDatabaseType() != null ? db.getDatabaseType().toUpperCase() : "";
        String host   = db.getHost();
        Integer port  = db.getPort();
        String dbName = db.getDatabaseName();
        String schema = db.getSchemaName();

        switch (type) {
            case "MARIADB":
                return String.format("jdbc:mariadb://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8", host, port, dbName);
            case "POSTGRESQL":
            case "POSTGRES":
                if (schema != null && !schema.isBlank()) {
                    return String.format("jdbc:postgresql://%s:%d/%s?currentSchema=%s", host, port, dbName, schema);
                }
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
            case "ORACLE":
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, dbName);
            default:
                log.warn("[Snapshot] 알 수 없는 databaseType: {} (지원: MARIADB, POSTGRESQL, ORACLE)", type);
                return "";
        }
    }

    /**
     * [2026-04-21] incrementalColumnType → agent field1_type 변환.
     * NUMBER → NUMBER, DATETIME → TIMESTAMP, 그 외 → STRING
     */
    private String resolveIncrementalType(String type) {
        if (type == null) return "STRING";
        switch (type.toUpperCase()) {
            case "NUMBER":   return "NUMBER";
            case "DATETIME": return "TIMESTAMP";
            default:         return "STRING";
        }
    }

    /**
     * [2026-04-21] 파일 패턴 확장자로 에이전트 fileFormat 결정.
     */
    private String resolveFileFormat(String filePattern) {
        if (filePattern == null) return "LOG";
        String lower = filePattern.toLowerCase();
        if (lower.endsWith(".csv"))  return "CSV";
        if (lower.endsWith(".json")) return "JSON";
        return "LOG";
    }
}
