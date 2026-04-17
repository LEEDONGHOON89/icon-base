package com.itmasters.icon.engine.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itmasters.icon.engine.executor.RuleExecutor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 룰 실행 응답 DTO
 * 
 * Event Stream 기반 룰 실행 결과:
 * - matched: 룰 조건 만족 여부
 * - matchedCount: 조건을 만족한 이벤트 수
 * - totalChecked: 총 검사한 이벤트 수
 * - firstMatchedEventId: 첫 번째 매칭된 이벤트 ID (있는 경우)
 * - errorMessage: 오류 메시지 (실패 시)
 * - executionTime: 실행 시간
 */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "룰 실행 응답")
public class RuleExecutionResponse {
    
    @Schema(description = "룰 ID", example = "RULE001")
    private String ruleId;
    
    @Schema(description = "그룹 키", example = "CUS001")
    private String groupKey;
    
    @Schema(description = "룰 조건 만족 여부", example = "true")
    private Boolean matched;
    
    @Schema(description = "조건을 만족한 이벤트 수", example = "3")
    private Integer matchedCount;
    
    @Schema(description = "총 검사한 이벤트 수", example = "50")
    private Integer totalChecked;
    
    @Schema(description = "첫 번째 매칭된 이벤트 ID", example = "12345")
    private Long firstMatchedEventId;
    
    @Schema(description = "오류 메시지 (실패 시)")
    private String errorMessage;
    
    @Schema(description = "실행 시간", example = "2025-08-22T15:30:00")
    private LocalDateTime executionTime;
    
    @Schema(description = "오류 발생 여부", example = "false")
    private Boolean hasError;
    
    /**
     * RuleExecutionResult로부터 응답 DTO 생성
     */
    public static RuleExecutionResponse from(RuleExecutor.RuleExecutionResult result) {
        Long firstEventId = result.getFirstMatchedEvent() != null 
                ? result.getFirstMatchedEvent().getEventStreamId() 
                : null;
        
        LocalDateTime executionTime = LocalDateTime.now(); // 실행 완료 시점
        
        return new RuleExecutionResponse(
                result.getRuleId(),
                result.getGroupKey(),
                result.isMatched(),
                result.getMatchedCount(),
                result.getTotalChecked(),
                firstEventId,
                result.getErrorMessage(),
                executionTime,
                result.hasError()
        );
    }
}