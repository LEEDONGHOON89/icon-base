package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/**
 * 고객 관련 룰 조건 처리 (CUSTOMER 도메인 전용)
 * 
 * 지원 조건:
 * - C01: 만65세 이상 고객
 * - C02: 만20세 미만 고객  
 * - C03: 미성년자
 * - C04: 최근 12개월 비대면뱅킹 거래 이력 없는 고객
 * - C05: 최근 12개월 ATM 입/출금 거래 없는 고객
 * - C06: 최근 12개월 오픈뱅킹 사용 이력 없는 고객
 * - C07: 최근 12개월 대출이력 없는 고객
 */
@Slf4j
public class CustomerCondition extends RuleConditionV2 {
    
    // standard_fields 테이블 기반 필수 필드들
    private static final Set<String> REQUIRED_FIELDS_FOR_AGE = Set.of(
        "customer_age",           // 직접 나이 필드
        "birth_date"              // 생년월일 필드 (standard_fields에 있음)
    );
    
    private static final Set<String> REQUIRED_FIELDS_FOR_HISTORY = Set.of(
        "customer_id",            // 고객 ID (standard_fields에 있음)
        "transaction_date",       // 거래일 (standard_fields에 있음)
        "transaction_type"        // 거래 유형 (standard_fields에 있음)
    );
    
    // 97개 샘플 지원을 위한 필드 매핑
    private static final Map<String, Set<String>> TRANSACTION_TYPE_MAPPING = Map.of(
        "비대면뱅킹", Set.of("ONLINE_BANKING", "NON_FACE_BANKING"),
        "ATM", Set.of("ATM_DEPOSIT", "ATM_WITHDRAWAL"), 
        "오픈뱅킹", Set.of("OPEN_BANKING"),
        "대출", Set.of("LOAN", "CREDIT")
    );
    
    @Override
    public boolean supports(RuleDomain domain) {
        return domain == RuleDomain.CUSTOMER;
    }
    
    @Override
    public boolean evaluate(String groupKey, Object ruleEntity) {
        try {
            // TODO: ruleEntity에서 실제 룰 정보 추출하는 로직 필요
            // 현재는 임시로 Map으로 처리
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;
            
            RuleOperator operator = extractOperator(rule);
            Object conditionValue = rule.get("condition_value");
            
            // EventStream에서 해당 groupKey의 현재 및 이력 데이터 조회
            Map<String, Object> eventData = getCurrentEventData(groupKey);
            
            return switch (operator) {
                case GREATER_THAN_OR_EQUALS -> evaluateAgeGreaterThanOrEqual(eventData, conditionValue);
                case LESS_THAN_OR_EQUALS -> evaluateAgeLessThan(eventData, conditionValue);
                case NO_HISTORY_WITHIN_MONTHS -> evaluateNoHistoryWithinMonths(groupKey, conditionValue, rule);
                default -> {
                    log.error("지원하지 않는 연산자: {} (CUSTOMER 도메인)", operator);
                    throw new IllegalArgumentException("CUSTOMER 도메인에서 지원하지 않는 연산자: " + operator);
                }
            };
            
        } catch (Exception e) {
            log.error("CustomerCondition 평가 중 오류 발생 - groupKey: {}, error: {}", groupKey, e.getMessage(), e);
            throw new RuntimeException("고객 조건 평가 실패", e);
        }
    }
    
    /**
     * C01: 만65세 이상, C02: 만20세 미만 등 나이 기반 조건 평가
     */
    private boolean evaluateAgeGreaterThanOrEqual(Map<String, Object> eventData, Object conditionValue) {
        validateRequiredFieldsForAge(eventData);
        
        int targetAge = Integer.parseInt(conditionValue.toString());
        int customerAge = extractCustomerAge(eventData);
        
        boolean result = customerAge >= targetAge;
        log.debug("나이 조건 평가: {} >= {} = {}", customerAge, targetAge, result);
        return result;
    }
    
