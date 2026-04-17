package com.itmasters.icon.api.standardfield.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 표준 필드 그룹 멤버 도메인
 * 그룹별 필드 구성
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StandardFieldGroupMember {
    
    private StandardFieldGroup group;
    private StandardField standardField;
    private int fieldOrder;
    private boolean isRequired;
    
    @Builder
    private StandardFieldGroupMember(StandardFieldGroup group, StandardField standardField,
                                   int fieldOrder, boolean isRequired) {
        validateGroup(group);
        validateField(standardField);
        
        this.group = group;
        this.standardField = standardField;
        this.fieldOrder = fieldOrder;
        this.isRequired = isRequired;
    }
    
    /**
     * 그룹 멤버 생성
     */
    public static StandardFieldGroupMember create(StandardFieldGroup group, 
                                                StandardField standardField,
                                                int fieldOrder,
                                                boolean isRequired) {
        return StandardFieldGroupMember.builder()
                .group(group)
                .standardField(standardField)
                .fieldOrder(fieldOrder)
                .isRequired(isRequired)
                .build();
    }
    
    /**
     * 필드 순서 변경
     */
    public void updateOrder(int newOrder) {
        if (newOrder < 0) {
            throw new IllegalArgumentException("필드 순서는 0 이상이어야 합니다");
        }
        this.fieldOrder = newOrder;
    }
    
    /**
     * 필수 여부 변경
     */
    public void updateRequired(boolean required) {
        this.isRequired = required;
    }
    
    // 유효성 검증
    private static void validateGroup(StandardFieldGroup group) {
        if (group == null) {
            throw new IllegalArgumentException("그룹은 필수입니다");
        }
    }
    
    private static void validateField(StandardField field) {
        if (field == null) {
            throw new IllegalArgumentException("표준 필드는 필수입니다");
        }
    }
}