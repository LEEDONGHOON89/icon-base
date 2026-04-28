# ICON 프로젝트 작업 이력 관리

> 이 파일은 `docs/work_list/` 디렉토리에 있는 작업 이력 파일들의 목록을 관리합니다.
> 작업 완료 후 반드시 해당 날짜의 작업 파일을 생성하고 이 목록에 추가하세요.

---

## 작업 이력 파일 목록

| 작업일 | 파일명 | 작업 내용 요약 |
|---|---|---|
| 2026-04-17 | [2026-04-17_시스템초기화및인프라개선.md](2026-04-17_시스템초기화및인프라개선.md) | icon.sh/deploy.sh 서버 배포 환경 구축, CORS·API URL·DB 마이그레이션 초기 셋업 |
| 2026-04-20 | [2026-04-20_DB마이그레이션통합및버전관리체계도입.md](2026-04-20_DB마이그레이션통합및버전관리체계도입.md) | V001~V006 통합 init.sql 생성, DB v1.x.x 버전 관리 체계 도입, 컬럼 길이 오류 수정 |
| 2026-04-20 | [2026-04-20_표준필드CRUD기능추가.md](2026-04-20_표준필드CRUD기능추가.md) | 표준 필드 화면 읽기전용 → CRUD (추가/수정/삭제) 기능 추가, 백엔드 POST/PUT/DELETE 엔드포인트 추가 |
| 2026-04-20 | [2026-04-20_파서기능구현.md](2026-04-20_파서기능구현.md) | 파서 관리 기능 추가 — DELIMITER/FIXED_WIDTH/REGEX 파서 CRUD, 엔진 파서 적용, 원본 필드 파서 연동 |
| 2026-04-21 | [2026-04-21_파서개선및DB폴링버그수정.md](2026-04-21_파서개선및DB폴링버그수정.md) | 파서 표준필드 제거, JSONB 바인딩 수정, 모달 스크롤 수정, _line 필드 제거, DB 폴링 TIMESTAMP 바인딩 버그 수정, TODO 목록 신규 작성 |
| 2026-04-21 | [2026-04-21_에이전트스냅샷아키텍처리팩토링.md](2026-04-21_에이전트스냅샷아키텍처리팩토링.md) | Phase 1~3 에이전트 스냅샷 아키텍처 리팩토링 — AgentSnapshotService, 폴링 설정 컬럼 추가, 수집기 스냅샷 UI |
| 2026-04-21 | [2026-04-21_JVM최적화및감사로그분리.md](2026-04-21_JVM최적화및감사로그분리.md) | JVM 옵션 경량화 (Xmx512m, MaxDirectMemorySize 제거), AGENT_JAVA_OPTS 추가, AuditLogger 신규 생성 및 통합 |
| 2026-04-21 | [2026-04-21_AgentId중복방지.md](2026-04-21_AgentId중복방지.md) | agentId UUID 자동 영속화(data/agent.id), 서버 중복 감지 시 HANDSHAKE_NACK 반환 및 연결 거부 |
| 2026-04-21 | [2026-04-21_agent-targets-full-settings-persistence.md](2026-04-21_agent-targets-full-settings-persistence.md) | targets.json 전체 설정 영속화 — TLS/배치/큐/스풀/속도제한, CONFIG_UPDATE 동기화, target show CLI 추가 |
| 2026-04-22 | [2026-04-22_에이전트개선및수집기동기화버그수정.md](2026-04-22_에이전트개선및수집기동기화버그수정.md) | 에이전트 해제 시 수집기 미제거 버그 수정, 스풀 크기 제한·Rate Limiting 즉시 적용, 감사 로그 분리, JVM 경량화, TODO 004~009 등록 |
| 2026-04-22 | [2026-04-22_수집기초기화기능추가.md](2026-04-22_수집기초기화기능추가.md) | 수집기 last_position 초기화 기능 추가 — DATABASE/FILE_SYSTEM_REALTIME/FILE_SYSTEM 5가지 케이스, 에이전트 COLLECTOR_RESET 메시지, Frontend 초기화 버튼 |
| 2026-04-22 | [2026-04-22_배치기본값튜닝및maxLinesPerPoll설정UI추가.md](2026-04-22_배치기본값튜닝및maxLinesPerPoll설정UI추가.md) | 배치 설정 튜닝 가이드 기본값 적용 (maxBatchMs 5s, maxBatchBytes 512KB, maxBatchesPerSecond 10), FILE_SYSTEM_REALTIME/DATABASE 수집 설정 UI에 maxLinesPerPoll 등 추가 |
| 2026-04-22 | [2026-04-22_폴링간격초단위통합.md](2026-04-22_폴링간격초단위통합.md) | FILE_SYSTEM_REALTIME/DATABASE 폴링 간격을 초(seconds) 단위로 통합 — scanIntervalMinutes 분→초 재해석, AgentSnapshotService fallback 수정, ConfigForm 단일 필드로 통합 |
| 2026-04-22 | [2026-04-22_백엔드직접수집maxLinesPerPoll_maxRecordBytes적용.md](2026-04-22_백엔드직접수집maxLinesPerPoll_maxRecordBytes적용.md) | 백엔드 직접 수집 시 maxLinesPerPoll/maxRecordBytes 미적용 버그 수정 — 엔진 엔티티 필드 추가, FileSystemRealtimeService/DatabaseService 제한 로직 적용 |
| 2026-04-22 | [2026-04-22_미사용에이전트수집기테이블삭제.md](2026-04-22_미사용에이전트수집기테이블삭제.md) | agent_collector_configs/file/jdbc 테이블 및 관련 Java 클래스·프론트엔드 함수 전체 삭제 (AgentSnapshotService로 대체됨) |
| 2026-04-22 | [2026-04-22_TODO001_002_수집원본_매핑결과_조회화면추가.md](2026-04-22_TODO001_002_수집원본_매핑결과_조회화면추가.md) | TODO-001 수집원본/TODO-002 매핑결과 조회 화면 추가, 파서 프론트엔드 재설계 완성, DB 인덱스 추가 |
| 2026-04-24 | [2026-04-24_탭기능및엔티티필드관리및버그수정.md](2026-04-24_탭기능및엔티티필드관리및버그수정.md) | 헤더 탭 기능 구현, 엔티티 필드 CRUD (V1_0_14), DestinationType 오류 수정 (V1_0_15), 시나리오 폼 버그 2건, 데이터소스 새 창→페이지 전환, 상세 페이지 탭 제외 처리 |
| 2026-04-24 | [2026-04-24_탭Keep-alive및시나리오수정버튼수정.md](2026-04-24_탭Keep-alive및시나리오수정버튼수정.md) | 탭 Keep-alive 구현 (CSS display:none 페이지 캐시), 시나리오 수정 버튼 새 창→페이지 전환 |

---

## 작업 파일 생성 규칙

### 파일명 형식
```
YYYY-MM-DD_작업내용요약.md
```

### 필수 항목
- **변경일자**: `YYYY-MM-DD`
- **변경내용**: 한 줄 요약
- **변경상세**: 파일별 변경 내역, 변경 이유, 관련 커밋

### 작업 절차
1. 작업 완료 후 `docs/work_list/YYYY-MM-DD_작업내용.md` 파일 생성
2. 이 `INDEX.md`의 목록에 행 추가
3. `git add docs/work_list/` 후 커밋

---

## Claude Code 참조 안내

다른 작업자의 Claude Code에서 이 디렉토리를 참조할 때:
```
docs/work_list/INDEX.md        ← 전체 작업 이력 목록
docs/work_list/YYYY-MM-DD_*.md ← 날짜별 작업 상세
```
작업 시작 전 최신 작업 이력을 확인하여 충돌 및 중복 작업을 예방하세요.
