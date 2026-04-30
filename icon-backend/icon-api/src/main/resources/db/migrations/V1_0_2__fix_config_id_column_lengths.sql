-- [2026-04-20] v1.0.2: 데이터소스 설정 테이블 ID 컬럼 길이 확장
-- 원인: 코드에서 접두사 + TSID(13자) 형태로 ID를 생성하여 VARCHAR(13) 초과
--   FSR_ + 13자 = 17자 (ds_file_system_config_id)
--   FS_  + 13자 = 16자 (ds_file_system_config_id)
--   DB_  + 13자 = 16자 (ds_database_config_id)
--   API_ + 13자 = 17자 (ds_api_config_id, 예비 포함)
-- 모든 config PK 및 FK 참조 컬럼을 VARCHAR(50)으로 통일

-- ── ds_file_system_config ────────────────────────────────────
ALTER TABLE ds_file_system_config
    ALTER COLUMN ds_file_system_config_id TYPE VARCHAR(50);

-- ── ds_file_system_log (FK) ──────────────────────────────────
ALTER TABLE ds_file_system_log
    ALTER COLUMN ds_file_system_config_id TYPE VARCHAR(50);

-- ── ds_database_config ───────────────────────────────────────
ALTER TABLE ds_database_config
    ALTER COLUMN ds_database_config_id TYPE VARCHAR(50);

-- ── ds_database_log (FK) ─────────────────────────────────────
ALTER TABLE ds_database_log
    ALTER COLUMN ds_database_config_id TYPE VARCHAR(50);

-- ── ds_api_config ────────────────────────────────────────────
ALTER TABLE ds_api_config
    ALTER COLUMN ds_api_config_id TYPE VARCHAR(50);

-- ── ds_api_log (FK) ──────────────────────────────────────────
ALTER TABLE ds_api_log
    ALTER COLUMN ds_api_config_id TYPE VARCHAR(50);
