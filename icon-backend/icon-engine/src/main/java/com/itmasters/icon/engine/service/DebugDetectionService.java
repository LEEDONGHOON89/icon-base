package com.itmasters.icon.engine.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.LandingRawRecordEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.MappedDataStorageEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineProfileRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.LandingRawRecordRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.MappedDataStorageRepository;
import com.itmasters.icon.engine.dto.DataProcessingResultDto;
import com.itmasters.icon.engine.dto.DetectionContext;
import com.itmasters.icon.engine.dto.DetectionResult;
import com.itmasters.icon.engine.detector.RuleDetector;
import com.itmasters.icon.engine.mapping.FieldMappingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Debug utility: evaluate rules for a single mapped_storages row.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DebugDetectionService {

    private final MappedDataStorageRepository mappedDataStorageRepository;
    private final LandingRawRecordRepository landingRawRecordRepository;
    private final EngineProfileRepository engineProfileRepository;
    private final FieldMappingEngine fieldMappingEngine;
    private final RuleDetector ruleDetector;
    private final DetectRuleService detectRuleService;

    /**
     * Evaluate rules for a single mapped_storage_id and profile.
     * @param mappedStorageId target mapped_storages.mapped_storage_id
     * @param profileId target profile
     * @param persist whether to persist detect_rules/detect_rule_results
     * @param executedBy executor id
     * @return DetectionResult (and persisted if requested)
     */
    public DetectionResult evaluateSingleMappedRow(Long mappedStorageId,
                                                   String profileId,
                                                   boolean persist,
                                                   String executedBy) {
        MappedDataStorageEntity mds = mappedDataStorageRepository.findById(mappedStorageId)
                .orElseThrow(() -> new IllegalArgumentException("mapped_storages not found: " + mappedStorageId));
        EngineProfileEntity profile = engineProfileRepository.findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("profile not found: " + profileId));

        // landing_records를 통해 execDsMpId와 dataSourceId 조회
        LandingRawRecordEntity landingRecord = landingRawRecordRepository.findById(mds.getLandingRecordId())
                .orElse(null);
        Long execDsMpId = landingRecord != null && landingRecord.getExecDsMp() != null
                ? landingRecord.getExecDsMp().getExecDsMpId() : null;
        String dataSourceId = landingRecord != null ? landingRecord.getDataSourceId() : profile.getDataSourceId();

        Map<String, Object> row = mds.getRowData();
        // DataSourceSchema mapping to standard fields
        List<Map<String, Object>> mappedList = fieldMappingEngine.mapByDataSource(List.of(row), dataSourceId);
        Map<String, Object> mapped = mappedList.isEmpty() ? row : mappedList.get(0);
        // add source tracking ids like Step3
        if (mapped != null) {
            mapped.put("mapped_storage_id", mappedStorageId);
            mapped.put("_mapped_storage_id", "MDS_" + mappedStorageId);
        }

        DataProcessingResultDto processed = DataProcessingResultDto.success(null, List.of(mapped), null);

        DetectionContext context = DetectionContext.builder()
                .executionId(java.util.UUID.randomUUID().toString())
                .dataSourceId(dataSourceId)
                .profileId(profileId)
                .startTime(LocalDateTime.now())
                .executionMode(com.itmasters.icon.common.domain.type.ExecutionMode.MANUAL)
                .executedBy(executedBy)
                .build();

        DetectionResult result = ruleDetector.detect(processed, profileId, context);

        if (persist) {
            try {
                detectRuleService.saveDetectionResult(execDsMpId, result, context);
            } catch (Exception e) {
                log.error("Persisting detection result failed", e);
            }
        }

        return result;
    }
}
