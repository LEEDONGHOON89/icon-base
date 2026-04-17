package com.itmasters.icon.engine.adapter.in.web;

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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이벤트 수신
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Single Ingest", description = "단일 이벤트 수신 → Step2/3/4/5 실행")
public class SingleIngestController {

    private final SingleIngestService ingestService;

    @PostMapping("/api/v1/engine/ingest/single-run")
    @Operation(summary = "단일 이벤트 수신 및 전체 실행", description = "단일 이벤트를 저장한 뒤 Step2→Step3→Step4→Step5를 순차 실행")
    @ApiResponse(responseCode = "200", description = "실행 완료",
            content = @Content(schema = @Schema(implementation = SingleRunResponse.class)))
    public ResponseEntity<SingleRunResponse> ingestAndRun(
            @RequestBody SingleRunRequest req) {
        var resp = ingestService.ingestAndRun(req);
        return ResponseEntity.ok(resp);
    }
}
