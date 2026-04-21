# TODO-001: 수집 데이터 원본 조회 화면 추가

## 메타

| 항목 | 내용 |
|---|---|
| 상태 | 🔲 미착수 |
| 분류 | 프론트엔드 + 백엔드 API |
| 작성일 | 2026-04-21 |
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

| 필터 | 타입 | 설명 |
|---|---|---|
| 조회 기간 | 날짜 범위 선택 (기본: 최근 24시간) | `created_at` 기준 |
| 수집 상태 | 드롭다운 멀티선택 | `ingestion_status`: ALL / SUCCESS / FAILED / PENDING |
| 실행 ID | 텍스트 입력 (선택) | `exec_ds_mp_id` 검색 |

### 통계 요약 카드 (필터 결과 기준)

| 카드 | 내용 |
|---|---|
| 전체 건수 | 조회된 레코드 수 |
| 성공 건수 | ingestion_status = SUCCESS |
| 실패 건수 | ingestion_status = FAILED |
| 최근 수집 시각 | 가장 최신 created_at |

### 목록 테이블

| 컬럼 | DB 컬럼 | 비고 |
|---|---|---|
| # | `row_index` | 정렬 기준 |
| 수집 시각 | `created_at` | `YYYY-MM-DD HH:mm:ss` 형식 |
| 실행 ID | `exec_ds_mp_id` | 클릭 시 해당 실행 이력 필터링 |
| 수집 상태 | `ingestion_status` | SUCCESS(초록) / FAILED(빨강) / PENDING(회색) 배지 |
| 원본 데이터 | `raw_payload` | JSON 한 줄 미리보기 (100자 truncate) |
| 상세 버튼 | - | raw_payload 전체 모달로 보기 |

### 상세 모달

- `raw_payload` JSON을 포맷팅하여 전체 표시
- 복사 버튼 (`raw_payload` 클립보드 복사)
- 해당 레코드의 `exec_ds_mp_id`, `row_index`, `ingestion_status`, `created_at` 메타 정보 표시

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
| `startDate` | ISO datetime | 24시간 전 | 조회 시작 시각 |
| `endDate` | ISO datetime | 현재 | 조회 종료 시각 |
| `ingestionStatus` | string (반복 가능) | - | SUCCESS / FAILED / PENDING |
| `execDsMpId` | string | - | 특정 실행 ID 필터 |
| `page` | int | 0 | 페이지 번호 |
| `size` | int | 20 | 페이지 크기 (최대 100) |

**Response Body**

```json
{
  "data": [
    {
      "landingRecordId": "string",
      "execDsMpId": "string",
      "dataSourceId": "string",
      "rowIndex": 0,
      "rawPayload": { },
      "ingestionStatus": "SUCCESS",
      "createdAt": "2026-04-21T10:00:00"
    }
  ],
  "total": 1234,
  "page": 0,
  "size": 20
}
```

---

## 구현 파일 목록

### 백엔드

| 파일 | 변경 유형 | 설명 |
|---|---|---|
| `icon-api/.../landingrecord/adapter/in/web/LandingRecordController.java` | 신규 | GET API 컨트롤러 |
| `icon-api/.../landingrecord/adapter/in/web/LandingRecordDto.java` | 신규 | 응답 DTO |
| `icon-api/.../landingrecord/adapter/out/persistence/entity/LandingRecordEntity.java` | 신규 또는 기존 확인 | `landing_records` JPA 엔티티 |
| `icon-api/.../landingrecord/adapter/out/persistence/repository/LandingRecordJpaRepository.java` | 신규 또는 기존 확인 | Spring Data JPA |
| `icon-api/.../landingrecord/application/service/LandingRecordService.java` | 신규 | 조회 서비스 |

> **사전 확인 필요**: `LandingRecordEntity`가 icon-engine 또는 다른 모듈에 이미 존재할 수 있음. Grep으로 확인 후 재사용.

### 프론트엔드

| 파일 | 변경 유형 | 설명 |
|---|---|---|
| `icon-frontend/src/app/data-sources/api.ts` | 수정 | `fetchLandingRecords()` 함수 및 타입 추가 |
| `icon-frontend/src/components/datasource/LandingRecordView.tsx` | 신규 | 수집 원본 조회 컴포넌트 |
| `icon-frontend/src/app/data-sources/[id]/page.tsx` | 수정 | `"landing"` 탭 추가 및 `LandingRecordView` 렌더 |

---

## DB 참조

**테이블**: `landing_records`

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `exec_ds_mp_id` | VARCHAR | 실행 이력 FK |
| `data_source_id` | VARCHAR | 데이터소스 FK |
| `raw_payload` | JSONB | 원본 수집 데이터 |
| `row_index` | INTEGER | 레코드 순번 |
| `ingestion_status` | VARCHAR | SUCCESS / FAILED / PENDING |

> **성능 주의**: `landing_records`는 대용량 테이블이 될 수 있음.  
> `data_source_id + created_at` 복합 인덱스 확인 필요. 없으면 마이그레이션으로 추가.

---

## 완료 조건

- [ ] 데이터소스 상세 페이지에 "수집 원본" 탭 표시
- [ ] 기간/상태 필터 동작
- [ ] 페이지네이션 동작
- [ ] `raw_payload` 상세 모달에서 전체 JSON 확인 가능
- [ ] 복사 버튼 동작
- [ ] 데이터 없을 때 빈 상태(Empty State) 표시
