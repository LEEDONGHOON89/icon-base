package com.itmasters.icon.api.metadata.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 위험 레벨 메타데이터 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskLevelMetadataResponse {
    private String value;       // ID (예: MONITOR, REVIEW, BLOCK)
    private int levelCode;      // 레벨 코드 (예: 10, 20, 30, 40)
    private String label;       // 표시명 (예: 모니터링, 심사, 차단)
    private String actionType;  // 액션 타입 (예: ALERT, HOLD, BLOCK)
    private String description; // 설명
}
