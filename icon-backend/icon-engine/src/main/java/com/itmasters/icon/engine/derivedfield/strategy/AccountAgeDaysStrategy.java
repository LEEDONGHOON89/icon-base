package com.itmasters.icon.engine.derivedfield.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityAttributeRepository;
import com.itmasters.icon.engine.derivedfield.strategy.context.ComputationContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

/**
 * ACCOUNT_AGE_DAYS Strategy
 *
 * ACCOUNT 엔티티의 account_open_date와 이벤트의 transaction_datetime을 비교하여
 * 계좌 개설 후 경과 일수를 계산합니다.
 *
 * 설정 예시:
 * {
 *   "entity_type": "ACCOUNT",              // 조회할 엔티티 타입
 *   "entity_id_field": "customer_id",      // 이벤트에서 엔티티 ID로 사용할 필드
 *   "date_field": "account_open_date",     // 엔티티에서 조회할 날짜 필드
 *   "event_date_field": "transaction_datetime"  // 이벤트의 날짜 필드
 * }
 *
 * 반환값:
 * - 정수: 계좌 개설 후 경과 일수 (0 이상)
 * - null: 계산 불가 (데이터 부족)
 */
@Slf4j
@Component("engineAccountAgeDaysStrategy")
@RequiredArgsConstructor
public class AccountAgeDaysStrategy implements FieldComputationStrategy {

    /**
     * computation_config JSON 키 정의
     */
    @Getter
    @RequiredArgsConstructor
    public enum ConfigKey {
        ENTITY_TYPE("entity_type", "ACCOUNT"),
        ENTITY_ID_FIELD("entity_id_field", "customer_id"),
        DATE_FIELD("date_field", "account_open_date"),
        EVENT_DATE_FIELD("event_date_field", "transaction_datetime");

        private final String key;
        private final String defaultValue;

        /**
         * JsonNode에서 값을 추출하거나 기본값 반환
         */
        public String getValueOrDefault(JsonNode config) {
            return config.has(key) ? config.get(key).asText() : defaultValue;
        }
    }

    private final ObjectMapper objectMapper;

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 설정 값 추출 (enum 사용)
            String entityType = ConfigKey.ENTITY_TYPE.getValueOrDefault(config);
            String entityIdField = ConfigKey.ENTITY_ID_FIELD.getValueOrDefault(config);
            String dateField = ConfigKey.DATE_FIELD.getValueOrDefault(config);
            String eventDateField = ConfigKey.EVENT_DATE_FIELD.getValueOrDefault(config);

            // 이벤트에서 entity_id 추출
            String entityId = context.getStringValue(entityIdField);
            if (entityId == null || entityId.trim().isEmpty()) {
                log.warn("ACCOUNT_AGE_DAYS: entity_id_field '{}' is null or empty", entityIdField);
                return null;
            }

            // 이벤트에서 거래일시 추출
            String eventDateStr = context.getStringValue(eventDateField);
            if (eventDateStr == null || eventDateStr.trim().isEmpty()) {
                log.warn("ACCOUNT_AGE_DAYS: event_date_field '{}' is null or empty", eventDateField);
                return null;
            }

            LocalDate eventDate = parseDate(eventDateStr);
            if (eventDate == null) {
                log.warn("ACCOUNT_AGE_DAYS: Failed to parse event date '{}'", eventDateStr);
                return null;
            }

            // entity_attributes에서 ACCOUNT 조회
            EntityAttributeRepository entityAttributeRepository =
                context.getRepositoryHolder().getEntityAttributeRepository();

            Optional<Map<String, Object>> attributesOpt =
                entityAttributeRepository.findAttributesByEntity(entityType, entityId);

            if (attributesOpt.isEmpty()) {
                log.debug("ACCOUNT_AGE_DAYS: Entity attributes not found ({}:{})", entityType, entityId);
                return null;
            }

            Map<String, Object> attributes = attributesOpt.get();
            Object accountOpenDateObj = attributes.get(dateField);

            if (accountOpenDateObj == null) {
                log.debug("ACCOUNT_AGE_DAYS: {} not found in entity attributes ({}:{})", dateField, entityType, entityId);
                return null;
            }

            // 계좌 개설일 파싱
            LocalDate accountOpenDate = parseDate(accountOpenDateObj.toString());
            if (accountOpenDate == null) {
                log.warn("ACCOUNT_AGE_DAYS: Failed to parse account open date '{}'", accountOpenDateObj);
                return null;
            }

            // 일수 계산 (거래일 - 개설일)
            long daysSinceOpen = ChronoUnit.DAYS.between(accountOpenDate, eventDate);

            // 음수인 경우 0으로 처리 (개설일이 거래일보다 미래인 경우)
            if (daysSinceOpen < 0) {
                log.warn("ACCOUNT_AGE_DAYS: Negative days calculated. accountOpenDate={}, eventDate={}",
                        accountOpenDate, eventDate);
                daysSinceOpen = 0;
            }

            log.info("ACCOUNT_AGE_DAYS - entityId={}, accountOpenDate={}, eventDate={}, days={}",
                    entityId, accountOpenDate, eventDate, daysSinceOpen);

            return (int) daysSinceOpen;

        } catch (Exception e) {
            log.error("Failed to compute ACCOUNT_AGE_DAYS: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 날짜 문자열을 LocalDate로 파싱
     * 지원 형식: "2024-10-15", "2024-10-15 11:20:00", "2024-10-15T11:20:00", "20241015"
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        try {
            // ISO DateTime 형식: "2024-10-15T11:20:00"
            if (dateStr.contains("T")) {
                return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME).toLocalDate();
            }

            // ISO 형식: "2024-10-15" 또는 "2024-10-15 11:20:00"
            if (dateStr.contains("-")) {
                String datePart = dateStr.split(" ")[0]; // 시간 부분 제거
                return LocalDate.parse(datePart, DateTimeFormatter.ISO_LOCAL_DATE);
            }

            // "20241015" 형식
            if (dateStr.length() == 8 && dateStr.matches("\\d{8}")) {
                return LocalDate.parse(dateStr, DateTimeFormatter.BASIC_ISO_DATE);
            }

            // 기본 파싱 시도
            return LocalDate.parse(dateStr);

        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", dateStr, e);
            return null;
        }
    }

    @Override
    public String getSupportedComputationType() {
        return "ACCOUNT_AGE_DAYS";
    }
}
