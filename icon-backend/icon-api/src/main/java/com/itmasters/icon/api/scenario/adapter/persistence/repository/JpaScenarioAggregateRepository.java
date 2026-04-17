package com.itmasters.icon.api.scenario.adapter.persistence.repository;

import com.itmasters.icon.entity.ScenarioAggregateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface JpaScenarioAggregateRepository extends JpaRepository<ScenarioAggregateEntity, Long> {
    List<ScenarioAggregateEntity> findByScenarioId(String scenarioId);
    List<ScenarioAggregateEntity> findByScenarioIdOrderByOrderNoAsc(String scenarioId);
    List<ScenarioAggregateEntity> findByAggregateId(String aggregateId);

    @Modifying
    @Transactional
    void deleteByScenarioId(String scenarioId);

    @Modifying
    @Transactional
    void deleteByAggregateId(String aggregateId);

    @Modifying
    @Transactional
    void deleteByScenarioIdAndAggregateId(String scenarioId, String aggregateId);
    boolean existsByScenarioIdAndAggregateId(String scenarioId, String aggregateId);
}

