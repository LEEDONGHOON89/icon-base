# ICON 프로젝트 작업 지침

---

## 작업 이력 참조

작업 시작 전 최신 작업 이력을 확인하여 충돌 및 중복 작업을 예방한다.

- **목록**: [`docs/work_list/INDEX.md`](docs/work_list/INDEX.md) — 전체 작업 이력 파일 목록
- **상세**: `docs/work_list/YYYY-MM-DD_작업내용.md` — 날짜별 변경 상세

작업 완료 후 해당 날짜의 작업 파일을 생성하고 INDEX.md에 추가한다.

---

## TODO 목록 참조

신규 기능 개발 작업 시 TODO 목록을 확인하여 기존 스펙과 중복·충돌을 방지한다.

- **목록**: [`docs/todo_list/TODO.md`](docs/todo_list/TODO.md) — 전체 TODO 인덱스 (상태 관리)
- **상세**: `docs/todo_list/todo-NNN_작업명.md` — 항목별 상세 스펙 (UI 구성, API, 구현 파일 목록)

TODO 항목을 구현 완료한 경우:
1. 해당 `todo-NNN_*.md` 파일의 상태를 `✅ 완료`로 변경
2. `TODO.md` 인덱스에서 해당 행을 "완료 항목" 표로 이동
3. `docs/work_list/YYYY-MM-DD_작업내용.md` 작업이력 파일 생성

---

## 아키텍처 문서 (작업 전 참조)

icon 관련 기능 추가·수정 작업 시 아래 문서를 먼저 읽어 컨텍스트를 파악한다.

| 문서 | 내용 | 참조 상황 |
|---|---|---|
| `docs/architecture/overview.md` | 전체 시스템 구조, 프로젝트 구성, 기술스택 | 모든 작업 시작 전 |
| `docs/architecture/pipeline.md` | 7단계 탐지 파이프라인 (PREP-1~5, DET-1~2, SYNC-1) | 파이프라인·엔진·시뮬레이션 작업 |
| `docs/architecture/domain-concepts.md` | 핵심 도메인 개념 (센서/룰/시나리오/엔티티/위험수준) | 탐지 정책 관련 작업 |
| `docs/architecture/screens.md` | 전체 화면 기능 명세 (19개 화면) | 프론트엔드 기능 추가·수정 |
| `docs/architecture/db-tables.md` | DB 테이블 목록 및 주요 컬럼 (44개 테이블) | DB 쿼리·엔티티·마이그레이션 작업 |

---

## 프로젝트 구성

| 프로젝트 | 역할 | 포트 |
|---|---|---|
| `icon-agent/` | 외부 시스템 데이터 수집 에이전트 (Java 17, Gradle) | 8080 (Health) |
| `icon-backend/` | 탐지 엔진 + REST API (Spring Boot 3.2.2, PostgreSQL) | 11100 |
| `icon-frontend/` | 관리 웹 UI (Next.js 15, React 19, TypeScript) | 5160 |

**DB**: PostgreSQL 18, DB명 `icon`, `jdbc:postgresql://localhost:5432/icon`  
**Swagger**: `http://localhost:11100/swagger-ui.html`

---

## 도메인 핵심 규칙

- **센서 ID**: `S_` 접두사 필수, 대문자/숫자/언더스코어만 허용, 생성 후 변경 불가
- **룰 ID**: `AGG_` 접두사 필수
- **도메인 ID**: 대문자/숫자/언더스코어만 허용, 생성 후 변경 불가
- **탐지 정책 vs 탐지 결과 테이블 구분**:
  - 정의: `sensors`, `rules`, `scenarios`, `scenario_rules`
  - 결과: `detect_sensors`, `detect_rules`, `detect_scenarios`
- **파이프라인 PREP-1 수집 테이블**: `landing_records` (`landing_raw_records` 아님)

---

## 백엔드 멀티모듈 구조 (icon-backend)

```
icon-common/      # 순수 Java 공통 도메인 (Spring 없음)
icon-entity/      # JPA 엔티티 라이브러리
icon-engine/      # 실시간 탐지 엔진 (Hexagonal)
icon-rpc-server/  # WebSocket RPC 수신 (에이전트 연결)
icon-api/         # REST API 실행 JAR (:11100)
```

**아키텍처 패턴 (Hexagonal)**:
```
adapter/in/web/          → @RestController
adapter/out/persistence/ → JPA Repository
application/service/     → @Service (비즈니스 로직)
dto/                     → 요청/응답 DTO
```

