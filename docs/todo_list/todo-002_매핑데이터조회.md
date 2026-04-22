# TODO-002: 파싱 및 필드 매핑 후 데이터 조회 화면 추가

## 메타

| 항목 | 내용 |
|---|---|
| 상태 | 🔲 미착수 |
| 분류 | 프론트엔드 + 백엔드 API |
| 작성일 | 2026-04-21 |
| 수정일 | 2026-04-22 — cust_no 필터 및 확장형 JSONB 필터 구조 추가, DB 컬럼 실제값 반영 |
| 우선순위 | 중 |

---

## 목적

PREP-3 단계에서 파서 적용 + 표준 필드 매핑이 완료된 레코드(`mapped_storages`)를 데이터소스 상세 화면에서 직접 조회할 수 있도록 한다.  
파서 설정이나 필드 매핑이 의도대로 동작하는지 실제 데이터로 검증하는 용도이다.

---

## 위치

- **데이터소스 상세 페이지** `/data-sources/[id]` 에 새 탭으로 추가
- 탭명: **"매핑 결과"**
- 기존 탭 순서: `개요 | 연결설정 | 원본필드(스키마) | 프로파일관리 | 파서 설정 | 수집 원본 | 매핑 결과` ← 추가

---

## 화면 구성

### 필터 영역

> **설계 원칙**: 기본 필터(기간·상태)는 고정 UI로, JSONB 내부 필드 검색은 확장형 동적 필터 행으로 처리.  
> `cust_no`는 `row_data` JSONB 안의 키이므로 동적 필터로 검색한다.  
> `JsonFilterBuilder` 컴포넌트는 TODO-001과 공유.

#### 기본 필터 (항상 표시)

| 필터 | 타입 | DB 컬럼 | 기본값 |
|---|---|---|---|
| 조회 기간 (From) | 날짜+시간 선택 | `reg_dt >=` | 최근 24시간 |
| 조회 기간 (To) | 날짜+시간 선택 | `reg_dt <=` | 현재 |
| 처리 상태 | 드롭다운 멀티선택 | `processing_status` | ALL |

#### 동적 필드 필터 (확장형, + 버튼으로 행 추가)

`row_data` JSONB 내부 키를 자유롭게 검색할 수 있는 확장 가능한 필터 행.

| 구성 요소 | 설명 |
|---|---|
| 필드명 입력 | `row_data` 내 키 이름 (예: `cust_no`, `account_id`, ...) |
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
| 처리 완료 | `processing_status = 'DONE'` (또는 실제 상태값 확인 필요) |
| 실패 건수 | `processing_status = 'FAILED'` |
| 최근 매핑 시각 | 가장 최신 `reg_dt` |

---

### 목록 테이블

| 컬럼 | DB 컬럼 | 비고 |
|---|---|---|
| # | `mapped_storage_id` | 정렬 기준 |
| 매핑 시각 | `reg_dt` | `YYYY-MM-DD HH:mm:ss` |
| 거래 ID | `transaction_id` | - |
| 처리 상태 | `processing_status` | 배지 표시 |
| cust_no | `row_data->>'cust_no'` | 해당 필드 있을 경우 표시, 없으면 `-` |
| 데이터 미리보기 | `row_data` | 주요 필드 3~5개 요약 |
| 상세 | - | 전체 매핑 데이터 모달 |

> **컬럼 확장 고려**: 동적 필터에 입력된 필드명은 테이블 컬럼으로도 추가 표시하는 것을 고려.

---

### 상세 모달 — 탭 2개

#### 탭 1: 매핑 결과
- `row_data` JSONB 필드를 테이블로 표시 (필드명 + 값)
- 처리 상태 / `error_message` (FAILED 시) 표시
- 메타 정보: `mapped_storage_id`, `transaction_id`, `row_index`, `reg_dt`

#### 탭 2: 원본 비교

