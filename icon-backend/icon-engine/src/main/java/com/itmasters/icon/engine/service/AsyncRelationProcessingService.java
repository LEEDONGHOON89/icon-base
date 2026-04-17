package com.itmasters.icon.engine.service;

import com.itmasters.icon.engine.dto.Step1Result;
import com.itmasters.icon.engine.dto.Step4Result;
import com.itmasters.icon.engine.processor.prep.Prep4EnrichProcessor;
import com.itmasters.icon.engine.processor.prep.Prep5AExplicitRelationProcessor;
import com.itmasters.icon.engine.processor.prep.Prep5PatternDiscoveryProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 비동기 엔티티 관계 처리 서비스
 *
 * 실시간 탐지 파이프라인과 분리하여 엔티티 관계 추출을 비동기로 처리
 *
 * 처리 항목:
 * - PREP-4: Enrich (엔티티 관계 추출)
 * - PREP-5-A: Explicit Relations (명시적 관계 즉시 생성)
 * - PREP-5-B: Pattern Discovery (패턴 자동 발견)
 *
 * 비동기 처리의 장점:
 * - 실시간 탐지 지연 없음 (DET-1 → DET-2 빠르게 진행)
 * - 관계 추출은 백그라운드에서 처리
 * - 그래프 DB 마이그레이션 시에도 탐지 파이프라인 영향 없음
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncRelationProcessingService {

    private final Prep4EnrichProcessor prep4EnrichProcessor;
    private final Prep5AExplicitRelationProcessor prep5AExplicitRelationProcessor;
    private final Prep5PatternDiscoveryProcessor prep5PatternDiscoveryProcessor;

    /**
     * 비동기로 모든 관계 처리 실행
     *
     * DET-1 (Event Stream 저장) 이후에 호출되어 백그라운드에서 실행
     *
     * @param step1Result Step1 실행 결과
     */
    @Async("relationTaskExecutor")
    public void processRelationsAsync(Step1Result step1Result) {
        Long execDsMpId = step1Result.getExecDsMpId();
        String dataSourceId = step1Result.getDataSourceId();

        log.info("========== [ASYNC] 엔티티 관계 처리 시작 (execDsMpId: {}) ==========", execDsMpId);

        try {
            // PREP-4: 엔티티 관계 추출
            Step4Result prep4Result = prep4EnrichProcessor.execute(step1Result);
            log.info("[ASYNC] PREP-4 완료 - Enrich: 엔티티 관계 추출 (dataSourceId: {})", dataSourceId);

            // PREP-5-A: 명시적 관계 즉시 생성 (Palantir 방식)
            int explicitRelationsCreated = prep5AExplicitRelationProcessor.execute(step1Result);
            log.info("[ASYNC] PREP-5-A 완료 - Explicit Relations: {} 건 생성", explicitRelationsCreated);

            // PREP-5-B: 패턴 자동 발견 (통계 기반)
            Step4Result prep5Result = prep5PatternDiscoveryProcessor.execute(step1Result);
            log.info("[ASYNC] PREP-5-B 완료 - Pattern Discovery");

            log.info("========== [ASYNC] 엔티티 관계 처리 완료 (execDsMpId: {}) ==========", execDsMpId);

        } catch (Exception e) {
            log.error("[ASYNC] 엔티티 관계 처리 실패 - execDsMpId: {}, error: {}",
                execDsMpId, e.getMessage(), e);
            // 비동기 처리이므로 예외가 발생해도 메인 파이프라인에 영향 없음
        }
    }
}
