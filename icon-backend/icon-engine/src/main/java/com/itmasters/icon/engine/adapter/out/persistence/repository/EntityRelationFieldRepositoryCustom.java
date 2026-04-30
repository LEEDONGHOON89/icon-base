package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityRelationFieldEntity;

import java.util.List;

/**
 * Entity Relation Fields 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface EntityRelationFieldRepositoryCustom {

    /**
     * 특정 역할의 활성화된 필드 조회
     */
    List<EntityRelationFieldEntity> findByDataSourceIdAndRoles(String dataSourceId, List<String> roles);
}
