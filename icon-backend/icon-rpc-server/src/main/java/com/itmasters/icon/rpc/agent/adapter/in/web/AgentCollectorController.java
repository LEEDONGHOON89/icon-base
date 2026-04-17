package com.itmasters.icon.rpc.agent.adapter.in.web;

import com.itmasters.icon.rpc.agent.application.dto.AgentCollectorDto;
import com.itmasters.icon.rpc.agent.application.service.AgentCollectorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rpc/agents/{agentId}/target-configs/{targetConfigId}/collectors")
@RequiredArgsConstructor
public class AgentCollectorController {

    private final AgentCollectorService collectorService;

    @GetMapping
    public ResponseEntity<List<AgentCollectorDto.Info>> list(
            @PathVariable String agentId,
            @PathVariable String targetConfigId) {
        return ResponseEntity.ok(collectorService.findAllByTargetConfigId(agentId, targetConfigId));
    }

    @GetMapping("/{collectorConfigId}")
    public ResponseEntity<AgentCollectorDto.Info> get(
            @PathVariable String agentId,
            @PathVariable String targetConfigId,
            @PathVariable String collectorConfigId) {
        return ResponseEntity.ok(collectorService.findById(agentId, targetConfigId, collectorConfigId));
    }

    @PostMapping
    public ResponseEntity<AgentCollectorDto.Info> create(
            @PathVariable String agentId,
            @PathVariable String targetConfigId,
            @RequestBody @Valid AgentCollectorDto.CreateRequest req) {
        return ResponseEntity.ok(collectorService.create(agentId, targetConfigId, req));
    }

    @PutMapping("/{collectorConfigId}")
    public ResponseEntity<AgentCollectorDto.Info> update(
            @PathVariable String agentId,
            @PathVariable String targetConfigId,
            @PathVariable String collectorConfigId,
            @RequestBody @Valid AgentCollectorDto.UpdateRequest req) {
        return ResponseEntity.ok(collectorService.update(agentId, targetConfigId, collectorConfigId, req));
    }

    @DeleteMapping("/{collectorConfigId}")
    public ResponseEntity<Void> delete(
            @PathVariable String agentId,
            @PathVariable String targetConfigId,
            @PathVariable String collectorConfigId) {
        collectorService.delete(agentId, targetConfigId, collectorConfigId);
        return ResponseEntity.noContent().build();
    }
}
