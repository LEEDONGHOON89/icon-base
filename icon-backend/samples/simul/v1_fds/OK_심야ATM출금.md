# 시나리오: 심야 ATM 출금

## 예상 탐지
- **AGG_ATM_NIGHT_1X**: 심야 ATM 1회 (60분 내 1회)
- **S_R005**: 심야 ATM 1회 시나리오

## 📝 1단계: 고객 정보 저장 (CRM 시스템)

**먼저 아래 고객 기본 정보를 저장하세요:**

**DataSource 선택**: `DS_CRM` (고객관리(CRM))

### Customer Entity (고객 속성)
```json
{"customer_id":"DEMO_CUS999","customer_age":35,"grade":"일반","region":"서울","join_date":"2024-05-10"}
```

---

## 📝 2단계: 계좌 개설 정보 저장 (Core Banking 시스템)

**고객 정보가 저장된 후, 계좌 개설 정보를 저장하세요:**

**DataSource 선택**: `DS_ACC_OPEN` (계좌 개설)

### Account Entity (계좌 속성)
```json
{"account_number":"110-999-888777","customer_id":"DEMO_CUS999","account_open_date":"2024-06-01 10:00:00","account_type":"대면계좌","transaction_type":"대면계좌개설","initial_balance":10000000}
```

---

## 🚀 3단계: 거래 이벤트 (Transaction 시스템)

**Entity Attributes가 저장된 후, 아래 거래 데이터를 실행하세요:**

**DataSource 선택**: `0ME2YHMK6FGZN` (금융거래내역)

### 심야 시간 ATM 출금 (100만원) 🚨
```json
{"CUS_ID":"DEMO_CUS999","TRX_DT":"2025-12-04 23:30:00","TRX_AMT":1000000,"TRX_TYPE":"출금","BAL_AMT":9000000,"ATM_WD_CNT":1}
```
