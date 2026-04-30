-- [2026-04-22] agent_target_configs 테이블에 max_batches_per_second 컬럼 추가
-- 초당 최대 배치 전송 수. 0 = 무제한 (기본값)
ALTER TABLE agent_target_configs
    ADD COLUMN IF NOT EXISTS max_batches_per_second INTEGER NOT NULL DEFAULT 10;
