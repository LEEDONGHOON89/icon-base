package com.itmasters.icon.engine.processor;

import com.itmasters.icon.engine.mapping.FieldMappingEngine;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDataSourceSchemaEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Profile;

import java.util.List;
import java.util.Map;

/**
 * 필드 매핑이 적용된 향상된 룰 프로세서
 * 실제 사용 시나리오 예시
 */
@Slf4j
@Service
@Profile("demo")
@RequiredArgsConstructor
public class EnhancedRuleProcessor {
    
    private final FieldMappingEngine mappingEngine;
    private final RuleProcessor ruleProcessor;
    
    /**
     * 데이터소스에서 데이터를 읽어 룰 실행
     */
    public void processDataSourceWithRule(
            String dataSourceId,
            String ruleId,
            List<Map<String, Object>> sourceData) {
        
        log.info("Processing {} records from DataSource: {} with Rule: {}", 
                 sourceData.size(), dataSourceId, ruleId);
        
        // 1. 해당 데이터소스-룰 매핑 조회
        List<EngineDataSourceSchemaEntity> mappings = loadFieldMappings(dataSourceId, ruleId);
        
        // 2. 각 레코드에 대해 처리
        for (Map<String, Object> record : sourceData) {
            try {
                // 3. 필드 매핑 실행 (DataSourceSchema 기반)
                List<Map<String, Object>> mappedList = mappingEngine.mapByDataSource(List.of(record), dataSourceId);
                Map<String, Object> mappedData = mappedList.isEmpty() ? record : mappedList.get(0);

                // 4. 매핑된 데이터로 룰 실행
                boolean ruleResult = executeRule(ruleId, mappedData);
                
                // 5. 결과 처리
                handleRuleResult(dataSourceId, ruleId, record, mappedData, ruleResult);
                
            } catch (Exception e) {
                log.error("Failed to process record: {}", record, e);
                handleProcessingError(dataSourceId, ruleId, record, e);
            }
        }
    }
    
    /**
     * 실제 사용 예시: A회사 거래 데이터 처리
     */
    public void processCompanyATransactions() {
        // A회사 데이터 (실제로는 DB나 API에서 조회)
        List<Map<String, Object>> companyAData = List.of(
            Map.of(
                "transaction_id", "TXN001",
                "user_id", "USER123",
                "transaction_amount", 150000,  // 15만원
                "transaction_time", "2025-01-26 10:30:00"
            ),
            Map.of(
                "transaction_id", "TXN002",
                "user_id", "USER456",
                "transaction_amount", 3000000, // 300만원
                "transaction_time", "2025-01-26 11:45:00"
            )
        );
        
        // 고액 거래 감지 룰 실행
        processDataSourceWithRule(
            "DS_COMPANY_A_TXN",
            "RULE_HIGH_AMOUNT_DETECTION",
            companyAData
        );
    }
    
    /**
     * 필드 매핑 조회 (실제로는 DB에서)
     */
    private List<EngineDataSourceSchemaEntity> loadFieldMappings(String dataSourceId, String ruleId) {
        // TODO: FieldMappingRepository에서 조회
        // 여기서는 예시로 하드코딩
        
        if ("DS_COMPANY_A_TXN".equals(dataSourceId)) {
            // TODO: 실제로는 DB에서 조회
            return List.of(
            );
        }
        
        return List.of();
    }
    
    /**
     * 룰 실행
     */
    private boolean executeRule(String ruleId, Map<String, Object> data) {
        // 실제 룰 엔진 호출
        // 여기서는 간단한 예시
        if ("RULE_HIGH_AMOUNT_DETECTION".equals(ruleId)) {
            Object amount = data.get("amount");
            if (amount instanceof Number) {
                // 100만원(1억분) 이상이면 true
                return ((Number) amount).longValue() > 100000000;
            }
        }
        return false;
    }
    
    /**
     * 룰 실행 결과 처리
     */
    private void handleRuleResult(
            String dataSourceId, 
            String ruleId,
            Map<String, Object> originalData,
            Map<String, Object> mappedData,
            boolean ruleResult) {
        
        if (ruleResult) {
            log.warn("Rule triggered! DataSource: {}, Rule: {}, Data: {}", 
                     dataSourceId, ruleId, mappedData);
            
            // TODO: 알림 발송, 이벤트 저장 등
        } else {
            log.debug("Rule passed. DataSource: {}, Rule: {}", dataSourceId, ruleId);
        }
    }
    
    /**
     * 에러 처리
     */
    private void handleProcessingError(
            String dataSourceId,
            String ruleId,
            Map<String, Object> record,
            Exception error) {
        
        log.error("Processing error - DataSource: {}, Rule: {}, Record: {}", 
                  dataSourceId, ruleId, record, error);
        
        // TODO: 에러 로깅, 재시도 큐 등록 등
    }
}
