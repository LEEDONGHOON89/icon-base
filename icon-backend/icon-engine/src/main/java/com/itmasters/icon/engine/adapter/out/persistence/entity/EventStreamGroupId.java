package com.itmasters.icon.engine.adapter.out.persistence.entity;

import lombok.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * EventStreamGroupEntity의 복합 키 클래스
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventStreamGroupId implements Serializable {

    private Long eventStreamId;
    private String ruleId;
}
