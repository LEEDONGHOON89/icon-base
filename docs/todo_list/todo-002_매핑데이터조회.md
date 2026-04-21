# TODO-002: 파싱 및 필드 매핑 후 데이터 조회 화면 추가

## 메타

| 항목 | 내용 |
|---|---|
| 상태 | 🔲 미착수 |
| 분류 | 프론트엔드 + 백엔드 API |
| 작성일 | 2026-04-21 |
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

| 필터 | 타입 | 설명 |
|---|---|---|
| 조회 기간 | 날짜 범위 선택 (기본: 최근 24시간) | `created_at` 기준 |
| 프로파일 | 드롭다운 단일선택 | 데이터소스에 속한 프로파일 목록. "전체"가 기본값 |
| 거래 ID | 텍스트 입력 (선택) | `transaction_id` 직접 검색 |

### 통계 요약 카드 (필터 결과 기준)

| 카드 | 내용 |
|---|---|
| 전체 건수 | 조회된 레코드 수 |
| 적용 프로파일 수 | 조회된 레코드에서 고유 프로파일 수 |
| 최근 매핑 시각 | 가장 최신 `created_at` |

### 목록 테이블

| 컬럼 | DB 컬럼/설명 | 비고 |
|---|---|---|
| # | 행 번호 | - |
| 매핑 시각 | `created_at` | `YYYY-MM-DD HH:mm:ss` |
| 거래 ID | `transaction_id` | - |
| 프로파일 | `profile_id` → 프로파일명 | 이름으로 표시 |
| 데이터 미리보기 | `mapped_data` JSONB | 주요 표준 필드 3~5개 요약 표시 |
| 상세 버튼 | - | 전체 매핑 데이터 모달 |

> **주요 표준 필드 미리보기**: 데이터소스 프로파일에서 `entity_id_field`, `timestamp_key` 등 핵심 필드를 먼저 표시.

### 상세 모달 — 탭 2개

#### 탭 1: 매핑 결과
- 표준 필드명(fieldName) + 값을 테이블로 표시
- 파서로 추출된 필드(COLUMN1, COLUMN2 등)와 매핑된 표준 필드를 함께 표시
- 미매핑 필드는 회색으로 표시

#### 탭 2: 원본 비교

| 구분 | 내용 |
|---|---|
| 왼쪽 | 수집 원본 (`landing_records.raw_payload`) |
| 오른쪽 | 매핑 결과 (`mapped_storages.mapped_data`) |

- 원본 → 매핑 변환 과정을 나란히 비교 가능
- 각 패널에 클립보드 복사 버튼

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
| `startDate` | ISO datetime | 24시간 전 | 조회 시작 시각 |
| `endDate` | ISO datetime | 현재 | 조회 종료 시각 |
| `profileId` | string | - | 프로파일 필터 |
| `transactionId` | string | - | 거래 ID 검색 |
| `page` | int | 0 | 페이지 번호 |
| `size` | int | 20 | 페이지 크기 (최대 100) |

**Response Body**

```json
{
  "data": [
    {
      "mappedStorageId": "string",
      "dataSourceId": "string",
      "profileId": "string",
      "profileName": "string",
      "transactionId": "string",
      "mappedData": { },
      "createdAt": "2026-04-21T10:00:00"
    }
  ],
  "total": 1234,
  "page": 0,
  "size": 20
}
```

### 기존 API 확인 필요

`mapped_storages` 조회 관련 API가 트랜잭션 추적(`/transactions`) 화면에서 이미 일부 구현되어 있을 수 있음. 중복 구현 방지를 위해 구현 전 확인 필요.

---

## 구현 파일 목록

### 백엔드

| 파일 | 변경 유형 | 설명 |
|---|---|---|
| `icon-api/.../mappedstorage/adapter/in/web/MappedStorageController.java` | 신규 또는 기존 확인 | GET API 컨트롤러 |
| `icon-api/.../mappedstorage/adapter/in/web/MappedStorageDto.java` | 신규 또는 기존 확인 | 응답 DTO |
| `icon-api/.../mappedstorage/adapter/out/persistence/entity/MappedStorageEntity.java` | 신규 또는 기존 확인 | `mapped_storages` JPA 엔티티 |
| `icon-api/.../mappedstorage/adapter/out/persistence/repository/MappedStorageJpaRepository.java` | 신규 또는 기존 확인 | Spring Data JPA |
| `icon-api/.../mappedstorage/application/service/MappedStorageService.java` | 신규 또는 기존 확인 | 조회 서비스 |

> **사전 확인 필요**: 트랜잭션 추적 기능 구현 시 `MappedStorageEntity`가 이미 생성됐을 가능성 있음.

### 프론트엔드

| 파일 | 변경 유형 | 설명 |
|---|---|---|
| `icon-frontend/src/app/data-sources/api.ts` | 수정 | `fetchMappedStorages()` 함수 및 타입 추가 |
| `icon-frontend/src/components/datasource/MappedStorageView.tsx` | 신규 | 매핑 결과 조회 컴포넌트 |
| `icon-frontend/src/app/data-sources/[id]/page.tsx` | 수정 | `"mapped"` 탭 추가 및 `MappedStorageView` 렌더 |

---

## DB 참조

**테이블**: `mapped_storages`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `data_source_id` | VARCHAR | 데이터소스 FK |
| `profile_id` | VARCHAR | 프로파일 FK |
| `transaction_id` | VARCHAR | 거래 식별자 |
| `mapped_data` | JSONB | 표준 필드 매핑 결과 (`{표준필드명: 값, ...}`) |
| `created_at` | TIMESTAMP | 매핑 처리 시각 |

> **성능 주의**: `mapped_storages`는 대용량 테이블.  
> `data_source_id + created_at` 복합 인덱스 확인 필요. 없으면 마이그레이션으로 추가.

---

## 관련 화면

| 화면 | 연관성 |
|---|---|
| TODO-001 수집 원본 조회 | 상세 모달의 "원본 비교" 탭에서 `landing_records.raw_payload` 참조 |
| 트랜잭션 추적 (`/transactions`) | 거래 ID 클릭 시 트랜잭션 추적 화면으로 딥링크 연결 고려 |
| 파서 설정 탭 | 파서 적용 결과 확인 목적으로 이 화면이 활용됨 |

---

## 완료 조건

- [ ] 데이터소스 상세 페이지에 "매핑 결과" 탭 표시
- [ ] 기간/프로파일 필터 동작
- [ ] 페이지네이션 동작
- [ ] 상세 모달 — "매핑 결과" 탭: 표준 필드 목록 + 값 표시
- [ ] 상세 모달 — "원본 비교" 탭: 원본 raw_payload vs 매핑 결과 나란히 표시
- [ ] 클립보드 복사 버튼 동작
- [ ] 데이터 없을 때 빈 상태(Empty State) 표시
- [ ] 거래 ID 클릭 시 `/transactions?tab=search&txId={id}` 딥링크 이동 (선택)
