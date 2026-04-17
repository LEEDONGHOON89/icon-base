package com.itmasters.icon.api.analytics.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventStreamDto {
    private String groupKey;
    private LocalDateTime eventDt;
    private Map<String, Object> eventData;
}

