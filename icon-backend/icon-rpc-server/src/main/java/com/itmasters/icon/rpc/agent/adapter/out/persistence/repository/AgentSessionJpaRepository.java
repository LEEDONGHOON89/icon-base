package com.itmasters.icon.rpc.agent.adapter.out.persistence.repository;

import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentSessionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AgentSessionJpaRepository extends JpaRepository<AgentSessionEntity, String> {

    List<AgentSessionEntity> findByAgentIdOrderByConnectedAtDesc(String agentId, Pageable pageable);

    Optional<AgentSessionEntity> findBySessionIdAndStatus(String sessionId, String status);

    // [2026-04-21] 활성 세션 수 조회 — agentId 중복 연결 감지용
    long countByAgentIdAndStatus(String agentId, String status);

    // [2026-04-21] 기존 활성 세션의 원격 주소 조회 — 중복 오류 메시지에 포함
    Optional<AgentSessionEntity> findFirstByAgentIdAndStatusOrderByConnectedAtDesc(String agentId, String status);

    @Modifying
    @Query("UPDATE AgentSessionEntity s SET s.status = 'DISCONNECTED', s.disconnectedAt = CURRENT_TIMESTAMP, s.disconnectReason = :reason WHERE s.agentId = :agentId AND s.status = 'CONNECTED'")
    int disconnectAllByAgentId(@Param("agentId") String agentId, @Param("reason") String reason);

    /** 서버 기동 시 모든 CONNECTED 세션을 DISCONNECTED로 초기화 */
    @Modifying
    @Query("UPDATE AgentSessionEntity s SET s.status = 'DISCONNECTED', s.disconnectedAt = CURRENT_TIMESTAMP, s.disconnectReason = :reason WHERE s.status = 'CONNECTED'")
    int disconnectAllSessions(@Param("reason") String reason);
}
