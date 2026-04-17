package com.itmasters.icon.engine.service;

import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
// import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectRuleResultEntity;  // detect_rule_results 테이블 삭제
import com.itmasters.icon.engine.adapter.out.persistence.entity.DetectRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.MappedDataStorageEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineProfileRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.JpaDetectRuleRepository;
// import com.itmasters.icon.engine.adapter.out.persistence.repository.DetectRuleResultRepository;  // detect_rule_results 테이블 삭제
import com.itmasters.icon.engine.adapter.out.persistence.repository.MappedDataStorageRepository;
import com.itmasters.icon.engine.dto.DetectionContext;
import com.itmasters.icon.engine.dto.DetectionResult;
import com.itmasters.icon.engine.dto.RuleMatchedData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * 룰 탐지 결과 저장 서비스
 * 시나리오 엔진과 연동하여 상태 기반 탐지 지원
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DetectRuleService {

    // private final DetectRuleResultRepository detectionResultRepository;  // detect_rule_results 테이블 삭제
    private final JpaDetectRuleRepository detectionDetailRepository;
    private final EngineProfileRepository engineProfileRepository;
    private final ScenarioEngineService scenarioEngineService;
    private final ScenarioDetectionService scenarioDetectionService;
    private final MappedDataStorageRepository mappedDataStorageRepository;
    
    /**
     * 룰 탐지 결과 저장 (detect_rule_results 테이블 삭제로 no-op)
     *
     * @param execDsMpId 실행 ID
     * @param detectionResult 탐지 결과
     * @param context 탐지 컨텍스트
     */
    /**
     * 룰 탐지 결과 저장 (detect_rules에 직접 저장)
     *
     * @param execDsMpId 실행 ID
     * @param detectionResult 탐지 결과
     * @param context 탐지 컨텍스트
     */
    @Transactional
    public void saveDetectionResult(
            Long execDsMpId,
            DetectionResult detectionResult,
            DetectionContext context) {

        // detect_rules에 매칭된 데이터 저장
        if (detectionResult.getMatchedData() != null && !detectionResult.getMatchedData().isEmpty()) {
            List<DetectRuleEntity> details = new ArrayList<>();
            
            for (RuleMatchedData matchedData : detectionResult.getMatchedData()) {
                try {
                    // mapped_storage_id 추출
                    Long mdsId = extractMappedStorageId(matchedData.getMappedData());
                    if (mdsId == null) {
                        log.warn("mapped_storage_id를 찾을 수 없음 - ruleId: {}, data: {}", 
                                matchedData.getRuleId(), matchedData.getMappedData().keySet());
                        continue;
                    }
                    
                    MappedDataStorageEntity mdsRef = mappedDataStorageRepository.getReference(mdsId);

                    // group_key 추출
                    String correlationKey = extractCorrelationKey(matchedData.getMappedData(), context.getProfileId());
                    if (correlationKey == null) {
                        log.warn("group_key를 찾을 수 없음 - ruleId: {}, profileId: {}",
                                matchedData.getRuleId(), context.getProfileId());
                        correlationKey = "UNKNOWN";
                    }

                    // detected_at: 실제 탐지 시점 (현재 시간)
                    java.time.LocalDateTime detectedAt = java.time.LocalDateTime.now();

                    // DetectRuleEntity 생성
                    DetectRuleEntity detail = DetectRuleEntity.builder()
                            .ruleId(matchedData.getRule().getRuleId())
                            .detectedDt(detectedAt)  // 탐지 실행 시간
                            .eventDt(detectedAt)  // TODO: 실제 이벤트 발생 시간으로 수정 필요
                            .groupKey(correlationKey)
                            .build();

                    details.add(detail);
                } catch (Exception e) {
                    log.error("detect_rule 엔티티 생성 실패 - ruleId: {}", matchedData.getRuleId(), e);
                    // 개별 실패는 무시하고 계속 진행
                }
            }
            
            if (!details.isEmpty()) {
                // 배치 저장
                detectionDetailRepository.saveAll(details);
                log.info("detect_rules 저장 완료 - execDsMpId: {}, profileId: {}, matched: {} 건",
                        execDsMpId, context.getProfileId(), details.size());
            } else {
                log.warn("저장할 detect_rules 없음 - execDsMpId: {}, profileId: {}", 
                        execDsMpId, context.getProfileId());
            }
        } else {
            log.info("매칭된 데이터 없음 - execDsMpId: {}, profileId: {}", 
                    execDsMpId, context.getProfileId());
        }
    }

    private Long extractMappedStorageId(Map<String, Object> data) {
        if (data == null) return null;
        Object id = data.get("mapped_storage_id");
        if (id instanceof Number) return ((Number) id).longValue();
        Object alt = data.get("_mapped_storage_id");
        if (alt != null) {
            String s = alt.toString();
            if (s.startsWith("MDS_")) {
                try { return Long.parseLong(s.substring(4)); } catch (NumberFormatException ignore) {}
            }
        }
        return null;
    }

    private Map<String, Object> extractMatchedFields(Map<String, Object> row, com.itmasters.icon.engine.adapter.out.persistence.entity.EngineRuleEntity rule) {
        if (row == null || rule == null) return Map.of();

        try {
            ObjectMapper om = new ObjectMapper();
            String raw = rule.getWhereJson();
            if (raw == null || raw.isBlank()) return Map.of();

            List<String> fieldsToPick = new ArrayList<>();
            if (raw.trim().startsWith("[")) {
                java.util.List<java.util.Map<String, Object>> list = om.readValue(raw, new TypeReference<java.util.List<java.util.Map<String,Object>>>(){});
                for (Map<String,Object> m : list) {
                    Object fn = m.get("fieldName");
                    if (fn != null) {
                        String fieldName = fn.toString();
                        if (fieldName.contains(",")) {
                            for (String f : fieldName.split(",")) fieldsToPick.add(f.trim());
                        } else {
                            fieldsToPick.add(fieldName);
                        }
                    }
                }
            } else {
                Map<String, Object> cond = om.readValue(raw, new TypeReference<Map<String, Object>>(){});
                Object fn = cond.get("fieldName");
                if (fn == null) return Map.of();
                String fieldName = fn.toString();
                if (fieldName.contains(",")) {
                    for (String f : fieldName.split(",")) fieldsToPick.add(f.trim());
                } else {
                    fieldsToPick.add(fieldName);
                }
            }

            Map<String, Object> out = new java.util.HashMap<>();
            for (String f : fieldsToPick) {
                String key = f.trim();
                if (key.isEmpty()) continue;
                if (row.containsKey(key)) {
                    out.put(key, row.get(key));
                } else {
                    // 보조: 원본 대문자 키가 남아있을 수 있는 경우 최소한의 매핑
                    if ("transaction_type".equalsIgnoreCase(key) && row.containsKey("TRX_TYPE")) {
                        out.put("transaction_type", row.get("TRX_TYPE"));
                    } else if ("transaction_amount".equalsIgnoreCase(key) && row.containsKey("TRX_AMT")) {
                        out.put("transaction_amount", row.get("TRX_AMT"));
                    } else if ("beneficiary_account".equalsIgnoreCase(key)) {
                        if (row.containsKey("beneficiary_account")) {
                            out.put("beneficiary_account", row.get("beneficiary_account"));
                        } else if (row.containsKey("to_account")) {
                            out.put("beneficiary_account", row.get("to_account"));
                        } else if (row.containsKey("RECEIVER")) {
                            out.put("beneficiary_account", row.get("RECEIVER"));
                        }
                    } else if ("sender_account".equalsIgnoreCase(key)) {
                        if (row.containsKey("sender_account")) {
                            out.put("sender_account", row.get("sender_account"));
                        } else if (row.containsKey("from_account")) {
                            out.put("sender_account", row.get("from_account"));
                        } else if (row.containsKey("SENDER")) {
                            out.put("sender_account", row.get("SENDER"));
                        }
                    } else if ("is_third_party".equalsIgnoreCase(key)) {
                        Object existing = findValueIgnoreCase(row, "is_third_party");
                        Boolean provided = coerceToBoolean(existing);
                        if (provided != null) {
                            out.put("is_third_party", provided);
                        }
                    }
                }
            }
            return out;
        } catch (Exception e) {
            // 파싱 실패 시 비어있는 맵 반환하여 안전하게 진행
            return Map.of();
        }
    }

    private java.time.LocalDateTime resolveEventTime(Map<String, Object> row) {
        if (row == null) return java.time.LocalDateTime.now();
        Object dt = row.get("transaction_datetime");
        if (dt instanceof java.time.LocalDateTime ldt) return ldt;
        if (dt instanceof String s) {
            try { return java.time.LocalDateTime.parse(s.replace(" ", "T")); } catch (Exception ignore) {}
        }
        Object evt = row.get("event_dt");
        if (evt instanceof java.time.LocalDateTime ldt2) return ldt2;
        if (evt instanceof String s2) {
            try { return java.time.LocalDateTime.parse(s2.replace(" ", "T")); } catch (Exception ignore) {}
        }
        return java.time.LocalDateTime.now();
    }

    private java.time.LocalDateTime resolveEventTime(Map<String, Object> row, com.itmasters.icon.engine.adapter.out.persistence.entity.EngineRuleEntity rule) {
        if (row == null) return java.time.LocalDateTime.now();
        // Prefer domain-specific datetime if available
        try {
            if (rule != null && rule.getDomain() != null) {
                String field = rule.getDomain().getFieldDatetime();
                if (field != null && !field.isBlank()) {
                    Object v = row.get(field);
                    if (v instanceof java.time.LocalDateTime ldt) return ldt;
                    if (v instanceof String s) {
                        try { return java.time.LocalDateTime.parse(s.replace(" ", "T")); } catch (Exception ignore) {}
                    }
                }
            }
        } catch (Exception ignore) {}
        // Fallback to common fields
        return resolveEventTime(row);
    }
    
    // detect_rule_results 테이블 삭제로 인해 주석 처리
    /*
    private void processScenarioDetection(Long execDsMpId,
                                         DetectionResult detectionResult,
                                         DetectionContext context,
                                         DetectRuleResultEntity resultEntity) {
        try {
            if (detectionResult.getMatchedData() == null || detectionResult.getMatchedData().isEmpty()) {
                return;
            }
            
            log.debug("시나리오 엔진 연동 시작 - profileId: {}, matched: {} 건", 
                     context.getProfileId(), detectionResult.getTotalMatched());
            
            // 각 매칭된 데이터에 대해 시나리오 컨텍스트 처리
            List<String> errors = new ArrayList<>();
            java.util.Set<Long> touchedContextIds = new java.util.HashSet<>();
            for (RuleMatchedData matchedData : detectionResult.getMatchedData()) {
                try {
                    // 상관 관계 키 추출 
                    String correlationKey = extractCorrelationKey(matchedData.getMappedData(), context.getProfileId());
                    
                    if (correlationKey == null) {
                        log.debug("상관 관계 키를 찾을 수 없음 - ruleId: {}", matchedData.getRuleId());
                        continue;
                    }
                    
                    // 시나리오 엔진에 이벤트 전달하여 컨텍스트 생성/업데이트
                    try {
                        var ctx = scenarioEngineService.processDetectionEvent(
                            correlationKey,
                            context.getProfileId(),
                            matchedData.getRuleId(),
                            matchedData.getMappedData(),
                            execDsMpId
                        );
                        if (ctx != null && ctx.getDetectionContextId() != null) {
                            touchedContextIds.add(ctx.getDetectionContextId());
                        }
                        log.debug("시나리오 이벤트 처리 완료 - correlationKey: {}, ruleId: {}", 
                                 correlationKey, matchedData.getRuleId());
                    } catch (Exception e) {
                        String msg = String.format("시나리오 이벤트 처리 실패 - correlationKey: %s, ruleId: %s, err: %s",
                                correlationKey, matchedData.getRuleId(), e.getMessage());
                        log.error(msg, e);
                        errors.add(msg);
                        // 계속 진행
                    }
                    
                } catch (Exception e) {
                    String msg = String.format("시나리오 처리 실패 - ruleId: %s, err: %s",
                            matchedData.getRuleId(), e.getMessage());
                    log.error(msg, e);
                    errors.add(msg);
                    // 개별 실패는 무시하고 계속 진행
                }
            }
            
            // 터치된 컨텍스트들에 대해 시나리오 평가 수행 (저장 포함)
            for (Long ctxId : touchedContextIds) {
                try {
                    scenarioDetectionService.evaluateScenarios(context.getProfileId(), ctxId, execDsMpId);
                } catch (Exception e) {
                    String msg = String.format("시나리오 평가 실패 - contextId: %s, err: %s", ctxId, e.getMessage());
                    log.error(msg, e);
                    errors.add(msg);
                }
            }

            if (!errors.isEmpty()) {
                resultEntity.addError(String.join(" | ", errors));
                detectionResultRepository.save(resultEntity);
            }
            log.info("시나리오 엔진 연동 완료 - profileId: {}", context.getProfileId());
            
        } catch (Exception e) {
            log.error("시나리오 엔진 연동 중 전체 오류", e);
            // 시나리오 처리 실패해도 룰 탐지 결과는 영향받지 않음
        }
    }
    */

    /**
     * 매핑된 데이터에서 상관 관계 키 추출
     * 프로파일의 detect_key 설정을 우선 확인하고, 없으면 기본 우선순위 사용
     */
    private String extractCorrelationKey(Map<String, Object> mappedData, String profileId) {
        if (mappedData == null) {
            return null;
        }
        
        // 1. 프로파일의 detect_key 설정 확인
        String profileDetectKey = getProfileDetectKey(profileId);
        if (profileDetectKey != null && !profileDetectKey.trim().isEmpty()) {
            // 콤마로 구분된 필드들 확인 (예: "customer_id,account_number")
            String[] keyFields = profileDetectKey.split(",");
            for (String field : keyFields) {
                String trimmedField = field.trim();
                
                // 대소문자 구분 없이 키 찾기
                for (Map.Entry<String, Object> entry : mappedData.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(trimmedField)) {
                        Object value = entry.getValue();
                        if (value != null && !value.toString().trim().isEmpty()) {
                            log.debug("상관 관계 키 찾음 - field: {}, value: {}", entry.getKey(), value);
                            return value.toString();
                        }
                    }
                }
            }
            log.debug("프로파일 detect_key 필드를 찾을 수 없음 - detect_key: {}, available: {}", 
                     profileDetectKey, mappedData.keySet());
        }
        
        // 2. 기본 우선순위별로 키 확인 (fallback) - 대소문자 구분 없이
        String[] defaultKeyFields = {
            "SENDER", "RECEIVER", // 대문자 버전 추가
            "customer_id", "customer_no", "cust_id",
            "account_number", "account_no", "acct_no", 
            "ip_address", "ip", "client_ip",
            "device_id", "device_uuid", "device",
            "email", "email_address",
            "phone_number", "phone", "mobile"
        };
        
        for (String field : defaultKeyFields) {
            // 대소문자 구분 없이 키 찾기
            for (Map.Entry<String, Object> entry : mappedData.entrySet()) {
                if (entry.getKey().equalsIgnoreCase(field)) {
                    Object value = entry.getValue();
                    if (value != null && !value.toString().trim().isEmpty()) {
                        log.debug("기본 상관 관계 키 찾음 - field: {}, value: {}", entry.getKey(), value);
                        return value.toString();
                    }
                }
            }
        }
        
        log.debug("상관 관계 키를 찾을 수 없음 - available fields: {}", mappedData.keySet());
        return null;
    }

    // anchor-based helpers removed; group key is resolved from profile/event data only
    
    /**
     * 프로파일의 detect_key 설정 조회
     *
     * TODO: groupKey 제거됨 - entity_id_field로 대체 (임시)
     */
    private String getProfileDetectKey(String profileId) {
        try {
            return engineProfileRepository.findById(profileId)
                .map(EngineProfileEntity::getEntityIdField)
                .orElse(null);
        } catch (Exception e) {
            log.error("프로파일 entity_id_field 조회 실패 - profileId: {}", profileId, e);
        }
        return null;
    }
    
    // detect_rule_results 테이블 삭제로 인해 주석 처리
    /*
    @Transactional
    public List<DetectRuleResultEntity> saveDetectionResults(
            Long execDsMpId,
            List<DetectionResult> results) {

        log.info("탐지 결과 일괄 저장 시작 - execDsMpId: {}, 프로파일 수: {}",
                execDsMpId, results.size());

        List<DetectRuleResultEntity> savedResults = new ArrayList<>();

        for (DetectionResult result : results) {
            try {
                DetectRuleResultEntity saved = saveDetectionResult(
                    execDsMpId, result, result.getContext());
                savedResults.add(saved);
            } catch (Exception e) {
                log.error("탐지 결과 저장 실패 - profileId: {}",
                         result.getContext().getProfileId(), e);
            }
        }

        log.info("탐지 결과 일괄 저장 완료 - 성공: {}/{}",
                savedResults.size(), results.size());

        return savedResults;
    }

    @Transactional(readOnly = true)
    public List<DetectRuleResultEntity> getDetectionResults(Long execDsMpId) {
        return detectionResultRepository.findByExecDsMpId(execDsMpId);
    }

    @Transactional(readOnly = true)
    public List<DetectRuleResultEntity> getDetectionResultsByProfile(String profileId) {
        return detectionResultRepository.findByProfileId(profileId);
    }

    @Transactional(readOnly = true)
    public List<DetectRuleEntity> getDetectionDetails(Long detectRuleResultId) {
        return detectionDetailRepository.findByDetectionResult_DetectRuleResultId(detectRuleResultId);
    }
    */
    
    /**
     * 매칭된 값 추출 (상세 저장용)
     */
    private String extractMatchedValue(RuleMatchedData matchedData) {
        if (matchedData.getMappedData() == null) {
            return null;
        }
        
        // 룰 조건에서 필드명 추출하여 해당 값 반환
        // 예: "amount > 1000000" -> amount 필드의 값 추출
        String condition = matchedData.getRuleCondition();
        if (condition != null && condition.contains(" ")) {
            String fieldName = condition.split(" ")[0];
            Object value = matchedData.getMappedData().get(fieldName);
            return value != null ? value.toString() : null;
        }
        
        return null;
    }

    private Object findValueIgnoreCase(Map<String, Object> row, String targetKey) {
        if (row == null || row.isEmpty()) {
            return null;
        }
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(targetKey)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Boolean coerceToBoolean(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        String text = value.toString();
        if (text == null) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if ("true".equalsIgnoreCase(trimmed) || "y".equalsIgnoreCase(trimmed) ||
                "yes".equalsIgnoreCase(trimmed) || "1".equals(trimmed)) {
            return true;
        }
        if ("false".equalsIgnoreCase(trimmed) || "n".equalsIgnoreCase(trimmed) ||
                "no".equalsIgnoreCase(trimmed) || "0".equals(trimmed)) {
            return false;
        }
        return null;
    }
}