| 구분 | 내용 |
|---|---|
| 왼쪽 | 수집 원본 (`landing_records.raw_payload`, `landing_record_id`로 JOIN) |
| 오른쪽 | 매핑 결과 (`mapped_storages.row_data`) |

- 원본 → 매핑 변환 과정을 나란히 비교 가능
- 각 패널에 클립보드 복사 버튼

---

### 페이지네이션

- 페이지당 20건
- 총 건수 표시

---

## 백엔드 API

### 신규 API

```
GET /api/v1/data-sources/{dataSourceId}/mapped-storages
```

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|---|---|---|---|
| `startDate` | ISO datetime | 24시간 전 | `reg_dt >=` |
| `endDate` | ISO datetime | 현재 | `reg_dt <=` |
| `processingStatus` | string (반복 가능) | - | 처리 상태 필터 |
| `transactionId` | string | - | `transaction_id` 완전 일치 검색 |
| `custNo` | string | - | `row_data->>'cust_no' = ?` JSONB 검색 |
| `jsonFilters` | `key:op:value` 반복 | - | 확장형 JSONB 필터 (아래 참조) |
| `page` | int | 0 | 페이지 번호 |
| `size` | int | 20 | 페이지 크기 (최대 100) |

**`jsonFilters` 파라미터 설계 (확장형)**

TODO-001과 동일 구조. `row_data` JSONB 내부 키 검색.

```
GET /api/v1/data-sources/DS001/mapped-storages
    ?jsonFilters=cust_no:eq:12345678
    &jsonFilters=account_id:like:ACC%
```

| 연산자 코드 | SQL 변환 | 설명 |
|---|---|---|
| `eq` | `row_data->>'key' = 'value'` | 완전 일치 |
| `like` | `row_data->>'key' ILIKE '%value%'` | 부분 일치 (대소문자 무시) |
| `neq` | `row_data->>'key' != 'value'` | 불일치 |

> **보안 주의**: `key` 값은 정규식 검증 필수 (`^[a-zA-Z0-9_]+$`).

**Response Body**

