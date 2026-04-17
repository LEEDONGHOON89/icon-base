package com.itmasters.icon.engine.reader;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CSV Reader 기본 테스트 (실제 파일 사용)
 */
class SimpleCsvReaderTest {
    
    @Test
    void testReadActualCsvFile() throws Exception {
        // Given
        CsvDataReader reader = new CsvDataReader();
        String filePath = "test/resources/samples/financial_transactions.csv";
        
        // When
        List<Map<String, Object>> data = reader.read(filePath);
        
        // Then
        System.out.println("\n=== CSV 파일 읽기 테스트 ===");
        System.out.println("총 레코드 수: " + data.size());
        
        assertThat(data).isNotEmpty();
        
        // 첫 번째 레코드 확인
        Map<String, Object> firstRecord = data.get(0);
        System.out.println("\n첫 번째 레코드:");
        firstRecord.forEach((key, value) -> {
            if (!key.startsWith("_")) { // 메타데이터 제외
                System.out.printf("  %s = %s (%s)\n", 
                    key, value, 
                    value != null ? value.getClass().getSimpleName() : "null");
            }
        });
        
        // 타입 검증
        assertThat(firstRecord.get("TRX_DT")).isInstanceOf(LocalDateTime.class);
        assertThat(firstRecord.get("TRX_AMT")).isInstanceOf(BigDecimal.class);
        assertThat(firstRecord.get("TRX_TYPE")).isInstanceOf(String.class);
        
        // 값 검증
        BigDecimal amount = (BigDecimal) firstRecord.get("TRX_AMT");
        assertThat(amount).isEqualTo(new BigDecimal("1500000"));
        
        String type = (String) firstRecord.get("TRX_TYPE");
        assertThat(type).isEqualTo("입금");
        
        // 대량 거래 찾기 (예: 1000만원 이상)
        System.out.println("\n=== 대량 거래 검색 (1000만원 이상) ===");
        BigDecimal threshold = new BigDecimal("10000000");
        
        data.stream()
            .filter(record -> {
                BigDecimal amt = (BigDecimal) record.get("TRX_AMT");
                return amt.compareTo(threshold) >= 0;
            })
            .forEach(record -> {
                System.out.printf("Line %d: %s %,d원 (%s → %s)\n",
                    record.get("_lineNumber"),
                    record.get("TRX_TYPE"),
                    ((BigDecimal) record.get("TRX_AMT")).longValue(),
                    record.get("SENDER"),
                    record.get("RECEIVER"));
            });
        
        System.out.println("\n✅ CSV 파일 읽기 성공!");
    }
}