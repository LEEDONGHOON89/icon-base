package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityRelationEntity;

import java.util.List;

/**
 * Entity Relations 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface EntityRelationRepositoryCustom {

    /**
     * 디바이스 공유 탐지 (같은 디바이스를 사용하는 고객들)
     *
     * USES 관계에서 같은 to_entity_id(DEVICE)를 가진 관계들을 찾습니다.
     *
     * @param relationType 관계 타입 (USES)
     * @param toEntityType To 엔티티 타입 (DEVICE)
     * @return 공유되는 디바이스를 사용하는 관계 목록
     */
    List<EntityRelationEntity> findSharedDevices(String relationType, String toEntityType);
}
