package com.itmasters.icon.rpc.agent.application.service;

import com.github.f4b6a3.tsid.TsidCreator;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentCollectorConfigEntity;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentCollectorConfigJpaRepository;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentTargetConfigJpaRepository;
import com.itmasters.icon.rpc.agent.application.dto.AgentCollectorDto;
import com.itmasters.icon.rpcserver.agent.AgentRpcWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentCollectorService {

    private final AgentTargetConfigJpaRepository targetConfigJpaRepository;
    private final AgentCollectorConfigJpaRepository collectorConfigJpaRepository;
    // [2026-03-13] agent_collector_file_configs, agent_collector_jdbc_configs 제거 —
    //              파일/JDBC 설정은 ds_file_system_config / ds_database_config 에서 관리
    private final AgentRpcWebSocketHandler wsHandler;

    @Transactional(readOnly = true)
    public List<AgentCollectorDto.Info> findAllByTargetConfigId(String agentId, String targetConfigId) {
        ensureTargetConfigBelongsToAgent(agentId, targetConfigId);
        List<AgentCollectorConfigEntity> list = collectorConfigJpaRepository.findByTargetConfigIdOrderByCreatedAtAsc(targetConfigId);
        return list.stream()
                .map(this::toInfo)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AgentCollectorDto.Info findById(String agentId, String targetConfigId, String collectorConfigId) {
        ensureTargetConfigBelongsToAgent(agentId, targetConfigId);
        AgentCollectorConfigEntity e = collectorConfigJpaRepository.findById(collectorConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Collector not found: " + collectorConfigId));
        if (!e.getTargetConfigId().equals(targetConfigId)) {
            throw new IllegalArgumentException("Collector does not belong to targetConfig: " + targetConfigId);
        }
        return toInfo(e);
    }

    @Transactional
    public AgentCollectorDto.Info create(String agentId, String targetConfigId, AgentCollectorDto.CreateRequest req) {
        ensureTargetConfigBelongsToAgent(agentId, targetConfigId);
        String type = req.getCollectorType() != null ? req.getCollectorType().toUpperCase() : "";
        if (!"FILE".equals(type) && !"JDBC".equals(type)) {
            throw new IllegalArgumentException("collectorType must be FILE or JDBC");
        }

        String id = TsidCreator.getTsid256().toString();
        AgentCollectorConfigEntity collector = AgentCollectorConfigEntity.create(
                id, targetConfigId, type, req.getName(), req.isEnabled(),
                req.getPollIntervalMs(), req.getMaxLinesPerPoll(), req.getMaxRecordBytes());
        collectorConfigJpaRepository.save(collector);

        log.info("[RPC] Collector created - agentId={}, targetConfigId={}, type={}, id={}", agentId, targetConfigId, type, id);
        syncCollectorsToAgent(agentId, targetConfigId);
        return toInfo(collector);
    }

    @Transactional
    public AgentCollectorDto.Info update(String agentId, String targetConfigId, String collectorConfigId,
                                        AgentCollectorDto.UpdateRequest req) {
        ensureTargetConfigBelongsToAgent(agentId, targetConfigId);
        AgentCollectorConfigEntity collector = collectorConfigJpaRepository.findById(collectorConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Collector not found: " + collectorConfigId));
        if (!collector.getTargetConfigId().equals(targetConfigId)) {
            throw new IllegalArgumentException("Collector does not belong to targetConfig");
        }

        if (req.getName() != null) collector.setName(req.getName());
        if (req.getEnabled() != null) collector.setEnabled(req.getEnabled());
        if (req.getPollIntervalMs() != null) collector.setPollIntervalMs(req.getPollIntervalMs());
        if (req.getMaxLinesPerPoll() != null) collector.setMaxLinesPerPoll(req.getMaxLinesPerPoll());
        if (req.getMaxRecordBytes() != null) collector.setMaxRecordBytes(req.getMaxRecordBytes());
        collectorConfigJpaRepository.save(collector);

        log.info("[RPC] Collector updated - id={}", collectorConfigId);
        syncCollectorsToAgent(agentId, targetConfigId);
        return toInfo(collector);
    }

    @Transactional
    public void delete(String agentId, String targetConfigId, String collectorConfigId) {
        ensureTargetConfigBelongsToAgent(agentId, targetConfigId);
        AgentCollectorConfigEntity collector = collectorConfigJpaRepository.findById(collectorConfigId)
                .orElseThrow(() -> new IllegalArgumentException("Collector not found: " + collectorConfigId));
        if (!collector.getTargetConfigId().equals(targetConfigId)) {
            throw new IllegalArgumentException("Collector does not belong to targetConfig");
        }
        // [2026-03-13] file/jdbc 자식 테이블 제거됨 — collector 레코드만 삭제
        collectorConfigJpaRepository.delete(collector);
        log.info("[RPC] Collector deleted - id={}", collectorConfigId);
        syncCollectorsToAgent(agentId, targetConfigId);
    }

    /**
     * [2026-03-13] Builds collector list using base collector info only.
     * File/JDBC config details are resolved by DataSourceConfigService (icon-api)
     * which reads from ds_file_system_config / ds_database_config directly.
     */
    public void syncCollectorsToAgent(String agentId, String targetConfigId) {
        var targetConfig = targetConfigJpaRepository.findById(targetConfigId)
                .filter(tc -> agentId.equals(tc.getAgentId()))
                .orElse(null);
        if (targetConfig == null) return;
        String targetId = targetConfig.getTargetId();
        List<AgentCollectorConfigEntity> list = collectorConfigJpaRepository.findByTargetConfigIdOrderByCreatedAtAsc(targetConfigId);
        List<Map<String, Object>> payload = buildPayload(list);
        wsHandler.pushCollectorsSync(agentId, targetId, payload);
    }

    /**
     * [2026-03-13] 데이터소스 설정 저장 시 파일/JDBC 설정을 포함한 전체 payload로 sync.
     * icon-api의 DataSourceConfigService에서 직접 호출한다.
     */
    public void syncCollectorsToAgentWithConfig(String agentId, String targetConfigId,
                                                Map<String, Map<String, Object>> configsByCollectorId) {
        var targetConfig = targetConfigJpaRepository.findById(targetConfigId)
                .filter(tc -> agentId.equals(tc.getAgentId()))
                .orElse(null);
        if (targetConfig == null) return;
        String targetId = targetConfig.getTargetId();
        List<AgentCollectorConfigEntity> list = collectorConfigJpaRepository.findByTargetConfigIdOrderByCreatedAtAsc(targetConfigId);
        List<Map<String, Object>> payload = buildPayload(list);
        // 파일/JDBC 설정 오버라이드 적용
        for (Map<String, Object> m : payload) {
            String id = (String) m.get("id");
            if (id != null && configsByCollectorId.containsKey(id)) {
                m.putAll(configsByCollectorId.get(id));
            }
        }
        wsHandler.pushCollectorsSync(agentId, targetId, payload);
    }

    private List<Map<String, Object>> buildPayload(List<AgentCollectorConfigEntity> list) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (AgentCollectorConfigEntity c : list) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", c.getCollectorConfigId());
            m.put("type", c.getCollectorType());
            m.put("name", c.getName());
            m.put("enabled", c.isEnabled());
            m.put("pollIntervalMs", c.getPollIntervalMs());
            m.put("maxLinesPerPoll", c.getMaxLinesPerPoll());
            m.put("maxRecordBytes", c.getMaxRecordBytes());
            if (c.getDataSourceId() != null) {
                m.put("dataSourceId", c.getDataSourceId());
            }
            payload.add(m);
        }
        return payload;
    }

    private void ensureTargetConfigBelongsToAgent(String agentId, String targetConfigId) {
        targetConfigJpaRepository.findById(targetConfigId)
                .filter(tc -> agentId.equals(tc.getAgentId()))
                .orElseThrow(() -> new IllegalArgumentException(
                        "TargetConfig not found or does not belong to agent: " + targetConfigId));
    }

    private AgentCollectorDto.Info toInfo(AgentCollectorConfigEntity e) {
        return AgentCollectorDto.Info.from(e);
    }
}
