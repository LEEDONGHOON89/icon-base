# todo-005 스풀 파일 AES-256-GCM 암호화 (보안)

**상태**: 🔲 미착수  
**분류**: 에이전트 보안  
**작성일**: 2026-04-21  
**우선순위**: 높음 (금융권 필수)

---

## 문제

`SpoolManager.write()` 에서 배치 데이터를 JSON 평문으로 디스크에 저장함.
서버 단절 시 수집된 금융 원본 데이터가 에이전트 호스트 디스크에 비암호화 상태로 잔류.

```java
// SpoolManager.java L54~55
byte[] data = JSON.writeValueAsBytes(batch);
Files.write(tmpFile, data, ...);  // 평문 저장
```

---

## 목표

- 스풀 파일 저장 시 AES-256-GCM 암호화 적용
- 복호화는 `read()` 에서만 수행 — 디스크에는 항상 암호문만 존재
- 키 관리: config.yaml 또는 환경변수에서 암호화 키 로드

---

## 구현 방안

```
config.yaml:
  spool:
    encryptionKey: "${SPOOL_ENCRYPTION_KEY}"  # 환경변수 치환
    # 또는 키 파일 경로: encryptionKeyFile: "certs/spool.key"
```

```java
// 암호화 흐름
byte[] plaintext  = JSON.writeValueAsBytes(batch);
byte[] iv         = SecureRandom 12 bytes (GCM nonce)
byte[] ciphertext = AES-256-GCM encrypt(plaintext, key, iv)
byte[] stored     = iv + ciphertext  // nonce를 파일 앞에 붙여 저장
```

---

## 구현 파일

| 파일 | 변경 내용 |
|---|---|
| `icon-agent/src/.../spool/SpoolManager.java` | `write()` 암호화, `read()` 복호화 |
| `icon-agent/src/.../spool/SpoolCipher.java` | AES-256-GCM 유틸 클래스 (신규) |
| `icon-agent/config.yaml` | `spool.encryptionKey` 항목 추가 |

---

## 검토 사항

- 키 미설정 시 동작: 암호화 비적용(경고) vs 기동 실패
- 기존 평문 스풀 파일 처리: 마이그레이션 로직 또는 무시 후 경고
- 키 롤오버(교체) 시 기존 파일 복호화 방안
