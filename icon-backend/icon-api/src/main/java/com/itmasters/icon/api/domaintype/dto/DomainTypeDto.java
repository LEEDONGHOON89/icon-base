package com.itmasters.icon.api.domaintype.dto;

import com.itmasters.icon.engine.adapter.out.persistence.entity.DomainTypeEntity;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 도메인 타입 DTO
 */
public class DomainTypeDto {

    /**
     * 도메인 타입 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        /**
         * 도메인 타입 ID (예: CUSTOMER, ACCOUNT)
         */
        private String domainTypeId;

        /**
         * 도메인 이름 (예: 고객, 계좌)
         */
        private String domainName;

        /**
         * 도메인 설명
         */
        private String description;

        /**
         * 아이콘 이름
         */
        private String icon;

        /**
         * 색상 코드
         */
        private String color;

        /**
         * 활성화 여부
         */
        private Boolean isActive;

        /**
         * 표시 순서
         */
        private Integer displayOrder;

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
        public static Response from(DomainTypeEntity entity) {
            return Response.builder()
                    .domainTypeId(entity.getDomainTypeId())
                    .domainName(entity.getDomainName())
                    .description(entity.getDescription())
                    .icon(entity.getIcon())
                    .color(entity.getColor())
                    .isActive(entity.getIsActive())
                    .displayOrder(entity.getDisplayOrder())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 도메인 타입 생성 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        /**
         * 도메인 타입 ID (예: CUSTOMER, ACCOUNT)
         * 영문 대문자, 숫자, 언더스코어만 허용
         */
        private String domainTypeId;

        /**
         * 도메인 이름 (예: 고객, 계좌)
         */
        private String domainName;

        /**
         * 도메인 설명
         */
        private String description;

        /**
         * 아이콘 이름
         */
        private String icon;

        /**
         * 색상 코드
         */
        private String color;

        /**
         * 표시 순서
         */
        private Integer displayOrder;
    }

    /**
     * 도메인 타입 수정 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        /**
         * 도메인 이름
         */
        private String domainName;

        /**
         * 도메인 설명
         */
        private String description;

        /**
         * 아이콘 이름
         */
        private String icon;

        /**
         * 색상 코드
         */
        private String color;

        /**
         * 활성화 여부
         */
        private Boolean isActive;

        /**
         * 표시 순서
         */
        private Integer displayOrder;
    }
}
