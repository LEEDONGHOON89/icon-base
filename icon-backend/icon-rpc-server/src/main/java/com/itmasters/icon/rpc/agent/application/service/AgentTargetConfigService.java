package com.itmasters.icon.rpc.agent.application.service;

import com.github.f4b6a3.tsid.TsidCreator;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentTargetConfigEntity;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentJpaRepository;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentTargetConfigJpaRepository;
import com.itmasters.icon.rpc.agent.application.dto.AgentTargetConfigDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentTargetConfigService {

    private final AgentJpaRepository agentJpaRepository;
    private final AgentTargetConfigJpaRepository targetConfigJpaRepository;

    @Transactional(readOnly = true)
    public List<AgentTargetConfigDto.Info> findAll(String agentId) {
        return targetConfigJpaRepository.findByAgentId(agentId)
                .stream()
                .map(AgentTargetConfigDto.Info::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgentTargetConfigDto.Info findById(String targetConfigId) {
        return targetConfigJpaRepository.findById(targetConfigId)
                .map(AgentTargetConfigDto.Info::from)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TargetConfig not found: " + targetConfigId));
    }

    @Transactional
    public AgentTargetConfigDto.Info create(String agentId, AgentTargetConfigDto.CreateRequest req) {
        if (!agentJpaRepository.existsById(agentId)) {
            throw new IllegalArgumentException("Agent not found: " + agentId);
        }

        // [2026-04-22] 에이전트당 타겟 1개 제한
        //   하나의 에이전트는 내부통제시스템 서버 1곳에만 연결해야 한다.
        //   이미 타겟이 존재하면 추가 생성을 거부한다.
        List<AgentTargetConfigDto.Info> existing = findAll(agentId);
        if (!existing.isEmpty()) {
            throw new IllegalStateException(
                    "에이전트당 타겟은 1개만 허용됩니다. agentId=" + agentId
                    + ", 기존 타겟=" + existing.get(0).getTargetId());
        }

        if (targetConfigJpaRepository.existsByAgentIdAndTargetId(agentId, req.getTargetId())) {
            throw new IllegalStateException(
                    "TargetConfig already exists: agentId=" + agentId + ", targetId=" + req.getTargetId());
        }

        String id = TsidCreator.getTsid256().toString();
        AgentTargetConfigEntity entity = AgentTargetConfigEntity.create(
                id, agentId, req.getTargetId(),
                req.getRpcEndpoint(), req.isCompress(),
                req.getTlsKeystorePath(), req.getTlsKeystorePassword(),
                req.getTlsTruststorePath(), req.getTlsTruststorePassword(),
                req.getQueueCapacity(), req.getMaxBatchSize(),
                req.getMaxBatchMs(), req.getMaxBatchBytes(),
                // [2026-04-22] maxBatchesPerSecond 추가
                req.getMaxBatchesPerSecond());

        targetConfigJpaRepository.save(entity);
        log.info("[RPC] TargetConfig created - agentId={}, targetId={}, id={}", agentId, req.getTargetId(), id);
        return AgentTargetConfigDto.Info.from(entity);
    }

    @Transactional
    public AgentTargetConfigDto.Info update(String targetConfigId, AgentTargetConfigDto.UpdateRequest req) {
        AgentTargetConfigEntity entity = targetConfigJpaRepository.findById(targetConfigId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TargetConfig not found: " + targetConfigId));

        // rpcEndpoint is immutable (not updated)
        entity.update(entity.getRpcEndpoint(), req.isCompress(),
                req.getTlsKeystorePath(), req.getTlsKeystorePassword(),
                req.getTlsTruststorePath(), req.getTlsTruststorePassword(),
                req.getQueueCapacity(), req.getMaxBatchSize(),
                req.getMaxBatchMs(), req.getMaxBatchBytes(),
                // [2026-04-22] maxBatchesPerSecond 추가
                req.getMaxBatchesPerSecond());

        targetConfigJpaRepository.save(entity);
        log.info("[RPC] TargetConfig updated - id={}", targetConfigId);
        return AgentTargetConfigDto.Info.from(entity);
    }

    @Transactional
    public void delete(String targetConfigId) {
        AgentTargetConfigEntity entity = targetConfigJpaRepository.findById(targetConfigId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "TargetConfig not found: " + targetConfigId));
        targetConfigJpaRepository.delete(entity);
        log.info("[RPC] TargetConfig deleted - id={}", targetConfigId);
    }
}
