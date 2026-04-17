package com.itmasters.icon.api.common.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 규칙 처리 액션 타입
 */
@Getter
@RequiredArgsConstructor
public enum ActionType {
    BLOCK("차단"),
    ALERT("경고"),
    REVIEW("검토"),
    LOG("로그");
    
    private final String description;
}