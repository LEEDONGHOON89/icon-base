package com.itmasters.icon.common.domain.rule.condition;

import com.itmasters.icon.common.domain.RuleDomain;
import com.itmasters.icon.common.domain.rule.RuleOperator;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;

/**
 * ATM 거래 관련 룰 조건 처리 (ATM 도메인 전용)

 * 지원 조건:
 * - A01: ATM 고액 출금 (50만원 이상)
 * - A02: ATM 연속 출금 (5회 이상)
 * - A03: ATM 야간 출금 (22시-06시)
 * - A04: ATM 휴일/주말 출금
 * - A05: ATM 잔액 한도 초과 출금
 * - A06: ATM 일일 한도 초과 출금
 * - A07: ATM 신규 지역 출금 (처음 방문 지역)
 * - A08: ATM 해외 출금
 * - A09: ATM 단시간 다량 출금 (1시간 내 3회 이상)
 * - A10: ATM 소액 다빈도 출금 (1만원 이하 10회 이상)
 * - A11: ATM 카드 분실 신고 후 출금
 * - A12: ATM 무카드 출금 (모바일 출금)
 */
@Slf4j
public class ATMCondition extends RuleConditionV2 {

    // standard_fields 테이블 기반 필수 필드들
    private static final Set<String> REQUIRED_FIELDS_FOR_ATM = Set.of(
            "atm_id",                 // ATM ID (standard_fields에 있음)
            "atm_location",           // ATM 위치 (standard_fields에 있음)
            "atm_bank_code",          // ATM 은행 코드 (standard_fields에 있음)
            "terminal_id"             // 터미널 ID (standard_fields에 있음)
    );

    private static final Set<String> REQUIRED_FIELDS_FOR_AMOUNT = Set.of(
            "withdrawal_amount",      // 출금 금액 (standard_fields에 있음)
            "transaction_amount",     // 거래 금액 (standard_fields에 있음)
            "amount"                  // 금액 (standard_fields에 있음)
    );

    private static final Set<String> REQUIRED_FIELDS_FOR_ACCOUNT = Set.of(
            "account_id",             // 계좌 ID (standard_fields에 있음)
            "account_balance",        // 계좌 잔액 (standard_fields에 있음)
            "card_number"             // 카드 번호 (standard_fields에 있음)
    );

    private static final Set<String> REQUIRED_FIELDS_FOR_TIME = Set.of(
            "atm_dt",                 // ATM 거래 시간 (standard_fields에 있음)
            "transaction_date",       // 거래 일시 (standard_fields에 있음)
            "created_at"              // 생성 일시 (standard_fields에 있음)
    );

    private static final Set<String> REQUIRED_FIELDS_FOR_LOCATION = Set.of(
            "atm_location",           // ATM 위치 (standard_fields에 있음)
            "location_code",          // 위치 코드 (standard_fields에 있음)
            "region_code"             // 지역 코드 (standard_fields에 있음)
    );

    // 금액 기준
    private static final BigDecimal HIGH_WITHDRAWAL_AMOUNT = new BigDecimal("500000"); // 50만원
    private static final BigDecimal SMALL_AMOUNT_THRESHOLD = new BigDecimal("10000");  // 1만원
    private static final BigDecimal DAILY_LIMIT_THRESHOLD = new BigDecimal("3000000"); // 300만원 (일반적 일일한도)

    // 시간 기준
    private static final LocalTime NIGHT_START = LocalTime.of(22, 0);    // 22:00
    private static final LocalTime NIGHT_END = LocalTime.of(6, 0);       // 06:00

    // 횟수 기준
    private static final int CONSECUTIVE_WITHDRAWAL_COUNT = 5;    // 연속 출금 기준
    private static final int HIGH_FREQUENCY_COUNT = 3;           // 단시간 다량 출금 기준
    private static final int SMALL_AMOUNT_FREQUENCY = 10;        // 소액 다빈도 출금 기준

    // 국가 코드
    private static final String KOREA_COUNTRY_CODE = "KR";

    @Override
    public boolean supports(RuleDomain domain) {
        return domain == RuleDomain.ATM;
    }

