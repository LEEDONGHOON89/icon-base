# TODO-011 — relation-async 스레드풀 포화 및 작업 유실 위험 개선 검토

> 상태: ⏸ 보류 (검토 대기)
> 작성: 2026-04-23
> 분류: 백엔드 성능

---

## 배경

2026-04-23 운영 서버 스레드 덤프 분석 중 발견.

`jstack` 결과에서 `relation-async` 스레드 4개가 **모두 runnable** 상태로 maxPoolSize(4) 한계에 도달한 것을 확인.

```
"relation-async-2" → runnable
"relation-async-4" → runnable
"relation-async-5" → runnable   ← 번호가 연속되지 않음 = 스레드 빈번히 재생성
"relation-async-6" → runnable
```

스레드 번호가 1,2,3,4가 아닌 2,4,5,6으로 불연속적 → 스레드가 빈번하게 종료/재생성되며 번호 증가 중.

---

## 현재 CPU 부하 추산 (분석 시점 기준)

| 스레드 그룹 | 스레드 수 | 스레드당 평균 CPU | 합계 |
|---|---|---|---|
| ingest-async | 10개 | ~2% | ~20% |
| relation-async | 4개 | ~6% | ~24% |
| JVM/시스템/HTTP | ~41개 | - | ~10% |
| **전체 추산** | **~55개** | - | **~54%** |

relation-async 스레드가 ingest-async 대비 **약 3배 CPU 소모** → 엔티티 관계 추출 처리가 상대적으로 무거움.

---

## 문제점

### 1. maxPoolSize(4) 포화
- 현재 설정: `core=2, max=4, queue=100`
- 4개 모두 runnable → 큐도 포화 상태일 가능성 높음
- 추가 관계 처리 작업이 거부(Reject)될 수 있음

### 2. 작업 거부 시 유실 위험
현재 `AsyncConfig.java`의 relation 스레드풀 거부 핸들러:
```java
executor.setRejectedExecutionHandler((r, e) ->
    log.warn("관계 처리 작업 거부됨 - 큐가 가득 참"));
```
→ **경고 로그만 남기고 작업을 버림** → 엔티티 관계 추출 결과 유실 가능

---

## 검토 방향

### 방안 A: 스레드풀 크기 확대 (단기)
```properties
icon.engine.async.relation.core-pool-size=4    # 2 → 4
icon.engine.async.relation.max-pool-size=8     # 4 → 8
icon.engine.async.relation.queue-capacity=200  # 100 → 200
```

### 방안 B: 거부 정책 CallerRunsPolicy 적용 (단기)
```java
executor.setRejectedExecutionHandler(
    new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
```
→ 큐+풀 포화 시 호출 스레드(WebSocket 수신 스레드)가 직접 실행 → 유실 없음
→ 단, WebSocket 수신 지연 발생 가능

### 방안 C: 관계 처리 로직 성능 개선 (중기)
- 현재 CPU per thread가 ingest 대비 3배 → 알고리즘 최적화 여지 검토
- 배치 처리 vs 건별 처리 방식 비교

---

## 관련 파일

- `icon-backend/icon-engine/src/main/java/com/itmasters/icon/engine/config/AsyncConfig.java`
- `icon-backend/icon-api/src/main/resources/application.properties`
