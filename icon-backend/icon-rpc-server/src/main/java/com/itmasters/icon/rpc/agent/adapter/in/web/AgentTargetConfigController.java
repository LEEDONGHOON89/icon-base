package com.itmasters.icon.rpc.agent.adapter.in.web;

import com.itmasters.icon.rpc.agent.application.dto.AgentTargetConfigDto;
import com.itmasters.icon.rpc.agent.application.service.AgentTargetConfigService;
import com.itmasters.icon.rpcserver.agent.AgentRpcWebSocketHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/rpc/agents/{agentId}/target-configs")
@RequiredArgsConstructor
public class AgentTargetConfigController {

    private final AgentTargetConfigService targetConfigService;
    private final AgentRpcWebSocketHandler wsHandler;

    @GetMapping
    public ResponseEntity<List<AgentTargetConfigDto.Info>> list(@PathVariable String agentId) {
        return ResponseEntity.ok(targetConfigService.findAll(agentId));
    }

    @PostMapping
    public ResponseEntity<AgentTargetConfigDto.Info> create(
            @PathVariable String agentId,
            @RequestBody @Valid AgentTargetConfigDto.CreateRequest req) {
        return ResponseEntity.ok(targetConfigService.create(agentId, req));
    }

    @PutMapping("/{targetConfigId}")
    public ResponseEntity<AgentTargetConfigDto.Info> update(
            @PathVariable String agentId,
            @PathVariable String targetConfigId,
            @RequestBody @Valid AgentTargetConfigDto.UpdateRequest req) {
        AgentTargetConfigDto.Info updated = targetConfigService.update(targetConfigId, req);
        pushToAgent(agentId, updated);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{targetConfigId}")
    public ResponseEntity<Void> delete(
            @PathVariable String agentId,
            @PathVariable String targetConfigId) {
        targetConfigService.delete(targetConfigId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{targetConfigId}/push")
    public ResponseEntity<Map<String, Object>> push(
            @PathVariable String agentId,
            @PathVariable String targetConfigId) {
        AgentTargetConfigDto.Info info = targetConfigService.findAll(agentId).stream()
                .filter(c -> c.getTargetConfigId().equals(targetConfigId))
                .findFirst()
                .orElse(null);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        boolean sent = pushToAgent(agentId, info);
        return ResponseEntity.ok(Map.of(
                "agentId", agentId,
                "pushed",  sent,
                "message", sent ? "CONFIG_UPDATE sent to agent" : "Agent not connected"
        ));
    }

    // [2026-04-21] payload 빌드를 AgentRpcWebSocketHandler.buildConfigPayload()로 통일
    private boolean pushToAgent(String agentId, AgentTargetConfigDto.Info cfg) {
        if (!wsHandler.isAgentConnected(agentId)) {
            log.info("[TargetConfig] Agent not connected, skipping push: agentId={}", agentId);
            return false;
        }
        return wsHandler.pushConfigUpdate(agentId, AgentRpcWebSocketHandler.buildConfigPayload(cfg));
    }
}
