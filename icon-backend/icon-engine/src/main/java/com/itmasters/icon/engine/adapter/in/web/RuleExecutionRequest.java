package com.itmasters.icon.engine.adapter.in.web;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 룰 실행 요청 DTO
 * 
 * Event Stream 기반 룰 실행을 위한 파라미터:
 * - ruleConditionJson: JSON 형태의 룰 조건 (fieldName, operator, value)
 * - groupKey: 검사 대상 그룹 키 (예: CUS001, USER123)
 * - timeWindowMinutes: 검사할 시간 윈도우 (분 단위)
 * - limit: 최대 검사할 이벤트 수
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "룰 실행 요청")
public class RuleExecutionRequest {
    
    @NotBlank(message = "룰 조건 JSON은 필수입니다")
    @Schema(description = "룰 조건 JSON", 
            example = "{\"fieldName\":\"TRX_AMT\",\"operator\":\"GREATER_THAN\",\"value\":1000000}",
            required = true)
    private String ruleConditionJson;
    
    @NotBlank(message = "그룹 키는 필수입니다")
    @Schema(description = "검사 대상 그룹 키", example = "CUS001", required = true)
    private String groupKey;
    
    @NotNull(message = "시간 윈도우는 필수입니다")
    @Min(value = 1, message = "시간 윈도우는 1분 이상이어야 합니다")
    @Schema(description = "검사할 시간 윈도우 (분)", example = "60", required = true)
    private Integer timeWindowMinutes;
    
    @NotNull(message = "검사 제한 수는 필수입니다")
    @Min(value = 1, message = "검사 제한 수는 1개 이상이어야 합니다")
    @Schema(description = "최대 검사할 이벤트 수", example = "100", required = true)
    private Integer limit;
}