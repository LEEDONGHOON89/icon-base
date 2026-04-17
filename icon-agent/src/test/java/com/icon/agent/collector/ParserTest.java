package com.icon.agent.collector;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    @Test
    void testCsvParser() {
        CsvParser parser = new CsvParser(",", "time,level,msg");
        String line = "2026-02-24,INFO,Hello World";
        String parsed = parser.parse(line);

        assertTrue(parsed.contains("\"time\":\"2026-02-24\""));
        assertTrue(parsed.contains("\"level\":\"INFO\""));
        assertTrue(parsed.contains("\"msg\":\"Hello World\""));
    }

    @Test
    void testJsonParser() {
        JsonParser parser = new JsonParser();
        String line = "{\"key\":\"value\", \"number\":123}";
        String parsed = parser.parse(line);

        assertTrue(parsed.contains("\"key\":\"value\""));
        assertTrue(parsed.contains("\"number\":123"));
    }

    @Test
    void testLogParser() {
        LogParser parser = new LogParser();
        String line = "standard log line";
        assertEquals(line, parser.parse(line));
    }
}
