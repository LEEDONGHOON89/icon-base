# 2026-04-22 배치 기본값 튜닝 및 maxLinesPerPoll 설정 UI 추가

## 변경일자
2026-04-22

## 변경내용
배치 설정 튜닝 가이드 기반 기본값 적용, 에이전트 수집기 설정 UI에 `maxLinesPerPoll`/`pollIntervalMs`/`maxRecordBytes` 필드 추가

## 변경상세

### 1. TargetConfig.java — Java 기본값 튜닝 가이드 반영
**파일**: `icon-agent/src/main/java/com/icon/agent/config/TargetConfig.java`

| 설정 | 이전 값 | 변경 값 | 이유 |
|---|---|---|---|
| `maxBatchMs` | 2,000ms | 5,000ms | 1분 폴링 환경에서 응답성·효율 균형 |
| `maxBatchBytes` | 1,048,576 (1MB) | 524,288 (512KB) | 메모리 절약, 배치 크기 예측 가능성 향상 |
| `maxBatchesPerSecond` | 0 (무제한) | 10 | 서버 부하 방지 기본값 적용 |

`maxBatchSize`(500), `queueCapacity`(10000)은 기존 값 유지.

### 2. config/config.yaml — 템플릿 기본값 업데이트
**파일**: `icon-agent/config/config.yaml`

- `maxBatchSize: 500` (이전: 102)
- `maxBatchMs: 5000` (이전: 550)
- `maxBatchBytes: 524288` (이전: 0)
- `maxBatchesPerSecond: 10` (신규 추가)
- 설명 주석 추가

### 3. ConfigForm.tsx — FILE_SYSTEM_REALTIME 에이전트 수집 설정 섹션 추가
**파일**: `icon-frontend/src/components/datasource/ConfigForm.tsx`

에이전트 선택 시 표시되는 "에이전트 수집 설정" 섹션 추가:
- **폴링 간격 (ms)** → `fs.pollIntervalMs`
- **폴 당 최대 처리 라인 수** → `fs.maxLinesPerPoll`
- **최대 레코드 크기 (bytes)** → `fs.maxRecordBytes`

기존에는 이 3개 필드가 state에 로드는 되지만 UI에 표시되지 않았음.

### 4. ConfigForm.tsx — DATABASE maxLinesPerPoll 위치 변경 및 maxRecordBytes 추가
**파일**: `icon-frontend/src/components/datasource/ConfigForm.tsx`

| 변경 내용 | 상세 |
|---|---|
| `maxLinesPerPoll` 위치 변경 | "에이전트 연결" 섹션 내부(agentId 조건부) → "적재 설정" 섹션(항상 표시) |
| `maxRecordBytes` 추가 | 에이전트 선택 시 표시되는 에이전트 폴링 설정에 추가 |
| `maxLinesPerPoll` 에이전트 조건부 제거 | 에이전트 없는 경우(백엔드 직접 폴링)에도 설정 가능 |

## 관련 파일

- `icon-agent/src/main/java/com/icon/agent/config/TargetConfig.java`
- `icon-agent/config/config.yaml`
- `icon-frontend/src/components/datasource/ConfigForm.tsx`
