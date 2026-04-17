-- AI 채팅 세션 테이블
CREATE TABLE IF NOT EXISTS ai_chat_sessions (
    session_id BIGSERIAL PRIMARY KEY,
    external_session_id VARCHAR(100) NOT NULL UNIQUE,
    user_id VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_user_id ON ai_chat_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_ai_chat_sessions_updated_at ON ai_chat_sessions(updated_at DESC);

-- AI 채팅 메시지 테이블
CREATE TABLE IF NOT EXISTS ai_chat_messages (
    message_id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES ai_chat_sessions(session_id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 인덱스 생성
CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_session_id ON ai_chat_messages(session_id);
CREATE INDEX IF NOT EXISTS idx_ai_chat_messages_created_at ON ai_chat_messages(created_at);

-- 코멘트 추가
COMMENT ON TABLE ai_chat_sessions IS 'AI 서포트 채팅 세션';
COMMENT ON COLUMN ai_chat_sessions.session_id IS '내부 세션 ID (PK)';
COMMENT ON COLUMN ai_chat_sessions.external_session_id IS '외부 연동용 세션 ID';
COMMENT ON COLUMN ai_chat_sessions.user_id IS '사용자 ID';
COMMENT ON COLUMN ai_chat_sessions.title IS '세션 제목 (첫 메시지 기반 자동생성)';
COMMENT ON COLUMN ai_chat_sessions.created_at IS '생성일시';
COMMENT ON COLUMN ai_chat_sessions.updated_at IS '최종 업데이트일시';

COMMENT ON TABLE ai_chat_messages IS 'AI 서포트 채팅 메시지';
COMMENT ON COLUMN ai_chat_messages.message_id IS '메시지 ID (PK)';
COMMENT ON COLUMN ai_chat_messages.session_id IS '세션 ID (FK)';
COMMENT ON COLUMN ai_chat_messages.role IS '메시지 발신자 (USER/ASSISTANT)';
COMMENT ON COLUMN ai_chat_messages.content IS '메시지 내용';
COMMENT ON COLUMN ai_chat_messages.created_at IS '생성일시';
