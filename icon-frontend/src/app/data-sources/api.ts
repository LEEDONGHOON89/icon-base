import api from "@/lib/api";

// 응답 리스트 타입
interface ResponseList<T> {
  data: T[];
  total?: number;
}

// 데이터 소스 타입
export type DataSourceType =
  | "DATABASE"
  | "FILE_SYSTEM"
  | "FILE_SYSTEM_REALTIME"
  | "LOG_SERVER"
  | "API"
  | "MESSAGE_QUEUE"
  | "CLOUD_STORAGE"
  | "FTP"
  | "SYSLOG"
  | "ELASTIC_SEARCH"
  | "SPLUNK";


// 데이터 소스 인터페이스
export interface DataSource {
  dataSourceId: string;
  name: string;
  description?: string;
  sourceType: DataSourceType;
  isActive: boolean;
  mappedRuleCount: number;
}

// 데이터 소스 연결 설정 DTO
export interface FileSystemConfig {
  dsFileSystemConfigId?: string;
  dataSourceId?: string;
  connectionName: string;
  watchDirectory: string;
  filePattern?: string;
  fileEncoding?: string;
  delimiter?: string;
  quoteChar?: string;
  escapeChar?: string;
  hasHeader?: boolean;
  skipLines?: number;
  processingStrategy?: string;
  scanIntervalMinutes?: number;
  moveProcessedFiles?: boolean;
  processedFilesDirectory?: string;
  isActive?: boolean;
  connectionStatus?: string;
  lastErrorMessage?: string;
  // [2026-03-12] FILE_SYSTEM_REALTIME → 에이전트 연결 정보
  agentId?: string;
}

export interface DatabaseConfig {
  dsDatabaseConfigId?: string;
  dataSourceId?: string;
  connectionName: string;
  databaseType?: string;
  host?: string;
  port?: number;
  databaseName?: string;
  schemaName?: string;
  username?: string;
  password?: string; // write-only
  minPoolSize?: number;
  maxPoolSize?: number;
  connectionTimeoutSeconds?: number;
  idleTimeoutSeconds?: number;
  mainQuery?: string;
  incrementalColumn?: string;
  incrementalColumnType?: string;
  batchSize?: number;
  isActive?: boolean;
  connectionStatus?: string;
  lastErrorMessage?: string;
  // [2026-03-13] DATABASE → 에이전트 연결 정보 (에이전트가 JDBC 폴링 후 Push하는 경우)
  agentId?: string;
}

export interface DataSourceConfigResponse {
  type: DataSourceType | string;
  fileSystem?: FileSystemConfig | null;
  database?: DatabaseConfig | null;
}

export const fetchDataSourceConfig = async (dataSourceId: string): Promise<DataSourceConfigResponse> => {
  const response = await api.get<DataSourceConfigResponse>(`/api/v1/data-sources/${dataSourceId}/config`);
  return response.data;
};

export const updateDataSourceConfig = async (dataSourceId: string, payload: Partial<DataSourceConfigResponse>): Promise<DataSourceConfigResponse> => {
  const response = await api.put<DataSourceConfigResponse>(`/api/v1/data-sources/${dataSourceId}/config`, payload);
  return response.data;
};

// 데이터 소스 생성 요청
export interface DataSourceCreateRequest {
  name: string;
  description?: string;
  sourceType: DataSourceType;
}

// 데이터 소스 수정 요청
export interface DataSourceUpdateRequest {
  name?: string;
  description?: string;
  isActive?: boolean;
}

// 전체 데이터 소스 목록 조회
export const fetchDataSources = async (): Promise<DataSource[]> => {
  const response = await api.get<ResponseList<DataSource>>("/api/v1/data-sources");
  return response.data.data;
};

// 데이터 소스 단건 조회
export const fetchDataSource = async (id: string): Promise<DataSource> => {
  const response = await api.get<DataSource>(`/api/v1/data-sources/${id}`);
  return response.data;
};

// 데이터 소스 단건 조회 (별칭)
export const fetchDataSourceById = fetchDataSource;

// 데이터 소스 생성
export const createDataSource = async (
  data: DataSourceCreateRequest
): Promise<DataSource> => {
  const response = await api.post("/api/v1/data-sources", data);
  return response.data;
};

// 데이터 소스 수정
export const updateDataSource = async (
  id: string,
  data: DataSourceUpdateRequest
): Promise<DataSource> => {
  const response = await api.put(`/api/v1/data-sources/${id}`, data);
  return response.data;
};

