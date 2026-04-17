# 시나리오: 60분 이내 타인 명의 계좌로 3회 이상 이체

## 예상 탐지
- **RULE_FIN_TO_THIRD_PARTY**: 타인 명의 이체
- **AGG_60M_3TX_THIRD_PARTY**: 60분 타인명의 3회 (threshold=3, window=60분)
- **S_CUS014**: 60분 내 타인 명의로 3회 이체 시나리오

## 📝 1단계: 고객 정보 저장 (CRM 시스템)

**먼저 아래 고객 기본 정보를 저장하세요:**

**DataSource 선택**: `DS_CRM` (고객관리(CRM))

### Customer Entity
1. 송금자 고객 속성
2. 수취인 1 - 타인 명의
3. 수취인 2 - 타인 명의
4. 수취인 3 - 타인 명의

```json
[
{"customer_id":"DEMO_CUS100","customer_age":35,"grade":"우수","region":"서울","join_date":"2020-03-15"}
, {"customer_id":"DEMO_CUS200","customer_age":42,"grade":"일반","region":"경기","join_date":"2019-07-10"}
, {"customer_id":"DEMO_CUS300","customer_age":28,"grade":"일반","region":"인천","join_date":"2021-11-05"}
, {"customer_id":"DEMO_CUS400","customer_age":51,"grade":"일반","region":"부산","join_date":"2018-02-20"} 
]
```

---

## 🏦 2단계: 계좌 정보 저장 (Core Banking 시스템)

**고객 정보가 저장된 후, 계좌 정보를 저장하세요:**

**DataSource 선택**: `DS_ACC_OPEN` (계좌 개설)

### Account Entity
1. 송금자 계좌
2. 수취인 1 계좌 - 타인 명의
3. 수취인 2 계좌 - 타인 명의
4. 수취인 3 계좌 - 타인 명의

```json
[
  {"account_number":"110-100-111111","customer_id":"DEMO_CUS100","account_open_date":"2020-03-20 10:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2025-12-10 08:00:00"}
  , {"account_number":"220-200-222222","customer_id":"DEMO_CUS200","account_open_date":"2019-07-15 09:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2025-12-09 15:30:00"}
  , {"account_number":"330-300-333333","customer_id":"DEMO_CUS300","account_open_date":"2021-11-10 11:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2025-12-08 12:45:00"}
  , {"account_number":"440-400-444444","customer_id":"DEMO_CUS400","account_open_date":"2018-02-25 14:00:00","account_type":"보통예금","account_status":"ACTIVE","last_transaction_date":"2025-12-07 10:20:00"}
]
```


---

## 💳 3단계: 이체 거래 이벤트 (Transaction 시스템)

**Entity Attributes가 저장된 후, 아래 거래 데이터를 실행하세요:**

**DataSource 선택**: `0ME2YHMK6FGZN` (금융거래내역)

1. 1차 타인 명의 계좌로 이체 (200만원) 🚨
2. 2차 타인 명의 계좌로 이체 (150만원) 🚨
3. 3차 타인 명의 계좌로 이체 (300만원) 🚨

```json
[
  {"CUS_ID":"DEMO_CUS100","TRX_DT":"2025-12-10 09:00:00","TRX_AMT":2000000,"TRX_TYPE":"이체","BAL_AMT":8000000,"SENDER":"110-100-111111","RECEIVER":"220-200-222222"}
  , {"CUS_ID":"DEMO_CUS100","TRX_DT":"2025-12-10 09:20:00","TRX_AMT":1500000,"TRX_TYPE":"이체","BAL_AMT":6500000,"SENDER":"110-100-111111","RECEIVER":"330-300-333333"}
  , {"CUS_ID":"DEMO_CUS100","TRX_DT":"2025-12-10 09:40:00","TRX_AMT":3000000,"TRX_TYPE":"이체","BAL_AMT":3500000,"SENDER":"110-100-111111","RECEIVER":"440-400-444444"}
]
```
> **중요**: 3건의 이체가 모두 **60분 이내**(09:00 → 09:20 → 09:40)에 발생하며, 수취인이 **모두 다른 고객**임

