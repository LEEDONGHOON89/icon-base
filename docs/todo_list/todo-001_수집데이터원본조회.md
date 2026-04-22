# TODO-001: 수집 데이터 원본 조회 화면 추가

## 메타

| 항목 | 내용 |
|---|---|
| 상태 | 🔲 미착수 |
| 분류 | 프론트엔드 + 백엔드 API |
| 작성일 | 2026-04-21 |
| 수정일 | 2026-04-22 — cust_no 필터 및 확장형 JSONB 필터 구조 추가 |
| 우선순위 | 중 |

---

## 목적

PREP-1 단계에서 수집된 원본 레코드(`landing_records`)를 데이터소스 상세 화면에서 직접 조회할 수 있도록 한다.  
현재는 수집이 제대로 됐는지 확인하려면 DB 직접 쿼리가 필요한 상황으로, 운영/디버깅 편의성 향상이 목적이다.

---

## 위치

- **데이터소스 상세 페이지** `/data-sources/[id]` 에 새 탭으로 추가
- 탭명: **"수집 원본"**
- 기존 탭 순서: `개요 | 연결설정 | 원본필드(스키마) | 프로파일관리 | 파서 설정 | 수집 원본` ← 추가

---

## 화면 구성

### 필터 영역

> **설계 원칙**: 기본 필터(기간·상태)는 고정 UI로, JSONB 내부 필드 검색은 확장형 동적 필터 행으로 처리.  
> `cust_no`는 `raw_payload` JSONB 안의 키이므로 동적 필터로 검색한다.

#### 기본 필터 (항상 표시)

| 필터 | 타입 | DB 컬럼 | 기본값 |
|---|---|---|---|
| 조회 기간 (From) | 날짜+시간 선택 | `extracted_at >=` | 최근 24시간 |
| 조회 기간 (To) | 날짜+시간 선택 | `extracted_at <=` | 현재 |
| 수집 상태 | 드롭다운 멀티선택 | `ingestion_status` | ALL |

#### 동적 필드 필터 (확장형, + 버튼으로 행 추가)

`raw_payload` JSONB 내부 키를 자유롭게 검색할 수 있는 확장 가능한 필터 행.

| 구성 요소 | 설명 |
|---|---|
| 필드명 입력 | `raw_payload` 내 키 이름 (예: `cust_no`, `account_id`, ...) |
| 연산자 선택 | `=` (완전 일치) / `LIKE` (부분 일치) |
| 값 입력 | 검색할 값 |
| 행 삭제 버튼 | 해당 필터 행 제거 |

**기본 제공 필드 힌트**: `cust_no`, `account_id`, `transaction_id` (자동완성 후보, 추후 확장)

```
[ 기간: 2026-04-21 00:00 ~ 2026-04-22 00:00 ]  [ 상태: ALL ▼ ]
┌─────────────────────────────────────────────────────────────┐
│ 필드 필터 (+)                                                │
│  [cust_no      ▼] [= ▼] [12345678    ] [✕]                 │
│  [account_id   ▼] [= ▼] [ACC001      ] [✕]                 │
└─────────────────────────────────────────────────────────────┘
[ 조회 ]
```

---

### 통계 요약 카드 (필터 결과 기준)

| 카드 | 내용 |
|---|---|
| 전체 건수 | 조회된 레코드 수 |
| 성공 건수 | ingestion_status = TRANSFORMED |
| 실패 건수 | ingestion_status = FAILED |
| 최근 수집 시각 | 가장 최신 `extracted_at` |

---

### 목록 테이블

| 컬럼 | DB 컬럼 | 비고 |
|---|---|---|
| # | `landing_record_id` | 정렬 기준 |
| 수집 시각 | `extracted_at` | `YYYY-MM-DD HH:mm:ss` 형식 |
| 소스 유형 | `source_type` | FILE / JDBC 배지 |
| 수집 상태 | `ingestion_status` | NEW(회색) / TRANSFORMED(초록) / FAILED(빨강) 배지 |
| cust_no | `raw_payload->>'cust_no'` | 해당 필드가 있을 경우 표시, 없으면 `-` |
| 원본 데이터 | `raw_payload` | JSON 한 줄 미리보기 (100자 truncate) |
| 상세 | - | raw_payload 전체 모달 |

> **컬럼 확장 고려**: 동적 필터에 입력된 필드명은 테이블 컬럼으로도 추가 표시하는 것을 고려.  
> 예: `cust_no` 필터 활성 시 테이블에 `cust_no` 컬럼 자동 추가.

---

### 상세 모달

- `raw_payload` JSON을 포맷팅하여 전체 표시
- 복사 버튼 (`raw_payload` 클립보드 복사)
- 메타 정보: `landing_record_id`, `exec_ds_mp_id`, `row_index`, `ingestion_status`, `extracted_at`
- `ingestion_status = FAILED` 인 경우 `ingestion_message` 오류 메시지 표시

---

### 페이지네이션

- 페이지당 20건
- 총 건수 표시

---

## 백엔드 API

### 신규 API

```
GET /api/v1/data-sources/{dataSourceId}/landing-records
```

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `startDate` | ISO datetime | 24시간 전 | `extracted_at >=` |
| `endDate` | ISO datetime | 현재 | `extracted_at <=` |
| `ingestionStatus` | string (반복 가능) | - | NEW / TRANSFORMED / FAILED |
| `custNo` | string | - | `raw_payload->>'cust_no' = ?` JSONB 검색 |
| `jsonFilters` | `key:op:value` 반복 | - | 확장형 JSONB 필터 (아래 참조) |
| `page` | int | 0 | 페이지 번호 |
| `size` | int | 20 | 페이지 크기 (최대 100) |

