package com.itmasters.icon.api.common.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 필드 데이터 타입
 * - 데이터 변환 규칙을 내포하는 타입 정의
 */
@Getter
@RequiredArgsConstructor
public enum FieldDataType {
    
    STRING("STRING", "문자열", false),              // 변환 없음
    NUMBER("NUMBER", "숫자", false),                // 변환 없음
    DECIMAL("DECIMAL", "소수", false),
    DATETIME("DATETIME", "날짜시간", true),         // 포맷 변환 (공백→T)
    DATE("DATE", "날짜", true),                     // 날짜만 추출
    BOOLEAN("BOOLEAN", "불린", true),               // Y/N → true/false
    ;
    
    private final String code;
    private final String description;
    private final boolean requiresTransformation;  // 변환 필요 여부
    
    /**
     * 변환이 필요한 타입인지 확인
     */
    public boolean needsTransformation() {
        return requiresTransformation;
    }
    
    /**
     * 코드로 enum 찾기
     */
    public static FieldDataType fromCode(String code) {
        if (code == null) return STRING;
        
        for (FieldDataType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return STRING;  // 기본값
    }
}