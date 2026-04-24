-- ============================================================
-- [2026-04-17] ICON 데이터베이스 초기화 스크립트
-- V001~V006 마이그레이션 통합본 (단일 init 처리)
-- icon.sh start 최초 실행 시 DB 신규 생성 시 자동 적용됨
-- ============================================================

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET row_security = off;


-- ============================================================
-- [2026-04-17] 에이전트 관련 테이블 (V002~V005)
-- ============================================================

-- 에이전트 등록
CREATE TABLE IF NOT EXISTS agents (
    agent_id             VARCHAR(100)  NOT NULL PRIMARY KEY,
    hostname             VARCHAR(300),
    ip_address           VARCHAR(50),
    agent_version        VARCHAR(50),
    os_info              VARCHAR(200),
    display_name         VARCHAR(200),
    description          VARCHAR(1000),
    status               VARCHAR(30)   NOT NULL,
    first_connected_at   TIMESTAMP     NOT NULL,
    last_connected_at    TIMESTAMP,
    last_disconnected_at TIMESTAMP,
    created_at           TIMESTAMP,
    updated_at           TIMESTAMP
);

-- 에이전트 세션
CREATE TABLE IF NOT EXISTS agent_sessions (
    session_id        VARCHAR(200)  NOT NULL PRIMARY KEY,
    agent_id          VARCHAR(100)  NOT NULL,
    remote_address    VARCHAR(200),
    agent_version     VARCHAR(50),
    status            VARCHAR(20)   NOT NULL,
    connected_at      TIMESTAMP     NOT NULL,
    last_heartbeat_at TIMESTAMP,
    disconnected_at   TIMESTAMP,
    disconnect_reason VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_sessions_agent_status ON agent_sessions (agent_id, status);
CREATE INDEX IF NOT EXISTS idx_sessions_connected_at ON agent_sessions (connected_at);

-- 에이전트 타겟 설정
CREATE TABLE IF NOT EXISTS agent_target_configs (
    target_config_id        VARCHAR(100)   NOT NULL PRIMARY KEY,
    agent_id                VARCHAR(100)   NOT NULL,
    target_id               VARCHAR(100)   NOT NULL,
    rpc_endpoint            VARCHAR(500)   NOT NULL,
    compress                BOOLEAN        NOT NULL DEFAULT FALSE,
    tls_keystore_path       VARCHAR(500),
    tls_keystore_password   VARCHAR(200),
    tls_truststore_path     VARCHAR(500),
    tls_truststore_password VARCHAR(200),
    queue_capacity          INT            NOT NULL DEFAULT 10000,
    max_batch_size          INT            NOT NULL DEFAULT 500,
    max_batch_ms            BIGINT         NOT NULL DEFAULT 2000,
    max_batch_bytes         BIGINT         NOT NULL DEFAULT 1048576,
    is_active               BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP,
    updated_at              TIMESTAMP,
    CONSTRAINT fk_target_agent FOREIGN KEY (agent_id)
        REFERENCES agents (agent_id) ON DELETE CASCADE,
    CONSTRAINT uq_agent_target UNIQUE (agent_id, target_id)
);

-- 에이전트 수집기 설정
CREATE TABLE IF NOT EXISTS agent_collector_configs (
    collector_config_id VARCHAR(100)  NOT NULL PRIMARY KEY,
    target_config_id    VARCHAR(100)  NOT NULL,
    collector_type      VARCHAR(20)   NOT NULL,
    name                VARCHAR(200)  NOT NULL,
    enabled             BOOLEAN       NOT NULL DEFAULT TRUE,
    poll_interval_ms    BIGINT        NOT NULL DEFAULT 1000,
    max_lines_per_poll  INT           NOT NULL DEFAULT 1000,
    max_record_bytes    INT           NOT NULL DEFAULT 524288,
    created_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_collector_target FOREIGN KEY (target_config_id)
        REFERENCES agent_target_configs (target_config_id) ON DELETE CASCADE,
    CONSTRAINT chk_collector_type CHECK (collector_type IN ('FILE', 'JDBC'))
);

CREATE INDEX IF NOT EXISTS idx_collectors_target ON agent_collector_configs (target_config_id);

-- FILE 수집기 설정
CREATE TABLE IF NOT EXISTS agent_collector_file_configs (
    collector_config_id VARCHAR(100)     NOT NULL PRIMARY KEY,
    directory           VARCHAR(1000)    NOT NULL,
    file_name_pattern   VARCHAR(500)     NOT NULL,
    file_format         VARCHAR(10)      NOT NULL DEFAULT 'LOG',
    csv_has_header      BOOLEAN          NOT NULL DEFAULT TRUE,
    csv_delimiter       VARCHAR(5)       NULL DEFAULT ',',
    csv_columns         VARCHAR(2000)    NULL,
    charset             VARCHAR(30)      NOT NULL DEFAULT 'UTF-8',
    CONSTRAINT fk_file_config_collector FOREIGN KEY (collector_config_id)
        REFERENCES agent_collector_configs (collector_config_id) ON DELETE CASCADE,
    CONSTRAINT chk_file_format CHECK (file_format IN ('LOG', 'CSV', 'JSON'))
);

-- JDBC 수집기 설정
CREATE TABLE IF NOT EXISTS agent_collector_jdbc_configs (
    collector_config_id  VARCHAR(100)    NOT NULL PRIMARY KEY,
    url                  VARCHAR(1000)   NOT NULL,
    username             VARCHAR(200)    NOT NULL,
    password             VARCHAR(500)    NOT NULL,
    query                TEXT            NOT NULL,
    field1               VARCHAR(200)    NOT NULL,
    field1_type          VARCHAR(20)     NOT NULL DEFAULT 'STRING',
    field1_initial_value VARCHAR(500)    NULL,
    field2               VARCHAR(200)    NULL,
    field2_type          VARCHAR(20)     NULL,
    field2_initial_value VARCHAR(500)    NULL,
    CONSTRAINT fk_jdbc_config_collector FOREIGN KEY (collector_config_id)
        REFERENCES agent_collector_configs (collector_config_id) ON DELETE CASCADE,
    CONSTRAINT chk_field1_type CHECK (field1_type IN ('STRING', 'TIMESTAMP', 'NUMBER')),
    CONSTRAINT chk_field2_type CHECK (field2_type IS NULL OR field2_type IN ('STRING', 'TIMESTAMP', 'NUMBER'))
);

-- ============================================================
-- [2026-04-17] AI 채팅 테이블 (V002)
-- ============================================================

CREATE TABLE IF NOT EXISTS ai_chat_sessions (
    session_id          BIGSERIAL    PRIMARY KEY,
    external_session_id VARCHAR(100) NOT NULL UNIQUE,
    user_id             VARCHAR(50)  NOT NULL,
    title               VARCHAR(255),
    created_at          TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_user_id    ON ai_chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_updated_at ON ai_chat_sessions(updated_at DESC);

CREATE TABLE IF NOT EXISTS ai_chat_messages (
    message_id BIGSERIAL PRIMARY KEY,
    session_id BIGINT    NOT NULL REFERENCES ai_chat_sessions(session_id) ON DELETE CASCADE,
    role       VARCHAR(20)  NOT NULL,
    content    TEXT         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_session_id ON ai_chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_created_at ON ai_chat_messages(created_at);

-- ============================================================
-- [2026-04-17] 코어 스키마 (V006 - pg_dump 기반)
-- ============================================================
CREATE OR REPLACE FUNCTION public.update_detection_context() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- 이벤트 카운트 증가
    UPDATE detection_context
    SET 
        event_count = event_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE context_id = NEW.context_id;
    
    RETURN NEW;
END;
$$;


--
-- Name: update_detection_context_on_rule(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.update_detection_context_on_rule() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- 룰 매칭 카운트 증가
    UPDATE detection_context
    SET 
        rule_match_count = rule_match_count + 1,
        updated_at = CURRENT_TIMESTAMP
    WHERE context_id = NEW.context_id;
    
    RETURN NEW;
END;
$$;


--
-- Name: update_entity_relation_fields_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.update_entity_relation_fields_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


--
-- Name: update_pattern_relations_updated_at(); Type: FUNCTION; Schema: public; Owner: -
--

CREATE OR REPLACE FUNCTION public.update_pattern_relations_updated_at() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$;


--
-- Name: ai_chat_messages_message_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.ai_chat_messages_message_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ai_chat_sessions_session_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.ai_chat_sessions_session_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: data_source_schemas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.data_source_schemas (
    data_source_schema_id character varying(13) NOT NULL,
    data_source_id character varying(13) NOT NULL,
    field_name character varying(255) NOT NULL,
    data_type character varying(50) NOT NULL,
    is_required boolean DEFAULT false NOT NULL,
    default_value character varying(500),
    description text,
    field_order integer DEFAULT 0,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_by character varying(13),
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by character varying(13),
    standard_field_id character varying(100)
);


--
-- Name: TABLE data_source_schemas; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.data_source_schemas IS '데이터소스 스키마 정보';


--
-- Name: COLUMN data_source_schemas.data_source_schema_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.data_source_schema_id IS '스키마 ID';


--
-- Name: COLUMN data_source_schemas.data_source_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.data_source_id IS '데이터소스 ID';


--
-- Name: COLUMN data_source_schemas.field_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.field_name IS '필드명';


--
-- Name: COLUMN data_source_schemas.data_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.data_type IS '데이터 타입 - STRING, NUMBER, BOOLEAN, DATE, DATETIME, TIME, JSON, ARRAY, OBJECT';


--
-- Name: COLUMN data_source_schemas.is_required; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.is_required IS '필수 여부';


--
-- Name: COLUMN data_source_schemas.default_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.default_value IS '기본값';


--
-- Name: COLUMN data_source_schemas.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.description IS '필드 설명';


--
-- Name: COLUMN data_source_schemas.field_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.field_order IS '필드 순서';


--
-- Name: COLUMN data_source_schemas.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.is_active IS '활성화 여부';


--
-- Name: COLUMN data_source_schemas.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.created_at IS '생성일시';


--
-- Name: COLUMN data_source_schemas.created_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.created_by IS '생성자 ID';


--
-- Name: COLUMN data_source_schemas.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.updated_at IS '수정일시';


--
-- Name: COLUMN data_source_schemas.updated_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_source_schemas.updated_by IS '수정자 ID';


--
-- Name: data_sources; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.data_sources (
    data_source_id character varying(13) NOT NULL,
    name character varying(100) NOT NULL,
    description character varying(500),
    source_type character varying(50) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    transaction_id_field character varying(100)
);


--
-- Name: TABLE data_sources; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.data_sources IS '데이터 소스 정보';


--
-- Name: COLUMN data_sources.data_source_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_sources.data_source_id IS '데이터 소스 ID (ULID - 기존 유지)';


--
-- Name: COLUMN data_sources.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_sources.name IS '데이터 소스 이름';


--
-- Name: COLUMN data_sources.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_sources.description IS '데이터 소스 설명';


--
-- Name: COLUMN data_sources.source_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_sources.source_type IS '데이터 소스 타입';


--
-- Name: COLUMN data_sources.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.data_sources.is_active IS '활성화 여부';


--
-- Name: derived_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.derived_rules (
    rule_id bigint NOT NULL,
    data_source_id character varying(50) NOT NULL,
    target_field character varying(100) NOT NULL,
    field_type character varying(20) NOT NULL,
    computation_type character varying(50) NOT NULL,
    computation_config jsonb NOT NULL,
    priority integer DEFAULT 0 NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    reg_user_id character varying(50),
    reg_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    upd_user_id character varying(50),
    upd_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE derived_rules; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.derived_rules IS '파생 필드 계산 규칙 - 데이터소스별 파생 필드 생성 로직 정의';


--
-- Name: COLUMN derived_rules.rule_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.rule_id IS '파생 규칙 ID (PK, 자동 생성)';


--
-- Name: COLUMN derived_rules.data_source_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.data_source_id IS '데이터소스 ID (FK to data_sources)';


--
-- Name: COLUMN derived_rules.target_field; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.target_field IS '생성할 파생 필드명';


--
-- Name: COLUMN derived_rules.field_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.field_type IS '필드 데이터 타입 (STRING, NUMBER, BOOLEAN, DATE 등)';


--
-- Name: COLUMN derived_rules.computation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.computation_type IS '계산 타입 (FIELD_EQUALS_FIELD, TIME_RANGE_CHECK, FIELD_COMPARISON 등)';


--
-- Name: COLUMN derived_rules.computation_config; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.computation_config IS '계산 설정 JSON (field1, field2, operator 등)';


--
-- Name: COLUMN derived_rules.priority; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.priority IS '실행 우선순위 (낮을수록 먼저 실행)';


--
-- Name: COLUMN derived_rules.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.description IS '규칙 설명';


--
-- Name: COLUMN derived_rules.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.is_active IS '활성화 여부';


--
-- Name: COLUMN derived_rules.reg_user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.reg_user_id IS '등록자 ID';


--
-- Name: COLUMN derived_rules.reg_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.reg_dt IS '등록 일시';


--
-- Name: COLUMN derived_rules.upd_user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.upd_user_id IS '수정자 ID';


--
-- Name: COLUMN derived_rules.upd_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.derived_rules.upd_dt IS '수정 일시';


--
-- Name: derived_rules_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.derived_rules_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: derived_rules_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.derived_rules_rule_id_seq OWNED BY public.derived_rules.rule_id;


--
-- Name: detect_actions; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.detect_actions (
    detect_action_id bigint NOT NULL,
    detect_scenario_id bigint NOT NULL,
    scenario_id character varying(50) NOT NULL,
    group_key character varying(100) NOT NULL,
    action_type character varying(20) NOT NULL,
    action_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    risk_level character varying(20),
    action_memo text,
    action_reason text,
    requested_by character varying(50) NOT NULL,
    requested_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    approved_by character varying(50),
    approved_at timestamp without time zone,
    completed_by character varying(50),
    completed_at timestamp without time zone,
    metadata jsonb,
    reg_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE detect_actions; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.detect_actions IS '탐지된 시나리오에 대한 조치 관리 테이블';


--
-- Name: COLUMN detect_actions.detect_action_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.detect_action_id IS '조치 ID (PK)';


--
-- Name: COLUMN detect_actions.detect_scenario_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.detect_scenario_id IS '탐지 시나리오 ID (FK)';


--
-- Name: COLUMN detect_actions.scenario_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.scenario_id IS '시나리오 정의 ID';


--
-- Name: COLUMN detect_actions.group_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.group_key IS '그룹 키 (사용자/계좌 등)';


--
-- Name: COLUMN detect_actions.action_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.action_type IS '조치 유형 (BLOCK, HOLD, ALLOW, MONITOR)';


--
-- Name: COLUMN detect_actions.action_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.action_status IS '조치 상태 (PENDING, APPROVED, REJECTED, COMPLETED)';


--
-- Name: COLUMN detect_actions.risk_level; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.risk_level IS '리스크 레벨 (HIGH, CRITICAL, MEDIUM, LOW)';


--
-- Name: COLUMN detect_actions.action_memo; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.action_memo IS '조치 메모';


--
-- Name: COLUMN detect_actions.action_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.action_reason IS '조치 사유';


--
-- Name: COLUMN detect_actions.requested_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.requested_by IS '조치 요청자';


--
-- Name: COLUMN detect_actions.requested_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.requested_at IS '조치 요청 시간';


--
-- Name: COLUMN detect_actions.approved_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.approved_by IS '조치 승인자';


--
-- Name: COLUMN detect_actions.approved_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.approved_at IS '조치 승인 시간';


--
-- Name: COLUMN detect_actions.completed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.completed_by IS '조치 완료자';


--
-- Name: COLUMN detect_actions.completed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.completed_at IS '조치 완료 시간';


--
-- Name: COLUMN detect_actions.metadata; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.metadata IS '추가 메타데이터 (JSON)';


--
-- Name: COLUMN detect_actions.reg_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_actions.reg_dt IS '등록 일시';


--
-- Name: detect_actions_detect_action_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.detect_actions_detect_action_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: detect_actions_detect_action_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.detect_actions_detect_action_id_seq OWNED BY public.detect_actions.detect_action_id;


--
-- Name: detect_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.detect_rules (
    detect_rule_id bigint NOT NULL,
    group_key character varying(200) NOT NULL,
    rule_id character varying(50) NOT NULL,
    operator character varying(30) NOT NULL,
    window_minutes integer NOT NULL,
    dedup_minutes integer,
    start_dt timestamp without time zone NOT NULL,
    end_dt timestamp without time zone NOT NULL,
    detected_dt timestamp without time zone NOT NULL,
    matched_count numeric,
    threshold_count numeric,
    pass boolean NOT NULL,
    created_at timestamp without time zone,
    mapped_storage_id bigint,
    original_group_key character varying(200),
    transaction_id character varying(255),
    event_dt timestamp without time zone
);


--
-- Name: TABLE detect_rules; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.detect_rules IS '집계 결과 저장 테이블. aggregates 테이블의 정의에 따라 집계를 실행한 후, 그 결과를 저장합니다.';


--
-- Name: COLUMN detect_rules.detect_rule_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.detect_rule_id IS '집계 결과의 고유 ID';


--
-- Name: COLUMN detect_rules.group_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.group_key IS '집계 그룹 키 (예: 고객 ID)';


--
-- Name: COLUMN detect_rules.rule_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.rule_id IS '실행된 집계 정의 ID (aggregates.aggregate_id)';


--
-- Name: COLUMN detect_rules.operator; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.operator IS '실행된 집계 연산자';


--
-- Name: COLUMN detect_rules.window_minutes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.window_minutes IS '실행된 집계 시간 범위';


--
-- Name: COLUMN detect_rules.dedup_minutes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.dedup_minutes IS '실행된 중복 방지 기간';


--
-- Name: COLUMN detect_rules.start_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.start_dt IS '집계 시작 시간';


--
-- Name: COLUMN detect_rules.end_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.end_dt IS '집계 종료 시간';


--
-- Name: COLUMN detect_rules.detected_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.detected_dt IS '집계 기준 시간';


--
-- Name: COLUMN detect_rules.matched_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.matched_count IS '집계 결과 값 (예: 실제 카운트된 횟수)';


--
-- Name: COLUMN detect_rules.threshold_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.threshold_count IS '비교한 임계값';


--
-- Name: COLUMN detect_rules.pass; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.pass IS '집계 결과 (임계값 통과 여부)';


--
-- Name: COLUMN detect_rules.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.created_at IS '생성일시';


--
-- Name: COLUMN detect_rules.mapped_storage_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_rules.mapped_storage_id IS '앵커 이벤트의 원본 mapped_storage_id';


--
-- Name: detect_rules_detect_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.detect_rules_detect_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: detect_rules_detect_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.detect_rules_detect_rule_id_seq OWNED BY public.detect_rules.detect_rule_id;


--
-- Name: detect_scenarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.detect_scenarios (
    detect_scenario_id bigint NOT NULL,
    scenario_id character varying(100) NOT NULL,
    group_key character varying(200) NOT NULL,
    detected_dt timestamp without time zone NOT NULL,
    risk_level character varying(20),
    window_start timestamp without time zone,
    window_end timestamp without time zone,
    exec_ds_mp_id bigint,
    created_at timestamp without time zone DEFAULT now(),
    transaction_id character varying(255),
    event_dt timestamp without time zone,
    mapped_storage_id bigint
);


--
-- Name: TABLE detect_scenarios; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.detect_scenarios IS '시나리오 탐지 결과(컨텍스트 없이 signals-only). 중복 억제/완성도(>=50% 등) 필터는 앱 레벨에서 적용.';


--
-- Name: COLUMN detect_scenarios.detect_scenario_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_scenarios.detect_scenario_id IS 'PK';


--
-- Name: COLUMN detect_scenarios.scenario_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_scenarios.scenario_id IS '시나리오 식별자(전역 유일 권장)';


--
-- Name: COLUMN detect_scenarios.group_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_scenarios.group_key IS '상관키(= detect_key). event_stream과 일관된 키 사용';


--
-- Name: COLUMN detect_scenarios.detected_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_scenarios.detected_dt IS '탐지 앵커 시각(평가 기준 시각)';


--
-- Name: COLUMN detect_scenarios.risk_level; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_scenarios.risk_level IS '탐지 시 위험도(선택)';


--
-- Name: COLUMN detect_scenarios.window_start; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_scenarios.window_start IS '평가 윈도우 시작(선택)';


--
-- Name: COLUMN detect_scenarios.window_end; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_scenarios.window_end IS '평가 윈도우 종료(선택)';


--
-- Name: COLUMN detect_scenarios.exec_ds_mp_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_scenarios.exec_ds_mp_id IS '실행(런) 식별자. 실행별 필터/감사 목적(선택)';


--
-- Name: COLUMN detect_scenarios.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_scenarios.created_at IS '레코드 생성 시각';


--
-- Name: detect_scenarios_detect_scenario_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.detect_scenarios_detect_scenario_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: detect_scenarios_detect_scenario_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.detect_scenarios_detect_scenario_id_seq OWNED BY public.detect_scenarios.detect_scenario_id;


--
-- Name: detect_sensors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.detect_sensors (
    detect_sensor_id integer NOT NULL,
    mapped_storage_id bigint,
    sensor_id character varying(50) NOT NULL,
    row_number integer,
    detected_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    matched_fields jsonb,
    group_key character varying(200),
    transaction_id character varying(255),
    event_dt timestamp without time zone
);


--
-- Name: TABLE detect_sensors; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.detect_sensors IS '룰 탐지 상세 결과 - 룰에 매칭된 개별 데이터를 저장';


--
-- Name: COLUMN detect_sensors.detect_sensor_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_sensors.detect_sensor_id IS '상세 레코드 고유 ID';


--
-- Name: COLUMN detect_sensors.sensor_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_sensors.sensor_id IS '적용된 룰의 ID';


--
-- Name: COLUMN detect_sensors.row_number; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_sensors.row_number IS '원본 CSV/데이터의 행 번호';


--
-- Name: COLUMN detect_sensors.detected_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_sensors.detected_dt IS '데이터가 탐지된 시간';


--
-- Name: COLUMN detect_sensors.group_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detect_sensors.group_key IS '상관키(= event_stream.group_key). signals-only Step4 윈도우 조회용';


--
-- Name: detect_sensors_detect_sensor_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.detect_sensors_detect_sensor_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: detect_sensors_detect_sensor_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.detect_sensors_detect_sensor_id_seq OWNED BY public.detect_sensors.detect_sensor_id;


--
-- Name: detection_areas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.detection_areas (
    detection_area_id character varying(50) NOT NULL,
    area_name character varying(100) NOT NULL,
    description character varying(500),
    icon character varying(50),
    color character varying(50),
    is_active boolean DEFAULT true NOT NULL,
    display_order integer,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: TABLE detection_areas; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.detection_areas IS '탐지 영역 마스터 - 시나리오를 영역별로 분류 (고객, 계좌, 직원 등)';


--
-- Name: COLUMN detection_areas.detection_area_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_areas.detection_area_id IS '탐지 영역 ID (PK)';


--
-- Name: COLUMN detection_areas.area_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_areas.area_name IS '영역 명칭';


--
-- Name: COLUMN detection_areas.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_areas.description IS '영역 설명';


--
-- Name: COLUMN detection_areas.icon; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_areas.icon IS 'UI 아이콘명';


--
-- Name: COLUMN detection_areas.color; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_areas.color IS 'UI 표시 색상';


--
-- Name: COLUMN detection_areas.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_areas.is_active IS '활성화 여부';


--
-- Name: COLUMN detection_areas.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_areas.display_order IS 'UI 표시 순서';


--
-- Name: COLUMN detection_areas.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_areas.created_at IS '생성 일시';


--
-- Name: COLUMN detection_areas.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_areas.updated_at IS '수정 일시';


--
-- Name: detection_config_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.detection_config_audit (
    audit_id bigint NOT NULL,
    target_type character varying(20) NOT NULL,
    target_id character varying(50) NOT NULL,
    target_name character varying(255),
    action character varying(10) NOT NULL,
    changed_fields jsonb,
    before_snapshot jsonb,
    after_snapshot jsonb,
    change_reason text,
    changed_by character varying(50) NOT NULL,
    changed_at timestamp without time zone DEFAULT now() NOT NULL,
    ip_address character varying(45)
);


--
-- Name: TABLE detection_config_audit; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.detection_config_audit IS '탐지 설정 변경 감사 로그. rules, sensors, scenarios 등 탐지 설정의 생성/수정/삭제 이력을 기록';


--
-- Name: COLUMN detection_config_audit.audit_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.audit_id IS '감사 로그 ID (PK, 자동생성)';


--
-- Name: COLUMN detection_config_audit.target_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.target_type IS '변경 대상 유형 (RULE, SENSOR, SCENARIO, PROFILE 등)';


--
-- Name: COLUMN detection_config_audit.target_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.target_id IS '변경 대상 ID (해당 테이블의 PK 값)';


--
-- Name: COLUMN detection_config_audit.target_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.target_name IS '변경 대상 이름 (조회 편의를 위한 비정규화)';


--
-- Name: COLUMN detection_config_audit.action; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.action IS '수행된 액션 (CREATE, UPDATE, DELETE)';


--
-- Name: COLUMN detection_config_audit.changed_fields; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.changed_fields IS '변경된 필드 목록 (JSON 배열)';


--
-- Name: COLUMN detection_config_audit.before_snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.before_snapshot IS '변경 전 데이터 스냅샷 (JSON)';


--
-- Name: COLUMN detection_config_audit.after_snapshot; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.after_snapshot IS '변경 후 데이터 스냅샷 (JSON)';


--
-- Name: COLUMN detection_config_audit.change_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.change_reason IS '변경 사유 (선택적 입력)';


--
-- Name: COLUMN detection_config_audit.changed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.changed_by IS '변경 수행자 (사용자 ID 또는 시스템)';


--
-- Name: COLUMN detection_config_audit.changed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.changed_at IS '변경 일시 (기본값: 현재 시간)';


--
-- Name: COLUMN detection_config_audit.ip_address; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.detection_config_audit.ip_address IS '변경 요청 IP 주소 (선택적)';


--
-- Name: detection_config_audit_audit_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.detection_config_audit_audit_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: detection_config_audit_audit_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.detection_config_audit_audit_id_seq OWNED BY public.detection_config_audit.audit_id;


--
-- Name: ds_api_config; Type: TABLE; Schema: public; Owner: -
--

-- [2026-04-20] ds_api_config_id: API_ + TSID(13자) = 17자 → VARCHAR(50)으로 확장
CREATE TABLE IF NOT EXISTS public.ds_api_config (
    ds_api_config_id character varying(50) NOT NULL,
    data_source_id character varying(13) NOT NULL,
    connection_name character varying(100) NOT NULL,
    base_url character varying(1000) NOT NULL,
    endpoint_path character varying(500) NOT NULL,
    http_method character varying(10) DEFAULT 'GET'::character varying,
    content_type character varying(100) DEFAULT 'application/json'::character varying,
    auth_type character varying(50),
    auth_header_name character varying(100),
    auth_token_encrypted text,
    username character varying(100),
    password_encrypted text,
    request_headers jsonb,
    request_body_template text,
    query_parameters jsonb,
    pagination_type character varying(50),
    page_size integer DEFAULT 100,
    max_pages integer DEFAULT 1000,
    requests_per_minute integer DEFAULT 60,
    retry_attempts integer DEFAULT 3,
    retry_delay_seconds integer DEFAULT 5,
    last_cursor character varying(500),
    last_page integer DEFAULT 0,
    last_fetched_at timestamp without time zone,
    last_record_id character varying(100),
    total_requests_made integer DEFAULT 0,
    total_records_fetched bigint DEFAULT 0,
    is_active boolean DEFAULT true,
    connection_status character varying(20) DEFAULT 'IDLE'::character varying,
    last_error_message text,
    last_error_time timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50)
);


--
-- Name: ds_api_log; Type: TABLE; Schema: public; Owner: -
--

-- [2026-04-20] ds_api_config_id FK: VARCHAR(50)으로 확장 (ds_api_config PK와 일치)
CREATE TABLE IF NOT EXISTS public.ds_api_log (
    ds_api_log_id character varying(13) NOT NULL,
    ds_api_config_id character varying(50) NOT NULL,
    request_url character varying(1000),
    request_method character varying(10),
    request_headers jsonb,
    request_body text,
    response_status integer,
    response_headers jsonb,
    response_body_size_bytes bigint,
    records_returned integer,
    requested_at timestamp without time zone NOT NULL,
    response_time_ms bigint,
    processing_status character varying(20) DEFAULT 'SUCCESS'::character varying,
    error_message text,
    cursor_before character varying(500),
    cursor_after character varying(500),
    page_number integer
);


--
-- Name: ds_database_config; Type: TABLE; Schema: public; Owner: -
--

-- [2026-04-20] ds_database_config_id: DB_ + TSID(13자) = 16자 → VARCHAR(50)으로 확장
CREATE TABLE IF NOT EXISTS public.ds_database_config (
    ds_database_config_id character varying(50) NOT NULL,
    data_source_id character varying(13) NOT NULL,
    connection_name character varying(100) NOT NULL,
    database_type character varying(50) NOT NULL,
    host character varying(255) NOT NULL,
    port integer NOT NULL,
    database_name character varying(100) NOT NULL,
    schema_name character varying(100),
    username character varying(100) NOT NULL,
    password_encrypted text NOT NULL,
    min_pool_size integer DEFAULT 1,
    max_pool_size integer DEFAULT 10,
    connection_timeout_seconds integer DEFAULT 30,
    idle_timeout_seconds integer DEFAULT 300,
    main_query text NOT NULL,
    incremental_column character varying(100),
    incremental_column_type character varying(50),
    batch_size integer DEFAULT 1000,
    last_processed_value character varying(500),
    last_query_time timestamp without time zone,
    total_queries_executed integer DEFAULT 0,
    total_records_fetched bigint DEFAULT 0,
    is_active boolean DEFAULT true,
    connection_status character varying(20) DEFAULT 'IDLE'::character varying,
    last_error_message text,
    last_error_time timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    agent_id character varying(100)
);


--
-- Name: COLUMN ds_database_config.agent_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.ds_database_config.agent_id IS '연결된 에이전트 ID. NULL이면 엔진 직접 폴링, 설정 시 에이전트 Push 모드';


--
-- Name: ds_database_log; Type: TABLE; Schema: public; Owner: -
--

-- [2026-04-20] ds_database_config_id FK: VARCHAR(50)으로 확장 (ds_database_config PK와 일치)
CREATE TABLE IF NOT EXISTS public.ds_database_log (
    ds_database_log_id character varying(13) NOT NULL,
    ds_database_config_id character varying(50) NOT NULL,
    executed_query text,
    query_parameters jsonb,
    execution_start_time timestamp without time zone NOT NULL,
    execution_end_time timestamp without time zone,
    execution_time_ms bigint,
    records_fetched integer,
    rows_affected integer,
    processing_status character varying(20) DEFAULT 'SUCCESS'::character varying,
    error_message text,
    checkpoint_before character varying(500),
    checkpoint_after character varying(500)
);


--
-- Name: ds_file_system_config; Type: TABLE; Schema: public; Owner: -
--

-- [2026-04-20] ds_file_system_config_id: FSR_/FS_ + TSID(13자) = 최대 17자 → VARCHAR(50)으로 확장
CREATE TABLE IF NOT EXISTS public.ds_file_system_config (
    ds_file_system_config_id character varying(50) NOT NULL,
    data_source_id character varying(13) NOT NULL,
    connection_name character varying(100) NOT NULL,
    watch_directory character varying(500) NOT NULL,
    file_pattern character varying(100) DEFAULT '*'::character varying,
    file_encoding character varying(20) DEFAULT 'UTF-8'::character varying,
    delimiter character varying(5) DEFAULT ','::character varying,
    quote_char character varying(1) DEFAULT '"'::character varying,
    escape_char character varying(1),
    has_header boolean DEFAULT true,
    skip_lines integer DEFAULT 0,
    processing_strategy character varying(50) DEFAULT 'INCREMENTAL'::character varying,
    scan_interval_minutes integer DEFAULT 60,
    move_processed_files boolean DEFAULT false,
    processed_files_directory character varying(500),
    last_processed_file character varying(500),
    last_scan_time timestamp without time zone,
    total_files_processed integer DEFAULT 0,
    total_records_processed bigint DEFAULT 0,
    is_active boolean DEFAULT true,
    connection_status character varying(20) DEFAULT 'IDLE'::character varying,
    last_error_message text,
    last_error_time timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50)
);


--
-- Name: ds_file_system_log; Type: TABLE; Schema: public; Owner: -
--

-- [2026-04-20] ds_file_system_config_id FK: VARCHAR(50)으로 확장 (ds_file_system_config PK와 일치)
CREATE TABLE IF NOT EXISTS public.ds_file_system_log (
    ds_file_system_log_id character varying(13) NOT NULL,
    ds_file_system_config_id character varying(50) NOT NULL,
    file_name character varying(500) NOT NULL,
    file_path character varying(1000) NOT NULL,
    file_size bigint,
    file_modified_time timestamp without time zone,
    file_hash character varying(64),
    processed_at timestamp without time zone NOT NULL,
    records_processed integer,
    processing_time_ms bigint,
    processing_status character varying(20) DEFAULT 'SUCCESS'::character varying,
    error_message text
);


--
-- Name: engine_execution_warnings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.engine_execution_warnings (
    warning_id bigint NOT NULL,
    exec_ds_mp_id bigint NOT NULL,
    step character varying(20) NOT NULL,
    code character varying(50) NOT NULL,
    message text,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: engine_execution_warnings_warning_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.engine_execution_warnings_warning_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: engine_execution_warnings_warning_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.engine_execution_warnings_warning_id_seq OWNED BY public.engine_execution_warnings.warning_id;


--
-- Name: entity_attributes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.entity_attributes (
    entity_attr_id bigint NOT NULL,
    entity_type character varying(20) NOT NULL,
    entity_id character varying(100) NOT NULL,
    attributes jsonb NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    discovered_from character varying(50),
    discovered_at timestamp without time zone,
    status character varying(20) DEFAULT 'SHELL'::character varying
);


--
-- Name: TABLE entity_attributes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.entity_attributes IS '엔티티 정적 속성 저장';


--
-- Name: COLUMN entity_attributes.entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_attributes.entity_type IS '엔티티 타입';


--
-- Name: COLUMN entity_attributes.entity_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_attributes.entity_id IS '엔티티 실제 ID';


--
-- Name: COLUMN entity_attributes.attributes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_attributes.attributes IS '속성 값 (JSONB)';


--
-- Name: COLUMN entity_attributes.discovered_from; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_attributes.discovered_from IS '엔티티를 처음 발견한 DataSource ID';


--
-- Name: COLUMN entity_attributes.discovered_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_attributes.discovered_at IS '엔티티를 처음 발견한 시각';


--
-- Name: COLUMN entity_attributes.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_attributes.status IS '엔티티 상태: SHELL(껍데기), ENRICHED(보강됨), COMPLETE(완전함)';


--
-- Name: entity_attributes_entity_attr_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.entity_attributes_entity_attr_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: entity_attributes_entity_attr_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.entity_attributes_entity_attr_id_seq OWNED BY public.entity_attributes.entity_attr_id;


--
-- Name: entity_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.entity_fields (
    entity_field_id character varying(100) NOT NULL,
    display_name character varying(200),
    data_type character varying(50),
    description character varying(500),
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone,
    created_by character varying(100),
    updated_at timestamp without time zone,
    updated_by character varying(100)
);


--
-- Name: TABLE entity_fields; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.entity_fields IS '시나리오 필터링에 사용 가능한 엔티티 필드 정의';


--
-- Name: COLUMN entity_fields.entity_field_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_fields.entity_field_id IS '엔티티 필드 ID (예: customer_age, attributes.grade)';


--
-- Name: COLUMN entity_fields.display_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_fields.display_name IS '화면 표시명';


--
-- Name: COLUMN entity_fields.data_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_fields.data_type IS '데이터 타입 (STRING, NUMBER, BOOLEAN)';


--
-- Name: COLUMN entity_fields.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_fields.description IS '필드 설명';


--
-- Name: COLUMN entity_fields.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_fields.is_active IS '활성화 여부';


--
-- Name: COLUMN entity_fields.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_fields.created_at IS '생성 일시';


--
-- Name: COLUMN entity_fields.created_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_fields.created_by IS '생성자';


--
-- Name: COLUMN entity_fields.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_fields.updated_at IS '수정 일시';


--
-- Name: COLUMN entity_fields.updated_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_fields.updated_by IS '수정자';


--
-- Name: entity_relation_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.entity_relation_fields (
    config_id bigint NOT NULL,
    data_source_id character varying(50) NOT NULL,
    field_name character varying(100) NOT NULL,
    is_enabled boolean DEFAULT true NOT NULL,
    field_role character varying(20),
    priority integer DEFAULT 0,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    entity_type character varying(50),
    relation_type character varying(50),
    target_field character varying(100),
    target_entity_type character varying(50)
);


--
-- Name: TABLE entity_relation_fields; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.entity_relation_fields IS '엔티티 관계 발견을 위한 후보 필드 설정';


--
-- Name: COLUMN entity_relation_fields.config_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.config_id IS '설정 ID';


--
-- Name: COLUMN entity_relation_fields.data_source_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.data_source_id IS '데이터소스 ID';


--
-- Name: COLUMN entity_relation_fields.field_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.field_name IS '관계 연결에 사용될 필드명 (표준 필드명 기준)';


--
-- Name: COLUMN entity_relation_fields.is_enabled; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.is_enabled IS '활성화 여부';


--
-- Name: COLUMN entity_relation_fields.field_role; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.field_role IS '필드 역할: FROM(출발), TO(도착), BOTH(양방향)';


--
-- Name: COLUMN entity_relation_fields.priority; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.priority IS '우선순위 (높을수록 먼저 고려)';


--
-- Name: COLUMN entity_relation_fields.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.created_at IS '생성 일시';


--
-- Name: COLUMN entity_relation_fields.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.updated_at IS '수정 일시';


--
-- Name: COLUMN entity_relation_fields.entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.entity_type IS '이 필드가 나타내는 엔티티 타입 (ACCOUNT, CUSTOMER 등)';


--
-- Name: COLUMN entity_relation_fields.relation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.relation_type IS '관계 타입 (TRANSFERS_TO, OWNS 등). NULL이면 패턴 발견용 힌트';


--
-- Name: COLUMN entity_relation_fields.target_field; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.target_field IS '대상 필드명 (relation_type이 있을 때 필수)';


--
-- Name: COLUMN entity_relation_fields.target_entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_fields.target_entity_type IS '대상 엔티티 타입';


--
-- Name: entity_relation_fields_config_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.entity_relation_fields_config_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: entity_relation_fields_config_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.entity_relation_fields_config_id_seq OWNED BY public.entity_relation_fields.config_id;


--
-- Name: entity_relation_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.entity_relation_rules (
    rule_id integer NOT NULL,
    data_source_id character varying(50) NOT NULL,
    from_entity_type character varying(50) NOT NULL,
    from_id_field character varying(100) NOT NULL,
    relation_type character varying(50) NOT NULL,
    to_entity_type character varying(50) NOT NULL,
    to_id_field character varying(100) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    description character varying(500),
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    properties_template jsonb DEFAULT '{}'::jsonb,
    to_entity_attributes_template jsonb DEFAULT '{}'::jsonb
);


--
-- Name: COLUMN entity_relation_rules.to_entity_attributes_template; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relation_rules.to_entity_attributes_template IS 'To 엔티티를 entity_attributes에 생성할 때 사용할 초기 속성 템플릿';


--
-- Name: entity_relations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.entity_relations (
    relation_id integer NOT NULL,
    from_entity_type character varying(50) NOT NULL,
    from_entity_id character varying(100) NOT NULL,
    relation_type character varying(50) NOT NULL,
    to_entity_type character varying(50) NOT NULL,
    to_entity_id character varying(100) NOT NULL,
    properties jsonb,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);


--
-- Name: TABLE entity_relations; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.entity_relations IS 'entity_attributes 간 상태 관계 저장 (그래프 엣지) - OWNS, USES, AUTHENTICATES 등 지속적인 관계만 저장. 행위(TRANSFERS_TO)는 event_stream 사용';


--
-- Name: COLUMN entity_relations.relation_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relations.relation_id IS '관계 고유 ID (자동 증가)';


--
-- Name: COLUMN entity_relations.from_entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relations.from_entity_type IS '출발 엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE, AUTHENTICATION)';


--
-- Name: COLUMN entity_relations.from_entity_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relations.from_entity_id IS '출발 엔티티 ID (entity_attributes 참조)';


--
-- Name: COLUMN entity_relations.relation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relations.relation_type IS '관계 유형 (OWNS: 소유, USES: 사용, AUTHENTICATES: 인증, ACCESSES: 접근)';


--
-- Name: COLUMN entity_relations.to_entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relations.to_entity_type IS '도착 엔티티 타입';


--
-- Name: COLUMN entity_relations.to_entity_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relations.to_entity_id IS '도착 엔티티 ID (entity_attributes 참조)';


--
-- Name: COLUMN entity_relations.properties; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relations.properties IS '관계 속성 (JSONB) - ownership_ratio, first_used, usage_count, is_trusted 등 관계별 메타데이터';


--
-- Name: COLUMN entity_relations.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relations.created_at IS '관계 최초 생성 시점';


--
-- Name: COLUMN entity_relations.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_relations.updated_at IS '관계 속성 마지막 업데이트 시점';


--
-- Name: entity_relations_relation_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.entity_relations_relation_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: entity_relations_relation_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.entity_relations_relation_id_seq OWNED BY public.entity_relations.relation_id;


--
-- Name: entity_source_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.entity_source_records (
    record_id bigint NOT NULL,
    entity_type character varying(50) NOT NULL,
    entity_id character varying(255) NOT NULL,
    data_source_id character varying(50) NOT NULL,
    source_tx_id character varying(100),
    source_data jsonb NOT NULL,
    received_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    mapped_storage_id bigint,
    processing_batch_id character varying(100)
);


--
-- Name: TABLE entity_source_records; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.entity_source_records IS '엔티티 원본 데이터 이력 - DataSource에서 수신한 모든 row를 그대로 보관';


--
-- Name: COLUMN entity_source_records.entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_source_records.entity_type IS '엔티티 타입 (EMPLOYEE, CUSTOMER 등)';


--
-- Name: COLUMN entity_source_records.entity_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_source_records.entity_id IS '엔티티 ID (EMP001 등)';


--
-- Name: COLUMN entity_source_records.source_tx_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_source_records.source_tx_id IS '원본 시스템의 트랜잭션 ID (hr_tx_id, cust_tx_id 등)';


--
-- Name: COLUMN entity_source_records.source_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_source_records.source_data IS '원본 row 데이터 전체 (JSONB)';


--
-- Name: COLUMN entity_source_records.received_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_source_records.received_at IS '데이터 수신 시각';


--
-- Name: entity_source_records_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.entity_source_records_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: entity_source_records_record_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.entity_source_records_record_id_seq OWNED BY public.entity_source_records.record_id;


--
-- Name: entity_update_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.entity_update_rules (
    entity_update_rule_id character varying(50) NOT NULL,
    scenario_id character varying(50),
    entity_type character varying(50) NOT NULL,
    field_name character varying(100),
    field_value character varying(500),
    field_type character varying(20),
    description text,
    is_active boolean DEFAULT true,
    reg_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    upd_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    trigger_type character varying(20) DEFAULT 'SCENARIO'::character varying NOT NULL,
    data_source_id character varying(50),
    event_condition text,
    entity_id_expression character varying(200),
    entity_filter text,
    update_type character varying(20) DEFAULT 'SET'::character varying,
    field_value_expression character varying(500),
    delete_entity_if character varying(200),
    CONSTRAINT chk_entity_update_rules_trigger_fields CHECK (((((trigger_type)::text = 'SCENARIO'::text) AND (scenario_id IS NOT NULL)) OR (((trigger_type)::text = 'EVENT'::text) AND (data_source_id IS NOT NULL)))),
    CONSTRAINT chk_entity_update_rules_update_type CHECK (((update_type)::text = ANY (ARRAY[('SET'::character varying)::text, ('INCREMENT'::character varying)::text, ('DECREMENT'::character varying)::text, ('DELETE'::character varying)::text])))
);


--
-- Name: TABLE entity_update_rules; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.entity_update_rules IS '시나리오 탐지 시 엔티티 속성 자동 업데이트 규칙';


--
-- Name: COLUMN entity_update_rules.entity_update_rule_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.entity_update_rule_id IS '업데이트 규칙 ID (예: EUR_CUS013_DORMANT)';


--
-- Name: COLUMN entity_update_rules.scenario_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.scenario_id IS '트리거 시나리오 ID (예: S_CUS013_휴면계좌_탐지)';


--
-- Name: COLUMN entity_update_rules.entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.entity_type IS '엔티티 타입 (CUSTOMER, ACCOUNT, DEVICE)';


--
-- Name: COLUMN entity_update_rules.field_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.field_name IS '업데이트할 필드명 (예: IS_DORMANT_ACCOUNT)';


--
-- Name: COLUMN entity_update_rules.field_value; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.field_value IS '설정할 값 (예: true, HIGH, 2024-01-01)';


--
-- Name: COLUMN entity_update_rules.field_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.field_type IS '필드 타입 (STRING, BOOLEAN, NUMBER, DATE)';


--
-- Name: COLUMN entity_update_rules.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.description IS '규칙 설명';


--
-- Name: COLUMN entity_update_rules.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.is_active IS '활성 여부';


--
-- Name: COLUMN entity_update_rules.reg_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.reg_dt IS '등록 일시';


--
-- Name: COLUMN entity_update_rules.upd_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.upd_dt IS '수정 일시';


--
-- Name: COLUMN entity_update_rules.trigger_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.trigger_type IS '트리거 타입 (SCENARIO: 시나리오 탐지 시, EVENT: 이벤트 발생 시)';


--
-- Name: COLUMN entity_update_rules.data_source_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.data_source_id IS '데이터소스 ID (trigger_type=EVENT일 때 사용)';


--
-- Name: COLUMN entity_update_rules.event_condition; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.event_condition IS '이벤트 조건 JSON (trigger_type=EVENT일 때 사용, 예: {"trade_type": "SELL"})';


--
-- Name: COLUMN entity_update_rules.entity_id_expression; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.entity_id_expression IS '엔티티 ID 표현식 (동적 계산, 예: "customer_id", "CONCAT(customer_id,''_'',security_id)")';


--
-- Name: COLUMN entity_update_rules.entity_filter; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.entity_filter IS '엔티티 필터 조건 JSON (복수 엔티티 업데이트 시 사용)';


--
-- Name: COLUMN entity_update_rules.update_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.update_type IS '업데이트 타입 (SET: 값 설정, INCREMENT: 증가, DECREMENT: 감소, DELETE: 삭제)';


--
-- Name: COLUMN entity_update_rules.field_value_expression; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.field_value_expression IS '필드 값 표현식 (동적 계산, 예: "trade_quantity", "quantity - trade_quantity")';


--
-- Name: COLUMN entity_update_rules.delete_entity_if; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.entity_update_rules.delete_entity_if IS '엔티티 삭제 조건 (예: "quantity <= 0")';


--
-- Name: event_stream; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.event_stream (
    event_stream_id bigint NOT NULL,
    event_data jsonb NOT NULL,
    event_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    mapped_storage_id bigint,
    transaction_id character varying(255)
);


--
-- Name: TABLE event_stream; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.event_stream IS '그룹별 이벤트 스트림 데이터';


--
-- Name: COLUMN event_stream.event_stream_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.event_stream.event_stream_id IS '이벤트 스트림 고유 ID';


--
-- Name: COLUMN event_stream.event_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.event_stream.event_data IS '이벤트 상세 데이터 (JSONB)';


--
-- Name: COLUMN event_stream.event_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.event_stream.event_dt IS '이벤트 생성 시간';


--
-- Name: COLUMN event_stream.mapped_storage_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.event_stream.mapped_storage_id IS '원본 데이터 추적용 외래키';


--
-- Name: event_stream_groups; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.event_stream_groups (
    event_stream_id bigint NOT NULL,
    aggregate_id character varying(100) NOT NULL,
    group_key character varying(500) NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    mapped_storage_id bigint
);


--
-- Name: TABLE event_stream_groups; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.event_stream_groups IS '이벤트-집계 그룹핑 매핑 테이블: 각 event가 어떤 aggregate의 어떤 group_key에 속하는지 저장';


--
-- Name: COLUMN event_stream_groups.event_stream_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.event_stream_groups.event_stream_id IS 'event_stream.event_stream_id FK';


--
-- Name: COLUMN event_stream_groups.aggregate_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.event_stream_groups.aggregate_id IS 'aggregates.aggregate_id FK';


--
-- Name: COLUMN event_stream_groups.group_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.event_stream_groups.group_key IS '집계 그룹 키 (aggregate.group_by_fields 기반으로 생성, 예: "EMP004|ACC_001")';


--
-- Name: event_stream_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.event_stream_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: event_stream_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.event_stream_id_seq OWNED BY public.event_stream.event_stream_id;


--
-- Name: exec_ds_mp; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.exec_ds_mp (
    exec_ds_mp_id bigint NOT NULL,
    data_source_id character varying(50) NOT NULL,
    execution_mode character varying(20),
    execution_context jsonb,
    start_dt timestamp without time zone NOT NULL,
    complete_at timestamp without time zone,
    status character varying(20) NOT NULL,
    total_rows integer,
    error_message text,
    executed_by character varying(100)
);


--
-- Name: TABLE exec_ds_mp; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.exec_ds_mp IS '로그-데이터소소 필드매핑 기록';


--
-- Name: COLUMN exec_ds_mp.exec_ds_mp_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exec_ds_mp.exec_ds_mp_id IS 'UUID v7 형식의 실행 고유 ID';


--
-- Name: COLUMN exec_ds_mp.data_source_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exec_ds_mp.data_source_id IS '데이터소스 ID (datasource 테이블 참조)';


--
-- Name: COLUMN exec_ds_mp.execution_mode; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exec_ds_mp.execution_mode IS '실행 모드: MANUAL, SCHEDULED, TRIGGERED, API';


--
-- Name: COLUMN exec_ds_mp.execution_context; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exec_ds_mp.execution_context IS '실행 시점의 구체적 정보';


--
-- Name: COLUMN exec_ds_mp.start_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exec_ds_mp.start_dt IS '실행 시작 시간';


--
-- Name: COLUMN exec_ds_mp.complete_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exec_ds_mp.complete_at IS '실행 완료 시간';


--
-- Name: COLUMN exec_ds_mp.status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exec_ds_mp.status IS '실행 상태: RUNNING, SUCCESS,FAILED, PARTIAL';


--
-- Name: COLUMN exec_ds_mp.total_rows; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exec_ds_mp.total_rows IS '처리된 총 데이터 행 수';


--
-- Name: COLUMN exec_ds_mp.error_message; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exec_ds_mp.error_message IS '오류 발생 시 에러메시지';


--
-- Name: COLUMN exec_ds_mp.executed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.exec_ds_mp.executed_by IS '실행자 (사용자 ID 또는 system)';


--
-- Name: exec_ds_mp_exec_ds_mp_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.exec_ds_mp_exec_ds_mp_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: exec_ds_mp_exec_ds_mp_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.exec_ds_mp_exec_ds_mp_id_seq OWNED BY public.exec_ds_mp.exec_ds_mp_id;


--
-- Name: icon_migrations_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.icon_migrations_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: landing_records; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.landing_records (
    landing_record_id bigint NOT NULL,
    exec_ds_mp_id bigint NOT NULL,
    data_source_id character varying(50) NOT NULL,
    source_type character varying(50) NOT NULL,
    raw_payload jsonb NOT NULL,
    row_index integer,
    batch_key character varying(100),
    file_path text,
    file_name text,
    extracted_at timestamp without time zone DEFAULT now() NOT NULL,
    ingestion_status character varying(20) DEFAULT 'NEW'::character varying NOT NULL,
    ingestion_message text,
    CONSTRAINT ck_landing_status CHECK (((ingestion_status)::text = ANY (ARRAY[('NEW'::character varying)::text, ('TRANSFORMED'::character varying)::text, ('FAILED'::character varying)::text])))
);


--
-- Name: TABLE landing_records; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.landing_records IS 'ELT Landing 영역: exec_ds_mp 실행 시 원본 데이터를 그대로 적재.';


--
-- Name: COLUMN landing_records.exec_ds_mp_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.landing_records.exec_ds_mp_id IS 'exec_ds_mp 실행 로그 FK';


--
-- Name: COLUMN landing_records.raw_payload; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.landing_records.raw_payload IS '원본 JSON/Key-Value 페이로드';


--
-- Name: COLUMN landing_records.batch_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.landing_records.batch_key IS '파일 이름, 파티션 키 등 배치 분류용 식별자';


--
-- Name: COLUMN landing_records.ingestion_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.landing_records.ingestion_status IS 'Transform 진행 상태 (NEW/TRANSFORMED/FAILED)';


--
-- Name: landing_raw_records_landing_record_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.landing_raw_records_landing_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: landing_raw_records_landing_record_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.landing_raw_records_landing_record_id_seq OWNED BY public.landing_records.landing_record_id;


--
-- Name: mapped_storages; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.mapped_storages (
    mapped_storage_id bigint NOT NULL,
    row_index integer NOT NULL,
    row_data jsonb NOT NULL,
    reg_dt timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    landing_record_id bigint NOT NULL,
    processing_status character varying(20) DEFAULT 'NEW'::character varying,
    error_message text,
    transaction_id character varying(255),
    exec_ds_mp_id bigint
);


--
-- Name: TABLE mapped_storages; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.mapped_storages IS '프로파일별 매핑된 데이터 저장 - row별로 저장';


--
-- Name: COLUMN mapped_storages.row_index; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.mapped_storages.row_index IS 'Row 순서 인덱스';


--
-- Name: COLUMN mapped_storages.row_data; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.mapped_storages.row_data IS '매핑된 개별 row 데이터 (JSONB 형식)';


--
-- Name: COLUMN mapped_storages.reg_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.mapped_storages.reg_dt IS '생성 시간';


--
-- Name: mapped_data_storage_mapped_data_storage_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.mapped_data_storage_mapped_data_storage_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: mapped_data_storage_mapped_data_storage_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.mapped_data_storage_mapped_data_storage_id_seq OWNED BY public.mapped_storages.mapped_storage_id;


--
-- Name: pattern_relations; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.pattern_relations (
    pattern_id bigint NOT NULL,
    data_source_id character varying(50) NOT NULL,
    from_entity_type character varying(50) NOT NULL,
    from_id_field character varying(100) NOT NULL,
    relation_type character varying(50) NOT NULL,
    to_entity_type character varying(50) NOT NULL,
    to_id_field character varying(100) NOT NULL,
    confidence_score numeric(5,4) NOT NULL,
    occurrence_count integer DEFAULT 0 NOT NULL,
    first_seen_at timestamp without time zone NOT NULL,
    last_seen_at timestamp without time zone NOT NULL,
    detection_method character varying(50),
    pattern_evidence jsonb,
    approval_status character varying(20) DEFAULT 'PENDING'::character varying NOT NULL,
    reviewed_by character varying(100),
    reviewed_at timestamp without time zone,
    reject_reason text,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


--
-- Name: TABLE pattern_relations; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.pattern_relations IS '자동 발견된 관계 패턴';


--
-- Name: COLUMN pattern_relations.pattern_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.pattern_id IS '패턴 ID';


--
-- Name: COLUMN pattern_relations.data_source_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.data_source_id IS '데이터소스 ID';


--
-- Name: COLUMN pattern_relations.from_entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.from_entity_type IS 'From 엔티티 타입';


--
-- Name: COLUMN pattern_relations.from_id_field; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.from_id_field IS 'From ID 필드명 (추론)';


--
-- Name: COLUMN pattern_relations.relation_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.relation_type IS '관계 타입 (추론)';


--
-- Name: COLUMN pattern_relations.to_entity_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.to_entity_type IS 'To 엔티티 타입';


--
-- Name: COLUMN pattern_relations.to_id_field; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.to_id_field IS 'To ID 필드명 (추론)';


--
-- Name: COLUMN pattern_relations.confidence_score; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.confidence_score IS '신뢰도 (0.0000 ~ 1.0000)';


--
-- Name: COLUMN pattern_relations.occurrence_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.occurrence_count IS '발견 빈도';


--
-- Name: COLUMN pattern_relations.first_seen_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.first_seen_at IS '최초 발견 일시';


--
-- Name: COLUMN pattern_relations.last_seen_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.last_seen_at IS '마지막 발견 일시';


--
-- Name: COLUMN pattern_relations.detection_method; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.detection_method IS '탐지 방법 (FREQUENCY, TIME_PATTERN, AMOUNT_PATTERN)';


--
-- Name: COLUMN pattern_relations.pattern_evidence; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.pattern_evidence IS '패턴 근거 상세 데이터';


--
-- Name: COLUMN pattern_relations.approval_status; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.approval_status IS '승인 상태 (PENDING, APPROVED, REJECTED)';


--
-- Name: COLUMN pattern_relations.reviewed_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.reviewed_by IS '검토자';


--
-- Name: COLUMN pattern_relations.reviewed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.reviewed_at IS '검토 일시';


--
-- Name: COLUMN pattern_relations.reject_reason; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.reject_reason IS '거부 사유';


--
-- Name: COLUMN pattern_relations.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.created_at IS '생성 일시';


--
-- Name: COLUMN pattern_relations.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.pattern_relations.updated_at IS '수정 일시';


--
-- Name: pattern_relations_pattern_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.pattern_relations_pattern_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: pattern_relations_pattern_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.pattern_relations_pattern_id_seq OWNED BY public.pattern_relations.pattern_id;


--
-- Name: profiles; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.profiles (
    profile_id character varying(13) NOT NULL,
    data_source_id character varying(13) NOT NULL,
    profile_name character varying(100) NOT NULL,
    profile_purpose character varying(50),
    description text,
    display_order integer DEFAULT 0,
    is_active boolean DEFAULT true,
    created_by character varying(13),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by character varying(13),
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    schema_count integer DEFAULT 0,
    rule_count integer DEFAULT 0 NOT NULL,
    group_key_type character varying(20),
    timestamp_key character varying(200),
    destination_type character varying(20),
    entity_type character varying(20),
    entity_id_field character varying(100),
    store_fields jsonb,
    event_type character varying(50),
    event_type_mapping jsonb,
    group_key character varying(500),
    CONSTRAINT chk_profile_destination_type CHECK (((destination_type)::text = ANY (ARRAY[('EVENT_STREAM'::character varying)::text, ('ENTITY'::character varying)::text, ('BOTH'::character varying)::text])))
);


--
-- Name: TABLE profiles; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.profiles IS '데이터 프로파일 정보';


--
-- Name: COLUMN profiles.profile_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.profile_id IS '프로파일 ID';


--
-- Name: COLUMN profiles.data_source_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.data_source_id IS '데이터소스 ID';


--
-- Name: COLUMN profiles.profile_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.profile_name IS '프로파일명 (예: 보안 분석, 성능 분석)';


--
-- Name: COLUMN profiles.profile_purpose; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.profile_purpose IS '목적 - DEFAULT, SECURITY, PERFORMANCE, BUSINESS, COMPLIANCE';


--
-- Name: COLUMN profiles.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.description IS '프로파일 설명';


--
-- Name: COLUMN profiles.display_order; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.display_order IS '표시 순서';


--
-- Name: COLUMN profiles.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.is_active IS '활성화 여부';


--
-- Name: COLUMN profiles.created_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.created_by IS '생성자 ID';


--
-- Name: COLUMN profiles.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.created_at IS '생성일시';


--
-- Name: COLUMN profiles.updated_by; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.updated_by IS '수정자 ID';


--
-- Name: COLUMN profiles.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.updated_at IS '수정일시';


--
-- Name: COLUMN profiles.schema_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.schema_count IS '스키마 등록 수';


--
-- Name: COLUMN profiles.group_key_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.group_key_type IS '기준키 유형';


--
-- Name: COLUMN profiles.timestamp_key; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.timestamp_key IS '시계열 분석에 사용할 타임스탬프 필드명 (standard_field_id)';


--
-- Name: COLUMN profiles.event_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.event_type IS '고정 이벤트 타입 (단순 프로파일용)';


--
-- Name: COLUMN profiles.event_type_mapping; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.profiles.event_type_mapping IS '조건부 이벤트 타입 매핑 (JSON 규칙)';


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.refresh_tokens (
    id character varying(13) NOT NULL,
    token character varying(500) NOT NULL,
    user_id character varying(13) NOT NULL,
    expire_dt timestamp without time zone NOT NULL
);


--
-- Name: TABLE refresh_tokens; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.refresh_tokens IS '리프레시 토큰 테이블';


--
-- Name: COLUMN refresh_tokens.id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refresh_tokens.id IS '토큰 고유 식별자 (TSID)';


--
-- Name: COLUMN refresh_tokens.token; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refresh_tokens.token IS '토큰 값 (TSID)';


--
-- Name: COLUMN refresh_tokens.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refresh_tokens.user_id IS '사용자 ID';


--
-- Name: COLUMN refresh_tokens.expire_dt; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.refresh_tokens.expire_dt IS '만료 일시';


--
-- Name: relation_rules_rule_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE IF NOT EXISTS public.relation_rules_rule_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: relation_rules_rule_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.relation_rules_rule_id_seq OWNED BY public.entity_relation_rules.rule_id;


--
-- Name: risk_levels; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.risk_levels (
    risk_level_id character varying(30) NOT NULL,
    level_code integer NOT NULL,
    level_name character varying(50) NOT NULL,
    description text,
    color_code character varying(20),
    action_type character varying(30),
    notification_required boolean DEFAULT false,
    display_order integer NOT NULL,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE risk_levels; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.risk_levels IS '위험수준 마스터 테이블';


--
-- Name: COLUMN risk_levels.risk_level_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.risk_levels.risk_level_id IS '위험수준 ID (MONITOR, INTENSIVE, BLOCK 등)';


--
-- Name: COLUMN risk_levels.level_code; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.risk_levels.level_code IS '비교용 숫자 코드 (10, 20, 30...)';


--
-- Name: COLUMN risk_levels.level_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.risk_levels.level_name IS '표시명 (모니터링, 집중모니터링, 차단)';


--
-- Name: COLUMN risk_levels.action_type; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.risk_levels.action_type IS '자동 처리 유형 (ALERT, HOLD, BLOCK)';


--
-- Name: rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.rules (
    rule_id character varying(50) NOT NULL,
    name character varying(255) NOT NULL,
    description text,
    operator character varying(30) NOT NULL,
    predicate_sensor_id character varying(50),
    prev_sensor_id character varying(50),
    next_sensor_id character varying(50),
    anchor_sensor_id character varying(50),
    window_minutes integer,
    threshold_count numeric,
    threshold_amount numeric,
    dedup_minutes integer,
    is_active boolean DEFAULT true,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    group_by_fields text[],
    aggregation_field character varying(100),
    where_json jsonb,
    evaluation_mode character varying(20) DEFAULT 'WINDOW'::character varying NOT NULL
);


--
-- Name: TABLE rules; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.rules IS '집계 정의 테이블. 어떤 룰의 Signal을 어떻게 집계할지에 대한 설계도를 저장합니다.';


--
-- Name: COLUMN rules.rule_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.rule_id IS '집계 정의의 고유 ID';


--
-- Name: COLUMN rules.name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.name IS '집계의 이름 (예: "30분 내 3회 이상 이체")';


--
-- Name: COLUMN rules.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.description IS '집계에 대한 상세 설명';


--
-- Name: COLUMN rules.operator; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.operator IS '집계 연산자 (COUNT_WITHIN, SUM_WITHIN, SEQUENCE_WITHIN 등)';


--
-- Name: COLUMN rules.predicate_sensor_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.predicate_sensor_id IS 'sensors 테이블의 ID';


--
-- Name: COLUMN rules.prev_sensor_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.prev_sensor_id IS '시퀀스 연산(A->B)에서의 이전(A) 룰 ID';


--
-- Name: COLUMN rules.next_sensor_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.next_sensor_id IS '시퀀스 연산(A->B)에서의 다음(B) 룰 ID';


--
-- Name: COLUMN rules.anchor_sensor_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.anchor_sensor_id IS '집계 시간의 기준이 되는 앵커 룰 ID (옵션)';


--
-- Name: COLUMN rules.window_minutes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.window_minutes IS '집계 시간 범위 (분 단위)';


--
-- Name: COLUMN rules.threshold_count; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.threshold_count IS '횟수 기반 임계값 (예: 3회 이상)';


--
-- Name: COLUMN rules.threshold_amount; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.threshold_amount IS '금액 기반 임계값 (예: 500만원 이상)';


--
-- Name: COLUMN rules.dedup_minutes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.dedup_minutes IS '집계 결과 중복 방지 기간 (분 단위)';


--
-- Name: COLUMN rules.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.is_active IS '활성화 여부';


--
-- Name: COLUMN rules.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.created_at IS '생성일시';


--
-- Name: COLUMN rules.updated_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.updated_at IS '수정일시';


--
-- Name: COLUMN rules.group_by_fields; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.group_by_fields IS '그룹핑 기준 필드 목록(text[]). COUNT/SUM/AVG/MIN/MAX 등 집계에서 동일 그룹(동일 값 조합) 단위로 윈도우 내 이벤트를 묶어 계산합니다. 예) 동일 수취계좌 3회 → receiver_account. 여러 필드 지정 시 (필드1, 필드2) 튜플로 그룹핑합니다.';


--
-- Name: COLUMN rules.aggregation_field; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.aggregation_field IS '수치 집계 대상 필드명(예: transaction_amount). SUM/AVG/MAX/MIN 등에서 값을 추출할 때 사용합니다.';


--
-- Name: COLUMN rules.where_json; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.where_json IS 'predicate_rule_id 없이 직접 필터링할 조건 (Rule의 where_json과 동일 형식)';


--
-- Name: COLUMN rules.evaluation_mode; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.rules.evaluation_mode IS '집계 평가 모드: IMMEDIATE (즉시, 단일 이벤트), WINDOW (시간 윈도우)';


--
-- Name: scenario_rules; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.scenario_rules (
    scenario_aggregate_id character varying(50) NOT NULL,
    scenario_id character varying(50) NOT NULL,
    rule_id character varying(50) NOT NULL,
    operator character varying(10) NOT NULL,
    order_no integer DEFAULT 0 NOT NULL,
    override_params_json jsonb,
    created_at timestamp without time zone DEFAULT now()
);


--
-- Name: TABLE scenario_rules; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.scenario_rules IS '시나리오 정의와 집계 정의의 매핑(AND/OR, 순서)';


--
-- Name: COLUMN scenario_rules.scenario_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scenario_rules.scenario_id IS '시나리오 ID';


--
-- Name: COLUMN scenario_rules.rule_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scenario_rules.rule_id IS '집계 정의 ID';


--
-- Name: COLUMN scenario_rules.operator; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scenario_rules.operator IS '연결 연산자(AND|OR)';


--
-- Name: COLUMN scenario_rules.order_no; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scenario_rules.order_no IS '평가 순서';


--
-- Name: COLUMN scenario_rules.override_params_json; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scenario_rules.override_params_json IS '시나리오 단위 파라미터 오버라이드(JSON)';


--
-- Name: COLUMN scenario_rules.created_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scenario_rules.created_at IS '생성일시';


--
-- Name: scenarios; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.scenarios (
    scenario_id character varying(13) NOT NULL,
    scenario_name character varying(255) NOT NULL,
    description text,
    dedup_minutes integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    entity_filter_json jsonb,
    primary_entity_type character varying(50) DEFAULT 'CUSTOMER'::character varying NOT NULL,
    risk_level_id character varying(30),
    detection_area_id character varying(30),
    CONSTRAINT ck_scenarios_dedup_minutes_nonneg CHECK ((dedup_minutes >= 0))
);


--
-- Name: COLUMN scenarios.dedup_minutes; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scenarios.dedup_minutes IS '중복 억제 창(분). 0=중복 억제 없음';


--
-- Name: COLUMN scenarios.risk_level_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.scenarios.risk_level_id IS '위험수준 ID';


--
-- Name: sensors; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.sensors (
    sensor_id character varying(100) NOT NULL,
    sensor_name character varying(255) NOT NULL,
    description text,
    version integer DEFAULT 1 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50),
    where_json jsonb
);


--
-- Name: TABLE sensors; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.sensors IS '규칙 테이블';


--
-- Name: COLUMN sensors.sensor_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sensors.sensor_id IS '규칙 고유 식별자 (TSID)';


--
-- Name: COLUMN sensors.sensor_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sensors.sensor_name IS '규칙 이름';


--
-- Name: COLUMN sensors.description; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sensors.description IS '규칙 설명';


--
-- Name: COLUMN sensors.version; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sensors.version IS '버전 (Optimistic Lock)';


--
-- Name: COLUMN sensors.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.sensors.is_active IS '활성화 여부';


--
-- Name: standard_fields; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.standard_fields (
    standard_field_id character varying(100) NOT NULL,
    category character varying(50),
    display_name character varying(255),
    data_type character varying(50) NOT NULL,
    description text,
    is_active boolean DEFAULT true,
    created_by character varying(13),
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_by character varying(13),
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    can_anchor boolean DEFAULT false,
    aliases jsonb,
    allowed_operators text[]
);


--
-- Name: users; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE IF NOT EXISTS public.users (
    user_id character varying(13) NOT NULL,
    login_id character varying(50) NOT NULL,
    password character varying(255) NOT NULL,
    user_name character varying(50) NOT NULL,
    email character varying(100) NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    last_login_at timestamp without time zone,
    password_changed_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    created_by character varying(50),
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_by character varying(50)
);


--
-- Name: TABLE users; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON TABLE public.users IS '사용자 정보 테이블';


--
-- Name: COLUMN users.user_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.user_id IS '사용자 고유 식별자 (TSID)';


--
-- Name: COLUMN users.login_id; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.login_id IS '로그인 ID';


--
-- Name: COLUMN users.password; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.password IS '비밀번호 (암호화)';


--
-- Name: COLUMN users.user_name; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.user_name IS '사용자 실명';


--
-- Name: COLUMN users.email; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.email IS '이메일 주소';


--
-- Name: COLUMN users.is_active; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.is_active IS '활성화 여부';


--
-- Name: COLUMN users.last_login_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.last_login_at IS '마지막 로그인 일시';


--
-- Name: COLUMN users.password_changed_at; Type: COMMENT; Schema: public; Owner: -
--

COMMENT ON COLUMN public.users.password_changed_at IS '비밀번호 변경 일시';


--
-- Name: derived_rules rule_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.derived_rules ALTER COLUMN rule_id SET DEFAULT nextval('public.derived_rules_rule_id_seq'::regclass);


--
-- Name: detect_actions detect_action_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detect_actions ALTER COLUMN detect_action_id SET DEFAULT nextval('public.detect_actions_detect_action_id_seq'::regclass);


--
-- Name: detect_rules detect_rule_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detect_rules ALTER COLUMN detect_rule_id SET DEFAULT nextval('public.detect_rules_detect_rule_id_seq'::regclass);


--
-- Name: detect_scenarios detect_scenario_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detect_scenarios ALTER COLUMN detect_scenario_id SET DEFAULT nextval('public.detect_scenarios_detect_scenario_id_seq'::regclass);


--
-- Name: detect_sensors detect_sensor_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detect_sensors ALTER COLUMN detect_sensor_id SET DEFAULT nextval('public.detect_sensors_detect_sensor_id_seq'::regclass);


--
-- Name: detection_config_audit audit_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detection_config_audit ALTER COLUMN audit_id SET DEFAULT nextval('public.detection_config_audit_audit_id_seq'::regclass);


--
-- Name: engine_execution_warnings warning_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.engine_execution_warnings ALTER COLUMN warning_id SET DEFAULT nextval('public.engine_execution_warnings_warning_id_seq'::regclass);


--
-- Name: entity_attributes entity_attr_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_attributes ALTER COLUMN entity_attr_id SET DEFAULT nextval('public.entity_attributes_entity_attr_id_seq'::regclass);


--
-- Name: entity_relation_fields config_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_relation_fields ALTER COLUMN config_id SET DEFAULT nextval('public.entity_relation_fields_config_id_seq'::regclass);


--
-- Name: entity_relation_rules rule_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_relation_rules ALTER COLUMN rule_id SET DEFAULT nextval('public.relation_rules_rule_id_seq'::regclass);


--
-- Name: entity_relations relation_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_relations ALTER COLUMN relation_id SET DEFAULT nextval('public.entity_relations_relation_id_seq'::regclass);


--
-- Name: entity_source_records record_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_source_records ALTER COLUMN record_id SET DEFAULT nextval('public.entity_source_records_record_id_seq'::regclass);


--
-- Name: event_stream event_stream_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_stream ALTER COLUMN event_stream_id SET DEFAULT nextval('public.event_stream_id_seq'::regclass);


--
-- Name: exec_ds_mp exec_ds_mp_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exec_ds_mp ALTER COLUMN exec_ds_mp_id SET DEFAULT nextval('public.exec_ds_mp_exec_ds_mp_id_seq'::regclass);


--
-- Name: landing_records landing_record_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.landing_records ALTER COLUMN landing_record_id SET DEFAULT nextval('public.landing_raw_records_landing_record_id_seq'::regclass);


--
-- Name: mapped_storages mapped_storage_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mapped_storages ALTER COLUMN mapped_storage_id SET DEFAULT nextval('public.mapped_data_storage_mapped_data_storage_id_seq'::regclass);


--
-- Name: pattern_relations pattern_id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pattern_relations ALTER COLUMN pattern_id SET DEFAULT nextval('public.pattern_relations_pattern_id_seq'::regclass);


--
-- Name: data_source_schemas data_source_original_schemas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_source_schemas
    ADD CONSTRAINT data_source_original_schemas_pkey PRIMARY KEY (data_source_schema_id);


--
-- Name: data_sources data_sources_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_sources
    ADD CONSTRAINT data_sources_pkey PRIMARY KEY (data_source_id);


--
-- Name: derived_rules derived_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.derived_rules
    ADD CONSTRAINT derived_rules_pkey PRIMARY KEY (rule_id);


--
-- Name: detect_actions detect_actions_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detect_actions
    ADD CONSTRAINT detect_actions_pkey PRIMARY KEY (detect_action_id);


--
-- Name: detect_rules detect_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detect_rules
    ADD CONSTRAINT detect_rules_pkey PRIMARY KEY (detect_rule_id);


--
-- Name: detect_scenarios detect_scenarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detect_scenarios
    ADD CONSTRAINT detect_scenarios_pkey PRIMARY KEY (detect_scenario_id);


--
-- Name: detect_sensors detect_sensors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detect_sensors
    ADD CONSTRAINT detect_sensors_pkey PRIMARY KEY (detect_sensor_id);


--
-- Name: detection_config_audit detection_config_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detection_config_audit
    ADD CONSTRAINT detection_config_audit_pkey PRIMARY KEY (audit_id);


--
-- Name: detection_areas domain_types_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detection_areas
    ADD CONSTRAINT domain_types_pkey PRIMARY KEY (detection_area_id);


--
-- Name: ds_api_config ds_api_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_api_config
    ADD CONSTRAINT ds_api_config_pkey PRIMARY KEY (ds_api_config_id);


--
-- Name: ds_api_log ds_api_log_ds_api_config_id_cursor_before_page_number_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_api_log
    ADD CONSTRAINT ds_api_log_ds_api_config_id_cursor_before_page_number_key UNIQUE (ds_api_config_id, cursor_before, page_number);


--
-- Name: ds_api_log ds_api_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_api_log
    ADD CONSTRAINT ds_api_log_pkey PRIMARY KEY (ds_api_log_id);


--
-- Name: ds_database_config ds_database_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_database_config
    ADD CONSTRAINT ds_database_config_pkey PRIMARY KEY (ds_database_config_id);


--
-- Name: ds_database_log ds_database_log_ds_database_config_id_executed_query_checkp_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_database_log
    ADD CONSTRAINT ds_database_log_ds_database_config_id_executed_query_checkp_key UNIQUE (ds_database_config_id, executed_query, checkpoint_before);


--
-- Name: ds_database_log ds_database_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_database_log
    ADD CONSTRAINT ds_database_log_pkey PRIMARY KEY (ds_database_log_id);


--
-- Name: ds_file_system_config ds_file_system_connections_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_file_system_config
    ADD CONSTRAINT ds_file_system_connections_pkey PRIMARY KEY (ds_file_system_config_id);


--
-- Name: ds_file_system_log ds_file_system_log_config_id_file_path_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_file_system_log
    ADD CONSTRAINT ds_file_system_log_config_id_file_path_key UNIQUE (ds_file_system_config_id, file_path);


--
-- Name: ds_file_system_log ds_file_system_processed_files_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_file_system_log
    ADD CONSTRAINT ds_file_system_processed_files_pkey PRIMARY KEY (ds_file_system_log_id);


--
-- Name: engine_execution_warnings engine_execution_warnings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.engine_execution_warnings
    ADD CONSTRAINT engine_execution_warnings_pkey PRIMARY KEY (warning_id);


--
-- Name: entity_attributes entity_attributes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_attributes
    ADD CONSTRAINT entity_attributes_pkey PRIMARY KEY (entity_attr_id);


--
-- Name: entity_fields entity_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_fields
    ADD CONSTRAINT entity_fields_pkey PRIMARY KEY (entity_field_id);


--
-- Name: entity_relation_fields entity_relation_fields_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_relation_fields
    ADD CONSTRAINT entity_relation_fields_pkey PRIMARY KEY (config_id);


--
-- Name: entity_relations entity_relations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_relations
    ADD CONSTRAINT entity_relations_pkey PRIMARY KEY (relation_id);


--
-- Name: entity_source_records entity_source_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_source_records
    ADD CONSTRAINT entity_source_records_pkey PRIMARY KEY (record_id);


--
-- Name: entity_update_rules entity_update_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_update_rules
    ADD CONSTRAINT entity_update_rules_pkey PRIMARY KEY (entity_update_rule_id);


--
-- Name: event_stream_groups event_stream_groups_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_stream_groups
    ADD CONSTRAINT event_stream_groups_pkey PRIMARY KEY (event_stream_id, aggregate_id);


--
-- Name: event_stream event_stream_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_stream
    ADD CONSTRAINT event_stream_pkey PRIMARY KEY (event_stream_id);


--
-- Name: exec_ds_mp exec_ds_mp_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.exec_ds_mp
    ADD CONSTRAINT exec_ds_mp_pkey PRIMARY KEY (exec_ds_mp_id);


--
-- Name: landing_records landing_raw_records_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.landing_records
    ADD CONSTRAINT landing_raw_records_pkey PRIMARY KEY (landing_record_id);


--
-- Name: mapped_storages mapped_data_storage_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mapped_storages
    ADD CONSTRAINT mapped_data_storage_pkey PRIMARY KEY (mapped_storage_id);


--
-- Name: pattern_relations pattern_relations_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pattern_relations
    ADD CONSTRAINT pattern_relations_pkey PRIMARY KEY (pattern_id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_user_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_user_id_key UNIQUE (user_id);


--
-- Name: entity_relation_rules relation_rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_relation_rules
    ADD CONSTRAINT relation_rules_pkey PRIMARY KEY (rule_id);


--
-- Name: risk_levels risk_levels_level_code_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risk_levels
    ADD CONSTRAINT risk_levels_level_code_key UNIQUE (level_code);


--
-- Name: risk_levels risk_levels_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.risk_levels
    ADD CONSTRAINT risk_levels_pkey PRIMARY KEY (risk_level_id);


--
-- Name: rules rules_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.rules
    ADD CONSTRAINT rules_pkey PRIMARY KEY (rule_id);


--
-- Name: scenario_rules scenario_aggregates_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scenario_rules
    ADD CONSTRAINT scenario_aggregates_pkey PRIMARY KEY (scenario_aggregate_id);


--
-- Name: scenarios scenarios_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scenarios
    ADD CONSTRAINT scenarios_pkey PRIMARY KEY (scenario_id);


--
-- Name: scenarios scenarios_scenario_name_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scenarios
    ADD CONSTRAINT scenarios_scenario_name_key UNIQUE (scenario_name);


--
-- Name: profiles schema_profiles_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT schema_profiles_pkey PRIMARY KEY (profile_id);


--
-- Name: sensors sensors_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.sensors
    ADD CONSTRAINT sensors_pkey PRIMARY KEY (sensor_id);


--
-- Name: standard_fields standard_fields_new_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.standard_fields
    ADD CONSTRAINT standard_fields_new_pkey PRIMARY KEY (standard_field_id);


--
-- Name: data_source_schemas uk_ds_original_field; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_source_schemas
    ADD CONSTRAINT uk_ds_original_field UNIQUE (data_source_id, field_name);


--
-- Name: entity_relation_rules uk_entity_relation_rules; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_relation_rules
    ADD CONSTRAINT uk_entity_relation_rules UNIQUE (data_source_id, from_entity_type, relation_type, to_entity_type);


--
-- Name: entity_relations uk_entity_relations; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_relations
    ADD CONSTRAINT uk_entity_relations UNIQUE (from_entity_type, from_entity_id, relation_type, to_entity_type, to_entity_id);


--
-- Name: pattern_relations uk_pattern; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.pattern_relations
    ADD CONSTRAINT uk_pattern UNIQUE (data_source_id, from_entity_type, from_id_field, relation_type, to_entity_type, to_id_field);


--
-- Name: profiles uk_profile_name; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT uk_profile_name UNIQUE (data_source_id, profile_name);


--
-- Name: entity_relation_fields uk_relation_field; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_relation_fields
    ADD CONSTRAINT uk_relation_field UNIQUE (data_source_id, field_name);


--
-- Name: derived_rules uq_derived_rules_datasource_targetfield; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.derived_rules
    ADD CONSTRAINT uq_derived_rules_datasource_targetfield UNIQUE (data_source_id, target_field);


--
-- Name: landing_records uq_landing_exec_row; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.landing_records
    ADD CONSTRAINT uq_landing_exec_row UNIQUE (exec_ds_mp_id, row_index);


--
-- Name: users users_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_email_key UNIQUE (email);


--
-- Name: users users_login_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_login_id_key UNIQUE (login_id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (user_id);


--
-- Name: idx_amount; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_amount ON public.event_stream USING btree ((((event_data ->> 'amount'::text))::numeric)) WHERE (event_data ? 'amount'::text);


--
-- Name: idx_api_config_datasource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_api_config_datasource ON public.ds_api_config USING btree (data_source_id, is_active);


--
-- Name: idx_api_config_last_fetch; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_api_config_last_fetch ON public.ds_api_config USING btree (last_fetched_at) WHERE (is_active = true);


--
-- Name: idx_api_config_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_api_config_status ON public.ds_api_config USING btree (connection_status);


--
-- Name: idx_api_log_config; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_api_log_config ON public.ds_api_log USING btree (ds_api_config_id, requested_at DESC);


--
-- Name: idx_api_log_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_api_log_status ON public.ds_api_log USING btree (processing_status);


--
-- Name: idx_api_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_api_log_time ON public.ds_api_log USING btree (requested_at DESC);


--
-- Name: idx_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_created_at ON public.event_stream USING btree (event_dt DESC);


--
-- Name: idx_data_profiles_ds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_data_profiles_ds ON public.profiles USING btree (data_source_id);


--
-- Name: idx_data_source_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_data_source_active ON public.data_sources USING btree (is_active);


--
-- Name: idx_data_source_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_data_source_type ON public.data_sources USING btree (source_type);


--
-- Name: idx_db_config_datasource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_db_config_datasource ON public.ds_database_config USING btree (data_source_id, is_active);


--
-- Name: idx_db_config_last_query; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_db_config_last_query ON public.ds_database_config USING btree (last_query_time) WHERE (is_active = true);


--
-- Name: idx_db_config_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_db_config_status ON public.ds_database_config USING btree (connection_status);


--
-- Name: idx_db_log_config; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_db_log_config ON public.ds_database_log USING btree (ds_database_config_id, execution_start_time DESC);


--
-- Name: idx_db_log_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_db_log_status ON public.ds_database_log USING btree (processing_status);


--
-- Name: idx_db_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_db_log_time ON public.ds_database_log USING btree (execution_start_time DESC);


--
-- Name: idx_dca_changed_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_dca_changed_at ON public.detection_config_audit USING btree (changed_at);


--
-- Name: idx_dca_changed_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_dca_changed_by ON public.detection_config_audit USING btree (changed_by);


--
-- Name: idx_dca_target; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_dca_target ON public.detection_config_audit USING btree (target_type, target_id);


--
-- Name: idx_derived_rules_datasource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_derived_rules_datasource ON public.derived_rules USING btree (data_source_id, is_active);


--
-- Name: idx_derived_rules_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_derived_rules_priority ON public.derived_rules USING btree (data_source_id, priority);


--
-- Name: idx_detect_actions_group_key; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_actions_group_key ON public.detect_actions USING btree (group_key);


--
-- Name: idx_detect_actions_requested_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_actions_requested_at ON public.detect_actions USING btree (requested_at DESC);


--
-- Name: idx_detect_actions_requested_by; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_actions_requested_by ON public.detect_actions USING btree (requested_by);


--
-- Name: idx_detect_actions_scenario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_actions_scenario ON public.detect_actions USING btree (detect_scenario_id);


--
-- Name: idx_detect_actions_scenario_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_actions_scenario_id ON public.detect_actions USING btree (scenario_id);


--
-- Name: idx_detect_actions_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_actions_status ON public.detect_actions USING btree (action_status);


--
-- Name: idx_detect_actions_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_actions_type ON public.detect_actions USING btree (action_type);


--
-- Name: idx_detect_rules_gk_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_rules_gk_time ON public.detect_rules USING btree (group_key, end_dt DESC);


--
-- Name: idx_detect_rules_key_anchor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_rules_key_anchor ON public.detect_rules USING btree (group_key, rule_id, detected_dt);


--
-- Name: idx_detect_rules_orig_gk; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_rules_orig_gk ON public.detect_rules USING btree (original_group_key);


--
-- Name: idx_detect_rules_rule_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_rules_rule_time ON public.detect_rules USING btree (rule_id, end_dt DESC);


--
-- Name: idx_detect_rules_tx_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_rules_tx_id ON public.detect_rules USING btree (transaction_id);


--
-- Name: idx_detect_scenarios_exec; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_scenarios_exec ON public.detect_scenarios USING btree (exec_ds_mp_id);


--
-- Name: idx_detect_scenarios_group_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_scenarios_group_time ON public.detect_scenarios USING btree (group_key, detected_dt DESC);


--
-- Name: idx_detect_scenarios_mapped_storage_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_scenarios_mapped_storage_id ON public.detect_scenarios USING btree (mapped_storage_id);


--
-- Name: idx_detect_scenarios_scenario_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_scenarios_scenario_time ON public.detect_scenarios USING btree (scenario_id, detected_dt DESC);


--
-- Name: idx_detect_scenarios_tx_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_scenarios_tx_id ON public.detect_scenarios USING btree (transaction_id);


--
-- Name: idx_detect_scn_exec; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_scn_exec ON public.detect_scenarios USING btree (exec_ds_mp_id);


--
-- Name: idx_detect_scn_key_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_scn_key_time ON public.detect_scenarios USING btree (group_key, detected_dt DESC);


--
-- Name: idx_detect_scn_scen_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_scn_scen_time ON public.detect_scenarios USING btree (scenario_id, detected_dt DESC);


--
-- Name: idx_detect_sensors_detected; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_sensors_detected ON public.detect_sensors USING btree (detected_dt);


--
-- Name: idx_detect_sensors_group_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_sensors_group_time ON public.detect_sensors USING btree (group_key, detected_dt DESC);


--
-- Name: idx_detect_sensors_mapped_storage_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_sensors_mapped_storage_id ON public.detect_sensors USING btree (mapped_storage_id);


--
-- Name: idx_detect_sensors_sensor; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_sensors_sensor ON public.detect_sensors USING btree (sensor_id);


--
-- Name: idx_detect_sensors_tx_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_detect_sensors_tx_id ON public.detect_sensors USING btree (transaction_id);


--
-- Name: idx_device_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_device_id ON public.event_stream USING btree (((event_data ->> 'device_id'::text))) WHERE (event_data ? 'device_id'::text);


--
-- Name: idx_domain_types_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_domain_types_active ON public.detection_areas USING btree (is_active);


--
-- Name: idx_domain_types_display_order; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_domain_types_display_order ON public.detection_areas USING btree (display_order);


--
-- Name: idx_ds_original_schemas_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_ds_original_schemas_active ON public.data_source_schemas USING btree (is_active);


--
-- Name: idx_ds_original_schemas_datasource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_ds_original_schemas_datasource ON public.data_source_schemas USING btree (data_source_id);


--
-- Name: idx_entity_attributes_discovered_from; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_attributes_discovered_from ON public.entity_attributes USING btree (discovered_from);


--
-- Name: idx_entity_attributes_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_attributes_status ON public.entity_attributes USING btree (status);


--
-- Name: idx_entity_attributes_unique; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX IF NOT EXISTS idx_entity_attributes_unique ON public.entity_attributes USING btree (entity_type, entity_id);


--
-- Name: idx_entity_attributes_updated; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_attributes_updated ON public.entity_attributes USING btree (updated_at);


--
-- Name: idx_entity_relations_from; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_relations_from ON public.entity_relations USING btree (from_entity_type, from_entity_id);


--
-- Name: idx_entity_relations_to; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_relations_to ON public.entity_relations USING btree (to_entity_type, to_entity_id);


--
-- Name: idx_entity_relations_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_relations_type ON public.entity_relations USING btree (relation_type);


--
-- Name: idx_entity_source_datasource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_source_datasource ON public.entity_source_records USING btree (data_source_id, received_at DESC);


--
-- Name: idx_entity_source_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_source_lookup ON public.entity_source_records USING btree (entity_type, entity_id, received_at DESC);


--
-- Name: idx_entity_source_received_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_source_received_at ON public.entity_source_records USING btree (received_at DESC);


--
-- Name: idx_entity_source_tx_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_source_tx_id ON public.entity_source_records USING btree (source_tx_id) WHERE (source_tx_id IS NOT NULL);


--
-- Name: idx_entity_update_rules_data_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_update_rules_data_source ON public.entity_update_rules USING btree (data_source_id) WHERE ((is_active = true) AND ((trigger_type)::text = 'EVENT'::text));


--
-- Name: idx_entity_update_rules_entity_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_update_rules_entity_type ON public.entity_update_rules USING btree (entity_type) WHERE (is_active = true);


--
-- Name: idx_entity_update_rules_scenario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_update_rules_scenario ON public.entity_update_rules USING btree (scenario_id) WHERE (is_active = true);


--
-- Name: idx_entity_update_rules_trigger_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_entity_update_rules_trigger_type ON public.entity_update_rules USING btree (trigger_type) WHERE (is_active = true);


--
-- Name: idx_event_data_gin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_data_gin ON public.event_stream USING gin (event_data);


--
-- Name: idx_event_stream_account_open_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_stream_account_open_type ON public.event_stream USING btree (((event_data ->> 'account_open_type'::text))) WHERE (event_data ? 'account_open_type'::text);


--
-- Name: idx_event_stream_auth_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_stream_auth_type ON public.event_stream USING btree (((event_data ->> 'auth_type'::text))) WHERE (event_data ? 'auth_type'::text);


--
-- Name: idx_event_stream_device_uuid; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_stream_device_uuid ON public.event_stream USING btree (((event_data ->> 'device_uuid'::text))) WHERE (event_data ? 'device_uuid'::text);


--
-- Name: idx_event_stream_event_dt; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_stream_event_dt ON public.event_stream USING btree (event_dt);


--
-- Name: idx_event_stream_event_dt_range; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_stream_event_dt_range ON public.event_stream USING btree (event_dt DESC);


--
-- Name: idx_event_stream_groups_event_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_stream_groups_event_id ON public.event_stream_groups USING btree (event_stream_id);


--
-- Name: idx_event_stream_groups_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_stream_groups_lookup ON public.event_stream_groups USING btree (aggregate_id, group_key);


--
-- Name: idx_event_stream_groups_mapped_storage; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_stream_groups_mapped_storage ON public.event_stream_groups USING btree (mapped_storage_id, aggregate_id);


--
-- Name: idx_event_stream_mapped_storage; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_stream_mapped_storage ON public.event_stream USING btree (mapped_storage_id);


--
-- Name: idx_event_stream_transaction_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_stream_transaction_type ON public.event_stream USING btree (((event_data ->> 'transaction_type'::text))) WHERE (event_data ? 'transaction_type'::text);


--
-- Name: idx_event_stream_tx_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_event_stream_tx_id ON public.event_stream USING btree (transaction_id);


--
-- Name: idx_exec_ds_mp_datasource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_exec_ds_mp_datasource ON public.exec_ds_mp USING btree (data_source_id, start_dt DESC);


--
-- Name: idx_file_config_datasource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_file_config_datasource ON public.ds_file_system_config USING btree (data_source_id, is_active);


--
-- Name: idx_file_config_last_scan; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_file_config_last_scan ON public.ds_file_system_config USING btree (last_scan_time) WHERE (is_active = true);


--
-- Name: idx_file_config_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_file_config_status ON public.ds_file_system_config USING btree (connection_status);


--
-- Name: idx_file_system_log_config; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_file_system_log_config ON public.ds_file_system_log USING btree (ds_file_system_config_id, processed_at DESC);


--
-- Name: idx_file_system_log_lookup; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_file_system_log_lookup ON public.ds_file_system_log USING btree (ds_file_system_config_id, file_name);


--
-- Name: idx_file_system_log_time; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_file_system_log_time ON public.ds_file_system_log USING btree (processed_at DESC);


--
-- Name: idx_ip; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_ip ON public.event_stream USING btree (((event_data ->> 'ip'::text))) WHERE (event_data ? 'ip'::text);


--
-- Name: idx_landing_records_datasource; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_landing_records_datasource ON public.landing_records USING btree (data_source_id, extracted_at DESC);


--
-- Name: idx_landing_records_extracted_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_landing_records_extracted_at ON public.landing_records USING btree (extracted_at DESC);


--
-- Name: idx_landing_records_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_landing_records_status ON public.landing_records USING btree (ingestion_status);


--
-- Name: idx_mapped_storage_landing; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_mapped_storage_landing ON public.mapped_storages USING btree (landing_record_id);


--
-- Name: idx_mapped_storages_exec_ds_mp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_mapped_storages_exec_ds_mp ON public.mapped_storages USING btree (exec_ds_mp_id);


--
-- Name: idx_mapped_storages_tx_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_mapped_storages_tx_id ON public.mapped_storages USING btree (transaction_id);


--
-- Name: idx_pattern_approval_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_pattern_approval_status ON public.pattern_relations USING btree (approval_status);


--
-- Name: idx_pattern_confidence; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_pattern_confidence ON public.pattern_relations USING btree (confidence_score DESC);


--
-- Name: idx_pattern_created_at; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_pattern_created_at ON public.pattern_relations USING btree (created_at DESC);


--
-- Name: idx_pattern_data_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_pattern_data_source ON public.pattern_relations USING btree (data_source_id);


--
-- Name: idx_profiles_entity_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_profiles_entity_type ON public.profiles USING btree (destination_type, entity_type) WHERE ((destination_type)::text = 'ENTITY_ATTRIBUTES'::text);


--
-- Name: idx_refresh_token; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_refresh_token ON public.refresh_tokens USING btree (token);


--
-- Name: idx_refresh_token_user_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_refresh_token_user_id ON public.refresh_tokens USING btree (user_id);


--
-- Name: idx_relation_field_data_source; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_relation_field_data_source ON public.entity_relation_fields USING btree (data_source_id);


--
-- Name: idx_relation_field_enabled; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_relation_field_enabled ON public.entity_relation_fields USING btree (is_enabled);


--
-- Name: idx_relation_field_priority; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_relation_field_priority ON public.entity_relation_fields USING btree (priority DESC);


--
-- Name: idx_relation_rules_ds_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_relation_rules_ds_active ON public.entity_relation_rules USING btree (data_source_id, is_active);


--
-- Name: idx_scenarios_entity_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_scenarios_entity_type ON public.scenarios USING btree (primary_entity_type) WHERE (is_active = true);


--
-- Name: idx_schema_profiles_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_schema_profiles_active ON public.profiles USING btree (data_source_id, is_active);


--
-- Name: idx_schema_profiles_ds; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_schema_profiles_ds ON public.profiles USING btree (data_source_id);


--
-- Name: idx_sensors_is_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_sensors_is_active ON public.sensors USING btree (is_active);


--
-- Name: idx_standard_fields_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_standard_fields_active ON public.standard_fields USING btree (is_active);


--
-- Name: idx_standard_fields_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_standard_fields_category ON public.standard_fields USING btree (category);


--
-- Name: idx_standard_fields_data_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_standard_fields_data_type ON public.standard_fields USING btree (data_type);


--
-- Name: idx_user_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_user_email ON public.users USING btree (email);


--
-- Name: idx_user_username; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_user_username ON public.users USING btree (user_name);


--
-- Name: idx_warn_exec; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_warn_exec ON public.engine_execution_warnings USING btree (exec_ds_mp_id);


--
-- Name: idx_warn_step; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX IF NOT EXISTS idx_warn_step ON public.engine_execution_warnings USING btree (step);


--
-- Name: uq_detect_rules_dedup; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX IF NOT EXISTS uq_detect_rules_dedup ON public.detect_rules USING btree (group_key, rule_id, detected_dt);


--
-- Name: uq_scenario_agg; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX IF NOT EXISTS uq_scenario_agg ON public.scenario_rules USING btree (scenario_id, rule_id);


--
-- Name: entity_relation_fields trigger_update_entity_relation_fields_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trigger_update_entity_relation_fields_updated_at BEFORE UPDATE ON public.entity_relation_fields FOR EACH ROW EXECUTE FUNCTION public.update_entity_relation_fields_updated_at();


--
-- Name: pattern_relations trigger_update_pattern_relations_updated_at; Type: TRIGGER; Schema: public; Owner: -
--

CREATE TRIGGER trigger_update_pattern_relations_updated_at BEFORE UPDATE ON public.pattern_relations FOR EACH ROW EXECUTE FUNCTION public.update_pattern_relations_updated_at();


--
-- Name: ds_api_config ds_api_config_data_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_api_config
    ADD CONSTRAINT ds_api_config_data_source_id_fkey FOREIGN KEY (data_source_id) REFERENCES public.data_sources(data_source_id);


--
-- Name: ds_api_log ds_api_log_ds_api_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_api_log
    ADD CONSTRAINT ds_api_log_ds_api_config_id_fkey FOREIGN KEY (ds_api_config_id) REFERENCES public.ds_api_config(ds_api_config_id);


--
-- Name: ds_database_config ds_database_config_data_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_database_config
    ADD CONSTRAINT ds_database_config_data_source_id_fkey FOREIGN KEY (data_source_id) REFERENCES public.data_sources(data_source_id);


--
-- Name: ds_database_log ds_database_log_ds_database_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_database_log
    ADD CONSTRAINT ds_database_log_ds_database_config_id_fkey FOREIGN KEY (ds_database_config_id) REFERENCES public.ds_database_config(ds_database_config_id);


--
-- Name: ds_file_system_config ds_file_system_connections_data_source_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_file_system_config
    ADD CONSTRAINT ds_file_system_connections_data_source_id_fkey FOREIGN KEY (data_source_id) REFERENCES public.data_sources(data_source_id);


--
-- Name: ds_file_system_log ds_file_system_log_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ds_file_system_log
    ADD CONSTRAINT ds_file_system_log_config_id_fkey FOREIGN KEY (ds_file_system_config_id) REFERENCES public.ds_file_system_config(ds_file_system_config_id);


--
-- Name: event_stream_groups event_stream_groups_event_stream_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_stream_groups
    ADD CONSTRAINT event_stream_groups_event_stream_id_fkey FOREIGN KEY (event_stream_id) REFERENCES public.event_stream(event_stream_id) ON DELETE CASCADE;


--
-- Name: detect_actions fk_detect_actions_scenario; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detect_actions
    ADD CONSTRAINT fk_detect_actions_scenario FOREIGN KEY (detect_scenario_id) REFERENCES public.detect_scenarios(detect_scenario_id) ON DELETE CASCADE;


--
-- Name: detect_scenarios fk_detect_scenarios_mapped_storage; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detect_scenarios
    ADD CONSTRAINT fk_detect_scenarios_mapped_storage FOREIGN KEY (mapped_storage_id) REFERENCES public.mapped_storages(mapped_storage_id) ON DELETE CASCADE;


--
-- Name: detect_sensors fk_detect_sensors_mapped_storage; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.detect_sensors
    ADD CONSTRAINT fk_detect_sensors_mapped_storage FOREIGN KEY (mapped_storage_id) REFERENCES public.mapped_storages(mapped_storage_id);


--
-- Name: data_source_schemas fk_ds_original_schema_datasource; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.data_source_schemas
    ADD CONSTRAINT fk_ds_original_schema_datasource FOREIGN KEY (data_source_id) REFERENCES public.data_sources(data_source_id);


--
-- Name: entity_source_records fk_entity_source_mapped_storage; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.entity_source_records
    ADD CONSTRAINT fk_entity_source_mapped_storage FOREIGN KEY (mapped_storage_id) REFERENCES public.mapped_storages(mapped_storage_id) ON DELETE SET NULL;


--
-- Name: event_stream_groups fk_event_stream_groups_mapped_storage; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event_stream_groups
    ADD CONSTRAINT fk_event_stream_groups_mapped_storage FOREIGN KEY (mapped_storage_id) REFERENCES public.mapped_storages(mapped_storage_id) ON DELETE CASCADE;


--
-- Name: mapped_storages fk_mapped_storage_landing; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mapped_storages
    ADD CONSTRAINT fk_mapped_storage_landing FOREIGN KEY (landing_record_id) REFERENCES public.landing_records(landing_record_id) ON DELETE SET NULL;


--
-- Name: mapped_storages fk_mapped_storages_exec_ds_mp; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.mapped_storages
    ADD CONSTRAINT fk_mapped_storages_exec_ds_mp FOREIGN KEY (exec_ds_mp_id) REFERENCES public.exec_ds_mp(exec_ds_mp_id);


--
-- Name: refresh_tokens fk_refresh_token_user; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES public.users(user_id) ON DELETE CASCADE;


--
-- Name: profiles fk_sp_data_source; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.profiles
    ADD CONSTRAINT fk_sp_data_source FOREIGN KEY (data_source_id) REFERENCES public.data_sources(data_source_id) ON DELETE CASCADE;


--
-- Name: landing_records landing_raw_records_exec_ds_mp_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.landing_records
    ADD CONSTRAINT landing_raw_records_exec_ds_mp_id_fkey FOREIGN KEY (exec_ds_mp_id) REFERENCES public.exec_ds_mp(exec_ds_mp_id) ON DELETE CASCADE;


--
-- Name: scenarios scenarios_detection_area_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scenarios
    ADD CONSTRAINT scenarios_detection_area_id_fkey FOREIGN KEY (detection_area_id) REFERENCES public.detection_areas(detection_area_id);


--
-- Name: scenarios scenarios_risk_level_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scenarios
    ADD CONSTRAINT scenarios_risk_level_id_fkey FOREIGN KEY (risk_level_id) REFERENCES public.risk_levels(risk_level_id);


--


-- ============================================================
-- [2026-04-17] ds_database_config agent_id 컬럼 추가 (V003)
-- ============================================================
ALTER TABLE IF EXISTS ds_database_config
    ADD COLUMN IF NOT EXISTS agent_id VARCHAR(100) NULL;



-- ============================================================
-- [2026-04-24] 마스터 데이터 초기 적재
-- ============================================================

-- risk_levels: RiskLevel enum (icon-common) 과 동기화
-- ON CONFLICT DO NOTHING → 재실행 안전
INSERT INTO risk_levels
    (risk_level_id, level_code, level_name, description, action_type, notification_required, display_order, is_active)
VALUES
    ('MONITOR',   10, '모니터링',    '일반 모니터링 대상 — 정기적 확인 필요',      'ALERT', false, 1, true),
    ('INTENSIVE', 20, '집중모니터링', '집중 관찰 필요 — 담당자 주의 요망',          'ALERT', true,  2, true),
    ('REVIEW',    30, '심사',        '담당자 심사 필요 — 거래 보류 가능',           'HOLD',  true,  3, true),
    ('BLOCK',     40, '차단',        '거래 차단 — 즉시 조치 필요',                 'BLOCK', true,  4, true)
ON CONFLICT (risk_level_id) DO NOTHING;

-- [2026-04-24] entity_fields 마스터 데이터 초기 적재
INSERT INTO entity_fields
    (entity_field_id, display_name, data_type, description, is_active, created_by)
VALUES
    ('customer_age',    '고객 나이',       'NUMBER', '고객의 나이 (만 나이)',              true, 'SYSTEM'),
    ('grade',           '고객 등급',       'STRING', '고객 등급 (VIP, VVIP 등)',           true, 'SYSTEM'),
    ('owned_accounts',  '소유 계좌 목록',  'ARRAY',  '고객이 소유한 계좌 ID 목록',         true, 'SYSTEM'),
    ('region',          '지역',            'STRING', '고객 거주 지역',                     true, 'SYSTEM'),
    ('account_number',  '계좌 번호',       'STRING', '계좌 번호',                         true, 'SYSTEM'),
    ('account_type',    '계좌 유형',       'STRING', '계좌 유형 (입출금, 예금 등)',         true, 'SYSTEM'),
    ('open_date',       '개설일',          'DATE',   '계좌 개설 일자',                     true, 'SYSTEM'),
    ('open_type',       '개설 방법',       'STRING', '계좌 개설 방법 (대면/비대면)',        true, 'SYSTEM'),
    ('owner_id',        '소유자 ID',       'STRING', '계좌 소유 고객 ID',                  true, 'SYSTEM'),
    ('status',          '계좌 상태',       'STRING', '계좌 상태 (ACTIVE/CLOSED)',          true, 'SYSTEM')
ON CONFLICT (entity_field_id) DO NOTHING;
