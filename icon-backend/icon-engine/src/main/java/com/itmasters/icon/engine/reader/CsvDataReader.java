package com.itmasters.icon.engine.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.itmasters.icon.engine.config.Const.METADATA_LINE_NUMBER;

/**
 * CSV 데이터 리더
 * 대용량 파일 처리 및 다양한 데이터 타입 자동 변환 지원
 */
@Slf4j
@Component
public class CsvDataReader implements DataReader {
    
    private static final String CSV_DELIMITER = ",";
    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = Arrays.asList(
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"),
        DateTimeFormatter.ISO_LOCAL_DATE_TIME
    );
    
    @Override
    public List<Map<String, Object>> read(String filePath) throws Exception {
        log.info("CSV 파일 읽기 시작: {}", filePath);
        List<Map<String, Object>> data = new ArrayList<>();
        
        try (BufferedReader br = new BufferedReader(
                new FileReader(filePath, StandardCharsets.UTF_8))) {
            
            // 헤더 읽기
            String headerLine = br.readLine();
            if (headerLine == null) {
                log.warn("빈 CSV 파일: {}", filePath);
                return data;
            }
            
            String[] headers = parseCSVLine(headerLine);
            log.debug("CSV 헤더: {}", Arrays.toString(headers));
            
            // 데이터 읽기
            String line;
            int lineNumber = 1;
            
            while ((line = br.readLine()) != null) {
                lineNumber++;
                try {
                    Map<String, Object> record = parseRecord(line, headers, lineNumber);
                    data.add(record);
                } catch (Exception e) {
                    log.error("CSV 파싱 오류 - 라인 {}: {}", lineNumber, line, e);
                    // 오류가 있어도 계속 진행
                }
            }
            
            log.info("CSV 파일 읽기 완료: {} 건", data.size());
        } catch (IOException e) {
            log.error("파일 읽기 오류: {}", filePath, e);
            throw new Exception("CSV 파일 읽기 실패: " + filePath, e);
        }
        
        return data;
    }
    
    /**
     * CSV 라인 파싱 (따옴표 처리 포함)
     */
    private String[] parseCSVLine(String line) {
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
        return result.toArray(new String[0]);
    }
    
    /**
     * 레코드 파싱
     */
    private Map<String, Object> parseRecord(String line, String[] headers, int lineNumber) {
        String[] values = parseCSVLine(line);
        Map<String, Object> record = new LinkedHashMap<>();
        
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();
            String value = (i < values.length) ? values[i].trim() : "";
            
            // 빈 값 처리
            if (value.isEmpty() || "null".equalsIgnoreCase(value)) {
                record.put(header, null);
            } else {
                record.put(header, parseValue(value, header));
            }
        }
        
        // 메타데이터 추가
        record.put(METADATA_LINE_NUMBER, lineNumber);
        
        return record;
    }
    
    /**
     * 값 타입 자동 변환
     */
    private Object parseValue(String value, String fieldName) {
        // 1. Boolean 체크
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        
        // 2. 날짜/시간 체크 (필드명에 date, time, dt 포함 시 우선 시도)
        if (fieldName.toLowerCase().contains("date") || 
            fieldName.toLowerCase().contains("time") ||
            fieldName.toLowerCase().contains("dt")) {
            String formattedDateTime = tryParseDateTimeAsString(value);
            if (formattedDateTime != null) {
                return formattedDateTime;
            }
        }
        
        // 3. 숫자 체크
        try {
            // 금액 관련 필드는 BigDecimal로 처리
            if (fieldName.toLowerCase().contains("amount") || 
                fieldName.toLowerCase().contains("amt") ||
                fieldName.toLowerCase().contains("price")) {
                return new BigDecimal(value.replaceAll(",", ""));
            }
            
            // 일반 숫자 처리
            if (value.contains(".")) {
                return Double.parseDouble(value.replaceAll(",", ""));
            } else {
                return Long.parseLong(value.replaceAll(",", ""));
            }
        } catch (NumberFormatException e) {
            // 숫자가 아님
        }
        
        // 4. 날짜/시간 재시도 (모든 필드)
        String formattedDateTime = tryParseDateTimeAsString(value);
        if (formattedDateTime != null) {
            return formattedDateTime;
        }
        
        // 5. 문자열로 반환
        return value;
    }
    
    /**
     * 날짜/시간 파싱 시도
     */
    private LocalDateTime tryParseDateTime(String value) {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                return LocalDateTime.parse(value, formatter);
            } catch (DateTimeParseException e) {
                // 다음 포맷터 시도
            }
        }
        return null;
    }
    
    /**
     * 날짜/시간을 파싱하여 포맷팅된 문자열로 반환
     * 날짜만 있으면 yyyy-MM-dd
     * 날짜와 시간이 있으면 yyyy-MM-dd HH:mm:ss
     */
    private String tryParseDateTimeAsString(String value) {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(value, formatter);
                
                // 시간 부분이 00:00:00이면 날짜만 반환
                if (dateTime.getHour() == 0 && dateTime.getMinute() == 0 && dateTime.getSecond() == 0) {
                    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } else {
                    return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
            } catch (DateTimeParseException e) {
                // 다음 포맷터 시도
            }
        }
        
        // LocalDate로도 시도
        try {
            java.time.LocalDate date = java.time.LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
            return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            // 무시
        }
        
        // 다양한 날짜 형식 시도
        String[] datePatterns = {
            "yyyy/MM/dd", "yyyy.MM.dd", "dd/MM/yyyy", "dd-MM-yyyy", 
            "MM/dd/yyyy", "MM-dd-yyyy", "yyyyMMdd"
        };
        
        for (String pattern : datePatterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                java.time.LocalDate date = java.time.LocalDate.parse(value, formatter);
                return date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (DateTimeParseException e) {
                // 다음 패턴 시도
            }
        }
        
        return null;
    }
}