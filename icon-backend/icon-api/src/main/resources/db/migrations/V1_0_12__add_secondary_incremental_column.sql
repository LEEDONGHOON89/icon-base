-- [2026-04-22] ds_database_config 에 보조 증분 컬럼 추가
-- 복합 키 기반 증분 수집 지원: 기본 증분 컬럼 외에 선택적으로 보조 컬럼을 지정할 수 있다.
-- 예시 쿼리: SELECT * FROM tbl WHERE updated_at >= ? AND seq_id > ? ORDER BY updated_at, seq_id ASC

ALTER TABLE public.ds_database_config
    ADD COLUMN IF NOT EXISTS secondary_incremental_column        VARCHAR(200),
    ADD COLUMN IF NOT EXISTS secondary_incremental_column_type   VARCHAR(50),
    ADD COLUMN IF NOT EXISTS secondary_incremental_column_initial_value VARCHAR(200),
    ADD COLUMN IF NOT EXISTS last_secondary_processed_value      VARCHAR(200);
