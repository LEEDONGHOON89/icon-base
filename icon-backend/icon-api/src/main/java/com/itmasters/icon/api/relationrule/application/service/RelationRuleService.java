package com.itmasters.icon.api.relationrule.application.service;

import com.itmasters.icon.api.relationrule.dto.RelationRuleDto;
import com.itmasters.icon.engine.adapter.out.persistence.entity.RelationRuleEntity;
import com.itmasters.icon.engine.adapter.out.persistence.repository.RelationRuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 도메인 관계 규칙 관리 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelationRuleService {

    private final RelationRuleRepository relationRuleRepository;

    /**
     * 전체 도메인 관계 규칙 조회
     *
     * @return 모든 규칙 목록
     */
    public List<RelationRuleDto.Response> getAllRelationRules() {
        log.debug("전체 도메인 관계 규칙 조회");

        return relationRuleRepository.findAll()
                .stream()
                .map(RelationRuleDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * DataSource별 도메인 관계 규칙 조회
     *
     * @param dataSourceId 데이터소스 ID
     * @return 해당 DataSource의 규칙 목록
     */
    public List<RelationRuleDto.Response> getRelationRulesByDataSource(String dataSourceId) {
        log.debug("DataSource별 도메인 관계 규칙 조회 - dataSourceId: {}", dataSourceId);

        return relationRuleRepository.findByDataSourceId(dataSourceId)
                .stream()
                .map(RelationRuleDto.Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 단일 도메인 관계 규칙 조회
     *
     * @param ruleId 규칙 ID
     * @return 규칙 정보
     */
    public RelationRuleDto.Response getRelationRule(Long ruleId) {
        log.debug("도메인 관계 규칙 조회 - ruleId: {}", ruleId);

        RelationRuleEntity entity = relationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("규칙을 찾을 수 없습니다. ID: " + ruleId));

        return RelationRuleDto.Response.from(entity);
    }

    /**
     * 도메인 관계 규칙 생성
     *
     * @param request 생성 요청 DTO
     * @return 생성된 규칙 정보
     */
    @Transactional
    public RelationRuleDto.Response createRelationRule(RelationRuleDto.CreateRequest request) {
        log.info("도메인 관계 규칙 생성 - dataSourceId: {}, {}({}) --{}-->  {}({})",
                request.getDataSourceId(),
                request.getFromEntityType(),
                request.getFromIdField(),
                request.getRelationType(),
                request.getToEntityType(),
                request.getToIdField());

        // 중복 체크 (같은 DataSource에 같은 관계 규칙이 있는지)
        List<RelationRuleEntity> existingRules = relationRuleRepository
                .findByDataSourceIdAndFromEntityTypeAndToEntityTypeAndIsActive(
                        request.getDataSourceId(),
                        request.getFromEntityType(),
                        request.getToEntityType(),
                        true
                );

        for (RelationRuleEntity existing : existingRules) {
            if (existing.getRelationType().equals(request.getRelationType())) {
                throw new IllegalStateException(String.format(
                        "이미 동일한 관계 규칙이 존재합니다: %s %s --%s--> %s %s",
                        request.getFromEntityType(),
                        request.getFromIdField(),
                        request.getRelationType(),
                        request.getToEntityType(),
                        request.getToIdField()
                ));
            }
        }

        // 엔티티 생성
        RelationRuleEntity entity = RelationRuleEntity.of(
                request.getDataSourceId(),
                request.getFromEntityType(),
                request.getFromIdField(),
                request.getRelationType(),
                request.getToEntityType(),
                request.getToIdField(),
                request.getDescription()
        );

        // 저장
        RelationRuleEntity saved = relationRuleRepository.save(entity);

        log.info("도메인 관계 규칙 생성 완료 - ruleId: {}", saved.getRuleId());

        return RelationRuleDto.Response.from(saved);
    }

    /**
     * 도메인 관계 규칙 수정
     *
     * @param ruleId 규칙 ID
     * @param request 수정 요청 DTO
     * @return 수정된 규칙 정보
     */
    @Transactional
    public RelationRuleDto.Response updateRelationRule(Long ruleId, RelationRuleDto.UpdateRequest request) {
        log.info("도메인 관계 규칙 수정 - ruleId: {}", ruleId);

        // 기존 규칙 조회
        RelationRuleEntity entity = relationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("규칙을 찾을 수 없습니다. ID: " + ruleId));

        // 수정은 삭제 후 재생성으로 처리 (UNIQUE 제약조건 때문)
        relationRuleRepository.delete(entity);
        relationRuleRepository.flush();

        // 새 엔티티 생성
        RelationRuleEntity newEntity = RelationRuleEntity.of(
                request.getDataSourceId(),
                request.getFromEntityType(),
                request.getFromIdField(),
                request.getRelationType(),
                request.getToEntityType(),
                request.getToIdField(),
                request.getDescription()
        );

        // 활성화 상태 반영
        if (request.getIsActive() != null && !request.getIsActive()) {
            newEntity.deactivate();
        }

        // 저장
        RelationRuleEntity saved = relationRuleRepository.save(newEntity);

        log.info("도메인 관계 규칙 수정 완료 - ruleId: {}", saved.getRuleId());

        return RelationRuleDto.Response.from(saved);
    }

    /**
     * 도메인 관계 규칙 삭제
     *
     * @param ruleId 규칙 ID
     */
    @Transactional
    public void deleteRelationRule(Long ruleId) {
        log.info("도메인 관계 규칙 삭제 - ruleId: {}", ruleId);

        RelationRuleEntity entity = relationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("규칙을 찾을 수 없습니다. ID: " + ruleId));

        relationRuleRepository.delete(entity);

        log.info("도메인 관계 규칙 삭제 완료 - ruleId: {}", ruleId);
    }

    /**
     * 도메인 관계 규칙 활성화
     *
     * @param ruleId 규칙 ID
     * @return 수정된 규칙 정보
     */
    @Transactional
    public RelationRuleDto.Response activateRelationRule(Long ruleId) {
        log.info("도메인 관계 규칙 활성화 - ruleId: {}", ruleId);

        RelationRuleEntity entity = relationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("규칙을 찾을 수 없습니다. ID: " + ruleId));

        entity.activate();

        log.info("도메인 관계 규칙 활성화 완료 - ruleId: {}", ruleId);

        return RelationRuleDto.Response.from(entity);
    }

    /**
     * 도메인 관계 규칙 비활성화
     *
     * @param ruleId 규칙 ID
     * @return 수정된 규칙 정보
     */
    @Transactional
    public RelationRuleDto.Response deactivateRelationRule(Long ruleId) {
        log.info("도메인 관계 규칙 비활성화 - ruleId: {}", ruleId);

        RelationRuleEntity entity = relationRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("규칙을 찾을 수 없습니다. ID: " + ruleId));

        entity.deactivate();

        log.info("도메인 관계 규칙 비활성화 완료 - ruleId: {}", ruleId);

        return RelationRuleDto.Response.from(entity);
    }
}
