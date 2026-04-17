package com.itmasters.icon.api.common.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 규칙 카테고리 정의
 */
@Getter
@RequiredArgsConstructor
public enum RuleCategory {
    SECURITY("보안"),
    COMPLIANCE("규정 준수"),
    MONITORING("모니터링"),
    BACKUP("백업"),
    NETWORK("네트워크"),
    CUSTOM("사용자 정의"),
    LOCATION_BASED("위치 기반"),
    TRANSFER_PATTERN("이체 패턴"),
    DEVICE_BASED("디바이스 기반"),
    AUTHENTICATION("인증"),
    BLACKLIST("블랙리스트"),
    E_COMMERCE("전자상거래");
    
    private final String label;
}