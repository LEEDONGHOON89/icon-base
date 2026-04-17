package com.icon.agent.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

/**
 * Parser for JSON files. Validates JSON content.
 */
public class JsonParser implements LineParser {
    private static final Logger log = LoggerFactory.getLogger(JsonParser.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    @Override
    public String parse(String line) {
        // [2026-02-25] 빈 줄 입력 가드: null 반환 → FileCollector의 isEmpty() 체크에서 건너뜀
        if (line == null || line.trim().isEmpty()) {
            return null;
        }
        try {
            // Validate JSON
            JsonNode node = mapper.readTree(line);
            return mapper.writeValueAsString(node);
        } catch (Exception e) {
            log.warn("Invalid JSON line: {}", e.getMessage());
            return line; // Return as-is if invalid? Or skip? For now return as-is.
        }
    }

    @Override
    public Map<String, String> getAdditionalMetadata(String line) {
        return Collections.emptyMap();
    }
}
