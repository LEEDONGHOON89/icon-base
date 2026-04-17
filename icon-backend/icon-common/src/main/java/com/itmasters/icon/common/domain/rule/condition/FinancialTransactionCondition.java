package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/**
 * 금융거래 관련 룰 조건 처리 (FINANCIAL_TRANSACTION 도메인 전용)
 * 
 * 지원 조건:
 * - T01: 휴일 거래
 * - T02: 평일 거래
 * - T03: 야간 거래 (22시-06시)
 * - T04: 주간 거래 (06시-22시)
 * - T05: 점심시간 거래 (12시-13시)
 * - T06: 업무시간 거래 (09시-18시)
 * - T07: 새벽 거래 (00시-06시)
 * - T08: 오후 거래 (12시-18시)
 * - T09: 주말 거래
 * - T10: 월말 거래 (매월 마지막 3일)
 * - T11: 월초 거래 (매월 첫 3일)
 * - T12: 급여일 거래 (매월 25일)
 * - T13: 해외 거래
 * - T14: 국내 거래
 */
@Slf4j
public class FinancialTransactionCondition extends RuleConditionV2 {
    
    // standard_fields 테이블 기반 필수 필드들
    private static final Set<String> REQUIRED_FIELDS_FOR_TIME = Set.of(
        "transaction_date",       // 거래일시 (standard_fields에 있음)
        "transaction_time",       // 거래시간 (standard_fields에 있음)
        "created_at"              // 생성일시 (standard_fields에 있음)
    );
    
    private static final Set<String> REQUIRED_FIELDS_FOR_LOCATION = Set.of(
        "transaction_country",    // 거래 국가 (standard_fields에 있음)
        "country_code",          // 국가 코드 (standard_fields에 있음)
        "location"               // 위치 정보 (standard_fields에 있음)
    );
    
    private static final Set<String> REQUIRED_FIELDS_FOR_DATE = Set.of(
        "transaction_date",       // 거래일 (standard_fields에 있음)
        "created_at"              // 생성일시 (standard_fields에 있음)
    );
    
