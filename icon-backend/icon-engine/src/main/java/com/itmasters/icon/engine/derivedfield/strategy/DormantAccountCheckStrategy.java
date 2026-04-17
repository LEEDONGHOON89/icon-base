package com.itmasters.icon.engine.derivedfield.strategy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EntityAttributeRepository;
import com.itmasters.icon.engine.derivedfield.strategy.context.ComputationContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

/**
 * DORMANT_ACCOUNT_CHECK Strategy
 *
 * entity_attributes에서 ACCOUNT의 last_transaction_date를 조회하여
 * 휴면 계좌 여부를 판단
 *
 * 설정 예시:
 * {
 *   "receiver_field": "receiver_account",  // 수취 계좌번호 필드
 *   "dormant_days": 365                    // 휴면 기준 일수 (기본값: 365)
 * }
 *
 * 반환값:
 * - true: 휴면 계좌 (dormant_days 이상 거래 없음)
 * - false: 활동 계좌 (최근 거래 있음)
 * - null: 판단 불가 (데이터 부족)
 */
@Slf4j
@Component("engineDormantAccountCheckStrategy")
@RequiredArgsConstructor
public class DormantAccountCheckStrategy implements FieldComputationStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public Object compute(ComputationContext context) {
        try {
            String configJson = context.getComputationConfigJson();
            JsonNode config = objectMapper.readTree(configJson);

            // 설정 값 추출
            String receiverField = config.get("receiver_field").asText();
            
            // dormant_days 기본값: 365일
            int dormantDays = 365;
            if (config.has("dormant_days")) {
                dormantDays = config.get("dormant_days").asInt();
            }

            // receiver_account 값 추출
            String receiverAccount = context.getStringValue(receiverField);
            if (receiverAccount == null || receiverAccount.trim().isEmpty()) {
                log.warn("DORMANT_ACCOUNT_CHECK: receiver_field '{}' is null or empty", receiverField);
                return null;
            }

            // entity_attributes에서 ACCOUNT 조회
            EntityAttributeRepository entityAttributeRepository =
                context.getRepositoryHolder().getEntityAttributeRepository();

            Optional<Map<String, Object>> attributesOpt =
                entityAttributeRepository.findAttributesByEntity("ACCOUNT", receiverAccount);

            if (attributesOpt.isEmpty()) {
                log.debug("DORMANT_ACCOUNT_CHECK: ACCOUNT 엔티티 속성 없음 ({})", receiverAccount);
                return null;
            }

            Map<String, Object> attributes = attributesOpt.get();
            Object lastTxDateObj = attributes.get("last_transaction_date");

            if (lastTxDateObj == null) {
                log.debug("DORMANT_ACCOUNT_CHECK: last_transaction_date 없음 ({})", receiverAccount);
                // last_transaction_date가 없으면 신규 계좌이거나 데이터 없음
                // 보수적으로 휴면 아님으로 판단
                return false;
            }

            // 날짜 파싱
            LocalDate lastTxDate = parseDate(lastTxDateObj.toString());
            if (lastTxDate == null) {
                log.warn("DORMANT_ACCOUNT_CHECK: 날짜 파싱 실패 ({})", lastTxDateObj);
                return null;
            }

            // 현재 날짜와 비교
            LocalDate now = LocalDate.now();
            long daysSinceLastTx = ChronoUnit.DAYS.between(lastTxDate, now);

            boolean isDormant = daysSinceLastTx > dormantDays;

            log.info("DORMANT_ACCOUNT_CHECK - account={}, lastTxDate={}, daysSince={}, threshold={}, isDormant={}",
                    receiverAccount, lastTxDate, daysSinceLastTx, dormantDays, isDormant);

            return isDormant;

        } catch (Exception e) {
            log.error("Failed to compute DORMANT_ACCOUNT_CHECK: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 날짜 문자열을 LocalDate로 파싱
     * 지원 형식: "2024-10-15", "2024-10-15 11:20:00", "20241015"
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }

        try {
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
        return "DORMANT_ACCOUNT_CHECK";
    }
}
