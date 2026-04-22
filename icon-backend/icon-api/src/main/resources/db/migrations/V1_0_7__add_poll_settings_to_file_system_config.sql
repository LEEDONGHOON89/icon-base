-- [2026-04-21] ds_file_system_config 에 에이전트 폴링 설정 컬럼 추가
-- poll_interval_ms  : 에이전트가 파일을 폴링하는 간격 (밀리초), NULL 시 scan_interval_minutes 에서 변환
-- max_lines_per_poll: 폴링 1회당 최대 처리 라인 수, NULL 시 기본값 1000 적용
-- max_record_bytes  : 단일 레코드 최대 바이트 수, NULL 시 기본값 524288 (512KB) 적용

ALTER TABLE public.ds_file_system_config
    ADD COLUMN IF NOT EXISTS poll_interval_ms   BIGINT,
    ADD COLUMN IF NOT EXISTS max_lines_per_poll INTEGER,
    ADD COLUMN IF NOT EXISTS max_record_bytes   INTEGER;
