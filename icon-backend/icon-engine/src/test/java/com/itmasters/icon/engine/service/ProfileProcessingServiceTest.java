package com.itmasters.icon.engine.service;

import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineProfileRepository;
import com.itmasters.icon.engine.dto.DataProcessingResultDto;
import com.itmasters.icon.engine.dto.DetectionContext;
import com.itmasters.icon.engine.dto.DetectionResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class ProfileProcessingServiceTest {
    
    @Autowired
    private ProfileProcessingService profileProcessingService;
    
    @Autowired
    private EngineProfileRepository engineProfileRepository;
    
    @Autowired
    private DataProcessingService dataProcessingService;
    
    @Test
    public void testProcessProfileWithMappedData() {
        System.out.println("===== ProfileProcessingService 테스트 시작 =====");
        
        // 1. 테스트 데이터 준비 (거래 데이터)
        List<Map<String, Object>> testData = new ArrayList<>();
        
        // 1천만원 이상 거래 데이터
        Map<String, Object> row1 = new HashMap<>();
        row1.put("TRX_DT", "2025-07-27 14:22:00");
        row1.put("TRX_AMT", 12000000L);  // Long 타입으로 저장
        row1.put("TRX_TYPE", "이체");
        row1.put("BAL_AMT", 17150000L);
        row1.put("SENDER", "거래처A");
        row1.put("RECEIVER", "내계좌");
        testData.add(row1);
        
        // 5백만원 거래 (매칭 안 됨)
        Map<String, Object> row2 = new HashMap<>();
        row2.put("TRX_DT", "2025-07-27 10:30:00");
        row2.put("TRX_AMT", 5000000L);
        row2.put("TRX_TYPE", "출금");
        row2.put("BAL_AMT", 5150000L);
        row2.put("SENDER", "내계좌");
        row2.put("RECEIVER", "ATM");
        testData.add(row2);
        
        // 1천5백만원 거래 (매칭됨)
        Map<String, Object> row3 = new HashMap<>();
        row3.put("TRX_DT", "2025-07-27 18:00:00");
        row3.put("TRX_AMT", 15000000L);
        row3.put("TRX_TYPE", "입금");
        row3.put("BAL_AMT", 20000000L);
        row3.put("SENDER", "회사");
        row3.put("RECEIVER", "내계좌");
        testData.add(row3);
        
        System.out.println("테스트 데이터 준비 완료: " + testData.size() + " 건");
        
        // 2. DataProcessingResultDto 생성
        DataProcessingResultDto rawData = DataProcessingResultDto.success(testData, testData, null);
        
        // 3. 프로파일 조회 (거래이상감지)
        String profileId = "0MFMJ8F4FPRZS";
        EngineProfileEntity profile = engineProfileRepository.findById(profileId)
            .orElseThrow(() -> new RuntimeException("프로파일을 찾을 수 없습니다: " + profileId));
        
        System.out.println("프로파일 조회 완료: " + profile.getProfileId() + " (" + profile.getProfileName() + ")");
        
        // 4. DetectionContext 생성
        DetectionContext context = DetectionContext.builder()
            .executionId(UUID.randomUUID().toString())
            .dataSourceId("DS001")
            .profileId(profileId)
            .startTime(LocalDateTime.now())
            .executionMode(ExecutionMode.MANUAL)
            .executedBy("test-user")
            .build();
        
        // 5. processProfile 실행
        System.out.println("===== processProfile 실행 시작 =====");
        
        try {
            DetectionResult result = profileProcessingService.processProfile(
                999L,  // 테스트용 execDsMpId
                rawData,
                profile,
                context,
                "test-user"
            );
            
            // 6. 결과 확인
            System.out.println("===== 탐지 결과 =====");
            System.out.println("전체 데이터: " + result.getTotalRows() + " 건");
            System.out.println("매칭된 데이터: " + result.getTotalMatched() + " 건");
            System.out.println("실행한 룰: " + result.getTotalRules() + " 개");
            System.out.println("성공 여부: " + result.isSuccess());
            
            if (result.getMatchedData() != null && !result.getMatchedData().isEmpty()) {
                System.out.println("===== 매칭된 상세 데이터 =====");
                result.getMatchedData().forEach(matched -> {
                    System.out.println("룰ID: " + matched.getRuleId() + 
                        ", 룰명: " + matched.getRuleName() + 
                        ", 조건: " + matched.getRuleCondition());
                    System.out.println("매핑된 데이터: " + matched.getMappedData());
                });
            }
            
            // 검증
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getTotalRows()).isEqualTo(3);
            
            // 1천만원 이상 거래가 2건이므로 2건이 매칭되어야 함
            System.out.println("예상 매칭: 2건 (12000000원, 15000000원)");
            System.out.println("실제 매칭: " + result.getTotalMatched() + " 건");
            
        } catch (Exception e) {
            System.err.println("processProfile 실행 중 오류: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    @Test
    public void testFieldMappingOnly() {
        System.out.println("===== 필드 매핑만 테스트 =====");

        // 테스트 데이터
        List<Map<String, Object>> testData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("TRX_AMT", 12000000L);
        row.put("TRX_DT", "2025-07-27 14:22:00");
        row.put("TRX_TYPE", "이체");
        row.put("BAL_AMT", 17150000L);
        testData.add(row);

        String profileId = "0MFMJ8F4FPRZS";

        // 프로파일 조회하여 dataSourceId 가져오기
        EngineProfileEntity profile = engineProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("프로파일을 찾을 수 없습니다: " + profileId));
        String dataSourceId = profile.getDataSourceId();

        // 필드 매핑 실행 (dataSourceId 사용)
        List<Map<String, Object>> mappedData = profileProcessingService.performFieldMapping(testData, dataSourceId);

        System.out.println("매핑 전 데이터: " + testData.get(0));
        System.out.println("매핑 후 데이터: " + (mappedData.isEmpty() ? "비어있음" : mappedData.get(0)));

        assertThat(mappedData).isNotEmpty();
        assertThat(mappedData.get(0)).containsKey("transaction_amount");
        assertThat(mappedData.get(0).get("transaction_amount")).isEqualTo(12000000L);
    }
    
    @Test
    public void testProcessProfileWithExistingData() {
        System.out.println("===== 기존 실행 데이터로 프로파일 처리 테스트 =====");
        
        // 파라미터 설정
        Long execDsMpId = 53L;  // 실제 실행 ID
        String profileId = "0MFMJ8F4FPRZS";  // 거래이상감지 프로파일
        
        System.out.println("실행 ID: " + execDsMpId + ", 프로파일 ID: " + profileId);
        
        try {
            // 1. 프로파일 조회
            EngineProfileEntity profile = engineProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("프로파일을 찾을 수 없습니다: " + profileId));
            
            System.out.println("프로파일 조회 완료: " + profile.getProfileId() + " (" + profile.getProfileName() + ")");
            
            // 2. 기존 실행 데이터 조회 (dataProcessingService 사용)
            DataProcessingResultDto existingData = dataProcessingService.loadExistingData(execDsMpId, profileId);
            System.out.println("기존 데이터 로드 완료: " + existingData.getDataCount() + " 건");
            
            if (existingData.getDataCount() > 0) {
                System.out.println("첫 번째 데이터 샘플: " + existingData.getMappedData().get(0));
            }
            
            // 3. DetectionContext 생성
            DetectionContext context = DetectionContext.builder()
                .executionId(UUID.randomUUID().toString())
                .dataSourceId("DS001")
                .profileId(profileId)
                .startTime(LocalDateTime.now())
                .executionMode(ExecutionMode.MANUAL)
                .executedBy("test-user")
                .build();
            
            // 4. processProfile 실행
            System.out.println("===== processProfile 실행 시작 =====");
            DetectionResult result = profileProcessingService.processProfile(
                execDsMpId,
                existingData,
                profile,
                context,
                "test-user"
            );
            
            // 5. 결과 확인
            System.out.println("===== 탐지 결과 =====");
            System.out.println("전체 데이터: " + result.getTotalRows() + " 건");
            System.out.println("매칭된 데이터: " + result.getTotalMatched() + " 건");
            System.out.println("실행한 룰: " + result.getTotalRules() + " 개");
            System.out.println("성공 여부: " + result.isSuccess());
            
            if (result.getMatchedData() != null && !result.getMatchedData().isEmpty()) {
                System.out.println("===== 매칭된 상세 데이터 =====");
                result.getMatchedData().forEach(matched -> {
                    System.out.println("룰ID: " + matched.getRuleId() + ", 룰명: " + matched.getRuleName());
                    System.out.println("룰 조건: " + matched.getRuleCondition());
                    System.out.println("매핑된 데이터: " + matched.getMappedData());
                });
            } else {
                System.out.println("매칭된 데이터가 없습니다.");
            }
            
            // 검증
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            
        } catch (Exception e) {
            System.err.println("테스트 실행 중 오류: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
