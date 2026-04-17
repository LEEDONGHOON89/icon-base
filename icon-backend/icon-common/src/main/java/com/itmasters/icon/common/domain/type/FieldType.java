package com.itmasters.icon.common.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 룰 필드의 데이터 타입
 */
@Getter
@RequiredArgsConstructor
public enum FieldType {
    NUMBER("number", "숫자"),
//    NUMBER_LIST("number_list", "숫자 리스트(','로 구분된 숫자)"),
    STRING("string", "문자열"),
    BOOLEAN("boolean", "불린"),
    TIME("time", "시간"),
    ANY("any", "모든 타입");
    
    private final String code;
    private final String description;
}