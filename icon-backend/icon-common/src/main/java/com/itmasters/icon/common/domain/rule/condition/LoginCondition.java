package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/**
 * 로그인/인증 관련 룰 조건 처리 (LOGIN 도메인 전용)
 * 
 * 지원 조건:
 * - L01: 로그인 실패 횟수 (5회 이상)
 * - L02: 연속 로그인 실패 (3회 이상)
 * - L03: 동일 IP에서 다수 계정 로그인 (10개 이상)
 * - L04: 새로운 IP에서 로그인
 * - L05: 해외 IP에서 로그인
 * - L06: VPN/Proxy를 통한 로그인
 * - L07: 비정상적인 로그인 시간 (새벽 2-5시)
 * - L08: 휴일/주말 로그인
 * - L09: 장시간 세션 유지 (12시간 이상)
 * - L10: 동시 다중 세션 (3개 이상)
 */
@Slf4j
public class LoginCondition extends RuleConditionV2 {
    
    // standard_fields 테이블 기반 필수 필드들
    private static final Set<String> REQUIRED_FIELDS_FOR_LOGIN = Set.of(
        "customer_id",            // 고객 ID (standard_fields에 있음)
        "login_id",               // 로그인 ID (standard_fields에 있음)
        "user_id",                // 사용자 ID (standard_fields에 있음)
        "account_id"              // 계좌 ID (standard_fields에 있음)
    );
    
    private static final Set<String> REQUIRED_FIELDS_FOR_IP = Set.of(
        "ip_address",             // IP 주소 (standard_fields에 있음)
        "client_ip",              // 클라이언트 IP (standard_fields에 있음)
        "remote_ip"               // 원격 IP (standard_fields에 있음)
    );
    
    private static final Set<String> REQUIRED_FIELDS_FOR_SESSION = Set.of(
        "session_id",             // 세션 ID (standard_fields에 있음)
        "login_dt",               // 로그인 시간 (standard_fields에 있음)
        "session_start_time"      // 세션 시작 시간 (standard_fields에 있음)
    );
    
    private static final Set<String> REQUIRED_FIELDS_FOR_AUTH = Set.of(
        "login_result",           // 로그인 결과 (standard_fields에 있음)
        "auth_status",            // 인증 상태 (standard_fields에 있음)
        "login_status"            // 로그인 상태 (standard_fields에 있음)
    );
    
    // 한국 국가 코드
    private static final String KOREA_COUNTRY_CODE = "KR";
    
    @Override
    public boolean supports(RuleDomain domain) {
        return domain == RuleDomain.LOGIN;
    }
    
