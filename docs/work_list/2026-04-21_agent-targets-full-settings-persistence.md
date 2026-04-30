# 2026-04-21 — Agent targets.json 전체 설정 영속화

## 작업 개요
`data/targets.json`을 TLS/배치/큐/스풀/속도제한 전체 설정 저장·복원으로 확장.
CLI(`agent-cli.ps1`)에서 `target add` 시 모든 설정을 넘길 수 있도록 개선.
서버에서 CONFIG_UPDATE 수신 시 targets.json 관리 target도 in-memory 및 파일 동기화.

## 변경 파일

### icon-agent/src/main/java/com/icon/agent/admin/TargetStore.java
- `save()`: `id`/`endpoint`/`compress` 외 TLS(keystorePath/keystorePassword/truststorePath/truststorePassword/keystoreType/insecureTrustAll), reconnect(reconnectBaseMs/reconnectMaxMs/ackTimeoutMs), 배치(maxBatchSize/maxBatchMs/maxBatchBytes/queueCapacity), 스풀(maxSpoolFiles/maxSpoolSizeMb), 속도제한(maxBatchesPerSecond) 전체 저장
- `toTargetConfig()`: 위 전체 필드 복원

### icon-agent/src/main/java/com/icon/agent/admin/AdminServer.java
- `POST /targets` body에 TLS 및 배치/큐/스풀/속도제한 파라미터 수신
- `GET /targets/{id}` 엔드포인트 신규 추가 — target 전체 설정 조회
- target 추가 시 `storeSync` 콜백 제공 → CONFIG_UPDATE 후 targets.json 자동 갱신

### icon-agent/src/main/java/com/icon/agent/target/TargetContext.java
- 생성자에 `Runnable storeSync` 파라미터 추가 (nullable, 기존 2-arg 오버로드 유지)
- `applyConfigUpdate()` 메서드 추가 — CONFIG_UPDATE 수신 시 in-memory `TargetConfig` 업데이트
- persister lambda에서 `configManager.updateTargetRpcAndSave()` 호출 후 `applyConfigUpdate()` + `storeSync.run()` 호출

### icon-agent/src/main/java/com/icon/agent/target/TargetManager.java
- `addTarget(TargetConfig, ConfigManager, Runnable storeSync)` — storeSync 파라미터 추가

### icon-agent/bin/agent-cli.ps1
- `target add` 명령 — `key=value` 형식으로 배치/TLS/스풀/속도제한 옵션 지원
- `target show [id]` 서브커맨드 추가 — 전체 설정 출력
- `help` 업데이트

## 주요 개선 효과
| 항목 | 이전 | 이후 |
|---|---|---|
| TLS 설정 | 재시작 시 유실 | targets.json에 저장·복원 |
| 배치 정책 | 재시작 시 기본값 | targets.json에 저장·복원 |
| CONFIG_UPDATE(서버 push) | targets.json target 무시 | in-memory 업데이트 + targets.json 동기화 |
| compress | ✓ 저장 | ✓ 저장 (유지) |
