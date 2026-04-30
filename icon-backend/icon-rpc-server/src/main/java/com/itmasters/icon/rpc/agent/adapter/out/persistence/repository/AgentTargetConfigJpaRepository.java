package com.itmasters.icon.rpc.agent.adapter.out.persistence.repository;

import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentTargetConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentTargetConfigJpaRepository extends JpaRepository<AgentTargetConfigEntity, String> {

    List<AgentTargetConfigEntity> findByAgentId(String agentId);

    List<AgentTargetConfigEntity> findByAgentIdAndIsActive(String agentId, boolean isActive);

    Optional<AgentTargetConfigEntity> findByAgentIdAndTargetId(String agentId, String targetId);

    boolean existsByAgentIdAndTargetId(String agentId, String targetId);

    // [2026-04-24] 에이전트 삭제 시 해당 에이전트의 타겟 설정 일괄 삭제
    void deleteByAgentId(String agentId);
}
