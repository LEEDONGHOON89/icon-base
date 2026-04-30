package com.itmasters.icon.api.aichat.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

/**
 * AI Chat Session 커스텀 Repository 인터페이스
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
public interface AiChatSessionRepositoryCustom {

    /**
     * 사용자 ID로 최근 N개 세션 조회
     *
     * @param userId 사용자 ID
     * @param limit 조회할 세션 수
     * @return 최근 세션 목록
     */
    List<AiChatSessionEntity> findRecentSessionsByUserId(String userId, int limit);

    /**
     * 세션과 메시지를 함께 조회 (fetch join)
     *
     * @param sessionId 세션 ID
     * @return 세션 (메시지 포함)
     */
    Optional<AiChatSessionEntity> findByIdWithMessages(Long sessionId);

    /**
     * 외부 세션 ID로 세션과 메시지를 함께 조회
     *
     * @param externalSessionId 외부 세션 ID
     * @return 세션 (메시지 포함)
     */
    Optional<AiChatSessionEntity> findByExternalSessionIdWithMessages(String externalSessionId);
}
