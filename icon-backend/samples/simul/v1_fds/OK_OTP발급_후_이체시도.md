# 시나리오: OTP 발급 후 180분 이내 이체 시도

## 예상 탐지
- **SCN_AUTH_XFER**: OTP 발급 후 이체 시나리오

## 📝 1단계: 고객 정보 저장 (CRM 시스템)

**먼저 아래 고객 기본 정보를 저장하세요:**

**DataSource 선택**: `DS_CRM` (고객관리(CRM))

### Customer Entity (고객 속성)
```json
{"customer_id":"DEMO_CUS888","customer_age":42,"grade":"일반","region":"서울","join_date":"2023-06-15"}
```

---

## 🔐 2단계: OTP 발급 정보 저장 (인증 시스템)

**고객 정보가 저장된 후, OTP 발급 정보를 저장하세요:**

**DataSource 선택**: `DS_AUTH` (인증수단 발급)

### Authentication Entity (인증 속성)
```json
{"auth_id":"AUTH_888_20251204_100000","customer_id":"DEMO_CUS888","auth_type":"OTP","auth_issued_at":"2025-12-04 10:00:00","auth_method":"SMS","auth_status":"SUCCESS"}
```

---

## 💳 3단계: 이체 거래 이벤트 (180분 이내)

**인증 정보가 저장된 후, 180분 이내에 이체 이벤트를 전송하세요:**

**DataSource 선택**: `0ME2YHMK6FGZN` (금융거래내역)

### 이체 거래 (120분 후) 🚨
```json
{"CUS_ID":"DEMO_CUS888","TRX_DT":"2025-12-04 12:00:00","TRX_AMT":3000000,"TRX_TYPE":"이체","BAL_AMT":2000000,"SENDER":"110-987-654321","RECEIVER":"990-111-222333"}
```