---

## 💡 참고사항

### 🏦 시스템 분리 아키텍처

이 시나리오는 실제 은행 시스템 구조를 반영합니다:

1. **CRM 시스템 (고객관리)** → `DS_CRM`
   - 고객 기본 정보 관리 (나이, 등급, 지역, 가입일)
   - Entity Type: `CUSTOMER`
   - 저장 필드: customer_age, grade, region, join_date

2. **Core Banking 시스템 (계좌관리)** → `DS_ACC_OPEN`
   - 계좌 정보 관리 (계좌번호, 개설일, 상태, 소유자)
   - Entity Type: `ACCOUNT`
   - 저장 필드: customer_id, account_open_date, account_type, account_status, last_transaction_date

3. **Transaction 시스템 (거래내역)** → `0ME2YHMK6FGZN`
   - 실시간 거래 이벤트 처리
   - Destination: `EVENT_STREAM`

### 📊 Entity 관계

```
CUSTOMER (송금자)
  ├── customer_id: "DEMO_CUS100"
  ├── customer_age: 35
  ├── grade: "우수"
  ├── region: "서울"
  └── join_date: "2020-03-15"

CUSTOMER (수취인 1)
  ├── customer_id: "DEMO_CUS200"
  ├── customer_age: 42
  ├── grade: "일반"
  ├── region: "경기"
  └── join_date: "2019-07-10"

CUSTOMER (수취인 2)
  ├── customer_id: "DEMO_CUS300"
  ├── customer_age: 28
  ├── grade: "일반"
  ├── region: "인천"
  └── join_date: "2021-11-05"

CUSTOMER (수취인 3)
  ├── customer_id: "DEMO_CUS400"
  ├── customer_age: 51
  ├── grade: "일반"
  ├── region: "부산"
  └── join_date: "2018-02-20"

ACCOUNT (송금자 계좌)
  ├── account_number: "110-100-111111"
  ├── customer_id: "DEMO_CUS100" (외래키)
  ├── account_open_date: "2020-03-20"
  ├── account_status: "ACTIVE"
  └── last_transaction_date: "2025-12-10" ✅ 최근 거래

ACCOUNT (수취인 1 계좌 - 타인 명의)
  ├── account_number: "220-200-222222"
  ├── customer_id: "DEMO_CUS200" (외래키) 🚨 송금자와 다름
  ├── account_open_date: "2019-07-15"
  ├── account_status: "ACTIVE"
  └── last_transaction_date: "2025-12-09"

ACCOUNT (수취인 2 계좌 - 타인 명의)
  ├── account_number: "330-300-333333"
  ├── customer_id: "DEMO_CUS300" (외래키) 🚨 송금자와 다름
  ├── account_open_date: "2021-11-10"
  ├── account_status: "ACTIVE"
  └── last_transaction_date: "2025-12-08"

ACCOUNT (수취인 3 계좌 - 타인 명의)
  ├── account_number: "440-400-444444"
  ├── customer_id: "DEMO_CUS400" (외래키) 🚨 송금자와 다름
  ├── account_open_date: "2018-02-25"
  ├── account_status: "ACTIVE"
  └── last_transaction_date: "2025-12-07"
```

### 🎯 탐지 조건

#### 룰: RULE_FIN_TO_THIRD_PARTY (타인 명의 이체)
- **transaction_type** = "이체" (TRX_TYPE 필드)
- **송금자(SENDER)의 customer_id** ≠ **수취인(RECEIVER)의 customer_id**
- 송금자 계좌와 수취인 계좌의 소유자가 다름

#### 집계: AGG_60M_3TX_THIRD_PARTY (60분 타인명의 3회)
- **연산자**: COUNT_WITHIN (개수 세기)
- **임계값**: 3회
- **윈도우**: 60분

