-- [2026-04-21] V1_0_5: parser_rules 테이블에서 target_standard_field_id 컬럼 제거
-- 파서 규칙은 출력 필드명(target_field_name)만 관리, 표준 필드 매핑은 원본 스키마에서 별도 처리

ALTER TABLE public.parser_rules
    DROP COLUMN IF EXISTS target_standard_field_id;
