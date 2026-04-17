# ICON 내부통제시스템 - 아키텍처 개요

> 최초 작성: 2026-04-16 (소스 분석 + 산출물 문서 기반)

---

## 1. 시스템 개요

**ICON(Intelligent CONtrol)**은 금융기관 내부통제를 위한 실시간 이상거래 탐지 및 조치 관리 시스템이다.

- **탐지 구조**: 센서(S_) → 룰/집계(AGG_) → 시나리오 3계층 파이프라인
- **위험수준 4단계**: BLOCK(즉시차단) / REVIEW(담당자검토) / INTENSIVE(집중모니터링) / MONITOR(모니터링)
- **사용자 역할**: 보안담당자, 준법감시인, 시스템관리자
- **기술 기반**: Spring Boot 3.2.2 + PostgreSQL 14+ + Next.js 15

---

## 2. 프로젝트 구성

```
icon/
├── icon-agent/       # 외부 시스템 데이터 수집 에이전트 (Java, OS 독립)
├── icon-backend/     # 탐지 엔진 + REST API (Spring Boot 멀티모듈)
└── icon-frontend/    # 관리 웹 UI (Next.js)
```

| 프로젝트 | 역할 | 포트 | 언어/프레임워크 |
|---|---|---|---|
| icon-agent | 외부 DB/파일 데이터 수집 → 백엔드 전송 | 8080 (Health) | Java 17, Gradle |
| icon-backend | 탐지 엔진 + REST API | 11100 | Java 17, Spring Boot 3.2.2 |
| icon-frontend | 관리 웹 UI | 5160 | Next.js 15.3.5, React 19, TypeScript |

---

## 3. 시스템 구성도

```
┌─────────────────────────────────────────────────────────────┐
│                       외부 시스템                            │
│  REST API / DB(MySQL·Oracle·PostgreSQL) / File / Kafka      │
└──────────────────────┬──────────────────────────────────────┘
                       │ 데이터 수집 (4가지 방식)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    icon-agent                               │
│  - 파일 기반 오프셋 관리 (재기동 후 이어받기)                │
│  - 디스크 스풀링 (네트워크 장애 시 유실 방지)               │
│  - ACK 기반 At-least-once 전달 보장                         │
│  - Health API: HTTP :8080                                   │
└──────────────────────┬──────────────────────────────────────┘
                       │ WebSocket RPC  ws://{host}:11100/rpc
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    icon-backend                             │
│                                                             │
│  ┌─────────────────┐  ┌──────────────────────────────────┐ │
│  │  icon-rpc-server │  │          icon-engine             │ │
│  │  (WebSocket RPC) │──│  7단계 탐지 파이프라인            │ │
│  └─────────────────┘  │  PREP-1~5 / DET-1~2 / SYNC-1    │ │
│                       └──────────────────────────────────┘ │
│  ┌──────────────────┐                                      │
│  │    icon-api      │  REST API / JWT 인증                  │
│  │  :11100          │  Swagger: /swagger-ui.html            │
│  └──────────────────┘                                      │
│                                                             │
│  DB: PostgreSQL 14+  jdbc:postgresql://localhost:5432/icon  │
└──────────────────────┬──────────────────────────────────────┘
                       │ REST HTTP (API_URL: http://localhost:11100)
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   icon-frontend                             │
│  Next.js 15 App Router / Tailwind CSS v4                   │
│  Jotai + TanStack Query / ReactFlow / Recharts              │
│  URL: http://localhost:5160                                 │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. 탐지 파이프라인

상세 내용: [pipeline.md](pipeline.md)

```
[수집/변환]  PREP-1(Extract) → PREP-2(Load) → PREP-3(Transform)
              ↓
[스트리밍]   DET-1(Stream) → event_stream 테이블
              ↓
[탐지]       DET-2-1(Sensor Eval, S_ 조건 평가)
              ↓
             DET-2-2(Rule/AGG Eval, 시간창 집계)
              ↓
             DET-2-3(Scenario Eval, AND/OR 조합 판정)
              ↓
[위험분류]   BLOCK / REVIEW / INTENSIVE / MONITOR
              ↓
[엔티티갱신] SYNC-1(Entity Attribute Update)

