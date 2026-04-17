package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/**
 * 빈도/횟수 관련 룰 조건 처리 (FREQUENCY 도메인 전용)
 * 
 * 지원 조건:
 * - F01: 1회 이상
 * - F02: 2회 이상
 * - F03: 3회 이상
 * - F04: 5회 이상
 * - F05: 10회 이상
 * - F06: 동일한 액션 연속 3회 이상
 */
@Slf4j
public class FrequencyCondition extends RuleConditionV2 {
    
    // standard_fields 테이블 기반 필수 필드들
    private static final Set<String> REQUIRED_FIELDS_FOR_FREQUENCY = Set.of(
        "customer_id",            // 고객 ID (standard_fields에 있음)
        "account_id",             // 계좌 ID (standard_fields에 있음)
        "transaction_type",       // 거래 유형 (standard_fields에 있음)
        "action_type"             // 액션 타입 (standard_fields에 있음)
    );
    
    private static final Set<String> REQUIRED_FIELDS_FOR_TIME_RANGE = Set.of(
        "transaction_date",       // 거래일시 (standard_fields에 있음)
        "created_at",             // 생성일시 (standard_fields에 있음)
        "login_dt"                // 로그인일시 (standard_fields에 있음)
    );
    
    @Override
    public boolean supports(RuleDomain domain) {
        return domain == RuleDomain.FREQUENCY;
    }
    
