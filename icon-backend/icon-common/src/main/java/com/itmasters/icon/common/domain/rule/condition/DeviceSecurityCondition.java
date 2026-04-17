package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

/**
 * 디바이스보안 관련 룰 조건 처리 (DEVICE_SECURITY 도메인 전용)
 * 
 * 지원 조건:
 * - D01: 비대면 금융거래 허용 디바이스 (허용기기)
 * - D02: 비대면 금융거래 허용 디바이스 (비허용기기)
 * - D03: 접속 대역폭이 광대역인 디바이스
 * - D04: 접속 대역폭이 저대역인 디바이스
 * - D05: 기기 변경 후 첫 거래
 * - D06: 새 디바이스로부터의 접속
 * - D07: 루팅/탈옥 디바이스
 * - D08: VPN/Proxy 사용 디바이스
 */
@Slf4j
public class DeviceSecurityCondition extends RuleConditionV2 {
    
    // standard_fields 테이블 기반 필수 필드들
    private static final Set<String> REQUIRED_FIELDS_FOR_DEVICE = Set.of(
        "device_id",              // 디바이스 ID (standard_fields에 있음)
        "device_type",            // 디바이스 타입 (standard_fields에 있음)
        "device_os",              // 운영체제 (standard_fields에 있음)
        "device_model"            // 디바이스 모델 (standard_fields에 있음)
    );
    
    private static final Set<String> REQUIRED_FIELDS_FOR_NETWORK = Set.of(
        "ip_address",             // IP 주소 (standard_fields에 있음)
        "network_type",           // 네트워크 타입 (standard_fields에 있음)
        "bandwidth",              // 대역폭 (standard_fields에 있음)
        "connection_type"         // 연결 타입 (standard_fields에 있음)
    );
    
    private static final Set<String> REQUIRED_FIELDS_FOR_HISTORY = Set.of(
        "device_id",              // 디바이스 ID
        "customer_id",            // 고객 ID (standard_fields에 있음)
        "transaction_date",       // 거래일 (standard_fields에 있음)
        "first_use_date"          // 최초 사용일 (standard_fields에 있음)
    );
    
    // 허용/비허용 디바이스 리스트 (실제로는 별도 테이블에서 관리)
    private static final Set<String> ALLOWED_DEVICE_TYPES = Set.of(
        "SMARTPHONE", "TABLET", "PC", "LAPTOP"
    );
    
    // 광대역/저대역 구분 기준 (Mbps)
    private static final int HIGH_BANDWIDTH_THRESHOLD = 10;
    
    @Override
    public boolean supports(RuleDomain domain) {
        return domain == RuleDomain.DEVICE_SECURITY;
    }
    
