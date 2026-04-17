package com.itmasters.icon.engine.adapter.in.web;

import com.itmasters.icon.engine.service.MainEngineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/engine/steps")
@RequiredArgsConstructor
@Tag(name = "Engine Steps", description = "Step3/Step4/Step5/Step6 트리거 API")
public class StepTriggerController {

    private final MainEngineService mainEngineService;

    @PostMapping("/3")
    @Operation(summary = "Step3 실행 (Step5-1: 룰 평가)", description = "execDsMpId 기준 룰 평가 및 집계 감지")
    @ApiResponse(responseCode = "200", description = "룰 평가 완료",
            content = @Content(schema = @Schema(implementation = StepResponse.class)))
    public ResponseEntity<StepResponse> runStep3(@RequestBody Step3Request req) {
        int processed = mainEngineService.executeStep3Only(req.getExecDsMpId());
        return ResponseEntity.ok(StepResponse.ok(3, processed));
    }





    @Data
    public static class Step3Request { private Long execDsMpId; }
    @Data
    public static class Step4Request { private Long execDsMpId; }
    @Data
    public static class Step5Request { private Long execDsMpId; }
    @Data
    public static class Step6Request { private Long execDsMpId; }

    @Data
    @AllArgsConstructor(staticName = "of")
    public static class StepResponse {
        private int step;
        private String status;
        private Integer saved;

        public static StepResponse ok(int step, Integer saved) {
            return StepResponse.of(step, "OK", saved);
        }
    }
}

