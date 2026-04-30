package com.itmasters.icon.api.datasourceschema.adapter.in.web;

import com.itmasters.icon.common.domain.type.FieldDataType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 데이터소스 원본 스키마 DTO
 */
public class DataSourceOriginalSchemaDto {

    /**
     * 원본 스키마 생성 요청
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    @Schema(description = "데이터소스 원본 스키마 생성 요청")
    public static class CreateRequest {
        
        @NotBlank(message = "필드명은 필수입니다")
        @Schema(description = "필드명", example = "user_id")
        private String fieldName;
        
        @NotNull(message = "데이터 타입은 필수입니다")
        @Schema(description = "데이터 타입")
        private FieldDataType dataType;
        
        
        @Schema(description = "필수 여부", example = "true")
        private Boolean isRequired;
        
        @Schema(description = "기본값", example = "")
        private String defaultValue;
        
        @Schema(description = "설명", example = "고유 사용자 식별자")
        private String description;
        
        @Schema(description = "필드 순서", example = "1")
        private Integer fieldOrder;
    }

    /**
     * 원본 스키마 수정 요청
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    @Schema(description = "데이터소스 원본 스키마 수정 요청")
    public static class UpdateRequest {
        
        @Schema(description = "필드명", example = "user_id")
        private String fieldName;
        
        @Schema(description = "데이터 타입")
        private FieldDataType dataType;
        
        @Schema(description = "필수 여부", example = "true")
        private Boolean isRequired;
        
        @Schema(description = "기본값", example = "")
        private String defaultValue;
        
        @Schema(description = "설명", example = "고유 사용자 식별자")
        private String description;
        
        @Schema(description = "필드 순서", example = "1")
        private Integer fieldOrder;
        
        @Schema(description = "활성화 여부", example = "true")
        private Boolean isActive;
    }

    /**
     * 원본 스키마 일괄 등록 요청
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    @Schema(description = "데이터소스 원본 스키마 일괄 등록 요청")
    public static class BulkCreateRequest {
        private String dataSourceId;
        @NotNull(message = "스키마 목록은 필수입니다")
        @Schema(description = "등록할 스키마 목록")
        private List<CreateRequest> schemas;
    }

    /**
     * 원본 스키마 활성화 상태 변경 요청
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    @Schema(description = "데이터소스 원본 스키마 활성화 상태 변경 요청")
    public static class UpdateActiveRequest {
        
        @NotNull(message = "활성화 상태는 필수입니다")
        @Schema(description = "활성화 여부", example = "true")
        private boolean isActive;
    }

    /**
     * 원본 스키마 일괄 수정 요청
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    @Schema(description = "데이터소스 원본 스키마 일괄 수정 요청")
    public static class BulkUpdateRequest {
        
        @NotNull(message = "스키마 목록은 필수입니다")
        @Schema(description = "수정할 스키마 목록")
        private List<BulkUpdateItem> schemas;
        
        @Getter
        @NoArgsConstructor(access = AccessLevel.PROTECTED)
        @AllArgsConstructor
        @Builder
        @Schema(description = "일괄 수정할 스키마 정보")
        public static class BulkUpdateItem {
            
            @Schema(description = "스키마 ID (비어있으면 새 필드 생성)", example = "schema123")
            private String schemaId;
            
            @Schema(description = "필드명", example = "user_id")
            private String fieldName;
            
            @Schema(description = "데이터 타입")
            private FieldDataType dataType;
            
            @Schema(description = "필수 여부", example = "true")
            private Boolean isRequired;
            
            @Schema(description = "기본값", example = "")
            private String defaultValue;
            
            @Schema(description = "설명", example = "고유 사용자 식별자")
            private String description;
            
            @Schema(description = "필드 순서", example = "1")
            private Integer fieldOrder;
            
            @Schema(description = "활성화 여부", example = "true")
            private Boolean isActive;
            
            @Schema(description = "표준 필드 ID", example = "sf123")
            private String standardFieldId;

        }
    }

    /**
     * 원본 스키마 응답
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    @Schema(description = "데이터소스 원본 스키마 응답")
    public static class Response {
        
        @Schema(description = "스키마 ID", example = "schema123")
        private String schemaId;
        
        @Schema(description = "데이터소스 ID", example = "ds123")
        private String dataSourceId;
        
        @Schema(description = "데이터소스명", example = "사용자 데이터베이스")
        private String dataSourceName;
        
        @Schema(description = "필드명", example = "user_id")
        private String fieldName;
        
        @Schema(description = "데이터 타입")
        private FieldDataType dataType;
        
        
        @Schema(description = "필수 여부", example = "true")
        private Boolean isRequired;
        
        @Schema(description = "기본값", example = "")
        private String defaultValue;
        
        @Schema(description = "설명", example = "고유 사용자 식별자")
        private String description;
        
        @Schema(description = "필드 순서", example = "1")
        private Integer fieldOrder;
        
        @Schema(description = "활성 여부", example = "true")
        private Boolean isActive;
        
        @Schema(description = "표준 필드 ID", example = "sf123")
        private String standardFieldId;
        
        @Schema(description = "표준 필드명", example = "userId")
        private String standardFieldName;
    }

    /**
     * 스키마 탐지 결과 응답
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    @Schema(description = "스키마 탐지 결과 응답")
    public static class DetectionResponse {
        
        @Schema(description = "데이터소스 ID", example = "ds123")
        private String dataSourceId;
        
        @Schema(description = "탐지된 스키마 개수", example = "5")
        private int detectedCount;
        
        @Schema(description = "탐지된 스키마 목록")
        private List<Response> schemas;
        
        @Schema(description = "탐지 완료 시간", example = "2025-01-15T10:30:00")
        private LocalDateTime detectedAt;
    }

    /**
     * 스키마 통계 응답
     */
    @Getter
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Builder
    @Schema(description = "스키마 통계 응답")
    public static class StatisticsResponse {
        
        @Schema(description = "전체 스키마 개수", example = "10")
        private long totalCount;
        
        @Schema(description = "활성 스키마 개수", example = "8")
        private long activeCount;
        
        @Schema(description = "비활성 스키마 개수", example = "2")
        private long inactiveCount;
    }

    /**
     * 표준 필드 매핑 업데이트 요청
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "표준 필드 매핑 업데이트 요청")
    public static class StandardFieldMappingRequest {

        @Schema(description = "표준 필드 ID (null이면 매핑 해제)", example = "sf123")
        private String standardFieldId;

        @Schema(description = "활성화 여부", example = "true")
        private Boolean isActive;
    }
}
