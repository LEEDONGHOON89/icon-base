package com.itmasters.icon.rpc.agent.adapter.out.persistence.repository;

import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentEntity;
import com.itmasters.icon.rpc.agent.domain.AgentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AgentJpaRepository extends JpaRepository<AgentEntity, String> {

    List<AgentEntity> findAllByOrderByLastConnectedAtDesc();

    List<AgentEntity> findByStatus(AgentStatus status);

    /** 서버 기동 시 ACTIVE 상태 에이전트를 모두 DISCONNECTED로 초기화 */
    @Modifying
    @Query("UPDATE AgentEntity a SET a.status = 'DISCONNECTED', a.lastDisconnectedAt = CURRENT_TIMESTAMP WHERE a.status = 'ACTIVE'")
    int markAllActiveAsDisconnected();
}
