package com.itmasters.icon.engine.processor.detect.evaluate;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineExecutionWarningEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineExecutionWarningRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EventStreamGroupRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EventStreamRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.JpaRuleRepository;
import com.itmasters.icon.engine.dto.RuleEvaluationResult;
import com.itmasters.icon.engine.dto.RuleExecutionResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.itmasters.icon.engine.processor.RuleProcessor;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DET-2-2: 집계 평가 프로세서 (Aggregate Evaluation)
 * 
 * 책임:
 * - detect_aggregates 테이블 적재
 * - aggregate별 group_by_fields 조합으로 집계 수행
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Det2_2_RuleEvaluationProcessor {

    private final EventStreamRepository eventStreamRepository;
    private final EventStreamGroupRepository eventStreamGroupRepository;
    private final JpaRuleRepository ruleRepository;
    private final RuleProcessor ruleProcessor;
    private final EngineExecutionWarningRepository warningRepository;

    /**
     * 특정 exec_ds_mp 실행 내에서 모든 활성 집계를 평가
     * - 각 aggregate의 group_by_fields 기준으로 distinct 조합을 조회하여 집계 수행
     * 
     * @return 실제 저장된 룰 건수와 집계 건수
     */
    // public RuleEvaluationResult evaluateByExec(Long execDsMpId) {
    // log.info("========== DET-2-2 시작 (집계 평가) ==========");
    // log.info("🔍 [DET-2-2-DEBUG] ExecDsMpId: {}", execDsMpId);
    //
    // // 모든 활성 집계 조회
    // List<RuleEntity> activeAggregates = ruleRepository.findByIsActiveTrue();
    // if (activeAggregates == null || activeAggregates.isEmpty()) {
    // log.warn("🔍 [DET-2-2-DEBUG] ❌ 활성 집계가 없습니다.");
    // saveWarning(execDsMpId, "NO_ACTIVE_AGGREGATES", "No active aggregates
    // found");
    // log.info("========== DET-2-2 완료 (집계 평가) - 처리 없음 ==========");
    // return RuleEvaluationResult.empty();
    // }
    //
    // log.info("🔍 [DET-2-2-DEBUG] 활성 집계 {} 개 발견", activeAggregates.size());
    // for (RuleEntity agg : activeAggregates) {
    // log.info("🔍 [DET-2-2-DEBUG] - 집계: ruleId={}, predicateSensorId={},
    // groupByFields={}",
    // agg.getRuleId(), agg.getPredicateSensorId(),
    // agg.getGroupByFields() != null ? String.join(",", agg.getGroupByFields()) :
    // "NULL");
    // }
    // int totalSavedSensors = 0;
    // int totalSavedRules = 0;
    //
    // // 각 집계별로 처리
    // for (RuleEntity aggregate : activeAggregates) {
    // try {
    // RuleEvaluationResult result = processRule(execDsMpId, aggregate);
    // totalSavedSensors += result.savedSensors();
    // totalSavedRules += result.savedRules();
    // } catch (Exception e) {
    // log.error("집계 처리 실패 - ruleId={}, execDsMpId={}",
    // aggregate.getRuleId(), execDsMpId, e);
    // }
    // }
    //
    // log.info("========== DET-2-2 완료 (집계 평가) - 센서 {} 건, 룰 {} 건 탐지 ==========",
    // totalSavedSensors, totalSavedRules);
    // return new RuleEvaluationResult(totalSavedSensors, totalSavedRules);
    // }

    /**
     * 단일 집계에 대해 모든 필드 조합을 처리
     */
    // private RuleEvaluationResult processRule(Long execDsMpId, RuleEntity
    // aggregate) {
    // log.info("🔍 [PROCESS-AGG-DEBUG] processAggregate called: ruleId={},
    // execDsMpId={}",
    // aggregate.getRuleId(), execDsMpId);
    //
    // String[] groupByFields = aggregate.getGroupByFields();
    // log.info("🔍 [PROCESS-AGG-DEBUG] groupByFields: {}",
    // groupByFields != null ? String.join(",", groupByFields) : "NULL");
    //
    // // group_by_fields가 없으면 스킵
    // if (groupByFields == null || groupByFields.length == 0) {
    // log.warn("🔍 [PROCESS-AGG-DEBUG] ❌ 집계 {}에 group_by_fields가 없어 스킵합니다.",
    // aggregate.getRuleId());
    // return RuleEvaluationResult.empty();
    // }
    //
    // // 필드 리스트 변환
    // List<String> fields = Arrays.asList(groupByFields);
    // log.info("🔍 [PROCESS-AGG-DEBUG] 집계 {} 처리 시작 - fields: {}, evaluation_mode:
    // {}, predicateSensorId: {}",
    // aggregate.getRuleId(), fields, aggregate.getEvaluationMode(),
    // aggregate.getPredicateSensorId());
    //
    // // evaluation_mode 체크: SINGLE_ROW vs WINDOW
    // String evaluationMode = aggregate.getEvaluationMode();
    // if ("SINGLE_ROW".equals(evaluationMode)) {
    // // SINGLE_ROW 모드는 window 없이 처리
    // log.info("🚀 SINGLE_ROW 모드 - window 조회 없이 exec_ds_mp_id 기반 처리");
    // return processSingleRowAggregate(execDsMpId, aggregate, fields);
    // }
    //
    // // 집계 기간 계산 (window_minutes 기준) - WINDOW 모드
    // Integer windowMinutes = aggregate.getWindowMinutes();
    // if (windowMinutes == null || windowMinutes <= 0) {
    // log.warn("집계 {}의 window_minutes가 유효하지 않습니다.", aggregate.getRuleId());
    // return RuleEvaluationResult.empty();
    // }
    //
    // // 현재 실행(exec_ds_mp_id)의 event_stream 최신 이벤트 시간(event_dt) 기준으로 window 기간 계산
    // LocalDateTime latestEventTime =
    // eventStreamRepository.findLatestEventTimeByExecDsMpId(execDsMpId)
    // .orElse(LocalDateTime.now());
    // LocalDateTime endDate = latestEventTime;
    // LocalDateTime startDate = endDate.minusMinutes(windowMinutes);
    //
    // log.info("집계 기간: {} ~ {} (execDsMpId={}, 최신 이벤트 시간: {})",
    // startDate, endDate, execDsMpId, latestEventTime);
    //
    // // event_stream_groups 테이블에서 해당 집계의 distinct group_key 조회 (인덱스 활용)
    // log.info("🔍 [PROCESS-AGG-DEBUG] Querying event_stream_groups: ruleId={},
    // startDate={}, endDate={}",
    // aggregate.getRuleId(), startDate, endDate);
    //
    // List<String> groupKeys =
    // eventStreamGroupRepository.findDistinctGroupKeysByAggregateIdAndDateRange(
    // aggregate.getRuleId(),
    // startDate,
    // endDate
    // );
    //
    // log.info("🔍 [PROCESS-AGG-DEBUG] Query result: {} group_keys found",
    // groupKeys != null ? groupKeys.size() : "NULL");
    //
    // if (groupKeys == null || groupKeys.isEmpty()) {
    // log.warn("🔍 [PROCESS-AGG-DEBUG] ❌ 집계 {}에 대한 그룹 키가 없습니다. (기간: {} ~ {})",
    // aggregate.getRuleId(), startDate, endDate);
    // return RuleEvaluationResult.empty();
    // }
    //
    // log.info("🔍 [PROCESS-AGG-DEBUG] ✅ 집계 {} - {} 개 그룹 키 발견 (기간: {} ~ {})",
    // aggregate.getRuleId(), groupKeys.size(), startDate, endDate);
    // for (String gk : groupKeys) {
    // log.info("🔍 [PROCESS-AGG-DEBUG] - group_key: {}", gk);
    // }
    //
    // // group_key를 Map<String, String> 조합으로 변환
    // List<Map<String, String>> combinations = groupKeys.stream()
    // .map(groupKey -> parseGroupKey(groupKey, groupByFields))
    // .collect(Collectors.toList());
    //
    // // 각 조합별로 집계 실행 및 결과 누적
    // int totalSavedSensors = 0;
    // int totalSavedRules = 0;
    // int processed = 0;
    //
    // log.info("🔍 [PROCESS-AGG-DEBUG] Starting to process {} combinations for
    // ruleId={}",
    // combinations.size(), aggregate.getRuleId());
    //
    // for (Map<String, String> combination : combinations) {
    // try {
    // RuleExecutionResult result = ruleProcessor.execute(combination, execDsMpId,
    // aggregate);
    // totalSavedSensors += result.savedSensors();
    // totalSavedRules += result.savedRules();
    // processed++;
    // } catch (Exception e) {
    // log.error("🔍 [PROCESS-AGG-DEBUG] ❌ 집계 실행 실패 - ruleId={}, combination={},
    // execDsMpId={}",
    // aggregate.getRuleId(), combination, execDsMpId, e);
    // }
    // }
    //
    // log.info("집계 {} 처리 완료 - {} 개 조합 처리, 센서 {} 건, 룰 {} 건 저장됨",
    // aggregate.getRuleId(), processed, totalSavedSensors, totalSavedRules);
    // return new RuleEvaluationResult(totalSavedSensors, totalSavedRules);
    // }

    /**
     * 경고 저장 헬퍼 메서드
     */
    // private void saveWarning(Long execDsMpId, String code, String message) {
    // try {
    // warningRepository.save(EngineExecutionWarningEntity.builder()
    // .execDsMpId(execDsMpId)
    // .step("STEP5_2")
    // .code(code)
    // .message(message)
    // .createdAt(LocalDateTime.now())
    // .build());
    // log.warn("DET-2-2 warning saved: {} - execDsMpId={}", code, execDsMpId);
    // } catch (Exception e) {
    // log.warn("Failed to save DET-2-2 warning for execDsMpId={}: {}", execDsMpId,
    // e.getMessage());
    // }
    // }

    /**
     * group_key 문자열을 Map<String, String> 조합으로 파싱
     *
     * group_key 형식: "value1|value2|value3"
     * groupByFields: ["field1", "field2", "field3"]
     *
     * @param groupKey      "|"로 구분된 그룹 키 문자열
     * @param groupByFields 필드명 배열
     * @return 필드명과 값의 매핑
     */
    private Map<String, String> parseGroupKey(String groupKey, String[] groupByFields) {
        Map<String, String> combination = new HashMap<>();

        if (groupKey == null || groupByFields == null) {
            return combination;
        }

        String[] values = groupKey.split("\\|", -1); // -1: keep empty strings

        if (values.length != groupByFields.length) {
            log.warn("group_key 파싱 실패 - 필드 개수 불일치: groupKey='{}', fields={}, expected={}, actual={}",
                    groupKey, Arrays.toString(groupByFields), groupByFields.length, values.length);
            return combination;
        }

        for (int i = 0; i < groupByFields.length; i++) {
            combination.put(groupByFields[i], values[i]);
        }

        return combination;
    }

    /**
     * 특정 mappedStorageId 기반으로 모든 활성 집계를 평가
     * - exec_ds_mp_id 대신 개별 mapped_storage_id 기준으로 처리
     * - MainEngineService에서 각 mappedStorageId별 루프 호출용
     *
     * @param mappedStorageId mapped_storages 테이블의 ID
     * @return 실제 저장된 센서 건수와 룰 건수
     */
    @Transactional
    public RuleEvaluationResult evaluateByMappedStorageId(Long mappedStorageId) {
        log.info("========== DET-2-2 시작 (집계 평가 - mappedStorageId 기반) ==========");
        log.info("[DET-2-2] mappedStorageId: {}", mappedStorageId);

        // 모든 활성 집계 조회
        List<RuleEntity> activeRules = ruleRepository.findByIsActiveTrue();
        if (activeRules == null || activeRules.isEmpty()) {
            log.warn("[DET-2-2] 활성 집계가 없습니다.");
            log.info("========== DET-2-2 완료 (집계 평가) - 처리 없음 ==========");
            return RuleEvaluationResult.empty();
        }

        log.info("[DET-2-2] 활성 집계 {} 개 발견", activeRules.size());

        int totalSavedSensors = 0;
        int totalSavedRules = 0;

        // 각 집계별로 처리
        for (RuleEntity aggregate : activeRules) {
            try {
                RuleEvaluationResult result = processAggregateByMappedStorageId(mappedStorageId, aggregate);
                totalSavedSensors += result.savedSensors();
                totalSavedRules += result.savedRules();
            } catch (Exception e) {
                log.error("집계 처리 실패 - ruleId={}, mappedStorageId={}",
                        aggregate.getRuleId(), mappedStorageId, e);
            }
        }

        log.info("========== DET-2-2 완료 (집계 평가 - mappedStorageId 기반) - 센서 {} 건, 룰 {} 건 탐지 ==========",
                totalSavedSensors, totalSavedRules);
        return new RuleEvaluationResult(totalSavedSensors, totalSavedRules);
    }

    /**
     * 단일 집계에 대해 mappedStorageId 기반으로 모든 필드 조합을 처리
     *
     * @param mappedStorageId mapped_storages 테이블의 ID
     * @param aggregate       집계 정의
     * @return 집계 평가 결과
     */
    public RuleEvaluationResult processAggregateByMappedStorageId(Long mappedStorageId, RuleEntity aggregate) {
        log.debug("[PROCESS-AGG] processAggregateByMappedStorageId: ruleId={}, mappedStorageId={}",
                aggregate.getRuleId(), mappedStorageId);

        String[] groupByFields = aggregate.getGroupByFields();

        // group_by_fields가 없으면 스킵
        if (groupByFields == null || groupByFields.length == 0) {
            log.warn("[PROCESS-AGG] 집계 {}에 group_by_fields가 없어 스킵합니다.", aggregate.getRuleId());
            return RuleEvaluationResult.empty();
        }

        List<String> fields = Arrays.asList(groupByFields);
        log.info("[PROCESS-AGG] 집계 {} 처리 시작 - fields: {}, evaluation_mode: {}",
                aggregate.getRuleId(), fields, aggregate.getEvaluationMode());

        // evaluation_mode 체크: SINGLE_ROW vs WINDOW
        String evaluationMode = aggregate.getEvaluationMode();
        if ("SINGLE_ROW".equals(evaluationMode)) {
            log.info("SINGLE_ROW 모드 - mappedStorageId 기반 처리");
            return processSingleRowAggregateByMappedStorageId(mappedStorageId, aggregate, fields);
        }

        // WINDOW 모드: mappedStorageId 기반 window 집계
        Integer windowMinutes = aggregate.getWindowMinutes();
        if (windowMinutes == null || windowMinutes <= 0) {
            log.warn("집계 {}의 window_minutes가 유효하지 않습니다.", aggregate.getRuleId());
            return RuleEvaluationResult.empty();
        }

        // mappedStorageId 기준으로 event_stream_groups에서 distinct group_key 조회
        List<String> groupKeys = eventStreamGroupRepository.findDistinctGroupKeysByMappedStorageIdAndAggregateId(
                mappedStorageId,
                aggregate.getRuleId());

        if (groupKeys == null || groupKeys.isEmpty()) {
            log.debug("[PROCESS-AGG] 집계 {}에 대한 그룹 키가 없습니다. (mappedStorageId: {})",
                    aggregate.getRuleId(), mappedStorageId);
            return RuleEvaluationResult.empty();
        }

        log.info("[PROCESS-AGG] 집계 {} - {} 개 그룹 키 발견 (mappedStorageId: {})",
                aggregate.getRuleId(), groupKeys.size(), mappedStorageId);

        // group_key를 Map<String, String> 조합으로 변환
        List<Map<String, String>> combinations = groupKeys.stream()
                .map(groupKey -> parseGroupKey(groupKey, groupByFields))
                .collect(Collectors.toList());

        // 각 조합별로 집계 실행 및 결과 누적
        int totalSavedSensors = 0;
        int totalSavedRules = 0;
        int processed = 0;

        for (Map<String, String> combination : combinations) {
            try {
                RuleExecutionResult result = ruleProcessor.executeByMappedStorageId(
                        combination, mappedStorageId, aggregate);
                totalSavedSensors += result.savedSensors();
                totalSavedRules += result.savedRules();
                processed++;
            } catch (Exception e) {
                log.error("[PROCESS-AGG] 집계 실행 실패 - ruleId={}, combination={}, mappedStorageId={}",
                        aggregate.getRuleId(), combination, mappedStorageId, e);
            }
        }

        log.info("집계 {} 처리 완료 - {} 개 조합 처리, 센서 {} 건, 룰 {} 건 저장됨",
                aggregate.getRuleId(), processed, totalSavedSensors, totalSavedRules);
        return new RuleEvaluationResult(totalSavedSensors, totalSavedRules);
    }

    /**
     * SINGLE_ROW 모드 집계 처리 (mappedStorageId 기반)
     * - mapped_storage_id 기반으로 현재 실행의 이벤트만 조회
     * - event_stream_groups에서 distinct group_key 조회
     *
     * @param mappedStorageId mapped_storages 테이블의 ID
     * @param aggregate       집계 정의
     * @param fields          그룹핑 필드 리스트
     * @return 집계 평가 결과
     */
    private RuleEvaluationResult processSingleRowAggregateByMappedStorageId(
            Long mappedStorageId,
            RuleEntity aggregate,
            List<String> fields) {

        log.info("SINGLE_ROW 집계 처리 (mappedStorageId 기반) - ruleId: {}, mappedStorageId: {}",
                aggregate.getRuleId(), mappedStorageId);

        // mappedStorageId 기준으로 event_stream_groups에서 distinct group_key 조회
        List<String> groupKeys = eventStreamGroupRepository.findDistinctGroupKeysByMappedStorageIdAndAggregateId(
                mappedStorageId,
                aggregate.getRuleId());

        if (groupKeys == null || groupKeys.isEmpty()) {
            log.debug("SINGLE_ROW 집계 {} - mappedStorageId {} 에 대한 그룹 키가 없습니다.",
                    aggregate.getRuleId(), mappedStorageId);
            return RuleEvaluationResult.empty();
        }

        log.info("SINGLE_ROW 집계 {} - {} 개 그룹 키 발견 (mappedStorageId: {})",
                aggregate.getRuleId(), groupKeys.size(), mappedStorageId);

        // group_key를 Map<String, String> 조합으로 변환
        List<Map<String, String>> combinations = groupKeys.stream()
                .map(groupKey -> parseGroupKey(groupKey, aggregate.getGroupByFields()))
                .collect(Collectors.toList());

        // 각 조합별로 집계 실행
        int totalSavedSensors = 0;
        int totalSavedRules = 0;
        int processed = 0;

        for (Map<String, String> combination : combinations) {
            try {
                RuleExecutionResult result = ruleProcessor.executeByMappedStorageId(
                        combination, mappedStorageId, aggregate);
                totalSavedSensors += result.savedSensors();
                totalSavedRules += result.savedRules();
                processed++;
            } catch (Exception e) {
                log.error("SINGLE_ROW 집계 실행 실패 - ruleId={}, combination={}, mappedStorageId={}",
                        aggregate.getRuleId(), combination, mappedStorageId, e);
                throw e;
            }
        }

        log.info("SINGLE_ROW 집계 {} 처리 완료 - {} 개 조합 처리, 센서 {} 건, 룰 {} 건 저장됨",
                aggregate.getRuleId(), processed, totalSavedSensors, totalSavedRules);
        return new RuleEvaluationResult(totalSavedSensors, totalSavedRules);
    }

    /**
     * SINGLE_ROW 모드 집계 처리
     * - exec_ds_mp_id 기반으로 현재 실행의 이벤트만 조회
     * - event_stream_groups에서 distinct group_key 조회
     *
     * @param execDsMpId 실행 컨텍스트 ID
     * @param aggregate  집계 정의
     * @param fields     그룹핑 필드 리스트
     * @return 집계 평가 결과
     */
    // private RuleEvaluationResult processSingleRowAggregate(
    // Long execDsMpId,
    // RuleEntity aggregate,
    // List<String> fields) {
    //
    // log.info("🚀 SINGLE_ROW 집계 처리 - ruleId: {}, execDsMpId: {}",
    // aggregate.getRuleId(), execDsMpId);
    //
    // // exec_ds_mp_id 기준으로 event_stream_groups에서 distinct group_key 조회
    // List<String> groupKeys =
    // eventStreamGroupRepository.findDistinctGroupKeysByExecDsMpIdAndAggregateId(
    // execDsMpId,
    // aggregate.getRuleId()
    // );
    //
    // if (groupKeys == null || groupKeys.isEmpty()) {
    // log.debug("SINGLE_ROW 집계 {} - execDsMpId {} 에 대한 그룹 키가 없습니다.",
    // aggregate.getRuleId(), execDsMpId);
    // return RuleEvaluationResult.empty();
    // }
    //
    // log.info("SINGLE_ROW 집계 {} - {} 개 그룹 키 발견 (execDsMpId: {})",
    // aggregate.getRuleId(), groupKeys.size(), execDsMpId);
    //
    // // group_key를 Map<String, String> 조합으로 변환
    // List<Map<String, String>> combinations = groupKeys.stream()
    // .map(groupKey -> parseGroupKey(groupKey, aggregate.getGroupByFields()))
    // .collect(Collectors.toList());
    //
    // // 각 조합별로 집계 실행
    // int totalSavedSensors = 0;
    // int totalSavedRules = 0;
    // int processed = 0;
    //
    // for (Map<String, String> combination : combinations) {
    // try {
    // RuleExecutionResult result = ruleProcessor.execute(combination, execDsMpId,
    // aggregate);
    // totalSavedSensors += result.savedSensors();
    // totalSavedRules += result.savedRules();
    // processed++;
    // } catch (Exception e) {
    // log.error("SINGLE_ROW 집계 실행 실패 - ruleId={}, combination={}, execDsMpId={}",
    // aggregate.getRuleId(), combination, execDsMpId, e);
    // }
    // }
    //
    // log.info("SINGLE_ROW 집계 {} 처리 완료 - {} 개 조합 처리, 센서 {} 건, 룰 {} 건 저장됨",
    // aggregate.getRuleId(), processed, totalSavedSensors, totalSavedRules);
    // return new RuleEvaluationResult(totalSavedSensors, totalSavedRules);
    // }
}
