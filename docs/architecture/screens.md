# ICON - 화면 기능 명세

> 작성: 2026-04-16

---

## 화면 목록

| # | 메뉴 | 화면 | 상태 | Next.js 경로 |
|---|---|---|---|---|
| 2-1 | - | 대시보드 | ✅ | `/dashboard` |
| 2-2 | 탐지정책관리 | 센서 관리 | ✅ | `/sensors` |
| 2-3 | 탐지정책관리 | 룰 관리 | ✅ | `/rules` |
| 2-4 | 탐지정책관리 | 시나리오 관리 | ✅ | `/scenarios` |
| 2-5 | 탐지정책관리 | 도메인 설정 | ✅ | `/domain-settings` |
| 2-6 | 탐지정책관리 | 엔티티 관계 설정 | ✅ | `/relation-rules` |
| 2-7 | 탐지정책관리 | AI 서포트 | ✅ | `/ai-support` |
| 2-8 | 탐지모니터링 | 시나리오 탐지 | ✅ | `/detections/scenarios` |
| 2-9 | 탐지모니터링 | 룰 탐지 | ✅ | `/detections/rules` |
| 2-10 | 탐지모니터링 | 탐지 조치 | ✅ | `/detection-settings` |
| 2-11 | 탐지모니터링 | 엔티티 행적 | ✅ | `/detections/entities` |
| 2-12 | 탐지모니터링 | 시뮬레이션 | ✅ | `/detections/simulation` |
| 2-13 | 탐지모니터링 | 트랜잭션 추적 | ✅ | `/transactions` |
| 2-14 | 탐지모니터링 | AI 모니터링 | ⚠️ 미개발 | - |
| 2-15 | 감사 | 변경이력 | ✅ | `/audit` |
| 2-16 | 시스템 설정 | 데이터소스 | ✅ | `/data-sources` |
| 2-17 | 시스템 설정 | 표준 필드 | ✅ | `/fields` |
| 2-18 | 시스템 설정 | 엔티티 필드 | ✅ CRUD 관리 | `/fields/entity` |
| 2-19 | 탐지모니터링 | 실행이력 | ⛔ 사용안함 | `/detections/executions` |
| 2-20 | 시스템 설정 | 파서 관리 | ✅ | `/parsers` |

---

## 2-1. 대시보드

**경로**: `/dashboard`

### 구성
- 실시간 통계 카드: 전체거래수 / 탐지거래 / 탐지율
- 위험수준별 건수: BLOCK / REVIEW / INTENSIVE / MONITOR
- 자동 새로고침: 1/5/10/15분 선택
- 시간대별 탐지 차트 (시계열)
- 탐지영역별 현황 Top 5 시나리오

### 조치 상태 색상
| 상태 | 색상 |
|---|---|
| 미처리 | 회색 |
| 승인 | 파랑 |
| 거절 | 빨강 |
| 완료 | 초록 |

---

## 2-2. 센서 관리

**경로**: `/sensors`

### 기능
- 목록/카드 뷰 전환 (localStorage 저장)
- 센서 CRUD

### 센서 ID 규칙
- `S_` 접두사 필수
- 대문자/숫자/언더스코어만 허용
- 생성 후 변경 불가

### 조건 설정
- Builder 모드: UI에서 필드/연산자/값 선택
- JSON 모드: Predicate 직접 입력
- 조건 필드: `standard_fields` 테이블 동적 조회

### 참고
- 낙관적 잠금(Optimistic Locking) - 동시 편집 충돌 방지

---

## 2-3. 룰 관리

**경로**: `/rules`

### 기능
- 룰 CRUD
- 집계 연산자 선택 (COUNT/SUM/DISTINCT_COUNT/SEQUENCE)
- 센서 ID, 윈도우 시간(분), 임계값, Group-by 필드 설정

### 룰 ID 규칙
- `AGG_` 접두사 필수

### 평가 모드
- `WINDOW`: 시간창 기반 집계
- `SINGLE_ROW`: 단일 이벤트 즉시 평가

