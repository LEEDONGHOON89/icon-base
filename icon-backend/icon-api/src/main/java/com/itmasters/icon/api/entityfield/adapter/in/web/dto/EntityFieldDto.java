package com.itmasters.icon.api.entityfield.adapter.in.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * EntityField 관련 DTO
 */
public class EntityFieldDto {

    /**
     * EntityField 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private String entityFieldId;
        private String displayName;
        private String dataType;
        private String description;
        private Boolean isActive;
    }
}
