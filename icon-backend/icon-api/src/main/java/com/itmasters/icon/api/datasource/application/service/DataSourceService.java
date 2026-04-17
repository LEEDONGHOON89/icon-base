package com.itmasters.icon.api.datasource.application.service;

import com.itmasters.icon.api.datasource.dto.DataSourceDto;
import com.itmasters.icon.api.datasource.application.port.out.DataSourceRepository;
import com.itmasters.icon.api.datasource.adapter.out.persistence.entity.DataSourceEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 데이터 소스 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DataSourceService {
    
    private final DataSourceRepository dataSourceRepository;
    
    /**
     * 데이터 소스 생성
     */
    @Transactional
    public DataSourceDto.Info createDataSource(DataSourceDto.CreateCommand command) {
        log.info("Creating data source: {}", command.getName());
        
        // 동일 이름의 데이터 소스가 있는지 확인
        if (dataSourceRepository.existsByName(command.getName())) {
            throw new RuntimeException("이미 존재하는 데이터 소스 이름입니다: " + command.getName());
        }
        
        // DataSourceEntity 생성
        DataSourceEntity dataSource = DataSourceEntity.from(command);
        
        DataSourceEntity savedDataSource = dataSourceRepository.save(dataSource);
        log.info("Data source created successfully: {}", savedDataSource.getDataSourceId());
        
        // DEFAULT Profile 자동 생성
//        try {
//            dataProfileUseCase.createProfile(
//                savedDataSource.getDataSourceId(),
//                "DEFAULT",
//                ProfilePurpose.DEFAULT,
//                "자동 생성된 기본 프로파일",
//                0
//            );
//            log.info("DEFAULT profile created for data source: {}", savedDataSource.getDataSourceId());
//        } catch (Exception e) {
//            log.warn("Failed to create DEFAULT profile for data source {}: {}",
//                savedDataSource.getDataSourceId(), e.getMessage());
//            // Profile 생성 실패가 DataSource 생성을 막지 않도록 함
//        }
        
        return savedDataSource.toInfo();
    }
    
    /**
     * 데이터 소스 수정
     */
    @Transactional
    public DataSourceDto.Info updateDataSource(DataSourceDto.UpdateCommand command) {
        DataSourceEntity dataSource = dataSourceRepository.findById(command.getDataSourceId())
                .orElseThrow(() -> new RuntimeException("데이터 소스를 찾을 수 없습니다. ID: " + command.getDataSourceId()));
        
        // 이름 중복 검사 (자신 제외)
        if (command.getName() != null && !command.getName().equals(dataSource.getName())) {
            if (dataSourceRepository.existsByName(command.getName())) {
                throw new RuntimeException("이미 존재하는 데이터 소스 이름입니다: " + command.getName());
            }
        }
        
        // Entity의 업데이트 메서드 사용
        dataSource.update(command);
        
        DataSourceEntity updatedDataSource = dataSourceRepository.save(dataSource);
        log.info("Data source updated successfully: {}", updatedDataSource.getDataSourceId());
        
        // TODO: Rule mapping count 구현 필요
        Long ruleCount = 0L;
        return updatedDataSource.toInfoWithRuleCount(ruleCount);
    }
    
    /**
     * 데이터 소스 단건 조회
     */
    public DataSourceDto.Info getDataSource(String dataSourceId) {
        DataSourceEntity dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("데이터 소스를 찾을 수 없습니다. ID: " + dataSourceId));
        
        // TODO: Rule mapping count 구현 필요
        Long ruleCount = 0L;
        return dataSource.toInfoWithRuleCount(ruleCount);
    }
    
    /**
     * 전체 데이터 소스 목록 조회
     */
    public List<DataSourceDto.Info> getAllDataSources() {
        List<DataSourceEntity> dataSources = dataSourceRepository.findAll();
        
        return dataSources.stream()
                .map(dataSource -> {
                    // TODO: Rule mapping count 구현 필요
                    Long ruleCount = 0L;
                    return dataSource.toInfoWithRuleCount(ruleCount);
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 데이터 소스 삭제
     */
    @Transactional
    public void deleteDataSource(String dataSourceId) {
        log.info("Deleting data source: {}", dataSourceId);
        
        DataSourceEntity dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("데이터 소스를 찾을 수 없습니다. ID: " + dataSourceId));
        
        // TODO: 매핑된 규칙 체크 구현 필요
        // long mappingCount = mappingRepository.countByDataSourceId(dataSourceId);
        // if (mappingCount > 0) {
        //     throw new RuntimeException("매핑된 규칙이 있어 삭제할 수 없습니다. 매핑 수: " + mappingCount);
        // }
        
        dataSourceRepository.delete(dataSource);
        log.info("Data source deleted successfully: {}", dataSourceId);
    }
    
    /**
     * 데이터 소스 활성화
     */
    @Transactional
    public DataSourceDto.Info activateDataSource(String dataSourceId) {
        DataSourceEntity dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("데이터 소스를 찾을 수 없습니다. ID: " + dataSourceId));
        
        dataSource.activate();
        DataSourceEntity savedDataSource = dataSourceRepository.save(dataSource);
        
        // TODO: Rule mapping count 구현 필요
        Long ruleCount = 0L;
        return savedDataSource.toInfoWithRuleCount(ruleCount);
    }
    
    /**
     * 데이터 소스 비활성화
     */
    @Transactional
    public DataSourceDto.Info deactivateDataSource(String dataSourceId) {
        DataSourceEntity dataSource = dataSourceRepository.findById(dataSourceId)
                .orElseThrow(() -> new RuntimeException("데이터 소스를 찾을 수 없습니다. ID: " + dataSourceId));
        
        dataSource.deactivate();
        DataSourceEntity savedDataSource = dataSourceRepository.save(dataSource);
        
        // TODO: Rule mapping count 구현 필요
        Long ruleCount = 0L;
        return savedDataSource.toInfoWithRuleCount(ruleCount);
    }
    
    // 기본 데이터 소스 설정 기능 제거
}