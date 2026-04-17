package com.itmasters.icon.api.scenario.adapter.persistence.repository;

import com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 시나리오 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface ScenarioRepositoryCustom {

    /**
     * 시나리오 ID 또는 이름으로 검색
     */
    Page<ScenarioEntity> findByIdOrNameContaining(String search, Pageable pageable);

    /**
     * 활성화된 시나리오 중 ID 또는 이름으로 검색
     */
    Page<ScenarioEntity> findByIsActiveTrueAndIdOrNameContaining(String search, Pageable pageable);
}
