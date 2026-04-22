-- [2026-04-21] ds_database_config 에 증분 컬럼 초기값 컬럼 추가
-- 첫 수집 시 lastProcessedValue 가 null 인 경우 이 값을 하이워터마크 시작점으로 사용한다.

ALTER TABLE public.ds_database_config
    ADD COLUMN IF NOT EXISTS incremental_column_initial_value VARCHAR(200);
