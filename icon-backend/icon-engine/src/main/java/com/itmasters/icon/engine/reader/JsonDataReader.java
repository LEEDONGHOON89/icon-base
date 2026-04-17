package com.itmasters.icon.engine.reader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * JSON 데이터 리더
 */
@Component
public class JsonDataReader implements DataReader {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    @Override
    public List<Map<String, Object>> read(String filePath) throws Exception {
        return objectMapper.readValue(
            new File(filePath), 
            new TypeReference<List<Map<String, Object>>>() {}
        );
    }
}