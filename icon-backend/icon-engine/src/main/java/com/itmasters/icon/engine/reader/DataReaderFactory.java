package com.itmasters.icon.engine.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * DataReader 팩토리
 * 파일 확장자에 따라 적절한 DataReader를 선택
 */
@Component
@RequiredArgsConstructor
public class DataReaderFactory {
    
    private final CsvDataReader csvDataReader;
    private final JsonDataReader jsonDataReader;
    
    /**
     * 파일 경로에 따라 적절한 DataReader를 선택하여 데이터 읽기
     * @param filePath 파일 경로
     * @return 읽은 데이터
     * @throws Exception 읽기 실패 시
     */
    public List<Map<String, Object>> read(String filePath) throws Exception {
        String lowerPath = filePath.toLowerCase();
        
        if (lowerPath.endsWith(".csv")) {
            return csvDataReader.read(filePath);
        } else if (lowerPath.endsWith(".json")) {
            return jsonDataReader.read(filePath);
        } else {
            throw new IllegalArgumentException("지원하지 않는 파일 형식입니다: " + filePath);
        }
    }
    
    /**
     * 파일 형식 감지
     * @param filePath 파일 경로
     * @return 파일 형식 (CSV, JSON 등)
     */
    public String detectFileFormat(String filePath) {
        String lowerPath = filePath.toLowerCase();
        
        if (lowerPath.endsWith(".csv")) {
            return "CSV";
        } else if (lowerPath.endsWith(".json")) {
            return "JSON";
        } else {
            return "UNKNOWN";
        }
    }
}