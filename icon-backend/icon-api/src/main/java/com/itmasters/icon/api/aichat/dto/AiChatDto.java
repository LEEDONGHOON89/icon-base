package com.itmasters.icon.api.aichat.dto;

import com.itmasters.icon.api.aichat.adapter.out.persistence.AiChatMessageEntity;
import com.itmasters.icon.api.aichat.adapter.out.persistence.AiChatSessionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class AiChatDto {

    /**
     * 채팅 메시지 전송 요청
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageRequest {
        private String sessionId;  // null이면 새 세션 생성
        private String message;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendMessageResponse {
        private String sessionId;
        private String response;
        private LocalDateTime timestamp;
        private Boolean isError;
    }

    /**
     * 세션 요약 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionSummary {
        private Long id;
        private String sessionId;
        private String title;
        private int messageCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static SessionSummary from(AiChatSessionEntity entity) {
            return SessionSummary.builder()
                    .id(entity.getSessionId())
                    .sessionId(entity.getExternalSessionId())
                    .title(entity.getTitle())
                    .messageCount(entity.getMessages().size())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 세션 상세 정보 (메시지 포함)
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionDetail {
        private Long id;
        private String sessionId;
        private String title;
        private List<Message> messages;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static SessionDetail from(AiChatSessionEntity entity) {
            return SessionDetail.builder()
                    .id(entity.getSessionId())
                    .sessionId(entity.getExternalSessionId())
                    .title(entity.getTitle())
                    .messages(entity.getMessages().stream()
                            .map(Message::from)
                            .collect(Collectors.toList()))
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 메시지 정보
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private Long id;
        private String role;
        private String content;
        private LocalDateTime createdAt;

        public static Message from(AiChatMessageEntity entity) {
            return Message.builder()
                    .id(entity.getMessageId())
                    .role(entity.getRole().name().toLowerCase())
                    .content(entity.getContent())
                    .createdAt(entity.getCreatedAt())
                    .build();
        }
    }

    /**
     * 세션 제목 업데이트 요청
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateTitleRequest {
        private String title;
    }
}
