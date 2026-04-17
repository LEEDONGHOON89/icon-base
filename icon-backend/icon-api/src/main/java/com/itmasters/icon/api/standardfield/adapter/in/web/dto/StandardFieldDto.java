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
    
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Create {
        private String fieldName;
        private String displayName;
        private String description;
        private FieldCategory category;
        private FieldDataType dataType;
        private boolean isRequired;
        private boolean isSearchable;
        private String sampleValue;
        private String validationRule;
    }
    
    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Update {
        private String displayName;
        private String description;
        private boolean isRequired;
        private boolean isSearchable;
        private boolean isActive;
        private String sampleValue;
        private String validationRule;
    }
}