package com.itmasters.icon.api.aichat.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI Chat Session Repository
 *
 * QueryDSL 기반 복잡한 쿼리는 AiChatSessionRepositoryCustom에서 구현
 */
@Repository
public interface AiChatSessionRepository extends JpaRepository<AiChatSessionEntity, Long>, AiChatSessionRepositoryCustom {

    /**
     * 외부 세션 ID로 세션 조회
     */
    Optional<AiChatSessionEntity> findByExternalSessionId(String externalSessionId);

    /**
     * 사용자 ID로 세션 목록 조회 (최신순)
     */
    List<AiChatSessionEntity> findByUserIdOrderByUpdatedAtDesc(String userId);

    /**
     * 사용자 ID로 최근 N개 세션 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - AiChatSessionRepositoryCustom 인터페이스에서 선언, AiChatSessionRepositoryImpl에서 구현
    // List<AiChatSessionEntity> findRecentSessionsByUserId(String userId, int limit);

    /**
     * 세션과 메시지를 함께 조회 (fetch join) - QueryDSL로 구현
     */
    // @Query 제거됨 - AiChatSessionRepositoryCustom 인터페이스에서 선언, AiChatSessionRepositoryImpl에서 구현
    // Optional<AiChatSessionEntity> findByIdWithMessages(Long sessionId);

    /**
     * 외부 세션 ID로 세션과 메시지를 함께 조회 - QueryDSL로 구현
     */
    // @Query 제거됨 - AiChatSessionRepositoryCustom 인터페이스에서 선언, AiChatSessionRepositoryImpl에서 구현
    // Optional<AiChatSessionEntity> findByExternalSessionIdWithMessages(String externalSessionId);
}