    @Override
    public boolean evaluate(String groupKey, Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;
            
            RuleOperator operator = extractOperator(rule);
            Object conditionValue = rule.get("condition_value");
            
            // EventStream에서 해당 groupKey의 현재 및 이력 데이터 조회
            Map<String, Object> eventData = getCurrentEventData(groupKey);
            
            return switch (operator) {
                case EQUALS -> evaluateDeviceTypeEquals(eventData, conditionValue);
                case NOT_EQUALS -> evaluateDeviceTypeNotEquals(eventData, conditionValue);
                case GREATER_THAN_OR_EQUALS -> evaluateBandwidthGreaterThan(eventData, conditionValue);
                case LESS_THAN_OR_EQUALS -> evaluateBandwidthLessThan(eventData, conditionValue);
                case SAME_AS_FIELD -> evaluateDeviceChanged(eventData, groupKey);
                case NO_HISTORY_WITHIN_MONTHS -> evaluateNewDevice(groupKey, conditionValue);
                default -> {
                    log.error("지원하지 않는 연산자: {} (DEVICE_SECURITY 도메인)", operator);
                    throw new IllegalArgumentException("DEVICE_SECURITY 도메인에서 지원하지 않는 연산자: " + operator);
                }
            };
            
        } catch (Exception e) {
            log.error("DeviceSecurityCondition 평가 중 오류 발생 - groupKey: {}, error: {}", groupKey, e.getMessage(), e);
            throw new RuntimeException("디바이스보안 조건 평가 실패", e);
        }
    }
    
    /**
     * D01: 허용기기 조건 평가
     */
    private boolean evaluateDeviceTypeEquals(Map<String, Object> eventData, Object conditionValue) {
        validateRequiredFieldsForDevice(eventData);
        
        String deviceType = (String) eventData.get("device_type");
        String expectedType = conditionValue.toString();
        
        boolean isAllowed = ALLOWED_DEVICE_TYPES.contains(deviceType.toUpperCase()) 
                           && deviceType.equalsIgnoreCase(expectedType);
        
        log.debug("허용기기 조건 평가: {} equals {} = {}", deviceType, expectedType, isAllowed);
        return isAllowed;
    }
    
    /**
     * D02: 비허용기기 조건 평가
     */
    private boolean evaluateDeviceTypeNotEquals(Map<String, Object> eventData, Object conditionValue) {
        validateRequiredFieldsForDevice(eventData);
        
        String deviceType = (String) eventData.get("device_type");
        String forbiddenType = conditionValue.toString();
        
        boolean isForbidden = !ALLOWED_DEVICE_TYPES.contains(deviceType.toUpperCase()) 
                             || deviceType.equalsIgnoreCase(forbiddenType);
        
        log.debug("비허용기기 조건 평가: {} not equals {} = {}", deviceType, forbiddenType, isForbidden);
        return isForbidden;
    }
    
    /**
     * D03: 광대역 디바이스 조건 평가
     */
    private boolean evaluateBandwidthGreaterThan(Map<String, Object> eventData, Object conditionValue) {
        validateRequiredFieldsForNetwork(eventData);
        
        int bandwidth = Integer.parseInt(eventData.get("bandwidth").toString());
        int threshold = Integer.parseInt(conditionValue.toString());
        
        boolean isHighBandwidth = bandwidth > threshold;
        log.debug("광대역 조건 평가: {} > {} = {}", bandwidth, threshold, isHighBandwidth);
        return isHighBandwidth;
    }
    
    /**
     * D04: 저대역 디바이스 조건 평가
     */
    private boolean evaluateBandwidthLessThan(Map<String, Object> eventData, Object conditionValue) {
        validateRequiredFieldsForNetwork(eventData);
        
        int bandwidth = Integer.parseInt(eventData.get("bandwidth").toString());
        int threshold = Integer.parseInt(conditionValue.toString());
        
        boolean isLowBandwidth = bandwidth < threshold;
        log.debug("저대역 조건 평가: {} < {} = {}", bandwidth, threshold, isLowBandwidth);
        return isLowBandwidth;
    }
    
    /**
     * D05: 기기 변경 후 첫 거래 조건 평가
     */
    private boolean evaluateDeviceChanged(Map<String, Object> eventData, String groupKey) {
        validateRequiredFieldsForDevice(eventData);
        
        String currentDeviceId = (String) eventData.get("device_id");
        
        // EventStream에서 이전 거래의 디바이스 ID 조회
        String previousDeviceId = getPreviousDeviceId(groupKey);
        
        boolean isDeviceChanged = previousDeviceId != null && !currentDeviceId.equals(previousDeviceId);
        log.debug("기기 변경 조건 평가: current={}, previous={}, changed={}", 
                 currentDeviceId, previousDeviceId, isDeviceChanged);
        return isDeviceChanged;
    }
    
    /**
     * D06: 새 디바이스 조건 평가
     */
    private boolean evaluateNewDevice(String groupKey, Object conditionValue) {
        // EventStream에서 해당 groupKey의 디바이스 사용 이력 확인
        boolean hasDeviceHistory = hasDeviceUsageHistory(groupKey);
        
        // 새 디바이스는 사용 이력이 없는 디바이스
        boolean isNewDevice = !hasDeviceHistory;
        log.debug("새 디바이스 조건 평가: groupKey={}, hasHistory={}, isNew={}", 
                 groupKey, hasDeviceHistory, isNewDevice);
        return isNewDevice;
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
     * EventStream에서 이전 거래의 디바이스 ID 조회 (임시 구현)
     */
    private String getPreviousDeviceId(String groupKey) {
        // TODO: 실제로는 EventStreamService를 통해 이전 거래 조회
        // SELECT device_id FROM event_stream 
        // WHERE group_key = ? 
        //   AND transaction_date < CURRENT_TIMESTAMP
        // ORDER BY transaction_date DESC LIMIT 1
        return null;
    }
    
    /**
     * EventStream에서 디바이스 사용 이력 확인 (임시 구현)
     */
    private boolean hasDeviceUsageHistory(String groupKey) {
        // TODO: 실제로는 EventStreamService를 통해 이력 조회
        // SELECT COUNT(*) FROM event_stream 
        // WHERE group_key = ? AND device_id IS NOT NULL
        return false;
    }
    
    /**
     * 디바이스 관련 필수 필드 검증
     */
    private void validateRequiredFieldsForDevice(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_DEVICE.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("디바이스 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_DEVICE));
        }
    }
    
    /**
     * 네트워크 관련 필수 필드 검증
     */
    private void validateRequiredFieldsForNetwork(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_NETWORK.stream()
            .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);
        
        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                String.format("네트워크 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s", 
                             REQUIRED_FIELDS_FOR_NETWORK));
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
        return "DEVICE_SECURITY_CONDITION";
    }
    
    @Override
    public String toHumanReadableString(Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;
            
            RuleOperator operator = extractOperator(rule);
            Object value = rule.get("condition_value");
            
            return switch (operator) {
                case EQUALS -> String.format("허용기기: %s", value);
                case NOT_EQUALS -> String.format("비허용기기: %s", value);
                case GREATER_THAN_OR_EQUALS -> String.format("대역폭 %sMbps 이상", value);
                case LESS_THAN_OR_EQUALS -> String.format("대역폭 %sMbps 이하", value);
                case SAME_AS_FIELD -> "기기 변경 후 첫 거래";
                case NO_HISTORY_WITHIN_MONTHS -> "새 디바이스 접속";
                default -> String.format("디바이스 조건: %s %s", operator, value);
            };
            
        } catch (Exception e) {
            return "디바이스 조건 (표시 오류)";
        }
    }
    
    @Override
    public boolean requiresHistoryData() {
        return true; // 디바이스 조건은 대부분 이력 데이터가 필요
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
            log.error("디바이스 조건 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }
}