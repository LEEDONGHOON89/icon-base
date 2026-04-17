# 에이전트 모니터링 가이드

> 작성일: 2026-02-25  
> 관련 파일: `src/main/java/com/icon/agent/monitor/AgentMonitor.java`

---

## 개요

에이전트 기동 시 자동으로 시스템 자원 및 에이전트 내부 상태를 주기적으로 수집하여
`logs/agent-metrics.log` 파일에 **JSON Lines** 형식(1줄 = 1 스냅샷)으로 저장한다.

이후 `FileCollector`로 해당 로그 파일을 수집 대상에 추가하면
**내부통제시스템 서버에서 실시간 모니터링** 연동이 가능하다.

---

## 설정 (config.yaml)

```yaml
monitoring:
  enabled: true
  intervalMs: 30000           # 수집 주기 (기본 30초)
  logPath: logs/agent-metrics.log
  spoolRootPath: data         # 스풀 디렉토리 디스크 사용량 계산 기준
```

| 설정 | 기본값 | 설명 |
|---|---|---|
| `enabled` | `true` | `false`로 설정 시 모니터링 비활성화 |
| `intervalMs` | `30000` | 메트릭 수집 주기 (밀리초) |
| `logPath` | `logs/agent-metrics.log` | 메트릭 로그 저장 경로 |
| `spoolRootPath` | `data` | 스풀 디스크 사용량 계산 대상 디렉토리 |

---

## 로그 파일

- **경로**: `logs/agent-metrics.log`
- **형식**: JSON Lines (줄바꿈으로 구분된 JSON 객체)
- **롤링**: 일별 + 10MB 초과 시 분할, 최대 90일 보존, 총 200MB 상한
- **출력 예시** (실제 운영 데이터 기준):

```json
{
  "timestamp": "2026-02-25T15:29:29.581",
  "uptime_sec": 32,
  "cpu_percent": 0.2,
  "heap_used_mb": 15,
  "heap_max_mb": 8128,
  "heap_percent": 0,
  "non_heap_used_mb": 29,
  "gc_count": 2,
  "gc_time_ms": 15,
  "gc_delta_count": 0,
  "gc_delta_time_ms": 0,
  "threads": {
    "total": 26,
    "running": 8,
    "waiting": 4,
    "timed_waiting": 14,
    "blocked": 0,
    "other": 0,
    "peak": 26
  },
  "sys_memory_used_mb": 16139,
  "sys_memory_total_mb": 32499,
  "sys_memory_percent": 49,
  "open_fd_count": -1,
  "spool_disk_used_mb": 0,
  "targets": [
    { "id": "systemA", "connected": true, "queue_depth": 0, "spool_depth": 0 },
    { "id": "systemB", "connected": true, "queue_depth": 0, "spool_depth": 0 }
  ]
}
```

---

## 수집 지표 상세

### JVM 지표

| 필드 | 단위 | 설명 | 이상 징후 기준 |
|---|---|---|---|
| `cpu_percent` | % | JVM 프로세스 CPU 사용률 | 지속적으로 50% 이상 |
| `heap_used_mb` | MB | Heap 사용 중인 메모리 | - |
| `heap_max_mb` | MB | Heap 최대 메모리 | - |
| `heap_percent` | % | Heap 사용률 | 80% 이상 지속 시 OOM 위험 |
| `non_heap_used_mb` | MB | Non-Heap (메타스페이스, JIT 코드 캐시) | 지속 증가 시 클래스 누수 의심 |
| `gc_count` | 횟수 | GC 누적 횟수 | - |
| `gc_time_ms` | ms | GC 누적 소요 시간 | - |
| `gc_delta_count` | 횟수 | 이번 주기 GC 횟수 | 수집 주기(30초)당 5회 이상 |
| `gc_delta_time_ms` | ms | 이번 주기 GC 소요 시간 (Stop-The-World) | 주기당 1,000ms 이상 |
| `uptime_sec` | 초 | JVM 가동 시간 | 비정상적으로 짧으면 재시작 발생 |

### Thread 지표

