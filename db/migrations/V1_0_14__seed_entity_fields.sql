-- [2026-04-24] entity_fields 마스터 데이터 초기 적재
-- 시나리오 엔티티 필터에서 사용하는 기본 필드 목록
-- ON CONFLICT DO NOTHING → 재실행 안전

INSERT INTO entity_fields
    (entity_field_id, display_name, data_type, description, is_active, created_by)
VALUES
    -- 고객(Customer) 엔티티 필드
    ('customer_age',    '고객 나이',       'NUMBER', '고객의 나이 (만 나이)',              true, 'SYSTEM'),
    ('grade',           '고객 등급',       'STRING', '고객 등급 (VIP, VVIP 등)',           true, 'SYSTEM'),
    ('owned_accounts',  '소유 계좌 목록',  'ARRAY',  '고객이 소유한 계좌 ID 목록',         true, 'SYSTEM'),
    ('region',          '지역',            'STRING', '고객 거주 지역',                     true, 'SYSTEM'),
    -- 계좌(Account) 엔티티 필드
    ('account_number',  '계좌 번호',       'STRING', '계좌 번호',                         true, 'SYSTEM'),
    ('account_type',    '계좌 유형',       'STRING', '계좌 유형 (입출금, 예금 등)',         true, 'SYSTEM'),
    ('open_date',       '개설일',          'DATE',   '계좌 개설 일자',                     true, 'SYSTEM'),
    ('open_type',       '개설 방법',       'STRING', '계좌 개설 방법 (대면/비대면)',        true, 'SYSTEM'),
    ('owner_id',        '소유자 ID',       'STRING', '계좌 소유 고객 ID',                  true, 'SYSTEM'),
    ('status',          '계좌 상태',       'STRING', '계좌 상태 (ACTIVE/CLOSED)',          true, 'SYSTEM')
ON CONFLICT (entity_field_id) DO NOTHING;
