package com.itmasters.icon.engine.reader;

import java.util.List;
import java.util.Map;

/**
 * 데이터 리더 인터페이스
 * - 데이터소스에 있는 데이터를 읽음.
 */
public interface DataReader {
    
    /**
     * 파일에서 데이터를 읽어서 List<Map> 형태로 반환
     * @param filePath 파일 경로
     * @return 데이터 목록
     */
    List<Map<String, Object>> read(String filePath) throws Exception;
}