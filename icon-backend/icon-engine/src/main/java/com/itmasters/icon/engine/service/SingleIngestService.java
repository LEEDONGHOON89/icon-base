package com.itmasters.icon.engine.service;

import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.engine.dto.PipelineResult;
import com.itmasters.icon.engine.dto.Step1Result;
import com.itmasters.icon.engine.exception.EngineException;
import com.itmasters.icon.engine.processor.Step1Processor;
import com.itmasters.icon.engine.service.dto.SingleRunRequest;
import com.itmasters.icon.engine.service.dto.SingleRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SingleIngestService {

    private final Step1Processor step1Processor;
    private final MainEngineService mainEngineService;

    /**
     * 에이전트 배치 데이터를 받아 파이프라인을 실행한다.
     * 에이전트 WebSocket 배치 수신 시 handleBatch에서 비동기로 호출된다.
     *
     * @param dataSourceId  대상 데이터소스 ID
     * @param records       에이전트가 전송한 파싱된 레코드 목록 (Map 형태)
     * @param executedBy    실행자 식별자 (agentId 등)
     */
    @Async
    public void ingestBatchAndRun(String dataSourceId, List<Map<String, Object>> records, String executedBy) {
        if (records == null || records.isEmpty()) {
            log.debug("[Agent] 빈 배치 수신 - dataSourceId={}, 스킵", dataSourceId);
            return;
        }
        log.info("[Agent] 배치 파이프라인 시작 - dataSourceId={}, records={}, executedBy={}",
                dataSourceId, records.size(), executedBy);
        try {
            Step1Result step1Result = step1Processor.executeInline(
                    dataSourceId, executedBy, ExecutionMode.AUTO, records);

            if (!step1Result.isSuccess()) {
                log.error("[Agent] Step1 실패 - dataSourceId={}, error={}", dataSourceId, step1Result.getErrorMessage());
                return;
            }
            mainEngineService.executeFromStep1(step1Result, executedBy);
            log.info("[Agent] 배치 파이프라인 완료 - dataSourceId={}, records={}", dataSourceId, records.size());
        } catch (Exception e) {
            log.error("[Agent] 배치 파이프라인 오류 - dataSourceId={}: {}", dataSourceId, e.getMessage(), e);
        }
    }

    /**
     * 스케줄러/실시간 수집기 전용 실행.
     * 외부에서 row를 주입하지 않고, 데이터소스에서 직접 읽어 파이프라인을 실행한다.
     * (FileSystemRealtimeCollector 등에서 호출)
     *
     * - step1Processor.execute() → 파일 시스템에서 실제 신규 데이터 읽기
     * - 신규 데이터 없으면 즉시 반환 (빈 레코드 저장 방지)
     */
    public SingleRunResponse ingestFromDataSourceAndRun(String dataSourceId, String executedBy) {
        Step1Result step1Result = step1Processor.execute(
                dataSourceId,
                executedBy,
                ExecutionMode.SCHEDULED);

        if (!step1Result.isSuccess()) {
            log.warn("[{}] PREP-1 실패: {}", dataSourceId, step1Result.getErrorMessage());
            return SingleRunResponse.error(step1Result.getErrorMessage());
        }

        if (step1Result.getTotalRows() == 0) {
            log.debug("[{}] 신규 데이터 없음 - 파이프라인 스킵", dataSourceId);
            return SingleRunResponse.success(step1Result.getExecDsMpId(), 0, 0, 0, 0, 0);
        }

        PipelineResult result = mainEngineService.executeFromStep1(step1Result, executedBy);

        return SingleRunResponse.success(
                result.execDsMpId(),
                result.savedEvents(),
                result.updatedEntityAttributes(),
                result.savedSensors(),
                result.savedRules(),
                result.savedScenarios()
        );
    }

    public SingleRunResponse ingestAndRun(SingleRunRequest req) {
        String executedBy = (req.getExecutedBy() == null || req.getExecutedBy().isBlank()) ? "api" : req.getExecutedBy();
        String dsId = (req.getDataSourceId() == null || req.getDataSourceId().isBlank()) ? "DS_API" : req.getDataSourceId();

        Map<String, Object> rawRow = req.getRow() == null
                ? new java.util.LinkedHashMap<>()
                : new java.util.LinkedHashMap<>(req.getRow());

        // PREP-1: 데이터 읽기 및 저장 (인라인 실행)
        Step1Result step1Result = step1Processor.executeInline(
                dsId,
                executedBy,
                ExecutionMode.MANUAL,
                List.of(rawRow));

        Long execDsMpId = step1Result.getExecDsMpId();

        // 실패 시 예외 발생 (치명적 에러는 프론트엔드에 명확히 전달)
        if (!step1Result.isSuccess()) {
            String errorMessage = step1Result.getErrorMessage();
            log.error("🚨 데이터 처리 실패 - execDsMpId: {}, error: {}", execDsMpId, errorMessage);
            throw EngineException.dataProcessingFailed(execDsMpId, errorMessage);
        }

        // Step1 이후 공통 파이프라인 실행 (PREP-2 ~ SYNC-1)
        PipelineResult result = mainEngineService.executeFromStep1(step1Result, executedBy);

        // 결과를 SingleRunResponse로 변환
        return SingleRunResponse.success(
                result.execDsMpId(),
                result.savedEvents(),
                result.updatedEntityAttributes(),
                result.savedSensors(),
                result.savedRules(),
                result.savedScenarios()
        );
    }
}
