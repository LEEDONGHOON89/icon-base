# todo-007 어댑티브 배치 크기 — 분석 및 검토

**상태**: ⏸ 보류 (분석 필요)  
**분류**: 에이전트 성능  
**작성일**: 2026-04-21

---

## 개요

현재 배치 크기(`maxBatchSize`)가 config.yaml 고정값으로만 동작함.
ACK 레이턴시를 기반으로 배치 크기를 동적 조정하면 서버 부하를 줄이고 처리량을 높일 수 있음.

---

## 기본 아이디어

```
ACK 평균 < 100ms  → 배치 크기 유지 (정상)
ACK 평균 > 500ms  → 배치 크기 1.5배 증가 → 전송 횟수 감소 → 서버 부하 감소
ACK 평균 > 2000ms → 스풀 전환 + 배치 최대 크기로 고정
ACK 평균 < 100ms && 크기 증가 상태 → 점진적 복원
```

---

## ❓ 검토 필요 질의사항

### Q1. ACK 레이턴시 측정 기준
현재 `pendingAcks` Map에서 배치별 ACK 시간을 측정 가능.
어떤 기준으로 "느림"을 판단할지 결정 필요:
- **평균(mean)**: 이상치에 둔감, 계산 간단
- **P95**: 꼬리 레이턴시 감지, 계산 복잡
- **슬라이딩 윈도우 평균**: 최근 N건 평균 — 구현 간단 + 현실적

어떤 기준을 선호하시나요?

### Q2. 배치 크기 조정 범위
- 최소 크기(`minBatchSize`): 너무 작으면 전송 오버헤드 증가
- 최대 크기(`maxBatchSize`): config.yaml의 상한을 초과하면 안 되는지?
- 고정 상한(예: maxBatchSize의 3배)으로 제한할지, 완전 동적으로 할지?

### Q3. 서버 측 백프레셔 신호 활용 가능성
현재 서버가 `ACK`만 내려줌. 서버가 부하 상태를 명시적으로 신호로 내려줄 수 있다면
(예: `{"ack": true, "batchId": "...", "serverLoad": "HIGH"}`)
더 정확한 조정이 가능함. 서버 측 변경도 고려할지?

### Q4. config.yaml 설정 방식
```yaml
# 옵션 A: 어댑티브 활성화 플래그만
adaptiveBatch: true

# 옵션 B: 상세 파라미터 노출
adaptiveBatch:
  enabled: true
  slowAckThresholdMs: 500
  minBatchSize: 50
  maxMultiplier: 3.0
```
운영자가 파라미터를 직접 조정할 수 있어야 하는지, 내부 고정값으로 두는 것이 나은지?

---

## 관련 파일

- `BatchBuilder.java` — 배치 크기 조정 로직 위치
- `RpcClient.java` — ACK 레이턴시 측정 위치 (`pendingAcks`)
- `TargetConfig.java` — 설정 추가 위치
