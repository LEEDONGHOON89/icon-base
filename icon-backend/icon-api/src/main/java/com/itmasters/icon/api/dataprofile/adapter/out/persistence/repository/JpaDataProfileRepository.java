package com.itmasters.icon.api.dataprofile.adapter.out.persistence.repository;

import com.itmasters.icon.api.dataprofile.adapter.out.persistence.entity.ProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 데이터 프로파일 JPA Repository
 * QueryDSL 커스텀 메서드는 DataProfileQueryDslRepository 인터페이스에서 구현
 */
public interface JpaDataProfileRepository extends JpaRepository<ProfileEntity, String> {
}