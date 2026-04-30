package com.itmasters.icon.engine.datasource.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineDsFileSystemConfigEntity;
import com.itmasters.icon.engine.datasource.repository.FileSystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * FILE_SYSTEM_REALTIME incremental file reader service.
 *
 * Algorithm:
 * 1. List files matching filePattern under watchDirectory
 * 2. Load last-read byte offset from positions.json for each file
 * 3. Seek to offset and read only new lines using RandomAccessFile
 * 4. CSV: read headers on first scan, parse subsequent lines as data rows
 *    JSON: parse each line as a JSON object
 *    LOG : return { "message": "..." }
 * 5. Detect file rotation (rename / copytruncate) and reset offset if needed
 * 6. Persist updated positions.json after collection
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileSystemRealtimeService {

    private final FileSystemConfigRepository configRepository;
    private final FileSystemRealtimePositionStore positionStore;

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();

    public List<Map<String, Object>> readIncrementalData(String dataSourceId) {
        List<EngineDsFileSystemConfigEntity> configs =
                configRepository.findAllByDataSourceId(dataSourceId);

        if (configs.isEmpty()) {
            log.warn("[{}] No ds_file_system_config found.", dataSourceId);
            return List.of();
        }

        Map<String, FileSystemRealtimePositionRecord> positions = positionStore.load(dataSourceId);
        List<Map<String, Object>> allRecords = new ArrayList<>();
        boolean dirty = false;

        for (EngineDsFileSystemConfigEntity config : configs) {
            try {
                List<Path> files = resolveMatchingFiles(config);
                for (Path filePath : files) {
                    try {
                        int before = allRecords.size();
                        boolean changed = pollFile(dataSourceId, filePath, positions, allRecords, config);
                        dirty |= changed;
                        int added = allRecords.size() - before;
                        if (added > 0) {
                            log.info("[{}] incremental read - {}: {} records", dataSourceId,
                                    filePath.getFileName(), added);
                        }
                    } catch (Exception e) {
                        log.error("[{}] file poll error - {}: {}", dataSourceId, filePath, e.getMessage(), e);
                    }
                }
            } catch (Exception e) {
                log.error("[{}] file list error: {}", dataSourceId, e.getMessage(), e);
            }
        }

        if (dirty) {
            positionStore.save(dataSourceId, positions);
        }

        log.info("[{}] incremental read complete - total {} records", dataSourceId, allRecords.size());
        return allRecords;
    }

    private List<Path> resolveMatchingFiles(EngineDsFileSystemConfigEntity config) throws IOException {
        Path dir = Paths.get(config.getWatchDirectory());
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            log.warn("Watch directory not found: {}", config.getWatchDirectory());
            return List.of();
        }
        String pattern = config.getFilePattern() != null ? config.getFilePattern() : "*";
        PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> matcher.matches(p.getFileName()))
                    .collect(Collectors.toList());
        }
    }

    private boolean pollFile(String dataSourceId,
                             Path filePath,
                             Map<String, FileSystemRealtimePositionRecord> positions,
                             List<Map<String, Object>> out,
                             EngineDsFileSystemConfigEntity config) throws IOException {

        BasicFileAttributes attrs = Files.readAttributes(filePath, BasicFileAttributes.class);
        String currentKey = extractFileKey(attrs, filePath);
        long currentSize = attrs.size();

        FileSystemRealtimePositionRecord pos = positions.get(currentKey);
        if (pos == null) {
            Optional<FileSystemRealtimePositionRecord> byPath = positions.values().stream()
                    .filter(r -> filePath.toAbsolutePath().toString().equals(r.getFilePath()))
                    .findFirst();
            if (byPath.isPresent()) {
                FileSystemRealtimePositionRecord oldRecord = byPath.get();
                positions.remove(oldRecord.getFileKey());

                // [2026-04-20] Windows에서 에디터 저장 시 creationTime 변경으로 fileKey가 바뀌는 경우 처리.
                // currentSize >= oldOffset이면 파일 내용이 보존된 단순 재생성(에디터 저장)이므로
                // 기존 offset과 headers를 그대로 이어받아 중복 수집 방지.
                // currentSize < oldOffset인 경우에만 진짜 rotation으로 판단하여 offset 리셋.
                if (currentSize >= oldRecord.getOffset()) {
                    log.info("[{}] File re-created (editor save, key changed) - {}, keeping offset: {}",
                            dataSourceId, filePath.getFileName(), oldRecord.getOffset());
                    pos = new FileSystemRealtimePositionRecord(
                            filePath.toAbsolutePath().toString(), currentKey, oldRecord.getOffset());
                    pos.setHeaders(oldRecord.getHeaders());
                } else {
                    log.info("[{}] File rotation detected (rename) - {}, resetting position",
                            dataSourceId, filePath.getFileName());
                    pos = new FileSystemRealtimePositionRecord(
                            filePath.toAbsolutePath().toString(), currentKey, 0L);
                }
            } else {
                pos = new FileSystemRealtimePositionRecord(
                        filePath.toAbsolutePath().toString(), currentKey, 0L);
            }
            positions.put(currentKey, pos);
        }

        if (currentSize < pos.getOffset()) {
            log.info("[{}] File rotation detected (truncate) - {}, resetting offset: {} -> 0",
                    dataSourceId, filePath.getFileName(), pos.getOffset());
            pos.setOffset(0L);
            pos.setHeaders(null);
        }

        if (pos.getOffset() >= currentSize) {
            return false;
        }

        Charset charset = resolveCharset(config);
        String format = detectFormat(filePath.getFileName().toString());
        // [2026-04-22] config를 readNewLines에 전달하여 maxLinesPerPoll/maxRecordBytes 적용
        int linesRead = readNewLines(filePath, pos, charset, format, config, out);

        if (linesRead > 0) {
            pos.setLastUpdatedMs(System.currentTimeMillis());
            return true;
        }
        return false;
    }

    /**
     * [2026-04-22] config 파라미터 추가 — maxLinesPerPoll / maxRecordBytes 백엔드 직접 수집에도 적용.
     *   maxLinesPerPoll : 폴 당 최대 처리 라인 수 (0 또는 NULL → 1000 기본값)
     *   maxRecordBytes  : 라인 최대 바이트 크기 (0 또는 NULL → 제한 없음)
     *                     초과 시 라인을 잘라내고 [TRUNCATED] 마커를 추가한 뒤 파싱
     */
    private int readNewLines(Path filePath,
                             FileSystemRealtimePositionRecord pos,
                             Charset charset,
                             String format,
                             EngineDsFileSystemConfigEntity config,
                             List<Map<String, Object>> out) throws IOException {
        // [2026-04-22] 제한값 결정 (NULL/0 → 기본값)
        int maxLines     = (config.getMaxLinesPerPoll() != null && config.getMaxLinesPerPoll() > 0)
                           ? config.getMaxLinesPerPoll() : 1000;
        int maxBytes     = (config.getMaxRecordBytes()  != null && config.getMaxRecordBytes()  > 0)
                           ? config.getMaxRecordBytes()  : 0; // 0 = 제한 없음

        int linesRead = 0;
        boolean isFirstRead = (pos.getOffset() == 0L);

        try (RandomAccessFile raf = new RandomAccessFile(filePath.toFile(), "r")) {

            if ("CSV".equals(format)) {
                if (isFirstRead) {
                    String headerLine = readLine(raf, charset);
                    if (headerLine == null) return 0;
                    pos.setHeaders(parseCsvLine(headerLine));
                    pos.setOffset(raf.getFilePointer());
                } else {
                    raf.seek(pos.getOffset());
                }
            } else {
                raf.seek(pos.getOffset());
            }

            String line;
            // [2026-04-22] maxLinesPerPoll 건수 기준 루프 종료
            while (linesRead < maxLines && (line = readLine(raf, charset)) != null) {
                if (line.trim().isEmpty()) {
                    pos.setOffset(raf.getFilePointer());
                    continue;
                }
                // [2026-04-22] maxRecordBytes 초과 시 라인 잘라내기
                if (maxBytes > 0) {
                    line = truncateLineIfNeeded(line, charset, maxBytes, filePath.getFileName().toString());
                }
                // [2026-04-21] lineNumber 파라미터 제거 (_line 필드 삭제에 따른 정리)
                Map<String, Object> record = parseLine(line, format, pos.getHeaders());
                if (record != null && !record.isEmpty()) {
                    out.add(record);
                    linesRead++;
                }
                pos.setOffset(raf.getFilePointer());
            }
        }
        return linesRead;
    }

    /**
     * [2026-04-22] 라인 바이트 크기가 maxBytes를 초과하면 잘라내고 [TRUNCATED] 마커를 추가한다.
     * icon-agent FileCollector.truncateIfNeeded()와 동일한 방식.
     */
    private String truncateLineIfNeeded(String line, Charset charset, int maxBytes, String source) {
        byte[] bytes = line.getBytes(charset);
        if (bytes.length <= maxBytes) return line;
        int approxChars = (int) ((long) maxBytes * line.length() / bytes.length) - 60;
        approxChars = Math.max(0, approxChars);
        String marker = String.format("...[TRUNCATED: %d→%d bytes]", bytes.length, maxBytes);
        log.warn("[백엔드 직접 수집] 레코드 크기 초과 — 잘라냄: {} ({} bytes > {} bytes 제한)",
                source, bytes.length, maxBytes);
        return line.substring(0, approxChars) + marker;
    }

    // [2026-04-21] _line(행 번호) 필드 제거 — landing_records에 불필요한 메타데이터 저장 방지
    //              lineNumber 파라미터도 함께 제거
    private Map<String, Object> parseLine(String line, String format, List<String> headers) {
        switch (format) {
            case "CSV":
                return parseCsvRecord(line, headers);
            case "JSON":
                return parseJsonLine(line);
            default:
                Map<String, Object> rec = new LinkedHashMap<>();
                rec.put("message", line);
                return rec;
        }
    }

    private Map<String, Object> parseCsvRecord(String line, List<String> headers) {
        List<String> values = parseCsvLine(line);
        Map<String, Object> rec = new LinkedHashMap<>();

        if (headers != null && !headers.isEmpty()) {
            for (int i = 0; i < headers.size(); i++) {
                String header = headers.get(i).trim();
                String value = (i < values.size()) ? values.get(i).trim() : "";
                rec.put(header, (value.isEmpty() || "null".equalsIgnoreCase(value)) ? null : value);
            }
        } else {
            for (int i = 0; i < values.size(); i++) {
                rec.put("col_" + i, values.get(i));
            }
        }
        return rec;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonLine(String line) {
        try {
            return JSON_MAPPER.readValue(line, Map.class);
        } catch (Exception e) {
            Map<String, Object> rec = new LinkedHashMap<>();
            rec.put("raw", line);
            return rec;
        }
    }

    private List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result;
    }

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
        if (key != null) return key.toString();
        try {
            return path.toAbsolutePath().toRealPath() + "@" + attrs.creationTime().toMillis();
        } catch (IOException e) {
            return path.toAbsolutePath() + "@" + attrs.creationTime().toMillis();
        }
    }

    private Charset resolveCharset(EngineDsFileSystemConfigEntity config) {
        String enc = config.getFileEncoding();
        if (enc == null || enc.isBlank()) return StandardCharsets.UTF_8;
        try {
            return Charset.forName(enc);
        } catch (Exception e) {
            return StandardCharsets.UTF_8;
        }
    }

    private String detectFormat(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".csv")) return "CSV";
        if (lower.endsWith(".json")) return "JSON";
        return "LOG";
    }
}