### 집계 연산자
| 연산자 | 설명 |
|---|---|
| COUNT_WITHIN | 횟수 집계 |
| SUM_WITHIN | 합계 집계 |
| DISTINCT_COUNT_WITHIN | 고유값 횟수 |
| SEQUENCE_WITHIN | 순서 패턴 (A이벤트 후 B이벤트) |

---

## 2-4. 시나리오 관리

**경로**: `/scenarios`

### 기능
- 시나리오 CRUD
- 섹션별 독립 저장 (기본정보 / 엔티티 필터 / 집계 조합)
- AND/OR 로직 연산자로 AGG_ 룰 조합

### 필수 항목
- 이름(unique), 위험수준, 탐지영역
- 최소 1개 이상의 AGG_ 룰

### 엔티티 필터
- 모두 AND 조건으로 적용
- 예: `customer_age >= 65` (노년층 고객 대상)

---

## 2-5. 도메인 설정

**경로**: `/domain-settings`

### 기능
- 도메인 CRUD
- 아이콘명, 색상(HEX), 표시 순서, 활성화 토글 설정

### 도메인 ID 규칙
- 대문자 영문, 숫자, 언더스코어만 허용
- 생성 후 변경 불가

---

## 2-6. 엔티티 관계 설정

**경로**: `/relation-rules`

### 기능
- 관계 규칙 CRUD
- DataSource → From 엔티티 → 관계 타입(OWNS/USES/ACCESSES) → To 엔티티 설정
- 배열 필드 지원 (이벤트 1건 → 여러 관계 추출)

### 관련 테이블
- `entity_relation_rules` - 규칙 정의
- `entity_attributes` - 추출된 엔티티
- `entity_relations` - 관계 데이터
- `entity_source_records` - 추출 이력

---

## 2-7. AI 서포트

**경로**: `/ai-support`

### 기능
- 탭 2개: 대화(Chat) / AI 학습
- 빠른 질문 템플릿: 시스템개요 / 탐지영역설정 / 룰작성가이드 / 시나리오설정
- 타이핑 애니메이션, 마크다운 렌더링

### AI 학습
- 활성화된 룰 이름/설명/조건 전체를 AI에 전송
- RAG 기반: "VIP 고객 관련 룰 보여줘" 같은 맥락 기반 질의 가능

### 입력 UX
- Enter: 전송
- Shift+Enter: 줄바꿈
- 최대 100개 세션

---

## 2-8. 시나리오 탐지

**경로**: `/detections/scenarios`

### 목록 컬럼
- 시나리오명 / 탐지영역 / 그룹키(엔티티ID) / 거래ID / 탐지시각 / 결과(전체통과/부분통과)

### 상세 모달
- 기본정보 + 트리거 집계룰 + 매칭 이벤트 탭
- 이벤트 탭 구성: 거래정보 / 고객정보 / 디바이스정보 / 계좌정보 / 위험지표 / 원본

### 클릭 링크
| 항목 | 링크 대상 |
|---|---|
| 시나리오명 | 시나리오 편집기 |
| 그룹키 | 엔티티 행적 |
| 거래ID | 트랜잭션 추적 |

### 심각도 평가 가이드
- 전체통과 + 고위험 시나리오 + 신규고객 → 즉시 계좌정지 검토

---

## 2-9. 룰 탐지

**경로**: `/detections/rules`

### 목록 컬럼
- 앵커시각 / 그룹키 / 룰명 / 연산자(COUNT/SUM/AVG/MAX/MIN) / 매칭건수 / 임계값 / 윈도우(분)

### 상세 모달
- 앵커시각, 윈도우 범위, 대상 룰(predicate sensor), 매칭 이벤트 목록 + full JSON

---

## 2-10. 탐지 조치

**경로**: `/detection-settings`

### 필터
- 위험수준 드롭다운
- 조치상태 멀티선택 칩 (PENDING/APPROVED/REJECTED/COMPLETED)
- 날짜 범위

