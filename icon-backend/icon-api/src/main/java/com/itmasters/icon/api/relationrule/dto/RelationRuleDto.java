package com.itmasters.icon.api.relationrule.dto;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RelationRuleEntity;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 도메인 관계 규칙 DTO
 */
public class RelationRuleDto {

    /**
     * 도메인 관계 규칙 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        /**
         * 규칙 ID
         */
        private Long ruleId;

        /**
         * 데이터소스 ID
         */
        private String dataSourceId;

        /**
         * From 도메인 타입 (CUSTOMER, ACCOUNT 등)
         */
        private String fromEntityType;

        /**
         * From ID 필드명 (이벤트 데이터의 필드명)
         */
        private String fromIdField;

        /**
         * 관계 타입 (OWNS, USES, AUTHENTICATES, ACCESSES)
         */
        private String relationType;

        /**
         * To 도메인 타입
         */
        private String toEntityType;

        /**
         * To ID 필드명 (이벤트 데이터의 필드명)
         */
        private String toIdField;

        /**
         * 규칙 설명
         */
        private String description;

        /**
         * 활성화 여부
         */
        private Boolean isActive;

        /**
         * 생성 일시
         */
        private LocalDateTime createdAt;

        /**
         * 수정 일시
         */
        private LocalDateTime updatedAt;

        /**
         * Entity를 DTO로 변환
         */
        public static Response from(RelationRuleEntity entity) {
            return Response.builder()
                    .ruleId(entity.getRuleId())
                    .dataSourceId(entity.getDataSourceId())
                    .fromEntityType(entity.getFromEntityType())
                    .fromIdField(entity.getFromIdField())
                    .relationType(entity.getRelationType())
                    .toEntityType(entity.getToEntityType())
                    .toIdField(entity.getToIdField())
                    .description(entity.getDescription())
                    .isActive(entity.getIsActive())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 도메인 관계 규칙 생성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        /**
         * 데이터소스 ID
         */
        private String dataSourceId;

        /**
         * From 도메인 타입
         */
        private String fromEntityType;

        /**
         * From ID 필드명
         */
        private String fromIdField;

        /**
         * 관계 타입 (OWNS, USES, AUTHENTICATES, ACCESSES)
         */
        private String relationType;

        /**
         * To 도메인 타입
         */
        private String toEntityType;

        /**
         * To ID 필드명
         */
        private String toIdField;

        /**
         * 규칙 설명
         */
        private String description;
    }

    /**
     * 도메인 관계 규칙 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        /**
         * 데이터소스 ID
         */
        private String dataSourceId;

        /**
         * From 도메인 타입
         */
        private String fromEntityType;

        /**
         * From ID 필드명
         */
        private String fromIdField;

        /**
         * 관계 타입
         */
        private String relationType;

        /**
         * To 도메인 타입
         */
        private String toEntityType;

        /**
         * To ID 필드명
         */
        private String toIdField;

        /**
         * 규칙 설명
         */
        private String description;

        /**
         * 활성화 여부
         */
        private Boolean isActive;
    }
}
