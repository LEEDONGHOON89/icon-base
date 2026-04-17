package com.itmasters.icon.api.rulesnapshot.adapter.in.web;

import com.itmasters.icon.api.rulesnapshot.application.service.RuleSnapshotService;
import com.itmasters.icon.api.rulesnapshot.dto.RuleSnapshotDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 학습용 룰 스냅샷 API
 */
@RestController
@RequestMapping("/api/v1/ai/rule-snapshot")
@RequiredArgsConstructor
@Tag(name = "AI Rule Snapshot", description = "AI 학습용 룰 데이터 전송 API")
public class RuleSnapshotController {

    private final RuleSnapshotService ruleSnapshotService;

    /**
     * 활성화된 룰 스냅샷을 AI에 전송
     */
    @PostMapping("/send")
    @Operation(summary = "룰 스냅샷 전송", description = "활성화된 모든 룰을 AI 학습 웹훅으로 전송합니다.")
    public RuleSnapshotDto.Response sendRuleSnapshot() {
        return ruleSnapshotService.sendActiveRulesSnapshot();
    }
}
