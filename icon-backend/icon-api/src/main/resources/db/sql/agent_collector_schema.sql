-- [2026-03-13] 에이전트 수집기 기본 설정 테이블 자동 생성 스크립트
-- agent_collector_file_configs / agent_collector_jdbc_configs 는 제거됨
-- 파일/JDBC 상세 설정은 ds_file_system_config / ds_database_config 에서 관리

-- 에이전트 수집기 설정 테이블 (agent_target_configs 가 먼저 존재해야 함)
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
