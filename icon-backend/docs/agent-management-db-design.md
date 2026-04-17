# 에이전트 관리 DB 설계

## 개요

ICON-BACKEND(내부통제시스템)에서 에이전트를 중앙 관리하기 위한 RDB 설계.

### 설계 원칙

- **자동 등록** : 에이전트가 ICON-BACKEND에 최초 WebSocket 연결 시 `agents` 레코드 자동 생성
- **ICON-BACKEND** : 자동 등록된 에이전트의 데이터소스 설정을 관리하는 Master
- **AGENT** : WebSocket으로 설정을 수신하여 로컬 YAML 파일에 저장
- Watermark / File Position : 에이전트 로컬(`data/` 디렉토리) 자체 관리 - DB 불필요

---

## 최초 연결 흐름 (자동 등록)

```
[AGENT 최초 기동]
    |
    |  WebSocket 연결 + 핸드셰이크 메시지 전송
    |  { agent_id, hostname, ip, version, os_info }
    v
[ICON-BACKEND]
    |
    +-- agents 테이블 조회 (agent_id)
    |     존재하지 않으면 -> INSERT (자동 등록, status = PENDING_CONFIG)
    |     존재하면       -> UPDATE (last_connected_at, agent_version, status = ACTIVE)
    |
    +-- agent_sessions INSERT (새 세션 기록)
    |
    +-- agent_config_sync_log 에서 PENDING / FAILED 조회
          -> 미전달 설정이 있으면 WebSocket으로 즉시 재전송
```

## 재연결 흐름

```
[AGENT 재연결]
    |
    v
[ICON-BACKEND]
    +-- agents.last_connected_at UPDATE, status = ACTIVE
    +-- 이전 세션 DISCONNECTED 처리
    +-- agent_sessions INSERT (새 세션)
    +-- PENDING / FAILED sync_log 재전송
```

## 설정 변경 흐름 (관리자 -> 에이전트)

```
[관리자 UI]
    | 수집기 설정 변경 (REST API)
    v
[ICON-BACKEND DB]
  agent_target_configs           <- RPC / TLS / 배치 설정
  agent_collector_configs        <- 수집기 공통 설정
  agent_collector_file_configs   <- FILE 수집기 전용
  agent_collector_jdbc_configs   <- JDBC 수집기 전용
  agent_config_sync_log          <- 변경사항 push 추적 (PENDING -> SENT -> ACKNOWLEDGED)
    |
    |  WebSocket push (YAML 페이로드)
    v
[AGENT 서버]
  data/{target_id}/config.yaml    <- 수신한 설정 저장
  data/{target_id}/positions.*    <- Watermark / File position (로컬 관리)
```

---

## 테이블 관계

```
agents (자동 등록, 1)
  +-- agent_sessions        (N)  : WebSocket 세션 이력
  +-- agent_target_configs  (N)  : 타겟별 RPC/배치 설정
  |     +-- agent_collector_configs      (N) : 수집기 공통
  |           +-- agent_collector_file_configs  (1:1) : FILE 전용
  |           +-- agent_collector_jdbc_configs  (1:1) : JDBC 전용
  +-- agent_config_sync_log (N)  : 설정 동기화 이력
```

---

## DDL

### 1. agents - 에이전트 (최초 연결 시 자동 등록)

최초 연결 시 핸드셰이크 메시지 정보로 자동 INSERT.
이후 연결마다 `last_connected_at`, `agent_version`, `status` UPDATE.

```sql
CREATE TABLE agents (
    agent_id             VARCHAR(100)    NOT NULL,   -- 에이전트 식별자 (config.yaml targets[].id)

    -- 핸드셰이크 수신 정보 (최초 연결 시 에이전트가 전송)
    hostname             VARCHAR(300)    NULL,       -- 설치 서버 호스트명
    ip_address           VARCHAR(50)     NULL,       -- 설치 서버 IP
    agent_version        VARCHAR(50)     NULL,       -- 에이전트 버전
    os_info              VARCHAR(200)    NULL,       -- OS 정보 (선택)

    -- 관리자 설정 메타 정보 (자동 등록 후 수동 입력 가능)
    display_name         VARCHAR(200)    NULL,       -- 표시 이름 (미입력 시 agent_id 사용)
    description          VARCHAR(1000)   NULL,

    -- 상태
    status               VARCHAR(30)     NOT NULL DEFAULT 'PENDING_CONFIG',
                                                     -- PENDING_CONFIG : 최초 등록, 설정 미배포
                                                     -- ACTIVE         : 정상 연결 중
                                                     -- DISCONNECTED   : 연결 끊김
                                                     -- INACTIVE       : 비활성 처리

    first_connected_at   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_connected_at    TIMESTAMP       NULL,
    last_disconnected_at TIMESTAMP       NULL,

    created_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_agents PRIMARY KEY (agent_id),
    CONSTRAINT chk_agent_status CHECK (
        status IN ('PENDING_CONFIG', 'ACTIVE', 'DISCONNECTED', 'INACTIVE')
    )
);

CREATE INDEX idx_agents_status ON agents (status);
```