[비동기 관계 분석]
PREP-4(Enrich) → PREP-5-A(Explicit 관계 추출) → PREP-5-B(Pattern Discovery)
```

---

## 5. 도메인 개념

상세 내용: [domain-concepts.md](domain-concepts.md)

| 개념 | 설명 | ID 규칙 |
|---|---|---|
| **데이터소스** | 원본 데이터 연결 (DB/API/File/Kafka) | - |
| **표준 필드** | 이벤트 속성 정의 (읽기 전용) | `entity_id`, `transaction_amount` 등 |
| **엔티티 필드** | 엔티티 프로파일 속성 (읽기 전용) | `risk_score`, `is_blocked` 등 |
| **센서(Sensor)** | 단일 이벤트 조건 필터 | `S_` 접두사 필수 |
| **룰(Rule)** | 시간창 기반 집계 조건 | `AGG_` 접두사 필수 |
| **시나리오** | 룰들의 AND/OR 조합 → 탐지 판정 | unique 이름 |
| **엔티티** | 정적 프로파일 (CUSTOMER/ACCOUNT/DEVICE/EMPLOYEE/AUTHENTICATION) | - |
| **도메인** | 시나리오 그룹핑 단위 (대시보드 통계) | 대문자/숫자/언더스코어 |

---

## 6. 화면 목록

상세 내용: [screens.md](screens.md)

| 메뉴 | 화면 | 상태 |
|---|---|---|
| - | 대시보드 | ✅ |
| 탐지정책관리 | 센서 관리 | ✅ |
| 탐지정책관리 | 룰 관리 | ✅ |
| 탐지정책관리 | 시나리오 관리 | ✅ |
| 탐지정책관리 | 도메인 설정 | ✅ |
| 탐지정책관리 | 엔티티 관계 설정 | ✅ |
| 탐지정책관리 | AI 서포트 | ✅ |
| 탐지모니터링 | 시나리오 탐지 | ✅ |
| 탐지모니터링 | 룰 탐지 | ✅ |
| 탐지모니터링 | 탐지 조치 | ✅ |
| 탐지모니터링 | 엔티티 행적 | ✅ |
| 탐지모니터링 | 시뮬레이션 | ✅ |
| 탐지모니터링 | 트랜잭션 추적 | ✅ |
| 탐지모니터링 | AI 모니터링 | ⚠️ 미개발 |
| 감사 | 변경이력 | ✅ |
| 시스템 설정 | 데이터소스 | ✅ |
| 시스템 설정 | 표준 필드 | ✅ (읽기전용) |
| 시스템 설정 | 엔티티 필드 | ✅ (읽기전용) |
| 탐지모니터링 | 실행이력 | ⛔ 사용안함 |

---

## 7. icon-agent 상세

### 7.1 역할
외부 시스템(DB, 파일 등)에서 데이터를 수집하여 icon-backend로 전송하는 경량 에이전트. Windows / Linux 모두 지원.

### 7.2 지원 데이터소스
- MySQL (`mysql-connector-j:8.3.0`)
- Oracle (`ojdbc11:23.4.0`)
- PostgreSQL (`postgresql:42.7.3`)
- 파일 (CSV 등)

### 7.3 핵심 컴포넌트

```
CollectorAgent.java          # 메인 진입점
├── target/                  # 멀티 타겟 관리 (1 Agent → N 백엔드)
├── collector/               # 데이터 수집 (DB, 파일)
├── queue/                   # 인메모리 레코드 큐 (queueCapacity: 10000)
├── batch/                   # 배치 빌더 (maxSize=102, maxMs=550)
├── rpc/                     # WebSocket RPC 클라이언트
├── spool/                   # 디스크 스풀 (네트워크 장애 대응)
├── store/                   # 오프셋/위치 저장소 (파일 기반)
├── health/                  # HTTP Health Check (:8080)
├── monitor/                 # 모니터링 (intervalMs: 30000)
├── tls/                     # TLS/mTLS 지원
└── shutdown/                # Graceful Shutdown
```

### 7.4 설정 파일 (`config.yaml`)
```yaml
agent:
  healthPort: 8080
  agentId: "agent1"
targets:
  - id: "icon-backend"
    rpc:
      endpoint: "ws://localhost:11100/rpc"
    queueCapacity: 10000
    maxBatchSize: 102
    maxBatchMs: 550
monitoring:
  enabled: true
  intervalMs: 30000
  spoolRootPath: "data"
```

### 7.5 빌드
```bash
cd icon-agent
./gradlew shadowJar
# 결과: build/libs/collector-agent.jar
```

---

## 8. icon-backend 상세

### 8.1 멀티모듈 구조

```
icon-backend/
├── icon-common/       # 순수 Java 공통 도메인/유틸 (Spring 미포함)
│                      # - EntityType, RiskLevel, RuleCategory
│                      # - 조건 클래스 (AmountCondition, CompositeCondition 등)
│                      # - IdGenerator, TsidGenerator
├── icon-entity/       # JPA 엔티티 라이브러리
│                      # - Auditable, DerivedRuleEntity
│                      # - ScenarioAggregateEntity 등
├── icon-engine/       # 실시간 탐지 엔진 (236 Java files)
│                      # Hexagonal Architecture
├── icon-rpc-server/   # WebSocket RPC 서버 (에이전트 수신)
│                      # - AgentRpcWebSocketHandler
└── icon-api/          # REST API 서버 (277 Java files)
                       # 실행 JAR: icon-api-0.0.1-SNAPSHOT.jar
