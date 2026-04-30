-- [2026-04-29] ds_file_system_config 에 agent_id 컬럼 추가
-- ds_database_config 에는 이미 존재하지만 ds_file_system_config 에 누락되어 있어 추가
-- NULL 허용: 기존 데이터 호환 (NULL = 엔진 직접 폴링, 설정 시 에이전트 Push 모드)
ALTER TABLE public.ds_file_system_config
    ADD COLUMN IF NOT EXISTS agent_id VARCHAR(100) NULL;

COMMENT ON COLUMN public.ds_file_system_config.agent_id IS '연결된 에이전트 ID. NULL이면 엔진 직접 폴링, 설정 시 에이전트 Push 모드';
