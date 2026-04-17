package com.itmasters.icon.engine.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EventStreamGroupEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EventStreamGroupRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EventStreamRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.JpaRuleRepository;
import com.itmasters.icon.engine.dto.EventStreamResult;
import com.itmasters.icon.engine.dto.MappedDataRow;
import com.itmasters.icon.engine.dto.Step1Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.InvalidParameterException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.itmasters.icon.engine.dto.StreamKey;

/**
 * Event Stream 처리 서비스

 * 책임:
 * - 원본 데이터를 event_stream으로 변환 및 저장 (프로파일과 무관)
 * - group_key 기반 타임라인 구성
 * - Step3에서 활용할 이벤트 스트림 데이터 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventStreamService {

    private final EventStreamRepository eventStreamRepository;
    private final EventStreamGroupRepository eventStreamGroupRepository;
    private final JpaRuleRepository aggregateRepository;

    /**
     * event_stream 저장 (다중 관점 분석 지원)
     *
     * 🎯 핵심 설계 원칙

   * -<b>목적</b>: Event Stream은 group_key별 타임라인 저장이 핵심
   * -<b>다중 관점</b>: 서로 다른 group_key로 여러 관점 분석 지원 (고객별, 디바이스별 등)
   * -<b>중복 제거</b>: 동일한 StreamKey(group_key + timestamp_key)는 Set으로 자동 제거

     *
     * 🔄 처리 과정

   * -활성 프로파일의 StreamKey 수집 (Step2Processor에서 동일 StreamKey 중복 제거)
   * -mapped_storage_id 기준 중복 체크 (이미 처리된 원본 스킵)
   * -각 유효한 StreamKey별로 event_stream 생성 (서로 다른 group_key는 각각 저장)
   * -상세 통계 정보 반환

     *
     * 📊 저장 패턴
     * 
     * 예시 1: 동일한 StreamKey (중복 제거)
     * - Profile A, B, C 모두 group_key=CUS_ID 사용
     * - 1,000건 원본 → 1,000건 event_stream (중복 없음)
     *
     * 예시 2: 서로 다른 StreamKey (다중 관점)
     * - Profile A: group_key=CUS_ID (고객 관점)
     * - Profile B: group_key=DEVICE_ID (디바이스 관점)
     * - 1,000건 원본 → 2,000건 event_stream (각 관점별 타임라인)
     * 
     * 
     * @param step1Result 1단계 결과 (매핑된 데이터)  
     * @param activeStreamKeys 활성 프로파일의 stream key 조합 (중복 제거됨)
     * @param executedBy 실행자
     * @return 저장 결과 (통계 정보 포함)
     * 
     * @see StreamKey
     * @see <a href="docs/architecture-decisions/ADR-001">ADR-001: Event Stream 중복제거전략</a>
     */
    @Transactional
    public EventStreamResult saveEventStream(Step1Result step1Result
            , Set<StreamKey> activeStreamKeys
            , String executedBy) {

        List<MappedDataRow> mappedDataRows = step1Result.getMappedDataRows();
        String dataSourceId = step1Result.getDataSourceId();

        log.info("Event Stream 저장 시작 - dataSourceId: {}, 데이터 건수: {}, 활성 StreamKey: {}",
                dataSourceId, mappedDataRows.size(), activeStreamKeys.size());
        log.info("StreamKey 목록: {}",
                activeStreamKeys.stream().map(StreamKey::toDisplayString).toList());

        // 활성 집계 목록 조회 (event_stream_groups 매핑용)
        List<RuleEntity> activeAggregates = aggregateRepository.findByIsActiveTrue();
        log.info("활성 집계 {} 개 조회됨 (그룹 매핑용)", activeAggregates.size());

        try {
            // 통계 정보 수집용 변수들
            List<Long> savedIds = new ArrayList<>();
            int totalProcessed = 0;
            int totalFiltered = 0;
            int totalDuplicates = 0;
            // mapped_storage_id 기준 중복 제거로 전략 전환
            // 동일 원본 행은 한 번만 저장하고, 첫 번째로 유효한 StreamKey로 저장을 완료한다.
            int streamKeyDuplicates = 0; // 유지(로깅 호환), 실제로는 사용하지 않음

            // 원본 데이터별로 처리 (MappedDataRow를 외부 루프로)
            for (int i = 0; i < mappedDataRows.size(); i++) {
                MappedDataRow mappedDataRow = mappedDataRows.get(i);
                Map<String, Object> dataRow = mappedDataRow.getRawData();
                Long mappedStorageId = mappedDataRow.getMappedDataStorageId();
                totalProcessed++;
                
                log.debug("원본 데이터 처리 중 - row: {}, mappedStorageId: {}", i, mappedStorageId);

                // 1. DB에 이미 이 mapped_storage_id로 생성된 event_stream이 있는지 확인
                if (eventStreamRepository.findByMappedDataStorageId(mappedStorageId).isPresent()) {
                    totalDuplicates++;
                    log.debug("이미 처리된 원본 데이터 - mappedStorageId: {}", mappedStorageId);
                    continue;
                }

                // 2. 각 유효한 StreamKey별로 event_stream 저장 (서로 다른 group_key는 각각 저장)
                for (StreamKey streamKey : activeStreamKeys) {
                    log.debug("StreamKey 적용 중: {}", streamKey.toDisplayString());

                    // group_key 값 추출 (없으면 기본값 사용)
                    String groupKeyValue = extractGroupKeyValue(dataRow, streamKey.getGroupKey());
                    if (groupKeyValue == null || groupKeyValue.trim().isEmpty()) {
                        groupKeyValue = "GLOBAL"; // EVENT_STREAM 타입은 groupKey 없이도 저장
                        log.debug("groupKey 필드 없음, 기본값 사용 - mappedStorageId: {}, field: {}, defaultValue: GLOBAL",
                                mappedStorageId, streamKey.getGroupKey());
                    }

                    // timestamp 값 추출 (해당 StreamKey의 timestamp 필드에서)
                    Object timestampValue = dataRow.get(streamKey.getTimestampKey());
                    if (timestampValue == null) {
                        log.warn("timestamp 값이 없음 - mappedStorageId: {}, field: {}",
                                mappedStorageId, streamKey.getTimestampKey());
                        totalFiltered++;
                        continue;
                    }

                    // EventStreamEntity 생성 (Raw 데이터 + 원본 추적 정보)
                    // groupKey는 event_data JSONB 내부에 저장됨
                    EngineEventStreamEntity eventStream = EngineEventStreamEntity.ofWithMapping(
                            dataRow, // Raw 데이터 그대로 보존 (모든 원본 필드 포함, groupKey 포함)
                            mappedStorageId, // 원본 추적 ID 추가
                            mappedDataRow.getTransactionId(), // 비즈니스 트랜잭션 ID
                            parseTimestamp(timestampValue)
                    );

                    // 3. mapped_storage_id 기준 중복 제거 - 이미 위에서 전체 중복 확인함
                    EngineEventStreamEntity saved = eventStreamRepository.save(eventStream);
                    savedIds.add(saved.getEventStreamId());
                    log.debug("Event Stream 저장 - groupKey: {}, mappedStorageId: {}",
                            groupKeyValue, mappedStorageId);

                    // 4. event_stream_groups 매핑 저장
                    saveEventStreamGroups(saved.getEventStreamId(), dataRow, activeAggregates, mappedStorageId);
                }
            }

            log.info("Event Stream 저장 완료 - dataSourceId: {}, 저장: {}, 처리: {}, 필터링: {}, 원본중복: {}",
                    dataSourceId, savedIds.size(), totalProcessed, totalFiltered, totalDuplicates);

            return EventStreamResult.success(dataSourceId, savedIds, 
                    totalProcessed, totalFiltered, totalDuplicates);

        } catch (Exception e) {
            log.error("Event Stream 저장 실패 - dataSourceId: {}", dataSourceId, e);
            return EventStreamResult.failed(dataSourceId, e.getMessage());
        }
    }
    

    /**
     * group_key 값 추출 (필드명으로부터)
     */
    private String extractGroupKeyValue(Map<String, Object> dataRow, String groupKeyField) {
        Object value = dataRow.get(groupKeyField);
        return value != null ? value.toString() : null;
    }
    
    
    /**
     * timestamp 값을 LocalDateTime으로 파싱
     */
    private LocalDateTime parseTimestamp(Object timestampValue) {
        if (timestampValue == null) {
            return LocalDateTime.now();
        }
        
        // 다양한 timestamp 형식 처리
        if (timestampValue instanceof LocalDateTime) {
            return (LocalDateTime) timestampValue;
        } else if (timestampValue instanceof String) {
            try {
                // "2025-08-20 09:15:00" 형식 파싱 시도
                return LocalDateTime.parse(timestampValue.toString().replace(" ", "T"));
            } catch (Exception e) {
                log.warn("timestamp 파싱 실패: {}, 현재 시간 사용", timestampValue);
                return LocalDateTime.now();
            }
        }
        
        return LocalDateTime.now();
    }
    
    /**
     * 중복 확인 (같은 group_key + timestamp 조합)
     */
    private boolean isDuplicate(String groupKey, LocalDateTime timestamp) {
        return eventStreamRepository.existsByGroupKeyAndCreatedAt(groupKey, timestamp);
    }

    /**
     * 데이터에서 detect_key 필드 자동 결정
     * 우선순위: customer_id > user_id > account_id > device_id
     */
    private String determineDetectKeyField(List<Map<String, Object>> rawData) {
        if (rawData.isEmpty()) {
            return "customer_id"; // 기본값
        }

        // 첫 번째 데이터로 필드 확인
        Map<String, Object> firstRow = rawData.get(0);

        // 우선순위에 따라 필드 선택
        if (firstRow.containsKey("customer_id")) {
            return "customer_id";
        } else if (firstRow.containsKey("user_id")) {
            return "user_id";
        } else if (firstRow.containsKey("account_id")) {
            return "account_id";
        } else if (firstRow.containsKey("device_id")) {
            return "device_id";
        }

        // 기본값
        return "customer_id";
    }


    /**
     * group_key별 최근 이벤트 조회 (Step3에서 활용)
     *
     * @param groupKey          그룹 키
     * @param timeWindowMinutes 조회할 시간 윈도우 (분)
     * @param limit             최대 조회 건수
     * @return 이벤트 목록 (시간순 정렬)
     */
    @Transactional(readOnly = true)
    public List<EngineEventStreamEntity> getRecentEvents(String groupKey,
                                                         int timeWindowMinutes,
                                                         int limit) {

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(timeWindowMinutes);

        return eventStreamRepository.findByGroupKeyAndCreatedAtAfterOrderByCreatedAtDesc(
                groupKey, cutoffTime, limit);
    }


    /**
     * group_key별 이벤트 개수 조회
     */
    @Transactional(readOnly = true)
    public long countRecentEvents(String groupKey, int timeWindowMinutes) {

        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(timeWindowMinutes);

        return eventStreamRepository.countByGroupKeyAndCreatedAtAfter(groupKey, cutoffTime);
    }

    /**
     * 지정 anchor 시각 기준으로 N분 이내 이벤트 조회 (과거→anchor 포함)
     */
    public List<EngineEventStreamEntity> getEventsWithinWindow(String groupKey,
                                                               LocalDateTime anchorTime,
                                                               int timeWindowMinutes) {
        LocalDateTime start = anchorTime.minusMinutes(timeWindowMinutes);
        return eventStreamRepository.findByGroupKeyAndCreatedAtBetween(groupKey, start, anchorTime);
    }

    /**
     * 이벤트에 대한 집계 그룹 매핑 저장
     *
     * @param eventStreamId 저장된 이벤트 스트림 ID
     * @param eventData 이벤트 데이터 (JSONB)
     * @param activeAggregates 활성화된 모든 집계 목록
     * @param mappedStorageId mapped_storages 테이블의 ID
     */
    private void saveEventStreamGroups(Long eventStreamId, Map<String, Object> eventData, List<RuleEntity> activeAggregates, Long mappedStorageId) {
        List<EventStreamGroupEntity> groupsToSave = new ArrayList<>();

        for (RuleEntity aggregate : activeAggregates) {
            String[] groupByFields = aggregate.getGroupByFields();

            // group_by_fields가 없으면 스킵
            if (groupByFields == null || groupByFields.length == 0) {
                continue;
            }

            // 이벤트가 모든 필요한 필드를 가지고 있는지 확인
            if (!hasAllFields(eventData, groupByFields)) {
                log.debug("이벤트가 필요한 필드를 포함하지 않음 - eventStreamId: {}, ruleId: {}, required: {}",
                         eventStreamId, aggregate.getRuleId(), Arrays.toString(groupByFields));
                continue;
            }

            // group_key 생성
            String groupKey = makeGroupKey(eventData, groupByFields);
            if (groupKey == null) {
                log.warn("group_key 생성 실패 - eventStreamId: {}, ruleId: {}",
                        eventStreamId, aggregate.getRuleId());
                continue;
            }

            // 매핑 엔티티 생성
            EventStreamGroupEntity groupEntity = EventStreamGroupEntity.of(
                eventStreamId,
                aggregate.getRuleId(),
                groupKey,
                mappedStorageId
            );
            groupsToSave.add(groupEntity);

            log.debug("그룹 매핑 생성 - eventStreamId: {}, ruleId: {}, groupKey: {}",
                     eventStreamId, aggregate.getRuleId(), groupKey);
        }

        // 배치 저장
        if (!groupsToSave.isEmpty()) {
            eventStreamGroupRepository.saveAll(groupsToSave);
            log.debug("이벤트 그룹 매핑 저장 완료 - eventStreamId: {}, 저장 건수: {}",
                     eventStreamId, groupsToSave.size());
        }
    }

    /**
     * 이벤트 데이터가 모든 필요한 필드를 포함하는지 확인
     *
     * @param eventData 이벤트 데이터
     * @param requiredFields 필요한 필드 배열
     * @return 모든 필드가 존재하고 null이 아니면 true
     */
    private boolean hasAllFields(Map<String, Object> eventData, String[] requiredFields) {
        for (String field : requiredFields) {
            Object value = eventData.get(field);
            if (value == null || value.toString().trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 필드 값들을 조합하여 group_key 생성
     *
     * @param eventData 이벤트 데이터
     * @param groupByFields 그룹화 기준 필드 배열
     * @return "|"로 연결된 group_key (예: "EMP004|ACC_001")
     */
    private String makeGroupKey(Map<String, Object> eventData, String[] groupByFields) {
        List<String> values = new ArrayList<>();

        for (String field : groupByFields) {
            Object value = eventData.get(field);
            if (value == null) {
                return null; // 하나라도 null이면 group_key 생성 불가
            }
            values.add(value.toString());
        }

        return String.join("|", values);
    }

}
