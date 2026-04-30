# todo-006 에이전트 config.yaml 패스워드 보호 (보안)

**상태**: 🔲 미착수  
**분류**: 에이전트 보안  
**작성일**: 2026-04-21  
**우선순위**: 높음 (금융권 필수)

---

## 문제

`config.yaml` 에 TLS 인증서 패스워드가 평문으로 저장됨.

```yaml
# config.yaml 현재 상태
tls:
  keystorePassword: "changeit"        # 평문
  truststorePassword: "1234qwer!@"    # 평문
```

에이전트 서버에 접근하는 모든 사용자가 패스워드를 확인 가능.

---

## 목표

`${환경변수명}` 패턴으로 값을 런타임에 치환하여 config 파일에 실제 패스워드가 노출되지 않도록 함.

```yaml
# 목표 상태
tls:
  keystorePassword: "${KEYSTORE_PASS}"
  truststorePassword: "${TRUSTSTORE_PASS}"
```

실행 시:
```powershell
$env:KEYSTORE_PASS = "changeit"
.\icon-agent.bat start
```

---

## 구현 파일

| 파일 | 변경 내용 |
|---|---|
| `icon-agent/src/.../config/ConfigManager.java` | YAML 로드 후 `${VAR}` 패턴을 환경변수로 치환하는 후처리 추가 |
| `icon-agent/config.yaml` | 패스워드 항목을 `${ENV_VAR}` 형태로 변경 |
| `icon-agent/bin/icon-agent.ps1` | 환경변수 설정 가이드 주석 추가 |

---

## 구현 방안

```java
// ConfigManager.java — YAML 문자열 로드 후 치환
String yaml = Files.readString(configPath);
yaml = replaceEnvVars(yaml);  // ${VAR_NAME} → System.getenv("VAR_NAME")

private String replaceEnvVars(String yaml) {
    return yaml.replaceAll("\\$\\{([^}]+)}", m -> {
        String val = System.getenv(m.group(1));
        return val != null ? val : m.group(0);  // 미설정 시 원문 유지 + 경고
    });
}
```

---

## 검토 사항

- 환경변수 미설정 시 동작: 원문 유지(경고) vs 기동 거부
- Windows 서비스 등록 시 환경변수 전달 방법
- JDBC 수집기 DB 패스워드(todo-003, 백엔드)와 연계 고려
