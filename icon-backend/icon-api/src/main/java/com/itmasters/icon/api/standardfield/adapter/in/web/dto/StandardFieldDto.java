package com.itmasters.icon.api.standardfield.adapter.in.web.dto;

import com.itmasters.icon.common.domain.rule.FieldCategory;
import com.itmasters.icon.common.domain.type.FieldDataType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class StandardFieldDto {
    
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Response {
        private String fieldId;
        private String fieldName;
        private String displayName;
        private String description;
        private FieldCategory category;
        private FieldDataType dataType;
        private boolean isRequired;
        private boolean isSearchable;
        private boolean isActive;
        private String sampleValue;
        private String validationRule;
    }
    
    // [2026-04-20] Jackson 역직렬화를 위해 public 접근자로 변경, Create/Update CRUD 기능 추가
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Create {
        private String fieldName;
        private String displayName;
        private String description;
        private FieldCategory category;
        private FieldDataType dataType;
    }

    // [2026-04-20] category, dataType 수정 지원 추가, isActive를 Boolean 래퍼 타입으로 변경
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Update {
        private String displayName;
        private String description;
        private FieldCategory category;
        private FieldDataType dataType;
        private Boolean isActive;
    }
}