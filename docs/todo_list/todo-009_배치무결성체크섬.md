# todo-009 배치 무결성 체크섬 (SHA-256)

**상태**: 🔲 미착수  
**분류**: 에이전트 보안 / 데이터 무결성  
**작성일**: 2026-04-21  
**우선순위**: 중간 (금융권 권장)

---

## 문제

현재 에이전트 → 서버 배치 전송 시 무결성 검증이 없음.
전송 중 데이터 위변조 또는 손상을 감지할 수단이 없는 상태.

---

## 목표

각 배치에 SHA-256 체크섬 필드를 추가하여 서버에서 수신 데이터 무결성을 검증.

---

## 구현 방안

### 에이전트 (전송 측)
```java
// Batch.java에 checksum 필드 추가
private String checksum;  // SHA-256(records JSON)

// RpcClient.serializeBatch() 직렬화 전 계산
String recordsJson = JSON.writeValueAsString(batch.getRecords());
String checksum = sha256Hex(recordsJson);
// → JSON 전송 시 "checksum" 필드 포함
```

### 서버 (수신 측)
```java
// icon-rpc-server 배치 수신 핸들러에서 검증
String received = sha256Hex(batch.getRecords());
if (!received.equals(batch.getChecksum())) {
    // NACK 응답 또는 경고 로그
}
```

---

## 구현 파일

| 파일 | 변경 내용 |
|---|---|
| `icon-agent/src/.../batch/Batch.java` | `checksum` 필드 추가 |
| `icon-agent/src/.../rpc/RpcClient.java` | `serializeBatch()` 에서 체크섬 계산 후 JSON 포함 |
| `icon-rpc-server/...RpcBatchHandler.java` | 수신 배치 체크섬 검증 로직 추가 |

---

## 검토 사항

- 체크섬 불일치 시 서버 동작: NACK(재전송 요청) vs 경고만 기록
- 압축(GZIP) 전송 시 압축 전/후 어느 데이터 기준으로 체크섬 계산할지
- 스풀 파일에도 체크섬 기록하여 디스크 읽기 시 검증 여부
