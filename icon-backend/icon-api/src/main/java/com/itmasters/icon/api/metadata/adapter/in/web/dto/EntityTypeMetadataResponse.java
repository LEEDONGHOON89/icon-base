package com.itmasters.icon.api.metadata.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 엔티티 타입 메타데이터 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityTypeMetadataResponse {
    private String value;       // ID (예: CUSTOMER, ACCOUNT, AUTHENTICATION)
    private String label;       // 표시명 (예: 고객, 계좌, 인증)
    private String description; // 설명
}
