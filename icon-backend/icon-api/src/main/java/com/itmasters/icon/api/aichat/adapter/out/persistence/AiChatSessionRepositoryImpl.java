package com.itmasters.icon.api.aichat.adapter.out.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * AI Chat Session 커스텀 Repository 구현체
 * QueryDSL을 사용한 복잡한 쿼리 처리
 */
@Repository
@RequiredArgsConstructor
public class AiChatSessionRepositoryImpl implements AiChatSessionRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<AiChatSessionEntity> findRecentSessionsByUserId(String userId, int limit) {
        QAiChatSessionEntity q = QAiChatSessionEntity.aiChatSessionEntity;

        return queryFactory
                .selectFrom(q)
                .where(q.userId.eq(userId))
                .orderBy(q.updatedAt.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public Optional<AiChatSessionEntity> findByIdWithMessages(Long sessionId) {
        QAiChatSessionEntity q = QAiChatSessionEntity.aiChatSessionEntity;

        AiChatSessionEntity result = queryFactory
                .selectFrom(q)
                .leftJoin(q.messages).fetchJoin()
                .where(q.sessionId.eq(sessionId))
                .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public Optional<AiChatSessionEntity> findByExternalSessionIdWithMessages(String externalSessionId) {
        QAiChatSessionEntity q = QAiChatSessionEntity.aiChatSessionEntity;

        AiChatSessionEntity result = queryFactory
                .selectFrom(q)
                .leftJoin(q.messages).fetchJoin()
                .where(q.externalSessionId.eq(externalSessionId))
                .fetchOne();

        return Optional.ofNullable(result);
    }
}
