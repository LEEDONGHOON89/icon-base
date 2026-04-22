package com.itmasters.icon.api.datasource.application.service;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DatabaseConfigEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemConfigEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.DataSourceJpaRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.DatabaseConfigJpaRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.FileSystemConfigJpaRepository;
import com.itmasters.icon.api.datasource.dto.DataSourceConfigDto;
import com.itmasters.icon.common.domain.type.DataSourceType;
// [2026-04-21] AgentSnapshotService 로 수집기 동기화 일원화 (agent_collector_* 테이블 미사용)
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceConfigService {

    private final DataSourceJpaRepository dataSourceJpaRepository;
    private final FileSystemConfigJpaRepository fileSystemConfigJpaRepository;
    private final DatabaseConfigJpaRepository databaseConfigJpaRepository;
    // [2026-04-21] 스냅샷 서비스 — 에이전트 전체 수집기 동기화 담당
    private final AgentSnapshotService agentSnapshotService;

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
                // [2026-04-22] 버그 수정: agentId 변경 전 기존 값을 캡처
                //   - agentId 제거(null) 시 기존 에이전트에서 수집기를 제거해야 함
                //   - 저장 후 entity.getAgentId()는 이미 null이므로 기존 값을 미리 보관
                String prevAgentId = entity.getAgentId();

                entity.applyAgentLink(
                        in.getAgentId() != null ? in.getAgentId().trim() : null
                );

                // [2026-04-21] 에이전트 폴링 설정 저장
                entity.applyPollSettings(in.getPollIntervalMs(), in.getMaxLinesPerPoll(), in.getMaxRecordBytes());

                fileSystemConfigJpaRepository.save(entity);

                String newAgentId = entity.getAgentId();

                // [2026-04-22] 신규/변경 에이전트에 수집기 추가 스냅샷 전송
                if (newAgentId != null && !newAgentId.isBlank()) {
                    agentSnapshotService.pushSnapshot(newAgentId);
                }
                // [2026-04-22] 에이전트가 변경되거나 제거된 경우 기존 에이전트에서 수집기 제거 스냅샷 전송
                //   예) "agent-001" → null    : 기존 에이전트에서 이 수집기 제거
                //   예) "agent-001" → "agent-002" : 기존 에이전트에서도 제거 + 신규에 추가
                if (prevAgentId != null && !prevAgentId.isBlank()
                        && !prevAgentId.equals(newAgentId)) {
                    agentSnapshotService.pushSnapshot(prevAgentId);
                }
            } else {
                fileSystemConfigJpaRepository.save(entity);
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
            // [2026-04-21] incrementalColumnType: null 입력 시 "DATETIME" 기본값 적용
            String colType = (in.getIncrementalColumnType() != null && !in.getIncrementalColumnType().isBlank())
                    ? in.getIncrementalColumnType() : "DATETIME";
            entity.applyIngestion(
                    in.getMainQuery(),
                    in.getIncrementalColumn(),
                    colType,
                    in.getIncrementalColumnInitialValue(),
                    in.getBatchSize()
            );
            // [2026-03-13] DATABASE 에이전트 연결 정보 저장
            // [2026-04-22] 버그 수정: agentId 변경 전 기존 값을 캡처
            String prevDbAgentId = entity.getAgentId();

            entity.applyAgentLink(
                    in.getAgentId() != null ? in.getAgentId().trim() : null
            );
            // [2026-04-21] 에이전트 폴링 설정 저장
            entity.applyPollSettings(in.getPollIntervalMs(), in.getMaxLinesPerPoll(), in.getMaxRecordBytes());
            databaseConfigJpaRepository.save(entity);

            String newDbAgentId = entity.getAgentId();

            // [2026-04-22] 신규/변경 에이전트에 수집기 추가 스냅샷 전송
            if (newDbAgentId != null && !newDbAgentId.isBlank()) {
                agentSnapshotService.pushSnapshot(newDbAgentId);
            }
            // [2026-04-22] 에이전트가 변경되거나 제거된 경우 기존 에이전트에서 수집기 제거 스냅샷 전송
            if (prevDbAgentId != null && !prevDbAgentId.isBlank()
                    && !prevDbAgentId.equals(newDbAgentId)) {
                agentSnapshotService.pushSnapshot(prevDbAgentId);
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
                // [2026-04-21] 폴링 설정
                .pollIntervalMs(e.getPollIntervalMs())
                .maxLinesPerPoll(e.getMaxLinesPerPoll())
                .maxRecordBytes(e.getMaxRecordBytes())
                .build();
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
                // [2026-04-21] 초기값 포함
                .incrementalColumnInitialValue(e.getIncrementalColumnInitialValue())
                .batchSize(e.getBatchSize())
                .isActive(e.getIsActive())
                .connectionStatus(e.getConnectionStatus())
                .lastErrorMessage(e.getLastErrorMessage())
                // [2026-03-13] 에이전트 연결 정보
                .agentId(e.getAgentId())
                // [2026-04-21] 폴링 설정
                .pollIntervalMs(e.getPollIntervalMs())
                .maxLinesPerPoll(e.getMaxLinesPerPoll())
                .maxRecordBytes(e.getMaxRecordBytes())
                .build();
    }

    private String defaultIfBlank(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
