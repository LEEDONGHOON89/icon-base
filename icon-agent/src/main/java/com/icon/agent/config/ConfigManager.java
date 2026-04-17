package com.icon.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Loads and saves agent configuration from/to a YAML file using Jackson.
 */
public class ConfigManager {

    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());

    private final String configFilePath;
    private final AgentConfig config;

    public ConfigManager(String configFilePath) throws IOException {
        this.configFilePath = configFilePath;
        Path path = Path.of(configFilePath);
        if (!Files.exists(path)) {
            throw new IOException("Config file not found: " + configFilePath);
        }
        log.info("Loading configuration from: {}", path.toAbsolutePath());
        this.config = YAML_MAPPER.readValue(path.toFile(), AgentConfig.class);
        validate();
        log.info("Configuration loaded: {} target(s) configured", config.getTargets().size());
    }

    private void validate() {
        if (config.getTargets() == null || config.getTargets().isEmpty()) {
            throw new IllegalArgumentException("No targets configured in config file");
        }
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

    /**
     * Updates RPC and batch settings for the given target and writes config to file.
     * Called when CONFIG_UPDATE is received from the server.
     */
    public void updateTargetRpcAndSave(String targetId,
                                       String rpcEndpoint,
                                       boolean compress,
                                       String tlsKeystorePath,
                                       String tlsKeystorePassword,
                                       String tlsTruststorePath,
                                       String tlsTruststorePassword,
                                       int queueCapacity,
                                       int maxBatchSize,
                                       long maxBatchMs,
                                       long maxBatchBytes) throws IOException {
        TargetConfig target = config.getTargets().stream()
                .filter(t -> targetId.equals(t.getId()))
                .findFirst()
                .orElse(null);
        if (target == null) {
            log.warn("CONFIG_UPDATE: target not found in config, targetId={}", targetId);
            return;
        }
        // rpcEndpoint is not updated by CONFIG_UPDATE (immutable)
        target.getRpc().setCompress(compress);
        if (target.getRpc().getTls() == null) {
            target.getRpc().setTls(new TlsConfig());
        }
        TlsConfig tls = target.getRpc().getTls();
        if (tlsKeystorePath != null) tls.setKeystorePath(tlsKeystorePath.isEmpty() ? null : tlsKeystorePath);
        if (tlsKeystorePassword != null) tls.setKeystorePassword(tlsKeystorePassword.isEmpty() ? null : tlsKeystorePassword);
        if (tlsTruststorePath != null) tls.setTruststorePath(tlsTruststorePath.isEmpty() ? null : tlsTruststorePath);
        if (tlsTruststorePassword != null) tls.setTruststorePassword(tlsTruststorePassword.isEmpty() ? null : tlsTruststorePassword);
        target.setQueueCapacity(queueCapacity > 0 ? queueCapacity : target.getQueueCapacity());
        target.setMaxBatchSize(maxBatchSize > 0 ? maxBatchSize : target.getMaxBatchSize());
        target.setMaxBatchMs(maxBatchMs > 0 ? maxBatchMs : target.getMaxBatchMs());
        target.setMaxBatchBytes(maxBatchBytes >= 0 ? maxBatchBytes : target.getMaxBatchBytes());

        Path path = Path.of(configFilePath);
        YAML_MAPPER.writeValue(path.toFile(), config);
        log.info("[{}] config.yaml updated with CONFIG_UPDATE (endpoint={}, maxBatchSize={})",
                targetId, rpcEndpoint, maxBatchSize);
    }

    /**
     * Replaces collectors for the given target and writes config to file.
     * Called when COLLECTORS_SYNC is received from the server.
     */
    public void applyCollectorsAndSave(String targetId, java.util.List<CollectorConfig> collectors) throws IOException {
        TargetConfig target = config.getTargets().stream()
                .filter(t -> targetId.equals(t.getId()))
                .findFirst()
                .orElse(null);
        if (target == null) {
            log.warn("COLLECTORS_SYNC: target not found in config, targetId={}", targetId);
            return;
        }
        target.setCollectors(collectors != null ? collectors : new java.util.ArrayList<>());
        Path path = Path.of(configFilePath);
        YAML_MAPPER.writeValue(path.toFile(), config);
        log.info("[{}] config.yaml updated with COLLECTORS_SYNC (collectors={})", targetId, collectors != null ? collectors.size() : 0);
    }
}
