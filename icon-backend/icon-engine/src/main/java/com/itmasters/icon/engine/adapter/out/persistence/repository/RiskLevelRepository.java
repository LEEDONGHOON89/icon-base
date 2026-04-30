package com.itmasters.icon.engine.adapter.out.persistence.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RiskLevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * RiskLevel JPA Repository
 */
@Repository
public interface RiskLevelRepository extends JpaRepository<RiskLevelEntity, String> {

    /**
     * risk_level_id로 조회
     */
    Optional<RiskLevelEntity> findByRiskLevelId(String riskLevelId);
}
