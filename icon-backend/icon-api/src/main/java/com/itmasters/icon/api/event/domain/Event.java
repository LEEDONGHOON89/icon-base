package com.itmasters.icon.api.event.domain;

import com.itmasters.icon.entity.Auditable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
// 이상거래 이벤트 도메인
public class Event extends Auditable {
  private String eventId; // 이벤트 고유 ID - TSID
  private String ruleId; // 탐지된 규칙 ID - TSID
  private String companyId; // 소속 회사 ID - TSID
  private String eventData; // 이벤트 상세(원본 데이터, JSON 등)
  private LocalDateTime detectedAt; // 탐지 일시
}