// 데이터 소스 삭제
export const deleteDataSource = async (id: string): Promise<void> => {
  await api.delete(`/api/v1/data-sources/${id}`);
};

// 데이터 소스 활성화
export const activateDataSource = async (id: string): Promise<DataSource> => {
  const response = await api.put(`/api/v1/data-sources/${id}/activate`);
  return response.data;
};

// 데이터 소스 비활성화
export const deactivateDataSource = async (id: string): Promise<DataSource> => {
  const response = await api.put(`/api/v1/data-sources/${id}/deactivate`);
  return response.data;
};

// ======== 필드 데이터 타입 ========

// 탐지키 타입
export type DetectKeyType = "SINGLE" | "COMPOSITE" | "CUSTOM";

// 필드 데이터 타입
export type FieldDataType = 
  | "STRING"
  | "NUMBER"
  | "BOOLEAN"
  | "DATE"
  | "DATETIME"
  | "TIME"
  | "JSON"
  | "ARRAY"
  | "OBJECT";


// ======== 데이터 프로파일 관련 타입 ========

// 프로파일 용도
export type ProfilePurpose = 'DEFAULT' | 'SECURITY' | 'PERFORMANCE' | 'BUSINESS' | 'COMPLIANCE';

// 데이터 프로파일 인터페이스
export interface DataProfile {
  profileId: string;
  dataSourceId: string;
  profileName: string;
  profilePurpose: ProfilePurpose;
  profilePurposeDescription: string;
  description?: string;
  displayOrder: number;
  schemaCount: number;
  ruleCount: number;
  isActive: boolean;
  detectKey?: string;
  detectKeyType?: DetectKeyType;
  // Entity Attributes 필드
  destinationType?: string;
  entityType?: string;
  entityIdField?: string;
  storeFields?: string[];
  createdAt?: string;
  updatedAt?: string;
}

// 프로파일 생성 요청
export interface DataProfileCreateRequest {
  dataSourceId: string;
  profileName: string;
  profilePurpose: ProfilePurpose;
  description?: string;
  displayOrder?: number;
  detectKey?: string;
  detectKeyType?: string;
}

// 프로파일 수정 요청
export interface DataProfileUpdateRequest {
  profileName: string;
  profilePurpose: ProfilePurpose;
  description?: string;
  displayOrder?: number;
  detectKey?: string;
  detectKeyType?: string;
  // Entity Attributes 필드
  destinationType?: string;
  entityType?: string;
  entityIdField?: string;
  storeFields?: string[];
}

// ======== 데이터 프로파일 API 함수들 ========

// 프로파일 생성
export const createDataProfile = async (
  data: DataProfileCreateRequest
): Promise<DataProfile> => {
  const response = await api.post<DataProfile>("/api/v1/data-profiles", data);
  return response.data;
};

// 프로파일 수정
export const updateDataProfile = async (
  profileId: string,
  data: DataProfileUpdateRequest
): Promise<DataProfile> => {
  const response = await api.put<DataProfile>(`/api/v1/data-profiles/${profileId}`, data);
  return response.data;
};

// 프로파일 조회
export const fetchDataProfile = async (profileId: string): Promise<DataProfile> => {
  const response = await api.get<DataProfile>(`/api/v1/data-profiles/${profileId}`);
  return response.data;
};

// 데이터소스별 프로파일 목록 조회
export const fetchDataProfiles = async (dataSourceId: string): Promise<DataProfile[]> => {
  const response = await api.get<ResponseList<DataProfile>>(`/api/v1/data-sources/${dataSourceId}/profiles`);
  return response.data.data;
};

// 프로파일 활성화/비활성화
export const toggleDataProfile = async (profileId: string): Promise<DataProfile> => {
  const response = await api.patch<DataProfile>(`/api/v1/data-profiles/${profileId}/toggle`);
  return response.data;
};

// 프로파일 삭제
export const deleteDataProfile = async (profileId: string): Promise<void> => {
  await api.delete(`/api/v1/data-profiles/${profileId}`);
};



// ===== 원본 스키마 관련 API =====

// 원본 스키마 인터페이스
export interface DataSourceOriginalSchema {
  schemaId: string;
  dataSourceId: string;
  fieldName: string;
  description?: string;
  isRequired: boolean;
  dataType: FieldDataType;
  isActive: boolean;
  fieldOrder?: number;        // 필드 순서
  standardFieldId?: string;   // 표준 필드 매핑
  standardFieldName?: string; // 표준 필드명 (조회시)
  transformRule?: string;     // 변환 규칙
  createdAt?: string;
  updatedAt?: string;
}

