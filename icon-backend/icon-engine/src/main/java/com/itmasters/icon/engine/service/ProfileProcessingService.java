package com.itmasters.icon.engine.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
import com.itmasters.icon.engine.detector.RuleDetector;
import com.itmasters.icon.engine.dto.DataProcessingResultDto;
import com.itmasters.icon.engine.dto.DetectionContext;
import com.itmasters.icon.engine.dto.DetectionResult;
import com.itmasters.icon.engine.dto.RuleMatchedData;
import com.itmasters.icon.engine.mapping.FieldMappingEngine;
import com.itmasters.icon.engine.mapping.ProfileSchemaMappingEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 프로파일 처리 전용 서비스
 * 트랜잭션 경계를 명확히 하여 Lazy Loading 문제 해결
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileProcessingService {

    private final FieldMappingEngine fieldMappingEngine;
    private final ProfileSchemaMappingEngine profileSchemaMappingEngine;
    private final ExecDsMpService execDsMpService;
    private final LandingTransformService landingTransformService;
    private final RuleDetector ruleDetector;
    private final DetectRuleService detectRuleService;  // 탐지 결과 저장 서비스 추가
    private final EventLogService eventLogService;  // user_activity_log 저장 서비스 추가
    @org.springframework.beans.factory.annotation.Value("${engine.derived.highAmountThreshold:100000}")
    private long highAmountThreshold;

    /**
     * 프로파일별 처리 - 필드 매핑 + 데이터 저장 + 룰 탐지
     * 트랜잭션 내에서 실행되어 Lazy Loading 문제 해결
     * detect_key를 활용한 데이터 그룹화 처리 추가
     *
     * @param execDsMpId 실행 ID
     * @param rawData    원본 데이터
     * @param profile    프로파일 정보
     * @param context    실행 컨텍스트
     * @param executedBy 실행자
     * @return 탐지 결과
     */
    @Transactional
    public DetectionResult processProfile(Long execDsMpId,
                                          DataProcessingResultDto rawData,
                                          EngineProfileEntity profile,
                                          DetectionContext context,
                                          String executedBy) {
        try {
            // 데이터가 이미 매핑되었는지 확인 (표준 필드를 포함하고 있는지 체크)
            log.debug("필드 매핑 시작 - profileId: {}, dataSourceId: {}",
                    profile.getProfileId(), profile.getDataSourceId());
            List<Map<String, Object>> mappedData = fieldMappingEngine.mapByDataSource(
                    rawData.getRawData() != null ? rawData.getRawData() : List.of(),
                    profile.getDataSourceId()
            );

            // 매핑된 데이터가 비어있지 않은 경우만 필터링
            List<Map<String, Object>> dataForDetection = mappedData.stream()
                    .filter(row -> !row.isEmpty())
                    .collect(Collectors.toList());

            log.debug("필드 매핑 완료 - 원본: {} 건, 매핑된 데이터: {} 건",
                    mappedData.size(), dataForDetection.size());

            // 파생 신호 보강 후 mapped_storages에 저장하고 event_stream으로 변환
            log.info("mapped_storages 및 user_activity_log 저장 시작 - profileId: {}, 데이터 건수: {}",
                    profile.getProfileId(), dataForDetection.size());

            for (int i = 0; i < dataForDetection.size(); i++) {
                Map<String, Object> data = dataForDetection.get(i);
                enrichDerivedSignals(data);
                try {
                    // 1. mapped_storages에 저장하고 ID 받기
                    var landingRecord = execDsMpService.saveLandingRecord(execDsMpId, i, data);
                    var mappedRow = landingTransformService.transformLandingRecord(landingRecord, data);
                    Long mappedDataStorageId = mappedRow.getMappedDataStorageId();

                    // 2. user_activity_log에 저장 (mapped_storage_id 포함)
                    // EventLogService에서 MDS_ 접두사를 처리하므로 그대로 전달
                    String mappedDataStorageIdStr = "MDS_" + mappedDataStorageId;
                    eventLogService.convertToEventStream(data, profile.getProfileId(), mappedDataStorageIdStr);

                    log.debug("mapped_storages -> event_stream 저장 완료 - mappedDataStorageId: {}", mappedDataStorageId);
                } catch (Exception e) {
                    log.error("user_activity_log 저장 실패 - profileId: {}, error: {}",
                            profile.getProfileId(), e.getMessage());
                    // 개별 레코드 실패는 무시하고 계속 진행
                }
            }
            log.info("mapped_storages 및 user_activity_log 저장 완료 - profileId: {}", profile.getProfileId());

            // TODO: groupKey/groupKeyType 컬럼 제거됨 - 그룹화 로직은 deprecated
            // entity_id_field를 임시로 사용하되, 장기적으로는 aggregate.group_by_fields 사용 필요
            DetectionResult finalResult;
            String entityIdField = profile.getEntityIdField();
            if (entityIdField != null && !entityIdField.isEmpty()) {
                log.info("entity_id_field 기반 처리 시작 - profileId: {}, entityIdField: {}",
                        profile.getProfileId(), entityIdField);

                // group_key로 데이터 그룹화
                Map<String, List<Map<String, Object>>> groupedData =
                        profileSchemaMappingEngine.groupByGroupKey(dataForDetection, profile.getProfileId());

                // 각 그룹별로 룰 탐지 수행
                Map<String, DetectionResult> groupResults = new HashMap<>();
                for (Map.Entry<String, List<Map<String, Object>>> entry : groupedData.entrySet()) {
                    String detectKeyValue = entry.getKey();
                    List<Map<String, Object>> groupData = entry.getValue();

                    log.debug("그룹 처리 - detectKeyValue: {}, 데이터 건수: {}", detectKeyValue, groupData.size());

                    // 그룹 데이터로 DataProcessingResultDto 생성 (파생 신호 보강)
                    for (Map<String, Object> r : groupData) enrichDerivedSignals(r);
                    DataProcessingResultDto groupProcessedData = DataProcessingResultDto.success(
                            null,
                            groupData,
                            rawData.getValidation()
                    );

                    // 그룹별 룰 탐지
                    DetectionResult groupResult = ruleDetector.detect(groupProcessedData, profile.getProfileId(), context);
                    groupResults.put(detectKeyValue, groupResult);
                }

                // 그룹별 결과를 통합
                finalResult = mergeDetectionResults(groupResults, context);
                log.info("detect_key 기반 처리 완료 - 총 {} 개 그룹 처리", groupResults.size());

            } else {
                // detect_key가 없으면 기존 방식대로 처리
                // 단일 처리 (파생 신호 보강)
                for (Map<String, Object> r : dataForDetection) enrichDerivedSignals(r);
                DataProcessingResultDto mappedProcessedData = DataProcessingResultDto.success(
                        null,
                        dataForDetection,
                        rawData.getValidation()
                );

                log.debug("룰 탐지 시작 - profileId: {}", profile.getProfileId());
                finalResult = ruleDetector.detect(mappedProcessedData, profile.getProfileId(), context);
            }

            // 5. 컨텍스트 완료
            context.complete();

            // 6. 탐지 결과 DB 저장 (신규 추가)
            try {
                // 탐지 결과 로그 추가
                log.info("탐지 결과 요약 - profileId: {}, totalRows: {}, totalMatched: {}, totalRules: {}",
                        profile.getProfileId(), finalResult.getTotalRows(),
                        finalResult.getTotalMatched(), finalResult.getTotalRules());

                if (finalResult.getMatchedData() != null && !finalResult.getMatchedData().isEmpty()) {
                    log.info("매칭된 데이터 상세 - profileId: {}, matchedData size: {}",
                            profile.getProfileId(), finalResult.getMatchedData().size());
                } else {
                    log.warn("매칭된 데이터 없음 - profileId: {}", profile.getProfileId());
                }

                detectRuleService.saveDetectionResult(execDsMpId, finalResult, context);
                log.info("탐지 결과 DB 저장 완료 - profileId: {}, matched: {} 건",
                        profile.getProfileId(), finalResult.getTotalMatched());
            } catch (Exception e) {
                log.error("탐지 결과 저장 실패 (탐지는 성공) - profileId: {}",
                        profile.getProfileId(), e);
                // 저장 실패해도 탐지 결과는 반환
            }

            return finalResult;

        } catch (Exception e) {
            log.error("프로파일 처리 중 오류 - profileId: {}", profile.getProfileId(), e);
            context.complete();

            // 실패 결과도 DB에 저장
            DetectionResult failedResult = DetectionResult.failed(context, e.getMessage());
            try {
                detectRuleService.saveDetectionResult(execDsMpId, failedResult, context);
                log.info("실패 결과 DB 저장 완료 - profileId: {}", profile.getProfileId());
            } catch (Exception saveEx) {
                log.error("실패 결과 저장 중 오류", saveEx);
            }

            throw e;
        }
    }

    /**
     * Derived signals for long-term rule simplification.
     * - is_transfer: transaction_type == '이체' (or 'TRANSFER')
     * - is_high_amount_transfer: is_transfer && transaction_amount >= threshold
     */
    private void enrichDerivedSignals(Map<String, Object> row) {
        if (row == null || row.isEmpty()) return;
        try {
            String txType = null;
            Object t1 = row.get("transaction_type");
            if (t1 != null) txType = String.valueOf(t1);
            else if (row.containsKey("TRX_TYPE")) txType = String.valueOf(row.get("TRX_TYPE"));

            java.math.BigDecimal amt = null;
            Object a1 = row.get("transaction_amount");
            if (a1 != null) {
                try { amt = new java.math.BigDecimal(String.valueOf(a1)); } catch (Exception ignore) {}
            } else if (row.containsKey("TRX_AMT")) {
                try { amt = new java.math.BigDecimal(String.valueOf(row.get("TRX_AMT"))); } catch (Exception ignore) {}
            }

            boolean isTransfer = txType != null && ("이체".equals(txType) || "TRANSFER".equalsIgnoreCase(txType));
            boolean isHigh = false;
            if (amt != null) {
                try { isHigh = amt.compareTo(java.math.BigDecimal.valueOf(highAmountThreshold)) >= 0; } catch (Exception ignore) {}
            }
            row.put("is_transfer", isTransfer);
            row.put("is_high_amount_transfer", isTransfer && isHigh);

            // is_third_party 는 공급사가 명시적으로 전달해야 한다
            try {
                Boolean provided = extractBooleanFlag(row, "is_third_party");
                if (provided != null) {
                    row.put("is_third_party", provided);
                } else {
                    log.warn("is_third_party not provided; row keys={} (profile pipeline expects supplier-provided flag)", row.keySet());
                }
            } catch (Exception ignore) { }
        } catch (Exception ignore) { }
    }

    private Boolean extractBooleanFlag(Map<String, Object> row, String targetKey) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        String matchedKey = null;
        Object rawValue = null;
        for (String key : row.keySet()) {
            if (key != null && key.equalsIgnoreCase(targetKey)) {
                matchedKey = key;
                rawValue = row.get(key);
                break;
            }
        }
        if (matchedKey == null) {
            return null;
        }
        Boolean coerced = coerceToBoolean(rawValue);
        if (!targetKey.equals(matchedKey)) {
            row.remove(matchedKey);
        }
        if (coerced != null) {
            row.put(targetKey, coerced);
        }
        return coerced;
    }

    private Boolean coerceToBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = value.toString();
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ("true".equalsIgnoreCase(trimmed) || "y".equalsIgnoreCase(trimmed) ||
                "yes".equalsIgnoreCase(trimmed) || "1".equals(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed) || "n".equalsIgnoreCase(trimmed) ||
                "no".equalsIgnoreCase(trimmed) || "0".equals(trimmed)) {
            return false;
        }
        return null;
    }

    /**
     * 저장 없이 탐지만 수행 (Step3용)
     * - mapped_storages/event_stream에 재삽입하지 않음
     */
    @Transactional
    public DetectionResult processProfileDetectOnly(Long execDsMpId,
                                                    DataProcessingResultDto rawData,
                                                    EngineProfileEntity profile,
                                                    DetectionContext context,
                                                    String executedBy) {
        try {
            log.debug("[DetectOnly] 필드 매핑 시작 - profileId: {}", profile.getProfileId());

            List<Map<String, Object>> mappedData;
            if (rawData.getMappedData() != null && !rawData.getMappedData().isEmpty()) {
                // 이미 매핑된 데이터가 전달된 경우 복사본을 사용 (변형을 위해 가변 Map 필요)
                mappedData = rawData.getMappedData().stream()
                        .map(row -> row == null ? new HashMap<String, Object>() : new HashMap<>(row))
                        .collect(Collectors.toList());
            } else {
                // Raw 데이터만 있는 경우에만 매핑 수행
                List<Map<String, Object>> rawRows = rawData.getRawData() != null ? rawData.getRawData() : List.of();
                mappedData = fieldMappingEngine.mapByDataSource(rawRows, profile.getDataSourceId());
            }

            List<Map<String, Object>> dataForDetection = mappedData.stream()
                    .filter(row -> !row.isEmpty())
                    .collect(Collectors.toList());

            log.debug("[DetectOnly] 필드 매핑 완료 - 원본: {} 건, 매핑된 데이터: {} 건",
                    mappedData.size(), dataForDetection.size());

            // detect_key가 있는 경우 그룹화 처리 로직은 기존과 동일 (생략)

            DataProcessingResultDto mappedProcessedData = DataProcessingResultDto.success(
                    null,
                    dataForDetection,
                    rawData.getValidation()
            );

            log.debug("[DetectOnly] 룰 탐지 시작 - profileId: {}", profile.getProfileId());
            DetectionResult finalResult = ruleDetector.detect(mappedProcessedData, profile.getProfileId(), context);

            context.complete();

            try {
                detectRuleService.saveDetectionResult(execDsMpId, finalResult, context);
                log.info("[DetectOnly] 탐지 결과 저장 완료 - profileId: {}, matched: {} 건",
                        profile.getProfileId(), finalResult.getTotalMatched());
            } catch (Exception e) {
                log.error("[DetectOnly] 탐지 결과 저장 실패 - profileId: {}", profile.getProfileId(), e);
            }

            return finalResult;

        } catch (Exception e) {
            log.error("[DetectOnly] 프로파일 처리 중 오류 - profileId: {}", profile.getProfileId(), e);
            context.complete();
            DetectionResult failedResult = DetectionResult.failed(context, e.getMessage());
            try { detectRuleService.saveDetectionResult(execDsMpId, failedResult, context); } catch (Exception ignore) {}
            throw e;
        }
    }

    /**
     * 필드 매핑만 수행 (트랜잭션 내에서 실행)
     *
     * @param rawData      원본 데이터
     * @param dataSourceId 데이터소스 ID
     * @return 매핑된 데이터
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> performFieldMapping(List<Map<String, Object>> rawData, String dataSourceId) {
        log.debug("필드 매핑 수행 - dataSourceId: {}", dataSourceId);

        List<Map<String, Object>> mappedData = fieldMappingEngine.mapByDataSource(rawData, dataSourceId);

        // 빈 데이터 필터링
        return mappedData.stream()
                .filter(row -> !row.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * 데이터가 이미 매핑되었는지 확인
     * 표준 필드명을 포함하고 있으면 이미 매핑된 것으로 판단
     *
     * @param data 확인할 데이터
     * @return 매핑 여부
     */
    private boolean isDataAlreadyMapped(List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return false;
        }

        // 첫 번째 레코드에서 표준 필드명 확인
        Map<String, Object> firstRow = data.get(0);

        // 표준 필드명들 (예시)
        String[] standardFields = {
                "transaction_amount",
                "transaction_datetime",
                "transaction_type",
                "after_balance"
        };

        // 표준 필드 중 하나라도 있으면 매핑된 것으로 판단
        for (String field : standardFields) {
            if (firstRow.containsKey(field)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 그룹별 탐지 결과를 통합
     *
     * @param groupResults 그룹별 탐지 결과
     * @param context      탐지 컨텍스트
     * @return 통합된 탐지 결과
     */
    private DetectionResult mergeDetectionResults(Map<String, DetectionResult> groupResults,
                                                  DetectionContext context) {
        if (groupResults.isEmpty()) {
            return DetectionResult.builder()
                    .context(context)
                    .success(true)
                    .totalRows(0)
                    .totalMatched(0)
                    .totalRules(0)
                    .build();
        }

        // 단일 그룹인 경우 그대로 반환
        if (groupResults.size() == 1) {
            return groupResults.values().iterator().next();
        }

        // 여러 그룹의 결과를 통합
        int totalRows = 0;
        int totalMatched = 0;
        int totalRules = 0;
        List<Map<String, Object>> allMatchedRecords = new ArrayList<>();
        List<DetectionResult.RuleMatchDetail> allMatchDetails = new ArrayList<>();
        List<RuleMatchedData> allMatchedData = new ArrayList<>(); // matchedData 병합 추가

        for (DetectionResult result : groupResults.values()) {
            totalRows += result.getTotalRows();
            totalMatched += result.getTotalMatched();
            totalRules = Math.max(totalRules, result.getTotalRules()); // 룰 개수는 동일하므로 max 사용

            if (result.getMatchedRecords() != null) {
                allMatchedRecords.addAll(result.getMatchedRecords());
            }
            if (result.getMatchDetails() != null) {
                allMatchDetails.addAll(result.getMatchDetails());
            }
            // matchedData 병합 추가
            if (result.getMatchedData() != null) {
                allMatchedData.addAll(result.getMatchedData());
            }
        }

        DetectionResult mergedResult = DetectionResult.builder()
                .context(context)
                .success(true)
                .totalRows(totalRows)
                .totalMatched(totalMatched)
                .totalRules(totalRules)
                .matchedRecords(allMatchedRecords)
                .matchDetails(allMatchDetails)
                .matchedData(allMatchedData)  // matchedData 추가
                .build();

        log.debug("탐지 결과 통합 완료 - 그룹 수: {}, 총 매칭 수: {}",
                groupResults.size(), mergedResult.getTotalMatched());

        return mergedResult;
    }

    /**
     * detect_key 기반 데이터 처리 (별도 메서드로 분리 가능)
     *
     * @param dataForDetection 탐지할 데이터
     * @param profile          프로파일 정보
     * @return detect_key로 그룹화된 데이터 (key가 없으면 전체 데이터를 "default" 키로 반환)
     */
    public Map<String, List<Map<String, Object>>> groupDataByDetectKey(
            List<Map<String, Object>> dataForDetection,
            EngineProfileEntity profile) {

        // TODO: groupKey 컬럼 제거됨 - entity_id_field로 임시 대체
        String entityIdField = profile.getEntityIdField();
        if (entityIdField == null || entityIdField.isEmpty()) {
            // entity_id_field가 없으면 전체 데이터를 하나의 그룹으로
            return Map.of("default", dataForDetection);
        }

        // ProfileSchemaMappingEngine을 사용하여 그룹화
        return profileSchemaMappingEngine.groupByGroupKey(dataForDetection, profile.getProfileId());
    }
}
