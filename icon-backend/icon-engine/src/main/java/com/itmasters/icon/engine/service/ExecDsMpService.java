package com.itmasters.icon.engine.service;

import com.itmasters.icon.common.domain.type.DataSourceType;
import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.common.domain.type.ExecutionStatus;
import com.itmasters.icon.engine.adapter.out.persistence.entity.ExecDsMpEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.LandingRawRecordEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.MappedDataStorageEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineDataSourceRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.ExecDsMpRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.LandingRawRecordRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.MappedDataStorageRepository;
import com.itmasters.icon.engine.dto.MappedDataRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 데이터소스 필드매핑 실행 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExecDsMpService {

    private final ExecDsMpRepository execDsMpRepository;
    private final MappedDataStorageRepository mappedDataStorageRepository;
    private final LandingRawRecordRepository landingRawRecordRepository;
    private final EngineDataSourceRepository engineDataSourceRepository;
    private final LandingTransformService landingTransformService;
    /**
     * 새로운 실행 생성
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long createExecution(String dataSourceId, String executedBy, ExecutionMode mode) {
        ExecDsMpEntity execution = ExecDsMpEntity.builder()
                .dataSourceId(dataSourceId)
                .executionMode(mode)
                .executedBy(executedBy)
                .startDt(LocalDateTime.now())
                .status(ExecutionStatus.RUNNING)
                .executionContext(new HashMap<>())
                .build();

        execution = execDsMpRepository.save(execution);

        log.info("실행 로그 생성 - execDsMpId: {}, dataSourceId: {}, mode: {}",
                execution.getExecDsMpId(), dataSourceId, mode);

        return execution.getExecDsMpId();
    }

    /**
     * Landing 영역에 원본 데이터를 저장
     */
    @Transactional
    public List<LandingRawRecordEntity> saveLandingData(Long execDsMpId, List<Map<String, Object>> rawData) {

        if (rawData == null || rawData.isEmpty()) {
            return List.of();
        }

        ExecDsMpEntity execDsMp = execDsMpRepository.findById(execDsMpId)
                .orElseThrow();

        DataSourceType sourceType = resolveSourceType(execDsMp.getDataSourceId());

        List<LandingRawRecordEntity> entities = new ArrayList<>();
        for (int i = 0; i < rawData.size(); i++) {
            entities.add(LandingRawRecordEntity.builder()
                    .execDsMp(execDsMp)
                    .dataSourceId(execDsMp.getDataSourceId())
                    .sourceType(sourceType)
                    .rowIndex(i)
                    .rawPayload(rawData.get(i))
                    .build());
        }

        List<LandingRawRecordEntity> saved = landingRawRecordRepository.saveAll(entities);
        log.info("Landing 저장 완료 - execDsMpId: {}, total rows: {}", execDsMpId, saved.size());
        return saved;
    }

    @Transactional
    public List<MappedDataRow> persistMappedData(Long execDsMpId,
                                                 List<LandingRawRecordEntity> landingRecords,
                                                 List<Map<String, Object>> mappedPayloads) {
        if (mappedPayloads == null || mappedPayloads.isEmpty()) {
            return List.of();
        }

        List<LandingRawRecordEntity> sourceRecords = landingRecords;
        if (sourceRecords == null || sourceRecords.isEmpty()) {
            sourceRecords = landingRawRecordRepository.findByExecDsMpIdOrderByRowIndex(execDsMpId);
        }

        if (sourceRecords.size() != mappedPayloads.size()) {
            log.warn("Landing 기록 수와 매핑 데이터 수가 일치하지 않습니다. execDsMpId={}, landingRecords={}, mappedPayloads={}",
                    execDsMpId, sourceRecords.size(), mappedPayloads.size());
        }

        return landingTransformService.transformLandingRecords(sourceRecords, mappedPayloads);
    }

    @Transactional
    public List<MappedDataRow> transformPendingLanding(Long execDsMpId) {
        List<LandingRawRecordEntity> pending = landingRawRecordRepository
                .findByExecDsMpIdAndIngestionStatus(execDsMpId, LandingRawRecordEntity.IngestionStatus.NEW);
        if (pending.isEmpty()) {
            return List.of();
        }
        log.info("Landing → Transform 실행 - execDsMpId: {}, pending: {}", execDsMpId, pending.size());
        return landingTransformService.transformLandingRecords(pending);
    }

    @Transactional
    public List<MappedDataRow> ensureMappedData(Long execDsMpId) {
        List<MappedDataRow> mapped = getMappedDataWithIds(execDsMpId);
        if (mapped == null || mapped.isEmpty()) {
            transformPendingLanding(execDsMpId);
            mapped = getMappedDataWithIds(execDsMpId);
        }
        return mapped != null ? mapped : List.of();
    }

    /**
     * @deprecated Transform 레이어 도입에 따라 saveLandingData + LandingTransformService 사용을 권장
     */
    @Deprecated
    @Transactional
    public List<Long> saveMappedData(Long execDsMpId, List<Map<String, Object>> mappedData) {
        List<LandingRawRecordEntity> landingRecords = saveLandingData(execDsMpId, mappedData);
        return landingTransformService.transformLandingRecords(landingRecords, mappedData).stream()
                .map(MappedDataRow::getMappedDataStorageId)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 실행 완료 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeExecution(Long execDsMpId, int totalRows) {
        Optional<ExecDsMpEntity> findOpt = execDsMpRepository.findById(execDsMpId);
        findOpt.ifPresent(execution -> {
            execution.updateTotalRows(totalRows);
            execution.complete();
            execDsMpRepository.save(execution);

            log.info("실행 완료 - execDsMpId: {}, totalRows: {}",
                    execDsMpId, totalRows);
        });
    }

    /**
     * 실행 실패 처리
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExecDsMpEntity failExecution(Long execDsMpId, String errorMessage) {
        Optional<ExecDsMpEntity> execDsMpOpt = execDsMpRepository.findById(execDsMpId);
        execDsMpOpt.ifPresent(execution -> {
            execution.fail(errorMessage);
            execDsMpRepository.save(execution);

            log.error("실행 실패 - execDsMpId: {}, error: {}", execDsMpId, errorMessage);
        });
        return execDsMpOpt.get();
    }

    /**
     * 매핑된 데이터 조회 (저장된 데이터를 List로 조합)
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getMappedData(Long execDsMpId) {
        List<MappedDataStorageEntity> entities =
                mappedDataStorageRepository.findByExecDsMpIdOrderByRowIndex(execDsMpId);

        if (entities.isEmpty()) {
            return null;
        }

        // row별 데이터를 List로 조합
        return entities.stream()
                .map(MappedDataStorageEntity::getRowData)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 매핑된 데이터를 MappedDataRow로 조회 (실제 ID 포함)
     *
     * @param execDsMpId 실행 ID
     * @return MappedDataRow 목록 (실제 mapped_storage_id 포함)
     */
    @Transactional(readOnly = true)
    public List<MappedDataRow> getMappedDataWithIds(Long execDsMpId) {
        List<MappedDataStorageEntity> entities =
                mappedDataStorageRepository.findByExecDsMpIdOrderByRowIndex(execDsMpId);

        if (entities.isEmpty()) {
            return new ArrayList<>();
        }

        // 실제 mapped_storage_id, landingRecordId, transactionId와 함께 MappedDataRow 생성
        return entities.stream()
                .map(entity -> MappedDataRow.of(
                        entity.getMappedDataStorageId(),  // 실제 DB ID 사용
                        entity.getLandingRecordId(),      // 트랜잭션 단위 추적 ID
                        entity.getTransactionId(),        // 비즈니스 트랜잭션 ID
                        entity.getRowData()
                ))
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 최근 실행 로그 조회
     */
    public List<ExecDsMpEntity> getRecentExecutions(String dataSourceId, int limit) {
        return execDsMpRepository.findRecentByDataSourceId(dataSourceId, limit);
    }

    /**
     * 실행 ID로 데이터소스 ID 조회
     *
     * @param execDsMpId 실행 ID
     * @return 데이터소스 ID (없으면 null)
     */
    @Transactional(readOnly = true)
    public String getDataSourceId(Long execDsMpId) {
        return execDsMpRepository
                .findById(execDsMpId)
                .map(ExecDsMpEntity::getDataSourceId)
                .orElse(null);
    }

    /**
     * 개별 행 데이터 저장 (원본 추적용 ID 반환)
     *
     * @param execDsMpId 실행 ID
     * @param rowIndex   행 인덱스
     * @return 저장된 엔티티의 ID
     */
    @Transactional
    public LandingRawRecordEntity saveLandingRecord(Long execDsMpId, Integer rowIndex, Map<String, Object> rawData) {
        ExecDsMpEntity execDsMp = execDsMpRepository.findById(execDsMpId)
                .orElseThrow();

        LandingRawRecordEntity entity = LandingRawRecordEntity.builder()
                .execDsMp(execDsMp)
                .dataSourceId(execDsMp.getDataSourceId())
                .sourceType(resolveSourceType(execDsMp.getDataSourceId()))
                .rowIndex(rowIndex)
                .rawPayload(rawData)
                .build();

        return landingRawRecordRepository.save(entity);
    }

    @Transactional
    public Long saveSingleMappedData(Long execDsMpId, int rowIndex, Map<String, Object> rowData) {
        LandingRawRecordEntity landingRecord = saveLandingRecord(execDsMpId, rowIndex, rowData);
        return landingTransformService.transformLandingRecord(landingRecord, rowData)
                .getMappedDataStorageId();
    }

    private DataSourceType resolveSourceType(String dataSourceId) {
        return engineDataSourceRepository.findById(dataSourceId)
                .map(engineDataSource -> {
                    DataSourceType type = engineDataSource.getSourceType();
                    return type != null ? type : DataSourceType.DATABASE;
                })
                .orElseGet(() -> {
                    log.warn("데이터소스 타입을 찾을 수 없어 기본값(DATABASE)을 사용합니다. dataSourceId: {}", dataSourceId);
                    return DataSourceType.DATABASE;
                });
    }

    /**
     * 특정 실행의 매핑된 데이터 로드 (프로파일 필터링 옵션)
     *
     * @param execDsMpId 실행 ID
     * @param profileId  프로파일 ID (null이면 모든 데이터)
     * @return 매핑된 데이터 목록
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> loadMappedData(Long execDsMpId, String profileId) {
        log.info("매핑된 데이터 로드 - execDsMpId: {}, profileId: {}", execDsMpId, profileId);

        List<MappedDataStorageEntity> entities =
                mappedDataStorageRepository.findByExecDsMpIdOrderByRowIndex(execDsMpId);

        if (entities.isEmpty()) {
            log.warn("매핑된 데이터가 없음 - execDsMpId: {}", execDsMpId);
            return new ArrayList<>();
        }

        log.info("매핑된 데이터 조회 완료: {} 건", entities.size());

        // row별 데이터를 List로 조합
        return entities.stream()
                .map(MappedDataStorageEntity::getRowData)
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 특정 실행의 매핑된 데이터를 MappedDataRow로 로드 (실제 ID 포함)
     *
     * @param execDsMpId 실행 ID
     * @param profileId  프로파일 ID (null이면 모든 데이터)
     * @return MappedDataRow 목록 (실제 mapped_storage_id 포함)
     */
    @Transactional(readOnly = true)
    public List<MappedDataRow> loadMappedDataWithIds(Long execDsMpId, String profileId) {
        log.info("매핑된 데이터를 MappedDataRow로 로드 - execDsMpId: {}, profileId: {}", execDsMpId, profileId);

        List<MappedDataStorageEntity> entities =
                mappedDataStorageRepository.findByExecDsMpIdOrderByRowIndex(execDsMpId);

        if (entities.isEmpty()) {
            log.warn("매핑된 데이터가 없음 - execDsMpId: {}", execDsMpId);
            return new ArrayList<>();
        }

        log.info("매핑된 데이터 조회 완료: {} 건", entities.size());

        // 실제 mapped_storage_id, landingRecordId, transactionId와 함께 MappedDataRow 생성
        return entities.stream()
                .map(entity -> MappedDataRow.of(
                        entity.getMappedDataStorageId(),  // 실제 DB ID 사용
                        entity.getLandingRecordId(),      // 트랜잭션 단위 추적 ID
                        entity.getTransactionId(),        // 비즈니스 트랜잭션 ID
                        entity.getRowData()
                ))
                .collect(java.util.stream.Collectors.toList());
    }
}
