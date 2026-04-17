package com.itmasters.icon.common.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 필드 데이터 타입 enum
 * 데이터 소스 스키마의 필드가 가질 수 있는 데이터 타입을 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum FieldDataType {
    
    UNKNOWN("미지정", "알수없는 상태"),
    STRING("문자열", "텍스트 데이터"),
    NUMBER("숫자", "정수 또는 실수"),
    DECIMAL("소수", "십진수 (고정소수점)"),
    BOOLEAN("불리언", "참/거짓 값"),
    DATE("날짜", "날짜만 (YYYY-MM-DD)"),
    DATETIME("날짜시간", "날짜와 시간 (YYYY-MM-DD HH:mm:ss)"),
    TIME("시간", "시간만 (HH:mm:ss)"),
    JSON("JSON", "JSON 형식의 구조화된 데이터"),
    ARRAY("배열", "동일한 타입의 값들의 목록"),
    OBJECT("객체", "키-값 쌍의 구조화된 데이터"),
    ;
    
    private final String displayName;
    private final String description;
    
    /**
     * 문자열로부터 FieldDataType을 찾습니다.
     * 일반적인 데이터베이스 타입들도 매핑합니다.
     */
    public static FieldDataType fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        
        // 정확한 enum 이름 매칭 시도
        for (FieldDataType type : FieldDataType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        
        // 일반적인 데이터베이스 타입 매핑
        String upperValue = value.toUpperCase();
        switch (upperValue) {
            // 문자열 타입들
            case "VARCHAR":
            case "TEXT":
            case "CHAR":
            case "CHARACTER":
            case "NVARCHAR":
            case "NTEXT":
            case "CLOB":
                return STRING;
                
            // 숫자 타입들
            case "INTEGER":
            case "INT":
            case "BIGINT":
            case "SMALLINT":
            case "TINYINT":
            case "DECIMAL":
            case "NUMERIC":
            case "FLOAT":
            case "DOUBLE":
            case "REAL":
                return NUMBER;
                
            // 불리언 타입들
            case "BOOLEAN":
            case "BOOL":
            case "BIT":
                return BOOLEAN;
                
            // 날짜/시간 타입들
            case "DATE":
                return DATE;
            case "DATETIME":
            case "TIMESTAMP":
            case "TIMESTAMPTZ":
                return DATETIME;
            case "TIME":
            case "TIMETZ":
                return TIME;
                
            // JSON 타입들
            case "JSON":
            case "JSONB":
                return JSON;
                
            // 기타
            default:
                // 알 수 없는 타입은 UNKNOWN으로 처리 (예외 발생시키지 않음)
                return UNKNOWN;
        }
    }
}