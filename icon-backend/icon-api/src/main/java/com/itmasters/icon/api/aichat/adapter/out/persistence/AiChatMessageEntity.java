package com.itmasters.icon.api.aichat.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * AI 채팅 메시지 Entity
 */
@Entity
@Table(name = "ai_chat_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatMessageEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private AiChatSessionEntity session;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private MessageRole role;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 메시지 역할
     */
    public enum MessageRole {
        USER,
        ASSISTANT
    }

    /**
     * 새 메시지 생성 팩토리 메서드
     */
    public static AiChatMessageEntity create(MessageRole role, String content) {
        AiChatMessageEntity entity = new AiChatMessageEntity();
        entity.role = role;
        entity.content = content;
        entity.createdAt = LocalDateTime.now();
        return entity;
    }

    /**
     * 사용자 메시지 생성
     */
    public static AiChatMessageEntity userMessage(String content) {
        return create(MessageRole.USER, content);
    }

    /**
     * AI 응답 메시지 생성
     */
    public static AiChatMessageEntity assistantMessage(String content) {
        return create(MessageRole.ASSISTANT, content);
    }
}
