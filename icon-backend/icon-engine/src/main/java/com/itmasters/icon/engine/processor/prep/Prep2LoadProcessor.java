package com.itmasters.icon.engine.processor.prep;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineProfileRepository;
import com.itmasters.icon.engine.dto.MappedDataRow;
import com.itmasters.icon.engine.dto.Step1Result;
import com.itmasters.icon.engine.dto.Step2Result;
import com.itmasters.icon.engine.service.EntityExtractionService;
import com.itmasters.icon.engine.service.RelationExtractionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * PREP-2: Load (Entity Attributes 저장) 프로세서
 *
 * 책임:
 * - Profile 설정에 따라 entity_attributes 저장
 * - entity_relations 추출 (설정 기반)
 *
 * 입력: Step1Result (mappedDataRows 포함)
 * 출력: Step2Result (저장 통계)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Prep2LoadProcessor {

    private final EngineProfileRepository engineProfileRepository;
    private final EntityExtractionService entityExtractionService;
    private final RelationExtractionService relationExtractionService;

    /**
     * PREP-2 실행: entity_attributes 저장
     */
//    public Step2Result execute(Step1Result step1Result) {
//        List<MappedDataRow> mappedDataRows = step1Result.getMappedDataRows();
//        if (mappedDataRows == null) {
//            mappedDataRows = List.of();
//        }
//
//        log.info("========== PREP-2 시작 (Entity Attributes 저장) ==========");
//        log.info("ExecDsMpId: {}, DataSource: {}, 데이터 건수: {}",
//                step1Result.getExecDsMpId(), step1Result.getDataSourceId(), step1Result.getTotalRows());
//
//        if (!step1Result.hasData()) {
//            log.warn("처리할 매핑된 데이터가 없음 - dataSourceId: {}", step1Result.getDataSourceId());
//            return Step2Result.empty(step1Result);
//        }
//
//        List<EngineProfileEntity> activeProfiles = loadActiveProfiles(step1Result.getDataSourceId());
//        Counters counters = processRows(activeProfiles, mappedDataRows);
//
//        log.info("========== PREP-2 완료 (Entity Attributes 저장) ==========");
//        log.info("저장된 엔티티: {} 건, 관계: {} 건", counters.savedEntities(), counters.savedRelations());
//
//        return Step2Result.builder()
//                .step1Result(step1Result)
//                .streamResults(List.of())
//                .processedProfiles(activeProfiles)
//                .totalStreamEvents(0)
//                .executionTime(System.currentTimeMillis())
//                .activeStreamKeys(Set.of())
//                .totalDataRows(mappedDataRows.size())
//                .filteredRows(0)
//                .duplicateRows(0)
//                .actualSavedEvents(counters.savedEntities())
//                .build();
//    }

    /**
     * PREP-2 실행: entity_attributes 저장
     * - Row 파이프라인 전용 실행
     * */
    @Transactional
    public Step2Result processRow(Step1Result context, MappedDataRow mappedDataRow) {
        if (mappedDataRow == null) {
            return Step2Result.empty(context);
        }

        List<EngineProfileEntity> activeProfiles = loadActiveProfiles(context.getDataSourceId());
        Counters counters = processSingleRow(activeProfiles, mappedDataRow);

        return Step2Result.builder()
                .step1Result(context)
                .streamResults(List.of())
                .processedProfiles(activeProfiles)
                .totalStreamEvents(0)
                .executionTime(System.currentTimeMillis())
                .activeStreamKeys(Set.of())
                .totalDataRows(1)
                .filteredRows(0)
                .duplicateRows(0)
                .actualSavedEvents(counters.savedEntities())
                .build();
    }

    /**
     * Profile 설정에 따라 entity_attributes 저장
     */
    @Transactional
    public int saveEntityAttributesForProfile(MappedDataRow mappedDataRow, EngineProfileEntity profile) {
        if (!profile.shouldStoreEntityAttributes()) {
            return 0;
        }

        Map<String, Object> eventData = mappedDataRow.getRawData();
        LocalDateTime eventTime = LocalDateTime.now(); // TODO: Profile의 timestampKey에서 추출

        entityExtractionService.extractEntitiesFromProfile(profile, eventData, eventTime);
        return 1;
    }

    /**
     * 매핑된 데이터로부터 entity_relations 추출 (설정 기반)
     * @return 생성된 관계 수
     */
    @Transactional
    protected int extractRelationsFromRow(String dataSourceId, MappedDataRow mappedDataRow) {
        try {
            Map<String, Object> eventData = mappedDataRow.getRawData();
            LocalDateTime eventTime = LocalDateTime.now(); // 실제로는 row에서 추출

            relationExtractionService.extractRelationsFromEvent(
                    dataSourceId,
                    eventData,
                    eventTime
            );
            return 1;
        } catch (Exception e) {
            log.warn("행 데이터 관계 추출 실패 - mappedStorageId: {}",
                    mappedDataRow.getMappedDataStorageId(), e);
            return 0;
        }
    }

    private List<EngineProfileEntity> loadActiveProfiles(String dataSourceId) {
        List<EngineProfileEntity> profiles =
                engineProfileRepository.findActiveProfilesByDataSourceId(dataSourceId);
        log.info("활성 프로파일 {} 개 조회됨", profiles.size());
        return profiles;
    }


    private Counters processSingleRow(List<EngineProfileEntity> activeProfiles, MappedDataRow mappedDataRow) {
        int savedEntities = 0;
        int savedRelations = 0;
        for (EngineProfileEntity profile : activeProfiles) {
            if (!profile.shouldStoreEntityAttributes()) {
                continue;
            }
            savedEntities += saveEntityAttributesForProfile(mappedDataRow, profile);
            savedRelations += extractRelationsFromRow(profile.getDataSourceId(), mappedDataRow);
        }
        return new Counters(savedEntities, savedRelations);
    }

    private record Counters(int savedEntities, int savedRelations) { }
}
