package com.itmasters.icon.common.domain.rule;

import com.itmasters.icon.common.domain.type.FieldType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 룰 연산자의 파라미터 정의
 * 복수 파라미터를 가지는 연산자(예: BETWEEN, SUM_WITHIN)의 파라미터 구조 정의
 */
@Getter
@RequiredArgsConstructor
public class ParameterDefinition {
    
    private final String name;           // 파라미터 이름 (예: "시간윈도우", "임계값")
    private final FieldType type;        // 파라미터 타입 (NUMBER, STRING, TIME 등)
    private final boolean required;      // 필수 여부
    private final String description;    // 파라미터 설명
    
    /**
     * 필수 파라미터 생성
     */
    public static ParameterDefinition required(String name, FieldType type, String description) {
        return new ParameterDefinition(name, type, true, description);
    }
    
    /**
     * 선택적 파라미터 생성
     */
    public static ParameterDefinition optional(String name, FieldType type, String description) {
        return new ParameterDefinition(name, type, false, description);
    }
    
    /**
     * 파라미터 값 검증
     */
    public boolean validateValue(String value) {
        if (required && (value == null || value.trim().isEmpty())) {
            return false;
        }
        
        if (value == null || value.trim().isEmpty()) {
            return true; // 선택적 파라미터는 빈 값 허용
        }
        
        return validateByType(value.trim());
    }
    
    /**
     * 타입별 값 검증
     */
    private boolean validateByType(String value) {
        switch (type) {
            case NUMBER:
                try {
                    Double.parseDouble(value);
                    return true;
                } catch (NumberFormatException e) {
                    return false;
                }
                
            case TIME:
                // 시간 형식 검증 (예: "10", "HH:mm", "24h", "30m" 등)
                return value.matches("^(\\d+[hmsd]?|\\d{1,2}:\\d{2})$");
                
            case STRING:
                return true; // 문자열은 항상 허용
                
            case BOOLEAN:
                return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
                
            case ANY:
            default:
                return true; // ANY 타입은 모든 값 허용
        }
    }
    
    /**
     * 파라미터 형식 설명 반환
     */
    public String getFormatDescription() {
        String baseDesc = description;
        
        switch (type) {
            case NUMBER:
                return baseDesc + " (숫자)";
            case TIME:
                return baseDesc + " (시간: 숫자, HH:mm, 또는 24h/30m 형식)";
            case STRING:
                return baseDesc + " (문자열)";
            case BOOLEAN:
                return baseDesc + " (true/false)";
            case ANY:
            default:
                return baseDesc;
        }
    }
}