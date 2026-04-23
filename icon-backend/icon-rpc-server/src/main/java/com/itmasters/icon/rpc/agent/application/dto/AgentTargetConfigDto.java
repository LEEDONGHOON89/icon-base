package com.itmasters.icon.rpc.agent.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.itmasters.icon.rpc.agent.adapter.out.persistence.entity.AgentTargetConfigEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

public class AgentTargetConfigDto {

    @Getter
    @Builder
    public static class Info {
        private String targetConfigId;
        private String agentId;
        private String targetId;
        private String rpcEndpoint;
        private boolean compress;
        private String tlsKeystorePath;
        // [2026-04-21] push 전용 password 필드 추가 - GET 응답에는 포함하지 않음
        @JsonIgnore
        private String tlsKeystorePassword;
        private String tlsTruststorePath;
        @JsonIgnore
        private String tlsTruststorePassword;
        private int queueCapacity;
        private int maxBatchSize;
        private long maxBatchMs;
        private long maxBatchBytes;
        // [2026-04-23] maxBatchesPerSecond 필드 추가 (Entity와 동기화)
        private int maxBatchesPerSecond;
        private boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static Info from(AgentTargetConfigEntity e) {
            return Info.builder()
                    .targetConfigId(e.getTargetConfigId())
                    .agentId(e.getAgentId())
                    .targetId(e.getTargetId())
                    .rpcEndpoint(e.getRpcEndpoint())
                    .compress(e.isCompress())
                    .tlsKeystorePath(e.getTlsKeystorePath())
                    // [2026-04-21] push 전용 password 포함 - @JsonIgnore로 GET 응답에서 제외됨
                    .tlsKeystorePassword(e.getTlsKeystorePassword())
                    .tlsTruststorePath(e.getTlsTruststorePath())
                    .tlsTruststorePassword(e.getTlsTruststorePassword())
                    .queueCapacity(e.getQueueCapacity())
                    .maxBatchSize(e.getMaxBatchSize())
                    .maxBatchMs(e.getMaxBatchMs())
                    .maxBatchBytes(e.getMaxBatchBytes())
                    // [2026-04-23] maxBatchesPerSecond 매핑 추가
                    .maxBatchesPerSecond(e.getMaxBatchesPerSecond())
                    .isActive(e.isActive())
                    .createdAt(e.getCreatedAt())
                    .updatedAt(e.getUpdatedAt())
                    .build();
        }
    }

    @Getter
    public static class CreateRequest {
        @NotBlank private String targetId;
        @NotBlank private String rpcEndpoint;
        private boolean compress = false;
        private String tlsKeystorePath;
        private String tlsKeystorePassword;
        private String tlsTruststorePath;
        private String tlsTruststorePassword;
        private int queueCapacity = 10000;
        private int maxBatchSize = 500;
        private long maxBatchMs = 2000L;
        private long maxBatchBytes = 1048576L;
        // [2026-04-23] maxBatchesPerSecond 필드 추가 (0 = 무제한, 기본값 10)
        private int maxBatchesPerSecond = 10;
    }

    @Getter
    public static class UpdateRequest {
        @NotBlank private String rpcEndpoint;
        private boolean compress = false;
        private String tlsKeystorePath;
        private String tlsKeystorePassword;
        private String tlsTruststorePath;
        private String tlsTruststorePassword;
        private int queueCapacity = 10000;
        private int maxBatchSize = 500;
        private long maxBatchMs = 2000L;
        private long maxBatchBytes = 1048576L;
        // [2026-04-23] maxBatchesPerSecond 필드 추가 (0 = 무제한, 기본값 10)
        private int maxBatchesPerSecond = 10;
    }
}
