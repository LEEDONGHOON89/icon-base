package com.icon.agent.collector;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

/**
 * Parser for CSV files. Converts a line into a JSON string based on columns.
 *
 * <p>csvHasHeader=true 인 경우:
 * <ul>
 *   <li>csvColumns 미설정: {@link #applyHeaderLine(String)} 을 통해 파일 첫 줄에서 컬럼명을 추출한다.</li>
 *   <li>csvColumns 설정: 미리 설정된 컬럼명을 사용하고, 파일 첫 줄(헤더)은 FileCollector 가 offset 조정으로 건너뛴다.</li>
 * </ul>
 */
public class CsvParser implements LineParser {
    private static final Logger log = LoggerFactory.getLogger(CsvParser.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String delimiter;
    private final boolean csvHasHeader;

    /** 실제 사용할 컬럼 목록. 초기화 시 null 이면 헤더 라인에서 추출 대기 */
    private volatile String[] columns;

    /**
     * true: 컬럼이 확정된 상태 (csvColumns 미리 설정됐거나 applyHeaderLine 호출됨).
     * false: csvHasHeader=true 이고 아직 헤더 미추출 상태.
     */
    private volatile boolean headerProcessed;

    /**
     * @param csvHasHeader      CSV 파일에 헤더 행 포함 여부
     * @param delimiter         CSV 구분자 (null 이면 "," 사용)
     * @param configuredColumns csvColumns 설정값. null/blank 이면 헤더에서 추출
     */
    public CsvParser(boolean csvHasHeader, String delimiter, String configuredColumns) {
        this.csvHasHeader = csvHasHeader;
        this.delimiter = delimiter != null ? delimiter : ",";

        if (configuredColumns != null && !configuredColumns.isBlank()) {
            // csvColumns 명시 → 미리 설정된 컬럼 사용
            this.columns = configuredColumns.split(",");
            this.headerProcessed = true;
            log.debug("CsvParser: 미리 설정된 컬럼 사용 → {}", Arrays.toString(this.columns));
        } else {
            this.columns = null;
            // csvHasHeader=false 이고 컬럼도 없으면 raw 라인 fallback
            this.headerProcessed = !csvHasHeader;
        }
    }

    /**
     * CSV 파일의 헤더 라인에서 컬럼명을 추출한다.
     * FileCollector 가 파일 첫 줄을 읽은 뒤 이 메서드를 호출하여 컬럼을 확정한다.
     *
     * @param headerLine 파일의 첫 번째 줄 (헤더 행)
     */
    public synchronized void applyHeaderLine(String headerLine) {
        if (headerLine == null || headerLine.trim().isEmpty()) {
            log.warn("CsvParser: 헤더 라인이 비어 있어 컬럼 추출 스킵");
            return;
        }
        String[] cols = headerLine.split(delimiter);
        for (int i = 0; i < cols.length; i++) {
            cols[i] = cols[i].trim();
        }
        this.columns = cols;
        this.headerProcessed = true;
        log.info("CsvParser: 헤더에서 컬럼 추출 완료 → {}", Arrays.toString(cols));
    }

    /** csvHasHeader 설정 값 반환 */
    public boolean isCsvHasHeader() {
        return csvHasHeader;
    }

    /**
     * 컬럼이 확정된 상태인지 반환.
     * false 인 경우 FileCollector 가 byte 0 에서 헤더를 읽어 {@link #applyHeaderLine(String)} 을 호출해야 한다.
     */
    public boolean isHeaderProcessed() {
        return headerProcessed;
    }

    @Override
    public String parse(String line) {
        // [2026-02-25] 빈 줄 가드
        if (line == null || line.trim().isEmpty()) {
            return null;
        }

        String[] values = line.split(delimiter);

        // [2026-03-12] csvHasHeader=false + csvColumns 미설정:
        // 첫 번째 데이터 라인의 값 개수를 기준으로 column1, column2, ... 자동 생성
        if (columns == null || columns.length == 0) {
            String[] auto = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                auto[i] = "column" + (i + 1);
            }
            this.columns = auto;
            this.headerProcessed = true;
            log.info("CsvParser: 컬럼 미설정 → 자동 컬럼명 생성: {}", Arrays.toString(auto));
        }
        ObjectNode node = mapper.createObjectNode();

        for (int i = 0; i < columns.length; i++) {
            String colName = columns[i].trim();
            String val = (i < values.length) ? values[i] : "";
            node.put(colName, val);
        }

        try {
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Failed to convert CSV line to JSON: {}", e.getMessage());
            return line;
        }
    }

    @Override
    public Map<String, String> getAdditionalMetadata(String line) {
        return Collections.emptyMap();
    }
}
