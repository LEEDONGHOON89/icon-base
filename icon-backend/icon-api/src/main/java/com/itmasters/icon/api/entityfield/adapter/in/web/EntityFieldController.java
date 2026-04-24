package com.itmasters.icon.api.entityfield.adapter.in.web;

import com.itmasters.icon.api.entityfield.adapter.in.web.dto.EntityFieldDto;
import com.itmasters.icon.api.entityfield.application.service.EntityFieldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    // [2026-04-24] CRUD API 추가 — 엔티티 필드 등록
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EntityFieldDto.Response createEntityField(@Valid @RequestBody EntityFieldDto.CreateRequest request) {
        log.info("POST /api/v1/entity-fields - Create entity field: {}", request.getEntityFieldId());
        return entityFieldService.createEntityField(request);
    }

    // [2026-04-24] CRUD API 추가 — 엔티티 필드 수정
    @PutMapping("/{entityFieldId}")
    public EntityFieldDto.Response updateEntityField(
            @PathVariable String entityFieldId,
            @Valid @RequestBody EntityFieldDto.UpdateRequest request) {
        log.info("PUT /api/v1/entity-fields/{} - Update entity field", entityFieldId);
        return entityFieldService.updateEntityField(entityFieldId, request);
    }

    // [2026-04-24] CRUD API 추가 — 엔티티 필드 삭제
    @DeleteMapping("/{entityFieldId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEntityField(@PathVariable String entityFieldId) {
        log.info("DELETE /api/v1/entity-fields/{} - Delete entity field", entityFieldId);
        entityFieldService.deleteEntityField(entityFieldId);
    }
}
