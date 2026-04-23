package com.itmasters.icon.api.aggregationrule.application.service;

import com.itmasters.icon.api.aggregationrule.adapter.persistence.RuleRepository;
import com.itmasters.icon.api.aggregationrule.dto.RuleDto;
import com.itmasters.icon.common.domain.aggregate.AggregateOperator;
import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 룰 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RuleService {

    private final RuleRepository ruleRepository;

    /**
     * 전체 룰 목록 조회
     */
    public List<RuleDto.Response> findAll() {
        return ruleRepository.findAll().stream()
                .map(RuleDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 활성화된 룰 목록 조회
     */
    public List<RuleDto.Response> findActiveRules() {
        return ruleRepository.findByIsActiveTrue().stream()
                .map(RuleDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 룰 ID로 조회
     */
    public RuleDto.Response findById(String ruleId) {
        RuleEntity entity = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));
        return RuleDto.Response.from(entity);
    }

    /**
     * 룰 생성
     */
    @Transactional
    public RuleDto.Response createRule(RuleDto.CreateRequest request) {
        // [2026-04-23] AGG_ 접두사 필수 검증 추가 (도메인 규칙)
        if (request.getRuleId() == null || !request.getRuleId().startsWith("AGG_")) {
            throw new IllegalArgumentException("룰 ID는 AGG_ 접두사로 시작해야 합니다: " + request.getRuleId());
        }

        // 중복 ID 체크
        if (ruleRepository.existsById(request.getRuleId())) {
            throw new IllegalArgumentException("Rule ID already exists: " + request.getRuleId());
        }

        // WINDOW 모드에서는 연산자 필수
        String evaluationMode = request.getEvaluationMode() != null ? request.getEvaluationMode() : "WINDOW";
        if ("WINDOW".equals(evaluationMode) && (request.getOperator() == null || request.getOperator().isBlank())) {
            throw new IllegalArgumentException("연산자는 WINDOW 모드에서 필수입니다");
        }

        RuleEntity entity = RuleEntity.builder()
                .ruleId(request.getRuleId())
                .name(request.getName())
                .description(request.getDescription())
                .operator(request.getOperator() != null && !request.getOperator().isBlank()
                    ? AggregateOperator.valueOf(request.getOperator())
                    : null)
                .predicateSensorId(request.getPredicateSensorId())
                .prevSensorId(request.getPrevSensorId())
                .nextSensorId(request.getNextSensorId())
                .anchorSensorId(request.getAnchorSensorId())
                .windowMinutes(request.getWindowMinutes())
                .thresholdCount(request.getThresholdCount())
                .thresholdAmount(request.getThresholdAmount())
                .dedupMinutes(request.getDedupMinutes())
                .isActive(true)
                .groupByFields(request.getGroupByFields())
                .aggregationField(request.getAggregationField())
                .whereJson(request.getWhereJson())
                .evaluationMode(request.getEvaluationMode() != null ? request.getEvaluationMode() : "WINDOW")
                .build();

        RuleEntity saved = ruleRepository.save(entity);
        log.info("Rule created: {}", saved.getRuleId());
        return RuleDto.Response.from(saved);
    }

    /**
     * 룰 수정
     */
    @Transactional
    public RuleDto.Response updateRule(String ruleId, RuleDto.UpdateRequest request) {
        RuleEntity entity = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Rule not found: " + ruleId));

        // 기존 엔티티를 수정하는 대신 새 엔티티 생성 (불변성)
        RuleEntity updated = RuleEntity.builder()
                .ruleId(entity.getRuleId())
                .name(request.getName() != null ? request.getName() : entity.getName())
                .description(request.getDescription() != null ? request.getDescription() : entity.getDescription())
                .operator(request.getOperator() != null && !request.getOperator().isBlank()
                    ? AggregateOperator.valueOf(request.getOperator())
                    : entity.getOperator())
                .predicateSensorId(request.getPredicateSensorId() != null ? request.getPredicateSensorId() : entity.getPredicateSensorId())
                .prevSensorId(request.getPrevSensorId() != null ? request.getPrevSensorId() : entity.getPrevSensorId())
                .nextSensorId(request.getNextSensorId() != null ? request.getNextSensorId() : entity.getNextSensorId())
                .anchorSensorId(request.getAnchorSensorId() != null ? request.getAnchorSensorId() : entity.getAnchorSensorId())
                .windowMinutes(request.getWindowMinutes() != null ? request.getWindowMinutes() : entity.getWindowMinutes())
                .thresholdCount(request.getThresholdCount() != null ? request.getThresholdCount() : entity.getThresholdCount())
                .thresholdAmount(request.getThresholdAmount() != null ? request.getThresholdAmount() : entity.getThresholdAmount())
                .dedupMinutes(request.getDedupMinutes() != null ? request.getDedupMinutes() : entity.getDedupMinutes())
                .isActive(request.getIsActive() != null ? request.getIsActive() : entity.getIsActive())
                .groupByFields(request.getGroupByFields() != null ? request.getGroupByFields() : entity.getGroupByFields())
                .aggregationField(request.getAggregationField() != null ? request.getAggregationField() : entity.getAggregationField())
                .whereJson(request.getWhereJson() != null ? request.getWhereJson() : entity.getWhereJson())
                .evaluationMode(request.getEvaluationMode() != null ? request.getEvaluationMode() : entity.getEvaluationMode())
                .build();

        RuleEntity saved = ruleRepository.save(updated);
        log.info("Rule updated: {}", saved.getRuleId());
        return RuleDto.Response.from(saved);
    }

    /**
     * 룰 삭제
     */
    @Transactional
    public void deleteRule(String ruleId) {
        if (!ruleRepository.existsById(ruleId)) {
            throw new IllegalArgumentException("Rule not found: " + ruleId);
        }
        ruleRepository.deleteById(ruleId);
        log.info("Rule deleted: {}", ruleId);
    }
}
