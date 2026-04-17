package com.itmasters.icon.api.rulesnapshot.application.service;

import com.itmasters.icon.api.common.client.AiWebhookClient;
import com.itmasters.icon.api.rulesnapshot.dto.RuleSnapshotDto;
import com.itmasters.icon.engine.adapter.out.persistence.entity.RuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.JpaRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 학습용 룰 스냅샷 서비스
 * - 활성화된 룰 데이터를 수집하여 AI 웹훅으로 전송
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RuleSnapshotService {

    private final JpaRuleRepository ruleRepository;
    private final AiWebhookClient webhookClient;

//    @Value("${ai.rulesnapshot.webhook.url}")
    private String ruleSnapshotWebhookUrl;

    /**
     * 활성화된 모든 룰을 AI 학습용으로 전송
     *
     * @return 전송 결과
     */
    public RuleSnapshotDto.Response sendActiveRulesSnapshot() {
        log.info("Starting rule snapshot for AI learning...");

        // 1. 활성화된 룰 조회
        List<RuleEntity> activeRules = ruleRepository.findByIsActiveTrue();
        log.info("Found {} active rules", activeRules.size());

        if (activeRules.isEmpty()) {
            return RuleSnapshotDto.Response.builder()
                    .success(true)
                    .message("전송할 활성화된 룰이 없습니다.")
                    .ruleCount(0)
                    .build();
        }

        // 2. DTO 변환
        List<RuleSnapshotDto.RuleData> rulesData = activeRules.stream()
                .map(this::toRuleData)
                .collect(Collectors.toList());

        // 3. 요청 객체 생성
        RuleSnapshotDto.Request request = RuleSnapshotDto.Request.builder()
                .rulesJson(rulesData)
                .build();

        // 4. 웹훅 호출
        try {
            webhookClient.post(ruleSnapshotWebhookUrl, request, String.class);
            log.info("Successfully sent {} rules to AI webhook", rulesData.size());

            return RuleSnapshotDto.Response.builder()
                    .success(true)
                    .message("룰 스냅샷이 성공적으로 전송되었습니다.")
                    .ruleCount(rulesData.size())
                    .build();
        } catch (Exception e) {
            log.error("Failed to send rule snapshot: {}", e.getMessage(), e);

            return RuleSnapshotDto.Response.builder()
                    .success(false)
                    .message("룰 스냅샷 전송 실패: " + e.getMessage())
                    .ruleCount(0)
                    .build();
        }
    }

    /**
     * RuleEntity를 RuleData DTO로 변환
     */
    private RuleSnapshotDto.RuleData toRuleData(RuleEntity entity) {
        return RuleSnapshotDto.RuleData.builder()
                .id(entity.getRuleId())
                .name(entity.getName())
                .description(entity.getDescription())
                .whereJson(entity.getWhereJson())
                .build();
    }
}
