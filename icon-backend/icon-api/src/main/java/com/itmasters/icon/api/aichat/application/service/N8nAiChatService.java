package com.itmasters.icon.api.aichat.application.service;

import com.itmasters.icon.api.aichat.adapter.out.persistence.AiChatMessageEntity;
import com.itmasters.icon.api.aichat.adapter.out.persistence.AiChatSessionEntity;
import com.itmasters.icon.api.aichat.adapter.out.persistence.AiChatSessionRepository;
import com.itmasters.icon.api.aichat.dto.AiChatDto;
import com.itmasters.icon.api.common.client.AiWebhookClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class N8nAiChatService {

    private final AiChatSessionRepository sessionRepository;
    private final AiWebhookClient webhookClient;

//    @Value("${ai.chat.webhook.url}")
    private String webhookUrl;

    /**
     * 메시지 전송 및 AI 응답 받기
     */
    @Transactional
    public AiChatDto.SendMessageResponse sendMessage(String userId, AiChatDto.SendMessageRequest request) {
        // 세션 조회 또는 생성
        AiChatSessionEntity session = getOrCreateSession(userId, request.getSessionId());

        // 사용자 메시지 저장
        AiChatMessageEntity userMessage = AiChatMessageEntity.userMessage(request.getMessage());
        session.addMessage(userMessage);

        // 외부 AI API 호출
        String aiResponse = callAiWebhook(session.getExternalSessionId(), request.getMessage());

        // AI 응답 메시지 저장
        AiChatMessageEntity assistantMessage = AiChatMessageEntity.assistantMessage(aiResponse);
        session.addMessage(assistantMessage);

        // 첫 메시지인 경우 제목 자동 설정
        if (session.getTitle() == null || session.getTitle().isEmpty()) {
            String title = generateTitle(request.getMessage());
            session.updateTitle(title);
        }

        sessionRepository.save(session);

        return AiChatDto.SendMessageResponse.builder()
                .sessionId(session.getExternalSessionId())
                .response(aiResponse)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 세션 조회 또는 생성
     */
    private AiChatSessionEntity getOrCreateSession(String userId, String sessionId) {
        if (sessionId != null && !sessionId.isEmpty()) {
            return sessionRepository.findByExternalSessionId(sessionId)
                    .orElseGet(() -> createNewSession(userId, sessionId));
        }
        return createNewSession(userId, null);
    }

    /**
     * 새 세션 생성
     */
    private AiChatSessionEntity createNewSession(String userId, String sessionId) {
        String externalSessionId = sessionId != null ? sessionId : generateSessionId(userId);
        return AiChatSessionEntity.create(externalSessionId, userId, null);
    }

    /**
     * 세션 ID 생성
     */
    private String generateSessionId(String userId) {
        return String.format("%s-%s", userId, UUID.randomUUID().toString().substring(0, 8));
    }

    /**
     * 외부 AI Webhook 호출
     */
    private String callAiWebhook(String sessionId, String message) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("message", message);
            body.put("sessionId", sessionId);

            log.info("Calling AI webhook for session: {}", sessionId);
            String answer = webhookClient.postAndExtractField(webhookUrl, body, "answer");
            log.info("AI response received for session: {}", sessionId);

            return answer != null ? answer : "응답을 받지 못했습니다.";
        } catch (Exception e) {
            log.error("AI webhook call failed: {}", e.getMessage(), e);
            return "AI 서비스 연결에 실패했습니다. 잠시 후 다시 시도해주세요.";
        }
    }

    /**
     * 제목 자동 생성 (첫 메시지 기반)
     */
    private String generateTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.isEmpty()) {
            return "새 대화";
        }
        // 첫 50자까지만 사용
        int maxLength = 50;
        if (firstMessage.length() <= maxLength) {
            return firstMessage;
        }
        return firstMessage.substring(0, maxLength) + "...";
    }

    /**
     * 사용자의 세션 목록 조회
     */
    public List<AiChatDto.SessionSummary> getUserSessions(String userId) {
        return sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(AiChatDto.SessionSummary::from)
                .collect(Collectors.toList());
    }

    /**
     * 세션 상세 조회 (메시지 포함)
     */
    public AiChatDto.SessionDetail getSessionDetail(String sessionId) {
        AiChatSessionEntity session = sessionRepository.findByExternalSessionIdWithMessages(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
        return AiChatDto.SessionDetail.from(session);
    }

    /**
     * 세션 제목 업데이트
     */
    @Transactional
    public void updateSessionTitle(String sessionId, String title) {
        AiChatSessionEntity session = sessionRepository.findByExternalSessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
        session.updateTitle(title);
        sessionRepository.save(session);
    }

    /**
     * 세션 삭제
     */
    @Transactional
    public void deleteSession(String sessionId) {
        AiChatSessionEntity session = sessionRepository.findByExternalSessionId(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("세션을 찾을 수 없습니다: " + sessionId));
        sessionRepository.delete(session);
    }

    /**
     * 새 세션 시작
     */
    @Transactional
    public AiChatDto.SessionSummary createSession(String userId) {
        String externalSessionId = generateSessionId(userId);
        AiChatSessionEntity session = AiChatSessionEntity.create(externalSessionId, userId, "새 대화");
        session = sessionRepository.save(session);
        return AiChatDto.SessionSummary.from(session);
    }
}
