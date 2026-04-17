import api from "@/lib/api";

// 응답 리스트 타입
interface ResponseList<T> {
  data: T[];
  total?: number;
}

// 센서 카테고리
export type SensorCategory =
  | "SECURITY"
  | "COMPLIANCE"
  | "MONITORING"
  | "BACKUP"
  | "NETWORK"
  | "CUSTOM";

// 센서 도메인
export type SensorDomain =
  | "FINANCIAL_TRANSACTION"
  | "LOGIN"
  | "DEVICE_SECURITY"
  | "ACCOUNT"
  | "CUSTOMER"
  | "ATM";

// 센서 연산자
export type SensorOperator =
  | "EQUALS"
  | "NOT_EQUALS"
  | "GREATER_THAN"
  | "GREATER_THAN_OR_EQUALS"
  | "LESS_THAN"
  | "LESS_THAN_OR_EQUALS"
  | "CONTAINS"
  | "NOT_CONTAINS"
  | "STARTS_WITH"
  | "ENDS_WITH"
  | "IN"
  | "NOT_IN"
  | "IS_NULL"
  | "IS_NOT_NULL";

// 센서 조건
export interface SensorCondition {
  fieldName: string;
  operator: string;
  operatorSymbol: string;
  operatorLabel: string;
  value?: any;
}

// 센서 (백엔드 SensorDto.Response 객체)
export interface Sensor {
  sensorId: string; // 센서 ID
  sensorName: string; // 센서 이름
  category: SensorCategory; // 카테고리
  categoryLabel: string; // 카테고리 설명
  domain?: SensorDomain; // 도메인
  domainLabel?: string; // 도메인 라벨
  operator?: SensorOperator; // 연산자
  operatorLabel?: string; // 연산자 라벨
  condition?: SensorCondition; // 조건
  description?: string; // 설명
  version?: number; // 버전
  isActive: boolean; // 활성화 여부
  whereJson?: string; // 조건 JSON
}

// 센서 생성 요청
export interface SensorCreateRequest {
  sensorId: string; // 사용자 정의 센서 ID (예: S_LOGIN_FAIL)
  sensorName: string;
  category?: SensorCategory;
  domain?: SensorDomain;
  operator?: SensorOperator;
  fieldName?: string;
  value?: any;
  description?: string;
  anchor?: string; // JSON string
  whereJson?: string; // JSON string
}

// 센서 수정 요청
export interface SensorUpdateRequest {
  sensorName?: string;
  category?: SensorCategory;
  domain?: SensorDomain;
  operator?: SensorOperator;
  fieldName?: string;
  value?: any;
  description?: string;
  isActive?: boolean;
  anchor?: string;
  whereJson?: string;
}

// 센서 목록 조회
export const fetchSensors = async (params?: {
  q?: string;
  active?: boolean;
  sort?: string;
  dir?: string;
  page?: number;
  size?: number;
}): Promise<ResponseList<Sensor>> => {
  const response = await api.get<ResponseList<Sensor>>("/api/v1/sensors", {
    params,
  });
  return response.data;
};

// 단일 센서 조회
export const fetchSensorById = async (sensorId: string): Promise<Sensor> => {
  const response = await api.get<Sensor>(`/api/v1/sensors/${sensorId}`);
  return response.data;
};

// 활성화된 센서 조회
export const fetchActiveSensors = async (): Promise<ResponseList<Sensor>> => {
  const response = await api.get<ResponseList<Sensor>>("/api/v1/sensors/active");
  return response.data;
};

// 센서 생성
export const createSensor = async (data: SensorCreateRequest): Promise<Sensor> => {
  const response = await api.post<Sensor>("/api/v1/sensors", data);
  return response.data;
};

// 센서 수정
export const updateSensor = async (
  sensorId: string,
  data: SensorUpdateRequest
): Promise<Sensor> => {
  const response = await api.put<Sensor>(`/api/v1/sensors/${sensorId}`, data);
  return response.data;
};

// 센서 삭제 (만약 API가 있다면)
export const deleteSensor = async (sensorId: string): Promise<void> => {
  await api.delete(`/api/v1/sensors/${sensorId}`);
};
