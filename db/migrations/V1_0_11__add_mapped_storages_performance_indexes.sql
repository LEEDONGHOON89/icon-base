-- [2026-04-22] TODO-001/002 성능 인덱스 추가
-- mapped_storages에 reg_dt 기반 인덱스 추가 (기간 필터 최적화)

-- mapped_storages: landing_record_id + reg_dt 복합 인덱스
-- TODO-002 쿼리에서 landing_records JOIN 후 reg_dt 범위 필터 최적화
CREATE INDEX IF NOT EXISTS idx_mapped_storages_landing_reg_dt
    ON public.mapped_storages (landing_record_id, reg_dt DESC);

-- mapped_storages: reg_dt 단독 인덱스 (기간 필터 기준)
CREATE INDEX IF NOT EXISTS idx_mapped_storages_reg_dt
    ON public.mapped_storages (reg_dt DESC);

-- mapped_storages: processing_status 인덱스 (상태 필터)
CREATE INDEX IF NOT EXISTS idx_mapped_storages_processing_status
    ON public.mapped_storages (processing_status);

-- mapped_storages: transaction_id 인덱스 (거래 ID 완전 일치 검색)
CREATE INDEX IF NOT EXISTS idx_mapped_storages_transaction_id
    ON public.mapped_storages (transaction_id);
