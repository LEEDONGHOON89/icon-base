-- [2026-03-13] ds_database_config 테이블에 agent_id 컬럼 추가
-- DATABASE 타입 데이터소스의 에이전트 폴링 모드 지원
-- agent_id 설정 시: 에이전트(JdbcCollector)가 JDBC 폴링 후 Push
-- agent_id 미설정: 엔진(DatabaseService)이 직접 JDBC 폴링

ALTER TABLE ds_database_config
    ADD COLUMN IF NOT EXISTS agent_id VARCHAR(100) NULL;

COMMENT ON COLUMN ds_database_config.agent_id IS
    '연결된 에이전트 ID. NULL이면 엔진 직접 폴링, 설정 시 에이전트 Push 모드';
