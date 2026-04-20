package com.itmasters.icon.api.parser.application.service;

import com.itmasters.icon.api.parser.adapter.in.web.ParserDto;
import com.itmasters.icon.api.parser.adapter.out.persistence.entity.DataSourceParserEntity;
import com.itmasters.icon.api.parser.adapter.out.persistence.entity.ParserEntity;
import com.itmasters.icon.api.parser.adapter.out.persistence.repository.DataSourceParserJpaRepository;
import com.itmasters.icon.api.parser.adapter.out.persistence.repository.ParserJpaRepository;
import com.itmasters.icon.common.domain.EntityType;
import com.itmasters.icon.common.domain.IdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// [2026-04-20] 데이터소스-파서 연결 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class DataSourceParserService {

    private final DataSourceParserJpaRepository dataSourceParserRepository;
    private final ParserJpaRepository parserRepository;
    private final ParserService parserService;
    private final IdGenerator idGenerator;

    /** 데이터소스에 연결된 파서 목록 조회 */
    public List<ParserDto.LinkResponse> getLinkedParsers(String dataSourceId) {
        return dataSourceParserRepository
                .findByDataSourceIdOrderByParserOrderAsc(dataSourceId)
                .stream()
                .map(this::toLinkResponse)
                .toList();
    }

    /** 데이터소스에 파서 연결 */
    @Transactional
    public ParserDto.LinkResponse linkParser(String dataSourceId, ParserDto.LinkRequest request) {
        if (dataSourceParserRepository.existsByDataSourceIdAndParser_ParserId(
                dataSourceId, request.getParserId())) {
            throw new IllegalArgumentException("이미 연결된 파서입니다: " + request.getParserId());
        }

        ParserEntity parser = parserRepository.findById(request.getParserId())
                .orElseThrow(() -> new IllegalArgumentException("파서를 찾을 수 없습니다: " + request.getParserId()));

        DataSourceParserEntity entity = DataSourceParserEntity.builder()
                .dataSourceParserId(idGenerator.generateId(EntityType.DATA_SOURCE_PARSER))
                .dataSourceId(dataSourceId)
                .parser(parser)
                .parserOrder(request.getParserOrder())
                .isActive(true)
                .build();

        DataSourceParserEntity saved = dataSourceParserRepository.save(entity);
        log.info("데이터소스-파서 연결 완료 - dataSourceId: {}, parserId: {}", dataSourceId, request.getParserId());
        return toLinkResponse(saved);
    }

    /** 데이터소스에서 파서 연결 해제 */
    @Transactional
    public void unlinkParser(String dataSourceId, String parserId) {
        if (!dataSourceParserRepository.existsByDataSourceIdAndParser_ParserId(dataSourceId, parserId)) {
            throw new IllegalArgumentException("연결된 파서를 찾을 수 없습니다: " + parserId);
        }
        dataSourceParserRepository.deleteByDataSourceIdAndParser_ParserId(dataSourceId, parserId);
        log.info("데이터소스-파서 연결 해제 - dataSourceId: {}, parserId: {}", dataSourceId, parserId);
    }

    // ─── 내부 헬퍼 ──────────────────────────────────────────────────────────

    private ParserDto.LinkResponse toLinkResponse(DataSourceParserEntity e) {
        return ParserDto.LinkResponse.builder()
                .dataSourceParserId(e.getDataSourceParserId())
                .dataSourceId(e.getDataSourceId())
                .parser(parserService.toSummary(e.getParser()))
                .parserOrder(e.getParserOrder())
                .isActive(e.isActive())
                .build();
    }
}
