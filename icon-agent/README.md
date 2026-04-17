# Java Lightweight Collector Agent

A production-grade, lightweight Java Collector Agent (Pure Java, no Spring Boot).

## Features
- **Multi-Target Isolation**: 1 Agent → N Control Systems. Failure in one target does not affect others.
- **TLS/mTLS Support**: Certificate-based security for WebSocket connections.
- **Durable Persistence**: File-based position store (no SQLite) and disk spooling on network failure.
- **At-least-once Delivery**: Guaranteed delivery via ACK handling and retry logic.
- **Cross-Platform**: Tested on Windows and Linux.

## Prerequisites
- JDK 17+
- Gradle (optional, wrapper included)

## Build
To build the fat jar:
```powershell
./gradlew shadowJar
```
The output jar will be in `build/libs/collector-agent.jar`.

## Run
```powershell
java -jar build/libs/collector-agent.jar -c config/config.yaml
```

## Health check
```powershell
curl http://localhost:8080/health
```

## Architecture Flow

### Normal Flow
```mermaid
sequenceDiagram
    participant FC as FileCollector
    participant RQ as RecordQueue
    participant BB as BatchBuilder
    participant RC as RpcClient
    participant WS as Control System (WS)

    FC->>RQ: offer(Record)
    BB->>RQ: drain(maxSize, timeout)
    BB->>RC: send(Batch)
    RC->>WS: JSON Body
    WS-->>RC: ACK (batchId)
    RC->>RC: Remove pending ACK
```

### Spool Recovery Flow
```mermaid
sequenceDiagram
    participant RC as RpcClient
    participant SM as SpoolManager
    participant Disk as FileSystem

    Note over RC: Connection Lost
    RC->>SM: write(Batch)
    SM->>Disk: write to spool/targetId/
    Note over RC: Reconnected
    RC->>SM: listPending()
    SM->>Disk: read from disk
    RC->>RC: send(Batch)
    RC->>SM: ack(Entry)
    SM->>Disk: delete spool file
```

## Target Isolation Flow
```mermaid
graph TD
    Agent[Collector Agent] --> TM[TargetManager]
    TM --> TA[TargetContext: systemA]
    TM --> TB[TargetContext: systemB]

    subgraph systemA
        TA --> QA[Queue A]
        QA --> BA[BatchBuilder A]
        BA --> RA[RpcClient A]
        RA --> WSA[WebSocket A]
    end

    subgraph systemB
        TB --> QB[Queue B]
        QB --> BB[BatchBuilder B]
        BB --> RB[RpcClient B]
        RB --> WSB[WebSocket B]
    end

    WSA -- Connectivity Failure --- RA
    NoticeA[Failure in System A leads to Spooling A]
    NoticeB[System B continues unaffected]
```
"# icon-agent" 
