package com.itmasters.icon.api.detection.adapter.in.web;

import com.itmasters.icon.engine.service.SingleIngestService;
import com.itmasters.icon.engine.service.dto.SingleRunRequest;
import com.itmasters.icon.engine.service.dto.SingleRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 실시간 탐지 API
 * 프론트엔드에서 JSON 이벤트를 전송하여 실시간 탐지를 수행
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Realtime Detection", description = "실시간 탐지 API - JSON 이벤트 전송 및 즉시 탐지")
public class RealtimeDetectionController {

    private final SingleIngestService ingestService;

    @PostMapping("/api/v1/detections/realtime/run")
    @Operation(
        summary = "실시간 탐지 실행",
        description = """
            JSON 이벤트 데이터를 전송하여 즉시 탐지를 수행합니다.

            **실행 순서:**
            1. 원본 데이터 저장 (landing_records)
            2. 프로파일 매핑 (mapped_storages)
            3. 이벤트 스트림 저장 (event_stream)
            4. Entity Attributes 저장 (entity_attributes)
            5. 룰/집계/시나리오 탐지 (detect_rules, detect_aggregates, detect_scenarios)

            **시연 시나리오:**
            - CSV 샘플을 JSON으로 변환하여 하나씩 전송
            - 실시간으로 탐지 결과가 누적됨
            - 프론트엔드 대시보드에서 실시간 업데이트 확인 가능

            **요청 예시:**
            ```json
            {
              "dataSourceId": "0ME2YHMK6FGZN",
              "executedBy": "DEMO_USER",
              "row": {
                "CUS_ID": "DEMO_CUS001",
                "TRX_DT": "2025-08-20 02:00:00",
                "TRX_TYPE": "비대면계좌개설",
                "CUSTOMER_AGE": 68,
                "TRX_AMT": 0
              }
            }
            ```
            """
    )
    @ApiResponse(
        responseCode = "200",
        description = "탐지 실행 완료",
        content = @Content(schema = @Schema(implementation = SingleRunResponse.class))
    )
    public SingleRunResponse executeRealtimeDetection(@RequestBody SingleRunRequest request) {
        try {
            log.info("🎯 실시간 탐지 요청 - dataSourceId: {}, executedBy: {}",
                request.getDataSourceId(), request.getExecutedBy());

            SingleRunResponse response = ingestService.ingestAndRun(request);

            log.info("✅ 실시간 탐지 완료 - execDsMpId: {}, savedSensors: {}, savedRules: {}, savedScenarios: {}",
                response.getExecDsMpId(),
                response.getSavedSensors(),
                response.getSavedRules(),
                response.getSavedScenarios());

            return response;
            
        } catch (Exception e) {
            log.error("❌ 실시간 탐지 실행 중 오류 발생 - dataSourceId: {}, executedBy: {}", 
                request.getDataSourceId(), request.getExecutedBy(), e);
            
            // 에러 응답 반환 (SingleRunResponse를 사용하되 에러 정보 포함)
            return SingleRunResponse.error(
                e.getClass().getSimpleName() + ": " + e.getMessage()
            );
        }
    }
}
