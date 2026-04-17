package com.itmasters.icon.rpc.agent.application.dto;

import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentCollectorConfigEntity;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// [2026-03-13] agent_collector_file_configs / agent_collector_jdbc_configs 통합 제거
//              파일/JDBC 상세 설정은 ds_file_system_config / ds_database_config 에서 관리
public class AgentCollectorDto {

    @Getter
    @Builder
    public static class Info {
        private String collectorConfigId;
        private String targetConfigId;
        private String collectorType;
        private String name;
        private boolean enabled;
        private long pollIntervalMs;
        private int maxLinesPerPoll;
        private int maxRecordBytes;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Info from(AgentCollectorConfigEntity e) {
            return Info.builder()
                    .collectorConfigId(e.getCollectorConfigId())
                    .targetConfigId(e.getTargetConfigId())
                    .collectorType(e.getCollectorType())
                    .name(e.getName())
                    .enabled(e.isEnabled())
                    .pollIntervalMs(e.getPollIntervalMs())
                    .maxLinesPerPoll(e.getMaxLinesPerPoll())
                    .maxRecordBytes(e.getMaxRecordBytes())
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    @lombok.Setter
    public static class CreateRequest {
        @NotBlank private String collectorType; // FILE | JDBC
        @NotBlank private String name;
        private boolean enabled = true;
        private long pollIntervalMs = 1000;
        private int maxLinesPerPoll = 1000;
        private int maxRecordBytes = 524288;
    }

    @Getter
    @lombok.Setter
    public static class UpdateRequest {
        private String name;
        private Boolean enabled;
        private Long pollIntervalMs;
        private Integer maxLinesPerPoll;
        private Integer maxRecordBytes;
    }
}