    // 시간대 정의
    private static final LocalTime NIGHT_START = LocalTime.of(22, 0);    // 22:00
    private static final LocalTime NIGHT_END = LocalTime.of(6, 0);       // 06:00
    private static final LocalTime DAY_START = LocalTime.of(6, 0);       // 06:00
    private static final LocalTime DAY_END = LocalTime.of(22, 0);        // 22:00
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);    // 12:00
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);      // 13:00
    private static final LocalTime BUSINESS_START = LocalTime.of(9, 0);  // 09:00
    private static final LocalTime BUSINESS_END = LocalTime.of(18, 0);   // 18:00
    private static final LocalTime DAWN_START = LocalTime.of(0, 0);      // 00:00
    private static final LocalTime DAWN_END = LocalTime.of(6, 0);        // 06:00
    private static final LocalTime AFTERNOON_START = LocalTime.of(12, 0); // 12:00
    private static final LocalTime AFTERNOON_END = LocalTime.of(18, 0);  // 18:00
    
    // 국가 코드
    private static final String KOREA_COUNTRY_CODE = "KR";
    
    @Override
    public boolean supports(RuleDomain domain) {
        return domain == RuleDomain.FINANCIAL_TRANSACTION;
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
                case EQUALS -> evaluateTransactionEquals(eventData, conditionValue);
                case NOT_EQUALS -> evaluateTransactionNotEquals(eventData, conditionValue);
                case WITHIN_HOURS -> evaluateTimeWithinHours(eventData, conditionValue);
                case WITHIN_DAYS -> evaluateDateWithinDays(eventData, conditionValue);
                default -> {
                    log.error("지원하지 않는 연산자: {} (FINANCIAL_TRANSACTION 도메인)", operator);
                    throw new IllegalArgumentException("FINANCIAL_TRANSACTION 도메인에서 지원하지 않는 연산자: " + operator);
                }
            };
            
        } catch (Exception e) {
            log.error("FinancialTransactionCondition 평가 중 오류 발생 - groupKey: {}, error: {}", groupKey, e.getMessage(), e);
            throw new RuntimeException("금융거래 조건 평가 실패", e);
        }
    }
    
    /**
     * 거래 타입/위치 조건 평가 (T01-T02, T09, T13-T14)
     */
    private boolean evaluateTransactionEquals(Map<String, Object> eventData, Object conditionValue) {
        String conditionStr = conditionValue.toString();
        
        switch (conditionStr) {
            case "HOLIDAY" -> {
                return evaluateHolidayTransaction(eventData);
            }
            case "WEEKDAY" -> {
                return evaluateWeekdayTransaction(eventData);
            }
            case "WEEKEND" -> {
                return evaluateWeekendTransaction(eventData);
            }
            case "MONTH_END" -> {
                return evaluateMonthEndTransaction(eventData);
            }
            case "MONTH_START" -> {
                return evaluateMonthStartTransaction(eventData);
            }
            case "PAYDAY" -> {
                return evaluatePaydayTransaction(eventData);
            }
            case "OVERSEAS" -> {
                return evaluateOverseasTransaction(eventData);
            }
            case "DOMESTIC" -> {
                return evaluateDomesticTransaction(eventData);
            }
            default -> {
                log.warn("알 수 없는 거래 조건: {}", conditionStr);
                return false;
            }
        }
    }
    
    /**
     * 거래 타입/위치 부정 조건 평가
     */
    private boolean evaluateTransactionNotEquals(Map<String, Object> eventData, Object conditionValue) {
        return !evaluateTransactionEquals(eventData, conditionValue);
    }
    
    /**
     * 시간대 조건 평가 (T03-T08)
     */
    private boolean evaluateTimeWithinHours(Map<String, Object> eventData, Object conditionValue) {
        validateRequiredFieldsForTime(eventData);
        
        LocalDateTime transactionTime = extractTransactionTime(eventData);
        LocalTime time = transactionTime.toLocalTime();
        String timeCondition = conditionValue.toString();
        
        return switch (timeCondition) {
            case "NIGHT" -> isNightTime(time);      // T03: 야간 (22시-06시)
            case "DAY" -> isDayTime(time);          // T04: 주간 (06시-22시)
            case "LUNCH" -> isLunchTime(time);      // T05: 점심시간 (12시-13시)
            case "BUSINESS" -> isBusinessTime(time); // T06: 업무시간 (09시-18시)
            case "DAWN" -> isDawnTime(time);        // T07: 새벽 (00시-06시)
            case "AFTERNOON" -> isAfternoonTime(time); // T08: 오후 (12시-18시)
            default -> {
                log.warn("알 수 없는 시간 조건: {}", timeCondition);
                yield  false;
            }
        };
    }
    
    /**
     * 날짜 조건 평가 (T10-T12)
     */
    private boolean evaluateDateWithinDays(Map<String, Object> eventData, Object conditionValue) {
        validateRequiredFieldsForDate(eventData);
        
        LocalDateTime transactionTime = extractTransactionTime(eventData);
        LocalDate transactionDate = transactionTime.toLocalDate();
        String dateCondition = conditionValue.toString();
        
        return switch (dateCondition) {
            case "MONTH_END" -> isMonthEnd(transactionDate);    // T10: 월말 거래
            case "MONTH_START" -> isMonthStart(transactionDate); // T11: 월초 거래
            case "PAYDAY" -> isPayday(transactionDate);         // T12: 급여일 거래
            default -> {
                log.warn("알 수 없는 날짜 조건: {}", dateCondition);
                yield  false;
            }
        };
    }
    
    // 시간대 체크 메서드들
    private boolean isNightTime(LocalTime time) {
        return time.isAfter(NIGHT_START) || time.isBefore(NIGHT_END);
    }
    
    private boolean isDayTime(LocalTime time) {
        return !isNightTime(time);
    }
    
    private boolean isLunchTime(LocalTime time) {
        return !time.isBefore(LUNCH_START) && time.isBefore(LUNCH_END);
    }
    
    private boolean isBusinessTime(LocalTime time) {
        return !time.isBefore(BUSINESS_START) && time.isBefore(BUSINESS_END);
    }
    
    private boolean isDawnTime(LocalTime time) {
        return !time.isBefore(DAWN_START) && time.isBefore(DAWN_END);
    }
    
    private boolean isAfternoonTime(LocalTime time) {
        return !time.isBefore(AFTERNOON_START) && time.isBefore(AFTERNOON_END);
    }
    
    // 날짜 조건 평가 메서드들
    private boolean evaluateHolidayTransaction(Map<String, Object> eventData) {
        // TODO: 실제로는 공휴일 테이블과 연동
        return false;
    }
    
    private boolean evaluateWeekdayTransaction(Map<String, Object> eventData) {
        validateRequiredFieldsForDate(eventData);
        LocalDateTime transactionTime = extractTransactionTime(eventData);
        int dayOfWeek = transactionTime.getDayOfWeek().getValue();
        return dayOfWeek >= 1 && dayOfWeek <= 5; // 월~금
    }
    
    private boolean evaluateWeekendTransaction(Map<String, Object> eventData) {
        return !evaluateWeekdayTransaction(eventData);
    }
    
    private boolean evaluateMonthEndTransaction(Map<String, Object> eventData) {
        validateRequiredFieldsForDate(eventData);
        LocalDateTime transactionTime = extractTransactionTime(eventData);
        return isMonthEnd(transactionTime.toLocalDate());
    }
    
    private boolean evaluateMonthStartTransaction(Map<String, Object> eventData) {
        validateRequiredFieldsForDate(eventData);
        LocalDateTime transactionTime = extractTransactionTime(eventData);
        return isMonthStart(transactionTime.toLocalDate());
    }
    
    private boolean evaluatePaydayTransaction(Map<String, Object> eventData) {
        validateRequiredFieldsForDate(eventData);
        LocalDateTime transactionTime = extractTransactionTime(eventData);
        return isPayday(transactionTime.toLocalDate());
    }
    
    private boolean evaluateOverseasTransaction(Map<String, Object> eventData) {
        validateRequiredFieldsForLocation(eventData);
        String countryCode = (String) eventData.getOrDefault("country_code", 
                                      eventData.get("transaction_country"));
        return !KOREA_COUNTRY_CODE.equals(countryCode);
    }
    
    private boolean evaluateDomesticTransaction(Map<String, Object> eventData) {
        return !evaluateOverseasTransaction(eventData);
    }
    
    // 날짜 체크 메서드들
    private boolean isMonthEnd(LocalDate date) {
        int dayOfMonth = date.getDayOfMonth();
        int lastDayOfMonth = date.lengthOfMonth();
        return dayOfMonth > lastDayOfMonth - 3; // 마지막 3일
    }
    
    private boolean isMonthStart(LocalDate date) {
        return date.getDayOfMonth() <= 3; // 첫 3일
    }
    
    private boolean isPayday(LocalDate date) {
        return date.getDayOfMonth() == 25; // 매월 25일
    }
    
    /**
     * EventStream에서 현재 이벤트 데이터 조회 (임시 구현)
     */
    private Map<String, Object> getCurrentEventData(String groupKey) {
        // TODO: 실제로는 EventStreamService를 통해 조회
        return Map.of();
    }
    
    /**
     * EventData에서 거래 시간 추출
     */
    private LocalDateTime extractTransactionTime(Map<String, Object> eventData) {
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
        
        // 3. transaction_time 필드가 있는 경우 (오늘 날짜로 가정)
        if (eventData.containsKey("transaction_time")) {
            String transactionTimeStr = eventData.get("transaction_time").toString();
            LocalTime time = LocalTime.parse(transactionTimeStr, DateTimeFormatter.ofPattern("HH:mm:ss"));
            return LocalDateTime.of(LocalDate.now(), time);
        }
        
        throw new IllegalArgumentException("거래 시간을 추출할 수 있는 필드가 없습니다.");
    }
    
    /**
     * 시간 관련 필수 필드 검증
     */
    private void validateRequiredFieldsForTime(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_TIME.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("거래 시간 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_TIME));
        }
    }
    
    /**
     * 위치 관련 필수 필드 검증
     */
    private void validateRequiredFieldsForLocation(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_LOCATION.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("거래 위치 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_LOCATION));
        }
    }
    
    /**
     * 날짜 관련 필수 필드 검증
     */
    private void validateRequiredFieldsForDate(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_DATE.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("거래 날짜 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_DATE));
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
        return "FINANCIAL_TRANSACTION_CONDITION";
    }
    
    @Override
    public String toHumanReadableString(Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;
            
            RuleOperator operator = extractOperator(rule);
            Object value = rule.get("condition_value");
            String valueStr = value.toString();
            
            return switch (operator) {
                case EQUALS -> switch (valueStr) {
                    case "HOLIDAY" -> "휴일 거래";
                    case "WEEKDAY" -> "평일 거래";
                    case "WEEKEND" -> "주말 거래";
                    case "MONTH_END" -> "월말 거래";
                    case "MONTH_START" -> "월초 거래";
                    case "PAYDAY" -> "급여일 거래";
                    case "OVERSEAS" -> "해외 거래";
                    case "DOMESTIC" -> "국내 거래";
                    default -> String.format("거래 조건: %s", valueStr);
                };
                case WITHIN_HOURS -> switch (valueStr) {
                    case "NIGHT" -> "야간 거래 (22시-06시)";
                    case "DAY" -> "주간 거래 (06시-22시)";
                    case "LUNCH" -> "점심시간 거래 (12시-13시)";
                    case "BUSINESS" -> "업무시간 거래 (09시-18시)";
                    case "DAWN" -> "새벽 거래 (00시-06시)";
                    case "AFTERNOON" -> "오후 거래 (12시-18시)";
                    default -> String.format("시간대 조건: %s", valueStr);
                };
                default -> String.format("거래 조건: %s %s", operator, value);
            };
            
        } catch (Exception e) {
            return "거래 조건 (표시 오류)";
        }
    }
    
    @Override
    public boolean requiresHistoryData() {
        return false; // 거래 조건은 현재 거래 데이터만 필요
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
            log.error("거래 조건 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }
}