```json
{
  "data": [
    {
      "mappedStorageId": 98765,
      "landingRecordId": 12345,
      "dataSourceId": "DS001",
      "transactionId": "TXN-20260421-001",
      "rowIndex": 0,
      "rowData": { "cust_no": "12345678", "AMOUNT": 50000 },
      "processingStatus": "DONE",
      "errorMessage": null,
      "regDt": "2026-04-21T10:00:05"
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

> `mapped_storages`는 `data_source_id` 컬럼이 없음.  
> `landing_record_id` → `landing_records.data_source_id` JOIN으로 데이터소스 필터링.

### 기존 API 확인 필요

`mapped_storages` 조회 관련 API가 트랜잭션 추적(`/transactions`) 화면에서 이미 일부 구현되어 있을 수 있음. 중복 구현 방지를 위해 구현 전 Grep 확인 필요.

---

## 구현 파일 목록

### 백엔드

| 파일 | 변경 유형 | 설명 |
|---|---|---|
| `icon-api/.../mappedstorage/adapter/in/web/MappedStorageController.java` | 신규 또는 기존 확인 | GET API 컨트롤러 |
| `icon-api/.../mappedstorage/adapter/in/web/MappedStorageDto.java` | 신규 또는 기존 확인 | 응답/요청 DTO |
| `icon-api/.../mappedstorage/adapter/in/web/JsonFilterParam.java` | TODO-001과 공통 모듈화 | `key:op:value` 파싱 |
| `icon-api/.../mappedstorage/adapter/out/persistence/entity/MappedStorageEntity.java` | 신규 또는 기존 확인 | `mapped_storages` JPA 엔티티 |
| `icon-api/.../mappedstorage/adapter/out/persistence/repository/MappedStorageJpaRepository.java` | 신규 또는 기존 확인 | Spring Data JPA |
| `icon-api/.../mappedstorage/adapter/out/persistence/repository/MappedStorageQueryRepository.java` | 신규 | JSONB 동적 필터 + landing JOIN 쿼리 |
| `icon-api/.../mappedstorage/application/service/MappedStorageService.java` | 신규 또는 기존 확인 | 조회 서비스 |

> **사전 확인 필요**: 트랜잭션 추적 기능 구현 시 `MappedStorageEntity`가 이미 생성됐을 가능성 있음.

### 프론트엔드

| 파일 | 변경 유형 | 설명 |
|---|---|---|
| `icon-frontend/src/app/data-sources/api.ts` | 수정 | `fetchMappedStorages()`, `MappedStorage` 타입 추가 |
| `icon-frontend/src/components/datasource/MappedStorageView.tsx` | 신규 | 매핑 결과 조회 컴포넌트 (필터+테이블+모달) |
| `icon-frontend/src/components/datasource/JsonFilterBuilder.tsx` | TODO-001과 공유 | 동적 JSONB 필터 행 UI |
| `icon-frontend/src/app/data-sources/[id]/page.tsx` | 수정 | `"mapped"` 탭 추가 및 `MappedStorageView` 렌더 |

---

## DB 참조

**테이블**: `mapped_storages`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `mapped_storage_id` | BIGINT PK | 식별자 |
| `landing_record_id` | BIGINT FK | `landing_records` 참조 (데이터소스 필터링에 사용) |
| `exec_ds_mp_id` | BIGINT | 실행 이력 참조 |
| `row_index` | INTEGER | Row 순서 |
| `row_data` | JSONB | 매핑된 데이터 (`cust_no` 등 필드 포함) |
| `processing_status` | VARCHAR(20) | 처리 상태 |
| `error_message` | TEXT | 오류 메시지 |
| `transaction_id` | VARCHAR(255) | 거래 식별자 |
| `reg_dt` | TIMESTAMP | 생성 시각 (기간 필터 기준 컬럼) |

> ⚠️ `mapped_storages`에는 `data_source_id` 컬럼이 없음.  
> 데이터소스 기준 조회는 `landing_record_id → landing_records.data_source_id` JOIN 필요.

**인덱스 확인 필요** (성능):
- `landing_record_id + reg_dt` 복합 인덱스 → 없으면 마이그레이션 추가
- `(row_data->>'cust_no')` 표현식 인덱스 → 자주 검색하는 필드 대상

---

## 관련 화면

| 화면 | 연관성 |
|---|---|
| TODO-001 수집 원본 조회 | 상세 모달 "원본 비교" 탭에서 `landing_records.raw_payload` 참조, `JsonFilterBuilder` 공유 |
| 트랜잭션 추적 (`/transactions`) | `transaction_id` 클릭 시 딥링크 이동 고려 |
| 파서 설정 탭 | 파서 적용 결과 확인 목적으로 이 화면 활용 |

---

## 완료 조건

- [ ] 데이터소스 상세 페이지에 "매핑 결과" 탭 표시
- [ ] 기간(From-To) 필터 동작 (`reg_dt` 기준)
- [ ] 처리 상태 필터 동작
- [ ] `cust_no` 동적 필터 동작 (`row_data` JSONB 내부 키 검색)
- [ ] `+` 버튼으로 동적 필터 행 추가/삭제 가능
- [ ] `landing_records` JOIN으로 데이터소스 기준 필터링 동작
- [ ] 페이지네이션 동작
- [ ] 상세 모달 — "매핑 결과" 탭: `row_data` 필드 목록 + 값 표시
- [ ] 상세 모달 — "원본 비교" 탭: `raw_payload` vs `row_data` 나란히 표시
- [ ] 클립보드 복사 버튼 동작
- [ ] 데이터 없을 때 빈 상태(Empty State) 표시
- [ ] `transaction_id` 클릭 시 `/transactions` 딥링크 이동 (선택)