#### 시나리오: S_CUS014 (60분 내 타인 명의로 3회 이체)
- **윈도우**: 60분
- **중복제거**: 60분
- **사용 Aggregate**: AGG_60M_3TX_THIRD_PARTY

### 🎯 탐지 룰 동작 원리

거래 이벤트(3단계) 발생 시:

**1차 이체 (09:00:00)**
1. `customer_id: DEMO_CUS100`로 송금자 CUSTOMER entity 조회
2. `SENDER: 110-100-111111`로 송금자 ACCOUNT entity 조회 → customer_id: DEMO_CUS100
3. `RECEIVER: 220-200-222222`로 수취인 ACCOUNT entity 조회 → customer_id: DEMO_CUS200
4. 송금자 customer_id(DEMO_CUS100) ≠ 수취인 customer_id(DEMO_CUS200) ✅ 탐지!
5. 집계: 1회 카운트

**2차 이체 (09:20:00)**
1. `customer_id: DEMO_CUS100`로 송금자 CUSTOMER entity 조회
2. `SENDER: 110-100-111111`로 송금자 ACCOUNT entity 조회 → customer_id: DEMO_CUS100
3. `RECEIVER: 330-300-333333`로 수취인 ACCOUNT entity 조회 → customer_id: DEMO_CUS300
4. 송금자 customer_id(DEMO_CUS100) ≠ 수취인 customer_id(DEMO_CUS300) ✅ 탐지!
5. 집계: 2회 카운트 (60분 이내)

**3차 이체 (09:40:00)**
1. `customer_id: DEMO_CUS100`로 송금자 CUSTOMER entity 조회
2. `SENDER: 110-100-111111`로 송금자 ACCOUNT entity 조회 → customer_id: DEMO_CUS100
3. `RECEIVER: 440-400-444444`로 수취인 ACCOUNT entity 조회 → customer_id: DEMO_CUS400
4. 송금자 customer_id(DEMO_CUS100) ≠ 수취인 customer_id(DEMO_CUS400) ✅ 탐지!
5. 집계: 3회 카운트 (60분 이내) 🚨 **임계값 도달!**
6. 시나리오 탐지: S_CUS014 생성

### 실행 순서

1. **1단계**: `DS_CRM`로 송금자/수취인(3명) 고객 정보 전송 → CUSTOMER entity 저장
2. **2단계**: `DS_ACC_OPEN`로 송금자/수취인(3개) 계좌 정보 전송 → ACCOUNT entity 저장
3. **3단계**: `0ME2YHMK6FGZN`으로 이체 거래 3건 전송 → 실시간 탐지 실행
4. **탐지 확인**: `/detections/scenarios`에서 SCN_OTHER_NAME_XFER 확인

### ✅ 탐지 확인

1. `/detections/rules` - RULE_FIN_TO_THIRD_PARTY 룰 매칭 3건 확인
2. `/detections/aggregates` - AGG_60M_3TX_THIRD_PARTY 집계 탐지 확인
3. `/detections/scenarios` - S_CUS014 시나리오 탐지 확인

### ⚠️ 주의사항

**🚨 중요: 반드시 순서대로 실행하세요!**

1. **Step 1 (고객 정보)** → Step 2 전에 먼저 실행
   - 송금자 고객 1명 + 수취인 고객 3명 = 총 4명 등록
2. **Step 2 (계좌 정보)** → Step 3 전에 **반드시** 실행해야 함
   - 송금자 계좌 1개 + 수취인 계좌 3개 = 총 4개 등록
   - 각 계좌의 `customer_id`가 entity_attributes에 저장되어야 함
   - Step 2를 건너뛰면 타인 명의 여부를 판별할 수 없어 탐지 실패
3. **Step 3 (거래 이벤트)** → Step 2 완료 후 60분 이내에 3건 실행
   - 3건의 이체를 60분 이내에 순차적으로 전송