    @Override
    public boolean evaluate(String groupKey, Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;

            RuleOperator operator = extractOperator(rule);
            Object conditionValue = rule.get("condition_value");

            // EventStream에서 해당 groupKey의 현재 ATM 거래 데이터 조회
            Map<String, Object> eventData = getCurrentEventData(groupKey);

            return switch (operator) {
                case GREATER_THAN_OR_EQUALS -> evaluateAmountGreaterThanOrEquals(eventData, conditionValue, groupKey);
                case REPEATED_TIMES -> evaluateRepeatedWithdrawals(eventData, conditionValue, groupKey);
                case WITHIN_HOURS -> evaluateTimeWithinHours(eventData, conditionValue);
                case EQUALS -> evaluateATMEquals(eventData, conditionValue, groupKey);
                case EXCEEDS_LIMIT -> evaluateExceedsLimit(eventData, conditionValue, groupKey);
                case NO_HISTORY -> evaluateNewLocationWithdrawal(eventData, groupKey);
                case FREQUENCY_WITHIN_TIME -> evaluateHighFrequencyWithdrawal(eventData, conditionValue, groupKey);
                default -> {
                    log.error("지원하지 않는 연산자: {} (ATM 도메인)", operator);
                    throw new IllegalArgumentException("ATM 도메인에서 지원하지 않는 연산자: " + operator);
                }
            };

        } catch (Exception e) {
            log.error("ATMCondition 평가 중 오류 발생 - groupKey: {}, error: {}", groupKey, e.getMessage(), e);
            throw new RuntimeException("ATM 조건 평가 실패", e);
        }
    }

    /**
     * A01: ATM 고액 출금 조건 평가
     */
    private boolean evaluateAmountGreaterThanOrEquals(Map<String, Object> eventData, Object conditionValue, String groupKey) {
        validateRequiredFieldsForAmount(eventData);

        BigDecimal withdrawalAmount = extractWithdrawalAmount(eventData);
        BigDecimal targetAmount = new BigDecimal(conditionValue.toString());

        boolean result = withdrawalAmount.compareTo(targetAmount) >= 0;
        log.debug("ATM 고액 출금 조건 평가: {} >= {} = {}", withdrawalAmount, targetAmount, result);
        return result;
    }

    /**
     * A02: ATM 연속 출금 조건 평가
     */
    private boolean evaluateRepeatedWithdrawals(Map<String, Object> eventData, Object conditionValue, String groupKey) {
        validateRequiredFieldsForATM(eventData);
        validateRequiredFieldsForAccount(eventData);

        int targetCount = Integer.parseInt(conditionValue.toString());
        String accountId = (String) eventData.get("account_id");

        // EventStream에서 해당 계좌의 연속 ATM 출금 횟수 조회
        int consecutiveCount = getConsecutiveATMWithdrawalCount(accountId);

        boolean result = consecutiveCount >= targetCount;
        log.debug("ATM 연속 출금 조건 평가: account={}, consecutive={} >= {} = {}",
                accountId, consecutiveCount, targetCount, result);
        return result;
    }

    /**
     * A03: ATM 야간 출금 조건 평가
     */
    private boolean evaluateTimeWithinHours(Map<String, Object> eventData, Object conditionValue) {
        validateRequiredFieldsForTime(eventData);

        LocalDateTime transactionTime = extractTransactionTime(eventData);
        LocalTime time = transactionTime.toLocalTime();
        String timeCondition = conditionValue.toString();

        return switch (timeCondition) {
            case "NIGHT" -> isNightTime(time);
            case "BUSINESS_HOURS" -> isBusinessHours(time);
            default -> {
                log.warn("알 수 없는 시간 조건: {}", timeCondition);
                yield false;
            }
        };
    }

    /**
     * A04, A08, A11, A12: 특정 ATM 조건 평가
     */
    private boolean evaluateATMEquals(Map<String, Object> eventData, Object conditionValue, String groupKey) {
        String conditionStr = conditionValue.toString();

        return switch (conditionStr) {
            case "HOLIDAY_WEEKEND" -> evaluateHolidayWeekendWithdrawal(eventData);
            case "OVERSEAS" -> evaluateOverseasWithdrawal(eventData);
            case "AFTER_CARD_LOSS_REPORT" -> evaluateAfterCardLossReport(eventData, groupKey);
            case "CARDLESS" -> evaluateCardlessWithdrawal(eventData);
            default -> {
                log.warn("알 수 없는 ATM 조건: {}", conditionStr);
                yield false;
            }
        };
    }

    /**
     * A05, A06: 한도 초과 조건 평가
     */
    private boolean evaluateExceedsLimit(Map<String, Object> eventData, Object conditionValue, String groupKey) {
        String limitType = conditionValue.toString();

        return switch (limitType) {
            case "BALANCE_LIMIT" -> evaluateExceedsBalanceLimit(eventData);
            case "DAILY_LIMIT" -> evaluateExceedsDailyLimit(eventData, groupKey);
            default -> {
                log.warn("알 수 없는 한도 조건: {}", limitType);
                yield false;
            }
        };
    }

    /**
     * A07: ATM 신규 지역 출금 조건 평가
     */
    private boolean evaluateNewLocationWithdrawal(Map<String, Object> eventData, String groupKey) {
        validateRequiredFieldsForLocation(eventData);
        validateRequiredFieldsForAccount(eventData);

        String currentLocation = (String) eventData.get("atm_location");
        String accountId = (String) eventData.get("account_id");

        // EventStream에서 해당 계좌의 ATM 위치 사용 이력 확인
        boolean hasLocationHistory = hasATMLocationHistory(accountId, currentLocation);

        boolean result = !hasLocationHistory;
        log.debug("ATM 신규 지역 출금 조건 평가: account={}, location={}, isNew={}",
                accountId, currentLocation, result);
        return result;
    }

    /**
     * A09: ATM 단시간 다량 출금 조건 평가
     */
    private boolean evaluateHighFrequencyWithdrawal(Map<String, Object> eventData, Object conditionValue, String groupKey) {
        validateRequiredFieldsForAccount(eventData);

        String[] parts = conditionValue.toString().split(","); // "1,3" (1시간, 3회)
        int hours = Integer.parseInt(parts[0]);
        int minCount = Integer.parseInt(parts[1]);

        String accountId = (String) eventData.get("account_id");

        // EventStream에서 지정 시간 내 ATM 출금 횟수 조회
        int withdrawalCount = getATMWithdrawalCountInHours(accountId, hours);

        boolean result = withdrawalCount >= minCount;
        log.debug("ATM 단시간 다량 출금 조건 평가: account={}, hours={}, count={} >= {} = {}",
                accountId, hours, withdrawalCount, minCount, result);
        return result;
    }

    // 개별 조건 평가 메서드들
    private boolean isNightTime(LocalTime time) {
        return time.isAfter(NIGHT_START) || time.isBefore(NIGHT_END);
    }

    private boolean isBusinessHours(LocalTime time) {
        return !isNightTime(time);
    }

    private boolean evaluateHolidayWeekendWithdrawal(Map<String, Object> eventData) {
        validateRequiredFieldsForTime(eventData);

        LocalDateTime transactionTime = extractTransactionTime(eventData);
        int dayOfWeek = transactionTime.getDayOfWeek().getValue();

        boolean result = dayOfWeek == 6 || dayOfWeek == 7; // 토요일, 일요일
        // TODO: 공휴일 체크 추가 필요
        log.debug("ATM 휴일/주말 출금 조건 평가: date={}, dayOfWeek={}, isHolidayWeekend={}",
                transactionTime.toLocalDate(), dayOfWeek, result);
        return result;
    }

    private boolean evaluateOverseasWithdrawal(Map<String, Object> eventData) {
        validateRequiredFieldsForATM(eventData);

        String countryCode = (String) eventData.getOrDefault("atm_country",
                eventData.get("country_code"));

        boolean result = !KOREA_COUNTRY_CODE.equals(countryCode);
        log.debug("ATM 해외 출금 조건 평가: country={}, isOverseas={}", countryCode, result);
        return result;
    }

    private boolean evaluateAfterCardLossReport(Map<String, Object> eventData, String groupKey) {
        validateRequiredFieldsForAccount(eventData);

        String cardNumber = (String) eventData.get("card_number");

        // EventStream에서 카드 분실 신고 이력 확인
        boolean hasLossReport = hasRecentCardLossReport(cardNumber);

        boolean result = hasLossReport;
        log.debug("ATM 카드 분실 신고 후 출금 조건 평가: card={}, hasLossReport={}",
                maskCardNumber(cardNumber), result);
        return result;
    }

    private boolean evaluateCardlessWithdrawal(Map<String, Object> eventData) {
        String withdrawalType = (String) eventData.getOrDefault("withdrawal_type",
                eventData.get("transaction_method"));

        boolean result = "CARDLESS".equals(withdrawalType) || "MOBILE".equals(withdrawalType);
        log.debug("ATM 무카드 출금 조건 평가: type={}, isCardless={}", withdrawalType, result);
        return result;
    }

    private boolean evaluateExceedsBalanceLimit(Map<String, Object> eventData) {
        validateRequiredFieldsForAmount(eventData);
        validateRequiredFieldsForAccount(eventData);

        BigDecimal withdrawalAmount = extractWithdrawalAmount(eventData);
        BigDecimal accountBalance = extractAccountBalance(eventData);

        boolean result = withdrawalAmount.compareTo(accountBalance) > 0;
        log.debug("ATM 잔액 한도 초과 조건 평가: withdrawal={}, balance={}, exceeds={}",
                withdrawalAmount, accountBalance, result);
        return result;
    }

    private boolean evaluateExceedsDailyLimit(Map<String, Object> eventData, String groupKey) {
        validateRequiredFieldsForAmount(eventData);
        validateRequiredFieldsForAccount(eventData);

        BigDecimal withdrawalAmount = extractWithdrawalAmount(eventData);
        String accountId = (String) eventData.get("account_id");

        // 오늘 하루 총 출금 금액 + 현재 출금 금액
        BigDecimal todayTotal = getTodayATMWithdrawalTotal(accountId);
        BigDecimal totalAfterWithdrawal = todayTotal.add(withdrawalAmount);

        boolean result = totalAfterWithdrawal.compareTo(DAILY_LIMIT_THRESHOLD) > 0;
        log.debug("ATM 일일 한도 초과 조건 평가: today={}, current={}, total={}, exceeds={}",
                todayTotal, withdrawalAmount, totalAfterWithdrawal, result);
        return result;
    }

    /**
     * EventStream에서 현재 이벤트 데이터 조회 (임시 구현)
     */
    private Map<String, Object> getCurrentEventData(String groupKey) {
        // TODO: 실제로는 EventStreamService를 통해 조회
        return Map.of();
    }

    /**
     * EventData에서 출금 금액 추출
     */
    private BigDecimal extractWithdrawalAmount(Map<String, Object> eventData) {
        if (eventData.containsKey("withdrawal_amount")) {
            return new BigDecimal(eventData.get("withdrawal_amount").toString());
        }
        if (eventData.containsKey("transaction_amount")) {
            return new BigDecimal(eventData.get("transaction_amount").toString());
        }
        if (eventData.containsKey("amount")) {
            return new BigDecimal(eventData.get("amount").toString());
        }
        throw new IllegalArgumentException("출금 금액을 추출할 수 있는 필드가 없습니다.");
    }

    /**
     * EventData에서 계좌 잔액 추출
     */
    private BigDecimal extractAccountBalance(Map<String, Object> eventData) {
        if (eventData.containsKey("account_balance")) {
            return new BigDecimal(eventData.get("account_balance").toString());
        }
        throw new IllegalArgumentException("계좌 잔액을 추출할 수 있는 필드가 없습니다.");
    }

    /**
     * EventData에서 거래 시간 추출
     */
    private LocalDateTime extractTransactionTime(Map<String, Object> eventData) {
        String timeStr = null;
        if (eventData.containsKey("atm_dt")) {
            timeStr = (String) eventData.get("atm_dt");
        } else if (eventData.containsKey("transaction_date")) {
            timeStr = (String) eventData.get("transaction_date");
        } else if (eventData.containsKey("created_at")) {
            timeStr = (String) eventData.get("created_at");
        }

        if (timeStr != null) {
            return LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        }

        throw new IllegalArgumentException("거래 시간을 추출할 수 있는 필드가 없습니다.");
    }

    /**
     * 카드번호 마스킹 처리
     */
    private String maskCardNumber(String cardNumber) {
        if (cardNumber == null || cardNumber.length() < 8) {
            return "****";
        }
        return cardNumber.substring(0, 4) + "****" + cardNumber.substring(cardNumber.length() - 4);
    }

    // EventStream 조회 메서드들 (임시 구현)
    private int getConsecutiveATMWithdrawalCount(String accountId) {
        // TODO: EventStreamService를 통해 구현
        return 0;
    }

    private boolean hasATMLocationHistory(String accountId, String location) {
        // TODO: EventStreamService를 통해 구현
        return false;
    }

    private int getATMWithdrawalCountInHours(String accountId, int hours) {
        // TODO: EventStreamService를 통해 구현
        return 0;
    }

    private boolean hasRecentCardLossReport(String cardNumber) {
        // TODO: EventStreamService를 통해 구현
        return false;
    }

    private BigDecimal getTodayATMWithdrawalTotal(String accountId) {
        // TODO: EventStreamService를 통해 구현
        return BigDecimal.ZERO;
    }

    // 필드 검증 메서드들
    private void validateRequiredFieldsForATM(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_ATM.stream()
                .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);

        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                    String.format("ATM 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s",
                            REQUIRED_FIELDS_FOR_ATM));
        }
    }

    private void validateRequiredFieldsForAmount(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_AMOUNT.stream()
                .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);

        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                    String.format("ATM 금액 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s",
                            REQUIRED_FIELDS_FOR_AMOUNT));
        }
    }

    private void validateRequiredFieldsForAccount(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_ACCOUNT.stream()
                .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);

        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                    String.format("ATM 계좌 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s",
                            REQUIRED_FIELDS_FOR_ACCOUNT));
        }
    }

    private void validateRequiredFieldsForTime(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_TIME.stream()
                .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);

        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                    String.format("ATM 시간 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s",
                            REQUIRED_FIELDS_FOR_TIME));
        }
    }

    private void validateRequiredFieldsForLocation(Map<String, Object> eventData) {
        boolean hasAnyRequiredField = REQUIRED_FIELDS_FOR_LOCATION.stream()
                .anyMatch(field -> eventData.containsKey(field) && eventData.get(field) != null);

        if (!hasAnyRequiredField) {
            throw new IllegalArgumentException(
                    String.format("ATM 위치 조건을 위한 필수 필드가 없습니다. 필요한 필드 중 하나: %s",
                            REQUIRED_FIELDS_FOR_LOCATION));
        }
    }

    /**
     * RuleEntity에서 연산자 추출 (임시 구현)
     */
    private RuleOperator extractOperator(Map<String, Object> rule) {
        // TODO: 실제 RuleEntity 구조에 맞게 수정
        String operatorStr = (String) rule.get("operator");
        return RuleOperator.valueOf(operatorStr);
    }

    @Override
    public String getConditionType() {
        return "ATM_CONDITION";
    }

    @Override
    public String toHumanReadableString(Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;

            RuleOperator operator = extractOperator(rule);
            Object value = rule.get("condition_value");

            return switch (operator) {
                case GREATER_THAN_OR_EQUALS -> String.format("ATM 출금 %s원 이상", formatAmount(value));
                case REPEATED_TIMES -> String.format("ATM 연속 출금 %s회 이상", value);
                case WITHIN_HOURS -> switch (value.toString()) {
                    case "NIGHT" -> "ATM 야간 출금";
                    case "BUSINESS_HOURS" -> "ATM 업무시간 출금";
                    default -> String.format("ATM 시간 조건: %s", value);
                };
                case EQUALS -> switch (value.toString()) {
                    case "HOLIDAY_WEEKEND" -> "ATM 휴일/주말 출금";
                    case "OVERSEAS" -> "ATM 해외 출금";
                    case "AFTER_CARD_LOSS_REPORT" -> "카드 분실 신고 후 ATM 출금";
                    case "CARDLESS" -> "ATM 무카드 출금";
                    default -> String.format("ATM 조건: %s", value);
                };
                case EXCEEDS_LIMIT -> switch (value.toString()) {
                    case "BALANCE_LIMIT" -> "ATM 잔액 한도 초과";
                    case "DAILY_LIMIT" -> "ATM 일일 한도 초과";
                    default -> String.format("ATM 한도 초과: %s", value);
                };
                case NO_HISTORY -> "ATM 신규 지역 출금";
                case FREQUENCY_WITHIN_TIME -> {
                    String[] parts = value.toString().split(",");
                    if (parts.length == 2) {
                        yield String.format("ATM %s시간 내 %s회 이상 출금", parts[0], parts[1]);
                    } else {
                        yield String.format("ATM 다빈도 출금: %s", value);
                    }
                }
                default -> String.format("ATM 조건: %s %s", operator, value);
            };

        } catch (Exception e) {
            return "ATM 조건 (표시 오류)";
        }
    }

    /**
     * 금액 포맷팅 (천 단위 구분)
     */
    private String formatAmount(Object amount) {
        try {
            BigDecimal decimal = new BigDecimal(amount.toString());
            return String.format("%,d", decimal.longValue());
        } catch (Exception e) {
            return amount.toString();
        }
    }

    @Override
    public boolean requiresHistoryData() {
        return true; // ATM 조건은 대부분 이력 데이터가 필요
    }

    @Override
    public boolean isValidCondition(Object ruleEntity) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> rule = (Map<String, Object>) ruleEntity;

            // 필수 필드 검증
            return rule.containsKey("operator") &&
                    rule.containsKey("condition_value") &&
                    extractOperator(rule) != null;

        } catch (Exception e) {
            log.error("ATM 조건 유효성 검증 실패: {}", e.getMessage());
            return false;
        }
    }
}