package com.itmasters.icon.engine.repository;

import com.itmasters.icon.entity.DerivedRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 파생 필드 규칙 Repository (icon-engine용)
 *
 * icon-api의 DerivedRuleRepository와 동일한 테이블 접근
 */
@Repository("engineDerivedRuleRepository")
public interface DerivedRuleRepository extends JpaRepository<DerivedRuleEntity, Long> {

    /**
     * DataSource ID로 활성화된 규칙 조회
     */
    List<DerivedRuleEntity> findByDataSourceIdAndIsActiveTrueOrderByPriorityAsc(String dataSourceId);
}