    private boolean evaluateAgeLessThan(Map<String, Object> eventData, Object conditionValue) {
        validateRequiredFieldsForAge(eventData);
        
        int targetAge = Integer.parseInt(conditionValue.toString());
        int customerAge = extractCustomerAge(eventData);
        
        boolean result = customerAge < targetAge;
        log.debug("나이 조건 평가: {} < {} = {}", customerAge, targetAge, result);
        return result;
    }
    
    /**
     * C04~C07: 최근 N개월 거래 이력 없음 조건 평가
     */
    private boolean evaluateNoHistoryWithinMonths(String groupKey, Object conditionValue, Map<String, Object> rule) {
        int months = Integer.parseInt(conditionValue.toString());
        String transactionType = (String) rule.get("transaction_type"); // "비대면뱅킹", "ATM", "오픈뱅킹", "대출"
        
        // EventStream에서 해당 groupKey의 최근 N개월 이력 조회
        boolean hasRecentHistory = hasRecentTransactionHistory(groupKey, months, transactionType);
        
        // "거래 이력 없음"이므로 이력이 없으면 true
        boolean result = !hasRecentHistory;
        log.debug("거래이력 조건 평가: groupKey={}, months={}, type={}, hasHistory={}, result={}", 
                 groupKey, months, transactionType, hasRecentHistory, result);
        return result;
    }
    
    /**
     * EventStream에서 현재 이벤트 데이터 조회 (임시 구현)
     */
    private Map<String, Object> getCurrentEventData(String groupKey) {
        // TODO: 실제로는 EventStreamService를 통해 조회
        // 현재는 임시로 빈 맵 반환
        return Map.of();
    }
    
    /**
     * EventStream에서 이력 데이터 존재 여부 확인 (임시 구현)
     */
    private boolean hasRecentTransactionHistory(String groupKey, int months, String transactionType) {
        // TODO: 실제로는 EventStreamService를 통해 이력 조회
        // SELECT COUNT(*) FROM event_stream 
        // WHERE group_key = ? AND transaction_type = ? 
        //   AND created_at >= DATE_SUB(NOW(), INTERVAL ? MONTH)
        return false;
    }
    
    /**
     * 나이 관련 필수 필드 검증
     */
    private void validateRequiredFieldsForAge(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_AGE.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("고객 나이 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_AGE));
        }
    }
    
    /**
     * EventData에서 고객 나이 추출
     */
    private int extractCustomerAge(Map<String, Object> eventData) {
        // 1. 직접 나이 필드가 있는 경우
        if (eventData.containsKey("customer_age")) {
            return Integer.parseInt(eventData.get("customer_age").toString());
        }
        
        // 2. 생년월일에서 계산
        if (eventData.containsKey("birth_date") || eventData.containsKey("customer_birth_date")) {
            String birthDateStr = (String) eventData.getOrDefault("birth_date", eventData.get("customer_birth_date"));
            LocalDate birthDate = LocalDate.parse(birthDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            return LocalDate.now().getYear() - birthDate.getYear();
        }
        
        throw new IllegalArgumentException("고객 나이를 계산할 수 있는 필드가 없습니다.");
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
        return "CUSTOMER_CONDITION";
    }
    
    @Override
    public String toHumanReadableString(Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;
            
            RuleOperator operator = extractOperator(rule);
            Object value = rule.get("condition_value");
            
            return switch (operator) {
                case GREATER_THAN_OR_EQUALS -> String.format("만%s세 이상", value);
                case LESS_THAN_OR_EQUALS -> String.format("만%s세 이하", value);
                case NO_HISTORY_WITHIN_MONTHS -> String.format("최근 %s개월 %s 거래 이력 없음",
                                                               value, rule.get("transaction_type"));
                default -> String.format("고객 조건: %s %s", operator, value);
            };
            
        } catch (Exception e) {
            return "고객 조건 (표시 오류)";
        }
    }
    
    @Override
    public boolean requiresHistoryData() {
        return true; // 고객 조건은 대부분 이력 데이터가 필요
    }
    
    @Override
    public boolean isValidCondition(Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;
            
            // 필수 필드 검증
            return rule.containsKey("operator") && 
                   rule.containsKey("condition_value") &&
                   extractOperator(rule) != null;
                   
        } catch (Exception e) {
            log.error("고객 조건 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }
}