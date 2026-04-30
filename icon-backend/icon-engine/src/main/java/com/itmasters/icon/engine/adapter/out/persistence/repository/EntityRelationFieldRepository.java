package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EntityRelationFieldEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Entity Relation Fields Repository
 * 엔티티 관계 발견을 위한 후보 필드 설정 관리
 * QueryDSL 기반 복잡한 쿼리는 EntityRelationFieldRepositoryCustom에서 구현
 */
@Repository
public interface EntityRelationFieldRepository extends JpaRepository<EntityRelationFieldEntity, Long>, EntityRelationFieldRepositoryCustom {

    /**
     * 데이터소스별 활성화된 관계 후보 필드 조회 (우선순위 높은 순)
     */
    List<EntityRelationFieldEntity> findByDataSourceIdAndIsEnabledOrderByPriorityDesc(
            String dataSourceId,
            Boolean isEnabled
    );

    /**
     * 데이터소스별 모든 관계 후보 필드 조회
     */
    List<EntityRelationFieldEntity> findByDataSourceIdOrderByPriorityDesc(String dataSourceId);

    /**
     * 특정 역할의 활성화된 필드 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - EntityRelationFieldRepositoryCustom 인터페이스에서 선언, EntityRelationFieldRepositoryImpl에서 구현
    // List<EntityRelationFieldEntity> findByDataSourceIdAndRoles(String dataSourceId, List<String> roles);
}
