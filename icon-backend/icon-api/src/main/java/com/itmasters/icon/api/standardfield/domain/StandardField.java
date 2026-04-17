package com.itmasters.icon.api.standardfield.domain;

import com.itmasters.icon.common.domain.type.FieldDataType;
import com.itmasters.icon.common.domain.rule.FieldCategory;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 표준 필드 도메인
 * 시스템 간 통일된 필드 정의
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StandardField {

    private String fieldId;
    private String fieldName;
    private FieldCategory fieldCategory;
    private String displayName;
    private FieldDataType dataType;
    private String description;
    private Boolean isActive;

    @Builder
    private StandardField(String fieldName
            , FieldCategory fieldCategory
            , String displayName
            , FieldDataType dataType
            , String description
    ) {
        validateFieldName(fieldName);
        validateDataType(dataType);

        this.fieldName = fieldName;
        this.fieldCategory = fieldCategory;
        this.displayName = displayName != null ? displayName : fieldName;
        this.dataType = dataType;
        this.description = description;
        this.isActive = true;
    }

    /**
     * 표준 필드 생성
     */
    public static StandardField create(String fieldName, FieldCategory fieldCategory,
                                       FieldDataType dataType) {
        return StandardField.builder()
                .fieldName(fieldName)
                .fieldCategory(fieldCategory)
                .dataType(dataType)
                .build();
    }

    /**
     * 전체 정보로 표준 필드 생성
     */
    public static StandardField createWithDetails(String fieldName, FieldCategory fieldCategory,
                                                  String displayName, FieldDataType dataType,
                                                  String description) {
        return StandardField.builder()
                .fieldName(fieldName)
                .fieldCategory(fieldCategory)
                .displayName(displayName)
                .dataType(dataType)
                .description(description)
                .build();
    }

    /**
     * ID 할당 (Repository에서 사용)
     */
    public void assignId(String id) {
        if (this.fieldId != null) {
            throw new IllegalStateException("ID가 이미 할당되었습니다");
        }
        validateId(id);
        this.fieldId = id;
    }

    /**
     * 필드 정보 업데이트
     */
    public void updateFieldInfo(String displayName, String description) {
        this.displayName = displayName != null ? displayName : this.displayName;
        this.description = description;
    }

    /**
     * 카테고리 getter (fieldCategory의 alias)
     */
    public FieldCategory getCategory() {
        return this.fieldCategory;
    }

    /**
     * 카테고리 변경
     */
    public void updateCategory(FieldCategory category) {
        this.fieldCategory = category;
    }

    /**
     * 데이터 타입 변경
     */
    public void updateDataType(FieldDataType dataType) {
        validateDataType(dataType);
        this.dataType = dataType;
    }

    /**
     * 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 비활성화
     */
    public void deactivate() {
        this.isActive = false;
    }

    // 유효성 검증
    private static void validateFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            throw new IllegalArgumentException("필드명은 필수입니다");
        }
        if (!fieldName.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("필드명은 영문자로 시작하고 영문자, 숫자, 언더스코어만 허용됩니다");
        }
    }

    private static void validateDataType(FieldDataType dataType) {
        if (dataType == null) {
            throw new IllegalArgumentException("데이터 타입은 필수입니다");
        }
    }

    private static void validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID는 필수입니다");
        }
    }
}