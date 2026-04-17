// 필드 옵션 인터페이스
export interface FieldOption {
  name: string;
  label: string;
  type: "string" | "number" | "boolean" | "date";
  valueOptions?: Array<{ value: string; label: string }>; // 선택 가능한 값 옵션
}

// 사용 가능한 필드 목록
export const AVAILABLE_FIELDS: FieldOption[] = [
  // 거래 관련
  { name: "transaction_amount", label: "거래금액", type: "number" as const },
  { name: "transaction_count", label: "거래횟수", type: "number" as const },
  { name: "transaction_date", label: "거래일자", type: "date" as const },
  { name: "balance_ratio", label: "잔액대비비율(%)", type: "number" as const },
  { name: "account_balance", label: "계좌잔액", type: "number" as const },
  { 
    name: "transaction_type", 
    label: "거래유형", 
    type: "string" as const,
    valueOptions: [
      { value: "withdrawal", label: "출금" },
      { value: "transfer", label: "이체" },
      { value: "deposit", label: "입금" },
      { value: "loan", label: "대출" },
      { value: "card_payment", label: "카드결제" },
      { value: "atm_withdrawal", label: "ATM출금" }
    ]
  },
  
  // 접속/로그인 관련
  { 
    name: "access_country", 
    label: "접속국가", 
    type: "string" as const,
    valueOptions: [
      { value: "KR", label: "대한민국" },
      { value: "US", label: "미국" },
      { value: "CN", label: "중국" },
      { value: "JP", label: "일본" },
      { value: "VN", label: "베트남" },
      { value: "TH", label: "태국" },
      { value: "PH", label: "필리핀" },
      { value: "OTHER", label: "기타" }
    ]
  },
  { name: "country_change_hours", label: "국가변경시간(시)", type: "number" as const },
  { name: "login_fail_count", label: "로그인실패횟수", type: "number" as const },
  { name: "login_fail_days", label: "로그인실패기간(일)", type: "number" as const },
  { name: "login_to_action_seconds", label: "로그인후행동시간(초)", type: "number" as const },
  { name: "last_access_months", label: "마지막접속개월수", type: "number" as const },
  
  // 디바이스 관련
  { 
    name: "device_type", 
    label: "디바이스타입", 
    type: "string" as const,
    valueOptions: [
      { value: "mobile", label: "모바일" },
      { value: "pc", label: "PC" },
      { value: "tablet", label: "태블릿" },
      { value: "atm", label: "ATM" },
      { value: "unknown", label: "알수없음" }
    ]
  },
  { name: "device_change_hours", label: "디바이스변경시간(시)", type: "number" as const },
  { name: "device_count", label: "디바이스수", type: "number" as const },
  { 
    name: "device_os", 
    label: "운영체제", 
    type: "string" as const,
    valueOptions: [
      { value: "android", label: "Android" },
      { value: "ios", label: "iOS" },
      { value: "windows", label: "Windows" },
      { value: "macos", label: "macOS" },
      { value: "linux", label: "Linux" },
      { value: "other", label: "기타" }
    ]
  },
  { name: "has_device_id", label: "디바이스ID존재", type: "boolean" as const },
  
  // 인증서/OTP 관련
  { name: "cert_issue_days", label: "인증서발급후일수", type: "number" as const },
  { name: "cert_issue_minutes", label: "인증서발급후분", type: "number" as const },
  { name: "cert_issue_count", label: "인증서발급횟수", type: "number" as const },
  { name: "cert_issue_months", label: "인증서발급기간(월)", type: "number" as const },
  
  // 대출 관련
  { name: "loan_amount", label: "대출금액", type: "number" as const },
  { name: "loan_to_transfer_hours", label: "대출후이체시간(시)", type: "number" as const },
  { name: "loan_history_months", label: "대출이력개월", type: "number" as const },
  
  // 계좌 관련
  { name: "account_number", label: "계좌번호", type: "string" as const },
  { name: "account_open_days", label: "계좌개설후일수", type: "number" as const },
  { name: "account_inactive_months", label: "계좌미사용개월", type: "number" as const },
  { 
    name: "account_type", 
    label: "계좌유형", 
    type: "string" as const,
    valueOptions: [
      { value: "checking", label: "입출금계좌" },
      { value: "savings", label: "예금계좌" },
      { value: "loan", label: "대출계좌" },
      { value: "corporate", label: "법인계좌" },
      { value: "foreign", label: "외화계좌" }
    ]
  },
  
  // 오픈뱅킹 관련
  { name: "openbank_register_minutes", label: "오픈뱅킹등록후분", type: "number" as const },
  { name: "openbank_inactive_months", label: "오픈뱅킹미사용개월", type: "number" as const },
  { name: "openbank_amount_sum", label: "오픈뱅킹합계금액", type: "number" as const },
  { name: "openbank_sum_hours", label: "오픈뱅킹합계시간(시)", type: "number" as const },
  
  // ATM 관련
  { name: "atm_withdraw_amount", label: "ATM출금금액", type: "number" as const },
  { name: "atm_withdraw_count", label: "ATM출금횟수", type: "number" as const },
  { name: "atm_withdraw_minutes", label: "ATM출금시간(분)", type: "number" as const },
  { name: "atm_inactive_months", label: "ATM미사용개월", type: "number" as const },
  { name: "atm_night_hours", label: "ATM야간시간대", type: "string" as const },
  
  // 고객 정보
  { name: "customer_name", label: "고객명", type: "string" as const },
  { name: "customer_age", label: "고객나이", type: "number" as const },
  { 
    name: "customer_info_change", 
    label: "고객정보변경항목", 
    type: "string" as const,
    valueOptions: [
      { value: "password", label: "패스워드" },
      { value: "phone", label: "전화번호" },
      { value: "email", label: "이메일" },
      { value: "address", label: "주소" },
      { value: "account_limit", label: "이체한도" },
      { value: "otp_device", label: "OTP기기" },
      { value: "cert_reissue", label: "인증서재발급" }
    ]
  },
  
  // 블랙/그레이리스트
  { 
    name: "blacklist_type", 
    label: "블랙리스트유형", 
    type: "string" as const,
    valueOptions: [
      { value: "id", label: "ID" },
      { value: "ip", label: "IP주소" },
      { value: "mac", label: "MAC주소" },
      { value: "phone", label: "전화번호" },
      { value: "account", label: "계좌번호" },
      { value: "device", label: "디바이스" }
    ]
  },
  { 
    name: "graylist_type", 
    label: "그레이리스트유형", 
    type: "string" as const,
    valueOptions: [
      { value: "suspicious_ip", label: "의심IP" },
      { value: "high_risk_device", label: "고위험디바이스" },
      { value: "fraud_history", label: "사기이력" },
      { value: "abnormal_pattern", label: "비정상패턴" }
    ]
  },
  
  // 기타
  { name: "ip_address", label: "IP주소", type: "string" as const },
  { name: "time_window_minutes", label: "시간범위(분)", type: "number" as const },
  { name: "time_window_hours", label: "시간범위(시)", type: "number" as const },
  { name: "time_window_days", label: "시간범위(일)", type: "number" as const },
  { 
    name: "subsequent_action", 
    label: "후속행동", 
    type: "string" as const,
    valueOptions: [
      { value: "withdrawal", label: "출금" },
      { value: "transfer", label: "이체" },
      { value: "loan_apply", label: "대출신청" },
      { value: "limit_change", label: "한도변경" },
      { value: "cert_reissue", label: "인증서재발급" },
      { value: "otp_register", label: "OTP등록" }
    ]
  },
];

