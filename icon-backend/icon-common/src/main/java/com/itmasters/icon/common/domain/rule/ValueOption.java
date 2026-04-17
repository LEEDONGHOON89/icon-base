package com.itmasters.icon.common.domain.rule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 필드의 선택 가능한 값 옵션
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ValueOption {
    private String value;
    private String label;
    
    public static ValueOption of(String value, String label) {
        return ValueOption.builder()
            .value(value)
            .label(label)
            .build();
    }
}