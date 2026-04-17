package com.itmasters.icon.engine.service;

import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.engine.adapter.out.persistence.entity.EngineProfileEntity;
import com.itmasters.icon.engine.adapter.out.persistence.entity.MappedDataStorageEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.EngineProfileRepository;
import com.itmasters.icon.engine.adapter.out.persistence.repository.MappedDataStorageRepository;
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
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
public class DirectProfileTest {
    
    @Autowired
    private ProfileProcessingService profileProcessingService;
    
    @Autowired
    private EngineProfileRepository engineProfileRepository;
    
    @Autowired
    private MappedDataStorageRepository mappedDataStorageRepository;
    
    @Test
    public void testProfileWithExistingData() {
        // 파라미터 설정
        Long execDsMpId = 53L;  // 기존 실행 ID
        String profileId = "0MFMJ8F4FPRZS";  // 거래이상감지 프로파일
        
        System.out.println("===== 프로파일 테스트 시작 =====");
        System.out.println("exec_ds_mp_id: " + execDsMpId);
        System.out.println("profile_id: " + profileId);
        
        try {
            // 1. 기존 데이터 조회
            List<MappedDataStorageEntity> mappedEntities = 
                mappedDataStorageRepository.findByExecDsMpIdOrderByRowIndex(execDsMpId);
            
            System.out.println("매핑된 데이터 조회: " + mappedEntities.size() + " 건");
            
            // 2. Map 형태로 변환
            List<Map<String, Object>> mappedData = mappedEntities.stream()
                .map(entity -> entity.getRowData())
                .collect(Collectors.toList());
            
            // 첫 번째 데이터 확인
            if (!mappedData.isEmpty()) {
                System.out.println("첫 번째 데이터: " + mappedData.get(0));
                System.out.println("transaction_amount 타입: " + 
                    (mappedData.get(0).get("transaction_amount") != null ? 
                    mappedData.get(0).get("transaction_amount").getClass() : "null"));
            }
            
            // 3. DataProcessingResultDto 생성
            DataProcessingResultDto processedData = DataProcessingResultDto.success(mappedData, mappedData, null);
            
            // 4. 프로파일 조회
            EngineProfileEntity profile = engineProfileRepository.findById(profileId)
                .orElseThrow(() -> new RuntimeException("프로파일을 찾을 수 없습니다: " + profileId));
            
            // 5. Context 생성
            DetectionContext context = DetectionContext.builder()
                .executionId(UUID.randomUUID().toString())
                .dataSourceId("DS001")
                .profileId(profileId)
                .startTime(LocalDateTime.now())
                .executionMode(ExecutionMode.MANUAL)
                .executedBy("test-user")
                .build();
            
            // 6. processProfile 실행
            System.out.println("\n===== processProfile 실행 =====");
            
            DetectionResult result = profileProcessingService.processProfile(
                execDsMpId,
                processedData,
                profile,
                context,
                "test-user"
            );
            
            // 7. 결과 출력
            System.out.println("\n===== 탐지 결과 =====");
            System.out.println("성공 여부: " + result.isSuccess());
            System.out.println("전체 데이터: " + result.getTotalRows() + " 건");
            System.out.println("실행한 룰: " + result.getTotalRules() + " 개");
            System.out.println("매칭된 데이터: " + result.getTotalMatched() + " 건");
            
            // 매칭된 데이터 상세
            if (result.getTotalMatched() > 0 && result.getMatchedData() != null) {
                System.out.println("\n===== 매칭된 데이터 상세 =====");
                result.getMatchedData().forEach(matched -> {
                    System.out.println("룰: " + matched.getRuleName() + " (" + matched.getRuleId() + ")");
                    System.out.println("조건: " + matched.getRuleCondition());
                    System.out.println("데이터: " + matched.getMappedData());
                    System.out.println("---");
                });
            }
            
            // 검증
            assertThat(result).isNotNull();
            assertThat(result.isSuccess()).isTrue();
            
            // 1천만원 이상 거래 확인
            long largeTransactions = mappedData.stream()
                .filter(row -> {
                    Object amt = row.get("transaction_amount");
                    if (amt instanceof Number) {
                        return ((Number) amt).longValue() >= 10000000;
                    }
                    return false;
                })
                .count();
                
            System.out.println("\n1천만원 이상 거래: " + largeTransactions + " 건");
            System.out.println("실제 매칭: " + result.getTotalMatched() + " 건");
            
        } catch (Exception e) {
            System.err.println("테스트 실패: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
