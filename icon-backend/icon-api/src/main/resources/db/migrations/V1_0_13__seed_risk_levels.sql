-- [2026-04-24] risk_levels 마스터 데이터 초기 적재
-- RiskLevel enum (icon-common) 과 동기화: MONITOR / INTENSIVE / REVIEW / BLOCK
-- ON CONFLICT DO NOTHING → 이미 존재하면 건너뜀 (재실행 안전)

INSERT INTO risk_levels
    (risk_level_id, level_code, level_name, description, action_type, notification_required, display_order, is_active)
VALUES
    ('MONITOR',   10, '모니터링',    '일반 모니터링 대상 — 정기적 확인 필요',      'ALERT', false, 1, true),
    ('INTENSIVE', 20, '집중모니터링', '집중 관찰 필요 — 담당자 주의 요망',          'ALERT', true,  2, true),
    ('REVIEW',    30, '심사',        '담당자 심사 필요 — 거래 보류 가능',           'HOLD',  true,  3, true),
    ('BLOCK',     40, '차단',        '거래 차단 — 즉시 조치 필요',                 'BLOCK', true,  4, true)
ON CONFLICT (risk_level_id) DO NOTHING;
