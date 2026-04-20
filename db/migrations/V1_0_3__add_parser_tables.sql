-- ============================================================
-- V1.0.3 - 파서(Parser) 기능 추가
-- 파서 정의 테이블, 파서 규칙 테이블 생성
-- data_source_schemas에 parser_id FK 추가
-- ============================================================

-- 파서 정의 테이블
CREATE TABLE IF NOT EXISTS public.parsers (
    parser_id           character varying(13)  NOT NULL,
    parser_name         character varying(100) NOT NULL,
    parser_type         character varying(50)  NOT NULL,
    description         text,
    is_active           boolean                NOT NULL DEFAULT true,
    created_at          timestamp              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          character varying(13),
    updated_at          timestamp              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by          character varying(13),
    CONSTRAINT parsers_pkey PRIMARY KEY (parser_id)
);

COMMENT ON TABLE  public.parsers                IS '파서 정의 (DELIMITER/FIXED_WIDTH/REGEX)';
COMMENT ON COLUMN public.parsers.parser_id      IS '파서 ID';
COMMENT ON COLUMN public.parsers.parser_name    IS '파서명';
COMMENT ON COLUMN public.parsers.parser_type    IS '파서 타입: DELIMITER, FIXED_WIDTH, REGEX';
COMMENT ON COLUMN public.parsers.description    IS '설명';
COMMENT ON COLUMN public.parsers.is_active      IS '활성 여부';

-- 파서 규칙 테이블 (1 파서 : N 추출 규칙)
CREATE TABLE IF NOT EXISTS public.parser_rules (
    parser_rule_id          character varying(13)  NOT NULL,
    parser_id               character varying(13)  NOT NULL,
    rule_order              integer                NOT NULL DEFAULT 0,
    config_json             jsonb                  NOT NULL,
    target_standard_field_id character varying(100),
    target_field_name       character varying(255),
    created_at              timestamp              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by              character varying(13),
    updated_at              timestamp              NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by              character varying(13),
    CONSTRAINT parser_rules_pkey        PRIMARY KEY (parser_rule_id),
    CONSTRAINT parser_rules_parser_fkey FOREIGN KEY (parser_id)
        REFERENCES public.parsers(parser_id) ON DELETE CASCADE,
    CONSTRAINT parser_rules_std_fkey    FOREIGN KEY (target_standard_field_id)
        REFERENCES public.standard_fields(standard_field_id) ON DELETE SET NULL
);

COMMENT ON TABLE  public.parser_rules                           IS '파서 추출 규칙';
COMMENT ON COLUMN public.parser_rules.parser_rule_id            IS '규칙 ID';
COMMENT ON COLUMN public.parser_rules.parser_id                 IS '파서 ID (FK)';
COMMENT ON COLUMN public.parser_rules.rule_order                IS '적용 순서';
COMMENT ON COLUMN public.parser_rules.config_json               IS '파서 타입별 설정 JSON
  DELIMITER  : {"delimiter":"|","index":0}
  FIXED_WIDTH: {"startByte":0,"byteLength":6}
  REGEX      : {"pattern":"^(\\w+)","group":1}';
COMMENT ON COLUMN public.parser_rules.target_standard_field_id  IS '추출 결과를 저장할 표준 필드 ID';
COMMENT ON COLUMN public.parser_rules.target_field_name         IS '표준 필드 없을 때 사용할 커스텀 필드명';

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_parser_rules_parser_id
    ON public.parser_rules USING btree (parser_id);

CREATE INDEX IF NOT EXISTS idx_parsers_type
    ON public.parsers USING btree (parser_type);

CREATE INDEX IF NOT EXISTS idx_parsers_active
    ON public.parsers USING btree (is_active);

-- data_source_schemas 에 parser_id 컬럼 추가
ALTER TABLE public.data_source_schemas
    ADD COLUMN IF NOT EXISTS parser_id character varying(13);

ALTER TABLE public.data_source_schemas
    DROP CONSTRAINT IF EXISTS data_source_schemas_parser_fkey;

ALTER TABLE public.data_source_schemas
    ADD CONSTRAINT data_source_schemas_parser_fkey
    FOREIGN KEY (parser_id)
    REFERENCES public.parsers(parser_id) ON DELETE SET NULL;

COMMENT ON COLUMN public.data_source_schemas.parser_id IS '적용할 파서 ID (선택사항, FK)';
