package com.itmasters.icon.api.standardfield.domain;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 표준 필드 그룹 도메인
 * 논리적 스키마 정의 (실제 테이블 없음)
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StandardFieldGroup {
    
    private String groupId;
    private String groupName;
    private GroupType groupType;
    private String description;
    private boolean isActive;
    private List<StandardFieldGroupMember> members = new ArrayList<>();
    
    @Builder
    private StandardFieldGroup(String groupName, GroupType groupType, String description) {
        validateGroupName(groupName);
        
        this.groupName = groupName;
        this.groupType = groupType != null ? groupType : GroupType.VIRTUAL;
        this.description = description;
        this.isActive = true;
    }
    
    /**
     * 표준 필드 그룹 생성
     */
    public static StandardFieldGroup create(String groupName, GroupType groupType) {
        return StandardFieldGroup.builder()
                .groupName(groupName)
                .groupType(groupType)
                .build();
    }
    
    /**
     * 전체 정보로 표준 필드 그룹 생성
     */
    public static StandardFieldGroup createWithDetails(String groupName, GroupType groupType,
                                                     String description) {
        return StandardFieldGroup.builder()
                .groupName(groupName)
                .groupType(groupType)
                .description(description)
                .build();
    }
    
    /**
     * ID 할당 (Repository에서 사용)
     */
    public void assignId(String id) {
        if (this.groupId != null) {
            throw new IllegalStateException("ID가 이미 할당되었습니다");
        }
        validateId(id);
        this.groupId = id;
    }
    
    /**
     * 그룹 정보 업데이트
     */
    public void updateGroupInfo(String groupName, String description) {
        if (groupName != null && !groupName.trim().isEmpty()) {
            validateGroupName(groupName);
            this.groupName = groupName;
        }
        this.description = description;
    }
    
    /**
     * 그룹 타입 변경
     */
    public void updateGroupType(GroupType groupType) {
        if (groupType != null) {
            this.groupType = groupType;
        }
    }
    
    /**
     * 멤버 추가
     */
    public void addMember(StandardField field, int order, boolean isRequired) {
        if (field == null) {
            throw new IllegalArgumentException("표준 필드는 필수입니다");
        }
        
        // 중복 체크
        boolean exists = members.stream()
                .anyMatch(m -> m.getStandardField().getFieldId().equals(field.getFieldId()));
        if (exists) {
            throw new IllegalArgumentException("이미 추가된 필드입니다: " + field.getFieldName());
        }
        
        StandardFieldGroupMember member = StandardFieldGroupMember.create(this, field, order, isRequired);
        members.add(member);
    }
    
    /**
     * 멤버 제거
     */
    public void removeMember(String fieldId) {
        members.removeIf(m -> m.getStandardField().getFieldId().equals(fieldId));
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
    
    /**
     * 멤버 수 조회
     */
    public int getMemberCount() {
        return members.size();
    }
    
    /**
     * 특정 필드가 필수인지 확인
     */
    public boolean isFieldRequired(String fieldId) {
        return members.stream()
                .filter(m -> m.getStandardField().getFieldId().equals(fieldId))
                .findFirst()
                .map(StandardFieldGroupMember::isRequired)
                .orElse(false);
    }
    
    // 유효성 검증
    private static void validateGroupName(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("그룹명은 필수입니다");
        }
        if (!groupName.matches("^[a-zA-Z][a-zA-Z0-9_]*$")) {
            throw new IllegalArgumentException("그룹명은 영문자로 시작하고 영문자, 숫자, 언더스코어만 허용됩니다");
        }
    }
    
    private static void validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID는 필수입니다");
        }
    }
    
    /**
     * 그룹 타입
     */
    public enum GroupType {
        VIRTUAL("가상 그룹 - 실제 테이블 없음"),
        PHYSICAL("물리적 그룹 - 실제 테이블 존재");
        
        private final String description;
        
        GroupType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}