### 조치 모달
- 탐지정보(읽기전용) + 조치상태(편집) + 사유 텍스트 + 메모 텍스트

### 처리 기준
| 수준 | 기준 |
|---|---|
| BLOCK | 1시간 내 |
| REVIEW | 당일 |
| INTENSIVE | 주 단위 |
| MONITOR | 월 단위 |

> 사유 기록 필수 (컴플라이언스 audit trail)

---

## 2-11. 엔티티 행적

**경로**: `/detections/entities`

### 검색
- 단일 엔티티 ID 또는 복합키 (파이프 구분: `EMP004|330-444-555666`)
- 최근 30개 탐지 엔티티 빠른 접근 버튼

### 통계 대시보드
- 시나리오 탐지수 / 활동로그수 / 탐지율% / 활동기간

### 탭 4개
| 탭 | 내용 | 제한 |
|---|---|---|
| 탐지이력 | 시나리오 탐지 결과 목록 | 최대 100건 |
| 활동로그 | 이벤트 스트림 로그 | 최대 200건 |
| 엔티티속성 | 누적 프로파일 속성 | - |
| 엔티티연결 | 관계 그래프 (depth=2, limit=10) | - |

### 탐지율 임계값
- 0~5%: 정상
- 5~10%: 주의
- 10%+: 고위험

---

## 2-12. 시뮬레이션

**경로**: `/detections/simulation`

### 입력 방식
- **DataSource 탭**: 단일 DS 선택 + JSON 입력
- **단일실행 탭**: 멀티 DS 배치 형식

### 결과 통계 6종
- Execution ID / 전송건수 / Event Streams / Entity Attributes / 센서탐지 / 룰탐지 / 시나리오탐지

### 단축키
- Ctrl+Enter / Cmd+Enter: 실행

### 주의사항
- 결과가 실제 DB에 저장됨
- 테스트 데이터는 `TEST_` 또는 `DEMO_` 접두사 사용 권고

---

## 2-13. 트랜잭션 추적

**경로**: `/transactions`

### 탭 구성
- **트랜잭션 검색 탭**: 특정 ID 직접 입력
- **최근 트랜잭션 목록 탭**: 최근 30건

### 트랜잭션 요약
- 트랜잭션 ID / 데이터소스 / 최초 수집 시각 / 최종 처리 시각

### 파이프라인 타임라인 (4단계 노드)
| 단계 | 레이블 | 테이블 |
|---|---|---|
| PREP-1 | 데이터 수집/변환 | `mapped_storages` |
| DET-1 | 이벤트 스트림 | `event_streams` |
| DET-2-1 | 센서 탐지 | `detect_rules` |
| DET-2-2 | 시나리오 탐지 | `detect_scenarios` |

- 초록 노드: 정상 통과
- 회색 노드: 0건/미도달 → 첫 번째 회색 = 장애 지점

### 상세 탭 5개
- 전체 개요 / PREP-1 / DET-1 / DET-2-1 / DET-2-2

### URL 딥링크
```
?tab=search&txId={거래ID}
```

### 처리 시간 기준
- 정상: 1~5초
- 지연: 10초 이상 → 점검 필요

### 권한
- 시스템 관리자 또는 개발자 권한 필요 (내부 데이터 구조 노출)

---

## 2-14. AI 모니터링 ⚠️ 미개발

**개발 예정 로드맵**:
- Q2 2026: 채팅 UI
- Q3 2026: 비지도학습 패턴 (Isolation Forest, DBSCAN)
- Q4 2026: 예측 분석
- 2027 H1: 실시간 스코어링 (XGBoost/LightGBM)
- 2027 H2: 딥러닝 + 자동화, SHAP 설명가능 AI

---

## 2-15. 변경이력

**경로**: `/audit`

### 필터
- 텍스트 검색 (ID/이름/편집자)
- 타입: 센서/룰/시나리오
- 액션: CREATE / UPDATE / DELETE
- 날짜 범위: 오늘/7일/30일/90일/사용자지정

