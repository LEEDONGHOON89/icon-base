package com.itmasters.icon.api.engine;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Engine Flow API", description = "탐지 엔진 플로우 시각화 API")
public class EngineFlowController {
    
    private final EngineFlowService engineFlowService;
    
    @GetMapping("/api/v1/engine/flow")
    @Operation(summary = "엔진 플로우 데이터 조회", description = "Rule → Aggregate → Scenario 연결 관계 조회")
    public EngineFlowDto.Response getEngineFlow(
            @RequestParam(required = false) String scenarioId
    ) {
        if (scenarioId != null) {
            return engineFlowService.getFlowByScenario(scenarioId);
        }
        return engineFlowService.getAllFlow();
    }
}
