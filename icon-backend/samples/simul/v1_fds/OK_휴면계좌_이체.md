# 시나리오: 최근 12개월간 거래 이력이 없던 계좌로 이체

## 예상 탐지
- **RULE_DORMANT_ACC_XFER**: 휴면 계좌로 이체
- **AGG_DORMANT_ACC_XFER_1X**: 휴면 계좌로 이체 1회 (60분 내)
- **SCN_DORMANT_ACC_XFER**: 휴면 계좌로 이체 시나리오

## 📝 1단계: 고객 정보 저장 (CRM 시스템)

**먼저 아래 고객 기본 정보를 저장하세요:**

**DataSource 선택**: `DS_CRM` (고객관리(CRM))

### Customer Entity (송금자 고객 속성)
### Customer Entity (수취인 고객 속성)
```json
[
  {"customer_id":"DEMO_CUS555","customer_age":45,"grade":"일반","region":"서울","join_date":"2022-01-15"}
  , {"customer_id":"DEMO_CUS777","customer_age":52,"grade":"일반","region":"부산","join_date":"2021-06-20"}
]
```

---

## 🏦 2단계: 계좌 정보 저장 (Core Banking 시스템)

**고객 정보가 저장된 후, 계좌 정보를 저장하세요:**

**DataSource 선택**: `DS_ACC_OPEN` (계좌 개설)

### Account Entity (송금자 계좌 - 정상 활동)
### Account Entity (수취인 계좌 - 12개월 이상 거래 없음) 🚨
```json
[
  {"account_number":"110-555-111111","customer_id":"DEMO_CUS555","account_open_date":"2022-01-20 10:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2025-11-20 14:30:00"}
  , {"account_number":"220-777-999999","customer_id":"DEMO_CUS777","account_open_date":"2021-07-01 09:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2024-10-15 11:20:00"}
]
```

```json
```

> **중요**: 수취인 계좌의 `last_transaction_date`가 **2024-10-15**로, 현재(2025-11-21)로부터 **13개월 이상** 경과

---

## 💳 3단계: 이체 거래 이벤트 (Transaction 시스템)

**Entity Attributes가 저장된 후, 아래 거래 데이터를 실행하세요:**

**DataSource 선택**: `0ME2YHMK6FGZN` (금융거래내역)

### 휴면 계좌로 이체 (500만원) 🚨
```json
{"CUS_ID":"DEMO_CUS555","TRX_DT":"2025-12-02 09:00:00","TRX_AMT":5000000,"TRX_TYPE":"이체","BAL_AMT":5000000,"SENDER":"110-555-111111","RECEIVER":"220-777-999999"}
```

---

## 💡 참고사항

### 🏦 시스템 분리 아키텍처

이 시나리오는 실제 은행 시스템 구조를 반영합니다:

1. **CRM 시스템 (고객관리)** → `DS_CRM`
   - 고객 기본 정보 관리 (나이, 등급, 지역, 가입일)
   - Entity Type: `CUSTOMER`
   - 저장 필드: customer_age, grade, region, join_date

2. **Core Banking 시스템 (계좌관리)** → `DS_ACC_OPEN`
   - 계좌 정보 관리 (계좌번호, 개설일, 상태, 마지막 거래일)
   - Entity Type: `ACCOUNT`
   - 저장 필드: customer_id, account_open_date, account_type, account_status, last_transaction_date

3. **Transaction 시스템 (거래내역)** → `0ME2YHMK6FGZN`
   - 실시간 거래 이벤트 처리
   - Destination: `EVENT_STREAM`

### 📊 Entity 관계

```
CUSTOMER (송금자)
  ├── customer_id: "DEMO_CUS555"
  ├── customer_age: 45
  ├── grade: "일반"
  ├── region: "서울"
  └── join_date: "2022-01-15"

CUSTOMER (수취인)
  ├── customer_id: "DEMO_CUS777"
  ├── customer_age: 52
  ├── grade: "일반"
  ├── region: "부산"
  └── join_date: "2021-06-20"

ACCOUNT (송금자 계좌)
  ├── account_number: "110-555-111111"
  ├── customer_id: "DEMO_CUS555" (외래키)
  ├── account_open_date: "2022-01-20"
  ├── account_status: "ACTIVE"
  └── last_transaction_date: "2025-11-20" ✅ 최근 거래

ACCOUNT (수취인 계좌 - 휴면)
  ├── account_number: "220-777-999999"
  ├── customer_id: "DEMO_CUS777" (외래키)
  ├── account_open_date: "2021-07-01"
  ├── account_status: "ACTIVE"
  └── last_transaction_date: "2024-10-15" 🚨 13개월 전
```

### 🎯 탐지 조건

#### 룰: RULE_DORMANT_ACC_XFER (휴면 계좌로 이체)
- **transaction_type** = "이체" (TRX_TYPE 필드)
- **수취인 계좌(RECEIVER)의 last_transaction_date**가 현재로부터 **365일(12개월) 이전**

#### 집계: AGG_DORMANT_ACC_XFER_1X (휴면 계좌로 이체 1회)
- **연산자**: COUNT_WITHIN (개수 세기)
- **임계값**: 1회
- **윈도우**: 60분