**status 전이**

```
최초 연결
    |
    v
PENDING_CONFIG -- 설정 배포 완료 --> ACTIVE -- 연결 끊김 --> DISCONNECTED
                                      ^                           |
                                      +-------- 재연결 ----------+
```

---

### 2. agent_sessions - WebSocket 연결 세션 이력

연결/재연결마다 새 레코드 INSERT. 연결 상태 모니터링 및 이력 추적 용도.

```sql
CREATE TABLE agent_sessions (
    session_id          VARCHAR(200)    NOT NULL,   -- WebSocket 세션 ID
    agent_id            VARCHAR(100)    NOT NULL,

    remote_address      VARCHAR(200)    NULL,       -- 에이전트 IP:PORT
    agent_version       VARCHAR(50)     NULL,       -- 연결 시점의 에이전트 버전

    status              VARCHAR(20)     NOT NULL DEFAULT 'CONNECTED',
                                                    -- CONNECTED | DISCONNECTED
    connected_at        TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_heartbeat_at   TIMESTAMP       NULL,       -- 마지막 heartbeat 수신 시각
    disconnected_at     TIMESTAMP       NULL,
    disconnect_reason   VARCHAR(500)    NULL,

    CONSTRAINT pk_agent_sessions PRIMARY KEY (session_id),
    CONSTRAINT fk_sessions_agent FOREIGN KEY (agent_id)
        REFERENCES agents (agent_id) ON DELETE CASCADE,
    CONSTRAINT chk_session_status CHECK (status IN ('CONNECTED', 'DISCONNECTED'))
);

CREATE INDEX idx_sessions_agent_status ON agent_sessions (agent_id, status);
CREATE INDEX idx_sessions_connected_at ON agent_sessions (connected_at DESC);
```

---

### 3. agent_target_configs - 타겟 설정

config.yaml의 `targets[]` 항목에 해당. 에이전트 자동 등록 후 관리자가 설정.

```sql
CREATE TABLE agent_target_configs (
    target_config_id        VARCHAR(100)    NOT NULL,
    agent_id                VARCHAR(100)    NOT NULL,
    target_id               VARCHAR(100)    NOT NULL,   -- config.yaml targets[].id

    -- RPC / TLS
    rpc_endpoint            VARCHAR(500)    NOT NULL,   -- wss://host:port/rpc
    compress                BOOLEAN         NOT NULL DEFAULT FALSE,
    tls_keystore_path       VARCHAR(500)    NULL,
    tls_keystore_password   VARCHAR(200)    NULL,
    tls_truststore_path     VARCHAR(500)    NULL,
    tls_truststore_password VARCHAR(200)    NULL,

    -- 배치 정책
    queue_capacity          INT             NOT NULL DEFAULT 10000,
    max_batch_size          INT             NOT NULL DEFAULT 500,
    max_batch_ms            BIGINT          NOT NULL DEFAULT 2000,
    max_batch_bytes         BIGINT          NOT NULL DEFAULT 1048576,    -- 1MB, 0=무제한

    is_active               BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_agent_target_configs PRIMARY KEY (target_config_id),
    CONSTRAINT fk_target_config_agent FOREIGN KEY (agent_id)
        REFERENCES agents (agent_id) ON DELETE CASCADE,
    CONSTRAINT uq_agent_target UNIQUE (agent_id, target_id)
);
```

---

### 4. agent_collector_configs - 수집기 공통 설정

FILE / JDBC 공통 필드. config.yaml의 `targets[].collectors[]` 항목에 해당.

```sql
CREATE TABLE agent_collector_configs (
    collector_config_id VARCHAR(100)    NOT NULL,       -- UUID
    target_config_id    VARCHAR(100)    NOT NULL,
    collector_type      VARCHAR(20)     NOT NULL,       -- 'FILE' | 'JDBC'
    name                VARCHAR(200)    NOT NULL,

    enabled             BOOLEAN         NOT NULL DEFAULT TRUE,
    poll_interval_ms    BIGINT          NOT NULL DEFAULT 1000,
    max_lines_per_poll  INT             NOT NULL DEFAULT 1000,
    max_record_bytes    INT             NOT NULL DEFAULT 524288,     -- 512KB, 0=무제한

    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_agent_collector_configs PRIMARY KEY (collector_config_id),
    CONSTRAINT fk_collector_target FOREIGN KEY (target_config_id)
        REFERENCES agent_target_configs (target_config_id) ON DELETE CASCADE,
    CONSTRAINT chk_collector_type CHECK (collector_type IN ('FILE', 'JDBC'))
);

CREATE INDEX idx_collectors_target ON agent_collector_configs (target_config_id);
```

