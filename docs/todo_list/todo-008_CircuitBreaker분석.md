# todo-008 Circuit Breaker 도입 — 분석 및 검토

**상태**: ⏸ 보류 (분석 필요)  
**분류**: 에이전트 안정성  
**작성일**: 2026-04-21

---

## 개요

현재 `RpcClient`는 연결 실패 시 지수 백오프(exponential backoff) + 재연결 루프로 동작함.
Circuit Breaker를 추가하면 서버 과부하 상태에서 에이전트의 무의미한 연결 시도를 차단하고
서버 복구 시간을 확보할 수 있음.

---

## 현재 동작

```
연결 실패 → reconnectBaseMs * 2^n (최대 reconnectMaxMs=60초) → 재연결 시도
최대 60초 간격으로 무기한 반복
```

---

## Circuit Breaker 추가 시 동작

```
CLOSED (정상)
  → 연속 N회 실패 또는 T초 내 실패율 X% 초과
  → OPEN (연결 차단, 스풀만 동작)
    → W초 대기 후
    → HALF-OPEN (1건 테스트 전송)
      → 성공 → CLOSED 복귀
      → 실패 → OPEN 유지 (W초 재대기)
```

---

## ❓ 검토 필요 질의사항

### Q1. 지수 백오프와의 중복 여부
현재 지수 백오프(최대 60초)가 이미 "빠른 재연결 방지" 역할을 함.
Circuit Breaker의 OPEN 상태 추가 시 실질적인 차이:
- 지수 백오프: 연결 시도 간격을 늘림 (계속 시도는 함)
- Circuit Breaker: 특정 임계치 초과 시 시도 자체를 완전 차단

**서버가 과부하 상태일 때 60초 간격 재연결도 부담이 될 수 있는 상황이 있는지?**
(예: 에이전트 100대 이상 운영 시)

### Q2. OPEN 진입 기준
어떤 기준이 더 적합한지:
- **A) 연속 실패 횟수**: 단순, 일시적 네트워크 오류에도 반응
- **B) 시간 창 내 실패율**: 더 정확, 구현 복잡
- **C) ACK 타임아웃 누적 횟수**: 연결은 됐지만 서버가 응답 못 하는 상황 감지 가능

### Q3. HALF-OPEN 테스트 방식
- **실제 배치 전송**: 서버 입장에서 데이터 처리 부하 발생
- **별도 ping 메시지**: 서버에 `/rpc/ping` 또는 WebSocket PING 활용
  - 서버 측 ping 처리 추가 필요 여부?

### Q4. 적용 범위
- 에이전트 단독 적용 (클라이언트 측만)?
- 서버 측에서 에이전트에 `CIRCUIT_OPEN` 메시지를 푸시하는 서버 주도 방식도 고려할지?

### Q5. 모니터링 연동
Circuit Breaker 상태(CLOSED/OPEN/HALF-OPEN)를 `AgentMonitor` 지표에 포함할지?
현재 모니터링 로그에 연결 상태는 기록되지만 CB 상태는 별도 항목 없음.

---

## 관련 파일

- `RpcClient.java` — `handleReconnect()`, `scheduleConnect()` — CB 삽입 위치
- `AgentMonitor.java` — CB 상태 지표 추가 위치
- `TargetConfig.java` — CB 설정 추가 위치
