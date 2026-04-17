package com.itmasters.icon.api.common.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 논리 연산자 타입
 */
@Getter
@RequiredArgsConstructor
public enum LogicalOperatorType {
    AND("그리고"),
    OR("또는");
    
    private final String description;
}