**빌드**: `./gradlew :icon-api:bootJar`  
**실행**: `./icon.sh start|stop|status`

---

## 프론트엔드 주요 라이브러리 (icon-frontend)

- 전역 상태: `Jotai` / 서버 상태: `TanStack Query`
- 그래프: `ReactFlow` (플로우), `Cytoscape` (도메인 관계)
- 차트: `Recharts`
- Path alias: `@/*` → `src/*`

**개발 서버**: `npm run dev` (Turbopack, :5160)

---

## 아키텍처 문서 업데이트 규칙

기능 추가·수정 작업 완료 후 변경 내용이 아래에 해당하면 관련 문서를 반드시 업데이트한다.

| 변경 내용 | 업데이트 대상 문서 |
|---|---|
| 새 화면 추가 / 기존 화면 기능 변경 | `docs/architecture/screens.md` |
| 새 테이블 추가 / 컬럼 변경 | `docs/architecture/db-tables.md` |
| 파이프라인 단계 추가·변경 | `docs/architecture/pipeline.md` |
| 도메인 개념 추가·변경 (센서/룰/시나리오 등) | `docs/architecture/domain-concepts.md` |
| 프로젝트 구성·기술스택·포트 변경 | `docs/architecture/overview.md` |

**업데이트 원칙:**
- 미개발 항목이 개발 완료되면 `⚠️ 미개발` 표시 제거 후 내용 추가
- 사용안함 항목이 재활성화되면 `⛔ 사용안함` 표시 제거
- 새로 확인된 DB 테이블명·컬럼은 실제 DB 기준으로 기재 (추정 금지)

---

## DB 버전 관리 규칙

### 버전 체계

| 구분 | 버전 | 파일 | 설명 |
|---|---|---|---|
| 기준선 (baseline) | **1.0.0** | `db/init.sql` | 전체 스키마 초기화 — 신규 DB 생성 시 자동 적용 |
| 증분 변경 | **1.x.x** | `db/migrations/V1_0_1__desc.sql` | 기존 DB에 증분 적용 |

### 파일 명명 규칙

```
db/migrations/V{major}_{minor}_{patch}__{설명}.sql

예시:
  V1_0_1__add_user_role_column.sql
  V1_0_2__create_notification_table.sql
  V1_1_0__add_detection_context_table.sql
```

- 버전 자리수: major(1자리 이상).minor(0~9).patch(0~9)
- 점(`.`) → 언더스코어(`_`)로 표기 (파일명에 점 사용 금지)
- 설명은 영문 소문자 + 언더스코어
- `icon_migrations` 테이블에 `1.0.1`, `1.0.2` 형태로 기록됨

### 동작 방식

```
./icon.sh start
  ├─ DB 없음 → db/init.sql 적용 (v1.0.0) → icon_migrations에 1.0.0 기록
  └─ DB 있음 → db/migrations/V*.sql 순서 적용 (미적용 버전만)
```

### DB 변경 작업 절차

1. `db/migrations/V1_0_x__설명.sql` 파일 작성
2. `docs/architecture/db-tables.md` 업데이트
3. `./deploy.sh backend vm` 으로 배포 (마이그레이션 파일 자동 전송)
4. VM에서 `./icon.sh start` — 자동으로 미적용 마이그레이션만 실행

### 주의사항

- `db/init.sql`은 직접 수정하지 않는다 — 새 테이블은 반드시 마이그레이션 파일로 추가
- 한 번 배포된 마이그레이션 파일의 내용을 수정하지 않는다 (체크섬 불일치 → 재적용 불가)
- 마이그레이션 파일은 반드시 `IF NOT EXISTS` / `ADD COLUMN IF NOT EXISTS` 등 멱등성 보장

---

## 코드 주석 규칙

코드를 수정하거나 추가할 때는 반드시 변경 내용에 주석을 달아야 한다.

**주석 형태:**
```
// [2026-03-13] 변경내용 설명
```

**규칙:**
- 모든 코드 변경 전에 해당 변경 내용을 나타내는 주석을 추가한다.
- 날짜는 `[YYYY-MM-DD]` 형식을 사용한다.
- 주석은 변경된 코드 바로 위에 위치한다.
- 언어에 맞는 주석 문법을 사용한다 (Java/JS: `//`, XML: `<!-- -->`, Properties: `#`).

---

## 미개발 / 사용안함 항목

- `AI 모니터링` — 미개발 (2026~2027 로드맵)
- `실행이력` (`/detections/executions`) — 사용안함, 트랜잭션 추적으로 대체
