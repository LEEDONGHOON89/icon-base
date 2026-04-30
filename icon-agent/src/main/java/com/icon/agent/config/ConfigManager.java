package com.icon.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// [2026-04-21] TlsConfig, CollectorConfig import 제거 — 관련 메서드 삭제됨

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Loads and saves agent configuration from/to a YAML file using Jackson.
 */
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory()); // config.yaml 로드 전용

    private final AgentConfig config;

    public ConfigManager(String configFilePath) throws IOException {
        Path path = Path.of(configFilePath);
        if (!Files.exists(path)) {
            throw new IOException("Config file not found: " + configFilePath);
        }
        log.info("Loading configuration from: {}", path.toAbsolutePath());
        this.config = YAML_MAPPER.readValue(path.toFile(), AgentConfig.class);
        // [2026-04-21] config.yaml에 agentId 없으면 data/agent.id에서 읽거나 신규 생성
        resolveAgentId();
        validate();
        // [2026-04-21] targets는 data/targets.json에서 관리 — config.yaml에서 제외됨
        log.info("Configuration loaded: agentId={}", config.getAgentId());
    }

    /**
     * [2026-04-21] agentId 결정 우선순위:
     *   1) config.yaml agentId 명시 → 그대로 사용
     *   2) data/agent.id 파일 존재 → 영속화된 ID 재사용 (재기동 시 동일 ID 보장)
     *   3) 둘 다 없음 → "agent-{12자리 hex}" UUID 생성 후 data/agent.id에 저장
     *
     * data/agent.id 분실 시 새 ID가 생성되므로 서버에서 신규 에이전트로 등록됨.
     */
    private void resolveAgentId() throws IOException {
        if (config.getAgentId() != null && !config.getAgentId().isBlank()) {
            log.info("AgentId: {} (config.yaml 명시)", config.getAgentId());
            return;
        }
        Path idFile = Path.of("data", "agent.id");
        if (Files.exists(idFile)) {
            String persisted = Files.readString(idFile, StandardCharsets.UTF_8).strip();
            if (!persisted.isBlank()) {
                config.setAgentId(persisted);
                log.info("AgentId: {} (data/agent.id 재사용)", persisted);
                return;
            }
        }
        String newId = "agent-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Files.createDirectories(idFile.getParent());
        Files.writeString(idFile, newId, StandardCharsets.UTF_8);
        config.setAgentId(newId);
        log.info("AgentId: {} (신규 생성 — data/agent.id 저장)", newId);
    }

    private void validate() {
        // [2026-04-21] targets는 선택 사항 — CLI로 런타임 추가 가능
        if (config.getTargets() == null || config.getTargets().isEmpty()) return;
        for (TargetConfig t : config.getTargets()) {
            if (t.getId() == null || t.getId().isBlank()) {
                throw new IllegalArgumentException("Each target must have a non-empty id");
            }
            if (t.getRpc() == null || t.getRpc().getEndpoint() == null) {
                throw new IllegalArgumentException("Target '" + t.getId() + "' missing rpc.endpoint");
            }
        }
    }

    public AgentConfig getConfig() {
        return config;
    }

    // [2026-04-21] updateTargetRpcAndSave(), applyCollectorsAndSave() 제거
    //              targets는 data/targets.json에서 관리 (TargetStore), config.yaml 저장 불필요
    //              CONFIG_UPDATE → TargetContext.applyConfigUpdate() + TargetStore.save()
    //              COLLECTORS_SYNC → TargetContext.applyCollectorsSync() (서버 재연결 시 복원)
}