**`jsonFilters` 파라미터 설계 (확장형)**

동적 JSONB 필터. 여러 개 중첩 가능.

```
GET /api/v1/data-sources/DS001/landing-records
    ?jsonFilters=cust_no:eq:12345678
    &jsonFilters=account_id:like:ACC%
```

| 연산자 코드 | SQL 변환 | 설명 |
|---|---|---|
| `eq` | `raw_payload->>'key' = 'value'` | 완전 일치 |
| `like` | `raw_payload->>'key' ILIKE '%value%'` | 부분 일치 (대소문자 무시) |
| `neq` | `raw_payload->>'key' != 'value'` | 불일치 |

> **보안 주의**: `key` 값은 허용 목록(allow-list) 또는 정규식 검증 필수 (`^[a-zA-Z0-9_]+$`).

**Response Body**

```json
{
  "data": [
    {
      "landingRecordId": 12345,
      "execDsMpId": 678,
      "dataSourceId": "DS001",
      "sourceType": "FILE",
      "rowIndex": 0,
      "rawPayload": { "cust_no": "12345678", "amount": 50000 },
      "ingestionStatus": "TRANSFORMED",
      "ingestionMessage": null,
      "extractedAt": "2026-04-21T10:00:00"
    }
  ],
  "total": 1234,
  "page": 0,
  "size": 20,
  "appliedFilters": {
    "custNo": "12345678",
    "jsonFilters": ["cust_no:eq:12345678"]
  }
}
```

> `appliedFilters` 필드: 프론트엔드가 현재 적용된 필터를 화면에 표시하는 데 사용.

---

## 구현 파일 목록

### 백엔드

| 파일 | 변경 유형 | 설명 |
|---|---|---|
| `icon-api/.../landingrecord/adapter/in/web/LandingRecordController.java` | 신규 | GET API 컨트롤러 |
| `icon-api/.../landingrecord/adapter/in/web/LandingRecordDto.java` | 신규 | 응답/요청 DTO |
| `icon-api/.../landingrecord/adapter/in/web/JsonFilterParam.java` | 신규 | `jsonFilters` 파싱 클래스 (`key:op:value`) |
| `icon-api/.../landingrecord/adapter/out/persistence/entity/LandingRecordEntity.java` | 신규 또는 기존 확인 | `landing_records` JPA 엔티티 |
| `icon-api/.../landingrecord/adapter/out/persistence/repository/LandingRecordJpaRepository.java` | 신규 또는 기존 확인 | Spring Data JPA |
| `icon-api/.../landingrecord/adapter/out/persistence/repository/LandingRecordQueryRepository.java` | 신규 | JSONB 동적 필터 쿼리 (QueryDSL 또는 네이티브 쿼리) |
| `icon-api/.../landingrecord/application/service/LandingRecordService.java` | 신규 | 조회 서비스 |

> **사전 확인**: `LandingRecordEntity`가 icon-engine 모듈에 이미 존재할 수 있음. Grep으로 확인 후 재사용.

### 프론트엔드

| 파일 | 변경 유형 | 설명 |
|---|---|---|
| `icon-frontend/src/app/data-sources/api.ts` | 수정 | `fetchLandingRecords()`, `LandingRecord` 타입 추가 |
| `icon-frontend/src/components/datasource/LandingRecordView.tsx` | 신규 | 수집 원본 조회 컴포넌트 (필터+테이블+모달) |
| `icon-frontend/src/components/datasource/JsonFilterBuilder.tsx` | 신규 | 동적 JSONB 필터 행 UI (재사용 컴포넌트) |
| `icon-frontend/src/app/data-sources/[id]/page.tsx` | 수정 | `"landing"` 탭 추가 및 `LandingRecordView` 렌더 |

> **`JsonFilterBuilder` 재사용**: TODO-002의 매핑 결과 화면에서도 동일 컴포넌트 재사용.

---

## DB 참조

**테이블**: `landing_records`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `landing_record_id` | BIGINT PK | 식별자 |
| `exec_ds_mp_id` | BIGINT | 실행 이력 FK |
| `data_source_id` | VARCHAR(50) | 데이터소스 FK |
| `source_type` | VARCHAR(50) | FILE / JDBC |
| `raw_payload` | JSONB | 원본 수집 데이터 (`cust_no` 등 비즈니스 필드 포함) |
| `row_index` | INTEGER | 레코드 순번 |
| `batch_key` | VARCHAR(100) | 파일명/파티션 키 |
| `extracted_at` | TIMESTAMP | 수집 시각 (기간 필터 기준 컬럼) |
| `ingestion_status` | VARCHAR(20) | NEW / TRANSFORMED / FAILED |
| `ingestion_message` | TEXT | 오류 메시지 |

**인덱스 확인 필요** (성능):
- `data_source_id + extracted_at` 복합 인덱스 → 없으면 마이그레이션 추가
- `(raw_payload->>'cust_no')` 표현식 인덱스 → 자주 검색하는 필드 대상

---

## 완료 조건

- [ ] 데이터소스 상세 페이지에 "수집 원본" 탭 표시
- [ ] 기간(From-To) 필터 동작 (`extracted_at` 기준)
- [ ] 수집 상태 필터 동작
- [ ] `cust_no` 동적 필터 동작 (JSONB 내부 키 검색)
- [ ] `+` 버튼으로 동적 필터 행 추가/삭제 가능
- [ ] 페이지네이션 동작
- [ ] `raw_payload` 상세 모달에서 전체 JSON 확인 가능
- [ ] 복사 버튼 동작
- [ ] 데이터 없을 때 빈 상태(Empty State) 표시
