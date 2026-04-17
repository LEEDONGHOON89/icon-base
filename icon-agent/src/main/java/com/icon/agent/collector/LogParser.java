package com.icon.agent.collector;

import java.util.Collections;
import java.util.Map;

/**
 * Parser for standard log files (returns line as-is).
 */
public class LogParser implements LineParser {
    @Override
    public String parse(String line) {
        return line;
    }

    @Override
    public Map<String, String> getAdditionalMetadata(String line) {
        return Collections.emptyMap();
    }
}
