package com.itmasters.icon.engine.processor;

import com.itmasters.icon.engine.adapter.out.persistence.entity.*;
import com.itmasters.icon.engine.adapter.out.persistence.repository.*;
import com.itmasters.icon.engine.processor.support.SensorDetectionRecorder;
import com.itmasters.icon.engine.processor.support.RuleDetectionRecorder;
import com.itmasters.icon.engine.repository.RuleRepository;
import com.itmasters.icon.engine.processor.support.SensorPredicateFilter;
import com.itmasters.icon.common.domain.aggregate.AggregateOperator;
import com.itmasters.icon.engine.processor.support.AggregationEvaluator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator; // For SEQUENCE_WITHIN sorting
import java.util.Objects; // For DISTINCT_COUNT_WITHIN filter
import java.util.stream.Collectors; // For Collectors.toList()
import java.math.MathContext; // For AVG_WITHIN precision

import com.itmasters.icon.engine.dto.AggregationResult;
import com.itmasters.icon.engine.dto.RuleExecutionResult;

import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 집계 처리기
 * - 집계 정의(Aggregates)를 읽어와서, 조건에 맞는 집계 결과를 생성하고 저장합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleProcessor {

    private final JpaDetectRuleRepository detectRuleRepository;
    private final EventStreamRepository eventStreamRepository;
    private final EngineRuleRepository engineRuleRepository;
    private final SensorRepository sensorRepository; // For sensor lookup
    private final EventStreamGroupRepository eventStreamGroupRepository; // For group_key lookup
    private final AggregationEvaluator aggregationEvaluator;
    private final SensorPredicateFilter sensorPredicateFilter;
    private final SensorDetectionRecorder sensorDetectionRecorder;
    private final RuleDetectionRecorder ruleDetectionRecorder;


    /**
     * 집계 정의에 따라 집계를 실행합니다. (mappedStorageId 기반)
     * - mapped_storage_id를 직접 사용하여 exec_ds_mp_id 변환 없이 처리
     *
     * @param groupKeyValues  그룹핑 필드들의 값
     * @param mappedStorageId mapped_storages 테이블의 ID
     * @param aggregate       집계 정의
     * @return 실행 결과 (저장된 룰 건수, 저장된 집계 건수)
     */
    public RuleExecutionResult executeByMappedStorageId(Map<String, String> groupKeyValues, Long mappedStorageId, RuleEntity aggregate) {
        log.info("Executing RuleProcessor (by mappedStorageId) for groupKeyValues: {}, mappedStorageId: {}, ruleId: {}",
                groupKeyValues, mappedStorageId, aggregate.getRuleId());

        // evaluation_mode 체크: SINGLE_ROW vs WINDOW
        String evaluationMode = aggregate.getEvaluationMode();
        if ("SINGLE_ROW".equals(evaluationMode)) {
            log.info("🚀 SINGLE_ROW mode - 단일 행 평가 (window 조회 없음) - ruleId={}", aggregate.getRuleId());
            return executeSingleRowByMappedStorageId(groupKeyValues, mappedStorageId, aggregate);
        }

        // WINDOW 모드는 기존 execute 메서드와 동일한 로직 (시간 기반 조회)
        // 단, 저장 시 mappedStorageId 정보를 활용
        return executeWindowMode(groupKeyValues, mappedStorageId, aggregate);
    }

    /**
     * 집계 정의에 따라 집계를 실행합니다.
     *
     * @param groupKeyValues 그룹핑 필드들의 값 (예: {login_id: EMP010} 또는 {login_id: EMP010, account_number: 123})
     * @param execDsMpId     실행 컨텍스트 ID
     * @param ruleEntity     집계 정의
     * @return 실행 결과 (저장된 룰 건수, 저장된 집계 건수)
     */
    public RuleExecutionResult execute(Map<String, String> groupKeyValues, Long execDsMpId, RuleEntity ruleEntity) {
        log.info("Executing RuleProcessor for groupKeyValues: {}, ruleId: {}",
                groupKeyValues, ruleEntity.getRuleId());

        // evaluation_mode 체크: SINGLE_ROW vs WINDOW
        String evaluationMode = ruleEntity.getEvaluationMode();
        if ("SINGLE_ROW".equals(evaluationMode)) {
            log.info("🚀 SINGLE_ROW mode - 단일 행 평가 (window 조회 없음) - ruleId={}", ruleEntity.getRuleId());
            return executeSingleRow(groupKeyValues, execDsMpId, ruleEntity);
        }

        // WINDOW 모드 (기존 로직)
        if (ruleEntity.getWindowMinutes() == null || ruleEntity.getWindowMinutes() <= 0) {
            log.warn("Skip ruleEntity without valid window_minutes - ruleId={}", ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        // groupKeyValues로부터 group_key 문자열 생성 (저장/표시용)
        String groupKey = buildGroupKeyString(groupKeyValues);

        // 타임라인 로드 - 이벤트 시간 기준으로 조회 (시뮬레이션/배치 처리 지원)
        // 1) 먼저 해당 execDsMpId의 이벤트 시간 범위를 조회
        int lookupWindowMinutes = ruleEntity.getWindowMinutes();

        // 전체 event_stream의 최신 이벤트 시간 조회 (시뮬레이션/과거 데이터 지원)
        Optional<LocalDateTime> execMaxEventTimeOpt = eventStreamRepository.findLatestEventTime();
        if (execMaxEventTimeOpt.isEmpty()) {
            log.info("⚠️ No events found in event_stream, skip ruleEntity {}",
                    ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }
        LocalDateTime execMaxEventTime = execMaxEventTimeOpt.get();

        // execDsMpId의 최신 이벤트 시각 기준 30일 전부터 조회 (시뮬레이션 지원: exec 필터 미사용)
        LocalDateTime maxLookback = execMaxEventTime.minusDays(30);
        List<EngineEventStreamEntity> recentEvents = eventStreamRepository.findByFieldValuesAndEventDtBetween(
                groupKeyValues, maxLookback, execMaxEventTime.plusDays(1));

        if (recentEvents == null || recentEvents.isEmpty()) {
            log.info("⚠️ No events for groupKeyValues={} within 30 days, skip ruleEntity {}",
                    groupKeyValues, ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        // 최신순 정렬
        recentEvents.sort((a, b) -> b.getEventDt().compareTo(a.getEventDt()));

        // 최신 이벤트 시간 기준으로 윈도우 계산
        LocalDateTime latestEventTime = recentEvents.get(0).getEventDt();
        LocalDateTime lookupStart = latestEventTime.minusMinutes(lookupWindowMinutes);

        // 이벤트 시간 기준 윈도우 내 이벤트만 필터링
        List<EngineEventStreamEntity> allByGroupDesc = recentEvents.stream()
                .filter(e -> !e.getEventDt().isBefore(lookupStart) && !e.getEventDt().isAfter(latestEventTime))
                .collect(Collectors.toList());

        if (allByGroupDesc.isEmpty()) {
            log.info("⚠️ No events for groupKeyValues={} within {}min window from latest event, skip ruleEntity {}",
                    groupKeyValues, lookupWindowMinutes, ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        log.debug("Found {} events for groupKey={} in window [{} ~ {}]",
                allByGroupDesc.size(), groupKey, lookupStart, latestEventTime);

        LocalDateTime anchorCandidate = determineAnchor(ruleEntity, allByGroupDesc);
        if (anchorCandidate == null) {
            log.info("⚠️ No anchor found by rule for groupKey={}, ruleId={}", groupKey, ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        // 윈도우 방향 결정: ABSENCE_WITHIN은 앵커 이후, 나머지는 앵커 이전
        LocalDateTime startTime;
        LocalDateTime endTime;

        if (ruleEntity.getOperator() == AggregateOperator.ABSENCE_WITHIN) {
            // ABSENCE_WITHIN: 앵커 이후 윈도우 (예: "신용등급 하락 후 7일 내 매도 부재")
            startTime = anchorCandidate;
            endTime = startTime.plusMinutes(ruleEntity.getWindowMinutes());
            log.info("🔍 [ABSENCE_WITHIN] Window: [{} ~ {}] (anchor AFTER)", startTime, endTime);
        } else {
            // COUNT/SUM/AVG/SEQUENCE: 앵커 이전 윈도우 (과거 집계)
            endTime = anchorCandidate;
            startTime = endTime.minusMinutes(ruleEntity.getWindowMinutes());
            log.info("🔍 [{}] Window: [{} ~ {}] (anchor BEFORE)", ruleEntity.getOperator(), startTime, endTime);
        }

        // 집계 창: [start, anchor] (end 포함) - recentEvents에서 필터링
        List<EngineEventStreamEntity> events = recentEvents.stream()
                .filter(e -> !e.getEventDt().isBefore(startTime) && !e.getEventDt().isAfter(endTime))
                .collect(Collectors.toList());

        // ✅ pass 체크 전에 predicate_sensor_id 센서 저장 (집계와 무관하게 항상 저장)
        // - 센서 탐지 여부를 추적하기 위해 pass 성공 여부와 무관하게 저장
        // - 디버깅 및 분석 시 센서 탐지 이력 확인 가능
        String predicateSensorId = ruleEntity.getPredicateSensorId();
        if (predicateSensorId != null && !predicateSensorId.isBlank()) {
            List<EngineEventStreamEntity> matchedEvents = sensorPredicateFilter.filter(predicateSensorId, events, ruleEntity);
            sensorDetectionRecorder.saveIfSensor(predicateSensorId, matchedEvents);
            log.info("✅ [SENSOR-EARLY-SAVE] Saved predicate_sensor_id={} detections BEFORE aggregation (pass-independent)", predicateSensorId);
        }

        // 4. 집계 연산을 수행합니다.
        AggregationResult aggregationResult = aggregationEvaluator.evaluate(ruleEntity, events, endTime);

        // 5. 결과를 저장합니다。
        // 앵커 시간: 결과에서 제공되면 사용, 없으면 endTime
        LocalDateTime anchor = aggregationResult.getDetectedAt() != null ? aggregationResult.getDetectedAt() : endTime;

        // pass가 아닌 경우 저장하지 않음 (정책: pass 1건만 적재)
        if (!aggregationResult.isPass()) {
            log.info("⚠️ Aggregate NOT passed (value={}, threshold={}) - groupKey={}, ruleId={}",
                    aggregationResult.getMatchedValue(), ruleEntity.getThresholdCount(), groupKey, ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        // actualGroupKey가 있으면 사용 (group_by_fields 기준), 없으면 기존 groupKey 사용
        String effectiveGroupKey = aggregationResult.getActualGroupKey() != null
                ? aggregationResult.getActualGroupKey()
                : groupKey;
        if (aggregationResult.getActualGroupKey() != null) {
            log.info("Using actualGroupKey={} instead of original groupKey={} for ruleEntity {}",
                    effectiveGroupKey, groupKey, ruleEntity.getRuleId());
        }

        // dedup 체크 1: 동일 (mapped_storage_id, rule_id) 존재 시 스킵
        Long anchorMappedStorageId = aggregationResult.getAnchorMappedStorageId();
        if (anchorMappedStorageId != null &&
                detectRuleRepository.existsByMappedStorageIdAndRuleId(anchorMappedStorageId, ruleEntity.getRuleId())) {
            log.info("⚠️ Skip duplicate detect_rules - mappedStorageId={}, ruleId={}", anchorMappedStorageId, ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        // dedup 체크 2: dedup_minutes 내에 같은 (group_key, aggregate_id)로 탐지된 적 있으면 스킵
        Integer dedupMinutes = ruleEntity.getDedupMinutes();
        if (dedupMinutes != null && dedupMinutes > 0) {
            LocalDateTime dedupStart = anchor.minusMinutes(dedupMinutes);
            if (detectRuleRepository.existsByGroupKeyAndRuleIdAndDetectedDtAfter(
                    effectiveGroupKey, ruleEntity.getRuleId(), dedupStart)) {
                log.info("⚠️ Skip by dedup_minutes - groupKey={}, ruleId={}, dedupMinutes={}, anchor={}",
                        effectiveGroupKey, ruleEntity.getRuleId(), dedupMinutes, anchor);
                return RuleExecutionResult.empty();
            }
        }

        // 집계 pass된 경우에만 predicate_sensor_id가 룰인 경우 저장
        // (predicate_sensor_id가 센서인 경우는 위에서 pass 체크 전에 이미 저장됨)
        int savedRulesCount = 0;
        String predicateSensorIdForRule = ruleEntity.getPredicateSensorId();
        if (predicateSensorIdForRule != null && !predicateSensorIdForRule.isBlank()) {
            SensorEntity sensor = sensorRepository.findBySensorId(predicateSensorIdForRule).orElse(null);
            if (sensor == null) {
                log.debug("Predicate {} is another rule; skipping additional detect_rule persistence", predicateSensorIdForRule);
            } else {
                log.debug("Predicate {} is a sensor, already saved before aggregation (pass-independent)", predicateSensorIdForRule);
            }
        }

        // originalGroupKey: actualGroupKey가 사용된 경우 원본 groupKey 저장
        // analytics에서 event_stream 조회 시 원본 키로 검색 가능하도록
        String originalGroupKey = aggregationResult.getActualGroupKey() != null ? groupKey : null;

        DetectRuleEntity result = DetectRuleEntity.builder()
                .groupKey(effectiveGroupKey)
                .originalGroupKey(originalGroupKey)
                .ruleId(ruleEntity.getRuleId())
                .operator(ruleEntity.getOperator() != null ? ruleEntity.getOperator().name() : null)
                .windowMinutes(ruleEntity.getWindowMinutes())
                .dedupMinutes(ruleEntity.getDedupMinutes())
                .startDt(startTime)
                .endDt(endTime)
                .detectedDt(LocalDateTime.now())  // 탐지 실행 시간
                .eventDt(aggregationResult.getDetectedAt() != null ? aggregationResult.getDetectedAt() : endTime)  // 앵커 이벤트 발생 시간
                .mappedStorageId(aggregationResult.getAnchorMappedStorageId())
                .transactionId(aggregationResult.getTransactionId())
                .pass(aggregationResult.isPass())
                .matchedCount(aggregationResult.getMatchedValue()) // Use matchedValue for count/sum
                .thresholdCount(ruleEntity.getThresholdCount())
                .build();

        ruleDetectionRecorder.save(result);

        log.info("RuleProcessor execution finished for groupKeyValues: {} - savedRules={}, savedAggregate=1",
                groupKeyValues, savedRulesCount);

        return new RuleExecutionResult(savedRulesCount, 1);
    }

    /**
     * 프레디킷/시퀀스 규칙으로 앵커(평가 기준 이벤트 시각)를 결정
     */
    private LocalDateTime determineAnchor(RuleEntity definition, List<EngineEventStreamEntity> eventsDesc) {
        AggregateOperator operator = definition.getOperator();
        if (operator == null) return null;

        if (operator == AggregateOperator.SEQUENCE_WITHIN) {
            String prevSensorId = definition.getPrevSensorId();
            String nextSensorId = definition.getNextSensorId();
            Integer w = definition.getWindowMinutes();
            if (prevSensorId == null || nextSensorId == null || w == null || w <= 0) return null;

            List<EngineEventStreamEntity> prevEvents = sensorPredicateFilter.filter(prevSensorId, eventsDesc, definition);
            // ✅ prev_sensor_id 센서 탐지 결과 저장
            sensorDetectionRecorder.saveIfSensor(prevSensorId, prevEvents);

            List<EngineEventStreamEntity> nextEvents = sensorPredicateFilter.filter(nextSensorId, eventsDesc, definition);
            // ✅ next_sensor_id 센서 탐지 결과 저장
            sensorDetectionRecorder.saveIfSensor(nextSensorId, nextEvents);

            for (EngineEventStreamEntity next : nextEvents) { // 최신순
                LocalDateTime nd = next.getEventDt();
                LocalDateTime since = nd.minusMinutes(w);
                boolean hasPrev = prevEvents.stream().anyMatch(p ->
                        !p.getEventDt().isBefore(since) && p.getEventDt().isBefore(nd)
                );
                if (hasPrev) return nd;
            }
            return null;
        }

        // ABSENCE_WITHIN: 앵커는 anchor_sensor_id(있으면) 또는 predicate_sensor_id로 필터링된 최신 이벤트
        if (operator == AggregateOperator.ABSENCE_WITHIN) {
            String anchorRule = definition.getAnchorSensorId();
            if (anchorRule != null && !anchorRule.isBlank()) {
                // anchor_sensor_id가 명시적으로 설정된 경우
                List<EngineEventStreamEntity> anchors = sensorPredicateFilter.filter(anchorRule, eventsDesc, definition);
                // ✅ anchor_sensor_id 센서 탐지 결과 저장
                sensorDetectionRecorder.saveIfSensor(anchorRule, anchors);

                return anchors.isEmpty() ? null : anchors.get(0).getEventDt();
            }

            // anchor_sensor_id가 없으면 predicate_sensor_id로 필터링된 이벤트를 앵커로 사용
            String predicateSensorId = definition.getPredicateSensorId();
            if (predicateSensorId != null && !predicateSensorId.isBlank()) {
                List<EngineEventStreamEntity> predicateEvents = sensorPredicateFilter.filter(predicateSensorId, eventsDesc, definition);
                // ✅ predicate_sensor_id 센서 탐지 결과 저장
                sensorDetectionRecorder.saveIfSensor(predicateSensorId, predicateEvents);

                return predicateEvents.isEmpty() ? null : predicateEvents.get(0).getEventDt();
            }

            // 둘 다 없으면 최신 이벤트를 앵커로 사용 (fallback)
            return eventsDesc.get(0).getEventDt();
        }

        // 비-시퀀스: predicate_rule_id 최신 이벤트 시각
        String predicate = definition.getPredicateSensorId();
        if (predicate == null || predicate.isBlank()) {
            return eventsDesc.get(0).getEventDt();
        }
        List<EngineEventStreamEntity> filtered = sensorPredicateFilter.filter(predicate, eventsDesc, definition);
        // ✅ predicate_sensor_id 센서 탐지 결과 저장 (앵커 찾기용)
        sensorDetectionRecorder.saveIfSensor(predicate, filtered);

        log.info("🔍 sensorPredicateFilter.filter for {} - Total events: {}, Filtered events: {}",
                definition.getRuleId(), eventsDesc.size(), filtered.size());
        if (filtered.isEmpty()) {
            log.warn("⚠️ No events matched predicate rule {} for aggregate {}", predicate, definition.getRuleId());
        }
        return filtered.isEmpty() ? null : filtered.get(0).getEventDt();
    }

    // Helper to get numeric field value from Map<String, Object>
//    private BigDecimal getNumericFieldValue(Map<String, Object> eventData, String fieldName) {
//        Object value = eventData.get(fieldName);
//        if (value instanceof Number) {
//            return new BigDecimal(value.toString());
//        }
//        // Handle cases where it might be a String that can be parsed to a number
//        if (value instanceof String) {
//            try {
//                return new BigDecimal((String) value);
//            } catch (NumberFormatException e) {
//                log.warn("Cannot parse '{}' as number for field '{}'", value, fieldName);
//            }
//        }
//        return BigDecimal.ZERO;
//    }
//
//    // Helper to get string field value from Map<String, Object>
//    private String getStringFieldValue(Map<String, Object> eventData, String fieldName) {
//        Object value = eventData.get(fieldName);
//        if (value != null) {
//            return value.toString();
//        }
//        return null;
//    }

    /**
     * groupKeyValues Map을 문자열로 변환 (저장/표시용)
     * 예: {login_id: EMP010} → "EMP010"
     * 예: {login_id: EMP010, account_number: 123} → "EMP010|123"
     *
     * @param groupKeyValues 그룹핑 필드들의 값
     * @return 문자열로 변환된 그룹 키
     */
    private String buildGroupKeyString(Map<String, String> groupKeyValues) {
        if (groupKeyValues == null || groupKeyValues.isEmpty()) {
            return "";
        }
        // Map의 values를 "|"로 연결하여 문자열 생성
        return String.join("|", groupKeyValues.values());
    }

    /**
     * SINGLE_ROW 모드 실행: 현재 이벤트만 체크 (window 조회 없음)
     * - where_json 조건만 체크하여 단일 행 평가
     * - threshold_count는 1로 가정 (단일 이벤트 탐지)
     *
     * @param groupKeyValues 그룹핑 필드 값
     * @param execDsMpId     실행 컨텍스트 ID
     * @param definition     집계 정의
     * @return 실행 결과
     */
    private RuleExecutionResult executeSingleRow(Map<String, String> groupKeyValues, Long execDsMpId, RuleEntity definition) {
        String groupKey = buildGroupKeyString(groupKeyValues);

        // exec_ds_mp_id 기준으로 이벤트 조회 (현재 실행에서 생성된 이벤트만)
        List<EngineEventStreamEntity> events = eventStreamRepository.findByExecDsMpIdAndGroupKeyValues(
                execDsMpId, groupKeyValues);

        if (events == null || events.isEmpty()) {
            log.debug("⚠️ No events for execDsMpId={}, groupKeyValues={} - ruleId={}",
                    execDsMpId, groupKeyValues, definition.getRuleId());
            return RuleExecutionResult.empty();
        }

        // 최신순으로 이미 정렬되어 있지만, 명시적으로 정렬하여 첫 번째 이벤트만 사용
        events.sort((a, b) -> b.getEventDt().compareTo(a.getEventDt()));
        EngineEventStreamEntity currentEvent = events.get(0);

        log.debug("🔍 SINGLE_ROW mode - 현재 이벤트 체크: event_stream_id={}, event_dt={}",
                currentEvent.getEventStreamId(), currentEvent.getEventDt());

        // where_json으로 현재 이벤트만 필터링
        List<EngineEventStreamEntity> matchedEvents = sensorPredicateFilter.filter(
                definition.getPredicateSensorId(),
                List.of(currentEvent),
                definition);

        if (matchedEvents.isEmpty()) {
            log.debug("⚠️ 현재 이벤트가 where_json 조건을 충족하지 않음 - ruleId={}", definition.getRuleId());
            return RuleExecutionResult.empty();
        }

        // 조건 충족 - 단일 행 탐지 생성
        log.info("✅ SINGLE_ROW 탐지 성공 - ruleId={}, groupKey={}, event_dt={}",
                definition.getRuleId(), groupKey, currentEvent.getEventDt());

        // DetectRuleEntity 생성 (builder 패턴)
        DetectRuleEntity result = DetectRuleEntity.builder()
                .groupKey(groupKey)
                .originalGroupKey(null) // SINGLE_ROW는 group_by_fields 기반 그룹핑 없음
                .ruleId(definition.getRuleId())
                .operator(definition.getOperator() != null ? definition.getOperator().name() : null)
                .windowMinutes(0) // SINGLE_ROW는 window 없음
                .dedupMinutes(definition.getDedupMinutes())
                .startDt(currentEvent.getEventDt())
                .endDt(currentEvent.getEventDt())
                .detectedDt(LocalDateTime.now())  // 탐지 실행 시간
                .eventDt(currentEvent.getEventDt())  // 이벤트 발생 시간
                .matchedCount(BigDecimal.ONE) // 단일 이벤트
                .thresholdCount(definition.getThresholdCount() != null ? definition.getThresholdCount() : BigDecimal.ONE)
                .pass(true) // SINGLE_ROW는 조건 충족 시 무조건 pass
                .mappedStorageId(currentEvent.getMappedDataStorageId())
                .transactionId(currentEvent.getTransactionId())
                .build();

        // 저장
        detectRuleRepository.save(result);
        log.info("💾 SINGLE_ROW 탐지 저장 완료 - ruleId={}, groupKey={}", definition.getRuleId(), groupKey);

        return new RuleExecutionResult(0, 1); // savedSensorsCount=0, savedRules=1
    }

    /**
     * SINGLE_ROW 모드 실행 (mappedStorageId 기반)
     * - mapped_storage_id를 직접 사용하여 현재 이벤트만 체크
     *
     * @param groupKeyValues  그룹핑 필드 값
     * @param mappedStorageId mapped_storages 테이블의 ID
     * @param definition      집계 정의
     * @return 실행 결과
     */
    private RuleExecutionResult executeSingleRowByMappedStorageId(
            Map<String, String> groupKeyValues, Long mappedStorageId, RuleEntity definition) {
        String groupKey = buildGroupKeyString(groupKeyValues);

        // mappedStorageId 기준으로 이벤트 조회
        List<EngineEventStreamEntity> events = eventStreamRepository.findByMappedStorageIdAndGroupKeyValues(
                mappedStorageId, groupKeyValues);

        if (events == null || events.isEmpty()) {
            log.debug("⚠️ No events for mappedStorageId={}, groupKeyValues={} - ruleId={}",
                    mappedStorageId, groupKeyValues, definition.getRuleId());
            return RuleExecutionResult.empty();
        }

        // 최신순 정렬
        events.sort((a, b) -> b.getEventDt().compareTo(a.getEventDt()));
        EngineEventStreamEntity currentEvent = events.get(0);

        log.debug("🔍 SINGLE_ROW mode (by mappedStorageId) - 현재 이벤트 체크: event_stream_id={}, event_dt={}",
                currentEvent.getEventStreamId(), currentEvent.getEventDt());

        // where_json으로 현재 이벤트만 필터링
        List<EngineEventStreamEntity> matchedEvents = sensorPredicateFilter.filter(
                definition.getPredicateSensorId(),
                List.of(currentEvent),
                definition);

        if (matchedEvents.isEmpty()) {
            log.debug("⚠️ 현재 이벤트가 where_json 조건을 충족하지 않음 - ruleId={}", definition.getRuleId());
            return RuleExecutionResult.empty();
        }

        // 중복 체크: 동일 (mapped_storage_id, rule_id) 존재 시 스킵
        if (detectRuleRepository.existsByMappedStorageIdAndRuleId(mappedStorageId, definition.getRuleId())) {
            log.info("⚠️ Skip duplicate detect_rules - mappedStorageId={}, ruleId={}", mappedStorageId, definition.getRuleId());
            return RuleExecutionResult.empty();
        }

        // 조건 충족 - 단일 행 탐지 생성
        log.info("✅ SINGLE_ROW 탐지 성공 (by mappedStorageId) - ruleId={}, groupKey={}, event_dt={}",
                definition.getRuleId(), groupKey, currentEvent.getEventDt());

        DetectRuleEntity result = DetectRuleEntity.builder()
                .groupKey(groupKey)
                .ruleId(definition.getRuleId())
                .operator(definition.getOperator() != null ? definition.getOperator().name() : null)
                .windowMinutes(0)
                .dedupMinutes(definition.getDedupMinutes())
                .startDt(currentEvent.getEventDt())
                .endDt(currentEvent.getEventDt())
                .detectedDt(LocalDateTime.now())
                .eventDt(currentEvent.getEventDt())
                .matchedCount(BigDecimal.ONE)
                .thresholdCount(definition.getThresholdCount() != null ? definition.getThresholdCount() : BigDecimal.ONE)
                .pass(true)
                .mappedStorageId(mappedStorageId)
                .transactionId(currentEvent.getTransactionId())
                .build();

        detectRuleRepository.save(result);
        log.info("💾 SINGLE_ROW 탐지 저장 완료 (by mappedStorageId) - ruleId={}, mappedStorageId={}",
                definition.getRuleId(), mappedStorageId);

        return new RuleExecutionResult(0, 1);
    }

    /**
     * WINDOW 모드 실행 (mappedStorageId 기반)
     * - 시간 기반 윈도우 조회하여 집계 수행
     *
     * @param groupKeyValues  그룹핑 필드 값
     * @param mappedStorageId mapped_storages 테이블의 ID
     * @param ruleEntity      집계 정의
     * @return 실행 결과
     */
    private RuleExecutionResult executeWindowMode(Map<String, String> groupKeyValues
            , Long mappedStorageId
            , RuleEntity ruleEntity) {

        if (ruleEntity.getWindowMinutes() == null || ruleEntity.getWindowMinutes() <= 0) {
            log.warn("Skip aggregate without valid window_minutes - ruleId={}", ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        String groupKey = buildGroupKeyString(groupKeyValues);
        int lookupWindowMinutes = ruleEntity.getWindowMinutes();

        // 전체 event_stream의 최신 이벤트 시간 조회
        Optional<LocalDateTime> execMaxEventTimeOpt = eventStreamRepository.findLatestEventTime();
        if (execMaxEventTimeOpt.isEmpty()) {
            log.info("⚠️ No events found in event_stream, skip aggregate {}", ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }
        LocalDateTime execMaxEventTime = execMaxEventTimeOpt.get();

        // 최신 이벤트 시각 기준 30일 전부터 조회
        LocalDateTime maxLookback = execMaxEventTime.minusDays(30);
        List<EngineEventStreamEntity> recentEvents = eventStreamRepository.findByFieldValuesAndEventDtBetween(
                groupKeyValues
                , maxLookback
                , execMaxEventTime.plusDays(1));

        if (recentEvents == null || recentEvents.isEmpty()) {
            log.info("⚠️ No events for groupKeyValues={} within 30 days, skip aggregate {}",
                    groupKeyValues, ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        // 최신순 정렬
        recentEvents.sort((a, b) -> b.getEventDt().compareTo(a.getEventDt()));

        LocalDateTime latestEventTime = recentEvents.get(0).getEventDt();
        LocalDateTime lookupStart = latestEventTime.minusMinutes(lookupWindowMinutes);

        // 윈도우 내 이벤트 필터링
        List<EngineEventStreamEntity> allByGroupDesc = recentEvents.stream()
                .filter(e -> !e.getEventDt().isBefore(lookupStart) && !e.getEventDt().isAfter(latestEventTime))
                .collect(Collectors.toList());

        if (allByGroupDesc.isEmpty()) {
            log.info("⚠️ No events for groupKeyValues={} within {}min window, skip aggregate {}",
                    groupKeyValues, lookupWindowMinutes, ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        LocalDateTime anchorCandidate = determineAnchor(ruleEntity, allByGroupDesc);
        if (anchorCandidate == null) {
            log.info("⚠️ No anchor found by rule for groupKey={}, ruleId={}", groupKey, ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        // 윈도우 방향 결정
        LocalDateTime startTime;
        LocalDateTime endTime;

        if (ruleEntity.getOperator() == AggregateOperator.ABSENCE_WITHIN) {
            startTime = anchorCandidate;
            endTime = startTime.plusMinutes(ruleEntity.getWindowMinutes());
        } else {
            endTime = anchorCandidate;
            startTime = endTime.minusMinutes(ruleEntity.getWindowMinutes());
        }

        // 집계 창: [start, anchor] 필터링
        List<EngineEventStreamEntity> events = recentEvents.stream()
                .filter(e -> !e.getEventDt().isBefore(startTime) && !e.getEventDt().isAfter(endTime))
                .collect(Collectors.toList());

        // predicate_sensor_id 센서 저장 (pass 여부와 무관하게)
        String predicateSensorId = ruleEntity.getPredicateSensorId();
        if (predicateSensorId != null && !predicateSensorId.isBlank()) {
            List<EngineEventStreamEntity> matchedEvents = sensorPredicateFilter.filter(predicateSensorId
                    , events
                    , ruleEntity);
            sensorDetectionRecorder.saveIfSensor(predicateSensorId, matchedEvents);
        }

        // 집계 연산 수행
        AggregationResult aggregationResult = aggregationEvaluator.evaluate(ruleEntity, events, endTime);

        LocalDateTime anchor = aggregationResult.getDetectedAt() != null ? aggregationResult.getDetectedAt() : endTime;

        if (!aggregationResult.isPass()) {
            log.info("⚠️ Aggregate NOT passed - groupKey={}, ruleId={}", groupKey, ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        String effectiveGroupKey = aggregationResult.getActualGroupKey() != null
                ? aggregationResult.getActualGroupKey()
                : groupKey;

        // dedup 체크
        Long anchorMappedStorageId = aggregationResult.getAnchorMappedStorageId();
        if (anchorMappedStorageId != null &&
                detectRuleRepository.existsByMappedStorageIdAndRuleId(anchorMappedStorageId, ruleEntity.getRuleId())) {
            log.info("⚠️ Skip duplicate detect_rules - mappedStorageId={}, ruleId={}", anchorMappedStorageId, ruleEntity.getRuleId());
            return RuleExecutionResult.empty();
        }

        Integer dedupMinutes = ruleEntity.getDedupMinutes();
        if (dedupMinutes != null && dedupMinutes > 0) {
            LocalDateTime dedupStart = anchor.minusMinutes(dedupMinutes);
            if (detectRuleRepository.existsByGroupKeyAndRuleIdAndDetectedDtAfter(
                    effectiveGroupKey, ruleEntity.getRuleId(), dedupStart)) {
                log.info("⚠️ Skip by dedup_minutes - groupKey={}, ruleId={}", effectiveGroupKey, ruleEntity.getRuleId());
                return RuleExecutionResult.empty();
            }
        }

        String originalGroupKey = aggregationResult.getActualGroupKey() != null ? groupKey : null;

        DetectRuleEntity result = DetectRuleEntity.builder()
                .groupKey(effectiveGroupKey)
                .originalGroupKey(originalGroupKey)
                .ruleId(ruleEntity.getRuleId())
                .operator(ruleEntity.getOperator() != null ? ruleEntity.getOperator().name() : null)
                .windowMinutes(ruleEntity.getWindowMinutes())
                .dedupMinutes(ruleEntity.getDedupMinutes())
                .startDt(startTime)
                .endDt(endTime)
                .detectedDt(LocalDateTime.now())
                .eventDt(aggregationResult.getDetectedAt() != null ? aggregationResult.getDetectedAt() : endTime)
                .mappedStorageId(aggregationResult.getAnchorMappedStorageId())
                .transactionId(aggregationResult.getTransactionId())
                .pass(aggregationResult.isPass())
                .matchedCount(aggregationResult.getMatchedValue())
                .thresholdCount(ruleEntity.getThresholdCount())
                .build();

        detectRuleRepository.save(result);

        log.info("💾 WINDOW 탐지 저장 완료 (by mappedStorageId) - ruleId={}, groupKey={}",
                ruleEntity.getRuleId(), effectiveGroupKey);

        return new RuleExecutionResult(0, 1);
    }
}
