# 배치 전송 vs 1건씩 전송 방식 비교 분석

> 작성일: 2026-02-25  
> 대상 파일: `src/main/java/com/icon/agent/batch/BatchBuilder.java`

---

## 현재 구조 흐름 (배치 방식)

```
파일 N행 읽기
    │
    ▼
RecordQueue (메모리 버퍼)
    │
    ▼
BatchBuilder.nextBatch()
    │  maxBatchSize=500건 OR maxBatchBytes=1MB OR maxBatchMs=2000ms
    ▼
Batch { records: [row1, row2, row3, ...] }
    │
    ▼
WebSocket.sendText(JSON) → 1회 전송
    │
    ▼
서버 ACK 1회
```

---

## 1건씩 전송 방식 흐름

```
파일 N행 읽기
    │
    ▼
RecordQueue
    │
    ▼
BatchBuilder → maxBatchSize=1
    │
    ▼
Batch { records: [row1] } → sendText() → ACK
Batch { records: [row2] } → sendText() → ACK
Batch { records: [row3] } → sendText() → ACK
    ...
```

---

## 장단점 비교표

| 항목 | 현재 (배치) | 1건씩 전송 |
|---|---|---|
| **네트워크 오버헤드** | ✅ 낮음 — HTTP/WS 프레임 헤더 1회 | ❌ 높음 — 헤더 N회 (3행이면 3배) |
| **서버 처리 부하** | ✅ 낮음 — ACK 1회, DB 저장 1 트랜잭션 가능 | ❌ 높음 — ACK N회, 트랜잭션 N회 |
| **처리량(Throughput)** | ✅ 높음 — 파이프라이닝 시 동시 4배치(4000건) 처리 | ❌ 낮음 — 파이프라이닝 시 동시 4건만 처리 |
| **실패 재전송 범위** | ❌ 배치 전체 재전송 (500건 중 1건 실패 → 500건 재전송) | ✅ 1건만 재전송 (정밀한 오류 격리) |
| **스풀 파일 크기** | ❌ 실패 시 배치 단위로 저장 (최대 1MB) | ✅ 실패 시 1건만 저장 |
| **레이턴시** | ❌ maxBatchMs(2초) 대기 가능 | ✅ 즉시 전송 |
| **중복 전송 위험** | ❌ 재전송 시 배치 내 일부 중복 가능 | ✅ 1건 단위 정밀 중복 방지 |
| **구현 복잡도** | ✅ 현재 구조 유지 | ✅ maxBatchSize=1 설정만으로 가능 |
| **메모리 사용량** | ❌ 배치 크기만큼 메모리 점유 | ✅ 레코드 1건만 점유 |
| **서버 순서 보장** | ✅ 배치 내 순서 보장 | ✅ 1건이므로 순서 문제 없음 |

---

## 실무 적합성

### 배치 방식이 유리한 경우
- 대용량 로그 수집 (초당 수백~수천 건)
- 서버 DB 저장 시 bulk insert 활용 가능한 환경
- 네트워크 비용이 민감한 환경 (WAN, 클라우드)

### 1건씩 전송이 유리한 경우
- 건당 처리 결과를 즉시 확인해야 하는 경우 (트랜잭션 per-record)
- 데이터 건수가 매우 적고 레이턴시가 중요한 경우
- 각 레코드 실패를 독립적으로 추적해야 하는 경우

---

## 1건씩 전송으로 변경하는 방법

코드 변경 없이 **설정만으로** 가능합니다.

### config.yaml 수정

```yaml
targets:
  - id: systemA
    maxBatchSize: 1       # 1건씩 배치 조립
    maxBatchMs: 0         # 대기 없이 즉시 전송
    maxBatchBytes: 0      # 바이트 제한 비활성화
```

### TargetContext.java 직접 수정 시

```java
batchBuilder = new BatchBuilder(
    targetId, queue,
    1,      // maxBatchSize = 1건
    0,      // maxBatchMs = 대기 없음
    0       // maxBatchBytes = 제한 없음
);
```

---

## 현재 프로젝트 설정값 (config.yaml 기준)

| 설정 | 값 | 설명 |
|---|---|---|
| `maxBatchSize` | 500 | 배치당 최대 레코드 수 |
| `maxBatchMs` | 2000 | 배치 최대 대기 시간 (2초) |
| `maxBatchBytes` | 1048576 | 배치 최대 바이트 (1MB) |

---

## 결론

현재 시스템(`maxBatchSize=500, maxBatchMs=2000`)은 **일반적인 로그 수집 에이전트에 적합한 표준 설정**입니다.

1건씩 전송은 데이터가 소량일 때만 의미 있으며, 실운영에서 초당 수백 건이 발생하면 서버 부하와 네트워크 오버헤드가 급증합니다.

**레이턴시가 중요하다면** `maxBatchMs`를 줄이는 것이 가장 현실적인 조정 방법입니다.

| 목적 | 권장 설정 |
|---|---|
| 처리량 최대화 | `maxBatchSize=500, maxBatchMs=2000` (현재값 유지) |
| 레이턴시 단축 | `maxBatchSize=100, maxBatchMs=500` |
| 1건씩 즉시 전송 | `maxBatchSize=1, maxBatchMs=0` |
