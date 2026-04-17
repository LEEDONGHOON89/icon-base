package com.itmasters.icon.api.aggregationrule.adapter.persistence;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 룰 Repository
 */
@Repository
public interface RuleRepository extends JpaRepository<RuleEntity, String> {

    /**
     * 활성화된 룰 목록 조회
     */
    List<RuleEntity> findByIsActiveTrue();

    /**
     * 룰 ID로 활성화된 룰 조회
     */
    Optional<RuleEntity> findByRuleIdAndIsActiveTrue(String ruleId);

    /**
     * 이름으로 검색 (부분 일치)
     */
    @Query("SELECT r FROM RuleEntity r WHERE LOWER(r.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<RuleEntity> searchByName(String keyword);
}
