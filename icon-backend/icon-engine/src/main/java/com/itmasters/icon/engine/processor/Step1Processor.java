package com.itmasters.icon.engine.processor;

import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.engine.adapter.out.persistence.entity.ExecDsMpEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.LandingRawRecordEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.ExecDsMpRepository;
import com.itmasters.icon.engine.dto.DataProcessingResultDto;
import com.itmasters.icon.engine.dto.MappedDataRow;
import com.itmasters.icon.engine.dto.Step1Result;
import com.itmasters.icon.engine.pipeline.DataPipeline;
import com.itmasters.icon.engine.service.ExecDsMpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Step1 처리 전담 프로세서

 * 책임:
 * - 데이터소스에서 데이터 읽기
 * - 실행 로그 생성 (exec_ds_mp)
 * - Landing 영역 저장

 * 트랜잭션: 독립적으로 실행되어 Step2 실패와 무관하게 데이터 보존
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Step1Processor {

    private final DataPipeline dataPipeline;
    private final ExecDsMpService execDsMpService;
    private final ExecDsMpRepository execDsMpRepoitory;

    /**
     * Step1 실행: 데이터 읽기 → 스키마 검증/매핑 → mapped_storages 저장
     *
     * @param dataSourceId  데이터소스 ID
     * @param executedBy    실행자
     * @param executionMode 실행 모드
     * @return Step1 실행 결과
     */
    public Step1Result execute(String dataSourceId, String executedBy, ExecutionMode executionMode) {
        log.info("========== Step1 시작 ==========");
        log.info("DataSource: {}, Mode: {}, ExecutedBy: {}", dataSourceId, executionMode, executedBy);

        // 1. 실행 로그 생성 (별도 트랜잭션)
        Long execDsMpId = createExecutionLog(dataSourceId, executedBy, executionMode);

        try {
            // 2. 데이터소스에서 데이터 읽기 + 스키마 검증 + 필드 매핑
            DataProcessingResultDto dataResult = readDataSource(dataSourceId);
            Step1Result result = finalizeStep(dataSourceId, execDsMpId, dataResult);
            if (result.isSuccess()) {
                log.info("========== Step1 완료 ==========");
                log.info("ExecDsMpId: {}, 처리 건수: {}", execDsMpId, result.getTotalRows());
            }
            return result;

        } catch (Exception e) {
            log.error("Step1 처리 중 오류", e);
            return handleFailure(execDsMpId, e.getMessage());
        }
    }

    /**
     * Step1 실행 (기존 exec_ds_mp 재사용)
     * - exec_ds_mp 레코드를 외부에서 생성해둔 경우 사용
     * - 새 exec 생성 없이 지정한 exec_ds_mp_id에 mapped_storages를 적재하고 완료 처리만 수행
     */
    public Step1Result executeWithExistingExec(Long execDsMpId,
                                               String executedBy,
                                               ExecutionMode executionMode) {
        log.info("========== Step1 (reuse exec) 시작 ==========");
        ExecDsMpEntity execDsMp = execDsMpRepoitory.findById(execDsMpId)
                .orElseThrow();

        if (execDsMpId == null) {
            return Step1Result.failed(execDsMp, "execDsMpId is required for executeWithExistingExec");
        }

        try {
            // 데이터 읽기 + 매핑
            DataProcessingResultDto dataResult = readDataSource(execDsMp.getDataSourceId());
            if (!dataResult.isSuccess()) {
                String errorMsg = dataResult.getErrorMessage() != null ? dataResult.getErrorMessage() : "데이터 읽기 실패";
                return handleFailure(execDsMp.getExecDsMpId(), errorMsg);
            }
            Step1Result result = finalizeStep(execDsMp.getDataSourceId(), execDsMpId, dataResult);
            if (result.isSuccess()) {
                log.info("========== Step1 (reuse exec) 완료 ==========");
            }
            return result;
        } catch (Exception e) {
            log.error("Step1 (reuse exec) 처리 중 오류", e);
            return handleFailure(execDsMp.getExecDsMpId(), e.getMessage());
        }
    }

    /**
     * 외부에서 전달된 raw 데이터로 Step1을 수행 (단건/실시간 입력용)
     */
    public Step1Result executeInline(String dataSourceId,
                                    String executedBy,
                                    ExecutionMode executionMode,
                                    List<Map<String, Object>> rawRows) {
        log.info("========== Step1 (inline) 시작 ==========");
        Long execDsMpId = createExecutionLog(dataSourceId, executedBy, executionMode);

        try {
            DataProcessingResultDto dataResult = dataPipeline.processInline(dataSourceId, rawRows);
            Step1Result result = finalizeStep(dataSourceId, execDsMpId, dataResult);
            if (result.isSuccess()) {
                log.info("========== Step1 (inline) 완료 ==========");
            }
            return result;
        } catch (Exception e) {
            log.error("Step1 (inline) 처리 중 오류", e);
            return handleFailure(execDsMpId, e.getMessage());
        }
    }

    /**
     * COUNT_WITHIN/이력 평가를 위한 기본 필드 존재 여부 로그 점검
     * - transaction_datetime
     * - group key 후보: customer_id, CUS_ID, user_id, account_number, device_id
     */
    protected void preflightCheck(List<Map<String, Object>> mappedData) {
        if (mappedData == null || mappedData.isEmpty()) return;
        int n = mappedData.size();
        int hasTime = 0;
        int hasGroup = 0;
        for (Map<String, Object> row : mappedData) {
            if (row.containsKey("transaction_datetime") || row.containsKey("event_dt")) hasTime++;
            if (row.containsKey("customer_id") || row.containsKey("CUS_ID")
                    || row.containsKey("user_id") || row.containsKey("account_number")
                    || row.containsKey("device_id")) hasGroup++;
        }
        if (hasTime < n) {
            log.warn("[Step1 Preflight] transaction_datetime 누락: {}/{} (COUNT_WITHIN/SEQUENCE 기준 시간 필요)", (n - hasTime), n);
        } else {
            log.debug("[Step1 Preflight] transaction_datetime 확인 완료: {} / {}", hasTime, n);
        }
        if (hasGroup < n) {
            log.warn("[Step1 Preflight] group_key 후보 누락: {}/{} (customer_id/CUS_ID/user_id/account_number/device_id 중 1개 필요)", (n - hasGroup), n);
        } else {
            log.debug("[Step1 Preflight] group_key 후보 확인 완료: {} / {}", hasGroup, n);
        }
    }

    /**
     * 실행 로그 생성 (독립 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Long createExecutionLog(String dataSourceId, String executedBy, ExecutionMode executionMode) {
        log.debug("실행 로그 생성 중...");
        Long execDsMpId = execDsMpService.createExecution(dataSourceId, executedBy, executionMode);
        log.info("실행 로그 생성 완료 - execDsMpId: {}", execDsMpId);
        return execDsMpId;
    }

    /**
     * 데이터소스 읽기 + 스키마 검증 + 필드 매핑
     */
    protected DataProcessingResultDto readDataSource(String dataSourceId) {
        log.debug("데이터소스 읽기 시작 - dataSourceId: {}", dataSourceId);
        return dataPipeline.processDataSource(dataSourceId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected List<LandingRawRecordEntity> saveLandingData(Long execDsMpId, List<Map<String, Object>> rawData) {
        log.debug("Landing 저장 시작 - execDsMpId: {}, 건수: {}", execDsMpId, rawData.size());
        List<LandingRawRecordEntity> landingRecords = execDsMpService.saveLandingData(execDsMpId, rawData);
        log.info("Landing 저장 완료 - execDsMpId: {}, 저장 건수: {}", execDsMpId, landingRecords.size());
        return landingRecords;
    }

    /**
     * 실행 완료 처리 (독립 트랜잭션)
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void completeExecution(Long execDsMpId, int totalRows) {
        execDsMpService.completeExecution(execDsMpId, totalRows);
        log.debug("실행 상태 업데이트 완료 - execDsMpId: {}, status: SUCCESS", execDsMpId);
    }

    /**
     * 실패 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected Step1Result handleFailure(Long execDsMpId, String errorMessage) {
        log.error("Step1 실패 처리 - execDsMpId: {}, error: {}", execDsMpId, errorMessage);
        ExecDsMpEntity execDsMp = execDsMpService.failExecution(execDsMpId, errorMessage);
        return Step1Result.failed(execDsMp, errorMessage);
    }

    private Step1Result finalizeStep(String dataSourceId,
                                     Long execDsMpId,
                                     DataProcessingResultDto dataResult) {

        if (!dataResult.isSuccess()) {
            String errorMsg = dataResult.getErrorMessage() != null
                    ? dataResult.getErrorMessage()
                    : "데이터 읽기 실패";
            return handleFailure(execDsMpId, errorMsg);
        }

        List<Map<String, Object>> rawData = dataResult.getRawData();
        List<Map<String, Object>> mappedData = dataResult.getMappedData();
        int mappedCount = mappedData != null ? mappedData.size() : 0;
        int rawCount = rawData != null ? rawData.size() : 0;
        log.info("데이터 매핑 완료: {} 건 (raw: {})", mappedCount, rawCount);

        preflightCheck(mappedData);

        List<LandingRawRecordEntity> landingRecords = rawCount == 0
                ? List.of()
                : saveLandingData(execDsMpId, rawData);
        int savedCount = landingRecords.size();

        if (mappedCount > 0 && savedCount > 0) {
            execDsMpService.persistMappedData(execDsMpId, landingRecords, mappedData);
        }

        completeExecution(execDsMpId, savedCount);

        ExecDsMpEntity execDsMp = execDsMpRepoitory.findById(execDsMpId)
                .orElseThrow();
        
        // ✅ FIX: mapped_storages에 저장된 데이터를 MappedDataRow로 로드하여 Step1Result에 포함
        List<MappedDataRow> mappedDataRows = execDsMpService.getMappedDataWithIds(execDsMpId);
        return Step1Result.success(execDsMp, mappedDataRows, savedCount);
    }

    /**
     * 기존 실행 데이터 재사용 (테스트/재실행용)
     *
     * @param execDsMpId 기존 실행 ID
     * @return Step1 실행 결과
     */
    @Transactional(readOnly = true)
    public Step1Result loadExistingExecution(Long execDsMpId) {
        log.info("기존 실행 데이터 로드 - execDsMpId: {}", execDsMpId);

        Optional<ExecDsMpEntity> execDsMpOpt = execDsMpRepoitory.findById(execDsMpId);
        if (execDsMpOpt.isEmpty()) {
            throw new IllegalArgumentException("exec_ds_mp를 찾을 수 없습니다: " + execDsMpId);
        }
        ExecDsMpEntity execDsMp = execDsMpOpt.get();

        try {
            List<MappedDataRow> mappedDataRows = execDsMpService.ensureMappedData(execDsMpId);

            if (mappedDataRows.isEmpty()) {
                return Step1Result.failed(execDsMp, "데이터를 찾을 수 없음");
            }

            return Step1Result.success(execDsMp, mappedDataRows, mappedDataRows.size());

        } catch (Exception e) {
            log.error("데이터 로드 실패", e);
            return Step1Result.failed(execDsMp, e.getMessage());
        }
    }
}
