package com.itmasters.icon.engine.adapter.in.web;

import com.itmasters.icon.engine.executor.RuleExecutor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 룰 실행 API 컨트롤러 - Event Stream 기반 룰 실행
 * 
 * Raw 데이터 기반 룰 실행:
 * - Event Stream에 저장된 원본 데이터에서 필드 동적 추출
 * - JSON 조건을 파싱하여 실제 데이터와 비교
 * - 타임라인 기반 복합 조건 지원
 * - 배치 처리를 통한 다중 그룹 룰 실행
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/engine/rules")
@RequiredArgsConstructor
@Tag(name = "Rule Execution", description = "룰 실행 엔진 API")
public class RuleExecutionController {
    
    private final RuleExecutor ruleExecutor;
    
    /**
     * 단일 그룹에 대한 룰 실행
     * 
     * @param ruleId 실행할 룰 ID
     * @param request 룰 실행 요청 정보
     * @return 룰 실행 결과
     */
    @PostMapping("/{ruleId}/execute")
    @Operation(summary = "단일 룰 실행", 
               description = "특정 그룹키에 대해 룰을 실행하고 결과를 반환합니다")
    @ApiResponse(responseCode = "200", description = "룰 실행 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터")
    @ApiResponse(responseCode = "500", description = "룰 실행 중 오류 발생")
    public ResponseEntity<RuleExecutionResponse> executeRule(
            @Parameter(description = "룰 ID", required = true) 
            @PathVariable String ruleId,
            
            @Parameter(description = "룰 실행 요청", required = true)
            @RequestBody RuleExecutionRequest request) {
        
        log.info("룰 실행 요청 - ruleId: {}, groupKey: {}", ruleId, request.getGroupKey());
        
        RuleExecutor.RuleExecutionResult result = ruleExecutor.executeRule(
                ruleId,
                request.getRuleConditionJson(),
                request.getGroupKey(),
                request.getTimeWindowMinutes(),
                request.getLimit()
        );
        
        RuleExecutionResponse response = RuleExecutionResponse.from(result);
        
        log.info("룰 실행 완료 - ruleId: {}, matched: {}, checked: {}", 
                ruleId, result.isMatched(), result.getTotalChecked());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 다중 그룹에 대한 룰 실행 (배치 처리)
     * 
     * @param ruleId 실행할 룰 ID
     * @param request 배치 룰 실행 요청 정보
     * @return 그룹별 룰 실행 결과 목록
     */
    @PostMapping("/{ruleId}/execute-batch")
    @Operation(summary = "배치 룰 실행", 
               description = "여러 그룹키에 대해 룰을 일괄 실행하고 결과를 반환합니다")
    @ApiResponse(responseCode = "200", description = "배치 룰 실행 성공")
    @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터")
    @ApiResponse(responseCode = "500", description = "룰 실행 중 오류 발생")
    public ResponseEntity<BatchRuleExecutionResponse> executeRuleForGroups(
            @Parameter(description = "룰 ID", required = true)
            @PathVariable String ruleId,
            
            @Parameter(description = "배치 룰 실행 요청", required = true)
            @RequestBody BatchRuleExecutionRequest request) {
        
        log.info("배치 룰 실행 요청 - ruleId: {}, 그룹 수: {}", ruleId, request.getGroupKeys().size());
        
        List<RuleExecutor.RuleExecutionResult> results = ruleExecutor.executeRuleForGroups(
                ruleId,
                request.getRuleConditionJson(),
                request.getGroupKeys(),
                request.getTimeWindowMinutes(),
                request.getLimit()
        );
        
        BatchRuleExecutionResponse response = BatchRuleExecutionResponse.from(results);
        
        long matchedCount = results.stream().mapToLong(r -> r.isMatched() ? 1 : 0).sum();
        log.info("배치 룰 실행 완료 - ruleId: {}, 총 그룹: {}, 매칭: {}", 
                ruleId, results.size(), matchedCount);
        
        return ResponseEntity.ok(response);
    }
}