---

### 5. agent_collector_file_configs - FILE 수집기 상세

| 필드 | yaml 매핑 | 설명 |
|---|---|---|
| directory | path | 감시할 디렉토리 절대경로 |
| file_name_pattern | file | 파일명 또는 glob 패턴 |
| file_format | format | LOG / CSV / JSON |
| csv_has_header | csvHasHeader | CSV 첫 행 헤더 포함 여부 (true=첫 행을 컬럼명으로 사용, false=csv_columns 사용) |
| csv_columns | csvColumns | CSV 헤더가 없을 때(csv_has_header=false) 컬럼명 직접 지정 |
| csv_delimiter | csvDelimiter | CSV 구분자 |

```sql
CREATE TABLE agent_collector_file_configs (
    collector_config_id VARCHAR(100)    NOT NULL,
    directory           VARCHAR(1000)   NOT NULL,       -- 감시 디렉토리 (yaml: path)
    file_name_pattern   VARCHAR(500)    NOT NULL,       -- 파일명 또는 glob 패턴 (yaml: file)
    file_format         VARCHAR(10)     NOT NULL DEFAULT 'LOG',  -- 'LOG' | 'CSV' | 'JSON'
    csv_has_header      BOOLEAN          NOT NULL DEFAULT TRUE,   -- CSV 첫 행 헤더 포함 여부
    csv_delimiter       VARCHAR(5)      NULL     DEFAULT ',',
    csv_columns         VARCHAR(2000)   NULL,           -- csv_has_header=false 일 때 컬럼명 (콤마 구분)
    charset             VARCHAR(30)     NOT NULL DEFAULT 'UTF-8',

    CONSTRAINT pk_collector_file_configs PRIMARY KEY (collector_config_id),
    CONSTRAINT fk_file_config_collector FOREIGN KEY (collector_config_id)
        REFERENCES agent_collector_configs (collector_config_id) ON DELETE CASCADE,
    CONSTRAINT chk_file_format CHECK (file_format IN ('LOG', 'CSV', 'JSON'))
);
```

---

### 6. agent_collector_jdbc_configs - JDBC 수집기 상세

> **비밀번호**는 AES 등으로 암호화하여 저장.

| 필드 | yaml 매핑 | 설명 |
|---|---|---|
| field1 / field2 | field1 / field2 | 하이워터마크 추적 컬럼명 |
| field1_type | field1_type | STRING / TIMESTAMP / NUMBER |
| field1_initial_value | field1_value | 초기 watermark 값 (이후는 에이전트 로컬 관리) |

```sql
CREATE TABLE agent_collector_jdbc_configs (
    collector_config_id     VARCHAR(100)    NOT NULL,
    url                     VARCHAR(1000)   NOT NULL,   -- jdbc:postgresql://... 등
    username                VARCHAR(200)    NOT NULL,
    password                VARCHAR(500)    NOT NULL,   -- 암호화 저장 필수

    query                   TEXT            NOT NULL,   -- ? 플레이스홀더 포함 SQL

    -- 하이워터마크 필드 1 (필수)
    field1                  VARCHAR(200)    NOT NULL,   -- 추적 컬럼명
    field1_type             VARCHAR(20)     NOT NULL DEFAULT 'STRING',
                                                        -- 'STRING' | 'TIMESTAMP' | 'NUMBER'
    field1_initial_value    VARCHAR(500)    NULL,       -- 에이전트 최초 배포 시 초기값

    -- 하이워터마크 필드 2 (선택)
    field2                  VARCHAR(200)    NULL,
    field2_type             VARCHAR(20)     NULL,
    field2_initial_value    VARCHAR(500)    NULL,

    CONSTRAINT pk_collector_jdbc_configs PRIMARY KEY (collector_config_id),
    CONSTRAINT fk_jdbc_config_collector FOREIGN KEY (collector_config_id)
        REFERENCES agent_collector_configs (collector_config_id) ON DELETE CASCADE,
    CONSTRAINT chk_field1_type CHECK (field1_type IN ('STRING', 'TIMESTAMP', 'NUMBER')),
    CONSTRAINT chk_field2_type CHECK (
        field2_type IS NULL OR field2_type IN ('STRING', 'TIMESTAMP', 'NUMBER')
    )
);
```

