package com.itmasters.icon.api.common.dto;

import com.itmasters.icon.api.common.domain.type.SystemType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통 코드를 객체로 표현하기 위한 DTO
 * SystemType, UserRole 등 다양한 Enum 타입에서 재사용 가능
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CodeTypeResult {
    private String code;        // Enum의 name() (예: "PINNACLE")
    private String label;       // Enum의 description (예: "피나클 코어뱅킹 시스템")
    
    public static CodeTypeResult from(SystemType systemType) {
        return new CodeTypeResult(
            systemType.name(),
            systemType.getDescription()
        );
    }
    
    // 다른 Enum 타입들을 위한 일반적인 생성 메서드들을 추가할 수 있음
    // 예: public static CodeTypeResult from(UserRole userRole) { ... }
}