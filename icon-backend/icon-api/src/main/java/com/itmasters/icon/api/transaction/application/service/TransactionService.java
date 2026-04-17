package com.itmasters.icon.api.transaction.application.service;

import com.itmasters.icon.api.datasource.application.port.out.DataSourceRepository;
import com.itmasters.icon.api.transaction.dto.TransactionDto;
import com.itmasters.icon.engine.adapter.out.persistence.entity.*;
import com.itmasters.icon.engine.adapter.out.persistence.repository.*;
import com.itmasters.icon.api.analytics.adapter.out.persistence.entity.ApiDetectRuleEntity;
import com.itmasters.icon.api.analytics.adapter.out.persistence.repository.DetectRuleRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.JpaDetectRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 트랜잭션 추적 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TransactionService {

    private final MappedDataStorageRepository mappedDataStorageRepository;
    private final EventStreamRepository eventStreamRepository;
    private final JpaDetectRuleRepository detectRuleRepository;
    private final DetectRuleRepository detectRuleRepository2;
    private final DetectScenarioRepository detectScenarioRepository;
    private final DataSourceRepository dataSourceRepository;
    private final LandingRawRecordRepository landingRawRecordRepository;
    
    /**
     * 트랜잭션 ID로 전체 파이프라인 추적
     */
    public TransactionDto.TrackingInfo trackTransaction(String transactionId) {
        log.info("Tracking transaction: {}", transactionId);
        
        // 1. MappedStorages 조회
        List<MappedDataStorageEntity> mappedStorages = mappedDataStorageRepository.findByTransactionId(transactionId);
        
        // 2. EventStreams 조회
        List<EngineEventStreamEntity> eventStreams = eventStreamRepository.findByTransactionId(transactionId);
        
        // 3. DetectRules 조회
        List<DetectRuleEntity> detectRules = detectRuleRepository.findByTransactionId(transactionId);

        // 4. DetectAggregates 조회 (mappedStorageId를 통해)
        List<Long> mappedStorageIds = mappedStorages.stream()
            .map(MappedDataStorageEntity::getMappedDataStorageId)
            .collect(Collectors.toList());
        List<ApiDetectRuleEntity> detectAggregates = mappedStorageIds.isEmpty()
            ? java.util.Collections.emptyList()
            : mappedStorageIds.stream()
                .flatMap(id -> detectRuleRepository2.findByMappedStorageId(id).stream())
                .collect(Collectors.toList());

        // 5. DetectScenarios 조회
        List<DetectScenarioEntity> detectScenarios = detectScenarioRepository.findByTransactionId(transactionId);
        
        // 6. DataSource 정보 추출 (첫 번째 mapped_storage의 landing_record에서)
        String dataSourceId = null;
        String dataSourceName = null;
        if (!mappedStorages.isEmpty()) {
            Long landingRecordId = mappedStorages.get(0).getLandingRecordId();
            dataSourceId = landingRawRecordRepository.findById(landingRecordId)
                    .map(LandingRawRecordEntity::getDataSourceId)
                    .orElse(null);
            if (dataSourceId != null) {
                dataSourceName = dataSourceRepository.findById(dataSourceId)
                        .map(ds -> ds.getName())
                        .orElse(null);
            }
        }
        
        // 7. 시간 범위 계산
        LocalDateTime firstSeenAt = null;
        LocalDateTime lastSeenAt = null;
        if (!mappedStorages.isEmpty()) {
            firstSeenAt = mappedStorages.stream()
                    .map(MappedDataStorageEntity::getRegDt)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);
            lastSeenAt = mappedStorages.stream()
                    .map(MappedDataStorageEntity::getRegDt)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);
        }
        
        // 8. DTO 변환
        return TransactionDto.TrackingInfo.builder()
                .transactionId(transactionId)
                .dataSourceId(dataSourceId)
                .dataSourceName(dataSourceName)
                .firstSeenAt(firstSeenAt)
                .lastSeenAt(lastSeenAt)
                .mappedStorages(mappedStorages.stream()
                        .map(this::toMappedStorageInfo)
                        .collect(Collectors.toList()))
                .eventStreams(eventStreams.stream()
                        .map(this::toEventStreamInfo)
                        .collect(Collectors.toList()))
                .detectRules(detectRules.stream()
                        .map(this::toDetectRuleInfo)
                        .collect(Collectors.toList()))
                .detectAggregates(detectAggregates.stream()
                        .map(this::toDetectAggregateInfo)
                        .collect(Collectors.toList()))
                .detectScenarios(detectScenarios.stream()
                        .map(this::toDetectScenarioInfo)
                        .collect(Collectors.toList()))
                .build();
    }
    
    private TransactionDto.MappedStorageInfo toMappedStorageInfo(MappedDataStorageEntity entity) {
        return TransactionDto.MappedStorageInfo.builder()
                .mappedDataStorageId(entity.getMappedDataStorageId())
                .landingRecordId(entity.getLandingRecordId())
                .rowIndex(entity.getRowIndex())
                .rowData(entity.getRowData())
                .regDt(entity.getRegDt())
                .build();
    }
    
    private TransactionDto.EventStreamInfo toEventStreamInfo(EngineEventStreamEntity entity) {
        // Extract groupKey from event_data JSONB
        String groupKey = null;
        if (entity.getEventData() != null) {
            Object groupKeyValue = entity.getEventData().get("group_key");
            if (groupKeyValue != null) {
                groupKey = groupKeyValue.toString();
            }
        }

        return TransactionDto.EventStreamInfo.builder()
                .eventStreamId(entity.getEventStreamId())
                .groupKey(groupKey)  // JSONB에서 추출한 groupKey
                .eventData(entity.getEventData())
                .mappedDataStorageId(entity.getMappedDataStorageId())
                .eventDt(entity.getEventDt())
                .build();
    }
    
    private TransactionDto.DetectRuleInfo toDetectRuleInfo(DetectRuleEntity entity) {
        return TransactionDto.DetectRuleInfo.builder()
                .detectRuleId(entity.getDetectRuleId())
                .ruleId(entity.getRuleId())
                .ruleName(null) // DetectRuleEntity doesn't have rule name
                .groupKey(entity.getGroupKey())
                .matchedFields(null) // DetectRuleEntity doesn't have matched fields
                .detectedAt(entity.getDetectedDt())
                .build();
    }
    
    private TransactionDto.DetectAggregateInfo toDetectAggregateInfo(ApiDetectRuleEntity entity) {
        return TransactionDto.DetectAggregateInfo.builder()
                .detectAggregateId(entity.getDetectRuleId())
                .aggregateId(entity.getRuleId())
                .groupKey(entity.getGroupKey())
                .operator(entity.getOperator())
                .pass(entity.getPass())
                .matchedCount(entity.getMatchedCount())
                .mappedStorageId(entity.getMappedStorageId())
                .detectedAt(entity.getDetectedDt())
                .build();
    }

    private TransactionDto.DetectScenarioInfo toDetectScenarioInfo(DetectScenarioEntity entity) {
        return TransactionDto.DetectScenarioInfo.builder()
                .detectScenarioId(entity.getDetectScenarioId())
                .scenarioId(entity.getScenarioId())
                .groupKey(entity.getGroupKey())
                .detectedAt(entity.getDetectedDt())
                .build();
    }
    
    /**
     * 최근 트랜잭션 목록 조회 (30개)
     */
    public List<TransactionDto.TransactionListItem> getRecentTransactions() {
        log.info("최근 트랜잭션 목록 조회 시작");
        
        // transaction_id가 있는 최근 데이터 조회
        List<Object[]> results = mappedDataStorageRepository.findRecentTransactions(30);
        
        return results.stream()
                .map(row -> {
                    String transactionId = (String) row[0];
                    String dataSourceId = (String) row[1];
                    LocalDateTime firstSeenAt = (LocalDateTime) row[2];
                    LocalDateTime lastSeenAt = (LocalDateTime) row[3];
                    Long totalRecords = (Long) row[4];
                    
                    // 데이터소스 이름 조회
                    String dataSourceName = dataSourceId != null
                            ? dataSourceRepository.findById(dataSourceId)
                                .map(ds -> ds.getName())
                                .orElse(dataSourceId)
                            : null;
                    
                    // 시나리오 탐지 개수 조회
                    Integer scenariosDetected = detectScenarioRepository.countByTransactionId(transactionId);
                    
                    return TransactionDto.TransactionListItem.builder()
                            .transactionId(transactionId)
                            .dataSourceId(dataSourceId)
                            .dataSourceName(dataSourceName)
                            .firstSeenAt(firstSeenAt)
                            .lastSeenAt(lastSeenAt)
                            .totalRecords(totalRecords.intValue())
                            .scenariosDetected(scenariosDetected)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
