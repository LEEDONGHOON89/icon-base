-- [2026-04-21] ds_database_config 에 에이전트 폴링 설정 컬럼 추가
-- poll_interval_ms  : 에이전트가 JDBC 폴링을 수행하는 간격 (밀리초), NULL 시 기본값 300000 (5분) 적용
-- max_lines_per_poll: 폴링 1회당 최대 처리 행 수, NULL 시 기본값 1000 적용
-- max_record_bytes  : 단일 레코드 최대 바이트 수, NULL 시 기본값 524288 (512KB) 적용

ALTER TABLE public.ds_database_config
    ADD COLUMN IF NOT EXISTS poll_interval_ms   BIGINT,
    ADD COLUMN IF NOT EXISTS max_lines_per_poll INTEGER,
    ADD COLUMN IF NOT EXISTS max_record_bytes   INTEGER;
