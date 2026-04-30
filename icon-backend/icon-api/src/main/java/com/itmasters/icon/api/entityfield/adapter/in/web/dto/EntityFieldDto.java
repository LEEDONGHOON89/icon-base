package com.itmasters.icon.api.entityfield.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    // [2026-04-24] CRUD API 추가 — 등록 요청 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "필드 ID는 필수입니다.")
        @Pattern(regexp = "^[a-z][a-z0-9_]{0,99}$",
                 message = "필드 ID는 소문자/숫자/언더스코어만 허용하며 소문자로 시작해야 합니다.")
        private String entityFieldId;

        @NotBlank(message = "표시명은 필수입니다.")
        private String displayName;

        @NotBlank(message = "데이터 타입은 필수입니다.")
        @Pattern(regexp = "^(STRING|NUMBER|BOOLEAN|DATE|DATETIME|TIMESTAMP|ARRAY|OBJECT)$",
                 message = "유효하지 않은 데이터 타입입니다.")
        private String dataType;

        private String description;
    }

    // [2026-04-24] CRUD API 추가 — 수정 요청 DTO
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        @NotBlank(message = "표시명은 필수입니다.")
        private String displayName;

        @NotBlank(message = "데이터 타입은 필수입니다.")
        @Pattern(regexp = "^(STRING|NUMBER|BOOLEAN|DATE|DATETIME|TIMESTAMP|ARRAY|OBJECT)$",
                 message = "유효하지 않은 데이터 타입입니다.")
        private String dataType;

        private String description;
        private Boolean isActive;
    }
}
