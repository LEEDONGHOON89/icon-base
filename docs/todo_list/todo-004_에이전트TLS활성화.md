# todo-004 에이전트 TLS 활성화 (보안)

**상태**: 🔲 미착수  
**분류**: 에이전트 보안  
**작성일**: 2026-04-21  
**우선순위**: 높음 (금융권 필수)

---

## 문제

`RpcClient.java` 에서 SSLContext가 주석 처리되어 에이전트 ↔ 서버 통신이 평문(ws://)으로 이루어지고 있음.

```java
// RpcClient.java L169~172
HttpClient client = HttpClient.newBuilder()
    // .sslContext(sslContext)   ← 비활성화 상태
    .build();
```

config.yaml 에 인증서 경로/패스워드가 설정되어 있으나 실제로 적용되지 않는 상태.

---

## 목표

- WebSocket 연결 시 mTLS(클라이언트 인증서) 적용
- ws:// → wss:// 전환
- 서버 인증서 검증 + 에이전트 클라이언트 인증서 제출

---

## 구현 파일

| 파일 | 변경 내용 |
|---|---|
| `icon-agent/src/.../rpc/RpcClient.java` | `.sslContext(sslContext)` 주석 해제 |
| `icon-agent/src/.../tls/SslContextFactory.java` | 클라이언트 인증서(Keystore) 로드 확인 |
| `icon-agent/config.yaml` | endpoint를 `wss://`로 변경 |

---

## 검토 사항

- 서버(icon-rpc-server) WebSocket 핸들러의 TLS 종단 설정 확인 필요
- `insecureTrustAll: true` 옵션 제거 또는 개발 환경 한정 제한
- 인증서 만료 모니터링 방안