```

### 8.2 icon-api 주요 도메인 패키지

| 패키지 | 역할 |
|---|---|
| `auth/` | JWT 인증/인가 (access 1h, refresh 7d) |
| `user/` | 사용자 관리 |
| `datasource/`, `datasourceschema/` | 데이터 소스 등록/스키마 |
| `rule/`, `rulesnapshot/` | 탐지 룰 관리 |
| `scenario/` | 시나리오 관리 |
| `aggregationrule/`, `relationrule/` | 집계·관계 규칙 |
| `detection/`, `detectaction/`, `detectionarea/` | 탐지 결과/조치/영역 |
| `engine/` | 엔진 제어 |
| `derivedfield/`, `standardfield/` | 파생·표준 필드 |
| `dataprofile/` | 데이터 프로파일 (필드 매핑) |
| `domaintype/`, `entityattribute/`, `entityfield/` | 도메인 모델 |
| `transaction/` | 거래 데이터 |
| `dashboard/` | 대시보드 통계 |
| `analytics/` | 분석 |
| `audit/` | 감사 로그 (변경이력) |
| `aichat/` | AI 서포트 채팅 |
| `agents/` | 에이전트 관리 |
| `metadata/` | 메타데이터 |
| `event/`, `eventhandling/` | 이벤트 처리 |

### 8.3 아키텍처 패턴 (Hexagonal)
```
adapter/in/web/          → REST 컨트롤러 (@RestController)
adapter/out/persistence/ → JPA 리포지토리
application/service/     → 비즈니스 로직 (@Service)
dto/                     → 요청/응답 DTO
```

### 8.4 핵심 설정 (`application.properties`)
```properties
spring.application.name=icon-api
server.port=11100
server.address=0.0.0.0
spring.datasource.url=jdbc:postgresql://localhost:5432/icon
spring.datasource.username=postgres
jwt.expiration-ms=3600000           # 1시간
jwt.refresh-expiration-ms=604800000 # 7일
```

### 8.5 빌드 및 실행
```bash
cd icon-backend
./gradlew :icon-api:bootJar
# 결과: icon-api/build/icon-api-0.0.1-SNAPSHOT.jar

# 실행 (icon.sh 사용)
./icon.sh start   # JVM: -Xmx2048m -XX:MaxDirectMemorySize=8192m -XX:+UseG1GC
./icon.sh stop
./icon.sh status
```

---

## 9. icon-frontend 상세

### 9.1 기술 스택

| 분류 | 기술 | 버전 |
|---|---|---|
| 프레임워크 | Next.js (App Router) | 15.3.5 |
| UI 라이브러리 | React | 19.0.0 |
| 언어 | TypeScript | 5.x |
| 스타일 | Tailwind CSS | 4.x |
| 전역 상태 | Jotai | 2.12.5 |
| 서버 상태 | TanStack Query | 5.82.0 |
| HTTP 클라이언트 | Axios | 1.10.0 |
| 폼 | React Hook Form | 7.60.0 |
| 그래프/플로우 | ReactFlow | 11.11.4 |
| 도메인 그래프 | Cytoscape + react-cytoscapejs | 3.33.1 |
| 차트 | Recharts | 3.5.1 |
| 드래그앤드롭 | @dnd-kit | 6.x |
| 아이콘 | @heroicons/react | 2.2.0 |
| 날짜 | dayjs | 1.11.x |
| 알림 | react-hot-toast | 2.5.2 |
| E2E 테스트 | Playwright | 1.54.1 |

### 9.2 디렉토리 구조

```
src/
├── app/           # Next.js App Router 페이지
├── atoms/         # Jotai 전역 상태
├── components/    # 재사용 컴포넌트 (common, datasource, profile, ui)
├── hooks/         # Custom React Hooks
├── lib/           # 유틸 함수
├── services/      # API 서비스 (Axios)
├── types/         # TypeScript 타입 정의
└── utils/         # 유틸 함수
```

### 9.3 개발 및 빌드
```bash
cd icon-frontend
npm install
npm run dev    # 개발 서버 (Turbopack, :5160)
npm run build  # 프로덕션 빌드
npm run start  # 프로덕션 서버 (:5160)
npm test       # Playwright E2E 테스트
```

---

## 10. 데이터베이스

| 항목 | 값 |
|---|---|
| 종류 | PostgreSQL 14+ |
| 접속 URL | `jdbc:postgresql://localhost:5432/icon` |
| DB명 | `icon` |
| 계정 | `postgres` |
| 테스트 | H2 (test 프로필) |

주요 테이블 목록: [db-tables.md](db-tables.md)

---

## 11. 코딩 규칙

- **코드 변경 주석**: `// [YYYY-MM-DD] 변경 내용` 형식으로 변경된 코드 위에 작성
- **파일 인코딩**: UTF-8 (LF)
- **들여쓰기**: Java/Kotlin = 4 spaces, TS/JS/JSON/YAML = 2 spaces
- 상세: 루트 `docs/coding-convention.md` 참조

---

## 12. 추가 문서

| 문서 | 내용 |
|---|---|
| [pipeline.md](pipeline.md) | 7단계 탐지 파이프라인 상세 |
| [domain-concepts.md](domain-concepts.md) | 도메인 개념 (센서/룰/시나리오/엔티티) 상세 |
| [screens.md](screens.md) | 전체 화면 기능 명세 |
| [db-tables.md](db-tables.md) | 주요 DB 테이블 목록 |
