/**
 * 트랜잭션 추적 타입 정의
 */

export interface MappedStorageInfo {
  mappedDataStorageId: number;
  landingRecordId: number;
  rowIndex: number;
  rowData: Record<string, any>;
  regDt: string;
}

export interface EventStreamInfo {
  eventStreamId: number;
  groupKey: string;
  eventData: Record<string, any>;
  mappedDataStorageId: number;
  eventDt: string;
}

export interface DetectRuleInfo {
  detectRuleId: number;
  ruleId: string;
  ruleName: string;
  groupKey: string;
  matchedFields: Record<string, any>;
  detectedAt: string;
}

// NOTE: DetectAggregateInfo는 detect_rules 테이블과 동일한 테이블을 참조하므로 제거됨
// 집계 탐지는 센서 탐지(DetectRuleInfo)로 통합됨

export interface DetectScenarioInfo {
  detectScenarioId: number;
  scenarioId: string;
  groupKey: string;
  detectedAt: string;
}

/**
 * 트랜잭션 추적 전체 정보
 */
export interface TransactionTrackingInfo {
  transactionId: string;
  dataSourceId: string;
  dataSourceName: string;
  firstSeenAt: string;
  lastSeenAt: string;

  mappedStorages: MappedStorageInfo[];
  eventStreams: EventStreamInfo[];
  detectRules: DetectRuleInfo[];
  detectAggregates?: any[]; // 백엔드 호환성을 위해 유지 (사용 안 함)
  detectScenarios: DetectScenarioInfo[];
}

/**
 * 파이프라인 단계
 */
export type PipelineStage =
  | 'PREP-1'   // 데이터 수집/변환
  | 'DET-1'    // 이벤트 스트림
  | 'DET-2-1'  // 센서 탐지 (룰/집계 통합)
  | 'DET-2-2'; // 시나리오 탐지

/**
 * 파이프라인 단계 정보
 */
export interface PipelineStageInfo {
  stage: PipelineStage;
  label: string;
  description: string;
  count: number;
  hasData: boolean;
}

/**
 * 최근 트랜잭션 목록 항목
 */
export interface TransactionListItem {
  transactionId: string;
  dataSourceId: string;
  dataSourceName: string;
  firstSeenAt: string;
  lastSeenAt: string;
  totalRecords: number;
  scenariosDetected: number;
}
