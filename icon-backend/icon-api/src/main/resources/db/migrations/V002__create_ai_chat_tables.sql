-- [2026-04-17] V002: AI 채팅 테이블
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
