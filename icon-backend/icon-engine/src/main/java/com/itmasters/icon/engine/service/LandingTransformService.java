package com.itmasters.icon.engine.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.LandingRawRecordEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.MappedDataStorageEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.LandingRawRecordRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.MappedDataStorageRepository;
import com.itmasters.icon.engine.dto.MappedDataRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Landing → Transform 레이어 담당 서비스
 * - Landing 영역에 적재된 원본을 변환하여 mapped_storages에 저장
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LandingTransformService {

    private final MappedDataStorageRepository mappedDataStorageRepository;
    private final LandingRawRecordRepository landingRawRecordRepository;
    private final com.itmasters.icon.engine.adapter.out.persistence.repository.EngineDataSourceRepository engineDataSourceRepository;

    @Value("${engine.step1.batchSize:1000}")
    private int batchSize;

    /**
     * Landing 레코드 일괄 변환 및 저장
     */
    @Transactional
    public List<MappedDataRow> transformLandingRecords(List<LandingRawRecordEntity> landingRecords) {
        return transformLandingRecords(landingRecords, null);
    }

    public List<MappedDataRow> transformLandingRecords(List<LandingRawRecordEntity> landingRecords,
                                                       List<Map<String, Object>> overridePayloads) {
        if (landingRecords == null || landingRecords.isEmpty()) {
            return List.of();
        }

        boolean useOverride = overridePayloads != null && !overridePayloads.isEmpty();
        if (useOverride && landingRecords.size() != overridePayloads.size()) {
            throw new IllegalArgumentException("Landing 기록 수와 매핑 데이터 수가 일치하지 않습니다.");
        }

        // entity_attributes 저장은 Step2 (Profile 처리 단계)에서 수행
        // Step1은 데이터 변환 및 mapped_storages 저장만 담당

        List<MappedDataRow> result = new ArrayList<>(landingRecords.size());
        List<MappedDataStorageEntity> batch = new ArrayList<>();
        List<Map<String, Object>> payloadBatch = new ArrayList<>();

        for (int i = 0; i < landingRecords.size(); i++) {
            LandingRawRecordEntity record = landingRecords.get(i);
            Map<String, Object> transformed;
            if (useOverride) {
                Map<String, Object> overridePayload = overridePayloads.get(i);
                transformed = overridePayload == null
                        ? new LinkedHashMap<>()
                        : new LinkedHashMap<>(overridePayload);
            } else {
                transformed = new LinkedHashMap<>(record.getRawPayload());
            }
            Integer rowIndex = record.getRowIndex() != null ? record.getRowIndex() : i;
            
            String transactionId = extractTransactionId(record.getDataSourceId(), transformed);

            batch.add(MappedDataStorageEntity.builder()
                    .rowIndex(rowIndex)
                    .rowData(transformed)
                    .landingRecordId(record.getLandingRecordId())
                    .execDsMpId(record.getExecDsMp() != null ? record.getExecDsMp().getExecDsMpId() : null)
                    .transactionId(transactionId)
                    .regDt(LocalDateTime.now())
                    .build());
            payloadBatch.add(transformed);

            if (batch.size() >= Math.max(1, batchSize)) {
                flushBatch(batch, payloadBatch, result);
            }
        }

        if (!batch.isEmpty()) {
            flushBatch(batch, payloadBatch, result);
        }

        landingRecords.forEach(LandingRawRecordEntity::markTransformed);
        landingRawRecordRepository.saveAll(landingRecords);

        return result;
    }

    /**
     * 단일 Landing 레코드 변환 및 저장
     */
    @Transactional
    public MappedDataRow transformLandingRecord(LandingRawRecordEntity landingRecord, Map<String, Object> overridePayload) {
        Map<String, Object> transformed = overridePayload != null
                ? new LinkedHashMap<>(overridePayload)
                : new LinkedHashMap<>(landingRecord.getRawPayload());

        Integer rowIndex = landingRecord.getRowIndex() != null ? landingRecord.getRowIndex() : 0;
        
        String transactionId = extractTransactionId(landingRecord.getDataSourceId(), transformed);

        MappedDataStorageEntity entity = MappedDataStorageEntity.builder()
                .rowIndex(rowIndex)
                .rowData(transformed)
                .landingRecordId(landingRecord.getLandingRecordId())
                .transactionId(transactionId)
                .regDt(LocalDateTime.now())
                .build();

        entity = mappedDataStorageRepository.save(entity);
        landingRecord.markTransformed();
        landingRawRecordRepository.save(landingRecord);

        log.debug("Landing → Transform 저장 완료 - landingRecordId: {}, mappedStorageId: {}, transactionId: {}",
                landingRecord.getLandingRecordId(), entity.getMappedDataStorageId(), entity.getTransactionId());

        return MappedDataRow.of(entity.getMappedDataStorageId(), entity.getLandingRecordId(), entity.getTransactionId(), transformed);
    }

    private void flushBatch(List<MappedDataStorageEntity> batch,
                            List<Map<String, Object>> payloadBatch,
                            List<MappedDataRow> result) {
        List<MappedDataStorageEntity> saved = mappedDataStorageRepository.saveAll(batch);
        for (int i = 0; i < saved.size(); i++) {
            MappedDataStorageEntity entity = saved.get(i);
            result.add(MappedDataRow.of(
                entity.getMappedDataStorageId(), 
                entity.getLandingRecordId(), 
                entity.getTransactionId(),
                payloadBatch.get(i)
            ));
        }
        batch.clear();
        payloadBatch.clear();
    }

    /**
     * DataSource 설정에서 transaction_id 필드명을 조회하여 row 데이터에서 추출
     */
    private String extractTransactionId(String dataSourceId, Map<String, Object> rowData) {
        if (dataSourceId == null || rowData == null) {
            return null;
        }
        
        return engineDataSourceRepository.findById(dataSourceId)
                .map(dataSource -> {
                    String txField = dataSource.getTransactionIdField();
                    if (txField != null && rowData.containsKey(txField)) {
                        Object value = rowData.get(txField);
                        return value != null ? String.valueOf(value) : null;
                    }
                    return null;
                })
                .orElse(null);
    }

}
