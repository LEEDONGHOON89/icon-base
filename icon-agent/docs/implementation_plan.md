# Java Multi-Target Collector Agent — Implementation Plan

## Summary

Build a production-grade, lightweight Java 17+ Collector Agent from scratch.  
The agent connects **one process → N internal control systems**, with per-target isolation of collectors, queues, spool, and RPC connections.  
Uses certificate-based TLS (mTLS-ready), file-only persistence, and guarantees at-least-once delivery.

**Entry point:** `java -jar collector-agent.jar -c config.yaml`

---

## Project Structure

```
icon-agent_anti2/
├── build.gradle                     (fat-jar, dependencies)
├── settings.gradle
├── gradle/libs.versions.toml
├── docs/
│   ├── implementation_plan.md       ← this file
│   └── task.md
├── config/
│   └── config.yaml                  (sample multi-target config)
├── certs/                           (placeholder for TLS certs)
└── src/
    ├── main/java/com/icon/agent/
    │   ├── CollectorAgent.java      (main)
    │   ├── config/
    │   │   ├── AgentConfig.java
    │   │   ├── TargetConfig.java
    │   │   ├── RpcConfig.java
    │   │   ├── TlsConfig.java
    │   │   ├── CollectorSetConfig.java
    │   │   ├── FileCollectorConfig.java
    │   │   ├── JdbcCollectorConfig.java
    │   │   └── ConfigManager.java
    │   ├── tls/
    │   │   └── SslContextFactory.java
    │   ├── store/
    │   │   ├── PositionRecord.java
    │   │   ├── PositionStore.java
    │   │   └── FilePositionStore.java
    │   ├── collector/
    │   │   ├── Record.java
    │   │   ├── FileCollector.java
    │   │   └── JdbcCollector.java
    │   ├── queue/
    │   │   └── RecordQueue.java
    │   ├── batch/
    │   │   ├── Batch.java
    │   │   └── BatchBuilder.java
    │   ├── rpc/
    │   │   └── RpcClient.java
    │   ├── spool/
    │   │   └── SpoolManager.java
    │   ├── target/
    │   │   ├── TargetContext.java
    │   │   └── TargetManager.java
    │   ├── health/
    │   │   └── HealthServer.java
    │   └── shutdown/
    │       └── ShutdownManager.java
    └── test/java/com/icon/agent/
        ├── MultiTargetIsolationTest.java
        ├── FileRotationTest.java
        ├── RestartResumeTest.java
        ├── NetworkOutageTest.java
        └── SpoolRecoveryTest.java
```

---

## Phase-by-Phase Plan

### Phase 1 — Build System
- `build.gradle`: Java 17, `application` plugin, `com.github.johnrengelman.shadow` fat-jar
- Dependencies: `jackson-databind`, `jackson-dataformat-yaml`, `slf4j-api`, `logback-classic`
- Main class: `com.icon.agent.CollectorAgent`

### Phase 2 — Configuration Models
- `AgentConfig` → `List<TargetConfig>` + global `healthPort`
- `TargetConfig` → `id`, `rpc (RpcConfig)`, `collectors (CollectorSetConfig)`
- `TlsConfig` → `keystorePath`, `keystorePassword`, `truststorePath`, `truststorePassword`
- `ConfigManager` → YAML loader via `ObjectMapper(YAMLFactory)`

### Phase 3 — TLS / mTLS
- `SslContextFactory` builds `SSLContext` from PKCS12 keystore + truststore
- Falls back to one-way TLS when no client cert configured
- Works on Windows + Linux

### Phase 4 — File-Based Position Store
- `data/{targetId}/positions.dat` — JSON map `fileKey → PositionRecord`
- Atomic write: `.tmp` → `FileChannel.force(true)` → `Files.move(ATOMIC_MOVE)`
- Corrupted file detection via `JsonParseException`; treated as fresh start

### Phase 5 — File Collector (Per-Target)
- Tail-based polling via `ScheduledExecutorService`
- Rotation detection: `BasicFileAttributes.fileKey()` (Linux inode); fallback canonical path + creation time (Windows)
- Handles rename rotation and copytruncate
- Backpressure: pause if `RecordQueue` full

### Phase 6 — JDBC Collector (Optional Per-Target)
- Enabled per target via `jdbc.enabled = true`
- Tracks `jdbcLastValue` in `PositionStore`

### Phase 7 — Record Queue
- `ArrayBlockingQueue<Record>` per target, configurable capacity
- `offer()` returns false when full (collector pauses)

### Phase 8 — Batch Builder
- Accumulates records up to `maxBatchSize` or `maxBatchMs`
- Produces `Batch` (UUID batchId + list of records)

### Phase 9 — RPC Client (WebSocket)
- `java.net.http.HttpClient` + `WebSocket.Builder` per target
- Injects per-target `SSLContext`
- Sends batch JSON, waits for ACK containing `batchId`
- On disconnect: notifies `SpoolManager`, reconnects with exponential backoff + jitter

### Phase 10 — Spool Manager
- Directory: `spool/{targetId}/`
- Write: `{timestamp}-{batchId}.spool.tmp` → rename → `.spool`
- Replay: sorted by filename (timestamp order), delete after ACK
- `IOException` scoped to that target only

### Phase 11 — Target Context & Manager
- `TargetContext` wires all per-target components; `start()` / `stop()` lifecycle
- Exceptions caught internally — do not propagate to other targets
- `TargetManager` creates and starts all contexts

### Phase 12 — Health Endpoint
- `com.sun.net.httpserver.HttpServer` (JDK built-in, no extra deps)
- `GET /health` → JSON with per-target `rpcConnected`, `queueDepth`, `spoolDepth`

### Phase 13 — Shutdown Manager
- JVM shutdown hook: stop collectors → flush queues → persist positions → close RPC

### Phase 14 — Tests
| Test | Verifies |
|------|---------|
| `MultiTargetIsolationTest` | systemA failure doesn't affect systemB |
| `FileRotationTest` | rename + copytruncate detection |
| `RestartResumeTest` | position store survives crash, resumes correctly |
| `NetworkOutageTest` | spool used when RPC fails (per-target) |
| `SpoolRecoveryTest` | spool replay in order after reconnect |

---

## Verification

```powershell
# Build fat jar
.\gradlew shadowJar

# Run tests
.\gradlew test

# Start agent
java -jar build/libs/collector-agent-all.jar -c config/config.yaml

# Health check
curl http://localhost:8080/health
```
