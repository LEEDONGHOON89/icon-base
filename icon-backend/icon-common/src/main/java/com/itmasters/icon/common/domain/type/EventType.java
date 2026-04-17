package com.itmasters.icon.common.domain.type;

/**
 * user_activity_log의 이벤트 타입 Enum
 * 거래 유형을 영문으로 매핑
 */
public enum EventType {
    // 금융 거래 관련
    TRANSFER("이체"),
    WITHDRAW("출금"),
    ATM_WITHDRAW("ATM출금"),
    DEPOSIT("입금"),
    
    // 인증 관련
    LOGIN("로그인"),
    LOGOUT("로그아웃"),
    OTP_ISSUE("OTP발급"),
    CERT_ISSUE("인증서발급"),
    
    // 기타
    TRANSACTION("거래"),  // 기본 거래
    UNKNOWN("알수없음");
    
    private final String koreanName;
    
    EventType(String koreanName) {
        this.koreanName = koreanName;
    }
    
    public String getKoreanName() {
        return koreanName;
    }
    
    /**
     * 한글 거래 타입을 EventType으로 변환
     * @param koreanType 한글 거래 타입
     * @return 매핑된 EventType, 없으면 UNKNOWN
     */
    public static EventType fromKorean(String koreanType) {
        if (koreanType == null || koreanType.isEmpty()) {
            return UNKNOWN;
        }
        
        // 정확한 매칭 시도
        for (EventType type : values()) {
            if (type.koreanName.equals(koreanType)) {
                return type;
            }
        }
        
        // 부분 매칭 (예: "ATM출금" -> ATM_WITHDRAW)
        String normalized = koreanType.trim();
        if (normalized.contains("ATM") && normalized.contains("출금")) {
            return ATM_WITHDRAW;
        }
        if (normalized.contains("출금")) {
            return WITHDRAW;
        }
        if (normalized.contains("이체")) {
            return TRANSFER;
        }
        if (normalized.contains("입금")) {
            return DEPOSIT;
        }
        if (normalized.contains("로그인")) {
            return LOGIN;
        }
        if (normalized.contains("로그아웃")) {
            return LOGOUT;
        }
        if (normalized.contains("OTP")) {
            return OTP_ISSUE;
        }
        if (normalized.contains("인증서")) {
            return CERT_ISSUE;
        }
        
        // 기본값
        return normalized.contains("거래") ? TRANSACTION : UNKNOWN;
    }
    
    /**
     * 필드 데이터로부터 이벤트 타입 추론
     * @param data 데이터 맵
     * @return 추론된 EventType
     */
    public static EventType inferFromData(java.util.Map<String, Object> data) {
        // transaction_type 또는 TRX_TYPE 필드 확인
        String trxType = (String) data.get("transaction_type");
        if (trxType == null) {
            trxType = (String) data.get("TRX_TYPE");
        }
        
        if (trxType != null) {
            return fromKorean(trxType);
        }
        
        // 특수 필드로 타입 추론
        if (data.get("OTP_ISSUED_AT") != null) {
            return OTP_ISSUE;
        }
        if (data.get("CERT_ISSUED_AT") != null) {
            return CERT_ISSUE;
        }
        if (data.get("LOGIN_TIME") != null || data.get("LOGIN_DT") != null) {
            return LOGIN;
        }
        
        return UNKNOWN;
    }
}