---

### 7. agent_config_sync_log - 설정 동기화 이력

설정 변경 시 PENDING 레코드 생성 -> WebSocket 전송 후 SENT -> 에이전트 ACK 시 ACKNOWLEDGED.
에이전트 재접속 시 PENDING / FAILED 레코드를 조회하여 재전송.

```sql
CREATE TABLE agent_config_sync_log (
    sync_id             BIGINT          NOT NULL AUTO_INCREMENT,
    agent_id            VARCHAR(100)    NOT NULL,
    target_config_id    VARCHAR(100)    NULL,           -- 타겟 단위 변경
    collector_config_id VARCHAR(100)    NULL,           -- 수집기 단위 변경

    change_type         VARCHAR(20)     NOT NULL,       -- CREATE | UPDATE | DELETE | ENABLE | DISABLE
    sync_status         VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
                                                        -- PENDING      : 전송 대기
                                                        -- SENT         : 전송 완료, ACK 대기
                                                        -- ACKNOWLEDGED : 에이전트 수신 확인
                                                        -- FAILED       : 전송 실패
    payload             TEXT            NULL,           -- 전송한 YAML/JSON 스냅샷

    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    sent_at             TIMESTAMP       NULL,           -- WebSocket 전송 시각
    ack_at              TIMESTAMP       NULL,           -- 에이전트 수신 확인 시각
    error_message       VARCHAR(2000)   NULL,

    CONSTRAINT pk_agent_config_sync_log PRIMARY KEY (sync_id),
    CONSTRAINT fk_sync_agent FOREIGN KEY (agent_id)
        REFERENCES agents (agent_id) ON DELETE CASCADE,
    CONSTRAINT chk_change_type  CHECK (change_type IN ('CREATE', 'UPDATE', 'DELETE', 'ENABLE', 'DISABLE')),
    CONSTRAINT chk_sync_status  CHECK (sync_status IN ('PENDING', 'SENT', 'ACKNOWLEDGED', 'FAILED'))
);

CREATE INDEX idx_sync_log_agent_status  ON agent_config_sync_log (agent_id, sync_status);
CREATE INDEX idx_sync_log_pending       ON agent_config_sync_log (sync_status, created_at);
```

---

## 동기화 상태 전이

```
설정 변경 발생
    |
    v
PENDING --- WebSocket 전송 ---> SENT --- 에이전트 ACK ---> ACKNOWLEDGED
    |                             |
    | 에이전트 미연결               | ACK 없음 (timeout)
    +-----------------------------+-----------------------> FAILED
                                                               |
                                   에이전트 재접속 시 재전송 <--+
```

---

## 핸드셰이크 메시지 규격 (에이전트 -> ICON-BACKEND)

에이전트가 WebSocket 연결 직후 전송하는 최초 메시지.

```json
{
  "type": "HANDSHAKE",
  "agentId": "systemA",
  "hostname": "server-01.company.com",
  "ipAddress": "192.168.1.100",
  "agentVersion": "1.2.0",
  "osInfo": "Windows Server 2022"
}
```

ICON-BACKEND 처리 로직:

```
1. agents 테이블에서 agentId 조회
   - 없음 -> INSERT (status = PENDING_CONFIG, first_connected_at = now)
   - 있음 -> UPDATE (last_connected_at, agent_version, ip_address, status = ACTIVE)

2. agent_sessions INSERT (session_id, agent_id, remote_address, connected_at)

3. agent_config_sync_log 에서 해당 agentId의 PENDING / FAILED 조회
   -> 있으면 WebSocket으로 순서대로 재전송
```

---

## 비고

| 항목 | 내용 |
|---|---|
| 자동 등록 | 최초 연결 시 핸드셰이크 메시지 기반으로 agents 자동 INSERT |
| display_name | 자동 등록 시 NULL, 관리자가 UI에서 이후 직접 입력 |
| Watermark 현재값 | 에이전트 로컬(data/ 디렉토리) 관리, DB 미저장 |
| File position | 에이전트 로컬(positions.dat) 관리, DB 미저장 |
| JDBC 초기값 | field1_initial_value 에만 저장 (에이전트 최초 배포 시 전달용) |
| 비밀번호 | agent_collector_jdbc_configs.password AES 암호화 필수 |
| 에이전트 재접속 | agent_sessions 신규 INSERT + sync_status = PENDING/FAILED 재전송 |
