package com.itmasters.icon.api.entityfield.domain;

import com.itmasters.icon.entity.Auditable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 엔티티 필드 도메인 - 시나리오 필터링에 사용될 수 있는 필드 정의
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EntityField extends Auditable {
    private String entityFieldId;    // 필드 ID (예: userId, accountType)
    private String displayName;      // 화면 표시명 (예: 사용자 ID, 계정 유형)
    private String dataType;         // 데이터 타입 (STRING, NUMBER, BOOLEAN)
    private String description;      // 설명
    private boolean isActive;        // 활성화 여부

    /**
     * 엔티티 필드 생성
     */
    public static EntityField of(String entityFieldId, String displayName, String dataType, String description) {
        EntityField field = new EntityField();
        field.entityFieldId = entityFieldId;
        field.displayName = displayName;
        field.dataType = dataType;
        field.description = description;
        field.isActive = true;
        return field;
    }

    /**
     * 엔티티 필드 수정
     */
    public void update(String displayName, String dataType, String description) {
        this.displayName = displayName;
        this.dataType = dataType;
        this.description = description;
    }

    /**
     * 활성화/비활성화
     */
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    /**
     * Setter for Repository (조회 시 사용)
     */
    public void setEntityFieldId(String entityFieldId) {
        this.entityFieldId = entityFieldId;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}
