package com.icon.agent.monitor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.icon.agent.target.TargetManager;
import com.icon.agent.target.TargetContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.management.*;
// [2026-02-25] 스레드 상태별 분류를 위한 ThreadInfo 임포트
import java.lang.management.ThreadInfo;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 에이전트 시스템 자원 및 내부 상태 모니터링.
 * [2026-02-25] 에이전트 안정적 운영을 위한 모니터링 기능 추가
 *
 * 수집 지표:
 *   [JVM]
 *   - cpu_percent       : JVM 프로세스 CPU 사용률 (%)
 *   - heap_used_mb      : Heap 사용 중인 메모리 (MB)
 *   - heap_max_mb       : Heap 최대 메모리 (MB)
 *   - heap_percent      : Heap 사용률 (%)
 *   - non_heap_used_mb  : Non-Heap 사용 메모리 (MB, 메타스페이스 등)
 *   - gc_count          : GC 누적 횟수 (GC가 자주 발생하면 힙 부족 신호)
 *   - gc_time_ms        : GC 누적 소요 시간 (ms, Stop-The-World 영향 감지)
 *   - thread_count      : 현재 활성 스레드 수 (증가 추세면 스레드 누수 의심)
 *   - uptime_sec        : JVM 가동 시간 (초, 비정상 재시작 감지)
 *
 *   [OS]
 *   - sys_memory_used_mb  : 시스템 메모리 사용량 (MB)
 *   - sys_memory_total_mb : 시스템 전체 메모리 (MB)
 *   - sys_memory_percent  : 시스템 메모리 사용률 (%)
 *   - open_fd_count       : Open File Descriptor 수 (Linux, 파일 핸들 누수 감지)
 *   - spool_disk_used_mb  : 스풀 디렉토리 디스크 사용량 (MB)
 *
 *   [에이전트]
 *   - targets[].id          : 대상 ID
 *   - targets[].connected   : WebSocket 연결 상태
 *   - targets[].queue_depth : RecordQueue 깊이 (증가 추세면 전송 병목 신호)
 *   - targets[].spool_depth : Spool 파일 수 (증가 추세면 서버 연결 장애 신호)
 *
 * 출력 형식: JSON Lines (1줄 = 1 스냅샷)
 *   → 이후 FileCollector로 수집하여 내부통제시스템 서버에서 확인 가능
 */
public class AgentMonitor {

    private static final Logger log = LoggerFactory.getLogger(AgentMonitor.class);
    private static final Logger metricsLog = LoggerFactory.getLogger("agent.metrics");
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS");

    private final MonitoringConfig config;
    private final TargetManager targetManager;

    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;
    private final RuntimeMXBean runtimeMXBean;
    private final List<GarbageCollectorMXBean> gcMXBeans;
    // [2026-02-25] com.sun.management.OperatingSystemMXBean: CPU/시스템 메모리 접근
    private final com.sun.management.OperatingSystemMXBean osMXBean;

    private final ScheduledExecutorService scheduler;

    // [2026-02-25] GC 델타 계산용 이전 값 저장
    private long prevGcCount = 0;
    private long prevGcTimeMs = 0;

