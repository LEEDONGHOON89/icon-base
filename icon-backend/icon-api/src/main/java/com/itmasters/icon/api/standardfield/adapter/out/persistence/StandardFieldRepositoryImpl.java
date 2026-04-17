package com.itmasters.icon.api.standardfield.adapter.out.persistence;

import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import com.itmasters.icon.api.standardfield.adapter.out.persistence.entity.StandardFieldEntity;
import com.itmasters.icon.api.standardfield.adapter.out.persistence.mapper.StandardFieldMapper;
import com.itmasters.icon.api.standardfield.adapter.out.persistence.repository.StandardFieldJpaRepository;
import com.itmasters.icon.api.standardfield.application.port.out.StandardFieldRepository;
import com.itmasters.icon.api.standardfield.domain.StandardField;
import com.itmasters.icon.common.domain.rule.FieldCategory;
import com.itmasters.icon.common.domain.type.FieldDataType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.itmasters.icon.api.standardfield.adapter.out.persistence.entity.QStandardFieldEntity.standardFieldEntity;

/**
 * 표준 필드 Repository 구현체
 */
@Repository
@RequiredArgsConstructor
public class StandardFieldRepositoryImpl implements StandardFieldRepository {

    private final StandardFieldJpaRepository jpaRepository;
    private final StandardFieldMapper mapper;
    private final IdGenerator idGenerator;
    private final JPAQueryFactory queryFactory;

    @Override
    public StandardField save(StandardField standardField) {
        // 새로운 엔티티인 경우 ID 할당
        if (standardField.getFieldId() == null) {
            standardField.assignId(idGenerator.generateId(EntityType.STANDARD_FIELD));
        }
        
        StandardFieldEntity entity = mapper.toEntity(standardField);
        StandardFieldEntity savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<StandardField> findById(String fieldId) {
        return jpaRepository.findById(fieldId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<StandardField> findByFieldName(String fieldName) {
        StandardFieldEntity result = queryFactory
                .selectFrom(standardFieldEntity)
                .where(standardFieldEntity.standardFieldId.eq(fieldName))
                .fetchOne();
        
        return Optional.ofNullable(result)
                .map(mapper::toDomain);
    }

    @Override
    public List<StandardField> findByIsActiveTrue() {
        List<StandardFieldEntity> results = queryFactory
                .selectFrom(standardFieldEntity)
                .where(standardFieldEntity.isActive.isTrue())
                .orderBy(standardFieldEntity.category.asc(), standardFieldEntity.standardFieldId.asc())
                .fetch();
        
        return results.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public List<StandardField> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(String fieldId) {
        jpaRepository.deleteById(fieldId);
    }

    @Override
    public boolean existsById(String fieldId) {
        return jpaRepository.existsById(fieldId);
    }

    @Override
    public boolean existsByFieldName(String fieldName) {
        Integer count = queryFactory
                .selectOne()
                .from(standardFieldEntity)
                .where(standardFieldEntity.standardFieldId.eq(fieldName))
                .fetchFirst();
        
        return count != null;
    }
    
    @Override
    public List<StandardField> findByCategory(FieldCategory category) {
        List<StandardFieldEntity> results = queryFactory
                .selectFrom(standardFieldEntity)
                .where(standardFieldEntity.category.eq(category)
                        .and(standardFieldEntity.isActive.isTrue()))
                .orderBy(standardFieldEntity.standardFieldId.asc())
                .fetch();
        
        return results.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<StandardField> findByDataType(FieldDataType dataType) {
        List<StandardFieldEntity> results = queryFactory
                .selectFrom(standardFieldEntity)
                .where(standardFieldEntity.dataType.eq(dataType)
                        .and(standardFieldEntity.isActive.isTrue()))
                .orderBy(standardFieldEntity.category.asc(), standardFieldEntity.standardFieldId.asc())
                .fetch();
        
        return results.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<StandardField> searchByKeyword(String keyword) {
        String searchKeyword = "%" + keyword.toLowerCase() + "%";
        
        List<StandardFieldEntity> results = queryFactory
                .selectFrom(standardFieldEntity)
                .where(standardFieldEntity.standardFieldId.lower().like(searchKeyword)
                        .or(standardFieldEntity.displayName.lower().like(searchKeyword))
                        .or(standardFieldEntity.description.lower().like(searchKeyword)))
                .where(standardFieldEntity.isActive.isTrue())
                .orderBy(standardFieldEntity.category.asc(), standardFieldEntity.standardFieldId.asc())
                .fetch();
        
        return results.stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }
}