    @Override
    public boolean evaluate(String groupKey, Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;
            
            RuleOperator operator = extractOperator(rule);
            Object conditionValue = rule.get("condition_value");
            
            // EventStream에서 해당 groupKey의 현재 거래 데이터 조회
            Map<String, Object> eventData = getCurrentEventData(groupKey);
            
            return switch (operator) {
                case GREATER_THAN_OR_EQUALS -> evaluateFrequencyGreaterThanOrEquals(eventData, conditionValue, groupKey);
                case REPEATED_TIMES -> evaluateRepeatedTimes(eventData, conditionValue, groupKey);
                default -> {
                    log.error("지원하지 않는 연산자: {} (FREQUENCY 도메인)", operator);
                    throw new IllegalArgumentException("FREQUENCY 도메인에서 지원하지 않는 연산자: " + operator);
                }
            };
            
        } catch (Exception e) {
            log.error("FrequencyCondition 평가 중 오류 발생 - groupKey: {}, error: {}", groupKey, e.getMessage(), e);
            throw new RuntimeException("빈도 조건 평가 실패", e);
        }
    }
    
    /**
     * F01-F05: 횟수 이상 조건 평가
     */
    private boolean evaluateFrequencyGreaterThanOrEquals(Map<String, Object> eventData, Object conditionValue, String groupKey) {
        validateRequiredFieldsForFrequency(eventData);
        validateRequiredFieldsForTimeRange(eventData);
        
        int targetCount = Integer.parseInt(conditionValue.toString());
        
        // 현재 거래 정보 추출
        String customerId = (String) eventData.get("customer_id");
        String transactionType = (String) eventData.getOrDefault("transaction_type", eventData.get("action_type"));
        
        // EventStream에서 해당 고객의 동일 거래 유형 발생 횟수 조회 (최근 24시간 내)
        int actualCount = getTransactionCountInTimeRange(customerId, transactionType, 24); // 24시간
        
        boolean result = actualCount >= targetCount;
        log.debug("빈도 조건 평가: customer={}, type={}, count={} >= {} = {}", 
                 customerId, transactionType, actualCount, targetCount, result);
        return result;
    }
    
    /**
     * F06: 동일 액션 연속 횟수 조건 평가
     */
    private boolean evaluateRepeatedTimes(Map<String, Object> eventData, Object conditionValue, String groupKey) {
        validateRequiredFieldsForFrequency(eventData);
        
        int targetConsecutiveCount = Integer.parseInt(conditionValue.toString());
        
        // 현재 거래 정보 추출
        String customerId = (String) eventData.get("customer_id");
        String transactionType = (String) eventData.getOrDefault("transaction_type", eventData.get("action_type"));
        
        // EventStream에서 해당 고객의 최근 연속 동일 액션 횟수 조회
        int consecutiveCount = getConsecutiveSameActionCount(customerId, transactionType);
        
        boolean result = consecutiveCount >= targetConsecutiveCount;
        log.debug("연속 액션 조건 평가: customer={}, type={}, consecutive={} >= {} = {}", 
                 customerId, transactionType, consecutiveCount, targetConsecutiveCount, result);
        return result;
    }
    
    /**
     * EventStream에서 현재 이벤트 데이터 조회 (임시 구현)
     */
    private Map<String, Object> getCurrentEventData(String groupKey) {
        // TODO: 실제로는 EventStreamService를 통해 조회
        return Map.of();
    }
    
    /**
     * 특정 시간 범위 내 거래 횟수 조회 (임시 구현)
     */
    private int getTransactionCountInTimeRange(String customerId, String transactionType, int hours) {
        // TODO: 실제로는 EventStreamService를 통해 조회
        // SELECT COUNT(*) FROM event_stream 
        // WHERE customer_id = ? 
        //   AND transaction_type = ?
        //   AND transaction_date >= DATE_SUB(NOW(), INTERVAL ? HOUR)
        return 0;
    }
    
    /**
     * 연속 동일 액션 횟수 조회 (임시 구현)
     */
    private int getConsecutiveSameActionCount(String customerId, String transactionType) {
        // TODO: 실제로는 EventStreamService를 통해 연속 액션 조회
        // 최근 거래부터 역순으로 조회하면서 동일한 transaction_type이 연속으로 몇 번 나오는지 계산
        /*
         * WITH ordered_transactions AS (
         *   SELECT transaction_type, 
         *          ROW_NUMBER() OVER (ORDER BY transaction_date DESC) as rn
         *   FROM event_stream 
         *   WHERE customer_id = ?
         *   ORDER BY transaction_date DESC
         * )
         * SELECT COUNT(*) 
         * FROM ordered_transactions 
         * WHERE transaction_type = ? 
         *   AND rn <= (
         *     SELECT MIN(rn) 
         *     FROM ordered_transactions 
         *     WHERE transaction_type != ? OR rn = 1
         *   ) - 1
         */
        return 0;
    }
    
    /**
     * EventData에서 현재 시간 추출
     */
    private LocalDateTime extractCurrentTime(Map<String, Object> eventData) {
        // 1. transaction_date 필드가 있는 경우
        if (eventData.containsKey("transaction_date")) {
            String transactionDateStr = eventData.get("transaction_date").toString();
            return LocalDateTime.parse(transactionDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        // 2. created_at 필드가 있는 경우
        if (eventData.containsKey("created_at")) {
            String createdAtStr = eventData.get("created_at").toString();
            return LocalDateTime.parse(createdAtStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        // 3. login_dt 필드가 있는 경우
        if (eventData.containsKey("login_dt")) {
            String loginDtStr = eventData.get("login_dt").toString();
            return LocalDateTime.parse(loginDtStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        // 기본값: 현재 시간
        return LocalDateTime.now();
    }
    
    /**
     * 빈도 관련 필수 필드 검증
     */
    private void validateRequiredFieldsForFrequency(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_FREQUENCY.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("빈도 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_FREQUENCY));
        }
    }
    
    /**
     * 시간 범위 관련 필수 필드 검증
     */
    private void validateRequiredFieldsForTimeRange(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_TIME_RANGE.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("시간 범위 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_TIME_RANGE));
        }
    }
    
    /**
     * RuleEntity에서 연산자 추출 (임시 구현)
     */
    private RuleOperator extractOperator(Map<String, Object> rule) {
        // TODO: 실제 RuleEntity 구조에 맞게 수정
        String operatorStr = (String) rule.get("operator");
        return RuleOperator.valueOf(operatorStr);
    }
    
    @Override
    public String getConditionType() {
        return "FREQUENCY_CONDITION";
    }
    
    @Override
    public String toHumanReadableString(Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;
            
            RuleOperator operator = extractOperator(rule);
            Object value = rule.get("condition_value");
            int count = Integer.parseInt(value.toString());
            
            return switch (operator) {
                case GREATER_THAN_OR_EQUALS -> {
                    if (count == 1) yield "1회 이상";
                    else yield String.format("%d회 이상", count);
                }
                case REPEATED_TIMES -> String.format("동일 액션 연속 %d회 이상", count);
                default -> String.format("빈도 조건: %s %s", operator, value);
            };
            
        } catch (Exception e) {
            return "빈도 조건 (표시 오류)";
        }
    }
    
    @Override
    public boolean requiresHistoryData() {
        return true; // 빈도 조건은 항상 이력 데이터가 필요
    }
    
    @Override
    public boolean isValidCondition(Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;
            
            // 필수 필드 검증
            boolean hasRequiredFields = rule.containsKey("operator") && 
                                       rule.containsKey("condition_value") &&
                                       extractOperator(rule) != null;
            
            if (!hasRequiredFields) {
                return false;
            }
            
            // 조건값이 양의 정수인지 검증
            try {
                int count = Integer.parseInt(rule.get("condition_value").toString());
                return count > 0;
            } catch (NumberFormatException e) {
                log.error("빈도 조건값이 유효한 정수가 아닙니다: {}", rule.get("condition_value"));
                return false;
            }
                   
        } catch (Exception e) {
            log.error("빈도 조건 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }
}