### 목록 컬럼
- 시각(상대+절대) / 타입 배지 / 액션 배지 / 대상(이름+ID) / 편집자 / 상세 버튼

### 상세 모달
- 기본정보
- 변경 필드 목록 (노란 배지)
- 변경 전 상태 (빨간 JSON)
- 변경 후 상태 (초록 JSON)
- IP 주소

### 기타
- 자동 새로고침: 30초
- 페이지네이션: 20건/페이지
- **롤백 절차**: "변경 전 상태" JSON 복사 → 관리 화면 수동 복원

---

## 2-16. 데이터소스 설정

**경로**: `/data-sources`

### 기능
- 카드 뷰(3열 그리드) CRUD

### 상세 탭 4개
| 탭 | 내용 |
|---|---|
| 개요 | 빠른시작 버튼 |
| 연결설정 | DB/API 연결 정보 |
| 원본필드(스키마) | 원본 필드 목록 |
| 프로파일관리 | 원본 → 표준 필드 매핑 |

### 지원 타입
- PostgreSQL, MySQL, CSV, JSON, Kafka, API

### 제약
- 타입 생성 후 변경 불가
- 룰 연결된 데이터소스 삭제 불가

---

## 2-17. 표준 필드 ✅

**경로**: `/fields`

### 기능
- 표준 필드 CRUD (추가/수정/삭제)
- 필드 ID / 표시명 검색

### 컬럼
- 필드 ID (monospace, 알파벳 정렬)
- 표시명
- 데이터 타입 (색상 배지)
- 카테고리
- 활성 상태
- 설명
- 작업 버튼 (수정/삭제)

### 필드 ID 규칙
- 영문자로 시작, 영문자/숫자/언더스코어만 허용
- 생성 후 변경 불가 (standard_field_id = PK)

### 수정 가능 항목
- 표시명, 데이터 타입, 카테고리, 설명, 활성 상태

---

## 2-18. 엔티티 필드 ✅ CRUD 관리

**경로**: `/fields/entity`

> [2026-04-24] 읽기전용 → 등록/수정/삭제 CRUD 관리 화면으로 확장

### 기능
- 엔티티 필드 목록 조회 (검색 포함)
- 신규 필드 등록 (모달) — entityFieldId, displayName, dataType, description
- 필드 수정 (모달) — displayName, dataType, description, 활성화 토글
- 필드 삭제 (확인 모달)

### 연동 API
- `GET /api/v1/entity-fields/all` — 전체 목록 (비활성 포함)
- `POST /api/v1/entity-fields` — 등록
- `PUT /api/v1/entity-fields/{entityFieldId}` — 수정
- `DELETE /api/v1/entity-fields/{entityFieldId}` — 삭제

### 초기 데이터 (V1_0_14)
고객: customer_age, grade, owned_accounts, region  
계좌: account_number, account_type, open_date, open_type, owner_id, status

---

## 2-19. 실행이력 ⛔ 사용안함

트랜잭션 추적(`/transactions`)으로 대체된 것으로 추정.

---

## 2-20. 파서 관리 ✅

**경로**: `/parsers`

### 기능
- 파서 목록/생성/수정/삭제 (CRUD)
- 데이터소스 원본 필드에서 파서 선택 연동

### 파서 타입
| 타입 | 설명 | config_json 예시 |
|---|---|---|
| DELIMITER | 구분자로 값을 분리하여 인덱스로 추출 | `{"delimiter":"|","index":0}` |
| FIXED_WIDTH | 바이트 위치와 길이로 값을 추출 | `{"startByte":0,"byteLength":10}` |
| REGEX | 정규식 캡처 그룹으로 값을 추출 | `{"pattern":"^(\w+)","group":1}` |

### 구조
- parsers(1) : parser_rules(N) — 파서 1개에 N개의 추출 규칙
- data_source_schemas.parser_id(FK) → parsers — 원본 필드 1개에 파서 1개 연동
- 파서 적용 시: 원본 필드 1개 값 → N개 추출 필드로 분리하여 mapped_storages에 저장
