package com.itmasters.icon.api.parser.application.service;

import com.itmasters.icon.api.parser.adapter.in.web.ParserDto;
import com.itmasters.icon.api.parser.adapter.out.persistence.entity.ParserEntity;
import com.itmasters.icon.api.parser.adapter.out.persistence.entity.ParserRuleEntity;
import com.itmasters.icon.api.parser.adapter.out.persistence.repository.ParserJpaRepository;
import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

// [2026-04-20] 파서 CRUD 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ParserService {

    private final ParserJpaRepository parserRepository;
    private final IdGenerator idGenerator;

    public List<ParserDto.SummaryResponse> getAllParsers() {
        return parserRepository.findAll().stream()
                .map(this::toSummary)
                .toList();
    }

    public List<ParserDto.SummaryResponse> getActiveParsers() {
        return parserRepository.findByIsActiveTrueOrderByParserNameAsc().stream()
                .map(this::toSummary)
                .toList();
    }

    public ParserDto.Response getParser(String parserId) {
        ParserEntity entity = parserRepository.findById(parserId)
                .orElseThrow(() -> new IllegalArgumentException("파서를 찾을 수 없습니다: " + parserId));
        return toResponse(entity);
    }

    @Transactional
    public ParserDto.Response createParser(ParserDto.CreateRequest request) {
        ParserEntity entity = ParserEntity.builder()
                .parserId(idGenerator.generateId(EntityType.PARSER))
                .parserName(request.getParserName())
                .parserType(request.getParserType())
                .description(request.getDescription())
                .isActive(true)
                .build();

        if (request.getRules() != null) {
            List<ParserRuleEntity> rules = buildRules(request.getRules(), entity);
            entity.setRules(rules);
        }

        ParserEntity saved = parserRepository.save(entity);
        log.info("파서 생성 완료 - parserId: {}, name: {}", saved.getParserId(), saved.getParserName());
        return toResponse(saved);
    }

    @Transactional
    public ParserDto.Response updateParser(String parserId, ParserDto.UpdateRequest request) {
        ParserEntity entity = parserRepository.findById(parserId)
                .orElseThrow(() -> new IllegalArgumentException("파서를 찾을 수 없습니다: " + parserId));

        if (request.getParserName() != null) entity.setParserName(request.getParserName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getIsActive() != null) entity.setActive(request.getIsActive());

        // 규칙 전체 교체
        if (request.getRules() != null) {
            entity.getRules().clear();
            List<ParserRuleEntity> newRules = buildRules(request.getRules(), entity);
            entity.getRules().addAll(newRules);
        }

        ParserEntity saved = parserRepository.save(entity);
        log.info("파서 수정 완료 - parserId: {}", saved.getParserId());
        return toResponse(saved);
    }

    @Transactional
    public void deleteParser(String parserId) {
        if (!parserRepository.existsById(parserId)) {
            throw new IllegalArgumentException("파서를 찾을 수 없습니다: " + parserId);
        }
        parserRepository.deleteById(parserId);
        log.info("파서 삭제 완료 - parserId: {}", parserId);
    }

    private List<ParserRuleEntity> buildRules(List<ParserDto.RuleItem> items, ParserEntity parser) {
        List<ParserRuleEntity> rules = new ArrayList<>();
        for (ParserDto.RuleItem item : items) {
            rules.add(ParserRuleEntity.builder()
                    .parserRuleId(idGenerator.generateId(EntityType.PARSER_RULE))
                    .parser(parser)
                    .ruleOrder(item.getRuleOrder())
                    .configJson(item.getConfigJson())
                    .targetStandardFieldId(item.getTargetStandardFieldId())
                    .targetFieldName(item.getTargetFieldName())
                    .build());
        }
        return rules;
    }

    private ParserDto.SummaryResponse toSummary(ParserEntity e) {
        return ParserDto.SummaryResponse.builder()
                .parserId(e.getParserId())
                .parserName(e.getParserName())
                .parserType(e.getParserType())
                .description(e.getDescription())
                .isActive(e.isActive())
                .ruleCount(e.getRules() != null ? e.getRules().size() : 0)
                .build();
    }

    private ParserDto.Response toResponse(ParserEntity e) {
        List<ParserDto.RuleResponse> ruleResponses = e.getRules() == null ? List.of() :
                e.getRules().stream().map(r -> ParserDto.RuleResponse.builder()
                        .parserRuleId(r.getParserRuleId())
                        .ruleOrder(r.getRuleOrder())
                        .configJson(r.getConfigJson())
                        .targetStandardFieldId(r.getTargetStandardFieldId())
                        .targetFieldName(r.getTargetFieldName())
                        .build()).toList();

        return ParserDto.Response.builder()
                .parserId(e.getParserId())
                .parserName(e.getParserName())
                .parserType(e.getParserType())
                .description(e.getDescription())
                .isActive(e.isActive())
                .rules(ruleResponses)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
