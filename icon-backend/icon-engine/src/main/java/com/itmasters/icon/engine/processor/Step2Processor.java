package com.itmasters.icon.engine.processor;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineProfileRepository;
import com.itmasters.icon.engine.dto.*;
import com.itmasters.icon.engine.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Step2 처리 전담 프로세서 - Event Stream 타임라인 저장
 * 
 * Event Stream 설계원칙 준수:
 * - 핵심 목적: group_key별 시간 순서의 이벤트 타임라인 저장
 * - Raw 데이터 기반: event_type 불필요, 원본 데이터 그대로 보존
 * - StreamKey 중복 제거: 동일한 group_key+timestamp 조합은 한 번만 저장
 * - 효율적인 저장: 프로파일 중복 방지로 저장 공간 최적화
 * 
 * 처리 흐름:
 * 1. mapped_storages → Raw 데이터 추출
 * 2. 활성 프로파일의 StreamKey 수집 (중복 제거)
 * 3. 각 StreamKey별 타임라인 데이터 생성 
 * 4. v4: mapped_storage_id 기준 중복 체크 후 저장(첫 유효 StreamKey로 1회 저장)
 * 
 * 트랜잭션: 단일 트랜잭션으로 전체 데이터 저장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Step2Processor {

    private final EventStreamService eventStreamService;
    private final EngineProfileRepository engineProfileRepository;
    private final ExecDsMpService execDsMpService;
    private final EntityExtractionService entityExtractionService;
    private final RelationExtractionService relationExtractionService;
    private final DerivedFieldService derivedFieldService;

    /**
     * Step2 실행: 활성 프로파일 기반 Event Stream 저장 (중복 제거 적용)
     *
     * @param step1Result   Step1 실행 결과 (dataSourceId, mappedData 포함)
     * @param executedBy    실행자
     * @return Step2 실행 결과 (Step3에서 활용)
     */
    public Step2Result execute(Step1Result step1Result
            , String executedBy) {

        List<MappedDataRow> mappedDataRows = execDsMpService.ensureMappedData(step1Result.getExecDsMpId());
        Step1Result enrichedStep1 = step1Result.withMappedData(mappedDataRows);

        log.info("========== Step2 시작 (Event Stream 저장) ==========");
        log.info("ExecDsMpId: {}, DataSource: {}, 데이터 건수(mapped): {}",
                enrichedStep1.getExecDsMpId(), enrichedStep1.getDataSourceId(), enrichedStep1.getTotalRows());

        // 매핑된 데이터가 없으면 빈 결과 반환
        if (!enrichedStep1.hasData()) {
            log.warn("처리할 매핑된 데이터가 없음 - dataSourceId: {}", enrichedStep1.getDataSourceId());
            return Step2Result.empty(enrichedStep1);
        }

        // 1. 활성 프로파일 조회 (entity_attributes와 event_stream 모두에서 사용)
        List<EngineProfileEntity> activeProfiles =
                engineProfileRepository.findActiveProfilesByDataSourceId(enrichedStep1.getDataSourceId());
        
        log.info("활성 프로파일 {} 개 조회됨 - dataSourceId: {}", 
                activeProfiles.size(), enrichedStep1.getDataSourceId());
        
        // 1-1. 활성 프로파일별 entity_attributes 저장 (Profile 설정에 따라)
        for (EngineProfileEntity profile : activeProfiles) {
            try {
                saveEntityAttributesForProfile(mappedDataRows, profile);
            } catch (Exception e) {
                log.error("entity_attributes 저장 실패 - profileId: {}", profile.getProfileId(), e);
            }
        }

        // 1-2. 파생 필드 계산 (entity_attributes 후, event_stream 전)
        // mapped_data에 is_third_party, risk_score 등 파생 필드 추가
        try {
            log.info("파생 필드 계산 시작 - dataSourceId: {}", enrichedStep1.getDataSourceId());

            // MappedDataRow → Map<String, Object> 변환
            List<Map<String, Object>> rawDataList = mappedDataRows.stream()
                .map(MappedDataRow::getRawData)
                .toList();

            // 파생 필드 계산 및 추가
            derivedFieldService.computeFields(enrichedStep1.getDataSourceId(), rawDataList);

            log.info("파생 필드 계산 완료 - {} rows processed", rawDataList.size());

            // DEBUG: 첫 번째 행 확인
            if (!rawDataList.isEmpty()) {
                Map<String, Object> firstRow = rawDataList.get(0);
                log.info("🔍 파생 필드 계산 후 첫 번째 행 is_third_party: {}", firstRow.get("is_third_party"));
            }
        } catch (Exception e) {
            log.error("파생 필드 계산 실패 - dataSourceId: {}", enrichedStep1.getDataSourceId(), e);
            // 파생 필드 계산 실패해도 다음 단계는 계속 진행
        }

        // 2. event_stream 저장용 StreamKey 수집 (destination_type 체크)
        Set<StreamKey> eventStreamKeys = new HashSet<>();
        for (EngineProfileEntity profile : activeProfiles) {
            if (profile.shouldStoreEventStream()) {
                try {
                    StreamKey streamKey = StreamKey.of(profile);
                    if (streamKey.isValid()) {
                        eventStreamKeys.add(streamKey);
                        log.debug("event_stream용 StreamKey 추가: {} (profile: {})",
                                streamKey.toDisplayString(), profile.getProfileId());
                    }
                } catch (Exception e) {
                    log.error("StreamKey 생성 실패 - profile: {}", profile.getProfileId(), e);
                }
            } else {
                log.debug("event_stream 저장 스킵 - destination_type={}, profileId: {}",
                    profile.getDestinationType(), profile.getProfileId());
            }
        }

        // 3. event_stream 저장 실행 (destination_type 체크 완료)
        EventStreamResult streamResult;
        if (eventStreamKeys.isEmpty()) {
            log.warn("event_stream에 저장할 활성 프로파일이 없음 - dataSourceId: {}",
                    enrichedStep1.getDataSourceId());
            // 빈 결과 반환
            streamResult = EventStreamResult.builder()
                .success(true)
                .totalEvents(0)
                .processedRows(0)
                .filteredRows(0)
                .duplicateRows(0)
                .build();
        } else {
            log.info("event_stream용 StreamKey {} 개 수집됨: {}", eventStreamKeys.size(),
                    eventStreamKeys.stream().map(StreamKey::toDisplayString).toList());

            // 사전 점검: StreamKey에 필요한 필드 존재 여부 로그
            preflightCheck(enrichedStep1, eventStreamKeys);

            // Event Stream 저장 (중복 제거 적용)
            streamResult = eventStreamService.saveEventStream(
                    enrichedStep1, eventStreamKeys, executedBy);
        }


        // 결과 요약 출력
        printSummary(streamResult);

        log.info("========== Step2 완료 (Event Stream 저장) ==========");
        log.info("저장된 이벤트: {} 건", streamResult.getTotalEvents());

        // 상세 통계 정보 포함하여 Step2Result 생성
        return Step2Result.builder()
                .step1Result(enrichedStep1)
                .streamResults(List.of(streamResult))
                .processedProfiles(List.of()) // 프로파일 정보는 생략
                .totalStreamEvents(streamResult.getTotalEvents())
                .executionTime(System.currentTimeMillis())
                // 추가된 통계 정보
                .activeStreamKeys(eventStreamKeys)
                .totalDataRows(streamResult.getProcessedRows())
                .filteredRows(streamResult.getFilteredRows())
                .duplicateRows(streamResult.getDuplicateRows())
                .actualSavedEvents(streamResult.getTotalEvents())
                .build();
    }
    
    /**
     * 활성 프로파일들의 StreamKey 수집 및 중복 제거
     */
    @Transactional(readOnly = true) // Lazy Loading 때문에 사용
    public Set<StreamKey> collectActiveStreamKeys(String dataSourceId) {
        Set<StreamKey> streamKeys = new HashSet<>();
        
        try {
            // 데이터소스의 활성 프로파일 조회
            List<EngineProfileEntity> activeProfiles = 
                    engineProfileRepository.findActiveProfilesByDataSourceId(dataSourceId);
            
            log.info("활성 프로파일 {} 개 조회됨 - dataSourceId: {}", 
                    activeProfiles.size(), dataSourceId);
            
            for (EngineProfileEntity profile : activeProfiles) {
                // destination_type 체크: event_stream을 저장하는 프로파일만 StreamKey 수집
                if (profile.shouldStoreEventStream()) {
                    try {
                        // [2026-04-23] StreamKey.of()는 timestampKey 미설정 시 null 반환 (exception 아님)
                        StreamKey streamKey = StreamKey.of(profile);
                        if (streamKey == null) {
                            // timestampKey 미설정 프로파일 → event_stream 저장 불가, 경고만 기록
                            log.warn("timestampKey 미설정으로 StreamKey 생성 불가 - profileId: {} (프로파일 관리 화면에서 타임스탬프 필드를 설정하세요)",
                                    profile.getProfileId());
                        } else if (streamKey.isValid()) {
                            streamKeys.add(streamKey);
                            log.debug("StreamKey 추가: {} (profile: {})",
                                    streamKey.toDisplayString(), profile.getProfileId());
                        } else {
                            log.warn("유효하지 않은 StreamKey 생략 - profile: {}, timestampKey: {}",
                                    profile.getProfileId(), profile.getTimestampKey());
                        }
                    } catch (Exception e) {
                        log.error("StreamKey 생성 중 예기치 않은 오류 - profile: {}", profile.getProfileId(), e);
                    }
                } else {
                    log.debug("event_stream 저장 스킵 (collectActiveStreamKeys) - destination_type={}, profileId: {}",
                            profile.getDestinationType(), profile.getProfileId());
                }
            }
            
        } catch (Exception e) {
            log.error("활성 프로파일 조회 실패 - dataSourceId: {}", dataSourceId, e);
        }
        
        return streamKeys;
    }

    /**
     * 결과 요약 출력
     */
    private void printSummary(EventStreamResult streamResult) {
        log.info("===== Step2 처리 요약 =====");

        if (streamResult.isSuccess()) {
            log.info("상태: 성공");
            log.info("저장된 이벤트: {} 건", streamResult.getTotalEvents());

            if (streamResult.getTotalEvents() > 0) {
                log.info("첫 번째 이벤트 ID: {}", streamResult.getFirstEventStreamId());
                log.info("마지막 이벤트 ID: {}", streamResult.getLastEventStreamId());
            }
        } else {
            log.error("상태: 실패");
            log.error("오류 메시지: {}", streamResult.getErrorMessage());
        }
    }

    /**
     * StreamKey별로 group_key 필드와 timestamp 필드 존재 여부를 간단 점검 (로그 전용)
     */
    protected void preflightCheck(Step1Result step1Result, Set<StreamKey> activeStreamKeys) {
        if (activeStreamKeys == null || activeStreamKeys.isEmpty()) return;
        if (step1Result.getMappedDataRows() == null || step1Result.getMappedDataRows().isEmpty()) return;

        for (StreamKey key : activeStreamKeys) {
            String gk = key.getGroupKey();
            String ts = key.getTimestampKey();
            int n = step1Result.getMappedDataRows().size();
            int hasGk = 0, hasTs = 0;
            for (var mdr : step1Result.getMappedDataRows()) {
                var row = mdr.getRawData();
                if (row.containsKey(gk)) hasGk++;
                if (row.containsKey(ts)) hasTs++;
            }
            if (hasGk < n) {
                log.warn("[Step2 Preflight] group_key 필드 누락: field='{}' {}/{} (profile timestampKey='{}')",
                        gk, (n - hasGk), n, ts);
            }
            if (hasTs < n) {
                log.warn("[Step2 Preflight] timestamp 필드 누락: field='{}' {}/{} (profile groupKey='{}')",
                        ts, (n - hasTs), n, gk);
            }
            if (hasGk == n && hasTs == n) {
                log.debug("[Step2 Preflight] OK - group_key='{}', timestamp='{}' 모두 존재", gk, ts);
            }
        }
    }

    /**
     * Profile 설정에 따라 entity_attributes 저장
     * - Profile의 destination_type이 ENTITY 또는 BOTH인 경우에만 저장
     * [2026-04-24] 주석 수정: ENTITY_ATTRIBUTES → ENTITY 또는 BOTH
     * - Profile의 entity_type, entity_id_field, store_fields 설정 사용
     * 
     * @param mappedDataRows 저장할 데이터
     * @param profile 프로파일 엔티티
     */
    @Transactional
    public void saveEntityAttributesForProfile(List<MappedDataRow> mappedDataRows, EngineProfileEntity profile) {
        
        log.info("🔍 [DEBUG] saveEntityAttributesForProfile 호출 - profileId: {}, mappedDataRows.size: {}", 
            profile.getProfileId(), mappedDataRows != null ? mappedDataRows.size() : 0);
        
        if (!profile.shouldStoreEntityAttributes()) {
            log.info("🔍 [DEBUG] Profile shouldStoreEntityAttributes() = false - profileId: {}, destinationType: {}", 
                profile.getProfileId(), profile.getDestinationType());
            return;
        }
        
        log.info("🔍 [DEBUG] Profile shouldStoreEntityAttributes() = true - profileId: {}", profile.getProfileId());

        int processedCount = 0;
        
        log.info("🔍 [DEBUG] mappedDataRows 처리 시작 - count: {}", mappedDataRows.size());

        // ✅ 각 이벤트 데이터를 EntityExtractionService로 처리 (설정 기반)
        for (MappedDataRow mappedDataRow : mappedDataRows) {
            Map<String, Object> eventData = mappedDataRow.getRawData();
            LocalDateTime eventTime = LocalDateTime.now(); // TODO: Profile의 timestampKey에서 추출
            
            log.info("🔍 [DEBUG] Processing event: {}", eventData);
            
            // EntityExtractionService를 통한 설정 기반 엔티티 생성
            entityExtractionService.extractEntitiesFromProfile(profile, eventData, eventTime);
            processedCount++;
        }

        log.info("✅ entity_attributes 처리 완료 - profileId: {}, processedCount: {}", 
            profile.getProfileId(), processedCount);
            
        // ✅ 저장 직후 즉시 entity_relations 생성 (설정 기반)
        if (processedCount > 0) {
            int relationCount = extractRelationsFromMappedData(
                profile.getDataSourceId(),
                mappedDataRows
            );
            if (relationCount > 0) {
                log.info("entity_relations 생성 완료 - profileId: {}, count: {}", 
                    profile.getProfileId(), relationCount);
            }
        }
    }

    /**
     * 매핑된 데이터로부터 entity_relations 추출 (설정 기반)
     *
     * relation_rules 설정에 따라 자동으로 관계 감지 및 생성
     *
     * @param dataSourceId DataSource ID
     * @param mappedDataRows 매핑된 데이터 행들
     * @return 생성된 관계 수
     */
    @Transactional
    protected int extractRelationsFromMappedData(
            String dataSourceId,
            List<MappedDataRow> mappedDataRows) {

        log.debug("관계 추출 시작 - dataSourceId: {}, rows: {}", dataSourceId, mappedDataRows.size());

        int relationCount = 0;

        for (MappedDataRow row : mappedDataRows) {
            try {
                Map<String, Object> eventData = row.getRawData();
                LocalDateTime eventTime = LocalDateTime.now(); // 실제로는 row에서 추출

                // RelationExtractionService에서 설정 기반으로 관계 추출
                relationExtractionService.extractRelationsFromEvent(
                    dataSourceId,
                    eventData,
                    eventTime
                );
                relationCount++;
            } catch (Exception e) {
                log.warn("행 데이터 관계 추출 실패 - mappedStorageId: {}",
                    row.getMappedDataStorageId(), e);
                // 개별 행 실패해도 계속 진행
            }
        }

        log.debug("관계 추출 완료 - dataSourceId: {}, count: {}", dataSourceId, relationCount);
        return relationCount;
    }

}
