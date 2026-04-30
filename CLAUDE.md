# ICON 프로젝트 작업 지침

---

## 작업 저장소 규칙

**모든 작업은 메인 저장소(`D:\sources\icon-base`)에서 직접 수행한다. worktree에서 작업하지 않는다.**

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

## 작업 이력 및 TODO 관리 규칙

작업 완료 후 반드시 아래 파일들을 업데이트한다.

### 작업 이력 (`docs/work_list/`)

| 시점 | 대상 파일 | 작업 내용 |
|---|---|---|
| 작업 완료 후 | `docs/work_list/YYYY-MM-DD_작업내용요약.md` (신규 생성) | 변경 파일 목록, 변경 이유, 주요 내용 상세 기록 |
| 작업 완료 후 | `docs/work_list/INDEX.md` | 위 파일 행 추가 (작업일 / 파일명 / 작업 내용 요약) |

**작업이력 파일 형식**: `YYYY-MM-DD_작업내용요약.md`  
**INDEX.md 위치**: `docs/work_list/INDEX.md` — 전체 이력 목록 관리

### TODO 목록 (`docs/todo_list/TODO.md`)

| 시점 | 작업 |
|---|---|
| 새 작업 항목 발생 시 | `TODO.md` TODO 목록에 행 추가 (번호 순번 부여, 상태 🔲, 상세 파일 있으면 링크) |
| TODO 항목 완료 시 | 해당 행 상태를 ✅로 변경 후 "완료 항목" 표로 이동, 완료일 기재 |
| TODO 항목 보류 시 | 상태를 ⏸로 변경 |

**번호 규칙**: 순번 부여, 삭제해도 번호 재사용 금지  
**TODO.md 위치**: `docs/todo_list/TODO.md`

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

## Git 브런치 규칙

> **이 규칙은 절대 예외 없이 반드시 준수한다.**

### 브런치 전략

| 브런치 | 용도 | push 가능 여부 |
|---|---|---|
| `leedh` | 작업 브런치 — 모든 개발·수정 작업 | ✅ 항상 허용 |
| `main` | 배포 브런치 — 머지 요청 시에만 변경 | ❌ 직접 push 절대 금지 |

### 일반 작업 규칙

- 모든 작업은 `leedh` 브런치에서 수행하고 `leedh`에만 push한다
- push 전 반드시 현재 브런치가 `leedh`인지 확인한다

```bash
# push 전 브런치 확인
git branch --show-current   # 반드시 leedh 출력 확인

# push 실행
git push origin leedh
```

### main 브런치 머지 규칙

- **사용자가 명시적으로 머지 요청을 한 경우에만** `leedh → main` 머지를 진행한다
- 머지 절차:
```bash
# 1. leedh 최신 상태 확인
git checkout leedh
git status

# 2. main 브런치로 전환
git checkout main

# 3. leedh → main 머지
git merge leedh --no-ff -m "Merge branch 'leedh' into main"

# 4. main push
git push origin main

# 5. 작업 브런치로 복귀
git checkout leedh
```

### 절대 실행 금지

```bash
# 머지 요청 없이 아래 명령은 절대 실행하지 않는다
git push origin main
git push --force origin main
git push -f origin main
```

---

## 미개발 / 사용안함 항목

- `AI 모니터링` — 미개발 (2026~2027 로드맵)
- `실행이력` (`/detections/executions`) — 사용안함, 트랜잭션 추적으로 대체