    public AgentMonitor(MonitoringConfig config, TargetManager targetManager) {
        this.config = config;
        this.targetManager = targetManager;

        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        this.gcMXBeans = ManagementFactory.getGarbageCollectorMXBeans();

        // [2026-02-25] OperatingSystemMXBean → com.sun.management 확장 인터페이스로 캐스팅
        //              CPU 사용률, 시스템 메모리 접근에 필요
        OperatingSystemMXBean osBase = ManagementFactory.getOperatingSystemMXBean();
        this.osMXBean = (osBase instanceof com.sun.management.OperatingSystemMXBean)
                ? (com.sun.management.OperatingSystemMXBean) osBase
                : null;

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "agent-monitor");
            t.setDaemon(true);
            return t;
        });
    }

    /** 모니터링 시작 */
    public void start() {
        if (!config.isEnabled()) {
            log.info("[AgentMonitor] 모니터링 비활성화됨 (config.monitoring.enabled=false)");
            return;
        }
        scheduler.scheduleWithFixedDelay(
                this::collect,
                5,  // 초기 5초 대기 (에이전트 기동 완료 후 수집)
                config.getIntervalMs(),
                TimeUnit.MILLISECONDS);
        log.info("[AgentMonitor] 모니터링 시작 — 수집 주기: {}ms, 로그: {}",
                config.getIntervalMs(), config.getLogPath());
    }

    /** 모니터링 중지 */
    public void stop() {
        scheduler.shutdownNow();
        try {
            scheduler.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("[AgentMonitor] 모니터링 중지");
    }

    /** 메트릭 수집 및 로그 기록 */
    private void collect() {
        try {
            ObjectNode snapshot = buildSnapshot();
            String line = JSON.writeValueAsString(snapshot);

            // [2026-02-25] 전용 로거로 출력 (logback에서 별도 파일로 라우팅)
            metricsLog.info(line);

        } catch (Exception e) {
            log.error("[AgentMonitor] 메트릭 수집 오류: {}", e.getMessage(), e);
        }
    }

    /** 메트릭 스냅샷 JSON 빌드 */
    private ObjectNode buildSnapshot() {
        ObjectNode root = JSON.createObjectNode();

        // ── 공통 ──────────────────────────────────────────────
        root.put("timestamp", LocalDateTime.now().format(TS_FMT));
        root.put("uptime_sec", runtimeMXBean.getUptime() / 1000);

        // ── JVM CPU ───────────────────────────────────────────
        // [2026-02-25] getProcessCpuLoad(): JVM 프로세스 CPU 사용률 (0.0 ~ 1.0, -1이면 미지원)
        if (osMXBean != null) {
            double cpuLoad = osMXBean.getProcessCpuLoad();
            root.put("cpu_percent", cpuLoad >= 0 ? Math.round(cpuLoad * 1000.0) / 10.0 : -1);
        } else {
            root.put("cpu_percent", -1);
        }

        // ── Java Heap ─────────────────────────────────────────
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        long heapUsedMb  = heap.getUsed()    / (1024 * 1024);
        long heapMaxMb   = heap.getMax()     / (1024 * 1024);
        long heapPercent = heapMaxMb > 0 ? heapUsedMb * 100 / heapMaxMb : 0;

        root.put("heap_used_mb",  heapUsedMb);
        root.put("heap_max_mb",   heapMaxMb);
        root.put("heap_percent",  heapPercent);

        // [2026-02-25] Non-Heap: 메타스페이스(클래스 메타데이터), JIT 코드 캐시 등
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();
        root.put("non_heap_used_mb", nonHeap.getUsed() / (1024 * 1024));

        // ── GC ───────────────────────────────────────────────
        // [2026-02-25] 이번 수집 주기 동안의 GC 증분값 계산 (누적값 - 이전값)
        long totalGcCount = 0;
        long totalGcTimeMs = 0;
        for (GarbageCollectorMXBean gc : gcMXBeans) {
            long cnt = gc.getCollectionCount();
            long t   = gc.getCollectionTime();
            if (cnt >= 0) totalGcCount  += cnt;
            if (t   >= 0) totalGcTimeMs += t;
        }
        long deltaGcCount  = totalGcCount  - prevGcCount;
        long deltaGcTimeMs = totalGcTimeMs - prevGcTimeMs;
        prevGcCount  = totalGcCount;
        prevGcTimeMs = totalGcTimeMs;

        root.put("gc_count",      totalGcCount);   // 누적 횟수
        root.put("gc_time_ms",    totalGcTimeMs);  // 누적 소요시간
        root.put("gc_delta_count",  deltaGcCount);   // 이번 주기 GC 횟수
        root.put("gc_delta_time_ms", deltaGcTimeMs); // 이번 주기 GC 소요시간

        // ── Thread ────────────────────────────────────────────
        // [2026-02-25] 스레드 상태별 분류 추가
        //   RUNNABLE     (running)      : CPU를 점유하며 실행 중
        //   WAITING      (waiting)      : Object.wait() 등 무기한 대기 (I/O, 락 해제 대기)
        //   TIMED_WAITING(timed_waiting): Thread.sleep(), wait(ms) 등 시간 제한 대기
        //   BLOCKED      (blocked)      : 모니터 락 획득 대기 → 값이 증가하면 락 경합 신호
        //   other                       : NEW / TERMINATED 등
        //   peak                        : JVM 기동 이후 최고 동시 스레드 수 (스레드 누수 추적)
        long[] threadIds = threadMXBean.getAllThreadIds();
        ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadIds);

        int thrRunnable = 0, thrWaiting = 0, thrTimedWaiting = 0, thrBlocked = 0, thrOther = 0;
        for (ThreadInfo ti : threadInfos) {
            if (ti == null) continue; // 이미 종료된 스레드는 null
            switch (ti.getThreadState()) {
                case RUNNABLE:       thrRunnable++;      break;
                case WAITING:        thrWaiting++;       break;
                case TIMED_WAITING:  thrTimedWaiting++;  break;
                case BLOCKED:        thrBlocked++;       break;
                default:             thrOther++;         break;
            }
        }

        ObjectNode threads = root.putObject("threads");
        threads.put("total",         threadMXBean.getThreadCount());
        threads.put("running",       thrRunnable);
        threads.put("waiting",       thrWaiting);
        threads.put("timed_waiting", thrTimedWaiting);
        threads.put("blocked",       thrBlocked);
        threads.put("other",         thrOther);
        // [2026-02-25] peak: JVM 기동 이후 최고 동시 스레드 수 (이상 증가 추세 감지)
        threads.put("peak",          threadMXBean.getPeakThreadCount());

        // ── OS 메모리 ─────────────────────────────────────────
        // [2026-02-25] getTotalMemorySize() / getFreeMemorySize(): 시스템 전체/여유 메모리
        if (osMXBean != null) {
            long sysTotalMb = osMXBean.getTotalMemorySize()  / (1024 * 1024);
            long sysFreeRaw = osMXBean.getFreeMemorySize()   / (1024 * 1024);
            long sysUsedMb  = sysTotalMb - sysFreeRaw;
            long sysPercent = sysTotalMb > 0 ? sysUsedMb * 100 / sysTotalMb : 0;

            root.put("sys_memory_used_mb",  sysUsedMb);
            root.put("sys_memory_total_mb", sysTotalMb);
            root.put("sys_memory_percent",  sysPercent);
        }

        // ── Open File Descriptor (Linux) ───────────────────────
        // [2026-02-25] UnixOperatingSystemMXBean: Linux/macOS 전용, Windows에서는 -1
        if (osMXBean instanceof com.sun.management.UnixOperatingSystemMXBean) {
            long openFd = ((com.sun.management.UnixOperatingSystemMXBean) osMXBean).getOpenFileDescriptorCount();
            root.put("open_fd_count", openFd);
        } else {
            root.put("open_fd_count", -1); // Windows 미지원
        }

        // ── Spool 디스크 사용량 ───────────────────────────────
        root.put("spool_disk_used_mb", calcDirSizeMb(config.getSpoolRootPath()));

        // ── Target별 에이전트 내부 상태 ───────────────────────
        ArrayNode targets = root.putArray("targets");
        if (targetManager != null) {
            for (TargetContext ctx : targetManager.getContexts()) {
                ObjectNode t = targets.addObject();
                t.put("id",          ctx.getTargetId());
                t.put("connected",   ctx.isRpcConnected());
                t.put("queue_depth", ctx.getQueueDepth());
                t.put("spool_depth", ctx.getSpoolDepth());
            }
        }

        return root;
    }

    /**
     * 지정 디렉토리의 전체 크기를 MB 단위로 계산한다.
     * [2026-02-25] 스풀 디렉토리 디스크 사용량 계산에 사용
     *              디렉토리가 없거나 접근 불가 시 -1 반환
     */
    private long calcDirSizeMb(String dirPath) {
        try {
            Path dir = Path.of(dirPath);
            if (!Files.exists(dir)) {
                return 0;
            }
            long totalBytes = Files.walk(dir)
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0;
                        }
                    })
                    .sum();
            return totalBytes / (1024 * 1024);
        } catch (Exception e) {
            log.debug("[AgentMonitor] 디렉토리 크기 계산 실패: {} ({})", dirPath, e.getMessage());
            return -1;
        }
    }
}
