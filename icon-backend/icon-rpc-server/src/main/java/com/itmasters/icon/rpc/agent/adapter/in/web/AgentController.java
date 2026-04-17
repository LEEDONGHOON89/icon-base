package com.itmasters.icon.rpc.agent.adapter.in.web;

import com.itmasters.icon.rpc.agent.adapter.out.persistence.repository.AgentJpaRepository;
import com.itmasters.icon.rpc.agent.application.dto.AgentDto;
import com.itmasters.icon.rpc.agent.application.service.AgentRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rpc/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentJpaRepository agentJpaRepository;
    private final AgentRegistrationService registrationService;

    @GetMapping
    public ResponseEntity<List<AgentDto.Info>> list() {
        List<AgentDto.Info> result = agentJpaRepository.findAllByOrderByLastConnectedAtDesc()
                .stream()
                .map(AgentDto.Info::from)
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{agentId}")
    public ResponseEntity<AgentDto.Info> get(@PathVariable String agentId) {
        return agentJpaRepository.findById(agentId)
                .map(e -> ResponseEntity.ok(AgentDto.Info.from(e)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{agentId}")
    public ResponseEntity<AgentDto.Info> update(@PathVariable String agentId,
                                                 @RequestBody AgentDto.UpdateRequest req) {
        return agentJpaRepository.findById(agentId)
                .map(e -> {
                    e.updateMeta(req.getDisplayName(), req.getDescription());
                    agentJpaRepository.save(e);
                    return ResponseEntity.ok(AgentDto.Info.from(e));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{agentId}/sessions")
    public ResponseEntity<List<AgentDto.SessionInfo>> sessions(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(registrationService.getSessions(agentId, limit));
    }
}
