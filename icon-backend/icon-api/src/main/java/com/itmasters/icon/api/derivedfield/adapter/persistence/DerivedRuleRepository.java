package com.itmasters.icon.api.derivedfield.adapter.persistence;

import com.itmasters.icon.entity.DerivedRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 파생 필드 규칙 Repository
 *
 * 파생 필드 계산 규칙을 조회/관리합니다.
 * QueryDSL 기반 복잡한 쿼리는 DerivedRuleRepositoryCustom에서 구현
 */
@Repository("apiDerivedRuleRepository")
public interface DerivedRuleRepository extends JpaRepository<DerivedRuleEntity, Long>, DerivedRuleRepositoryCustom {

    /**
     * DataSource별 활성 규칙 조회 (우선순위 순) - QueryDSL로 구현
     */
    // @Query 제거됨 - DerivedRuleRepositoryCustom 인터페이스에서 선언, DerivedRuleRepositoryImpl에서 구현
    // List<DerivedRuleEntity> findActiveRulesByDataSourceId(String dataSourceId);

    /**
     * DataSource와 target_field로 규칙 조회
     *
     * @param dataSourceId DataSource ID
     * @param targetField 타겟 필드명
     * @return 파생 필드 규칙 (없으면 null)
     */
    DerivedRuleEntity findByDataSourceIdAndTargetField(String dataSourceId, String targetField);

    /**
     * DataSource별 모든 규칙 조회 (활성/비활성 포함)
     *
     * @param dataSourceId DataSource ID
     * @return 파생 필드 규칙 목록
     */
    List<DerivedRuleEntity> findByDataSourceId(String dataSourceId);
}
