package com.itmasters.icon.api.eventhandling.domain;

import com.itmasters.icon.entity.Auditable;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
// 이상 이벤트 처리 이력 도메인
public class EventHandling extends Auditable {
  private String handlingId; // 처리 이력 고유 ID
  private String eventId; // 처리 대상 이벤트 ID
  private String userId; // 처리 담당자 ID
  private String action; // 처리 유형(확인, 예외, 무시 등)
  private String comment; // 처리 사유/메모
  private LocalDateTime handledAt; // 처리 일시
}
