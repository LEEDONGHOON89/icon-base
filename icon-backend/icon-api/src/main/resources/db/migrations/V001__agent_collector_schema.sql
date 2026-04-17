-- [2026-04-17] V001: 에이전트 수집기 기본 설정 테이블
CREATE TABLE IF NOT EXISTS agent_collector_configs (
    collector_config_id VARCHAR(100)  NOT NULL PRIMARY KEY,
    target_config_id    VARCHAR(100)  NOT NULL,
    collector_type      VARCHAR(20)   NOT NULL,
    name                VARCHAR(200)  NOT NULL,
    enabled             BOOLEAN       NOT NULL DEFAULT TRUE,
    poll_interval_ms    BIGINT        NOT NULL DEFAULT 1000,
    max_lines_per_poll  INT           NOT NULL DEFAULT 1000,
    max_record_bytes    INT           NOT NULL DEFAULT 524288,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_collector_target FOREIGN KEY (target_config_id)
        REFERENCES agent_target_configs (target_config_id) ON DELETE CASCADE,
    CONSTRAINT chk_collector_type CHECK (collector_type IN ('FILE', 'JDBC'))
);

CREATE INDEX IF NOT EXISTS idx_collectors_target ON agent_collector_configs (target_config_id);

-- FILE 수집기 설정
CREATE TABLE IF NOT EXISTS agent_collector_file_configs (
    collector_config_id VARCHAR(100)     NOT NULL PRIMARY KEY,
    directory           VARCHAR(1000)    NOT NULL,
    file_name_pattern   VARCHAR(500)     NOT NULL,
    file_format         VARCHAR(10)      NOT NULL DEFAULT 'LOG',
    csv_has_header      BOOLEAN          NOT NULL DEFAULT TRUE,
    csv_delimiter       VARCHAR(5)       NULL DEFAULT ',',
    csv_columns         VARCHAR(2000)    NULL,
    charset             VARCHAR(30)      NOT NULL DEFAULT 'UTF-8',
    CONSTRAINT fk_file_config_collector FOREIGN KEY (collector_config_id)
        REFERENCES agent_collector_configs (collector_config_id) ON DELETE CASCADE,
    CONSTRAINT chk_file_format CHECK (file_format IN ('LOG', 'CSV', 'JSON'))
);

-- JDBC 수집기 설정
CREATE TABLE IF NOT EXISTS agent_collector_jdbc_configs (
    collector_config_id       VARCHAR(100)    NOT NULL PRIMARY KEY,
    url                       VARCHAR(1000)   NOT NULL,
    username                  VARCHAR(200)    NOT NULL,
    password                  VARCHAR(500)    NOT NULL,
    query                     TEXT            NOT NULL,
    field1                    VARCHAR(200)    NOT NULL,
    field1_type               VARCHAR(20)     NOT NULL DEFAULT 'STRING',
    field1_initial_value      VARCHAR(500)    NULL,
    field2                    VARCHAR(200)    NULL,
    field2_type               VARCHAR(20)     NULL,
    field2_initial_value      VARCHAR(500)    NULL,
    CONSTRAINT fk_jdbc_config_collector FOREIGN KEY (collector_config_id)
        REFERENCES agent_collector_configs (collector_config_id) ON DELETE CASCADE,
    CONSTRAINT chk_field1_type CHECK (field1_type IN ('STRING', 'TIMESTAMP', 'NUMBER')),
    CONSTRAINT chk_field2_type CHECK (field2_type IS NULL OR field2_type IN ('STRING', 'TIMESTAMP', 'NUMBER'))
);
