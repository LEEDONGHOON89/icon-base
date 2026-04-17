package com.itmasters.icon.api.aichat.adapter.in.web;

import com.itmasters.icon.api.aichat.application.service.N8nAiChatService;
import com.itmasters.icon.api.aichat.dto.AiChatDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI Chat", description = "AI 서포트 채팅 API")
@RestController
@RequestMapping("/api/v1/ai-chat")
@RequiredArgsConstructor
public class AiChatController {

    private final N8nAiChatService aiChatService;

    @Operation(summary = "메시지 전송", description = "AI에게 메시지를 전송하고 응답을 받습니다")
    @PostMapping("/message")
    public AiChatDto.SendMessageResponse sendMessage(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody AiChatDto.SendMessageRequest request) {
        String userId = userDetails != null ? userDetails.getUsername() : "anonymous";
        return aiChatService.sendMessage(userId, request);
    }

    @Operation(summary = "새 세션 생성", description = "새로운 채팅 세션을 생성합니다")
    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public AiChatDto.SessionSummary createSession(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUsername() : "anonymous";
        return aiChatService.createSession(userId);
    }

    @Operation(summary = "세션 목록 조회", description = "사용자의 채팅 세션 목록을 조회합니다")
    @GetMapping("/sessions")
    public List<AiChatDto.SessionSummary> getSessions(
            @AuthenticationPrincipal UserDetails userDetails) {
        String userId = userDetails != null ? userDetails.getUsername() : "anonymous";
        return aiChatService.getUserSessions(userId);
    }

    @Operation(summary = "세션 상세 조회", description = "특정 세션의 메시지 이력을 조회합니다")
    @GetMapping("/sessions/{sessionId}")
    public AiChatDto.SessionDetail getSessionDetail(
            @PathVariable String sessionId) {
        return aiChatService.getSessionDetail(sessionId);
    }

    @Operation(summary = "세션 제목 수정", description = "세션 제목을 수정합니다")
    @PatchMapping("/sessions/{sessionId}/title")
    public void updateSessionTitle(
            @PathVariable String sessionId,
            @RequestBody AiChatDto.UpdateTitleRequest request) {
        aiChatService.updateSessionTitle(sessionId, request.getTitle());
    }

    @Operation(summary = "세션 삭제", description = "채팅 세션을 삭제합니다")
    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable String sessionId) {
        aiChatService.deleteSession(sessionId);
    }
}
