package com.icon.agent.collector;

import com.icon.agent.config.FileCollectorConfig;
import com.icon.agent.queue.RecordQueue;
import com.icon.agent.store.FilePositionStore;
import com.icon.agent.store.PositionRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tail-based file collector with cross-platform rotation detection and pattern
 * matching.
 */
public class FileCollector {

    private static final Logger log = LoggerFactory.getLogger(FileCollector.class);

    private final String targetId;
    private final FileCollectorConfig config;
    private final RecordQueue queue;
    private final FilePositionStore positionStore;
    private final LineParser parser;

    private final ScheduledExecutorService scheduler;
    private final Map<String, PositionRecord> positions;
    private volatile boolean running = false;

    public FileCollector(String targetId,
            FileCollectorConfig config,
            RecordQueue queue,
            FilePositionStore positionStore) throws IOException {
        this.targetId = targetId;
        this.config = config;
        this.queue = queue;
        this.positionStore = positionStore;
        this.positions = new HashMap<>(positionStore.loadAll());
        this.parser = createParser(config);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "file-collector-" + targetId + "-" + config.getId());
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * format이 명시(LOG 외 값)된 경우 그 값을 사용하고,
     * 미지정(기본값 "LOG")인 경우 fileNamePattern 확장자로 자동 감지한다.
     *   .csv  → CSV
     *   .json → JSON
     *   그 외  → LOG
     */
    static String resolveFormat(String configFormat, String fileNamePattern) {
        if (configFormat != null && !configFormat.equalsIgnoreCase("LOG")) {
            return configFormat.toUpperCase();
        }
        if (fileNamePattern != null) {
            String lower = fileNamePattern.toLowerCase();
            if (lower.endsWith(".csv"))  return "CSV";
            if (lower.endsWith(".json")) return "JSON";
        }
        return "LOG";
    }

    private LineParser createParser(FileCollectorConfig config) {
        String format = resolveFormat(config.getFormat(), config.getFileNamePattern());
        log.info("[{}] FileCollector ({}) resolved format={}", targetId, config.getFileNamePattern(), format);
        switch (format) {
            case "CSV":
                return new CsvParser(config.isCsvHasHeader(), config.getCsvDelimiter(), config.getCsvColumns());
            case "JSON":
                return new JsonParser();
            case "LOG":
            default:
                return new LogParser();
        }
    }

    public void start() {
        if (config.getDirectory() == null || config.getFileNamePattern() == null) {
            log.info("[{}] Incomplete file collector config (id={}), disabled", targetId, config.getId());
            return;
        }
        running = true;
        scheduler.scheduleWithFixedDelay(this::poll,
                0,
                config.getPollIntervalMs(),
                TimeUnit.MILLISECONDS);
        log.info("[{}] FileCollector ({}) started, path={}, pattern={}, format={}, poll={}ms",
                targetId, config.getName(), config.getDirectory(), config.getFileNamePattern(),
                resolveFormat(config.getFormat(), config.getFileNamePattern()), config.getPollIntervalMs());
    }

    /** 수집기 ID (동적 추가/제거 시 식별용) */
    public String getId() {
        return config.getId();
    }

    public void stop() {
        running = false;
        scheduler.shutdownNow();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("[{}] FileCollector ({}) did not terminate cleanly", targetId, config.getId());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        persistPositions();
        log.info("[{}] FileCollector ({}) stopped", targetId, config.getId());
    }

