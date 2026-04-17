package com.itmasters.icon.api.datasource.application.service;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DatabaseConfigEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemConfigEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.DataSourceJpaRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.DatabaseConfigJpaRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.FileSystemConfigJpaRepository;
import com.itmasters.icon.api.datasource.dto.DataSourceConfigDto;
import com.itmasters.icon.common.domain.type.DataSourceType;
// [2026-03-13] agent_collector_file_configs 제거 — ds_file_system_config 직접 참조
import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentCollectorConfigEntity;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentCollectorConfigJpaRepository;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentTargetConfigJpaRepository;
import com.itmasters.icon.rpcserver.agent.AgentRpcWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceConfigService {

    private final DataSourceJpaRepository dataSourceJpaRepository;
    private final FileSystemConfigJpaRepository fileSystemConfigJpaRepository;
    private final DatabaseConfigJpaRepository databaseConfigJpaRepository;
    // [2026-03-13] file/jdbc 전용 레포지토리 제거 — ds_file_system_config 직접 사용
    private final AgentCollectorConfigJpaRepository agentCollectorConfigJpaRepository;
    private final AgentTargetConfigJpaRepository agentTargetConfigJpaRepository;
    // [2026-03-13] wsHandler 직접 주입 — 중간 DB 쿼리 없이 바로 COLLECTORS_SYNC 푸시
    private final AgentRpcWebSocketHandler wsHandler;

    @Transactional(readOnly = true)
    public DataSourceConfigDto getConfig(String dataSourceId) {
        DataSourceEntity ds = dataSourceJpaRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("데이터 소스를 찾을 수 없습니다: " + dataSourceId));

        DataSourceType type = ds.getSourceType();
        if (type == DataSourceType.FILE_SYSTEM || type == DataSourceType.FILE_SYSTEM_REALTIME) {
            Optional<FileSystemConfigEntity> cfg = fileSystemConfigJpaRepository.findByDataSourceId(dataSourceId);
            return DataSourceConfigDto.builder()
                    .type(type.name())
                    .fileSystem(cfg.map(this::toDto).orElse(null))
                    .build();
        } else if (type == DataSourceType.DATABASE) {
            Optional<DatabaseConfigEntity> cfg = databaseConfigJpaRepository.findByDataSourceId(dataSourceId);
            return DataSourceConfigDto.builder()
                    .type("DATABASE")
                    .database(cfg.map(this::toDto).orElse(null))
                    .build();
        }
        return DataSourceConfigDto.builder().type(type.name()).build();
    }

    @Transactional
    public DataSourceConfigDto saveConfig(String dataSourceId, DataSourceConfigDto request) {
        DataSourceEntity ds = dataSourceJpaRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("데이터 소스를 찾을 수 없습니다: " + dataSourceId));

        DataSourceType type = ds.getSourceType();
        if (type == DataSourceType.FILE_SYSTEM || type == DataSourceType.FILE_SYSTEM_REALTIME) {
            DataSourceConfigDto.FileSystem in = request.getFileSystem();
            if (in == null) throw new IllegalArgumentException("fileSystem 설정이 필요합니다");

            boolean isRealtime = (type == DataSourceType.FILE_SYSTEM_REALTIME);
            String defaultPattern = isRealtime ? "*" : "*.csv";
            Boolean defaultHeader = !isRealtime;
            String idPrefix = isRealtime ? "FSR_" : "FS_";

            FileSystemConfigEntity entity = fileSystemConfigJpaRepository.findByDataSourceId(dataSourceId)
                    .orElseGet(() -> isRealtime
                            ? FileSystemConfigEntity.ofDefaultRealtime(idPrefix + dataSourceId, dataSourceId, in.getConnectionName(), in.getWatchDirectory())
                            : FileSystemConfigEntity.ofDefault(idPrefix + dataSourceId, dataSourceId, in.getConnectionName(), in.getWatchDirectory()));

            entity.applyBasic(
                    in.getConnectionName(),
                    in.getWatchDirectory(),
                    defaultIfBlank(in.getFilePattern(), defaultPattern),
                    defaultIfBlank(in.getFileEncoding(), "UTF-8"),
                    defaultIfBlank(in.getDelimiter(), ","),
                    in.getQuoteChar(),
                    in.getEscapeChar(),
                    in.getHasHeader() != null ? in.getHasHeader() : defaultHeader,
                    in.getSkipLines()
            );
            entity.applyAdvanced(
                    defaultIfBlank(in.getProcessingStrategy(), isRealtime ? "REALTIME" : "INCREMENTAL"),
                    in.getScanIntervalMinutes() != null ? in.getScanIntervalMinutes() : (isRealtime ? 5 : 60),
                    isRealtime ? Boolean.FALSE : in.getMoveProcessedFiles(),
                    isRealtime ? null : in.getProcessedFilesDirectory()
            );

            // [2026-03-12] FILE_SYSTEM_REALTIME인 경우 에이전트 연결 정보 저장
            if (isRealtime) {
                entity.applyAgentLink(
                        in.getAgentId() != null ? in.getAgentId().trim() : null
                );
            }

            fileSystemConfigJpaRepository.save(entity);

            // [2026-03-13] FILE_SYSTEM_REALTIME이고 에이전트가 연결된 경우 수집기 자동 생성/업데이트 및 동기화
            if (isRealtime
                    && entity.getAgentId() != null && !entity.getAgentId().isBlank()) {
                syncCollectorToAgent(dataSourceId, entity);
            }

            return getConfig(dataSourceId);
        } else if (type == DataSourceType.DATABASE) {
            DataSourceConfigDto.Database in = request.getDatabase();
            if (in == null) throw new IllegalArgumentException("database 설정이 필요합니다");
            DatabaseConfigEntity entity = databaseConfigJpaRepository.findByDataSourceId(dataSourceId)
                    .orElseGet(() -> DatabaseConfigEntity.create("DB_" + dataSourceId, dataSourceId));
            entity.applyBasic(
                    in.getConnectionName(),
                    in.getDatabaseType(),
                    in.getHost(),
                    in.getPort(),
                    in.getDatabaseName(),
                    in.getSchemaName(),
                    in.getUsername(),
                    in.getPassword(),
                    in.getMinPoolSize(),
                    in.getMaxPoolSize(),
                    in.getConnectionTimeoutSeconds(),
                    in.getIdleTimeoutSeconds()
            );
            entity.applyIngestion(
                    in.getMainQuery(),
                    in.getIncrementalColumn(),
                    in.getIncrementalColumnType(),
                    in.getBatchSize()
            );
            // [2026-03-13] DATABASE 에이전트 연결 정보 저장
            entity.applyAgentLink(
                    in.getAgentId() != null ? in.getAgentId().trim() : null
            );
            databaseConfigJpaRepository.save(entity);

            // [2026-03-13] 에이전트 연결 시 JDBC 수집기 자동 생성/업데이트 및 COLLECTORS_SYNC 푸시
            if (entity.getAgentId() != null && !entity.getAgentId().isBlank()) {
                syncJdbcCollectorToAgent(dataSourceId, entity);
            }

            return getConfig(dataSourceId);
        }

        throw new IllegalStateException("지원하지 않는 데이터 소스 타입: " + type);
    }

    private DataSourceConfigDto.FileSystem toDto(FileSystemConfigEntity e) {
        return DataSourceConfigDto.FileSystem.builder()
                .dsFileSystemConfigId(e.getDsFileSystemConfigId())
                .dataSourceId(e.getDataSourceId())
                .connectionName(e.getConnectionName())
                .watchDirectory(e.getWatchDirectory())
                .filePattern(e.getFilePattern())
                .fileEncoding(e.getFileEncoding())
                .delimiter(e.getDelimiter())
                .quoteChar(e.getQuoteChar())
                .escapeChar(e.getEscapeChar())
                .hasHeader(e.getHasHeader())
                .skipLines(e.getSkipLines())
                .processingStrategy(e.getProcessingStrategy())
                .scanIntervalMinutes(e.getScanIntervalMinutes())
                .moveProcessedFiles(e.getMoveProcessedFiles())
                .processedFilesDirectory(e.getProcessedFilesDirectory())
                .isActive(e.getIsActive())
                .connectionStatus(e.getConnectionStatus())
                .lastErrorMessage(e.getLastErrorMessage())
                .agentId(e.getAgentId())
                .build();
    }

    /**
     * [2026-03-13] FILE_SYSTEM_REALTIME 데이터소스 저장 시
     * 연결된 에이전트에 FILE 수집기를 자동으로 생성 또는 업데이트하고
     * COLLECTORS_SYNC 메시지를 에이전트에 직접 푸시한다.
     * <p>
     * 중간 DB 쿼리 없이 ds_file_system_config 값으로 payload를 직접 구성하여
     * targetConfigId 불일치로 인한 빈 payload 문제를 방지한다.
     */
    private void syncCollectorToAgent(String dataSourceId, FileSystemConfigEntity fs) {
        String agentId = fs.getAgentId();

        // 에이전트의 첫 번째 targetConfig 자동 선택
        var targetConfigOpt = agentTargetConfigJpaRepository.findByAgentId(agentId).stream().findFirst();
        if (targetConfigOpt.isEmpty()) {
            log.warn("[DataSourceConfig] 에이전트에 타겟 설정이 없음 - agentId={}, dataSourceId={}", agentId, dataSourceId);
            return;
        }
        var targetConfig = targetConfigOpt.get();
        String targetConfigId = targetConfig.getTargetConfigId();
        String targetId       = targetConfig.getTargetId(); // config.yaml의 target id

        String collectorConfigId = "DS_" + dataSourceId;
        String collectorName     = fs.getConnectionName() != null ? fs.getConnectionName() : dataSourceId;

        // scanIntervalMinutes → pollIntervalMs 변환
        long pollIntervalMs = fs.getScanIntervalMinutes() != null && fs.getScanIntervalMinutes() > 0
                ? (long) fs.getScanIntervalMinutes() * 60 * 1000L
                : 1000L;

        // agent_collector_configs 생성 또는 업데이트 (targetConfigId도 최신으로 갱신)
        Optional<AgentCollectorConfigEntity> existing =
                agentCollectorConfigJpaRepository.findById(collectorConfigId);
        AgentCollectorConfigEntity collector;
        if (existing.isEmpty()) {
            collector = AgentCollectorConfigEntity.create(
                    collectorConfigId, targetConfigId, "FILE",
                    collectorName, true, pollIntervalMs, 1000, 524288);
            agentCollectorConfigJpaRepository.save(collector);
            log.info("[DataSourceConfig] 에이전트 수집기 생성 - dataSourceId={}, agentId={}, collectorId={}", dataSourceId, agentId, collectorConfigId);
        } else {
            collector = existing.get();
            // [2026-03-13] targetConfigId 불일치 방지: 변경되었으면 갱신
            if (!targetConfigId.equals(collector.getTargetConfigId())) {
                log.info("[DataSourceConfig] 수집기 targetConfigId 갱신: {} → {}", collector.getTargetConfigId(), targetConfigId);
                collector.setTargetConfigId(targetConfigId);
            }
            collector.update(collectorName, collector.isEnabled(), pollIntervalMs,
                    collector.getMaxLinesPerPoll(), collector.getMaxRecordBytes());
            agentCollectorConfigJpaRepository.save(collector);
            log.info("[DataSourceConfig] 에이전트 수집기 업데이트 - dataSourceId={}, agentId={}, collectorId={}", dataSourceId, agentId, collectorConfigId);
        }

        // [2026-03-13] ds_file_system_config 값으로 sync payload 직접 구성
        //              (DB 재쿼리 없이 매개변수로 전달받은 FileSystemConfigEntity를 그대로 사용)
        String filePattern = fs.getFilePattern()    != null ? fs.getFilePattern()    : "*";
        String fileFormat  = resolveFileFormat(filePattern);
        boolean hasHeader  = Boolean.TRUE.equals(fs.getHasHeader());
        String delimiter   = fs.getDelimiter()      != null ? fs.getDelimiter()      : ",";
        String charset     = fs.getFileEncoding()   != null ? fs.getFileEncoding()   : "UTF-8";
        String directory   = fs.getWatchDirectory() != null ? fs.getWatchDirectory() : "";

        // COLLECTORS_SYNC payload 구성
        Map<String, Object> collectorMap = new HashMap<>();
        collectorMap.put("id",             collectorConfigId);
        collectorMap.put("type",           "FILE");
        collectorMap.put("name",           collectorName);
        collectorMap.put("enabled",        true);
        collectorMap.put("pollIntervalMs", pollIntervalMs);
        collectorMap.put("maxLinesPerPoll",1000);
        collectorMap.put("maxRecordBytes", 524288);
        // 파일 수집 세부 설정 (FileCollectorConfig의 @JsonProperty 필드명과 일치)
        collectorMap.put("path",           directory);
        collectorMap.put("file",           filePattern);
        collectorMap.put("format",         fileFormat);
        collectorMap.put("csvHasHeader",   hasHeader);
        collectorMap.put("csvDelimiter",   delimiter);
        collectorMap.put("charset",        charset);

        List<Map<String, Object>> payload = new ArrayList<>();
        payload.add(collectorMap);

        // COLLECTORS_SYNC 직접 푸시
        boolean pushed = wsHandler.pushCollectorsSync(agentId, targetId, payload);
        if (pushed) {
            log.info("[DataSourceConfig] COLLECTORS_SYNC 전송 완료 - agentId={}, targetId={}, collectorId={}",
                    agentId, targetId, collectorConfigId);
        } else {
            log.warn("[DataSourceConfig] COLLECTORS_SYNC 전송 실패 (에이전트 미연결) - agentId={}", agentId);
        }
    }

    private DataSourceConfigDto.Database toDto(DatabaseConfigEntity e) {
        return DataSourceConfigDto.Database.builder()
                .dsDatabaseConfigId(e.getDsDatabaseConfigId())
                .dataSourceId(e.getDataSourceId())
                .connectionName(e.getConnectionName())
                .databaseType(e.getDatabaseType())
                .host(e.getHost())
                .port(e.getPort())
                .databaseName(e.getDatabaseName())
                .schemaName(e.getSchemaName())
                .username(e.getUsername())
                .minPoolSize(e.getMinPoolSize())
                .maxPoolSize(e.getMaxPoolSize())
                .connectionTimeoutSeconds(e.getConnectionTimeoutSeconds())
                .idleTimeoutSeconds(e.getIdleTimeoutSeconds())
                .mainQuery(e.getMainQuery())
                .incrementalColumn(e.getIncrementalColumn())
                .incrementalColumnType(e.getIncrementalColumnType())
                .batchSize(e.getBatchSize())
                .isActive(e.getIsActive())
                .connectionStatus(e.getConnectionStatus())
                .lastErrorMessage(e.getLastErrorMessage())
                // [2026-03-13] 에이전트 연결 정보
                .agentId(e.getAgentId())
                .build();
    }

    /**
     * [2026-03-13] DATABASE 데이터소스 저장 시
     * 연결된 에이전트에 JDBC 수집기를 자동으로 생성 또는 업데이트하고
     * COLLECTORS_SYNC 메시지를 에이전트에 직접 푸시한다.
     *
     * icon-agent JdbcCollector가 처리할 수 있는 필드를 payload에 구성한다.
     * 새 DB 타입 추가 시 buildJdbcUrl() 부분만 확장하면 된다.
     */
    private void syncJdbcCollectorToAgent(String dataSourceId, DatabaseConfigEntity db) {
        String agentId = db.getAgentId();

        var targetConfigOpt = agentTargetConfigJpaRepository.findByAgentId(agentId).stream().findFirst();
        if (targetConfigOpt.isEmpty()) {
            log.warn("[DataSourceConfig] 에이전트에 타겟 설정이 없음 - agentId={}, dataSourceId={}", agentId, dataSourceId);
            return;
        }
        var targetConfig = targetConfigOpt.get();
        String targetConfigId = targetConfig.getTargetConfigId();
        String targetId       = targetConfig.getTargetId();

        String collectorConfigId = "DS_" + dataSourceId;
        String collectorName     = db.getConnectionName() != null ? db.getConnectionName() : dataSourceId;

        // 폴링 간격: batchSize 기반 — 기본 5분
        long pollIntervalMs = 5 * 60 * 1000L;

        // agent_collector_configs 생성 또는 업데이트
        Optional<AgentCollectorConfigEntity> existing =
                agentCollectorConfigJpaRepository.findById(collectorConfigId);
        AgentCollectorConfigEntity collector;
        if (existing.isEmpty()) {
            collector = AgentCollectorConfigEntity.create(
                    collectorConfigId, targetConfigId, "JDBC",
                    collectorName, true, pollIntervalMs, 1000, 524288);
            agentCollectorConfigJpaRepository.save(collector);
            log.info("[DataSourceConfig] JDBC 수집기 생성 - dataSourceId={}, agentId={}, collectorId={}", dataSourceId, agentId, collectorConfigId);
        } else {
            collector = existing.get();
            if (!targetConfigId.equals(collector.getTargetConfigId())) {
                collector.setTargetConfigId(targetConfigId);
            }
            collector.update(collectorName, collector.isEnabled(), pollIntervalMs,
                    collector.getMaxLinesPerPoll(), collector.getMaxRecordBytes());
            agentCollectorConfigJpaRepository.save(collector);
            log.info("[DataSourceConfig] JDBC 수집기 업데이트 - dataSourceId={}, agentId={}, collectorId={}", dataSourceId, agentId, collectorConfigId);
        }

        // JDBC URL 조합
        String jdbcUrl = buildJdbcUrl(db);

        // COLLECTORS_SYNC payload 구성 (JdbcCollectorConfig @JsonProperty 필드명과 일치)
        Map<String, Object> collectorMap = new HashMap<>();
        collectorMap.put("id",              collectorConfigId);
        collectorMap.put("type",            "JDBC");
        collectorMap.put("name",            collectorName);
        collectorMap.put("enabled",         true);
        collectorMap.put("pollIntervalMs",  pollIntervalMs);
        collectorMap.put("maxLinesPerPoll", 1000);
        collectorMap.put("maxRecordBytes",  524288);
        collectorMap.put("url",             jdbcUrl);
        collectorMap.put("username",        db.getUsername());
        collectorMap.put("password",        db.getPasswordEncrypted());
        collectorMap.put("query",           db.getMainQuery());
        collectorMap.put("field1",          db.getIncrementalColumn());
        collectorMap.put("field1_type",     resolveIncrementalType(db.getIncrementalColumnType()));
        collectorMap.put("field1_value",    ""); // 초기값 — 에이전트가 watermark.dat에서 관리

        List<Map<String, Object>> payload = new ArrayList<>();
        payload.add(collectorMap);

        boolean pushed = wsHandler.pushCollectorsSync(agentId, targetId, payload);
        if (pushed) {
            log.info("[DataSourceConfig] JDBC COLLECTORS_SYNC 전송 완료 - agentId={}, targetId={}, collectorId={}",
                    agentId, targetId, collectorConfigId);
        } else {
            log.warn("[DataSourceConfig] JDBC COLLECTORS_SYNC 전송 실패 (에이전트 미연결) - agentId={}", agentId);
        }
    }

    /**
     * [2026-03-13] DatabaseConfigEntity 기반 JDBC URL 조합.
     * 지원 대상: MARIADB, POSTGRESQL, ORACLE
     * 새 DB 타입 추가 시 이 메서드의 switch에만 case를 추가하면 된다.
     */
    private String buildJdbcUrl(DatabaseConfigEntity db) {
        String type = db.getDatabaseType() != null ? db.getDatabaseType().toUpperCase() : "";
        String host = db.getHost();
        Integer port = db.getPort();
        String dbName = db.getDatabaseName();
        String schema = db.getSchemaName();

        switch (type) {
            case "MARIADB":
                // MariaDB 기본 포트: 3306
                return String.format("jdbc:mariadb://%s:%d/%s?useUnicode=true&characterEncoding=UTF-8", host, port, dbName);
            case "POSTGRESQL":
            case "POSTGRES":
                // PostgreSQL 기본 포트: 5432 / schema 설정 시 currentSchema 파라미터 사용
                if (schema != null && !schema.isBlank()) {
                    return String.format("jdbc:postgresql://%s:%d/%s?currentSchema=%s", host, port, dbName, schema);
                }
                return String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
            case "ORACLE":
                // Oracle 기본 포트: 1521 / SID 방식 사용
                return String.format("jdbc:oracle:thin:@%s:%d:%s", host, port, dbName);
            default:
                log.warn("[DataSourceConfig] 알 수 없는 databaseType: {} (지원: MARIADB, POSTGRESQL, ORACLE)", type);
                return "";
        }
    }

    /**
     * [2026-03-13] incrementalColumnType(NUMBER|DATETIME) → agent field1_type(NUMBER|TIMESTAMP) 변환.
     */
    private String resolveIncrementalType(String incrementalColumnType) {
        if (incrementalColumnType == null) return "STRING";
        switch (incrementalColumnType.toUpperCase()) {
            case "NUMBER": return "NUMBER";
            case "DATETIME": return "TIMESTAMP";
            default: return "STRING";
        }
    }

    /**
     * [2026-03-12] 파일 패턴 확장자로 에이전트 수집기 fileFormat 결정.
     */
    private String resolveFileFormat(String filePattern) {
        if (filePattern == null) return "LOG";
        String lower = filePattern.toLowerCase();
        if (lower.endsWith(".csv"))  return "CSV";
        if (lower.endsWith(".json")) return "JSON";
        return "LOG";
    }

    private String defaultIfBlank(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
