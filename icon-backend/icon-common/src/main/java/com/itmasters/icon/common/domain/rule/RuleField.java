package com.itmasters.icon.common.domain.rule;

import com.itmasters.icon.common.domain.type.FieldType;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 룰 필드 메타데이터 정의
 * 수정일시: 2025-07-22 11:30:00
 */
@Getter
public enum RuleField {
    // 거래 관련 필드
    TRANSACTION_AMOUNT("transaction_amount", "거래 금액",
            FieldCategory.TRANSACTION,
            "거래 금액 (원)",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.LESS_THAN_OR_EQUALS, RuleOperator.BETWEEN)),

    TRANSACTION_TYPE("transaction_type", "거래 유형",
            FieldCategory.TRANSACTION, "거래의 유형",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN),
            ValueOption.of("DEPOSIT", "입금"),
            ValueOption.of("WITHDRAW", "출금"),
            ValueOption.of("TRANSFER", "이체")),

    TRADE_TYPE("trade_type", "매매 유형",
            FieldCategory.TRANSACTION, "매매의 유형 (매수/매도)",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN),
            ValueOption.of("BUY", "매수"),
            ValueOption.of("SELL", "매도")),

    BALANCE_BEFORE("balance_before", "거래 전 잔액",
            FieldCategory.TRANSACTION, "거래 전 계좌 잔액",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.LESS_THAN_OR_EQUALS)),

    BALANCE_AFTER("balance_after", "거래 후 잔액",
            FieldCategory.TRANSACTION, "거래 후 계좌 잔액",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.LESS_THAN_OR_EQUALS)),

    // 거래 관련 추가 필드
    TRANSACTION_TIME("transaction_time", "거래 발생 시간",
            FieldCategory.TRANSACTION, "거래 발생 시간 (HH:mm,HH:mm 형식으로 시간 범위 입력)",
            List.of(RuleOperator.TIME_RANGE, RuleOperator.TIME_RANGE)),

    TRANSACTION_HOUR("transaction_hour", "거래 시간대",
            FieldCategory.TRANSACTION, "거래 시간대 (0-23)",
            List.of(RuleOperator.EQUALS, RuleOperator.HOUR_RANGE, RuleOperator.BETWEEN, RuleOperator.IN)),

    TRANSACTION_MINUTE("transaction_minute", "거래 분",
            FieldCategory.TRANSACTION, "거래 발생 분 (0-59)",
            List.of(RuleOperator.EQUALS, RuleOperator.MINUTE_RANGE, RuleOperator.BETWEEN, RuleOperator.IN)),

    IS_NIGHT_TIME("is_night_time", "야간 거래 여부",
            FieldCategory.TRANSACTION, "야간 시간대 거래 여부",
            List.of(RuleOperator.IS_TRUE, RuleOperator.IS_FALSE)),

    DEPOSIT_TO_WITHDRAW_TIME("deposit_to_withdraw_time", "입금 후 출금까지 시간",
            FieldCategory.TRANSACTION, "입금 후 출금까지 경과 시간 (분 단위)",
            List.of(RuleOperator.LESS_THAN_OR_EQUALS, RuleOperator.WITHIN)),

    CONSECUTIVE_TRANS_COUNT("consecutive_trans_count", "N분 내 연속 거래 횟수",
            FieldCategory.TRANSACTION, "N분 내 연속 거래 횟수",
            List.of(RuleOperator.COUNT_WITHIN)),

    SAME_AMOUNT_TRANS_COUNT("same_amount_trans_count", "동일 금액 거래 횟수",
            FieldCategory.TRANSACTION, "동일 금액 거래 횟수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.EQUALS)),

    TRANS_ACCOUNT_COUNT("trans_account_count", "거래 상대 계좌 수",
            FieldCategory.TRANSACTION, "거래 상대 계좌 개수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.BETWEEN)),

    BALANCE_RATIO("balance_ratio", "잔액 대비 이체 비율",
            FieldCategory.TRANSACTION, "잔액 대비 이체 비율 (퍼센트)",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.BETWEEN)),

    TRANS_SUM_AMOUNT("trans_sum_amount", "거래 합계 금액",
            FieldCategory.TRANSACTION, "거래 합계 금액 (원 단위)",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.BETWEEN,
                    RuleOperator.SUM_WITHIN)),

    TRANS_TO_SAME_ACCOUNT("trans_to_same_account", "동일 계좌 이체 횟수",
            FieldCategory.TRANSACTION, "N분 내 동일 계좌로 이체 횟수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.EQUALS)),

    // 수취/송신 계좌 식별자(표준화)
    BENEFICIARY_ACCOUNT("beneficiary_account", "수취 계좌",
            FieldCategory.TRANSACTION, "수취 계좌 식별자",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN)),
    SENDER_ACCOUNT("sender_account", "송신 계좌",
            FieldCategory.TRANSACTION, "송신 계좌 식별자",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN)),

    // 타인 명의 여부(파생)
    IS_THIRD_PARTY("is_third_party", "타인 명의 여부",
            FieldCategory.TRANSACTION, "수취 계좌 소유자와 고객ID의 일치 여부 파생값",
            List.of(RuleOperator.IS_TRUE, RuleOperator.IS_FALSE)),

    // 접속/로그인 관련 필드
    ACCESS_IP("access_ip", "접속 IP",
            FieldCategory.ACCESS, "사용자 접속 IP 주소",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN,
                    RuleOperator.NOT_IN, RuleOperator.CONTAINS)),

    LOGIN_METHOD("login_method", "로그인 방법",
            FieldCategory.ACCESS, "로그인 인증 방법",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN),
            ValueOption.of("PASSWORD", "비밀번호"),
            ValueOption.of("BIOMETRIC", "생체인증"),
            ValueOption.of("CERTIFICATE", "공인인증서"),
            ValueOption.of("PIN", "PIN번호")),

    ACCESS_COUNTRY("access_country", "접속 국가",
            FieldCategory.ACCESS, "접속 국가 코드",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN,
                    RuleOperator.NOT_IN)),

    LOGIN_FAILURE_COUNT("login_failure_count", "로그인 실패 횟수",
            FieldCategory.ACCESS, "연속 로그인 실패 횟수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.EQUALS)),

    // 위치/접속 관련 추가 필드
    MAIN_ACCESS_COUNTRY("main_access_country", "주 접속 국가",
            FieldCategory.ACCESS, "사용자의 주 접속 국가 코드",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN,
                    RuleOperator.NOT_IN)),

    COUNTRY_CHANGE_TIME("country_change_time", "국가 변경 후 경과 시간",
            FieldCategory.ACCESS, "국가 변경 후 경과 시간 (분 단위)",
            List.of(RuleOperator.LESS_THAN_OR_EQUALS, RuleOperator.GREATER_THAN_OR_EQUALS,
                    RuleOperator.BETWEEN)),

    OVERSEAS_ACCESS_COUNT("overseas_access_count", "해외 접속 횟수",
            FieldCategory.ACCESS, "N일 내 해외 접속 횟수",
            List.of(RuleOperator.EQUALS, RuleOperator.GREATER_THAN_OR_EQUALS,
                    RuleOperator.BETWEEN)),

    ACCESS_HISTORY_PERIOD("access_history_period", "마지막 접속 후 경과 기간",
            FieldCategory.ACCESS, "마지막 접속 후 경과 기간 (일 단위)",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.BETWEEN)),

    ACCESS_IP_COUNT("access_ip_count", "동일 IP 접속 ID 수",
            FieldCategory.ACCESS, "동일 IP에서 접속한 ID 개수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.EQUALS,
                    RuleOperator.BETWEEN, RuleOperator.DISTINCT_COUNT)),

    MAIN_LOGIN_METHOD("main_login_method", "주 사용 로그인 방식",
            FieldCategory.ACCESS, "주로 사용하는 로그인 방식",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN),
            ValueOption.of("PASSWORD", "비밀번호"),
            ValueOption.of("BIOMETRIC", "생체인증"),
            ValueOption.of("CERTIFICATE", "공인인증서"),
            ValueOption.of("PIN", "PIN번호")),

    // 디바이스 관련 필드
    DEVICE_TYPE("device_type", "디바이스 유형",
            FieldCategory.DEVICE, "접속 디바이스 유형",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN),
            ValueOption.of("MOBILE", "모바일"),
            ValueOption.of("PC", "PC"),
            ValueOption.of("TABLET", "태블릿")),

    DEVICE_OS("device_os", "운영체제",
            FieldCategory.DEVICE, "디바이스 운영체제",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN),
            ValueOption.of("ANDROID", "안드로이드"),
            ValueOption.of("IOS", "iOS"),
            ValueOption.of("WINDOWS", "Windows"),
            ValueOption.of("MACOS", "macOS")),

    DEVICE_CHANGED("device_changed", "디바이스 변경 여부",
            FieldCategory.DEVICE, "최근 디바이스 변경 여부",
            List.of(RuleOperator.IS_TRUE, RuleOperator.IS_FALSE)),

    // 디바이스 관련 추가 필드    
    DEVICE_UUID("device_uuid", "디바이스 고유 식별자",
            FieldCategory.DEVICE, "디바이스 UUID",
            List.of(RuleOperator.EQUALS
                    , RuleOperator.NOT_EQUALS
                    , RuleOperator.IN
                    , RuleOperator.EXISTS)),

    DEVICE_MAC("device_mac", "MAC 주소",
            FieldCategory.DEVICE, "디바이스 MAC 주소",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN)),

    DEVICE_CHANGE_TIME("device_change_time", "단말 변경 후 경과 시간",
            FieldCategory.DEVICE, "단말 변경 후 경과 시간 (시간 단위)",
            List.of(RuleOperator.LESS_THAN_OR_EQUALS, RuleOperator.WITHIN)),

    SAME_DEVICE_LOGIN_COUNT("same_device_login_count", "동일 단말 로그인 ID 수",
            FieldCategory.DEVICE, "N일 내 동일 단말에서 로그인한 ID 개수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.EQUALS,
                    RuleOperator.COUNT_WITHIN, RuleOperator.DISTINCT_COUNT)),

    SAME_ACCOUNT_DEVICE_COUNT("same_account_device_count", "동일 계정 접속 단말 수",
            FieldCategory.DEVICE, "N일 내 동일 계정에서 접속한 단말 개수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.EQUALS)),

    DEVICE_ACCESS_HISTORY("device_access_history", "단말 접속 이력 기간",
            FieldCategory.DEVICE, "단말 접속 이력 기간 (개월 단위)",
            List.of(RuleOperator.EQUALS, RuleOperator.LESS_THAN_OR_EQUALS,
                    RuleOperator.NOT_EXISTS)),

    DEVICE_HAS_UUID("device_has_uuid", "고유번호 존재 여부",
            FieldCategory.DEVICE, "디바이스 고유번호 존재 여부",
            List.of(RuleOperator.IS_TRUE, RuleOperator.IS_FALSE)),

    // 계좌 관련 필드
    ACCOUNT_STATUS("account_status", "계좌 상태",
            FieldCategory.ACCOUNT, "계좌의 현재 상태",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS),
            ValueOption.of("ACTIVE", "정상"),
            ValueOption.of("DORMANT", "휴면"),
            ValueOption.of("BLOCKED", "차단")),

    ACCOUNT_AGE_DAYS("account_age_days", "계좌 개설일수",
            FieldCategory.ACCOUNT, "계좌 개설 후 경과 일수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS,
                    RuleOperator.LESS_THAN_OR_EQUALS, RuleOperator.BETWEEN)),

    // 수취 계좌 소유자/고객 식별자(정규화)
    BENEFICIARY_OWNER_ID("beneficiary_owner_id", "수취 계좌 소유자 ID",
            FieldCategory.ACCOUNT, "수취 계좌의 소유자 식별자",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN)),

    // 계좌 관련 추가 필드
    ACCOUNT_HISTORY_PERIOD("account_history_period", "마지막 거래 후 경과 기간",
            FieldCategory.ACCOUNT, "마지막 거래 후 경과 기간 (개월 단위)",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.NOT_EXISTS)),

    ACCOUNT_BALANCE("account_balance", "현재 잔액",
            FieldCategory.ACCOUNT, "계좌 현재 잔액 (원 단위)",
            List.of(RuleOperator.LESS_THAN_OR_EQUALS, RuleOperator.GREATER_THAN_OR_EQUALS,
                    RuleOperator.BETWEEN)),

    NEW_ACCOUNT_DAYS("new_account_days", "계좌 개설 후 경과일",
            FieldCategory.ACCOUNT, "계좌 개설 후 경과일 (일 단위)",
            List.of(RuleOperator.LESS_THAN_OR_EQUALS, RuleOperator.BETWEEN)),

    NON_FACE_ACCOUNT("non_face_account", "비대면 개설 계좌 여부",
            FieldCategory.ACCOUNT, "비대면으로 개설된 계좌 여부",
            List.of(RuleOperator.IS_TRUE, RuleOperator.IS_FALSE)),

    ACCOUNT_OWNER_RELATION("account_owner_relation", "계좌 소유자 관계",
            FieldCategory.ACCOUNT, "계좌 소유자와의 관계",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS),
            ValueOption.of("SELF", "본인"),
            ValueOption.of("OTHER", "타인")),

    ACCOUNT_MEMO("account_memo", "계좌 메모 내용",
            FieldCategory.ACCOUNT, "계좌 메모에 포함된 내용",
            List.of(RuleOperator.CONTAINS, RuleOperator.NOT_CONTAINS)),

    INPUT_ACCOUNT_COUNT("input_account_count", "입금 계좌 수",
            FieldCategory.ACCOUNT, "N시간 내 입금 계좌 개수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.EQUALS)),

    // 고객 정보 필드
    CUSTOMER_AGE("customer_age", "고객 나이",
            FieldCategory.CUSTOMER, "고객의 나이",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.LESS_THAN_OR_EQUALS,
                    RuleOperator.BETWEEN)),

    CUSTOMER_ID("customer_id", "고객 ID",
            FieldCategory.CUSTOMER, "고객 식별자",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN)),

    CUSTOMER_RISK_LEVEL("customer_risk_level", "고객 위험도",
            FieldCategory.CUSTOMER, "고객의 위험도 평가",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN),
            ValueOption.of("LOW", "낮음"),
            ValueOption.of("MEDIUM", "보통"),
            ValueOption.of("HIGH", "높음")),

    // 인증/보안 관련 필드    
    PASSWORD_CHANGED("password_changed", "패스워드 변경 여부",
            FieldCategory.CERTIFICATE, "최근 패스워드 변경 여부",
            List.of(RuleOperator.IS_TRUE, RuleOperator.IS_FALSE)),

    PHONE_CHANGED("phone_changed", "전화번호 변경 여부",
            FieldCategory.CERTIFICATE, "최근 전화번호 변경 여부",
            List.of(RuleOperator.IS_TRUE, RuleOperator.IS_FALSE)),

    OTP_ISSUE_ELAPSED("otp_issue_elapsed", "OTP 발급 후 경과 시간",
            FieldCategory.CERTIFICATE, "OTP 발급 후 경과 시간 (분 단위)",
            List.of(RuleOperator.LESS_THAN_OR_EQUALS, RuleOperator.WITHIN)),

    CERT_ISSUE_ELAPSED("cert_issue_elapsed", "인증서 발급 후 경과 시간",
            FieldCategory.CERTIFICATE, "인증서 발급 후 경과 시간 (분 단위)",
            List.of(RuleOperator.LESS_THAN_OR_EQUALS, RuleOperator.WITHIN)),

    CERT_ISSUE_COUNT("cert_issue_count", "인증서 발급 횟수",
            FieldCategory.CERTIFICATE, "N개월 내 인증서 발급 횟수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.EQUALS)),

    // 대출 관련 필드
    LOAN_EXECUTE_ELAPSED("loan_execute_elapsed", "대출 실행 후 경과 시간",
            FieldCategory.LOAN, "대출 실행 후 경과 시간 (시간 단위)",
            List.of(RuleOperator.LESS_THAN_OR_EQUALS, RuleOperator.WITHIN)),

    LOAN_AMOUNT("loan_amount", "대출 금액",
            FieldCategory.LOAN, "대출 금액 (원 단위)",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.BETWEEN)),

    LOAN_HISTORY_PERIOD("loan_history_period", "마지막 대출 후 경과 기간",
            FieldCategory.LOAN, "마지막 대출 후 경과 기간 (개월 단위)",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.NOT_EXISTS)),

    LOAN_SAME_DAY("loan_same_day", "약정일=실행일 여부",
            FieldCategory.LOAN, "대출 약정일과 실행일이 동일한지 여부",
            List.of(RuleOperator.IS_TRUE, RuleOperator.IS_FALSE)),

    LOAN_TYPE("loan_type", "대출 종류",
            FieldCategory.LOAN, "대출 종류",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN),
            ValueOption.of("MORTGAGE", "주택담보대출"),
            ValueOption.of("CREDIT", "신용대출"),
            ValueOption.of("STOCK_BACKED", "매도담보대출")),

    // 오픈뱅킹 관련 필드
    OPENBANK_REG_ELAPSED("openbank_reg_elapsed", "계좌 등록 후 경과 시간",
            FieldCategory.OPEN_BANKING, "오픈뱅킹 계좌 등록 후 경과 시간 (분 단위)",
            List.of(RuleOperator.LESS_THAN_OR_EQUALS, RuleOperator.WITHIN)),

    OPENBANK_HISTORY_PERIOD("openbank_history_period", "마지막 이용 후 경과 기간",
            FieldCategory.OPEN_BANKING, "오픈뱅킹 마지막 이용 후 경과 기간 (개월 단위)",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.NOT_EXISTS)),

    OPENBANK_WITHDRAW_SUM("openbank_withdraw_sum", "출금 합계 금액",
            FieldCategory.OPEN_BANKING, "N시간 내 오픈뱅킹 출금 합계 금액",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.BETWEEN,
                    RuleOperator.SUM_WITHIN)),

    OPENBANK_TRANS_COUNT("openbank_trans_count", "거래 횟수",
            FieldCategory.OPEN_BANKING, "오픈뱅킹 거래 횟수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.EQUALS)),

    OPENBANK_ORG_NAME("openbank_org_name", "이용 기관명",
            FieldCategory.OPEN_BANKING, "오픈뱅킹 이용 기관명",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN,
                    RuleOperator.CONTAINS)),

    // ATM 관련 필드
    ATM_HISTORY_PERIOD("atm_history_period", "마지막 ATM 이용 후 경과",
            FieldCategory.ATM, "마지막 ATM 이용 후 경과 기간 (개월 단위)",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.NOT_EXISTS)),

    ATM_WITHDRAW_COUNT("atm_withdraw_count", "ATM 출금 횟수",
            FieldCategory.ATM, "N분 내 ATM 출금 횟수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.EQUALS,
                    RuleOperator.COUNT_WITHIN)),

    ATM_LOCATION_COUNT("atm_location_count", "서로 다른 ATM 위치 수",
            FieldCategory.ATM, "서로 다른 ATM 위치 개수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS
                    , RuleOperator.EQUALS
                    , RuleOperator.DISTINCT_COUNT)),

    ATM_BRANCH_TYPES("atm_branch_types", "ATM 지점 종류 수",
            FieldCategory.ATM, "ATM 지점 종류 개수",
            List.of(RuleOperator.GREATER_THAN_OR_EQUALS, RuleOperator.EQUALS)),

    // 블랙/그레이리스트 관련 필드
    BLACKLIST_ID("blacklist_id", "블랙리스트 ID 매칭",
            FieldCategory.BLACKLIST, "블랙리스트 ID 매칭 여부(','로 구분)",
            List.of(RuleOperator.IN)),

    BLACKLIST_IP("blacklist_ip", "블랙리스트 IP 매칭",
            FieldCategory.BLACKLIST, "블랙리스트 IP 매칭 여부(','로 구분)",
            List.of(RuleOperator.IN)),

    BLACKLIST_MAC("blacklist_mac", "블랙리스트 MAC 매칭",
            FieldCategory.BLACKLIST, "블랙리스트 MAC 주소 매칭 여부(','로 구분)",
            List.of(RuleOperator.IN)),

    BLACKLIST_HDD("blacklist_hdd", "블랙리스트 HDD 매칭",
            FieldCategory.BLACKLIST, "블랙리스트 HDD 매칭 여부(','로 구분)",
            List.of(RuleOperator.IN)),

    BLACKLIST_PHONE("blacklist_phone", "블랙리스트 전화번호 매칭",
            FieldCategory.BLACKLIST, "블랙리스트 전화번호 매칭 여부(','로 구분)",
            List.of(RuleOperator.IN)),

    FRAUD_SHARE_RESULT("fraud_share_result", "금융사기정보공유 결과",
            FieldCategory.BLACKLIST, "금융사기정보공유 시스템 조회 결과",
            List.of(RuleOperator.EQUALS, RuleOperator.NOT_EQUALS, RuleOperator.IN),
            ValueOption.of("NORMAL", "정상"),
            ValueOption.of("WITHDRAWAL_BLOCKED", "인출정지"),
            ValueOption.of("SUSPICIOUS", "의심계좌"));

    private final String name;
    private final String label;
    private final FieldCategory category;
    private final String description;
    private final List<RuleOperator> availableOperators;
    private final List<ValueOption> valueOptions;

    // 연산자 지정 + ValueOption이 없는 필드용 생성자
    RuleField(String name, String label,
              FieldCategory category, String description,
              List<RuleOperator> availableOperators) {
        this.name = name;
        this.label = label;
        this.category = category;
        this.description = description;
        this.availableOperators = availableOperators;
        this.valueOptions = Collections.emptyList();
    }

    // 연산자 지정 + ValueOption이 있는 필드용 생성자
    RuleField(String name, String label,
              FieldCategory category, String description,
              List<RuleOperator> availableOperators, ValueOption... options) {
        this.name = name;
        this.label = label;
        this.category = category;
        this.description = description;
        this.availableOperators = availableOperators;
        this.valueOptions = Arrays.asList(options);
    }

    /**
     * 이 필드에서 사용 가능한 연산자 목록 조회
     */
    public List<RuleOperator> getAvailableOperators() {
        return availableOperators;
    }

    /**
     * 필드명으로 RuleField 찾기
     */
    public static RuleField findByName(String name) {
        return Arrays.stream(values())
                .filter(field -> field.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * fromFieldName 메서드 (하위 호환성)
     */
    public static RuleField fromFieldName(String fieldName) {
        return findByName(fieldName);
    }

    /**
     * getFieldType 메서드 - 연산자를 기반으로 타입 추론

     * 연산자의 특성을 분석하여 필드 타입을 추론합니다.
     * 예: COUNT_WITHIN, SUM_WITHIN → NUMBER
     * TIME_RANGE → TIME
     * EQUALS + "여부" → BOOLEAN
     */
    public FieldType getFieldType() {
        // 1. 집계 연산자는 항상 숫자 타입
        if (availableOperators.stream().anyMatch(op -> op.isAggregateOperator())) {
            return FieldType.NUMBER;
        }

        // 2. TIME_RANGE 연산자를 사용하면 TIME 타입
        if (availableOperators.contains(RuleOperator.TIME_RANGE)) {
            return FieldType.TIME;
        }

        // 3. ValueOption이 있으면 STRING (선택 목록이 있는 경우)
        if (!valueOptions.isEmpty()) {
            return FieldType.STRING;
        }

        // 4. BOOLEAN 타입 추론 (필드명과 연산자 기반)
        // IS_TRUE/IS_FALSE 연산자를 사용하는 경우
        if (availableOperators.contains(RuleOperator.IS_TRUE) || 
                availableOperators.contains(RuleOperator.IS_FALSE)) {
            return FieldType.BOOLEAN;
        }
        
        // EQUALS 연산자만 있고, 필드명이 boolean을 나타내는 경우 (legacy support)
        if (availableOperators.size() == 1 && availableOperators.contains(RuleOperator.EQUALS) &&
                (name.startsWith("is_") || name.endsWith("_changed") ||
                        name.endsWith("_여부") || label.endsWith("여부") ||
                        name.startsWith("has_"))) {
            return FieldType.BOOLEAN;
        }

        // 5. IN, CONTAINS 연산자를 사용하면 STRING
        if (availableOperators.contains(RuleOperator.IN) ||
                availableOperators.contains(RuleOperator.CONTAINS)) {
            return FieldType.STRING;
        }

        // 6. 숫자 타입 추론 (필드명 기반)
        // _country로 끝나는 경우는 제외 (국가 코드는 문자열)
        if (!name.endsWith("_country") &&
                (name.contains("_count") || name.contains("_amount") ||
                        name.contains("_age") || name.contains("_days") ||
                        name.contains("_hour") || name.contains("_minute") ||
                        name.contains("_time") || name.contains("_ratio") ||
                        name.contains("_period") || name.contains("_elapsed"))) {
            return FieldType.NUMBER;
        }

        // 7. 기본값은 STRING
        return FieldType.STRING;
    }

    /**
     * 표준 필드의 canonical ID(소문자 snake_case)를 반환
     * RuleField 상수명(대문자)과 구분되는 외부 식별자입니다.
     * DB standard_fields.standard_field_id, rule.condition_data.fieldName과 매칭에 사용하세요.
     */
    public String getId() {
        return this.name; // canonical id (e.g., "transaction_amount")
    }
}