    @Override
    public boolean evaluate(String groupKey, Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;
            
            RuleOperator operator = extractOperator(rule);
            Object conditionValue = rule.get("condition_value");
            
            // EventStream에서 해당 groupKey의 현재 로그인 데이터 조회
            Map<String, Object> eventData = getCurrentEventData(groupKey);
            
            return switch (operator) {
                case GREATER_THAN_OR_EQUALS -> evaluateFailureCountGreaterThanOrEquals(eventData, conditionValue, groupKey);
                case REPEATED_TIMES -> evaluateConsecutiveFailures(eventData, conditionValue, groupKey);
                case EQUALS -> evaluateLoginEquals(eventData, conditionValue);
                case NOT_EQUALS -> evaluateLoginNotEquals(eventData, conditionValue);
                case WITHIN_HOURS -> evaluateWithinHours(eventData, conditionValue);
                case NO_HISTORY -> evaluateNewIPLogin(eventData, groupKey);
                default -> {
                    log.error("지원하지 않는 연산자: {} (LOGIN 도메인)", operator);
                    throw new IllegalArgumentException("LOGIN 도메인에서 지원하지 않는 연산자: " + operator);
                }
            };
            
        } catch (Exception e) {
            log.error("LoginCondition 평가 중 오류 발생 - groupKey: {}, error: {}", groupKey, e.getMessage(), e);
            throw new RuntimeException("로그인 조건 평가 실패", e);
        }
    }
    
    /**
     * L01: 로그인 실패 횟수 조건 평가
     */
    private boolean evaluateFailureCountGreaterThanOrEquals(Map<String, Object> eventData, Object conditionValue, String groupKey) {
        validateRequiredFieldsForLogin(eventData);
        validateRequiredFieldsForAuth(eventData);
        
        int targetCount = Integer.parseInt(conditionValue.toString());
        String customerId = (String) eventData.get("customer_id");
        
        // EventStream에서 최근 24시간 내 로그인 실패 횟수 조회
        int failureCount = getLoginFailureCountInHours(customerId, 24);
        
        boolean result = failureCount >= targetCount;
        log.debug("로그인 실패 횟수 조건 평가: customer={}, count={} >= {} = {}", 
                 customerId, failureCount, targetCount, result);
        return result;
    }
    
    /**
     * L02: 연속 로그인 실패 조건 평가
     */
    private boolean evaluateConsecutiveFailures(Map<String, Object> eventData, Object conditionValue, String groupKey) {
        validateRequiredFieldsForLogin(eventData);
        validateRequiredFieldsForAuth(eventData);
        
        int targetConsecutiveCount = Integer.parseInt(conditionValue.toString());
        String customerId = (String) eventData.get("customer_id");
        
        // EventStream에서 최근 연속 로그인 실패 횟수 조회
        int consecutiveFailures = getConsecutiveLoginFailures(customerId);
        
        boolean result = consecutiveFailures >= targetConsecutiveCount;
        log.debug("연속 로그인 실패 조건 평가: customer={}, consecutive={} >= {} = {}", 
                 customerId, consecutiveFailures, targetConsecutiveCount, result);
        return result;
    }
    
    /**
     * L03, L09, L10: 특정 조건 평가
     */
    private boolean evaluateLoginEquals(Map<String, Object> eventData, Object conditionValue) {
        String conditionStr = conditionValue.toString();
        
        return switch (conditionStr) {
            case "MULTIPLE_ACCOUNTS_SAME_IP" -> evaluateMultipleAccountsSameIP(eventData);
            case "OVERSEAS_LOGIN" -> evaluateOverseasLogin(eventData);
            case "VPN_PROXY_LOGIN" -> evaluateVpnProxyLogin(eventData);
            case "ABNORMAL_TIME_LOGIN" -> evaluateAbnormalTimeLogin(eventData);
            case "HOLIDAY_WEEKEND_LOGIN" -> evaluateHolidayWeekendLogin(eventData);
            case "LONG_SESSION" -> evaluateLongSession(eventData);
            case "MULTIPLE_SESSIONS" -> evaluateMultipleSessions(eventData);
            default -> {
                log.warn("알 수 없는 로그인 조건: {}", conditionStr);
                yield false;
            }
        };
    }
    
    /**
     * 조건 부정 평가
     */
    private boolean evaluateLoginNotEquals(Map<String, Object> eventData, Object conditionValue) {
        return !evaluateLoginEquals(eventData, conditionValue);
    }
    
    /**
     * L09: 장시간 세션 유지 조건 평가
     */
    private boolean evaluateWithinHours(Map<String, Object> eventData, Object conditionValue) {
        validateRequiredFieldsForSession(eventData);
        
        int hours = Integer.parseInt(conditionValue.toString());
        LocalDateTime sessionStart = extractSessionStartTime(eventData);
        LocalDateTime currentTime = LocalDateTime.now();
        
        long sessionHours = java.time.Duration.between(sessionStart, currentTime).toHours();
        boolean result = sessionHours >= hours;
        
        log.debug("세션 지속시간 조건 평가: {}시간 >= {}시간 = {}", sessionHours, hours, result);
        return result;
    }
    
    /**
     * L04: 새로운 IP에서 로그인 조건 평가
     */
    private boolean evaluateNewIPLogin(Map<String, Object> eventData, String groupKey) {
        validateRequiredFieldsForIP(eventData);
        validateRequiredFieldsForLogin(eventData);
        
        String currentIP = extractIPAddress(eventData);
        String customerId = (String) eventData.get("customer_id");
        
        // EventStream에서 해당 고객의 IP 사용 이력 확인
        boolean hasIPHistory = hasCustomerIPHistory(customerId, currentIP);
        
        // 새로운 IP는 사용 이력이 없는 IP
        boolean result = !hasIPHistory;
        log.debug("새로운 IP 로그인 조건 평가: customer={}, ip={}, hasHistory={}, isNew={}", 
                 customerId, currentIP, hasIPHistory, result);
        return result;
    }
    
    // 개별 조건 평가 메서드들
    private boolean evaluateMultipleAccountsSameIP(Map<String, Object> eventData) {
        validateRequiredFieldsForIP(eventData);
        
        String currentIP = extractIPAddress(eventData);
        int accountCount = getAccountCountForIP(currentIP, 24); // 24시간 내
        
        boolean result = accountCount >= 10; // 10개 이상 계정
        log.debug("동일 IP 다수 계정 로그인 조건 평가: ip={}, accounts={} >= 10 = {}", 
                 currentIP, accountCount, result);
        return result;
    }
    
    private boolean evaluateOverseasLogin(Map<String, Object> eventData) {
        validateRequiredFieldsForIP(eventData);
        
        String countryCode = (String) eventData.getOrDefault("country_code", 
                                      eventData.get("ip_country"));
        
        boolean result = !KOREA_COUNTRY_CODE.equals(countryCode);
        log.debug("해외 로그인 조건 평가: country={}, isOverseas={}", countryCode, result);
        return result;
    }
    
    private boolean evaluateVpnProxyLogin(Map<String, Object> eventData) {
        String connectionType = (String) eventData.getOrDefault("connection_type", 
                                        eventData.get("network_type"));
        
        boolean result = "VPN".equals(connectionType) || "PROXY".equals(connectionType);
        log.debug("VPN/Proxy 로그인 조건 평가: type={}, isVpnProxy={}", connectionType, result);
        return result;
    }
    
    private boolean evaluateAbnormalTimeLogin(Map<String, Object> eventData) {
        validateRequiredFieldsForSession(eventData);
        
        LocalDateTime loginTime = extractSessionStartTime(eventData);
        int hour = loginTime.getHour();
        
        boolean result = hour >= 2 && hour < 5; // 새벽 2-5시
        log.debug("비정상 시간 로그인 조건 평가: time={}, hour={}, isAbnormal={}", 
                 loginTime, hour, result);
        return result;
    }
    
    private boolean evaluateHolidayWeekendLogin(Map<String, Object> eventData) {
        validateRequiredFieldsForSession(eventData);
        
        LocalDateTime loginTime = extractSessionStartTime(eventData);
        int dayOfWeek = loginTime.getDayOfWeek().getValue();
        
        boolean result = dayOfWeek == 6 || dayOfWeek == 7; // 토요일, 일요일
        // TODO: 공휴일 체크 추가 필요
        log.debug("휴일/주말 로그인 조건 평가: date={}, dayOfWeek={}, isHolidayWeekend={}", 
                 loginTime.toLocalDate(), dayOfWeek, result);
        return result;
    }
    
    private boolean evaluateLongSession(Map<String, Object> eventData) {
        return evaluateWithinHours(eventData, "12"); // 12시간 이상
    }
    
    private boolean evaluateMultipleSessions(Map<String, Object> eventData) {
        validateRequiredFieldsForLogin(eventData);
        
        String customerId = (String) eventData.get("customer_id");
        int activeSessionCount = getActiveSessionCount(customerId);
        
        boolean result = activeSessionCount >= 3; // 3개 이상 세션
        log.debug("다중 세션 조건 평가: customer={}, sessions={} >= 3 = {}", 
                 customerId, activeSessionCount, result);
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
     * EventData에서 IP 주소 추출
     */
    private String extractIPAddress(Map<String, Object> eventData) {
        if (eventData.containsKey("ip_address")) {
            return (String) eventData.get("ip_address");
        }
        if (eventData.containsKey("client_ip")) {
            return (String) eventData.get("client_ip");
        }
        if (eventData.containsKey("remote_ip")) {
            return (String) eventData.get("remote_ip");
        }
        throw new IllegalArgumentException("IP 주소를 추출할 수 있는 필드가 없습니다.");
    }
    
    /**
     * EventData에서 세션 시작 시간 추출
     */
    private LocalDateTime extractSessionStartTime(Map<String, Object> eventData) {
        String timeStr = null;
        if (eventData.containsKey("login_dt")) {
            timeStr = (String) eventData.get("login_dt");
        } else if (eventData.containsKey("session_start_time")) {
            timeStr = (String) eventData.get("session_start_time");
        }
        
        if (timeStr != null) {
            return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }
        
        throw new IllegalArgumentException("세션 시작 시간을 추출할 수 있는 필드가 없습니다.");
    }
    
    // EventStream 조회 메서드들 (임시 구현)
    private int getLoginFailureCountInHours(String customerId, int hours) {
        // TODO: EventStreamService를 통해 구현
        return 0;
    }
    
    private int getConsecutiveLoginFailures(String customerId) {
        // TODO: EventStreamService를 통해 구현
        return 0;
    }
    
    private int getAccountCountForIP(String ipAddress, int hours) {
        // TODO: EventStreamService를 통해 구현
        return 0;
    }
    
    private boolean hasCustomerIPHistory(String customerId, String ipAddress) {
        // TODO: EventStreamService를 통해 구현
        return false;
    }
    
    private int getActiveSessionCount(String customerId) {
        // TODO: EventStreamService를 통해 구현
        return 0;
    }
    
    // 필드 검증 메서드들
    private void validateRequiredFieldsForLogin(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_LOGIN.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("로그인 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_LOGIN));
        }
    }
    
    private void validateRequiredFieldsForIP(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_IP.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("IP 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_IP));
        }
    }
    
    private void validateRequiredFieldsForSession(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_SESSION.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("세션 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_SESSION));
        }
    }
    
    private void validateRequiredFieldsForAuth(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_AUTH.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("인증 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_AUTH));
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
        return "LOGIN_CONDITION";
    }
    
    @Override
    public String toHumanReadableString(Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;
            
            RuleOperator operator = extractOperator(rule);
            Object value = rule.get("condition_value");
            
            return switch (operator) {
                case GREATER_THAN_OR_EQUALS -> String.format("로그인 실패 %s회 이상", value);
                case REPEATED_TIMES -> String.format("연속 로그인 실패 %s회 이상", value);
                case WITHIN_HOURS -> String.format("세션 지속시간 %s시간 이상", value);
                case NO_HISTORY -> "새로운 IP에서 로그인";
                case EQUALS -> switch (value.toString()) {
                    case "MULTIPLE_ACCOUNTS_SAME_IP" -> "동일 IP에서 다수 계정 로그인";
                    case "OVERSEAS_LOGIN" -> "해외 IP에서 로그인";
                    case "VPN_PROXY_LOGIN" -> "VPN/Proxy를 통한 로그인";
                    case "ABNORMAL_TIME_LOGIN" -> "비정상적인 시간 로그인";
                    case "HOLIDAY_WEEKEND_LOGIN" -> "휴일/주말 로그인";
                    case "LONG_SESSION" -> "장시간 세션 유지";
                    case "MULTIPLE_SESSIONS" -> "동시 다중 세션";
                    default -> String.format("로그인 조건: %s", value);
                };
                default -> String.format("로그인 조건: %s %s", operator, value);
            };
            
        } catch (Exception e) {
            return "로그인 조건 (표시 오류)";
        }
    }
    
    @Override
    public boolean requiresHistoryData() {
        return true; // 로그인 조건은 대부분 이력 데이터가 필요
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
            log.error("로그인 조건 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }
}