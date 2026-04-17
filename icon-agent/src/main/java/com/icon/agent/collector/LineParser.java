package com.icon.agent.collector;

import java.util.Map;

/**
 * Interface for parsing a single line from a file.
 */
public interface LineParser {
    /**
     * Parses a line into a content string (could be JSON string).
     */
    String parse(String line);

    /**
     * Returns additional metadata extracted from the line.
     */
    Map<String, String> getAdditionalMetadata(String line);
}
