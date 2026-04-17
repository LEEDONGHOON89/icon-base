# Java Lightweight Collector Agent — Task Checklist

## Phase 1: Project Bootstrap & Build Setup
- [x] Create project directory structure
- [x] Create `docs/implementation_plan.md`
- [x] Create `docs/task.md`
- [x] `build.gradle` — fat-jar, Jackson, SLF4J/Logback, JUnit 5
- [x] `settings.gradle` & `gradle/libs.versions.toml`
- [x] `logback.xml` — multi-target identification via MDC

## Phase 2: Configuration Models & Manager
- [x] `AgentConfig` & `TargetConfig`
- [x] `ConfigManager` with Jackson YAML support

## Phase 3: TLS & Security
- [x] `SslContextFactory` — PKCS12 support for mTLS

## Phase 4: Persistence (Position Store)
- [x] `PositionStore` interface
- [x] `FilePositionStore` — atomic/fsync support
- [x] `PositionRecord` entity

## Phase 5: Core Collection
- [x] `Record` model
- [x] `FileCollector` — rotation-safe, tail-based
- [x] `JdbcCollector` — offset-based (incremental)

## Phase 6: Queuing & Batching
- [x] `RecordQueue` — bounded with backpressure
- [x] `Batch` & `BatchBuilder` — time/size triggers

## Phase 7: Delivering (RPC Client)
- [x] `RpcClient` — Java 11 WebSocket based
- [x] ACK handling & Pending ACK management

## Phase 8: Spooling & Recovery
- [x] `SpoolManager` — durable disk buffering
- [x] Spool-to-RPC replay logic

## Phase 9: Management & Lifecycle
- [x] `TargetContext` — per-target isolation
- [x] `TargetManager` — multi-target orchestration
- [x] `HealthServer` — per-target health status
- [x] `ShutdownManager` — graceful flushing
- [x] `CollectorAgent` — main entry point

## Phase 10: Verification & Docs
- [x] Sample `config.yaml`
- [x] `README.md` with Mermaid diagrams
- [x] Unit/Integration Tests:
    - [x] `RestartResumeTest`
    - [x] `FileRotationTest`
    - [x] `NetworkOutageTest`
    - [x] `MultiTargetIsolationTest`
    - [x] `SpoolRecoveryTest`