// 데이터소스의 원본 스키마 목록 조회
export const fetchDataSourceOriginalSchemas = async (
  dataSourceId: string
): Promise<DataSourceOriginalSchema[]> => {
  const response = await api.get<ResponseList<DataSourceOriginalSchema>>(
    `/api/v1/data-sources/${dataSourceId}/original-schemas`
  );
  return response.data.data || [];
};



// 개별 원본 스키마 표준 필드 매핑 업데이트
export interface StandardFieldMappingRequest {
  standardFieldId?: string;
  transformRule?: string;
  isActive?: boolean;
}

export const updateOriginalSchemaStandardFieldMapping = async (
  schemaId: string,
  request: StandardFieldMappingRequest
): Promise<DataSourceOriginalSchema> => {
  const response = await api.put<DataSourceOriginalSchema>(
    `/api/v1/original-schemas/${schemaId}/mapping`,
    request
  );
  return response.data;
};

// 원본 스키마 일괄 수정
export const updateDataSourceOriginalSchemasBulk = async (
  dataSourceId: string,
  schemas: Array<{
    schemaId: string;
    fieldLabel?: string;
    dataType?: string;
    isRequired?: boolean;
    description?: string;
    fieldOrder?: number;
    isActive?: boolean;
  }>
): Promise<DataSourceOriginalSchema[]> => {
  const response = await api.put<{ data: DataSourceOriginalSchema[] }>(
    `/api/v1/data-sources/${dataSourceId}/original-schemas/bulk`,
    {
      schemas: schemas
    }
  );
  return response.data.data || [];
};

// ===== 프로파일 스키마 관련 API =====

// 프로파일 스키마 인터페이스
export interface ProfileSchema {
  schemaId: string;
  profileId: string;
  originalSchemaId: string;
  originalFieldName?: string;
  fieldAlias?: string;
  fieldDescription?: string;
  dataType: FieldDataType;
  validationData?: any;
  fieldOrder: number;
  isActive: boolean;
  // FieldMapping 통합 필드
  standardFieldId?: string;
  standardFieldName?: string;
  transformType?: string;
  transformConfig?: any;
  createdAt?: string;
  updatedAt?: string;
}


// ===== 표준 필드 관련 API =====

// 표준 필드 인터페이스
export interface StandardField {
  fieldId: string;
  fieldName: string;
  displayName: string;
  category: string;  // 백엔드와 일치하도록 수정
  description?: string;
  dataType: FieldDataType;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

// 표준 필드 목록 조회
export const fetchStandardFields = async (): Promise<StandardField[]> => {
  const response = await api.get<ResponseList<StandardField>>(
    `/api/v1/standard-fields`
  );
  return response.data.data || [];
};

// ===== 프로파일 스키마 관련 API =====

// 프로파일 스키마 병합 뷰 응답
export interface ProfileSchemaMergedView {
  // 원본 스키마 정보
  originalSchemaId: string;
  originalFieldName: string;
  originalDescription?: string;
  originalDataType: FieldDataType;
  originalIsRequired: boolean;
  // 표준 필드 매핑 정보 (원본 스키마에서 가져옴)
  standardFieldId?: string;
  standardFieldName?: string;
  transformRule?: string;
  // 프로파일 스키마 정보
  profileSchemaId?: string;
  isIncluded: boolean;
  isActive: boolean;
  fieldOrder?: number;
}

// 프로파일 스키마 업데이트 요청
export interface ProfileSchemaUpdateRequest {
  originalSchemaId: string;
  isIncluded: boolean;
  isActive?: boolean;
  fieldOrder?: number;
}

// 프로파일 스키마 병합 뷰 조회
export const fetchProfileSchemasMergedView = async (
  profileId: string
): Promise<ProfileSchemaMergedView[]> => {
  const response = await api.get<ProfileSchemaMergedView[]>(
    `/api/v1/profile-schemas/profiles/${profileId}/merged-view`
  );
  return response.data;
};

// 프로파일 스키마 일괄 업데이트
export const updateProfileSchemas = async (
  profileId: string,
  schemas: ProfileSchemaUpdateRequest[]
): Promise<ProfileSchemaMergedView[]> => {
  const response = await api.put<ProfileSchemaMergedView[]>(
    `/api/v1/profile-schemas/profiles/${profileId}/bulk`,
    { schemas }
  );
  return response.data;
};

// DEFAULT 프로파일 동기화
export const syncDefaultProfileSchemas = async (
  dataSourceId: string
): Promise<void> => {
  await api.post(`/api/v1/profile-schemas/sync-default/${dataSourceId}`);
};
