-- ============================================================
-- V1.0.4 - 파서 재설계
-- 1) parsers 테이블: source_field, config_json(파서레벨) 추가
-- 2) parser_rules 테이블: config_json nullable 변경
-- 3) data_source_schemas: parser_id 컬럼 제거
-- 4) data_source_parsers 신규 테이블 (데이터소스:파서 = 1:N)
-- ============================================================

-- 1. parsers 테이블 변경
ALTER TABLE public.parsers
    ADD COLUMN IF NOT EXISTS source_field VARCHAR(100),
    ADD COLUMN IF NOT EXISTS config_json  JSONB;

COMMENT ON COLUMN public.parsers.source_field IS '파싱 대상 원본 필드명 (예: line)';
COMMENT ON COLUMN public.parsers.config_json  IS '파서 공통 설정 JSON
  DELIMITER  : {"delimiter":"|"}
  FIXED_WIDTH: null (규칙별 byteLength 사용)
  REGEX      : null (규칙별 pattern/group 사용)';

-- 2. parser_rules.config_json → nullable 허용
--    DELIMITER 규칙은 config_json이 필요 없음 (인덱스는 rule_order로 결정)
ALTER TABLE public.parser_rules
    ALTER COLUMN config_json DROP NOT NULL;

COMMENT ON COLUMN public.parser_rules.config_json IS '규칙별 설정 JSON (타입에 따라 사용)
  DELIMITER  : null (파서레벨 delimiter 사용, 인덱스는 rule_order)
  FIXED_WIDTH: {"byteLength":4}
  REGEX      : {"pattern":"^(\\w+)","group":1}';

-- 3. data_source_schemas 파서 연결 제거 (데이터소스 레벨로 이동)
ALTER TABLE public.data_source_schemas
    DROP CONSTRAINT IF EXISTS data_source_schemas_parser_fkey;

ALTER TABLE public.data_source_schemas
    DROP COLUMN IF EXISTS parser_id;

-- 4. data_source_parsers 신규 테이블 (데이터소스:파서 = 1:N)
CREATE TABLE IF NOT EXISTS public.data_source_parsers (
    data_source_parser_id VARCHAR(13)   NOT NULL,
    data_source_id        VARCHAR(100)  NOT NULL,
    parser_id             VARCHAR(13)   NOT NULL,
    parser_order          INTEGER       NOT NULL DEFAULT 0,
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at            TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by            VARCHAR(50),
    CONSTRAINT data_source_parsers_pkey
        PRIMARY KEY (data_source_parser_id),
    CONSTRAINT dsp_ds_fkey
        FOREIGN KEY (data_source_id) REFERENCES public.data_sources(data_source_id) ON DELETE CASCADE,
    CONSTRAINT dsp_parser_fkey
        FOREIGN KEY (parser_id) REFERENCES public.parsers(parser_id) ON DELETE CASCADE,
    CONSTRAINT dsp_uq
        UNIQUE (data_source_id, parser_id)
);

CREATE INDEX IF NOT EXISTS idx_dsp_data_source_id
    ON public.data_source_parsers USING btree (data_source_id);

CREATE INDEX IF NOT EXISTS idx_dsp_parser_id
    ON public.data_source_parsers USING btree (parser_id);

COMMENT ON TABLE  public.data_source_parsers                     IS '데이터소스-파서 연결 (1:N)';
COMMENT ON COLUMN public.data_source_parsers.data_source_parser_id IS '연결 ID';
COMMENT ON COLUMN public.data_source_parsers.data_source_id      IS '데이터소스 ID (FK)';
COMMENT ON COLUMN public.data_source_parsers.parser_id           IS '파서 ID (FK)';
COMMENT ON COLUMN public.data_source_parsers.parser_order        IS '파서 적용 순서 (낮을수록 먼저)';
COMMENT ON COLUMN public.data_source_parsers.is_active           IS '활성 여부';
