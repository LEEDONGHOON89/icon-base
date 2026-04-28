"use client";

import { DataProfile, StandardField, DetectKeyType } from "@/app/data-sources/api";
import LoadingButton from "@/components/common/LoadingButton";
import { KeyIcon, CircleStackIcon } from "@heroicons/react/24/outline";
import { useState, useEffect } from "react";

interface ProfileDetailPanelProps {
  profile: DataProfile | null;
  standardFields: StandardField[];
  onSave: (
    detectKey?: string,
    detectKeyType?: DetectKeyType,
    destinationType?: string,
    entityType?: string,
    entityIdField?: string,
    storeFields?: string[],
    timestampKey?: string
  ) => void;
}

export default function ProfileDetailPanel({
  profile,
  standardFields,
  onSave,
}: ProfileDetailPanelProps) {
  // 탐지키 관련 상태
  const [detectKeyType, setDetectKeyType] = useState<DetectKeyType | "">(profile?.detectKeyType || "");
  const [detectKey, setDetectKey] = useState<string[]>(profile?.detectKey?.split(",") || []);

  // Entity Attributes 관련 상태
  const [destinationType, setDestinationType] = useState<string>(profile?.destinationType || "EVENT_STREAM");
  const [entityType, setEntityType] = useState<string>(profile?.entityType || "");
  const [entityIdField, setEntityIdField] = useState<string>(profile?.entityIdField || "");
  const [storeFields, setStoreFields] = useState<string[]>(profile?.storeFields || []);
  const [newStoreField, setNewStoreField] = useState<string>("");
  // [2026-04-23] event_stream 타임스탬프 키 상태 추가
  const [timestampKey, setTimestampKey] = useState<string>(profile?.timestampKey || "");

  // 프로파일 변경 시 초기화
  useEffect(() => {
    setDetectKeyType(profile?.detectKeyType || "");
    setDetectKey(profile?.detectKey?.split(",").filter(Boolean) || []);
    setDestinationType(profile?.destinationType || "EVENT_STREAM");
    setEntityType(profile?.entityType || "");
    setEntityIdField(profile?.entityIdField || "");
    setStoreFields(profile?.storeFields || []);
    setNewStoreField("");
    setTimestampKey(profile?.timestampKey || "");
  }, [profile, standardFields]);

  if (!profile) {
    return (
      <div className="flex-1 flex items-center justify-center text-gray-500">
        프로파일을 선택해주세요
      </div>
    );
  }

  const handleSave = () => {
    const finalDetectKey = detectKey.filter(Boolean).join(",");
    const finalDetectKeyType = detectKeyType || undefined;

    // [2026-04-23] EVENT_STREAM 선택 시 timestampKey 필수 검증
    const needsTimestamp = destinationType === "EVENT_STREAM" || destinationType === "BOTH";
    if (needsTimestamp && !timestampKey.trim()) {
      alert("이벤트 스트림 저장 방식 선택 시 타임스탬프 필드를 입력해야 합니다.");
      return;
    }

    onSave(
      finalDetectKey || undefined,
      finalDetectKeyType,
      destinationType,
      entityType || undefined,
      entityIdField || undefined,
      storeFields.length > 0 ? storeFields : undefined,
      timestampKey.trim() || undefined
    );
  };

  const handleAddStoreField = () => {
    if (newStoreField.trim() && !storeFields.includes(newStoreField.trim())) {
      setStoreFields([...storeFields, newStoreField.trim()]);
      setNewStoreField("");
    }
  };

  const handleRemoveStoreField = (field: string) => {
    setStoreFields(storeFields.filter(f => f !== field));
  };

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      {/* 헤더 */}
      <div className="p-6 border-b border-gray-200">
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <h2 className="text-lg font-semibold text-gray-900">{profile.profileName}</h2>
            <p className="text-xs text-gray-500 font-mono mt-1">{profile.profileId}</p>
            {profile.description && (
              <p className="mt-1 text-sm text-gray-600">{profile.description}</p>
            )}
          </div>
        </div>
      </div>

      {/* 스크롤 가능한 폼 영역 */}
      <div className="flex-1 overflow-y-auto">
        <div className="p-6 space-y-8">
          {/* 탐지키 설정 섹션 */}
          <div className="bg-white border border-gray-200 rounded-xl p-6">
            <div className="flex items-center gap-2 mb-4">
              <KeyIcon className="h-5 w-5 text-blue-600" />
              <h3 className="text-base font-semibold text-gray-900">탐지키 설정</h3>
              <span className="text-xs text-red-500">*필수</span>
            </div>

            <div className="space-y-4">
              {/* 탐지키 타입과 필드를 1줄로 배치 */}
              <div className="grid grid-cols-2 gap-4">
                {/* 탐지키 타입 선택 */}
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    탐지키 타입
                  </label>
                  <select
                    value={detectKeyType}
                    onChange={(e) => {
                      setDetectKeyType(e.target.value as DetectKeyType | "");
                      if (e.target.value !== "COMPOSITE") {
                        setDetectKey(detectKey.slice(0, 1));
                      }
                    }}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  >
                    <option value="">선택안함</option>
                    <option value="SINGLE">단일필드</option>
                    <option value="COMPOSITE">복합필드</option>
                    <option value="CUSTOM">커스텀</option>
                  </select>
                </div>

                {/* 탐지키 필드 선택 - SINGLE 타입일 때만 같은 줄에 표시 */}
                {detectKeyType === "SINGLE" && (
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      탐지키 필드
                    </label>
                    <div className="space-y-2">
                      <select
                        value={detectKey[0] || ""}
                        onChange={(e) => setDetectKey(e.target.value ? [e.target.value] : [])}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                      >
                        <option value="">필드 선택</option>
                        {standardFields.map(field => (
                          <option key={field.fieldId} value={field.fieldName || field.fieldId}>
                            {field.displayName} - {field.fieldName || field.fieldId}
                          </option>
                        ))}
                      </select>

                      {/* 선택된 필드 표시 */}
                      {detectKey[0] && (
                        <div className="flex flex-wrap gap-2">
                          {(() => {
                            const fieldInfo = standardFields.find(f => (f.fieldName || f.fieldId) === detectKey[0]);
                            const displayText = fieldInfo
                              ? `${fieldInfo.displayName} - ${fieldInfo.fieldName || fieldInfo.fieldId}`
                              : detectKey[0];
                            return (
                              <span className="inline-flex items-center px-3 py-1 bg-blue-100 text-blue-800 rounded-md text-sm">
                                {displayText}
                              </span>
                            );
                          })()}
                        </div>
                      )}
                    </div>
                  </div>
                )}
              </div>

              {/* 복합필드 선택 - 별도 줄로 표시 */}
              {detectKeyType === "COMPOSITE" && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    탐지키 필드 (복합)
                  </label>
                  <div className="space-y-2">
                    <div className="flex flex-wrap gap-2 mb-2">
                      {detectKey.map((fieldName, idx) => {
                        const fieldInfo = standardFields.find(f => f.fieldName === fieldName);
                        const displayText = fieldInfo
                          ? `${fieldInfo.displayName} - ${fieldName}`
                          : fieldName;
                        return (
                          <span key={idx} className="inline-flex items-center px-3 py-1 bg-blue-100 text-blue-800 rounded-md text-sm">
                            {displayText}
                            <button
                              onClick={() => setDetectKey(detectKey.filter((_, i) => i !== idx))}
                              className="ml-2 text-blue-600 hover:text-blue-800 font-bold"
                            >
                              ×
                            </button>
                          </span>
                        );
                      })}
                    </div>
                    <select
                      value=""
                      onChange={(e) => {
                        if (e.target.value && !detectKey.includes(e.target.value)) {
                          setDetectKey([...detectKey, e.target.value]);
                        }
                      }}
                      className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                    >
                      <option value="">+ 필드 추가</option>
                      {standardFields
                        .filter(field => !detectKey.includes(field.fieldName))
                        .map(field => (
                          <option key={field.fieldId} value={field.fieldName}>
                            {field.displayName} - {field.fieldName}
                          </option>
                        ))}
                    </select>
                  </div>
                </div>
              )}

              {/* 커스텀 탐지키 입력 */}
              {detectKeyType === "CUSTOM" && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    커스텀 키
                  </label>
                  <input
                    type="text"
                    value={detectKey.join(",")}
                    onChange={(e) => setDetectKey(e.target.value ? [e.target.value] : [])}
                    placeholder="커스텀 키 입력"
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                  />
                </div>
              )}
            </div>
          </div>

          {/* Entity Attributes 설정 섹션 */}
          <div className="bg-white border border-gray-200 rounded-xl p-6">
            <div className="flex items-center gap-2 mb-4">
              <CircleStackIcon className="h-5 w-5 text-indigo-600" />
              <h3 className="text-base font-semibold text-gray-900">Entity Attributes 설정</h3>
            </div>

            <div className="space-y-4">
              {/* Destination Type */}
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">
                  저장 방식
                </label>
                {/* [2026-04-24] ENTITY_ATTRIBUTES → BOTH/ENTITY 로 수정 (실제 enum 값 반영) */}
                <select
                  value={destinationType}
                  onChange={(e) => setDestinationType(e.target.value)}
                  className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                >
                  <option value="EVENT_STREAM">이벤트 스트림만 저장</option>
                  <option value="ENTITY">엔티티 속성만 저장</option>
                  <option value="BOTH">이벤트 스트림 + 엔티티 속성 저장</option>
                </select>
                <p className="mt-1 text-xs text-gray-500">
                  {destinationType === "EVENT_STREAM"
                    ? "시계열 이벤트 데이터만 event_stream 테이블에 저장됩니다."
                    : destinationType === "ENTITY"
                    ? "entity_attributes 테이블에만 저장됩니다 (UPSERT)."
                    : "event_stream과 entity_attributes 양쪽에 저장됩니다 (UPSERT)."}
                </p>
              </div>

              {/* 타임스탬프 필드 (EVENT_STREAM / BOTH 선택 시 필수) */}
              {(destinationType === "EVENT_STREAM" || destinationType === "BOTH") && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    타임스탬프 필드 <span className="text-red-500">*필수</span>
                  </label>
                  <select
                    value={timestampKey}
                    onChange={(e) => setTimestampKey(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                  >
                    <option value="">필드 선택</option>
                    {standardFields.map(field => (
                      <option key={field.fieldId} value={field.fieldName || field.fieldId}>
                        {field.displayName} - {field.fieldName || field.fieldId}
                      </option>
                    ))}
                  </select>
                  <p className="mt-1 text-xs text-gray-500">
                    event_stream의 시간축으로 사용할 필드 (예: 거래일시, 발생시각)
                  </p>
                </div>
              )}

              {/* Entity Type (ENTITY 또는 BOTH 선택 시 표시) */}
              {/* [2026-04-24] ENTITY_ATTRIBUTES → ENTITY || BOTH 로 수정 */}
              {(destinationType === "ENTITY" || destinationType === "BOTH") && (
                <>
                  {/* 엔티티 타입과 Entity ID 필드를 1줄로 배치 */}
                  <div className="grid grid-cols-2 gap-4">
                    {/* 엔티티 타입 */}
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        엔티티 타입 <span className="text-red-500">*</span>
                      </label>
                      <select
                        value={entityType}
                        onChange={(e) => setEntityType(e.target.value)}
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      >
                        <option value="">선택</option>
                        <option value="CUSTOMER">고객 (CUSTOMER)</option>
                        <option value="ACCOUNT">계좌 (ACCOUNT)</option>
                        <option value="DEVICE">디바이스 (DEVICE)</option>
                      </select>
                    </div>

                    {/* Entity ID Field */}
                    <div>
                      <label className="block text-sm font-medium text-gray-700 mb-2">
                        Entity ID 필드 <span className="text-red-500">*</span>
                      </label>
                      <input
                        type="text"
                        value={entityIdField}
                        onChange={(e) => setEntityIdField(e.target.value)}
                        placeholder="예: customer_id, account_number"
                        className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                      />
                    </div>
                  </div>

                  <p className="text-xs text-gray-500 -mt-2">
                    entity_attributes 테이블의 entity_id로 사용할 원본 데이터 필드명
                  </p>

                  {/* Store Fields - 입력 박스가 먼저, chips가 아래 */}
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      저장 필드 목록
                    </label>
                    <div className="space-y-2">
                      {/* 새 필드 추가 입력 */}
                      <div className="flex gap-2">
                        <input
                          type="text"
                          value={newStoreField}
                          onChange={(e) => setNewStoreField(e.target.value)}
                          onKeyPress={(e) => e.key === "Enter" && handleAddStoreField()}
                          placeholder="필드명 입력 후 추가 버튼 클릭"
                          className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500"
                        />
                        <button
                          onClick={handleAddStoreField}
                          className="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700"
                        >
                          + 추가
                        </button>
                      </div>

                      {/* 선택된 필드들 chips */}
                      {storeFields.length > 0 && (
                        <div className="flex flex-wrap gap-2 p-3 bg-gray-50 rounded-lg">
                          {storeFields.map((field, idx) => (
                            <span key={idx} className="inline-flex items-center px-3 py-1 bg-indigo-100 text-indigo-800 rounded-md text-sm">
                              {field}
                              <button
                                onClick={() => handleRemoveStoreField(field)}
                                className="ml-2 text-indigo-600 hover:text-indigo-800 font-bold"
                              >
                                ×
                              </button>
                            </span>
                          ))}
                        </div>
                      )}

                      <p className="text-xs text-gray-500">
                        {storeFields.length === 0
                          ? "필드를 지정하지 않으면 모든 필드가 저장됩니다."
                          : `${storeFields.length}개 필드가 entity_attributes에 저장됩니다.`}
                      </p>
                    </div>
                  </div>
                </>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* 하단 저장 버튼 */}
      <div className="p-6 border-t border-gray-200 bg-gray-50">
        <LoadingButton
          onClick={handleSave}
          disabled={detectKeyType === "" || (detectKeyType !== "CUSTOM" && detectKey.length === 0)}
          className="w-full px-4 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed font-medium"
          title={detectKeyType === "" ? "탐지키 타입을 선택해주세요" : (detectKeyType !== "CUSTOM" && detectKey.length === 0 ? "탐지키 필드를 선택해주세요" : "")}
        >
          저장
        </LoadingButton>
      </div>
    </div>
  );
}
