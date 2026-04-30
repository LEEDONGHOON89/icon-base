package com.itmasters.icon.api.datasource.adapter.in.web;

import com.itmasters.icon.api.datasource.application.service.AgentSnapshotService;
import com.itmasters.icon.rpcserver.agent.AgentRpcWebSocketHandler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * [2026-04-21] 에이전트 스냅샷 동기화 컨트롤러.
 *
 * 에이전트에 등록된 전체 수집기 목록(ds_file_system_config + ds_database_config)을
 * COLLECTORS_SYNC 메시지로 즉시 푸시하거나, 현재 스냅샷 목록을 조회한다.
 *
 * Phase 3 UI: 에이전트 관리 화면 → "수집기 동기화" 버튼 → POST /{agentId}/sync
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
@Tag(name = "Agent Sync", description = "에이전트 수집기 스냅샷 동기화 API")
public class AgentSyncController {

    private final AgentSnapshotService agentSnapshotService;
    private final AgentRpcWebSocketHandler wsHandler;

    /**
     * [2026-04-21] 에이전트 수집기 스냅샷 즉시 푸시.
     * ds_file_system_config + ds_database_config WHERE agent_id = agentId 를 조회하여
     * COLLECTORS_SYNC 메시지로 에이전트에 전송한다.
     *
     * @param agentId 스냅샷을 푸시할 에이전트 ID
     * @return 푸시 결과 (connected: 에이전트 연결 여부, snapshotSize: 수집기 개수)
     */
    @Operation(summary = "에이전트 수집기 스냅샷 동기화", description = "에이전트의 전체 수집기 목록을 즉시 동기화합니다.")
    @PostMapping("/{agentId}/sync")
    public ResponseEntity<Map<String, Object>> syncAgent(@PathVariable String agentId) {
        log.info("[AgentSync] 수동 스냅샷 동기화 요청 - agentId={}", agentId);
        List<Map<String, Object>> snapshot = agentSnapshotService.buildSnapshot(agentId);
        agentSnapshotService.pushSnapshot(agentId);
        boolean connected = wsHandler.isAgentConnected(agentId);
        return ResponseEntity.ok(Map.of(
                "agentId",      agentId,
                "connected",    connected,
                "snapshotSize", snapshot.size(),
                "status",       connected ? "pushed" : "queued"
        ));
    }

    /**
     * [2026-04-21] 에이전트 수집기 스냅샷 조회.
     * 실제 전송 없이, ds_* 테이블에서 빌드된 수집기 목록을 반환한다.
     *
     * @param agentId 조회할 에이전트 ID
     * @return 수집기 목록 (COLLECTORS_SYNC payload 형태)
     */
    @Operation(summary = "에이전트 수집기 스냅샷 조회", description = "에이전트에 등록된 전체 수집기 목록을 조회합니다.")
    @GetMapping("/{agentId}/snapshot")
    public ResponseEntity<List<Map<String, Object>>> getSnapshot(@PathVariable String agentId) {
        List<Map<String, Object>> snapshot = agentSnapshotService.buildSnapshot(agentId);
        return ResponseEntity.ok(snapshot);
    }
}
