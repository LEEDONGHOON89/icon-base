package com.itmasters.icon.api.metadata.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

/**
 * 데이터 소스 타입 메타데이터 응답 DTO
 * 생성일시: 2025-01-24 12:00:00
 * 수정일시: 2025-01-24 12:00:00
 */
@Getter
@Builder
@Schema(description = "데이터 소스 타입 메타데이터 응답")
public class DataSourceTypeMetadataResponse {
    
    @Schema(description = "타입 값", example = "DATABASE")
    private final String value;
    
    @Schema(description = "타입 라벨", example = "데이터베이스")
    private final String label;
    
    @Schema(description = "타입 설명", example = "관계형 데이터베이스")
    private final String description;
    
    @Schema(description = "아이콘 타입", example = "database")
    private final String iconType;
    
    @Schema(description = "데이터베이스 타입 여부", example = "true")
    private final boolean isDatabaseType;
    
    @Schema(description = "로그 타입 여부", example = "false")
    private final boolean isLogType;
    
    @Schema(description = "파일 기반 타입 여부", example = "false")
    private final boolean isFileBasedType;
    
    @Schema(description = "스트리밍 타입 여부", example = "false")
    private final boolean isStreamingType;
}