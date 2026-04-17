package com.itmasters.icon.engine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.itmasters.icon.common.constants.EngineConstants;
import com.itmasters.icon.common.domain.type.EventType;
import com.itmasters.icon.engine.adapter.out.persistence.entity.*;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineProfileRepository;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineEventStreamEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EventStreamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

/**
 * mapped_storages의 데이터를 event_stream으로 변환하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EventLogService {
    
    private final ObjectMapper objectMapper;
    private final EngineProfileRepository profileRepository;
    private final EventStreamRepository eventStreamRepository;
    @Value("${engine.derived.highAmountThreshold:100000}")
    private long highAmountThreshold;
    
    /**
     * mapped_storages의 데이터를 event_stream으로 변환
     * 
     * @param mappedData mapped_storages에서 조회한 데이터
     * @param profileId 프로파일 ID (금융, 의료 등)
     * @param mappedDataStorageId mapped_storages의 ID (원본 추적용)
     */
    @Transactional
    public void convertToEventStream(Map<String, Object> mappedData, String profileId, String mappedDataStorageId) {
        try {
            log.debug("Converting mapped data to event stream. Keys: {}", mappedData.keySet());

            String eventType = determineEventType(mappedData);
            ObjectNode eventData = createEventData(mappedData, eventType, mappedDataStorageId);
            LocalDateTime eventTime = extractEventTime(mappedData, profileId);

            saveEventStream(eventType, eventData, eventTime);

            log.info("Successfully converted mapped data to event stream: eventType={}", eventType);

        } catch (Exception e) {
            log.error("Failed to convert mapped data to event stream", e);
            throw new RuntimeException("Event stream conversion failed", e);
        }
    }
    
    /**
     * 이벤트 타입 결정
     */
    private String determineEventType(Map<String, Object> data) {
        // EventType Enum을 사용하여 타입 추론
        EventType eventType = EventType.inferFromData(data);
        return eventType.name();
    }
    
    /**
     * 이벤트 데이터 생성
     * 필드 매핑이 완료된 표준 필드 데이터를 그대로 사용
     */
    private ObjectNode createEventData(Map<String, Object> data, String eventType, String mappedDataStorageId) {
        ObjectNode eventData = objectMapper.createObjectNode();
        
        // 원본 추적을 위한 mapped_storage_id 추가 (메타데이터로 언더스코어 접두사 사용)
        if (mappedDataStorageId != null) {
            eventData.put("_mapped_storage_id", mappedDataStorageId);
        }
        
        // 모든 표준 필드를 그대로 저장 (이미 매핑됨)
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            
            // 내부 메타데이터 필드 제외
            if (!isInternalField(key)) {
                // 표준 필드명 사용 (이미 매핑되어 있음)
                addIfNotNull(eventData, key, value);
            }
        }
        
        // 이벤트 타입별 추가 정보 (필요한 경우만)
        EventType type = EventType.valueOf(eventType);
        switch (type) {
            case TRANSFER:
                // transfer_type이 없으면 기본값 설정
                if (!eventData.has("transfer_type")) {
                    eventData.put("transfer_type", "immediate");
                }
                break;
                
            case LOGIN:
                // login_method가 없으면 기본값 설정
                if (!eventData.has("login_method")) {
                    eventData.put("login_method", "password");
                }
                // login_success가 없으면 기본값 설정
                if (!eventData.has("login_success")) {
                    eventData.put("login_success", true);
                }
                break;
                
            case OTP_ISSUE:
                // otp_issue_method가 없으면 기본값 설정
                if (!eventData.has("otp_issue_method")) {
                    eventData.put("otp_issue_method", "sms");
                }
                // otp_request_reason이 없으면 기본값 설정
                if (!eventData.has("otp_request_reason")) {
                    eventData.put("otp_request_reason", "transfer");
                }
                break;
                
            case CERT_ISSUE:
                // cert_type이 없으면 기본값 설정
                if (!eventData.has("cert_type")) {
                    eventData.put("cert_type", "공동인증서");
                }
                // cert_issue_method가 없으면 기본값 설정
                if (!eventData.has("cert_issue_method")) {
                    eventData.put("cert_issue_method", "online");
                }
                break;
                
            default:
                // 기타 이벤트는 추가 처리 없음
                break;
        }

        // === Derived signals (long-term design) ===
        try {
            String txType = null;
            if (eventData.has("transaction_type")) {
                txType = eventData.get("transaction_type").asText(null);
            } else if (eventData.has("TRX_TYPE")) {
                txType = eventData.get("TRX_TYPE").asText(null);
            }

            java.math.BigDecimal amt = null;
            if (eventData.has("transaction_amount")) {
                try { amt = new java.math.BigDecimal(eventData.get("transaction_amount").asText()); } catch (Exception ignore) {}
            } else if (eventData.has("TRX_AMT")) {
                try { amt = new java.math.BigDecimal(eventData.get("TRX_AMT").asText()); } catch (Exception ignore) {}
            }

            boolean isTransfer = txType != null && ("이체".equals(txType) || "TRANSFER".equalsIgnoreCase(txType));
            boolean isHigh = false;
            if (amt != null) {
                try { isHigh = amt.compareTo(java.math.BigDecimal.valueOf(highAmountThreshold)) >= 0; } catch (Exception ignore) {}
            }
            eventData.put("is_transfer", isTransfer);
            eventData.put("is_high_amount_transfer", isTransfer && isHigh);

            // is_third_party 는 공급사가 명시적으로 전달해야 한다
            try {
                Boolean provided = extractBooleanField(eventData, "is_third_party");
                if (provided != null) {
                    eventData.put("is_third_party", provided);
                } else {
                    log.warn("is_third_party not provided; event payload={} (event log expects supplier-provided flag)", eventData);
                }
            } catch (Exception ignore) { }
        } catch (Exception e) {
            log.warn("Failed to compute derived signals: {}", e.getMessage());
        }

        return eventData;
    }
    
    /**
     * 내부 시스템 필드 여부 확인
     * 시스템 운영을 위한 메타데이터는 user_activity_log에 저장하지 않음
     */
    private boolean isInternalField(String fieldName) {
        // 시스템 내부 필드는 제외
        return fieldName.startsWith("exec_") ||      // 실행 관련 메타데이터
               EngineConstants.Field.isMetadataField(fieldName) ||  // 언더스코어로 시작하는 메타데이터
               fieldName.equals("profile_id") ||     // 프로파일 참조
               fieldName.equals("profile_schema_id") || // 프로파일 스키마 참조
               fieldName.equals("data_source_id") || // 데이터 소스 참조
               fieldName.equals("data_source_schema_id") || // 데이터 소스 스키마 참조
               fieldName.equals("row_index") ||      // 행 인덱스
               fieldName.equals("reg_dt") ||         // 시스템 등록 시간
               fieldName.equals("is_active");        // 활성화 상태 플래그
    }
    
    /**
     * 이벤트 시간 추출 (프로파일의 timestamp 필드 사용)
     */
    private LocalDateTime extractEventTime(Map<String, Object> data, String profileId) {
        // 프로파일에서 timestamp 필드 정보 가져오기
        String timestampFieldName = getTimestampFieldName(profileId);
        
        if (timestampFieldName != null) {
            Object timestampValue = data.get(timestampFieldName);
            if (timestampValue != null) {
                if (timestampValue instanceof LocalDateTime) {
                    return (LocalDateTime) timestampValue;
                } else if (timestampValue instanceof String) {
                    try {
                        return LocalDateTime.parse((String) timestampValue, 
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                    } catch (Exception e) {
                        log.warn("Failed to parse timestamp field '{}': {}", timestampFieldName, timestampValue);
                    }
                }
            } else {
                log.warn("Timestamp field '{}' not found in data for profile: {}", timestampFieldName, profileId);
            }
        }
        
        // fallback: 표준 필드명으로 시도 (필드 매핑 완료 후이므로)
        Object trxDt = data.get("transaction_datetime");
        
        if (trxDt instanceof LocalDateTime) {
            return (LocalDateTime) trxDt;
        } else if (trxDt instanceof String) {
            try {
                return LocalDateTime.parse((String) trxDt, 
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception e) {
                log.warn("Failed to parse date: {}", trxDt);
            }
        }
        
        log.warn("No valid timestamp found for profile: {}, using current time", profileId);
        return LocalDateTime.now();
    }
    
    /**
     * 프로파일의 timestamp 필드명 가져오기
     */
    private String getTimestampFieldName(String profileId) {
        try {
            Optional<EngineProfileEntity> profileOpt = profileRepository.findById(profileId);
            if (!profileOpt.isPresent()) {
                log.warn("Profile not found: {}", profileId);
                return null;
            }

            return profileOpt.get().getTimestampKey();
        } catch (Exception e) {
            log.error("Error getting timestamp field for profile: {}", profileId, e);
            return null;
        }
    }
    
    /**
     * event_stream 테이블에 저장 (JPA 사용, mapped_storage_id 컬럼 포함)
     */
    private void saveEventStream(String eventType, ObjectNode eventData, LocalDateTime eventTime) {
        try {
            // event_data에서 _mapped_storage_id 추출
            String mappedStorageIdStr = eventData.has("_mapped_storage_id")
                ? eventData.get("_mapped_storage_id").asText()
                : null;

            Long mappedStorageId = null;
            if (mappedStorageIdStr != null && mappedStorageIdStr.startsWith("MDS_")) {
                try {
                    mappedStorageId = Long.parseLong(mappedStorageIdStr.replace("MDS_", ""));
                    // event_data에서 _mapped_storage_id 제거 (컬럼으로 이동했으므로)
                    ((ObjectNode) eventData).remove("_mapped_storage_id");
                } catch (NumberFormatException e) {
                    log.warn("Invalid mapped_storage_id format: {}", mappedStorageIdStr);
                }
            }

            // Map<String, Object>로 변환
            Map<String, Object> eventDataMap = objectMapper.convertValue(eventData, Map.class);

            // Event Stream 설계 원칙: eventType을 Raw 데이터에 포함
            eventDataMap.put("event_type", eventType);

            // JPA 엔티티 생성 및 저장 (Raw 데이터 기반)
            // API 요청을 통한 직접 이벤트 저장 시 transactionId는 null
            // 그룹핑 필드들은 event_data JSONB 내부에 저장됨
            EngineEventStreamEntity entity = EngineEventStreamEntity.ofWithMapping(
                    eventDataMap,
                    mappedStorageId,
                    null, // transactionId
                    eventTime
            );

            eventStreamRepository.save(entity);

            log.debug("Event stream saved - eventType: {}, mappedStorageId: {}", eventType, mappedStorageId);

        } catch (Exception e) {
            log.error("Failed to save event stream: eventType={}", eventType, e);
            throw new RuntimeException("Failed to save event stream", e);
        }
    }

    private Boolean extractBooleanField(ObjectNode node, String targetName) {
        if (node == null) {
            return null;
        }
        String matched = null;
        JsonNode raw = null;
        for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
            String name = it.next();
            if (name != null && name.equalsIgnoreCase(targetName)) {
                matched = name;
                raw = node.get(name);
                break;
            }
        }
        if (matched == null || raw == null || raw.isNull()) {
            return null;
        }
        Boolean coerced = coerceToBoolean(raw);
        if (!targetName.equals(matched)) {
            node.remove(matched);
        }
        if (coerced != null) {
            node.put(targetName, coerced);
        }
        return coerced;
    }

    private Boolean coerceToBoolean(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        if (node.isNumber()) {
            return node.intValue() != 0;
        }
        if (node.isTextual()) {
            String text = node.asText();
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
        }
        return null;
    }

    /**
     * null이 아닌 경우에만 추가
     */
    private void addIfNotNull(ObjectNode node, String key, Object value) {
        if (value != null) {
            if (value instanceof String) {
                node.put(key, (String) value);
            } else if (value instanceof Integer) {
                node.put(key, (Integer) value);
            } else if (value instanceof Long) {
                node.put(key, (Long) value);
            } else if (value instanceof BigDecimal) {
                node.put(key, ((BigDecimal) value).doubleValue());
            } else if (value instanceof Boolean) {
                node.put(key, (Boolean) value);
            } else {
                node.put(key, value.toString());
            }
        }
    }
}
