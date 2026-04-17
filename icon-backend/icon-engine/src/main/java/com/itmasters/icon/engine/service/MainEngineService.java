package com.itmasters.icon.engine.service;

import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.engine.dto.*;
import com.itmasters.icon.engine.processor.prep.*;
import com.itmasters.icon.engine.processor.detect.*;
import com.itmasters.icon.engine.processor.detect.evaluate.*;
import com.itmasters.icon.engine.processor.sync.*;
import com.itmasters.icon.engine.processor.*; // Legacy processors
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 룰 엔진 메인 조정자 서비스

 * 책임:
 * - PREP-1~PREP-3, DET-1~DET-2, SYNC-1 프로세서 조정 (동기)
 * - PREP-4~PREP-5 관계 처리 (비동기)
 * - 전체 실행 흐름 관리
 * - 결과 반환

 * 실행 순서:
 * [동기 - 실시간 탐지 파이프라인]
 * 1. PREP-1: Extract - 데이터 읽기 및 저장
 * 2. PREP-2: Load - Entity Attributes 저장
 * 3. PREP-3: Transform - 파생 필드 계산
 * 4. DET-1: Stream - Event Stream 저장
 * 5. DET-2-2: Evaluate - 집계 평가 (룰 탐지 포함)
 * 6. DET-2-3: Evaluate - 시나리오 평가
 * 7. SYNC-1: Entity Update - 시나리오 기반 속성 갱신

 * [비동기 - 엔티티 관계 처리] (DET-1 이후 백그라운드 실행)
 * - PREP-4: Enrich - 엔티티 관계 추출
 * - PREP-5-A: Explicit Relations - 명시적 관계 생성
 * - PREP-5-B: Pattern Discovery - 패턴 자동 발견

 * 비동기 처리 이유:
 * - 실시간 탐지 지연 방지
 * - 그래프 DB 마이그레이션 대비 (온톨로지 기반 개발)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MainEngineService {

    private final Prep1ExtractProcessor prep1ExtractProcessor;
    private final Prep2LoadProcessor prep2LoadProcessor;
    private final Prep25EventBasedEntityUpdateProcessor prep25EventBasedEntityUpdateProcessor;
    private final Prep3TransformProcessor prep3TransformProcessor;
    private final AsyncRelationProcessingService asyncRelationProcessingService;
    private final Det1StreamProcessor det1StreamProcessor;
    private final Det2_2_RuleEvaluationProcessor det2_2_RuleEvaluationProcessor;
    private final Det2_3_ScenarioEvaluationProcessor det2_3_ScenarioEvaluationProcessor;
    private final Sync1EntityUpdateProcessor sync1EntityUpdateProcessor;

    // Legacy processors (will be removed)
    private final Step2Processor step2Processor;  // 기존 Step2 (나중에 제거)
    private final ExecDsMpService execDsMpService;



    /**
     * Step1 이후 공통 파이프라인 실행

     * 배치 실행(executeByDataSource)과 실시간 실행(SingleIngestService) 모두 이 메서드를 사용
     *
     * @param step1Result Step1 실행 결과 (데이터 적재 완료)
     * @param executedBy  실행자
     * @return PipelineResult (파이프라인 실행 결과)
     */
    public PipelineResult executeFromStep1(Step1Result step1Result, String executedBy) {
        Step1Result enrichedStep1 = ensureMappedDataLoaded(step1Result);
        Long execDsMpId = enrichedStep1.getExecDsMpId();

        log.info("========== 공통 파이프라인 실행 시작 (execDsMpId: {}) ==========", execDsMpId);

        List<MappedDataRow> mappedDataRows = enrichedStep1.getMappedDataRows();
        if (mappedDataRows == null || mappedDataRows.isEmpty()) {
            log.warn("처리할 mappedDataRows가 없어 파이프라인을 종료합니다 - execDsMpId: {}", execDsMpId);
            return PipelineResult.empty(execDsMpId);
        }

        int totalSavedSensors = 0;
        int totalSavedRules = 0;
        int totalScenariosDetected = 0;
        int totalUpdatedEntities = 0;
        int totalSavedEvents = 0;
        int totalEntityAttributes = 0;
        int totalEventBasedUpdates = 0;

        log.info("{}개의 mappedStorageId에 대해 row 단위 파이프라인을 실행합니다.", mappedDataRows.size());

        for (MappedDataRow mappedDataRow : mappedDataRows) {
            Step1Result rowStep1 = enrichedStep1.withMappedData(List.of(mappedDataRow));
            Long mappedStorageId = mappedDataRow.getMappedDataStorageId();
            Step2Result rowStep2 = null;

            try {
                // PREP-2: Entity Attributes 저장
                rowStep2 = prep2LoadProcessor.processRow(enrichedStep1, mappedDataRow);
                totalEntityAttributes += rowStep2.getActualSavedEvents();

                // PREP-3: 파생 필드 계산
                Step3Result rowStep3 = prep3TransformProcessor.processRow(enrichedStep1, mappedDataRow);
                log.debug("파생 필드 계산 완료 - mappedStorageId: {}, calculated: {}",
                        mappedStorageId, rowStep3.getCalculatedFieldsCount());

                // DET-1: Event Stream 저장
                Step4Result rowStep4 = det1StreamProcessor.processRow(rowStep1, executedBy);
                asyncRelationProcessingService.processRelationsAsync(rowStep1);

                if (!rowStep4.isSuccess()) {
                    log.error("DET-1 실패 - mappedStorageId: {}, execDsMpId: {}",
                            mappedStorageId, execDsMpId);
                    continue;
                }

                totalSavedEvents += rowStep4.getTotalEvents();

                // NOTE: totalEvents == 0이어도 탐지는 실행
                // 기존 event_stream이 이미 존재하는 경우(중복) savedIds가 비어있어 totalEvents == 0
                // 하지만 event_stream_groups는 이미 저장되어 있으므로 탐지 가능
                if (rowStep4.getTotalEvents() == 0) {
                    log.info("EVENT_STREAM 신규 저장 없음 (기존 이벤트 존재 가능) - mappedStorageId: {}",
                            mappedStorageId);
                    // continue 제거: 탐지 단계는 계속 진행
                }

            } catch (Exception e) {
                log.error("PREP/DET-1 단계 실패 - mappedStorageId: {}", mappedStorageId, e);
                continue; // 이전 단계 실패 시 탐지 건너뜀
            }

            if (mappedStorageId == null) {
                log.warn("mappedStorageId가 없어 DET-2/3/SYNC 단계를 건너뜁니다.");
                continue;
            }

            try {
                RuleEvaluationResult result = det2_2_RuleEvaluationProcessor.evaluateByMappedStorageId(mappedStorageId);
                totalSavedSensors += result.savedSensors();
                totalSavedRules += result.savedRules();

                int scenariosDetected = det2_3_ScenarioEvaluationProcessor.executeByMappedStorageId(mappedStorageId);
                totalScenariosDetected += scenariosDetected;

                int updatedEntities = sync1EntityUpdateProcessor.executeByMappedStorageId(mappedStorageId);
                totalUpdatedEntities += updatedEntities;
            } catch (Exception e) {
                log.error("DET-2/3/SYNC 단계 실패 - mappedStorageId: {}", mappedStorageId, e);
            }

            // PREP-2.5: event_stream 기반 엔티티 속성 즉시 갱신
            if (rowStep2 != null) {
                try {
                    int eventBasedUpdates = prep25EventBasedEntityUpdateProcessor.execute(rowStep2);
                    totalEventBasedUpdates += eventBasedUpdates;
                } catch (Exception e) {
                    log.error("PREP-2.5 실패 - mappedStorageId: {}", mappedStorageId, e);
                }
            }
        }

        log.info("========== 공통 파이프라인 실행 완료 ==========");
        log.info("Event Stream 저장: {} 건, Entity Attributes 저장: {} 건", totalSavedEvents, totalEntityAttributes);
        log.info("PREP-2.5 이벤트 기반 엔티티 업데이트: {} 건", totalEventBasedUpdates);
        log.info("DET-2-2 탐지: 센서 {} 건, 룰 {} 건", totalSavedSensors, totalSavedRules);
        log.info("DET-2-3 탐지: {} 개 시나리오", totalScenariosDetected);
        log.info("SYNC-1 업데이트: {} 개 엔티티", totalUpdatedEntities);

        return new PipelineResult(
                execDsMpId,
                totalSavedEvents,
                totalEntityAttributes,
                totalSavedSensors,
                totalSavedRules,
                totalScenariosDetected
        );
    }

    private Step1Result ensureMappedDataLoaded(Step1Result step1Result) {
        if (step1Result == null) {
            throw new IllegalArgumentException("step1Result must not be null");
        }

        List<MappedDataRow> mappedDataRows = step1Result.getMappedDataRows();
        if (mappedDataRows != null && !mappedDataRows.isEmpty()) {
            return step1Result;
        }

        List<MappedDataRow> reloaded = execDsMpService.ensureMappedData(step1Result.getExecDsMpId());
        if (reloaded.isEmpty()) {
            return step1Result;
        }
        log.debug("Step1Result에 포함된 mappedDataRows가 없어 execDsMpId={} 기준으로 재조회했습니다.",
                step1Result.getExecDsMpId());
        return step1Result.withMappedData(reloaded);
    }


    /**
     * PREP-1만 실행 (테스트/디버깅용)
     * - exec_ds_mp, mapped_storages, ds_file_system_log 저장
     */
    public Step1Result executeStep1Only(String dataSourceId, String executedBy) {
        log.info("PREP-1만 실행 - dataSourceId: {}", dataSourceId);
        return prep1ExtractProcessor.execute(dataSourceId, executedBy, ExecutionMode.MANUAL);
    }

    /**
     * PREP-1만 실행 - 데이터 적재(exec_ds_mp, landing_records, mapped_storages)
     * - 외부에서 생성한 exec_ds_mp_id를 사용하여 Step1 적재만 수행
     */
    public Step1Result executeStep1OnlyWithExistingExec(Long execDsMpId, String executedBy) {
        return prep1ExtractProcessor.executeWithExistingExec(execDsMpId, executedBy, ExecutionMode.MANUAL);
    }

    /**
     * DET-1만 실행 - Event Stream 저장
     * - event_stream 저장
     */
    public Step2Result executeStep2Only(Long execDsMpId, String executedBy) {
        log.info("DET-1만 실행 - execDsMpId: {}", execDsMpId);

        // 기존 실행 데이터 로드
        Step1Result step1Result = prep1ExtractProcessor.loadExistingExecution(execDsMpId);

        if (!step1Result.isSuccess()) {
            log.error("기존 실행 데이터 로드 실패 - error: {}", step1Result.getErrorMessage());
            return Step2Result.empty(step1Result);
        }

        // Step2 실행 (Event Stream 저장)
        return step2Processor.execute(
                step1Result,
                executedBy
        );
    }

    /**
     * DET-2-1만 실행 - 룰 평가
     * - 저장 테이블: detect_aggregates
     */
    /**
     * @deprecated DET-2-1 프로세서가 제거되었습니다. DET-2-2 (executeStep4Only)를 사용하세요.
     */
    @Deprecated
    public int executeStep3Only(Long execDsMpId) {
        log.warn("⚠️ executeStep3Only는 더 이상 지원되지 않습니다. DET-2-2 (executeStep4Only)를 사용하세요.");
        return 0;
    }

    /**
     * DET-2-2만 실행 (집계 평가)
     * - 룰 탐지 (predicateSensorId)와 집계 탐지 (threshold) 모두 수행
     * - 실제 저장된 탐지 건수를 합산하여 반환
     */
//    public int executeStep4Only(Long execDsMpId) {
//        log.info("DET-2-2만 실행 - execDsMpId: {}", execDsMpId);
//        RuleEvaluationResult result = det2_2_RuleEvaluationProcessor.evaluateByExec(execDsMpId);
//        return result.savedSensors() + result.savedRules();
//    }

    /**
     * DET-2-3만 실행 (시나리오 평가)
     * - detect_aggregates를 기반으로 detect_scenarios 적재
     */
//    public int executeStep5Only(Long execDsMpId) {
//        log.info("DET-2-3만 실행 - execDsMpId: {}", execDsMpId);
//        return det2_3_ScenarioEvaluationProcessor.execute(execDsMpId);
//    }

    /**
     * SYNC-1만 실행 (Entity Update)
     * - detect_scenarios를 기반으로 entity_attributes 업데이트
     */
//    public int executeStep6Only(Long execDsMpId) {
//        log.info("SYNC-1만 실행 - execDsMpId: {}", execDsMpId);
//        return sync1EntityUpdateProcessor.execute(execDsMpId);
//    }

}