// 연산자 옵션 (백엔드에서 문자열로 처리하므로 단순화)
export const OPERATOR_OPTIONS = [
  // 기본 비교 연산자
  { value: ">", label: "보다 큰" },
  { value: "<", label: "보다 작은" },
  { value: ">=", label: "이상" },
  { value: "<=", label: "이하" },
  { value: "=", label: "같음" },
  { value: "!=", label: "다름" },
  
  // 포함 관련
  { value: "IN", label: "포함(목록)" },
  { value: "NOT_IN", label: "포함하지 않음(목록)" },
  { value: "contains", label: "포함(텍스트)" },
  { value: "not_contains", label: "포함하지 않음(텍스트)" },
  { value: "BETWEEN", label: "사이" },
  
  // 시간/패턴 관련
  { value: "WITHIN", label: "이내" },
  { value: "AFTER", label: "이후" },
  { value: "BEFORE", label: "이전" },
  { value: "EXISTS", label: "존재함" },
  { value: "NOT_EXISTS", label: "존재하지 않음" },
  
  // 특수 연산자
  { value: "CHANGED", label: "변경됨" },
  { value: "NOT_PRIMARY", label: "주 사용이 아님" },
  { value: "INACTIVE", label: "비활성" },
  { value: "BLACKLISTED", label: "블랙리스트" },
  { value: "GRAYLISTED", label: "그레이리스트" },
];