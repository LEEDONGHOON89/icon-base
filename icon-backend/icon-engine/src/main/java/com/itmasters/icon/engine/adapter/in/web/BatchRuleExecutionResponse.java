package com.itmasters.icon.engine.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.itmasters.icon.engine.executor.RuleExecutor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 배치 룰 실행 응답 DTO
 * 
 * 여러 그룹에 대한 룰 실행 결과:
 * - results: 그룹별 룰 실행 결과 목록
 * - totalGroups: 총 처리한 그룹 수
 * - matchedGroups: 룰 조건을 만족한 그룹 수
 * - totalEventsChecked: 총 검사한 이벤트 수
 * - totalMatchedEvents: 총 매칭된 이벤트 수
 * - executionTime: 배치 실행 완료 시간
 */
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "배치 룰 실행 응답")
public class BatchRuleExecutionResponse {
    
    @Schema(description = "그룹별 룰 실행 결과 목록")
    private List<RuleExecutionResponse> results;
    
    @Schema(description = "총 처리한 그룹 수", example = "3")
    private Integer totalGroups;
    
    @Schema(description = "룰 조건을 만족한 그룹 수", example = "2")
    private Integer matchedGroups;
    
    @Schema(description = "총 검사한 이벤트 수", example = "150")
    private Integer totalEventsChecked;
    
    @Schema(description = "총 매칭된 이벤트 수", example = "8")
    private Integer totalMatchedEvents;
    
    @Schema(description = "배치 실행 완료 시간", example = "2025-08-22T15:30:00")
    private LocalDateTime executionTime;
    
    /**
     * RuleExecutionResult 목록으로부터 배치 응답 DTO 생성
     */
    public static BatchRuleExecutionResponse from(List<RuleExecutor.RuleExecutionResult> results) {
        List<RuleExecutionResponse> responses = results.stream()
                .map(RuleExecutionResponse::from)
                .collect(Collectors.toList());
        
        int totalGroups = results.size();
        int matchedGroups = (int) results.stream().mapToLong(r -> r.isMatched() ? 1 : 0).sum();
        int totalEventsChecked = results.stream().mapToInt(RuleExecutor.RuleExecutionResult::getTotalChecked).sum();
        int totalMatchedEvents = results.stream().mapToInt(RuleExecutor.RuleExecutionResult::getMatchedCount).sum();
        
        return new BatchRuleExecutionResponse(
                responses,
                totalGroups,
                matchedGroups,
                totalEventsChecked,
                totalMatchedEvents,
                LocalDateTime.now()
        );
    }
}