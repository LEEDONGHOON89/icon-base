package com.itmasters.icon.api.engine;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.JpaRuleRepository;
import com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity;
import com.itmasters.icon.api.rule.adapter.persistence.repository.JpaSensorRepository;
import com.itmasters.icon.entity.EntityUpdateRuleEntity;
import com.itmasters.icon.entity.ScenarioAggregateEntity;
import com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioEntity;
import com.itmasters.icon.api.scenario.adapter.persistence.repository.JpaScenarioAggregateRepository;
import com.itmasters.icon.api.scenario.adapter.persistence.repository.JpaScenarioRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityUpdateRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EngineFlowService {
    
    private final JpaSensorRepository sensorRepository;
    private final JpaRuleRepository ruleRepository;
    private final JpaScenarioRepository scenarioRepository;
    private final JpaScenarioAggregateRepository scenarioAggregateRepository;
    private final EntityUpdateRuleRepository entityUpdateRuleRepository;
    
    /**
     * 전체 엔진 플로우 데이터 조회
     */
    public EngineFlowDto.Response getAllFlow() {
        // 1. 모든 Sensor 조회
        List<SensorEntity> allSensors = sensorRepository.findAll();

        // 2. 모든 Aggregate 조회
        List<RuleEntity> allAggregates = ruleRepository.findAll();

        // 3. 모든 Scenario 조회
        List<ScenarioEntity> allScenarios = scenarioRepository.findAll();

        // 4. 모든 ScenarioAggregate 조회
        List<ScenarioAggregateEntity> allScenarioAggregates = scenarioAggregateRepository.findAll();

        // 5. 모든 EntityUpdateRule 조회
        List<EntityUpdateRuleEntity> allEntityUpdateRules = entityUpdateRuleRepository.findAllActiveRules();

        // 6. Aggregate에서 사용된 Sensor만 필터링
        Set<String> usedSensorIds = allAggregates.stream()
            .map(RuleEntity::getPredicateSensorId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<SensorEntity> usedSensors = allSensors.stream()
            .filter(sensor -> usedSensorIds.contains(sensor.getSensorId()))
            .collect(Collectors.toList());

        return buildResponse(usedSensors, allAggregates, allScenarios, allScenarioAggregates, allEntityUpdateRules);
    }
    
    /**
     * 특정 시나리오의 플로우 데이터 조회
     */
    public EngineFlowDto.Response getFlowByScenario(String scenarioId) {
        // 1. 해당 Scenario 조회
        ScenarioEntity scenario = scenarioRepository.findById(scenarioId)
            .orElseThrow(() -> new IllegalArgumentException("Scenario not found: " + scenarioId));

        // 2. 해당 Scenario의 Aggregate 조회
        List<ScenarioAggregateEntity> scenarioAggregates =
            scenarioAggregateRepository.findByScenarioIdOrderByOrderNoAsc(scenarioId);

        // 3. Aggregate ID 추출
        Set<String> aggregateIds = scenarioAggregates.stream()
            .map(ScenarioAggregateEntity::getAggregateId)
            .collect(Collectors.toSet());

        // 4. 해당 Aggregate들 조회
        List<RuleEntity> aggregates = ruleRepository.findAllById(aggregateIds);

        // 5. Aggregate에서 사용된 Sensor 조회
        Set<String> sensorIds = aggregates.stream()
            .map(RuleEntity::getPredicateSensorId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

        List<SensorEntity> sensors = sensorRepository.findAllById(sensorIds);

        // 6. 해당 Scenario의 EntityUpdateRule 조회
        List<EntityUpdateRuleEntity> entityUpdateRules =
            entityUpdateRuleRepository.findActiveRulesByScenarioId(scenarioId);

        return buildResponse(sensors, aggregates, List.of(scenario), scenarioAggregates, entityUpdateRules);
    }
    
    /**
     * Response DTO 생성
     */
    private EngineFlowDto.Response buildResponse(
        List<SensorEntity> sensors,
        List<RuleEntity> aggregates,
        List<ScenarioEntity> scenarios,
        List<ScenarioAggregateEntity> scenarioAggregates,
        List<EntityUpdateRuleEntity> entityUpdateRules
    ) {
        // Sensor 변환
        List<EngineFlowDto.RuleInfo> ruleInfos = sensors.stream()
            .map(sensor -> new EngineFlowDto.RuleInfo(
                sensor.getSensorId(),
                sensor.getSensorName(),
                sensor.getWhereJson(), // whereJson 사용
                null, // domain은 @Transient
                null  // operator는 @Transient
            ))
            .collect(Collectors.toList());

        // Aggregate 변환
        List<EngineFlowDto.AggregateInfo> aggregateInfos = aggregates.stream()
            .map(agg -> new EngineFlowDto.AggregateInfo(
                agg.getRuleId(),
                agg.getName(), // name 필드 사용
                agg.getPredicateSensorId(),
                agg.getOperator() != null ? agg.getOperator().name() : null,
                agg.getAggregationField(), // aggregationField 사용
                null, // withinUnit은 별도 필드 없음
                agg.getWindowMinutes(), // windowMinutes 사용
                agg.getGroupByField() // @Transient 메서드 사용
            ))
            .collect(Collectors.toList());

        // Scenario 변환
        List<EngineFlowDto.ScenarioInfo> scenarioInfos = scenarios.stream()
            .map(scn -> new EngineFlowDto.ScenarioInfo(
                scn.getScenarioId(),
                scn.getScenarioName(),
                scn.getDescription(),
                null, // severity 필드 없음
                scn.getEntityFilterJson()
            ))
            .collect(Collectors.toList());

        // ScenarioAggregate 변환
        List<EngineFlowDto.ScenarioAggregateInfo> scenarioAggregateInfos = scenarioAggregates.stream()
            .map(sa -> new EngineFlowDto.ScenarioAggregateInfo(
                sa.getScenarioId(),
                sa.getAggregateId(),
                null, // threshold는 별도 테이블이거나 없을 수 있음
                sa.getOperator() != null ? sa.getOperator().name() : null,
                sa.getOrderNo()
            ))
            .collect(Collectors.toList());

        // EntityUpdateRule 변환
        List<EngineFlowDto.EntityUpdateRuleInfo> entityUpdateRuleInfos = entityUpdateRules.stream()
            .map(eur -> new EngineFlowDto.EntityUpdateRuleInfo(
                eur.getEntityUpdateRuleId(),
                eur.getScenarioId(),
                eur.getEntityType(),
                eur.getFieldName(),
                eur.getFieldValue(),
                eur.getFieldType(),
                eur.getDescription()
            ))
            .collect(Collectors.toList());

        return new EngineFlowDto.Response(ruleInfos, aggregateInfos, scenarioInfos, scenarioAggregateInfos, entityUpdateRuleInfos);
    }
}
