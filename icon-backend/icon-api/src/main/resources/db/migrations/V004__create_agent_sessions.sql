-- [2026-04-17] V004: 에이전트 세션 테이블 (icon-rpc-server AgentSessionEntity)
CREATE TABLE IF NOT EXISTS agent_sessions (
    session_id        VARCHAR(200)  NOT NULL PRIMARY KEY,
    agent_id          VARCHAR(100)  NOT NULL,
    remote_address    VARCHAR(200),
    agent_version     VARCHAR(50),
    status            VARCHAR(20)   NOT NULL,
    connected_at      TIMESTAMP     NOT NULL,
    last_heartbeat_at TIMESTAMP,
    disconnected_at   TIMESTAMP,
    disconnect_reason VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_sessions_agent_status ON agent_sessions (agent_id, status);
CREATE INDEX IF NOT EXISTS idx_sessions_connected_at ON agent_sessions (connected_at);