| 필드 | 설명 | 이상 징후 기준 |
|---|---|---|
| `threads.total` | 전체 활성 스레드 수 | 재시작 없이 지속 증가 → 스레드 누수 |
| `threads.running` | RUNNABLE: CPU 점유 실행 중 | 과도하게 높으면 CPU 병목 |
| `threads.waiting` | WAITING: Object.wait() 등 무기한 대기 | 급증 시 데드락 의심 |
| `threads.timed_waiting` | TIMED_WAITING: sleep/wait(ms) 시간 제한 대기 | 정상 범위 (idle 스레드 포함) |
| `threads.blocked` | BLOCKED: 모니터 락 경합 대기 | **0 이상 지속 → 락 경합/성능 저하** |
| `threads.peak` | JVM 기동 이후 최고 동시 스레드 수 | 재시작 없이 계속 증가 → 스레드 누수 |

### OS 지표

| 필드 | 단위 | 설명 | 이상 징후 기준 |
|---|---|---|---|
| `sys_memory_used_mb` | MB | 시스템 전체 메모리 사용량 | - |
| `sys_memory_total_mb` | MB | 시스템 전체 메모리 | - |
| `sys_memory_percent` | % | 시스템 메모리 사용률 | 90% 이상 지속 시 위험 |
| `open_fd_count` | 개 | Open File Descriptor 수 (Linux 전용) | 급증 시 파일 핸들 누수 의심 |
| `spool_disk_used_mb` | MB | 스풀 디렉토리 전체 디스크 사용량 | 지속 증가 시 서버 연결 장애 |

> `open_fd_count`는 **Linux/macOS 전용**이며, Windows에서는 `-1`로 표시된다.

### 에이전트 내부 상태 (targets 배열)

| 필드 | 설명 | 이상 징후 기준 |
|---|---|---|
| `connected` | WebSocket 서버 연결 상태 | `false` → 서버 단절 |
| `queue_depth` | RecordQueue 내 미전송 레코드 수 | 지속 증가 → 전송 병목 (OOM 전조) |
| `spool_depth` | 스풀 디렉토리 내 미전송 파일 수 | 지속 증가 → 서버 연결 장애 누적 |

---

## 실제 운영 관찰값 (2026-02-25 기준)

| 지표 | 기동 직후 | 안정 시 | 비고 |
|---|---|---|---|
| `cpu_percent` | 13.2% | 0.0~0.2% | 기동 시 초기화 비용 이후 급감 |
| `heap_used_mb` | 27 MB | 11~23 MB | GC 후 정리, 매우 안정적 |
| `heap_percent` | 0% | 0% | 최대 8GB Heap 대비 극히 미미 |
| `threads.total` | 26 | 18~22 | 재연결 스레드 종료 후 수렴 |
| `threads.blocked` | 0 | 0 | 락 경합 없음 (정상) |
| `gc_delta_count` | 1~2 | 0 | 안정 시 GC 거의 없음 |
| `sys_memory_percent` | 49% | 49% | 시스템 전체 메모리 안정 |

---

## 내부통제시스템 서버 연동 방법

에이전트 모니터링 로그를 FileCollector 수집 대상으로 추가하면 서버에서 실시간 확인이 가능하다.

```yaml
# config.yaml 수집기 목록에 추가
collectors:
  - id: agent-self-monitoring-001
    type: "FILE"
    name: "[에이전트] 자체 모니터링"
    enabled: true
    path: logs
    file: agent-metrics.log
    format: LOG          # JSON Lines → LOG 형식으로 그대로 전송
    pollIntervalMs: 30000
    maxLinesPerPoll: 100
    maxRecordBytes: 0    # JSON 한 줄이므로 크기 제한 불필요
```

---

## 관련 파일

| 파일 | 설명 |
|---|---|
| `src/main/java/com/icon/agent/monitor/AgentMonitor.java` | 메트릭 수집 및 JSON 로깅 구현 |
| `src/main/java/com/icon/agent/monitor/MonitoringConfig.java` | 모니터링 설정 클래스 |
| `src/main/resources/logback.xml` | `agent.metrics` 전용 로그 appender 설정 |
| `config/config.yaml` | `monitoring:` 섹션 설정 |
| `logs/agent-metrics.log` | 실시간 메트릭 출력 파일 (JSON Lines) |
