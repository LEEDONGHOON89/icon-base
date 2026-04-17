package com.itmasters.icon.api.scenario.adapter.persistence;

import com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioEntity;
import com.itmasters.icon.api.scenario.adapter.persistence.mapper.ScenarioMapper;
import com.itmasters.icon.api.scenario.adapter.persistence.repository.JpaScenarioRepository;
import com.itmasters.icon.api.scenario.application.port.out.ScenarioRepository;
import com.itmasters.icon.api.scenario.domain.Scenario;
import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * ScenarioRepository 어댑터 구현체
 * Port interface를 구현하고 JPA Repository를 사용
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ScenarioRepositoryImpl implements ScenarioRepository {

    private final JpaScenarioRepository jpaRepository;
    private final ScenarioMapper mapper;
    private final IdGenerator idGenerator;

    @Override
    public Scenario save(Scenario scenario) {
        // ID가 없는 경우 새로 생성
        if (scenario.getScenarioId() == null) {
            scenario.assignId(idGenerator.generateId(EntityType.SCENARIO));
        }

        ScenarioEntity entity = mapper.toEntity(scenario);
        ScenarioEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Scenario> findById(String scenarioId) {
        return jpaRepository.findById(scenarioId)
                .map(mapper::toDomain);
    }

    @Override
    public List<Scenario> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Scenario> findByIsActiveTrue() {
        return jpaRepository.findByIsActiveTrue().stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<Scenario> findByScenarioNameContaining(String scenarioName) {
        return jpaRepository.findByScenarioNameContaining(scenarioName).stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String scenarioId) {
        jpaRepository.deleteById(scenarioId);
    }

    @Override
    public boolean existsById(String scenarioId) {
        return jpaRepository.existsById(scenarioId);
    }

    @Override
    public boolean existsByScenarioName(String scenarioName) {
        return jpaRepository.existsByScenarioName(scenarioName);
    }

    @Override
    public Page<Scenario> findPagedFiltered(String search, Boolean activeOnly, Pageable pageable) {
        Page<ScenarioEntity> entityPage;

        if (search != null && !search.isBlank()) {
            if (activeOnly != null && activeOnly) {
                entityPage = jpaRepository.findByIsActiveTrueAndIdOrNameContaining(search, pageable);
            } else {
                entityPage = jpaRepository.findByIdOrNameContaining(search, pageable);
            }
        } else {
            if (activeOnly != null && activeOnly) {
                entityPage = jpaRepository.findByIsActiveTrue(pageable);
            } else {
                entityPage = jpaRepository.findAll(pageable);
            }
        }

        return entityPage.map(mapper::toDomain);
    }
}