    void poll() {
        MDC.put("targetId", targetId);
        try {
            List<Path> matchingFiles = resolveMatchingFiles();
            for (Path path : matchingFiles) {
                try {
                    pollFile(path);
                } catch (Exception e) {
                    log.error("[{}] Error polling file {}: {}", targetId, path, e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("[{}] Error resolving matching files for {}: {}", targetId, config.getId(), e.getMessage());
        } finally {
            MDC.remove("targetId");
        }
    }

    private List<Path> resolveMatchingFiles() throws IOException {
        Path dir = Paths.get(config.getDirectory());
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return List.of();
        }

        final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + config.getFileNamePattern());

        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> matcher.matches(p.getFileName()))
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        }
    }

    private void pollFile(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
        String currentKey = extractFileKey(attrs, path);
        long currentSize = attrs.size();

        PositionRecord pos = positions.get(currentKey);
        if (pos == null) {
            PositionRecord byPath = findByPath(path.toAbsolutePath().toString());
            if (byPath != null && !byPath.getFileKey().equals(currentKey)) {
                log.info("[{}] Rotation detected (rename) for {}, resetting offset", targetId, path);
                positions.remove(byPath.getFileKey());
            }
            pos = new PositionRecord(currentKey, path.toAbsolutePath().toString(), 0);
            positions.put(currentKey, pos);
        }

        if (currentSize < pos.getOffset()) {
            log.info("[{}] Rotation detected (copytruncate) for {}, resetting offset from {} to 0",
                    targetId, path, pos.getOffset());
            pos.setOffset(0);
        }

        if (pos.getOffset() >= currentSize) {
            return;
        }

        Charset charset = Charset.forName(config.getCharset());

        // [2026-03-12] csvHasHeader=true 처리:
        // 1) 컬럼이 미확정(csvColumns 미설정)인 경우: byte 0에서 헤더 읽어 컬럼 추출
        // 2) 파일을 처음 읽는 경우(offset=0): 헤더 행을 데이터로 보내지 않도록 offset 조정
        if (parser instanceof CsvParser csvParser && csvParser.isCsvHasHeader()) {
            if (!csvParser.isHeaderProcessed()) {
                // csvColumns 미설정: 파일 첫 줄에서 컬럼명 추출 (재시작 후에도 동작)
                try (RandomAccessFile headerRaf = new RandomAccessFile(path.toFile(), "r")) {
                    String headerLine = readLine(headerRaf, charset);
                    if (headerLine != null && !headerLine.trim().isEmpty()) {
                        csvParser.applyHeaderLine(headerLine);
                    }
                } catch (IOException e) {
                    log.warn("[{}] CSV 헤더 읽기 실패 ({}): {}", targetId, path, e.getMessage());
                }
            }
            if (pos.getOffset() == 0 && currentSize > 0) {
                // 파일을 처음부터 읽는 경우: 헤더 행을 건너뛰도록 offset 조정
                try (RandomAccessFile skipRaf = new RandomAccessFile(path.toFile(), "r")) {
                    readLine(skipRaf, charset); // 헤더 한 줄 소비
                    pos.setOffset(skipRaf.getFilePointer());
                    log.debug("[{}] CSV 헤더 행 스킵, offset → {}", targetId, pos.getOffset());
                } catch (IOException e) {
                    log.warn("[{}] CSV 헤더 스킵 실패 ({}): {}", targetId, path, e.getMessage());
                }
            }
        }

        int linesRead = 0;
        try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
            raf.seek(pos.getOffset());
            String line;
            while (linesRead < config.getMaxLinesPerPoll() && (line = readLine(raf, charset)) != null) {

                // Apply Parser
                String content = parser.parse(line);

                // Skip empty lines to prevent duplicated empty records on tailing
                if (content == null || content.trim().isEmpty()) {
                    pos.setOffset(raf.getFilePointer());
                    continue;
                }

                // [2026-02-25] 3번: 레코드 크기 제한 — maxRecordBytes 초과 시 잘라내기
                content = truncateIfNeeded(content, path.toString());

                Map<String, String> extraMeta = parser.getAdditionalMetadata(line);

                Map<String, String> metadata = new HashMap<>();
                metadata.put("file", path.toString());
                metadata.put("offset", String.valueOf(raf.getFilePointer()));
                metadata.put("format", resolveFormat(config.getFormat(), config.getFileNamePattern()));
                if (extraMeta != null) {
                    metadata.putAll(extraMeta);
                }

                Record record = new Record(
                        targetId,
                        config.getId(),
                        Record.Source.FILE,
                        path.toString(),
                        content,
                        metadata);

                if (!queue.offer(record)) {
                    log.warn("[{}] Queue full — pausing file collection for this cycle", targetId);
                    // Since it wasn't added to queue, we don't update the position offset
                    break;
                }
                linesRead++;
                pos.setOffset(raf.getFilePointer());
            }
        }

        if (linesRead > 0) {
            log.info("[{}] Read {} record(s) from {}, new offset={}", targetId, linesRead, path, pos.getOffset());
            persistPositions();
        }
    }

    /**
     * Reads a line directly from RandomAccessFile to track accurate byte offset.
     */
    private String readLine(RandomAccessFile raf, Charset charset) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        int b;
        boolean foundNewline = false;
        while ((b = raf.read()) != -1) {
            if (b == '\n') {
                foundNewline = true;
                break;
            } else if (b == '\r') {
                long cur = raf.getFilePointer();
                int next = raf.read();
                if (next != '\n') {
                    raf.seek(cur);
                }
                foundNewline = true;
                break;
            }
            bos.write(b);
        }
        if (bos.size() == 0 && !foundNewline) {
            return null;
        }
        return bos.toString(charset);
    }

    private String extractFileKey(BasicFileAttributes attrs, Path path) {
        Object key = attrs.fileKey();
        if (key != null) {
            return key.toString();
        }
        try {
            return path.toAbsolutePath().toRealPath().toString()
                    + "@" + attrs.creationTime().toMillis();
        } catch (IOException e) {
            return path.toAbsolutePath().toString() + "@" + attrs.creationTime().toMillis();
        }
    }

    private PositionRecord findByPath(String absolutePath) {
        return positions.values().stream()
                .filter(p -> absolutePath.equals(p.getFilePath()))
                .findFirst()
                .orElse(null);
    }

    private void persistPositions() {
        try {
            positionStore.saveAll(positions);
        } catch (IOException e) {
            log.error("[{}] Failed to persist positions: {}", targetId, e.getMessage(), e);
        }
    }

    /**
     * content가 maxRecordBytes를 초과하면 잘라내고 [TRUNCATED] 마커를 추가한다.
     * [2026-02-25] 3번: 초과 레코드 잘라내기 — 대용량 레코드로 인한 배치 크기 폭증 방지
     *              maxRecordBytes=0이면 제한 없음
     *              원본 byte 크기 정보를 마커에 포함하여 서버에서 인지 가능하도록 함
     */
    private String truncateIfNeeded(String content, String source) {
        int limit = config.getMaxRecordBytes();
        if (limit <= 0 || content == null) {
            return content;
        }
        byte[] utf8Bytes = content.getBytes(StandardCharsets.UTF_8);
        if (utf8Bytes.length <= limit) {
            return content;
        }
        // 마커 공간(60자)을 제외한 근사 문자 수 계산
        int approxChars = (int) ((long) limit * content.length() / utf8Bytes.length) - 60;
        approxChars = Math.max(0, approxChars);
        String marker = String.format("...[TRUNCATED: %d→%d bytes]", utf8Bytes.length, limit);
        log.warn("[{}] 레코드 크기 초과 — 잘라냄: {} ({} bytes > {} bytes 제한)",
                targetId, source, utf8Bytes.length, limit);
        return content.substring(0, approxChars) + marker;
    }

    public String getDirectory() {
        return config.getDirectory();
    }

    public String getFileNamePattern() {
        return config.getFileNamePattern();
    }
}
