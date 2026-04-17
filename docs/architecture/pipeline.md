# ICON - 탐지 파이프라인 상세

> 작성: 2026-04-16

---

## 1. 전체 파이프라인 개요

ICON의 탐지 엔진은 7단계 메인 파이프라인과 비동기 관계 분석 파이프라인으로 구성된다.

```
[메인 파이프라인]

PREP-1 ──→ PREP-2 ──→ PREP-3 ──→ DET-1 ──→ DET-2-1 ──→ DET-2-2 ──→ DET-2-3
Extract     Load      Transform   Stream   Sensor Eval  Rule Eval   Scenario Eval
                                              ↓            ↓            ↓
                                         detect_rules  (AGG 집계)  detect_scenarios
                                                                        ↓
                                                                    SYNC-1
                                                                 Entity Update

[비동기 관계 분석 파이프라인]

PREP-4(Enrich) → PREP-5-A(Explicit 관계 추출) → entity_relations
               → PREP-5-B(Pattern Discovery)
```

---

## 2. 단계별 상세

### PREP-1: Extract (데이터 수집)
- **역할**: 외부 시스템에서 원본 데이터 수집
- **수집 방식 4종**:
  - REST API Collector
  - File Reader (CSV / JSON)
  - Agent (icon-agent via WebSocket RPC)
  - Database Query Executor (JDBC: MySQL, Oracle, PostgreSQL)
- **저장**: `landing_records` 테이블

### PREP-2: Load (적재)
- **역할**: 원본 레코드를 내부 스토리지에 적재

### PREP-3: Transform (변환)
- **역할**: 원본 필드 → 표준 필드 매핑 (프로파일 기반)
- **매핑 규칙**: 데이터소스별 프로파일에서 정의 (예: `customer_id` → `entity_id`)
- **저장**: `mapped_storages` 테이블

### DET-1: Stream (이벤트 스트리밍)
- **역할**: 변환된 레코드를 이벤트 스트림으로 발행
- **저장**: `event_stream`, `event_stream_groups` 테이블

### DET-2-1: Sensor Evaluation (센서 탐지)
- **역할**: `S_` 접두사 센서의 Predicate 조건을 단일 이벤트에 적용
- **평가 모드**:
  - `SINGLE_ROW`: 단일 이벤트 즉시 평가
- **저장**: `detect_rules` (detect_sensors) 테이블

### DET-2-2: Rule/Aggregation Evaluation (룰 탐지)
- **역할**: `AGG_` 접두사 룰의 시간창 기반 집계 조건 평가
- **집계 연산자 4종**:

| 연산자 | 설명 | 예시 |
|---|---|---|
| `COUNT_WITHIN` | 시간창 내 횟수 집계 | 10분 내 로그인 실패 5회 이상 |
| `SUM_WITHIN` | 시간창 내 합계 집계 | 1시간 내 이체 합계 1000만원 초과 |
| `DISTINCT_COUNT_WITHIN` | 시간창 내 고유값 횟수 | 30분 내 서로 다른 IP 3개 이상 접속 |
| `SEQUENCE_WITHIN` | 시간창 내 순서 패턴 | A이벤트 발생 후 B이벤트 발생 |

- **SEQUENCE_WITHIN 설정**: 이전 센서ID(A), 다음 센서ID(B), 선택적 앵커 센서ID
- **저장**: `detect_rules` 테이블

### DET-2-3: Scenario Evaluation (시나리오 탐지)
- **역할**: 여러 AGG_ 룰을 AND/OR 로직으로 조합하여 최종 탐지 판정
- **판정 결과**: 전체통과 / 부분통과
- **위험수준 분류**:

| 수준 | 처리 기준 |
|---|---|
| BLOCK | 1시간 내 처리 필수 |
| REVIEW | 당일 처리 |
| INTENSIVE | 주 단위 모니터링 |
| MONITOR | 월 단위 모니터링 |

- **저장**: `detect_scenarios` 테이블

### SYNC-1: Entity Update (엔티티 동기화)
- **역할**: 탐지 결과를 엔티티 프로파일에 반영 (누적 속성 갱신)
- **저장**: `entity_attributes` 테이블

---

## 3. 비동기 관계 분석 파이프라인

### PREP-4: Enrich
- **역할**: 이벤트에 추가 컨텍스트 정보 보강

### PREP-5-A: Explicit Relation Extraction
- **역할**: 관계 규칙(`entity_relation_rules`)에 따라 명시적 엔티티 관계 추출
- **엔티티 타입**: CUSTOMER, ACCOUNT, DEVICE, EMPLOYEE, AUTHENTICATION
- **관계 타입**: OWNS / USES / ACCESSES
- **저장**: `entity_relations`, `entity_source_records`

### PREP-5-B: Pattern Discovery
- **역할**: 패턴 기반 암묵적 관계 발견

---

## 4. 트랜잭션 추적 (디버깅)

트랜잭션 추적 화면에서 각 단계 통과 여부를 실시간 확인할 수 있다.

```
[PREP-1]        [DET-1]       [DET-2-1]     [DET-2-2]
데이터 수집 → 이벤트 스트림 → 센서 탐지 → 시나리오 탐지
landing_records  event_streams  detect_rules  detect_scenarios

노드 색상: 초록=정상통과, 회색=0건/미도달
첫 번째 회색 노드 = 장애 발생 지점
```

**처리 시간 기준**:
- 정상: 1~5초
- 지연: 10초 이상 → 점검 필요

**URL 딥링크**: `?tab=search&txId={거래ID}`

---

## 5. 시뮬레이션

실제 데이터 없이 파이프라인 동작을 테스트할 수 있다.

- **입력 방식 2종**:
  - DataSource 선택 + JSON 입력 (단일 DS)
  - 멀티 DS 배치 형식
- **단축키**: Ctrl+Enter / Cmd+Enter
- **주의**: 시뮬레이션 결과가 실제 DB에 저장됨
  - 테스트 데이터는 `TEST_` 또는 `DEMO_` 접두사 사용 권고
- **결과 통계 6종**: Execution ID, Event Streams, Entity Attributes, 센서탐지, 룰탐지, 시나리오탐지
