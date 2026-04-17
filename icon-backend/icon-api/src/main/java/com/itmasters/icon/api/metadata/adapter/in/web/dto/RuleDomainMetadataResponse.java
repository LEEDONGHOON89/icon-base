package com.itmasters.icon.api.metadata.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleDomainMetadataResponse {
    private String value;        // enum name
    private String label;        // 한글명
    private String description;  // 설명
    private String fieldDatetime; // 해당 도메인의 기준 시간 필드
}

