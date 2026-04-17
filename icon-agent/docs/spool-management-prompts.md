# 스풀 관리 기능 추가 프롬프트 모음

> 현재 상태: 서버 ACK 수신 후 스풀 파일 삭제 (정상 동작)  
> 아래 프롬프트는 필요 시 복사하여 사용하세요.

---

## 1. 스풀 파일 TTL (자동 만료 삭제)

**언제 필요한가?**  
서버가 장기간 미기동 상태일 때 스풀 파일이 무한히 누적되는 것을 방지하고 싶을 때.

**프롬프트:**
```
SpoolManager에 스풀 파일 TTL(Time-To-Live) 기능을 추가해줘.

요구사항:
- config.yaml에 spoolMaxAgeHours 설정 추가 (기본값: 72시간)
- 에이전트 기동 시 또는 replaySpool() 호출 전에 TTL 초과 파일 자동 삭제
- 삭제 시 로그 출력: "[targetId] 스풀 파일 TTL 만료 삭제: {파일명} (생성: {시간})"
- 파일명에 포함된 타임스탬프(밀리초) 기준으로 만료 여부 판단
- 변경 파일: SpoolManager.java, RpcConfig.java (또는 AgentConfig.java), config.yaml
```

---

## 2. 스풀 최대 파일 수 제한 (LRU 방식)

**언제 필요한가?**  
디스크 용량 보호를 위해 스풀 파일이 일정 개수를 초과하면 오래된 파일부터 삭제하고 싶을 때.

**프롬프트:**
```
SpoolManager에 스풀 최대 파일 수 제한 기능을 추가해줘.

요구사항:
- config.yaml에 spoolMaxFiles 설정 추가 (기본값: 1000)
- write() 호출 전에 현재 파일 수 확인, 초과 시 가장 오래된 파일부터 삭제
- 삭제 시 경고 로그 출력: "[targetId] 스풀 한도 초과 — 오래된 파일 삭제: {파일명}"
- 변경 파일: SpoolManager.java, RpcConfig.java (또는 AgentConfig.java), config.yaml
```

---

## 3. 스풀 최대 디스크 용량 제한

**언제 필요한가?**  
파일 수가 아닌 실제 디스크 사용량으로 스풀 크기를 제한하고 싶을 때.

**프롬프트:**
```
SpoolManager에 스풀 최대 디스크 용량 제한 기능을 추가해줘.

요구사항:
- config.yaml에 spoolMaxMb 설정 추가 (기본값: 512 MB)
- write() 호출 전에 현재 스풀 디렉토리 전체 크기 확인
- 용량 초과 시 가장 오래된 파일부터 삭제하여 공간 확보 후 쓰기
- 삭제 시 경고 로그 출력: "[targetId] 스풀 용량 초과({현재MB}MB/{최대MB}MB) — 오래된 파일 삭제: {파일명}"
- 변경 파일: SpoolManager.java, RpcConfig.java (또는 AgentConfig.java), config.yaml
```

---

## 4. TTL + 최대 파일 수 동시 적용 (통합)

**언제 필요한가?**  
TTL과 파일 수 제한을 모두 적용하고 싶을 때 (위 1번 + 2번 통합).

**프롬프트:**
```
SpoolManager에 TTL과 최대 파일 수 제한을 동시에 적용하는 스풀 정리(purge) 기능을 추가해줘.

요구사항:
- config.yaml에 다음 설정 추가:
    spoolMaxAgeHours: 72      # 기본값: 72시간 (0이면 비활성화)
    spoolMaxFiles: 1000       # 기본값: 1000개 (0이면 비활성화)
- SpoolManager에 purgeExpired() 메서드 추가:
    1단계: TTL 초과 파일 삭제 (파일명 타임스탬프 기준)
    2단계: 파일 수 초과 시 오래된 파일부터 삭제
- purgeExpired()는 replaySpool() 호출 직전에 RpcClient에서 호출
- 각 삭제마다 로그 출력 (WARN 레벨)
- 변경 파일: SpoolManager.java, RpcClient.java, RpcConfig.java (또는 AgentConfig.java), config.yaml
```

---

## 5. 스풀 상태 모니터링 로그

**언제 필요한가?**  
스풀 디렉토리의 현재 상태(파일 수, 용량, 가장 오래된 파일)를 주기적으로 로그에 출력하고 싶을 때.

**프롬프트:**
```
SpoolManager에 스풀 상태를 주기적으로 로그에 출력하는 모니터링 기능을 추가해줘.

요구사항:
- config.yaml에 spoolMonitorIntervalMs 설정 추가 (기본값: 60000 = 1분, 0이면 비활성화)
- 출력 내용:
    [targetId] 스풀 상태 — 파일 수: {n}건 / 용량: {MB}MB / 가장 오래된 파일: {경과시간}
- ScheduledExecutorService를 사용하여 주기적으로 출력
- TargetContext 또는 SpoolManager 내부에서 스케줄링
- 변경 파일: SpoolManager.java, TargetContext.java, config.yaml
```

---

## 참고: 현재 스풀 파일 구조

| 항목 | 내용 |
|---|---|
| 저장 위치 | `spool/{targetId}/{timestamp}-{batchId}.spool` |
| 파일명 타임스탬프 | `System.currentTimeMillis()` (밀리초) |
| 삭제 시점 | 서버에서 `{"ack": true, "batchId": "..."}` 수신 후 |
| 재전송 순서 | 타임스탬프 오름차순 (FIFO) |
| 관련 클래스 | `SpoolManager.java`, `RpcClient.java` |
