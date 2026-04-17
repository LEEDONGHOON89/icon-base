package com.itmasters.icon.api.entityfield.adapter.in.web;

import com.itmasters.icon.api.entityfield.adapter.in.web.dto.EntityFieldDto;
import com.itmasters.icon.api.entityfield.application.service.EntityFieldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * EntityField REST API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/entity-fields")
@RequiredArgsConstructor
public class EntityFieldController {

    private final EntityFieldService entityFieldService;

    /**
     * 활성화된 엔티티 필드 목록 조회
     */
    @GetMapping
    public List<EntityFieldDto.Response> getActiveEntityFields() {
        log.info("GET /api/v1/entity-fields - Get active entity fields");
        return entityFieldService.getActiveEntityFields();
    }

    /**
     * 모든 엔티티 필드 목록 조회 (관리용)
     */
    @GetMapping("/all")
    public List<EntityFieldDto.Response> getAllEntityFields() {
        log.info("GET /api/v1/entity-fields/all - Get all entity fields");
        return entityFieldService.getAllEntityFields();
    }
}
