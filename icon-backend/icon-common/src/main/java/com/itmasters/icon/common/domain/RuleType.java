package com.itmasters.icon.common.domain;

/**
 * 룰 타입 정의
 * 각 타입별로 다른 평가 로직이 적용됨
 */
public enum RuleType {
    /**
     * 단순 비교 룰 (기존 룰)
     * 현재 데이터만으로 평가 가능
     */
    SIMPLE("단순 조건", "현재 데이터의 단순 비교"),
    
    /**
     * 기기 이력 체크
     * 예: 12개월간 없던 기기에서 접속
     */
    DEVICE_HISTORY("기기 이력", "과거 기기 사용 이력 체크"),
    
    /**
     * 빈도 체크 (시간 윈도우 내 횟수)
     * 예: 30분 내 3회 이상 (금액 조건 없음)
     */
    VELOCITY("빈도 체크", "일정 시간 내 발생 횟수 체크"),
    
    /**
     * 나이 조건 (나이만 체크)
     * 예: 65세 이상 (금액 조건 없음)
     */
    AGE_CONDITION("나이 조건", "사용자 나이 조건만 체크"),
    
    /**
     * 인증 후 행동 패턴
     * 예: OTP 발급 후 180분 이내 이체
     * @deprecated CERT_LIFECYCLE로 대체됨
     */
    @Deprecated
    AUTH_AFTER_ACTION("인증 후 행동", "인증 발급 후 특정 행동 감지"),
    
    /**
     * 인증 타이밍 체크 (시간만)
     * 예: OTP 발급 후 180분 이내 (금액/행동 조건 없음)
     */
    CERT_TIMING("인증 타이밍", "인증 발급 후 경과 시간만 체크"),
    
    /**
     * 위험 국가 접속
     * 예: 중국, 베트남 등 위험국가에서 접속
     */
    RISKY_COUNTRY("위험 국가", "위험 국가에서의 접속 감지"),
    
    /**
     * 비정상 국가 접속
     * 예: 주 접속 국가가 아닌 곳에서 접속
     */
    UNUSUAL_COUNTRY("비정상 국가", "평소와 다른 국가에서 접속"),
    
    /**
     * 휴면 계좌 활동
     * 예: 12개월간 거래 없던 계좌 활동
     */
    DORMANT_ACCOUNT("휴면 계좌", "장기간 미사용 계좌 활동 감지"),
    
    /**
     * 계좌 나이 체크
     * 예: 계좌 개설 후 X일 이내 (활동 조건 없음)
     */
    ACCOUNT_AGE("계좌 나이", "계좌 개설 후 경과 시간만 체크"),
    
    /**
     * 시간대 기반
     * 예: 심야 시간대(22:00-02:00) 활동
     */
    TIME_BASED("시간대 기반", "특정 시간대 활동 감지"),
    
    /**
     * 커스텀 SQL 쿼리
     * 복잡한 조건을 SQL로 직접 정의
     */
    CUSTOM_SQL("커스텀 SQL", "사용자 정의 SQL 쿼리"),
    
    /**
     * 잔액 비율
     * 예: 잔액 대비 30% 이상 금액 이체
     */
    BALANCE_RATIO("잔액 비율", "계좌 잔액 대비 거래 금액 비율 체크"),
    
    /**
     * 복합 조건
     * 예: (새 기기 AND 대액이체) OR (새벽시간 AND 해외IP)
     */
    COMPOSITE("복합 조건", "여러 룰 타입의 조합 (AND/OR)"),
    
    /**
     * 로그인 실패 감지
     * 예: 로그인 실패 5회 이상, 로그인 실패 상태 감지
     */
    LOGIN_FAILURE("로그인 실패", "로그인 실패 횟수 및 상태 감지"),
    
    /**
     * ATM 거래 관련
     * 예: ATM 출금 횟수 제한, ATM 거래 금액 제한
     */
    ATM_TRANSACTION("ATM 거래", "ATM 관련 거래 패턴 감지"),
    
    /**
     * IP 기반 보안
     * 예: 동일 IP 다중 계정 접속, 해외 IP 접속 감지
     */
    IP_SECURITY("IP 보안", "IP 주소 기반 보안 룰"),
    
    /**
     * 사기 위험도 감지
     * 예: 높은 사기 점수 감지, 사기 위험도 기반 차단
     */
    FRAUD_DETECTION("사기 감지", "사기 위험도 점수 기반 감지"),
    
    /**
     * 멀웨어/침입 감지
     * 예: 멀웨어 감지됨, 침입 시도 감지
     */
    THREAT_DETECTION("위협 감지", "멀웨어, 침입 등 보안 위협 감지"),
    
    /**
     * 위협 레벨 기반
     * 예: 높은 위협 레벨 (RED 등급)
     */
    THREAT_LEVEL("위협 등급", "시스템 위협 등급 기반 룰"),
    
    /**
     * 계좌 타입별 제한
     * 예: 비대면 개설 계좌 여부, 특정 계좌 타입 제한
     */
    ACCOUNT_TYPE("계좌 타입", "계좌 개설 방식 및 타입별 제한"),
    
    /**
     * 고객 등급/나이별 제한
     * 예: 고령자 대액 이체 제한, VIP 고객 우대
     */
    CUSTOMER_GRADE("고객 등급", "고객 나이, 등급별 차별화 룰"),
    
    /**
     * 전자상거래 특화
     * 예: 고액 상품 구매 감지, EC 거래 패턴 분석
     */
    E_COMMERCE("전자상거래", "온라인 쇼핑 및 전자상거래 특화 룰"),
    
    /**
     * 거래 타입별 모니터링
     * 예: 이체 거래 감지, 출금 거래 감지
     */
    TRANSACTION_TYPE("거래 타입", "거래 유형별 모니터링 룰");
    
    private final String displayName;
    private final String description;
    
    RuleType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 이력 데이터가 필요한 타입인지 확인
     */
    public boolean requiresHistory() {
        // 이력이 필요하지 않은 타입들 (현재 데이터만으로 판단 가능)
        return this != SIMPLE && 
               this != TIME_BASED && 
               this != BALANCE_RATIO && 
               this != THREAT_LEVEL &&
               this != ACCOUNT_TYPE &&
               this != FRAUD_DETECTION &&
               this != THREAT_DETECTION &&
               this != E_COMMERCE &&
               this != TRANSACTION_TYPE;
    }
    
    /**
     * 사용자 프로필 정보가 필요한 타입인지 확인
     */
    public boolean requiresUserProfile() {
        return this == AGE_CONDITION || 
               this == UNUSUAL_COUNTRY || 
               this == CUSTOMER_GRADE ||
               this == ACCOUNT_TYPE;
    }
}