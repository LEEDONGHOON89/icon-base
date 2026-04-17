package com.itmasters.icon.api.analytics.application;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.JpaRuleRepository;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.*;
import com.itmasters.icon.api.analytics.adapter.out.persistence.repository.*;
import com.itmasters.icon.api.analytics.dto.*;
import com.itmasters.icon.api.rule.adapter.persistence.entity.SensorEntity;
import com.itmasters.icon.api.rule.adapter.persistence.repository.SensorRepositoryImpl;
import com.itmasters.icon.api.scenario.adapter.persistence.entity.ScenarioEntity;
import com.itmasters.icon.api.scenario.adapter.persistence.repository.JpaScenarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExecutionAnalyticsService {

    private final LandingRawRecordJpaRepository landingRawRecordJpaRepository;
    private final MappedDataStorageJpaRepository mappedDataStorageJpaRepository;
    private final DetectRuleReadRepository detectRuleReadRepository;
    private final DetectRuleRepository detectRuleRepository;
    private final DetectScenarioRepository detectScenarioRepository;
    private final SensorRepositoryImpl sensorRepository;
    private final JpaRuleRepository ruleRepository;
    private final JpaScenarioRepository jpaScenarioRepository;

    public ExecutionPageDto getExecutions(int page, int size) {
        int pageIndex = Math.max(page, 0);
        int pageSize = size <= 0 ? 20 : size;

        Page<ApiLandingRawRecordEntity> result = landingRawRecordJpaRepository.findAll(
                PageRequest.of(pageIndex, pageSize, Sort.by(Sort.Direction.DESC, "landingRecordId"))
        );

        List<ApiLandingRawRecordEntity> rows = result.getContent();
        List<Long> landingIds = rows.stream()
                .map(ApiLandingRawRecordEntity::getLandingRecordId)
                .collect(Collectors.toList());

        Map<Long, ApiMappedDataStorageEntity> mappedByLandingId = landingIds.isEmpty()
                ? Collections.emptyMap()
                : mappedDataStorageJpaRepository.findByLandingRecordIdIn(landingIds).stream()
                        .collect(Collectors.toMap(ApiMappedDataStorageEntity::getLandingRecordId, m -> m));

        Set<Long> mappedStorageIds = mappedByLandingId.isEmpty()
                ? Collections.emptySet()
                : mappedByLandingId.values().stream()
                        .map(ApiMappedDataStorageEntity::getMappedStorageId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());

        Map<Long, Long> ruleCountMap = mappedStorageIds.isEmpty()
                ? Collections.emptyMap()
                : detectRuleReadRepository.countByMappedStorageIds(mappedStorageIds);
        Map<Long, Set<String>> ruleGroupKeys = mappedStorageIds.isEmpty()
                ? Collections.emptyMap()
                : detectRuleReadRepository.groupKeysByMappedStorageIds(mappedStorageIds);
        Map<Long, Long> aggregateCountMap = mappedStorageIds.isEmpty()
                ? Collections.emptyMap()
                : detectRuleRepository.countByMappedStorageIds(mappedStorageIds);
        Map<Long, Set<String>> aggregateGroupKeys = mappedStorageIds.isEmpty()
                ? Collections.emptyMap()
                : detectRuleRepository.groupKeysByMappedStorageIds(mappedStorageIds);

        List<ExecutionSummaryDto> items = new ArrayList<>();
        for (ApiLandingRawRecordEntity landing : rows) {
            ApiExecDsMpEntity exec = landing.getExecDsMp();
            ApiMappedDataStorageEntity mapped = mappedByLandingId.get(landing.getLandingRecordId());
            Long mappedStorageId = mapped != null ? mapped.getMappedStorageId() : null;

            long ruleCount = mappedStorageId != null ? ruleCountMap.getOrDefault(mappedStorageId, 0L) : 0L;
            long aggregateCount = mappedStorageId != null ? aggregateCountMap.getOrDefault(mappedStorageId, 0L) : 0L;

            Set<String> groupKeys = new LinkedHashSet<>();
            if (mappedStorageId != null) {
                groupKeys.addAll(ruleGroupKeys.getOrDefault(mappedStorageId, Collections.emptySet()));
                groupKeys.addAll(aggregateGroupKeys.getOrDefault(mappedStorageId, Collections.emptySet()));
            }
            long scenarioCount = groupKeys.isEmpty()
                    ? 0L
                    : detectScenarioRepository.countByExecIdAndGroupKeys(mappedStorageId, groupKeys);

            items.add(ExecutionSummaryDto.builder()
                    .landingRecordId(landing.getLandingRecordId())
                    .mappedStorageId(mappedStorageId)
                    .execDsMpId(exec.getExecDsMpId())
                    .dataSourceId(landing.getDataSourceId())
                    .sourceType(landing.getSourceType())
                    .executionMode(exec.getExecutionMode())
                    .status(exec.getStatus())
                    .startDt(exec.getStartDt())
                    .completeAt(exec.getCompleteAt())
                    .totalRows(exec.getTotalRows())
                    .executedBy(exec.getExecutedBy())
                    .rowIndex(landing.getRowIndex())
                    .batchKey(landing.getBatchKey())
                    .extractedAt(landing.getExtractedAt())
                    .ingestionStatus(landing.getIngestionStatus() != null ? landing.getIngestionStatus().name() : null)
                    .ingestionMessage(landing.getIngestionMessage())
                    .ruleCount(ruleCount)
                    .aggregateCount(aggregateCount)
                    .scenarioCount(scenarioCount)
                    .build());
        }

        return ExecutionPageDto.builder()
                .page(result.getNumber() + 1)
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .items(items)
                .build();
    }

    public ExecutionDetailDto getExecutionDetail(Long landingRecordId) {
        ApiLandingRawRecordEntity landing = landingRawRecordJpaRepository.findById(landingRecordId)
                .orElseThrow(() -> new IllegalArgumentException("Landing 이벤트가 존재하지 않습니다."));

        ApiExecDsMpEntity exec = landing.getExecDsMp();
        ApiMappedDataStorageEntity mapped = mappedDataStorageJpaRepository.findByLandingRecordId(landingRecordId).orElse(null);
        Long mappedStorageId = mapped != null ? mapped.getMappedStorageId() : null;

        List<ApiDetectRuleEntity> ruleRows = mappedStorageId != null
                ? detectRuleReadRepository.findByMappedStorageId(mappedStorageId)
                : Collections.emptyList();

        List<ApiDetectRuleEntity> aggregateRows = mappedStorageId != null
                ? detectRuleRepository.findByMappedStorageId(mappedStorageId)
                : Collections.emptyList();

        Set<String> groupKeys = collectGroupKeys(ruleRows, aggregateRows);

        List<ApiDetectScenarioEntity> scenarioRows = groupKeys.isEmpty()
                ? Collections.emptyList()
                : detectScenarioRepository.findByExecIdAndGroupKeys(mapped.getMappedStorageId(), groupKeys);

        Map<String, String> ruleNameMap = resolveRuleNames(ruleRows);
        Map<String, String> aggregateNameMap = resolveAggregateNames(aggregateRows);
        Map<String, String> scenarioNameMap = resolveScenarioNames(scenarioRows);

        List<DetectedRuleDto> ruleDtos = ruleRows.stream()
                .map(row -> DetectedRuleDto.builder()
                        .detectRuleId(row.getDetectRuleId())
                        .ruleId(row.getRuleId())
                        .ruleName(ruleNameMap.get(row.getRuleId()))
                        .groupKey(row.getGroupKey())
                        .mappedStorageId(row.getMappedStorageId())
                        .detectedAt(row.getDetectedDt())
                        .build())
                .collect(Collectors.toList());

        List<DetectRuleDto> aggregateDtos = aggregateRows.stream()
                .map(row -> DetectRuleDto.builder()
                        .groupKey(row.getGroupKey())
                        .ruleId(row.getRuleId())
                        .ruleName(aggregateNameMap.get(row.getRuleId()))
                        .operator(row.getOperator())
                        .windowMinutes(row.getWindowMinutes())
                        .matchedCount(row.getMatchedCount())
                        .thresholdCount(row.getThresholdCount())
                        .detectedAt(row.getDetectedDt())
                        .mappedStorageId(row.getMappedStorageId())
                        .build())
                .collect(Collectors.toList());

        List<DetectScenarioDto> scenarioDtos = scenarioRows.stream()
                .map(row -> DetectScenarioDto.builder()
                        .scenarioId(row.getScenarioId())
                        .scenarioName(scenarioNameMap.get(row.getScenarioId()))
                        .groupKey(row.getGroupKey())
                        .detectedAt(row.getDetectedDt())
                        .windowStart(row.getWindowStart())
                        .windowEnd(row.getWindowEnd())
                        .aggregateCount(null)
                        .passedCount(null)
                        .allPassed(null)
                        .build())
                .collect(Collectors.toList());

        ExecutionSummaryDto summary = ExecutionSummaryDto.builder()
                .landingRecordId(landing.getLandingRecordId())
                .mappedStorageId(mappedStorageId)
                .execDsMpId(exec.getExecDsMpId())
                .dataSourceId(landing.getDataSourceId())
                .sourceType(landing.getSourceType())
                .executionMode(exec.getExecutionMode())
                .status(exec.getStatus())
                .startDt(exec.getStartDt())
                .completeAt(exec.getCompleteAt())
                .totalRows(exec.getTotalRows())
                .executedBy(exec.getExecutedBy())
                .rowIndex(landing.getRowIndex())
                .batchKey(landing.getBatchKey())
                .extractedAt(landing.getExtractedAt())
                .ingestionStatus(landing.getIngestionStatus() != null ? landing.getIngestionStatus().name() : null)
                .ingestionMessage(landing.getIngestionMessage())
                .ruleCount(ruleRows.size())
                .aggregateCount(aggregateRows.size())
                .scenarioCount(scenarioRows.size())
                .build();

        return ExecutionDetailDto.builder()
                .summary(summary)
                .groupKeys(new ArrayList<>(groupKeys))
                .rules(ruleDtos)
                .aggregates(aggregateDtos)
                .scenarios(scenarioDtos)
                .rawPayload(landing.getRawPayload())
                .mappedRow(mapped != null ? mapped.getRowData() : null)
                .build();
    }

    private Map<String, String> resolveRuleNames(List<ApiDetectRuleEntity> rows) {
        Set<String> ruleIds = rows.stream()
                .map(ApiDetectRuleEntity::getRuleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ruleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return sensorRepository.findByIds(new ArrayList<>(ruleIds)).stream()
                .collect(Collectors.toMap(SensorEntity::getSensorId, SensorEntity::getSensorName));
    }

    private Map<String, String> resolveAggregateNames(List<ApiDetectRuleEntity> rows) {
        Set<String> aggregateIds = rows.stream()
                .map(ApiDetectRuleEntity::getRuleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (aggregateIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return ruleRepository.findAllById(aggregateIds).stream()
                .collect(Collectors.toMap(RuleEntity::getRuleId, RuleEntity::getName));
    }

    private Map<String, String> resolveScenarioNames(List<ApiDetectScenarioEntity> rows) {
        Set<String> scenarioIds = rows.stream()
                .map(ApiDetectScenarioEntity::getScenarioId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (scenarioIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return jpaScenarioRepository.findAllById(scenarioIds).stream()
                .collect(Collectors.toMap(ScenarioEntity::getScenarioId, ScenarioEntity::getScenarioName));
    }

    private Set<String> collectGroupKeys(List<ApiDetectRuleEntity> ruleRows,
                                          List<ApiDetectRuleEntity> aggregateRows) {
        Set<String> groupKeys = new LinkedHashSet<>();
        ruleRows.stream()
                .map(ApiDetectRuleEntity::getGroupKey)
                .filter(Objects::nonNull)
                .forEach(groupKeys::add);
        aggregateRows.stream()
                .map(ApiDetectRuleEntity::getGroupKey)
                .filter(Objects::nonNull)
                .forEach(groupKeys::add);
        return groupKeys;
    }
}
