package com.itmasters.icon.api.datasource.adapter.in.web;

import com.itmasters.icon.api.datasource.application.service.CollectionResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * [2026-04-22] 수집기 초기화 컨트롤러.
 *
 * 데이터소스의 수집 위치(last_position / watermark)를 초기화하여
 * 파일/DB를 처음부터 재수집하도록 지원한다.
 *
 * 타입별 동작:
 *   DATABASE             (직접) → last_processed_value, last_query_time = NULL
 *   DATABASE             (에이전트) → COLLECTOR_RESET WebSocket 메시지 전송
 *   FILE_SYSTEM_REALTIME (직접) → data/file-realtime/{id}/positions.json 삭제
 *   FILE_SYSTEM_REALTIME (에이전트) → COLLECTOR_RESET WebSocket 메시지 전송
 *   FILE_SYSTEM          (직접) → ds_file_system_log 레코드 삭제
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/data-sources")
@RequiredArgsConstructor
@Tag(name = "수집기 초기화 API", description = "수집기의 last_position을 초기화하여 처음부터 재수집하는 API")
public class CollectionResetController {

    private final CollectionResetService collectionResetService;

    /**
     * [2026-04-22] 수집기 초기화 — 수집 위치를 초기화하여 처음부터 재수집.
     *
     * @param dataSourceId 초기화할 데이터소스 ID
     * @return 초기화 결과 (mode: DIRECT|AGENT, message, agentConnected 등)
     */
    @Operation(
        summary = "수집기 초기화",
        description = "수집기의 last_position(파일 offset / DB watermark)을 초기화하여 처음부터 재수집합니다."
    )
    @PostMapping("/{dataSourceId}/reset-collection")
    public ResponseEntity<Map<String, Object>> resetCollection(
            @PathVariable String dataSourceId) {
        log.info("[CollectionReset] 초기화 요청 - dataSourceId={}", dataSourceId);
        Map<String, Object> result = collectionResetService.reset(dataSourceId);
        return ResponseEntity.ok(result);
    }
}
