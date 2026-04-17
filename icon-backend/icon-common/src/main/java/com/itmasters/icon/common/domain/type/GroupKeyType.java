package com.itmasters.icon.common.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 프로파일 그룹 키 타입
 * 데이터를 그룹핑하는 키의 유형을 정의
 */
@Getter
@RequiredArgsConstructor
public enum GroupKeyType {
    
    SINGLE("단일 표준 필드", "하나의 표준 필드를 키로 사용"),
    COMPOSITE("복합 표준 필드", "여러 표준 필드를 조합하여 키로 사용"),
    CUSTOM("커스텀 키", "표준 필드와 무관한 사용자 정의 키");
    
    private final String displayName;
    private final String description;
    
    /**
     * 키 값을 분석하여 타입을 자동 판별
     * @param groupKey 그룹 키 값
     * @return 추정되는 키 타입
     */
    public static GroupKeyType inferType(String groupKey) {
        if (groupKey == null || groupKey.isEmpty()) {
            return null;
        }
        
        // 콤마가 포함되어 있으면 복합키로 판단
        if (groupKey.contains(",")) {
            return COMPOSITE;
        }
        
        // 특수문자나 공백이 포함되어 있으면 커스텀키로 판단
        if (groupKey.contains(" ") || groupKey.contains("#") || groupKey.contains("@")) {
            return CUSTOM;
        }
        
        // 기본적으로 단일키로 판단
        return SINGLE;
    }
}