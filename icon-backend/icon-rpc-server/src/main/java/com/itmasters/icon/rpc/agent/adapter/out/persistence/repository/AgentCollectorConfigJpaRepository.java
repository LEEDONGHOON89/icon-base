package com.itmasters.icon.rpc.agent.adapter.out.persistence.repository;

import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentCollectorConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentCollectorConfigJpaRepository extends JpaRepository<AgentCollectorConfigEntity, String> {

    List<AgentCollectorConfigEntity> findByTargetConfigIdOrderByCreatedAtAsc(String targetConfigId);
}
