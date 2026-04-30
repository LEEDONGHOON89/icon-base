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
        if (targetConfigJpaRepository.existsByAgentIdAndTargetId(agentId, req.getTargetId())) {
            throw new IllegalStateException(
                    "TargetConfig already exists: agentId=" + agentId + ", targetId=" + req.getTargetId());
        }

        String id = TsidCreator.getTsid256().toString();
        // [2026-04-23] maxBatchesPerSecond 파라미터 누락 수정 (빌드 에러 해결)
        AgentTargetConfigEntity entity = AgentTargetConfigEntity.create(
                id, agentId, req.getTargetId(),
                req.getRpcEndpoint(), req.isCompress(),
                req.getTlsKeystorePath(), req.getTlsKeystorePassword(),
                req.getTlsTruststorePath(), req.getTlsTruststorePassword(),
                req.getQueueCapacity(), req.getMaxBatchSize(),
                req.getMaxBatchMs(), req.getMaxBatchBytes(),
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
        // [2026-04-21] 빈 문자열/null 비밀번호로 기존 값 덮어쓰는 버그 수정 - 빈 값이면 기존 비밀번호 유지
        String keystorePassword = (req.getTlsKeystorePassword() == null || req.getTlsKeystorePassword().isBlank())
                ? entity.getTlsKeystorePassword()
                : req.getTlsKeystorePassword();
        String truststorePassword = (req.getTlsTruststorePassword() == null || req.getTlsTruststorePassword().isBlank())
                ? entity.getTlsTruststorePassword()
                : req.getTlsTruststorePassword();
        // [2026-04-23] maxBatchesPerSecond 파라미터 누락 수정 (빌드 에러 해결)
        entity.update(entity.getRpcEndpoint(), req.isCompress(),
                req.getTlsKeystorePath(), keystorePassword,
                req.getTlsTruststorePath(), truststorePassword,
                req.getQueueCapacity(), req.getMaxBatchSize(),
                req.getMaxBatchMs(), req.getMaxBatchBytes(),
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
