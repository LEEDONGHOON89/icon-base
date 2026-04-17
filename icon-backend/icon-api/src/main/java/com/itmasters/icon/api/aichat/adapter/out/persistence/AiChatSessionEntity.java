package com.itmasters.icon.api.aichat.adapter.out.persistence;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 채팅 세션 Entity
 */
@Entity
@Table(name = "ai_chat_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "external_session_id", nullable = false, unique = true, length = 100)
    private String externalSessionId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<AiChatMessageEntity> messages = new ArrayList<>();

    /**
     * 새 세션 생성 팩토리 메서드
     */
    public static AiChatSessionEntity create(String externalSessionId, String userId, String title) {
        AiChatSessionEntity entity = new AiChatSessionEntity();
        entity.externalSessionId = externalSessionId;
        entity.userId = userId;
        entity.title = title;
        entity.createdAt = LocalDateTime.now();
        entity.updatedAt = LocalDateTime.now();
        return entity;
    }

    /**
     * 세션 제목 업데이트
     */
    public void updateTitle(String title) {
        this.title = title;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 업데이트 시간 갱신
     */
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 메시지 추가
     */
    public void addMessage(AiChatMessageEntity message) {
        messages.add(message);
        message.setSession(this);
        this.updatedAt = LocalDateTime.now();
    }
}
