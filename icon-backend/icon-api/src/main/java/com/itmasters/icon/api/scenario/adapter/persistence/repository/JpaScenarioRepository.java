package com.itmasters.icon.api.scenario.adapter.persistence.repository;

import com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 시나리오 JPA Repository
 * QueryDSL 기반 복잡한 쿼리는 ScenarioRepositoryCustom에서 구현
 */
@Repository
public interface JpaScenarioRepository extends JpaRepository<ScenarioEntity, String>, ScenarioRepositoryCustom {

    /**
     * 활성화된 시나리오 목록 조회
     */
    List<ScenarioEntity> findByIsActiveTrue();
    Page<ScenarioEntity> findByIsActiveTrue(Pageable pageable);

    /**
     * 시나리오명으로 검색
     */
    List<ScenarioEntity> findByScenarioNameContaining(String scenarioName);
    Page<ScenarioEntity> findByScenarioNameContainingIgnoreCase(String scenarioName, Pageable pageable);
    Page<ScenarioEntity> findByIsActiveTrueAndScenarioNameContainingIgnoreCase(String scenarioName, Pageable pageable);

    /**
     * 시나리오 ID 또는 이름으로 검색 - QueryDSL로 구현 (ScenarioRepositoryImpl)
     */
    // @Query 제거됨 - ScenarioRepositoryCustom 인터페이스에서 선언, ScenarioRepositoryImpl에서 구현
    // Page<ScenarioEntity> findByIdOrNameContaining(String search, Pageable pageable);

    /**
     * 활성화된 시나리오 중 ID 또는 이름으로 검색 - QueryDSL로 구현 (ScenarioRepositoryImpl)
     */
    // @Query 제거됨 - ScenarioRepositoryCustom 인터페이스에서 선언, ScenarioRepositoryImpl에서 구현
    // Page<ScenarioEntity> findByIsActiveTrueAndIdOrNameContaining(String search, Pageable pageable);

    /**
     * 시나리오명 중복 확인
     */
    boolean existsByScenarioName(String scenarioName);
}