**🔧 프로필 설정 확인사항:**
- `P_ACC_OPEN` 프로필의 `store_fields`에 `customer_id`가 **반드시** 포함되어 있어야 함
- 포함되지 않으면 Step 2 데이터를 보내도 entity_attributes에 저장되지 않음
- 확인 방법:
  ```sql
  SELECT store_fields FROM profiles WHERE profile_id = 'P_ACC_OPEN';
  -- 결과: ["customer_id", "account_open_date", "account_type", "initial_balance", "last_transaction_date", "account_status"]
  ```

**🔧 룰 설정 확인사항:**
- `RULE_FIN_TO_THIRD_PARTY`의 필드명이 **반드시** `transaction_type`이어야 함 (`TRX_TYPE` 아님!)
- 프로필 매핑 후 필드명과 룰의 where_json 필드명이 일치해야 탐지됨
- 확인 방법:
  ```sql
  SELECT where_json FROM rules WHERE rule_id = 'RULE_OTHER_NAME_XFER';
  -- 결과: [{"value": "이체", "operator": "EQUALS", "fieldName": "transaction_type"}]
  -- ⚠️ "TRX_TYPE"이 아니라 "transaction_type"이어야 함!
  ```

**추가 확인사항:**
- `RECEIVER` 필드의 계좌번호로 ACCOUNT entity를 조회합니다
- 수취인 계좌의 `customer_id`와 송금자의 `customer_id`를 비교합니다
- 두 값이 **다르면** 타인 명의 이체로 간주되어 탐지됩니다
- 중복 탐지 방지를 위해 60분 중복제거(dedup) 적용됩니다
- 계좌 상태(`account_status`)가 "ACTIVE"여야 정상적인 이체가 가능합니다

### ⏱️ 시간 기준

- ✅ **60분 이내 3회**: 탐지됨 (예: 09:00 → 09:20 → 09:40)
- ❌ **60분 초과**: 탐지 안 됨 (예: 09:00 → 10:30 → 11:00)
- ❌ **3회 미만**: 탐지 안 됨 (예: 60분 이내 2회만 발생)

### 📝 주요 필드

- `customer_id`: 고객 ID (그룹키로 사용)
- `SENDER`: 송금 계좌번호 (송금자 소유)
- `RECEIVER`: 수취 계좌번호 (수취인 소유)
- `TRX_TYPE`: 거래 유형 ("이체")
- `TRX_AMOUNT`: 이체 금액
- `TRX_DT`: 거래 시각 (60분 윈도우 계산 기준)

### 🎯 실제 업무 시나리오

이 탐지 규칙은 다음과 같은 의심 거래를 포착합니다:

1. **보이스피싱**: 피해자 계좌에서 여러 대포통장으로 분산 이체
2. **자금세탁**: 추적을 피하기 위해 다수의 타인 계좌로 자금 분산
3. **불법 대출**: 다수의 차명 계좌로 대출금 분산 인출
4. **횡령**: 회사 자금을 여러 개인 계좌로 빼돌리기
5. **사기 범죄**: 피해금을 여러 공범 계좌로 분산 송금

### 💡 추가 개선 아이디어

- 금액 임계값 추가 (예: 건당 100만원 이상, 누적 500만원 이상만 탐지)
- 시간대 분석 (심야/새벽 시간대 이체 시 가중치 부여)
- 송금자-수취인 관계 분석 (이전 거래 이력 없는 경우 가중치 부여)
- 수취인 계좌 개설일 분석 (최근 개설 계좌일 경우 가중치 부여)
- 지역 분석 (송금자와 수취인들의 지역이 모두 다른 경우 가중치 부여)
- 채널 분석 (다양한 채널 사용 시 가중치 부여)
- 거래 패턴 분석 (짧은 시간 간격으로 연속 이체 시 가중치 부여)
