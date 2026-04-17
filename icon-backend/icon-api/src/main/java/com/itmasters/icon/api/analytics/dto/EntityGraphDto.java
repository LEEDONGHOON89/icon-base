package com.itmasters.icon.api.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Entity Graph DTO (React 그래프 라이브러리용)

 * nodes + edges 형식으로 반환
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class EntityGraphDto {
    private List<GraphNode> nodes;
    private List<GraphEdge> edges;

    /**
     * 그래프 노드 (엔티티)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphNode {
        private String id;           // "ACCOUNT:ACC001"
        private String label;        // "ACC001"
        private String type;         // "ACCOUNT"
        private Object data;         // 추가 데이터 (attributes 등)
        private Integer totalConnections;      // 전체 연결 개수
        private Integer displayedConnections;  // 표시된 연결 개수
    }

    /**
     * 그래프 엣지 (관계)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GraphEdge {
        private String id;           // "ACCOUNT:ACC001->TRANSFERS_TO->ACCOUNT:ACC002"
        private String source;       // "ACCOUNT:ACC001"
        private String target;       // "ACCOUNT:ACC002"
        private String label;        // "TRANSFERS_TO"
        private String type;         // "TRANSFERS_TO"
        private Object data;         // 추가 데이터 (properties 등)
    }
}
