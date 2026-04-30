# 2026-04-21 — 에이전트 타겟 설정 버그 분석 (미완료 — 이어서 작업 필요)

## 작업 배경
내부통제시스템 에이전트 관리 화면의 "타겟 설정" 모달에서
배치 정책·TLS 설정 주입 기능에 오류가 발생한다는 제보.

## 분석된 문제점

### 문제 1: `AgentTargetConfigController.pushToAgent()` — TLS 비밀번호 미전송
**파일**: `icon-backend/icon-rpc-server/src/main/java/com/itmasters/icon/rpc/agent/adapter/in/web/AgentTargetConfigController.java` (74~90줄)

```java
payload.put("tlsKeystorePath",  cfg.getTlsKeystorePath());
payload.put("tlsTruststorePath",cfg.getTlsTruststorePath());
// ❌ 누락: tlsKeystorePassword, tlsTruststorePassword
```

에이전트가 CONFIG_UPDATE를 받을 때 (`RpcClient.handleConfigUpdate`) `tlsKeystorePassword`와
`tlsTruststorePassword`를 파싱하지만, 서버가 해당 필드를 전송하지 않아 항상 null.
→ TLS 설정을 저장 후 에이전트에 적용해도 비밀번호 없이 적용됨.

**수정 방법**: `AgentTargetConfigDto.Info`에 password 필드를 추가하거나,
`AgentTargetConfigService`에서 entity에서 password를 가져와 payload에 포함.

### 문제 2: `AgentTargetConfigDto.Info` — password 필드 누락 여부 확인 필요
**파일**: `icon-backend/icon-rpc-server/src/main/java/com/itmasters/icon/rpc/agent/application/dto/AgentTargetConfigDto.java`

`AgentTargetConfigDto.Info`에 `tlsKeystorePassword`, `tlsTruststorePassword` 필드가
있는지 확인 필요. 없으면 추가해야 pushToAgent에서 사용 가능.

단, password는 GET /target-configs 응답에는 포함하지 않고
push 전용으로만 내부에서 사용하는 방식이 보안상 적절.

### 문제 3: `AgentTargetConfigService.update()` 확인 필요
**파일**: `icon-backend/icon-rpc-server/src/main/java/com/itmasters/icon/rpc/agent/application/service/AgentTargetConfigService.java`

PUT 요청으로 전달된 `tlsKeystorePassword`가 실제 DB에 저장되는지 확인 필요.
폼에서 빈 문자열("")로 초기화(`initForm`)하므로, 저장 시 기존 비밀번호가
빈 문자열로 덮어써질 수 있음.

### 문제 4: 프론트엔드 `initForm` — 비밀번호 필드 초기화 위험
**파일**: `icon-frontend/src/app/agents/page.tsx` (141~154줄)

```typescript
tlsKeystorePassword:  "",   // ← 항상 빈 문자열로 초기화
tlsTruststorePassword:"",   // ← 항상 빈 문자열로 초기화
```

"저장" 버튼 클릭 시 비밀번호 필드가 비어 있으면 기존 값을 유지해야 함.
현재는 빈 문자열을 그대로 서버에 보내 기존 비밀번호를 지울 수 있음.

## 아직 확인하지 못한 파일
- `icon-backend/icon-rpc-server/src/main/java/com/itmasters/icon/rpc/agent/application/dto/AgentTargetConfigDto.java`
- `icon-backend/icon-rpc-server/src/main/java/com/itmasters/icon/rpc/agent/application/service/AgentTargetConfigService.java`
- `icon-backend/icon-rpc-server/src/main/java/com/itmasters/icon/rpc/agent/adapter/out/persistence/entity/AgentTargetConfigEntity.java`

## 수정해야 할 파일 목록
1. `AgentTargetConfigDto.java` — `Info`에 password 필드 추가 (push 전용, GET 응답 제외)
2. `AgentTargetConfigService.java` — update() 에서 빈 비밀번호면 기존 값 유지
3. `AgentTargetConfigController.java` — pushToAgent()에 password 필드 추가
4. `icon-frontend/src/app/agents/page.tsx` — 비밀번호 빈 문자열 → null 처리 (기존 값 유지)

## 새 세션에서 이어받는 방법
1. `docs/work_list/INDEX.md` 및 이 파일을 먼저 읽을 것
2. 위 4개 파일을 읽고 수정 적용
3. 백엔드 빌드: `icon-backend` 루트에서 `./gradlew :icon-api:bootJar`
4. 프론트엔드는 dev 서버로 확인
