-- [2026-04-17] v1.0.1: ds_file_system_config에 agent_id 컬럼 추가
-- EngineDsFileSystemConfigEntity가 agent_id를 참조하나 DB에 컬럼 없어 JDBC 오류 발생
-- agent_id 설정 시: 에이전트가 파일 수집 후 Push
-- agent_id 미설정: 엔진이 직접 파일 시스템 스캔
ALTER TABLE ds_file_system_config
    ADD COLUMN IF NOT EXISTS agent_id VARCHAR(100) NULL;

COMMENT ON COLUMN ds_file_system_config.agent_id IS
    '연결된 에이전트 ID. NULL이면 엔진 직접 스캔, 설정 시 에이전트 Push 모드';
