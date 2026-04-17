package com.itmasters.icon.engine.detector;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineRuleRepository;
import com.itmasters.icon.engine.dto.*;
import com.itmasters.icon.engine.service.EventStreamService;
import com.itmasters.icon.engine.service.RuleEvaluationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 룰 감지 엔진
 * 이미 매핑된 데이터에 대해 룰을 평가하고 결과를 생성
 * Event Stream 기반 이력 데이터를 활용한 스마트 룰 평가 지원
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleDetector {

    private final RuleEvaluationService ruleEvaluationService;
    private final EngineRuleRepository engineRuleRepository;
    private final EventStreamService eventStreamService;
    
    /**
     * 룰 감지 실행 (이미 매핑된 데이터에 대해)
     * 
     * @param processedData 처리된 데이터 (이미 필드 매핑 완료)
     * @param profileId 프로파일 ID
     * @param context 실행 컨텍스트
     * @return 탐지 결과
     */
    public DetectionResult detect(DataProcessingResultDto processedData, 
                                 String profileId, 
                                 DetectionContext context) {
        
        log.debug("룰 탐지 시작 - profileId: {}, 데이터 건수: {}", 
                 profileId, processedData.getDataCount());
        
        List<EvaluationResult> evaluations = new ArrayList<>();
        
        try {
            // 1. 프로파일에 해당하는 룰 조회
            List<EngineRuleEntity> rules = engineRuleRepository.findActiveRulesByProfileId(profileId);
            log.debug("프로파일 {} 에 대한 룰 {}개 로드", profileId, rules.size());
            
            // 2. Row-by-row 처리 (이미 매핑된 데이터)
            int rowNumber = 0;
            List<RuleMatchedData> matchedDataList = new ArrayList<>();  // 매칭된 상세 데이터
            Set<String> matchedRuleIds = new HashSet<>();
            Set<String> matchedGroupKeys = new HashSet<>();
            
            List<Map<String, Object>> mappedRows = processedData.getMappedData() != null
                    ? processedData.getMappedData()
                    : List.of();
            for (Map<String, Object> mappedRow : mappedRows) {
                rowNumber++;
                
                // 디버깅: 매핑된 데이터 확인
                if (rowNumber == 1) {
                    log.info("첫 번째 매핑된 데이터: {}", mappedRow);
                    log.info("데이터 타입 - transaction_amount: {}", 
                            mappedRow.get("transaction_amount") != null ? 
                            mappedRow.get("transaction_amount").getClass().getName() : "null");
                }
                
                // 3. 각 룰에 대해 평가 (where_json 조건 평가)
                for (EngineRuleEntity rule : rules) {
                    log.debug("룰 평가 시작 - ruleId: {}, where_json: {}",
                             rule.getRuleId(), rule.getWhereJson());
                    
                    // 룰 평가 수행 - 스마트 평가로 이력 필요 시 자동 전환
                    String groupKey = extractGroupKey(mappedRow);
                    
                    boolean isMatched;
                    if (groupKey != null) {
                        // 스마트 평가: 평가기가 이력을 필요로 하면 자동으로 이력 기반 평가 수행
                        isMatched = ruleEvaluationService.evaluateRuleSmart(
                            rule, 
                            mappedRow, 
                            groupKey, 
                            60,  // 기본 60분 이력 윈도우
                            100  // 최대 100개 이벤트 조회
                        );
                    } else {
                        // 그룹키가 없으면 기본 평가
                        log.debug("Group key not found for rule {}, using basic evaluation", rule.getRuleId());
                        isMatched = ruleEvaluationService.evaluateRule(rule, mappedRow);
                    }
                    
                    // EvaluationResult 생성
                    EvaluationResult evaluation = EvaluationResult.builder()
                        .ruleId(rule.getRuleId())
                        .ruleName(rule.getRuleName())
                        .matched(isMatched)
                        .matchedData(isMatched ? mappedRow : null)
                        .originalData(mappedRow)
                        .rowNumber((long) rowNumber)
                        .message(isMatched ? "Rule matched" : "Rule not matched")
                        .evaluatedAt(java.time.LocalDateTime.now())
                        .build();
                    
                    // 매치된 경우만 결과에 추가
                    if (evaluation.isMatched()) {
                        evaluations.add(evaluation);
                        // RuleMatchedData 생성 및 추가
                        RuleMatchedData matchedData = RuleMatchedData.of(
                            rule,
                            mappedRow,  // 매핑된 데이터
                            evaluation.getOriginalData(),  // 원본 데이터 (있다면)
                            rowNumber,
                            groupKey
                        );
                        matchedDataList.add(matchedData);

                        matchedRuleIds.add(rule.getRuleId());
                        if (groupKey != null) {
                            matchedGroupKeys.add(groupKey);
                        }
                        
                        log.debug("룰 매치 - row: {}, ruleId: {}, message: {}", 
                                rowNumber, rule.getRuleId(), evaluation.getMessage());
                    }
                }
            }
            
            // 5. 성공 결과 생성
            List<Map<String, Object>> matchedRecords = new ArrayList<>();
            for (EvaluationResult eval : evaluations) {
                matchedRecords.add(eval.getMatchedData());
            }
            
            DetectionResult result = DetectionResult.builder()
                .context(context)
                .success(true)
                .totalRows(processedData.getDataCount())
                .totalMatched(evaluations.size())
                .totalRules(rules.size())  // 실행한 룰 개수
                .matchedRecords(matchedRecords)
                .matchedData(matchedDataList)  // 매칭된 상세 데이터 추가
                .matchedRuleIds(matchedRuleIds)
                .matchedGroupKeys(matchedGroupKeys)
                .build();
            
            return result;
            
        } catch (Exception e) {
            log.error("룰 탐지 중 오류 발생", e);
            return DetectionResult.failed(context, e.getMessage());
        }
    }
    
    /**
     * 매핑된 데이터에서 그룹키 추출
     * 고객ID, 세션ID 등 이벤트 스트림 그룹핑에 사용되는 키 추출
     * 
     * @param mappedRow 매핑된 데이터 행
     * @return 그룹키 (customer_id, session_id 등)
     */
    private String extractGroupKey(Map<String, Object> mappedRow) {
        // 1순위: customer_id
        Object customerId = mappedRow.get("customer_id");
        if (customerId != null) {
            return customerId.toString();
        }
        
        // 2순위: CUS_ID (대문자)
        Object cusId = mappedRow.get("CUS_ID");
        if (cusId != null) {
            return cusId.toString();
        }
        
        // 3순위: user_id
        Object userId = mappedRow.get("user_id");
        if (userId != null) {
            return userId.toString();
        }
        
        // 4순위: session_id
        Object sessionId = mappedRow.get("session_id");
        if (sessionId != null) {
            return sessionId.toString();
        }
        
        // 5순위: account_number (계좌번호)
        Object accountNumber = mappedRow.get("account_number");
        if (accountNumber != null) {
            return accountNumber.toString();
        }
        
        log.debug("No suitable group key found in mapped data: {}", mappedRow.keySet());
        return null;
    }

    // no anchor prefilter; evaluation relies solely on where_json

    
}
