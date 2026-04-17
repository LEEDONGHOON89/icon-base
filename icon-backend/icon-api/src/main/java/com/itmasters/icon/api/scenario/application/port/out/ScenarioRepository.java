package com.itmasters.icon.api.scenario.application.port.out;

import com.itmasters.icon.api.scenario.domain.Scenario;

import java.util.List;
import java.util.Optional;

/**
 * 시나리오 Repository 인터페이스
 */
public interface ScenarioRepository {
    
    /**
     * 시나리오 저장
     */
    Scenario save(Scenario scenario);
    
    /**
     * 시나리오 조회
     */
    Optional<Scenario> findById(String scenarioId);
    
    /**
     * 모든 시나리오 목록 조회
     */
    List<Scenario> findAll();
    
    /**
     * 활성화된 시나리오 목록 조회
     */
    List<Scenario> findByIsActiveTrue();
    
    /**
     * 시나리오명으로 검색
     */
    List<Scenario> findByScenarioNameContaining(String scenarioName);
    
    /**
     * 시나리오 삭제
     */
    void deleteById(String scenarioId);
    
    /**
     * 시나리오 존재 여부 확인
     */
    boolean existsById(String scenarioId);
    
    /**
     * 시나리오명 중복 확인
     */
    boolean existsByScenarioName(String scenarioName);

    /**
     * 페이징/필터 조회
     */
    org.springframework.data.domain.Page<Scenario> findPagedFiltered(String search, Boolean activeOnly, org.springframework.data.domain.Pageable pageable);
}
