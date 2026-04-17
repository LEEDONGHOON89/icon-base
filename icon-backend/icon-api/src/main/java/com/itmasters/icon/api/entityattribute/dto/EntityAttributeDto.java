package com.itmasters.icon.api.entityattribute.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * entity_attributes 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityAttributeDto {

    /**
     * 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE 등)
     */
    private String entityType;

    /**
     * 엔티티 ID (CUS001, ACC001 등)
     */
    private String entityId;

    /**
     * 정적 속성 정보
     * 예: {"age": 68, "grade": "VIP", "region": "부산"}
     */
    private Map<String, Object> attributes;

    /**
     * 마지막 업데이트 시각
     */
    private LocalDateTime updatedAt;
}
