package com.itmasters.icon.engine.repository;

import com.itmasters.icon.engine.adapter.out.persistence.entity.ExecDsMpEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.ExecDsMpRepository;
import com.itmasters.icon.common.domain.type.ExecutionMode;
import com.itmasters.icon.common.domain.type.ExecutionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import com.itmasters.icon.engine.adapter.out.persistence.repository.ExecDsMpRepositoryImpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
//@DataJpaTest
//@Import({ExecDsMpRepositoryImpl.class, com.itmasters.icon.engine.config.TestJpaConfig.class})
//@TestPropertySource(properties = {
//    "spring.jpa.hibernate.ddl-auto=create",
//})
class ExecDsMpRepositoryTest {

    @Autowired
    private ExecDsMpRepository repository;

    @Test
    @DisplayName("ExecDsMp 엔티티 저장 및 조회 테스트")
    void testSaveAndFind() {
        // Given
        ExecDsMpEntity entity = ExecDsMpEntity.builder()
            .dataSourceId("DS001")
            .executionMode(ExecutionMode.MANUAL)
            .executedBy("test-user")
            .startDt(LocalDateTime.now())
            .status(ExecutionStatus.RUNNING)
            .executionContext(new HashMap<>())
            .build();

        // When
        ExecDsMpEntity saved = repository.save(entity);
        Optional<ExecDsMpEntity> found = repository.findById(saved.getExecDsMpId());

        // Then
        assertThat(saved).isNotNull();
        assertThat(saved.getExecDsMpId()).isEqualTo(saved.getExecDsMpId());
        assertThat(found).isPresent();
        assertThat(found.get().getDataSourceId()).isEqualTo("DS001");
    }


    @Test
    @DisplayName("ExecDsMp 업데이트 테스트")
    void testUpdate() {
        // Given
        ExecDsMpEntity entity = ExecDsMpEntity.builder()
            .dataSourceId("DS001")
            .executionMode(ExecutionMode.MANUAL)
            .executedBy("test-user")
            .startDt(LocalDateTime.now())
            .status(ExecutionStatus.RUNNING)
            .executionContext(new HashMap<>())
            .build();
        
        entity.updateTotalRows(100);
        entity = repository.save(entity);

        // When
        entity.complete();
        repository.save(entity);
        
        // Then
        Optional<ExecDsMpEntity> updated = repository.findById(entity.getExecDsMpId());
        assertThat(updated).isPresent();
        assertThat(updated.get().getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
        assertThat(updated.get().getTotalRows()).isEqualTo(100);
        assertThat(updated.get().getCompleteAt()).isNotNull();
    }
}