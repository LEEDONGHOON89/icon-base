package com.itmasters.icon.api.datasource.application.service;

import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DatabaseConfigEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.FileSystemConfigEntity;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.DatabaseConfigJpaRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.DataSourceJpaRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.FileSystemConfigJpaRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.repository.FileSystemLogJpaRepository;
import com.itmasters.icon.common.domain.type.DataSourceType;
import com.itmasters.icon.engine.datasource.realtime.FileSystemRealtimePositionStore;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentTargetConfigJpaRepository;
import com.itmasters.icon.rpcserver.agent.AgentRpcWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [2026-04-22] 수집기 초기화 서비스.
 *
 * 데이터소스 타입과 에이전트 연결 여부에 따라 적절한 초기화를 수행한다.
 *
 * | # | 타입                    | 에이전트 | 초기화 방법                                      |
 * |---|-------------------------|----------|--------------------------------------------------|
 * | 1 | DATABASE                | 없음     | ds_database_config.last_processed_value/time = NULL |
 * | 2 | DATABASE                | 있음     | WebSocket COLLECTOR_RESET → 에이전트 watermark.dat 삭제 |
 * | 3 | FILE_SYSTEM_REALTIME    | 없음     | data/file-realtime/{id}/positions.json 삭제      |
 * | 4 | FILE_SYSTEM_REALTIME    | 있음     | WebSocket COLLECTOR_RESET → 에이전트 positions.dat 삭제 |
 * | 5 | FILE_SYSTEM             | 없음(항상) | ds_file_system_log 레코드 삭제                   |
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectionResetService {

    private final DataSourceJpaRepository dataSourceJpaRepository;
    private final FileSystemConfigJpaRepository fileSystemConfigJpaRepository;
    private final DatabaseConfigJpaRepository databaseConfigJpaRepository;
    private final FileSystemLogJpaRepository fileSystemLogJpaRepository;
    private final FileSystemRealtimePositionStore positionStore;
    private final AgentRpcWebSocketHandler wsHandler;
    private final AgentTargetConfigJpaRepository agentTargetConfigJpaRepository;

    @Transactional
    public Map<String, Object> reset(String dataSourceId) {
        var ds = dataSourceJpaRepository.findById(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("데이터 소스를 찾을 수 없습니다: " + dataSourceId));

        DataSourceType type = ds.getSourceType();
        return switch (type) {
            case DATABASE             -> resetDatabase(dataSourceId);
            case FILE_SYSTEM_REALTIME -> resetFileSystemRealtime(dataSourceId);
            case FILE_SYSTEM          -> resetFileSystem(dataSourceId);
            default -> throw new IllegalStateException("초기화를 지원하지 않는 데이터 소스 타입: " + type);
        };
    }

    // ── Case 1 / 2: DATABASE ────────────────────────────────────────────────

    private Map<String, Object> resetDatabase(String dataSourceId) {
        DatabaseConfigEntity entity = databaseConfigJpaRepository.findByDataSourceId(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("DATABASE 설정을 찾을 수 없습니다: " + dataSourceId));

        String agentId = entity.getAgentId();

        if (agentId != null && !agentId.isBlank()) {
            // Case 2: 에이전트 수집 → COLLECTOR_RESET 전송
            String collectorId = "DS_" + dataSourceId;
            boolean sent = sendCollectorReset(agentId, collectorId);
            log.info("[Reset] DATABASE(에이전트) 초기화 - dataSourceId={}, agentId={}, sent={}",
                    dataSourceId, agentId, sent);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("dataSourceId",   dataSourceId);
            result.put("type",           "DATABASE");
            result.put("mode",           "AGENT");
            result.put("agentId",        agentId);
            result.put("collectorId",    collectorId);
            result.put("agentConnected", sent);
            result.put("message", sent
                    ? "에이전트에 초기화 메시지를 전송했습니다."
                    : "에이전트가 현재 연결되어 있지 않습니다. 에이전트 재기동 후 초기화가 적용됩니다.");
            return result;
        }

        // Case 1: 백엔드 직접 수집 → DB 컬럼 NULL 초기화
        entity.resetWatermark();
        databaseConfigJpaRepository.save(entity);
        log.info("[Reset] DATABASE(직접) 하이워터마크 초기화 - dataSourceId={}", dataSourceId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataSourceId", dataSourceId);
        result.put("type",         "DATABASE");
        result.put("mode",         "DIRECT");
        result.put("message",      "수집 하이워터마크가 초기화되었습니다. 다음 수집 주기에 처음부터 재수집합니다.");
        return result;
    }

    // ── Case 3 / 4: FILE_SYSTEM_REALTIME ────────────────────────────────────

    private Map<String, Object> resetFileSystemRealtime(String dataSourceId) {
        FileSystemConfigEntity entity = fileSystemConfigJpaRepository.findByDataSourceId(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("FILE_SYSTEM_REALTIME 설정을 찾을 수 없습니다: " + dataSourceId));

        String agentId = entity.getAgentId();

        if (agentId != null && !agentId.isBlank()) {
            // Case 4: 에이전트 수집 → COLLECTOR_RESET 전송
            String collectorId = "DS_" + dataSourceId;
            boolean sent = sendCollectorReset(agentId, collectorId);
            log.info("[Reset] FILE_SYSTEM_REALTIME(에이전트) 초기화 - dataSourceId={}, agentId={}, sent={}",
                    dataSourceId, agentId, sent);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("dataSourceId",   dataSourceId);
            result.put("type",           "FILE_SYSTEM_REALTIME");
            result.put("mode",           "AGENT");
            result.put("agentId",        agentId);
            result.put("collectorId",    collectorId);
            result.put("agentConnected", sent);
            result.put("message", sent
                    ? "에이전트에 초기화 메시지를 전송했습니다."
                    : "에이전트가 현재 연결되어 있지 않습니다. 에이전트 재기동 후 초기화가 적용됩니다.");
            return result;
        }

        // Case 3: 백엔드 직접 수집 → positions.json 삭제
        positionStore.delete(dataSourceId);
        log.info("[Reset] FILE_SYSTEM_REALTIME(직접) 위치 파일 삭제 - dataSourceId={}", dataSourceId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataSourceId", dataSourceId);
        result.put("type",         "FILE_SYSTEM_REALTIME");
        result.put("mode",         "DIRECT");
        result.put("message",      "파일 읽기 위치가 초기화되었습니다. 다음 수집 주기에 파일 처음부터 재수집합니다.");
        return result;
    }

    // ── Case 5: FILE_SYSTEM ──────────────────────────────────────────────────

    private Map<String, Object> resetFileSystem(String dataSourceId) {
        FileSystemConfigEntity entity = fileSystemConfigJpaRepository.findByDataSourceId(dataSourceId)
                .orElseThrow(() -> new IllegalArgumentException("FILE_SYSTEM 설정을 찾을 수 없습니다: " + dataSourceId));

        String configId = entity.getDsFileSystemConfigId();
        int deleted = fileSystemLogJpaRepository.deleteByDsFileSystemConfigId(configId);
        log.info("[Reset] FILE_SYSTEM 처리 로그 삭제 - dataSourceId={}, configId={}, deleted={}건",
                dataSourceId, configId, deleted);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dataSourceId", dataSourceId);
        result.put("type",         "FILE_SYSTEM");
        result.put("mode",         "DIRECT");
        result.put("deletedLogs",  deleted);
        result.put("message",      String.format("처리 로그 %d건이 삭제되었습니다. 다음 수집 주기에 파일을 재처리합니다.", deleted));
        return result;
    }

    // ── 공통 헬퍼 ────────────────────────────────────────────────────────────

    /**
     * agentId로 targetId를 조회하여 COLLECTOR_RESET 메시지를 WebSocket으로 전송.
     * agent_target_configs 항목이 없으면 agentId를 targetId 대용으로 사용.
     */
    private boolean sendCollectorReset(String agentId, String collectorId) {
        String targetId = agentTargetConfigJpaRepository.findByAgentId(agentId)
                .stream().findFirst()
                .map(t -> t.getTargetId())
                .orElse(agentId);
        return wsHandler.sendCollectorReset(agentId, targetId, collectorId);
    }
}