#### 시나리오: SCN_DORMANT_ACC_XFER (휴면 계좌로 이체)
- **윈도우**: 60분
- **중복제거**: 60분

### 🎯 탐지 룰 동작 원리

거래 이벤트(3단계) 발생 시:
1. `customer_id`로 송금자 CUSTOMER entity 조회 → 고객 정보 확인
2. `SENDER` 계좌번호로 송금자 ACCOUNT entity 조회
3. `RECEIVER` 계좌번호(`220-777-999999`)로 수취인 ACCOUNT entity 조회
4. 수취인 계좌의 `last_transaction_date: 2024-10-15` 확인
5. 현재 거래일(`2025-11-21`)과의 차이 계산: **13개월 (397일)**
6. 룰 평가: "이체" + "수취인 계좌 365일 이상 거래 없음" ✅ 탐지!

### 실행 순서

1. **1단계**: `DS_CRM`로 송금자/수취인 고객 정보 전송 → CUSTOMER entity 저장
2. **2단계**: `DS_ACC_OPEN`로 송금자/수취인 계좌 정보 전송 → ACCOUNT entity 저장
3. **3단계**: `0ME2YHMK6FGZN`으로 이체 거래 전송 → 실시간 탐지 실행
4. **탐지 확인**: `/detections/scenarios`에서 SCN_DORMANT_ACC_XFER 확인

### ✅ 탐지 확인

1. `/detections/aggregates` - AGG_DORMANT_ACC_XFER_1X 탐지 확인
2. `/detections/scenarios` - SCN_DORMANT_ACC_XFER 시나리오 탐지 확인

### ⚠️ 주의사항

**🚨 중요: 반드시 순서대로 실행하세요!**

1. **Step 1 (고객 정보)** → Step 2 전에 먼저 실행
2. **Step 2 (계좌 정보)** → Step 3 전에 **반드시** 실행해야 함
   - 수취인 계좌의 `last_transaction_date`가 entity_attributes에 저장되어야 함
   - Step 2를 건너뛰면 IS_DORMANT_ACCOUNT = false로 계산되어 탐지 실패
3. **Step 3 (거래 이벤트)** → Step 2 완료 후 실행

**🔧 프로필 설정 확인사항:**
- `P_ACC_OPEN` 프로필의 `store_fields`에 `last_transaction_date`가 **반드시** 포함되어 있어야 함
- 포함되지 않으면 Step 2 데이터를 보내도 entity_attributes에 저장되지 않음
- 확인 방법:
  ```sql
  SELECT store_fields FROM profiles WHERE profile_id = 'P_ACC_OPEN';
  -- 결과: ["customer_id", "account_open_date", "account_type", "initial_balance", "last_transaction_date", "account_status"]
  ```

**🔧 룰 설정 확인사항:**
- `RULE_DORMANT_ACC_XFER`의 필드명이 **반드시** `transaction_type`이어야 함 (`TRX_TYPE` 아님!)
- 프로필 매핑 후 필드명과 룰의 where_json 필드명이 일치해야 탐지됨
- 확인 방법:
  ```sql
  SELECT where_json FROM rules WHERE rule_id = 'RULE_DORMANT_ACC_XFER';
  -- 결과: [{"value": "이체", "operator": "EQUALS", "fieldName": "transaction_type"}, ...]
  -- ⚠️ "TRX_TYPE"이 아니라 "transaction_type"이어야 함!
  ```

**추가 확인사항:**
- `RECEIVER` 필드의 계좌번호로 ACCOUNT entity를 조회합니다
- 수취인 계좌의 `last_transaction_date`가 **365일(12개월) 이내**면 탐지되지 않습니다
- 중복 탐지 방지를 위해 60분 중복제거(dedup) 적용됩니다
- 계좌 상태(`account_status`)가 "ACTIVE"여야 정상적인 이체가 가능합니다

### ⏱️ 휴면 기준

- ✅ **365일(12개월) 초과**: 탐지됨 (예: 2024-10-15 → 2025-11-21 = 397일)
- ❌ **365일 이내**: 탐지 안 됨 (예: 2025-01-01 → 2025-11-21 = 324일)

### 📝 주요 필드

- `customer_id`: 고객 ID (그룹키로 사용)
- `SENDER`: 송금 계좌번호
- `RECEIVER`: 수취 계좌번호 (휴면 여부 체크 대상)
- `TRX_TYPE`: 거래 유형 ("이체")
- `last_transaction_date`: 계좌의 마지막 거래일 (Entity Attribute)

### 🎯 실제 업무 시나리오

이 탐지 규칙은 다음과 같은 의심 거래를 포착합니다:

1. **보이스피싱 대포통장**: 장기 미사용 계좌를 매입하여 피해금 수취
2. **자금세탁**: 유령 계좌로 불법 자금 이체
3. **명의도용**: 타인 명의 계좌로 무단 이체
4. **사기 범죄**: 휴면 계좌를 활용한 사기 거래

### 💡 추가 개선 아이디어

- 금액 임계값 추가 (예: 100만원 이상만 탐지)
- 송금자-수취인 관계 분석 (이전 거래 이력 확인)
- 지역 분석 (송금자와 수취인 지역이 다른 경우)
- 시간대 분석 (심야 시간대 이체 시 추가 가중치)
