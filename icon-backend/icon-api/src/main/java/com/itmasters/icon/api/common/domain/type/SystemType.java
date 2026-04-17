package com.itmasters.icon.api.common.domain.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 시스템 타입 Enum - 각 Rule이 어떤 솔루션에 적용되는지 구분
 * 솔루션별로 Rule을 작성하여 여러 회사에서 재사용 가능
 */
@Getter
@RequiredArgsConstructor
public enum SystemType {
    PINNACLE("피나클 코어뱅킹 시스템"),
    TMAX_CORE("티맥스 코어뱅킹 시스템"),
    IBM_EBANKING("IBM 인터넷뱅킹 솔루션"),
    CUSTOM_SYSTEM("자체개발 시스템"),
    
    // 레거시 호환성을 위한 기본값들 (추후 제거 예정)
    SYSTEM_A("시스템 A"),
    SYSTEM_B("시스템 B"), 
    SYSTEM_C("시스템 C");
    
    private final String description;
}