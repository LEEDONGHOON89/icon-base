package com.itmasters.icon.api.analytics.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionDetailDto {
    private ExecutionSummaryDto summary;
    private List<String> groupKeys;
    private List<DetectedRuleDto> rules;
    private List<DetectRuleDto> aggregates;
    private List<DetectScenarioDto> scenarios;
    private Map<String, Object> rawPayload;
    private Map<String, Object> mappedRow;
}
