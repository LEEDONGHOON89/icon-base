# TODO-003: DB 수집기 접속 패스워드 암호화

## 메타

| 항목 | 내용 |
|---|---|
| 상태 | 🔲 미착수 |
| 분류 | 백엔드 보안 |
| 작성일 | 2026-04-21 |
| 우선순위 | 높음 (금융권 보안 필수) |

---

## 배경 및 문제

`ds_database_config.password_encrypted` 컬럼명은 암호화를 시사하지만,
**실제로는 평문(plain text)이 그대로 저장**되고 있다.

```java
// DatabaseConfigEntity.java — 암호화 없이 바로 저장
this.passwordEncrypted = passwordEncrypted;

// DataSourceConfigService.java — 에이전트에도 평문 전달
collectorMap.put("password", db.getPasswordEncrypted());
```

### 금융권 위반 규정

| 규정 | 항목 | 내용 |
|---|---|---|
| 전자금융감독규정 | 제17조 | 접속정보(패스워드) 암호화 저장 의무 |
| ISMS-P | 2.10.3 | 인증정보 저장 시 암호화 필수 |
| 금융보안원 기반시설 보안통제기준 | 암호키 관리 | 암호키를 코드·DB와 분리 관리 |

---

## 현재 데이터 흐름 (문제)

```
[화면 입력] 평문 패스워드
     ↓
[API] DataSourceConfigService.applyBasic()
     ↓ 암호화 없음
[DB] ds_database_config.password_encrypted ← 평문 저장
     ↓
[에이전트 동기화] COLLECTORS_SYNC 페이로드 ← 평문 전달
```

---

## 목표 데이터 흐름 (개선)

```
[화면 입력] 평문 패스워드
     ↓
[API] PasswordEncryptionService.encrypt()  ← AES-256-GCM
     ↓
[DB] ds_database_config.password_encrypted ← 암호문 저장
     ↓
[에이전트 동기화] PasswordEncryptionService.decrypt() → 복호화 후 전달
[엔진 JDBC 직접 연결] PasswordEncryptionService.decrypt() → 복호화 후 사용
```

---

## 암호화 방식

### 선택: AES-256-GCM

| 항목 | 값 |
|---|---|
| 알고리즘 | AES/GCM/NoPadding |
| 키 길이 | 256 bit |
| IV(Nonce) | 12 byte, 매 암호화마다 랜덤 생성 |
| 저장 형식 | `Base64(IV) + ":" + Base64(암호문)` |

> AES-GCM은 기밀성 + 무결성(인증 태그)을 동시에 제공하여 CBC 대비 안전하다.

### 암호화 키 관리

```properties
# application.properties (실제 키는 환경변수로 주입)
icon.security.db-password-enc-key=${ICON_DB_ENC_KEY}
```

- 키는 **256 bit(32 byte) 랜덤 문자열** 사용
- 운영 서버 환경변수에만 등록, 코드·Git·DB 미포함
- 키 분실 시 모든 저장 패스워드 재입력 필요 → 키 백업 정책 수립 필요

---

## 구현 파일 목록

### 백엔드 — 신규

| 파일 | 설명 |
|---|---|
| `icon-common/.../security/PasswordEncryptionService.java` | AES-256-GCM 암호화/복호화 유틸 (`@Service`) |

```java
// 인터페이스 예시
String encrypt(String plainText);   // 평문 → "IV:암호문" Base64
String decrypt(String cipherText);  // "IV:암호문" Base64 → 평문
```

### 백엔드 — 수정

| 파일 | 변경 내용 |
|---|---|
| `icon-api/.../datasource/application/service/DataSourceConfigService.java` | 저장 시 `encrypt()`, 에이전트 전달 시 `decrypt()` 호출 |
| `icon-engine/.../entity/EngineDsDatabaseConfigEntity.java` | 엔진 내부에서 JDBC 연결 시 `decrypt()` 호출 |

### 설정

| 파일 | 변경 내용 |
|---|---|
| `icon-api/src/main/resources/application.properties` | `icon.security.db-password-enc-key` 환경변수 추가 |
| `icon-engine/src/main/resources/application.properties` | 동일 |

### DB 마이그레이션

| 파일 | 설명 |
|---|---|
| `db/migrations/V1_0_6__reencrypt_db_passwords.sql` 또는 별도 스크립트 | 기존 평문 데이터 재암호화 (운영 배포 전 수동 실행) |

> **주의**: 기존 데이터가 있는 경우 일괄 재암호화 스크립트를 별도 작성해야 한다.  
> SQL 마이그레이션으로 처리 불가 — Java 암호화 로직이 필요하므로 **별도 마이그레이션 유틸리티** 작성 권장.

---

## 현재 양호한 항목 (변경 불필요)

| 항목 | 상태 | 설명 |
|---|---|---|
| API 응답에 패스워드 미포함 | ✅ 양호 | `toDto()`에서 `password` 필드 제외됨 |
| WebSocket TLS 설정 구조 존재 | ✅ 양호 | keystore/truststore 설정 가능 |

---

## 완료 조건

- [ ] `PasswordEncryptionService` 구현 (AES-256-GCM, IV 랜덤)
- [ ] 암호화 키 환경변수 설정 (`ICON_DB_ENC_KEY`)
- [ ] DB 저장 시 암호화 적용
- [ ] 에이전트 COLLECTORS_SYNC 전송 시 복호화 후 전달
- [ ] 엔진 JDBC 직접 연결 시 복호화 적용
- [ ] 기존 평문 데이터 재암호화 스크립트 실행
- [ ] 운영 서버 환경변수 등록 및 키 백업 정책 수립
