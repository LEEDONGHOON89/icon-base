package com.itmasters.icon.engine.reader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CsvDataReaderTest {
    
    private CsvDataReader csvDataReader;
    
    @BeforeEach
    void setUp() {
        csvDataReader = new CsvDataReader();
    }
    
    @Test
    void testReadEmptyFile(@TempDir Path tempDir) throws Exception {
        // Given
        File emptyFile = tempDir.resolve("empty.csv").toFile();
        emptyFile.createNewFile();
        
        // When
        List<Map<String, Object>> result = csvDataReader.read(emptyFile.getAbsolutePath());
        
        // Then
        assertThat(result).isEmpty();
    }
    
    @Test
    void testReadSimpleCsv(@TempDir Path tempDir) throws Exception {
        // Given
        File csvFile = createCsvFile(tempDir, "simple.csv", 
            "name,age,city\n" +
            "John,25,Seoul\n" +
            "Jane,30,Tokyo\n"
        );
        
        // When
        List<Map<String, Object>> result = csvDataReader.read(csvFile.getAbsolutePath());
        
        // Then
        assertThat(result).hasSize(2);
        
        Map<String, Object> firstRow = result.get(0);
        assertThat(firstRow.get("name")).isEqualTo("John");
        assertThat(firstRow.get("age")).isEqualTo(25L);
        assertThat(firstRow.get("city")).isEqualTo("Seoul");
        
        Map<String, Object> secondRow = result.get(1);
        assertThat(secondRow.get("name")).isEqualTo("Jane");
        assertThat(secondRow.get("age")).isEqualTo(30L);
        assertThat(secondRow.get("city")).isEqualTo("Tokyo");
    }
    
    @Test
    void testReadWithQuotedValues(@TempDir Path tempDir) throws Exception {
        // Given
        File csvFile = createCsvFile(tempDir, "quoted.csv",
            "name,description,price\n" +
            "\"Product A\",\"This is a, comma separated, description\",1000.50\n" +
            "\"Product B\",\"Simple description\",2000\n"
        );
        
        // When
        List<Map<String, Object>> result = csvDataReader.read(csvFile.getAbsolutePath());
        
        // Then
        assertThat(result).hasSize(2);
        
        Map<String, Object> firstRow = result.get(0);
        assertThat(firstRow.get("name")).isEqualTo("Product A");
        assertThat(firstRow.get("description")).isEqualTo("This is a, comma separated, description");
        assertThat(firstRow.get("price")).isEqualTo(new BigDecimal("1000.50"));
    }
    
    @Test
    void testReadFinancialData(@TempDir Path tempDir) throws Exception {
        // Given
        File csvFile = createCsvFile(tempDir, "financial.csv",
            "TRX_DT,TRX_AMT,TRX_TYPE,BAL_AMT,SENDER,RECEIVER\n" +
            "2025-01-27 09:15:00,1500000,입금,5500000,홍길동,내계좌\n" +
            "2025-01-27 10:30:00,350000,출금,5150000,내계좌,편의점ATM\n"
        );
        
        // When
        List<Map<String, Object>> result = csvDataReader.read(csvFile.getAbsolutePath());
        
        // Then
        assertThat(result).hasSize(2);
        
        Map<String, Object> firstRow = result.get(0);
        assertThat(firstRow.get("TRX_DT")).isInstanceOf(LocalDateTime.class);
        assertThat(firstRow.get("TRX_AMT")).isEqualTo(new BigDecimal("1500000"));
        assertThat(firstRow.get("TRX_TYPE")).isEqualTo("입금");
        assertThat(firstRow.get("BAL_AMT")).isEqualTo(new BigDecimal("5500000"));
        assertThat(firstRow.get("SENDER")).isEqualTo("홍길동");
        assertThat(firstRow.get("RECEIVER")).isEqualTo("내계좌");
        
        // 날짜 확인
        LocalDateTime txDate = (LocalDateTime) firstRow.get("TRX_DT");
        assertThat(txDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .isEqualTo("2025-01-27 09:15:00");
    }
    
    @Test
    void testReadWithNullValues(@TempDir Path tempDir) throws Exception {
        // Given
        File csvFile = createCsvFile(tempDir, "with_nulls.csv",
            "id,name,email,phone\n" +
            "1,John,john@email.com,\n" +
            "2,Jane,,010-1234-5678\n" +
            "3,Bob,null,null\n"
        );
        
        // When
        List<Map<String, Object>> result = csvDataReader.read(csvFile.getAbsolutePath());
        
        // Then
        assertThat(result).hasSize(3);
        
        assertThat(result.get(0).get("phone")).isNull();
        assertThat(result.get(1).get("email")).isNull();
        assertThat(result.get(2).get("email")).isNull();
        assertThat(result.get(2).get("phone")).isNull();
    }
    
    @Test
    void testReadWithVariousDataTypes(@TempDir Path tempDir) throws Exception {
        // Given
        File csvFile = createCsvFile(tempDir, "types.csv",
            "is_active,score,price,created_dt,description\n" +
            "true,95.5,12345.67,2025-01-27 10:00:00,Test product\n" +
            "false,80,99999,2025-01-27T11:00:00,Another product\n"
        );
        
        // When
        List<Map<String, Object>> result = csvDataReader.read(csvFile.getAbsolutePath());
        
        // Then
        assertThat(result).hasSize(2);
        
        Map<String, Object> firstRow = result.get(0);
        assertThat(firstRow.get("is_active")).isEqualTo(true);
        assertThat(firstRow.get("score")).isEqualTo(95.5);
        assertThat(firstRow.get("price")).isEqualTo(new BigDecimal("12345.67"));
        assertThat(firstRow.get("created_dt")).isInstanceOf(LocalDateTime.class);
        assertThat(firstRow.get("description")).isEqualTo("Test product");
    }
    
    @Test
    void testReadNonExistentFile() {
        // Given
        String nonExistentFile = "/path/to/non/existent/file.csv";
        
        // When & Then
        assertThatThrownBy(() -> csvDataReader.read(nonExistentFile))
            .isInstanceOf(Exception.class)
            .hasMessageContaining("CSV 파일 읽기 실패");
    }
    
    @Test
    void testReadRealFinancialTransactionsFile() throws Exception {
        // Given
        String filePath = "test/resources/samples/financial_transactions.csv";
        File file = new File(filePath);
        
        // 파일이 존재하는 경우에만 테스트 실행
        if (file.exists()) {
            // When
            List<Map<String, Object>> result = csvDataReader.read(filePath);
            
            // Then
            assertThat(result).isNotEmpty();
            
            // 첫 번째 레코드 검증
            Map<String, Object> firstRow = result.get(0);
            assertThat(firstRow).containsKeys("TRX_DT", "TRX_AMT", "TRX_TYPE", "BAL_AMT", "SENDER", "RECEIVER");
            assertThat(firstRow.get("TRX_AMT")).isInstanceOf(BigDecimal.class);
            assertThat(firstRow.get("TRX_DT")).isInstanceOf(LocalDateTime.class);
            
            // 메타데이터 확인
            assertThat(firstRow).containsKey("_lineNumber");
            assertThat(firstRow.get("_lineNumber")).isEqualTo(2);
        }
    }
    
    private File createCsvFile(Path tempDir, String fileName, String content) throws IOException {
        File file = tempDir.resolve(fileName).toFile();
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
        return file;
    }
}