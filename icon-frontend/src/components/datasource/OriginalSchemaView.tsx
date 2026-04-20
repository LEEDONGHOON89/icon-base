"use client";

import {
  fetchDataSourceOriginalSchemas,
  updateDataSourceOriginalSchemasBulk,
  updateOriginalSchemaStandardFieldMapping,
  fetchStandardFields,
  StandardFieldMappingRequest,
} from "@/app/data-sources/api";
import LoadingButton from "@/components/common/LoadingButton";
import Autocomplete from "@/components/common/Autocomplete";
import { useErrorHandling } from "@/hooks/useErrorHandling";
import { useQueryWithErrorHandling } from "@/hooks/useQueryWithErrorHandling";
import {
  closestCenter,
  DndContext,
  DragEndEvent,
  KeyboardSensor,
  PointerSensor,
  useSensor,
  useSensors,
} from "@dnd-kit/core";
import {
  arrayMove,
  SortableContext,
  sortableKeyboardCoordinates,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import {
  ArrowPathIcon,
  DocumentTextIcon,
  MagnifyingGlassIcon,
  PlusIcon,
  TrashIcon,
} from "@heroicons/react/24/outline";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "react-hot-toast";

interface OriginalSchemaViewProps {
  dataSourceId: string;
  dataSourceName: string;
}

// Sortable Row Component
// [2026-04-20] 파서는 데이터소스 레벨로 이동 — 필드단 파서 관련 props 제거
function SortableRow({
  schema,
  editingSchemas,
  handleFieldChange,
  handleRemoveField,
  handleStandardFieldMappingChange,
  standardFields,
  index,
}: any) {
  const {
    attributes,
    listeners,
    setNodeRef,
    transform,
    transition,
    isDragging,
  } = useSortable({ id: schema.schemaId });

  const style = {
    transform: CSS.Transform.toString(transform),
    transition,
  };

  const currentEditData = editingSchemas[schema.schemaId] || {
    ...schema,
    fieldName: schema.fieldName || "",
    dataType: schema.dataType || "STRING",
    isRequired: schema.isRequired || false,
    description: schema.description || "",
    isActive: schema.isActive !== undefined ? schema.isActive : true,
    fieldOrder: schema.fieldOrder || 1,
  };

  return (
    <tr
      ref={setNodeRef}
      style={style}
      className={`
        ${schema.isNew ? "bg-blue-50" : ""} 
        ${isDragging ? "opacity-50 shadow-lg bg-gray-50" : ""}
        hover:bg-gray-50 transition-colors
      `}
    >
      {/* Order number and Drag handle */}
      <td className="px-3 py-4 whitespace-nowrap w-20">
        <div className="flex items-center gap-2">
          <span className="text-xs text-gray-500 w-6 text-center">
            {index + 1}
          </span>
          <div
            {...attributes}
            {...listeners}
            className="cursor-move text-gray-600 hover:text-gray-900 p-1 rounded hover:bg-gray-100 transition-all"
            style={{ touchAction: "none" }}
          >
            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
              <circle cx="5" cy="5" r="1.5" />
              <circle cx="5" cy="10" r="1.5" />
              <circle cx="5" cy="15" r="1.5" />
              <circle cx="10" cy="5" r="1.5" />
              <circle cx="10" cy="10" r="1.5" />
              <circle cx="10" cy="15" r="1.5" />
              <circle cx="15" cy="5" r="1.5" />
              <circle cx="15" cy="10" r="1.5" />
              <circle cx="15" cy="15" r="1.5" />
            </svg>
          </div>
        </div>
      </td>

      {/* Field Name */}
      <td className="px-4 py-4 whitespace-nowrap w-60">
        {true ? (
          <input
            type="text"
            value={currentEditData.fieldName || ""}
            onChange={(e) =>
              handleFieldChange(
                schema.schemaId,
                "fieldName",
                e.target.value,
                true
              )
            }
            className="w-full px-2 py-1 border border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500"
            placeholder="필드명 입력"
          />
        ) : (
          <span className="text-sm font-medium text-gray-900">
            {schema.fieldName}
          </span>
        )}
      </td>

      {/* Data Type */}
      <td className="px-4 py-4 whitespace-nowrap w-40">
        {true ? (
          <select
            value={currentEditData.dataType || ""}
            onChange={(e) =>
              handleFieldChange(
                schema.schemaId,
                "dataType",
                e.target.value,
                true
              )
            }
            className="w-full px-2 py-1 border border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500"
          >
            <option value="">선택</option>
            <option value="STRING">문자열</option>
            <option value="INTEGER">정수</option>
            <option value="DOUBLE">실수</option>
            <option value="BOOLEAN">불린</option>
            <option value="DATETIME">날짜시간</option>
          </select>
        ) : (
          <span className="text-sm text-gray-900">
            {schema.dataType}
          </span>
        )}
      </td>

      {/* Standard Field Mapping */}
      <td className="px-6 py-4 w-72">
        <div className="space-y-2">
          {/* 표준 필드 선택 - 신규 필드도 선택 가능 */}
          <Autocomplete
            options={[
              { value: "", label: "선택 안함", category: "NONE" },
              ...standardFields
                .filter((field: any) =>
                  field.dataType === schema.dataType &&
                  field.fieldId != null &&
                  field.fieldId !== ""
                )
                .map((field: any) => {
                  const name = field.displayName || field.fieldName || field.fieldId;
                  const code = field.fieldName || field.fieldId || "";
                  const label = code ? `${name} (${code})` : `${name}`;
                  return {
                    value: field.fieldId,
                    label,
                    category: field.category || 'UNCATEGORIZED'
                  };
                })
            ]}
            value={schema.standardFieldId || ""}
            onChange={(value) => handleStandardFieldMappingChange(
              schema.schemaId,
              value || undefined
            )}
            placeholder="표준 필드 선택..."
            groupBy={(option: any) => {
              const categoryLabels: Record<string, string> = {
                NONE: "선택 옵션",
                CUSTOMER: "고객 정보",
                TRANSACTION: "거래 관련",
                ACCOUNT: "계좌 관련",
                SECURITY: "보안 관련",
                DATETIME: "날짜/시간",
                SYSTEM: "시스템 관련",
                ACCESS: "접속/로그인",
                DEVICE: "디바이스 관련",
                CERTIFICATE: "인증서/OTP",
                LOAN: "대출 관련",
                OPEN_BANKING: "오픈뱅킹",
                ATM: "ATM 관련",
                BLACKLIST: "블랙리스트",
                UNCATEGORIZED: "기타"
              };
              return categoryLabels[option.category] || option.category;
            }}
          />

          {/* 변환 규칙 입력 제거됨 */}
        </div>
      </td>

      {/* Required */}
      <td className="px-4 py-4 whitespace-nowrap text-center">
        {true ? (
          <input
            type="checkbox"
            checked={currentEditData.isRequired || false}
            onChange={(e) =>
              handleFieldChange(
                schema.schemaId,
                "isRequired",
                e.target.checked,
                true
              )
            }
            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
          />
        ) : (
          <span className="text-sm text-gray-900">
            {schema.isRequired ? "O" : "X"}
          </span>
        )}
      </td>

      {/* Active Status */}
      <td className="px-4 py-4 whitespace-nowrap text-center">
        {true ? (
          <input
            type="checkbox"
            checked={currentEditData.isActive !== undefined ? currentEditData.isActive : false}
            onChange={(e) =>
              handleFieldChange(
                schema.schemaId,
                "isActive",
                e.target.checked,
                true
              )
            }
            className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
          />
        ) : (
          <span
            className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full ${
              schema.isActive
                ? "bg-green-100 text-green-800"
                : "bg-red-100 text-red-800"
            }`}
          >
            {schema.isActive ? "활성" : "비활성"}
          </span>
        )}
      </td>

      {/* Description */}
      <td className="px-4 py-4 w-60">
        {true ? (
          <input
            type="text"
            value={currentEditData.description || ""}
            onChange={(e) =>
              handleFieldChange(
                schema.schemaId,
                "description",
                e.target.value,
                true
              )
            }
            className="w-full px-2 py-1 border border-gray-300 rounded focus:ring-blue-500 focus:border-blue-500"
            placeholder="설명 입력"
          />
        ) : (
          <span className="text-sm text-gray-700">
            {schema.description || "-"}
          </span>
        )}
      </td>
    </tr>
  );
}

export default function OriginalSchemaView({
  dataSourceId,
  dataSourceName,
}: OriginalSchemaViewProps) {
  const [searchTerm, setSearchTerm] = useState("");
  const [editableSchemas, setEditableSchemas] = useState<any[]>([]);
  const [editingSchemas, setEditingSchemas] = useState<{ [key: string]: any }>(
    {}
  );
  const queryClient = useQueryClient();
  const { handleError } = useErrorHandling();

  const sensors = useSensors(
    useSensor(PointerSensor),
    useSensor(KeyboardSensor, {
      coordinateGetter: sortableKeyboardCoordinates,
    })
  );

  // 원본 스키마 조회
  const {
    data: schemas = [],
    isLoading,
    refetch,
  } = useQueryWithErrorHandling({
    queryKey: ["dataSourceOriginalSchemas", dataSourceId],
    queryFn: () => fetchDataSourceOriginalSchemas(dataSourceId),
  });

  // 표준 필드 조회
  const { data: standardFields = [] } = useQueryWithErrorHandling({
    queryKey: ["standardFields"],
    queryFn: fetchStandardFields,
  });

  // 스키마 데이터가 변경되면 편집 가능한 스키마 업데이트
  useEffect(() => {
    setEditableSchemas(schemas);
  }, [schemas]);

  const filteredSchemas = editableSchemas.filter((schema: any) =>
    schema.fieldName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const handleRefresh = async () => {
    await refetch();
  };

  // 스키마 일괄 수정 mutation (편집 내용 저장용)
  const updateSchemaMutation = useMutation({
    mutationFn: (schemas: any[]) =>
      updateDataSourceOriginalSchemasBulk(dataSourceId, schemas),
    onSuccess: () => {
      toast.success("필드가 저장되었습니다.");
      queryClient.invalidateQueries({
        queryKey: ["dataSourceOriginalSchemas", dataSourceId],
      });
      setEditingSchemas({});
    },
    onError: handleError,
  });

  // 표준 필드 매핑 업데이트 mutation (저장 버튼 클릭 시에만 사용)
  const updateMappingMutation = useMutation({
    mutationFn: async (params: {
      schemaId: string;
      request: StandardFieldMappingRequest;
    }) => {
      return updateOriginalSchemaStandardFieldMapping(params.schemaId, params.request);
    },
    onSuccess: (updatedSchema) => {
      // 로컬 상태 업데이트
      setEditableSchemas((prevSchemas) =>
        prevSchemas.map((schema) =>
          schema.schemaId === updatedSchema.schemaId ? updatedSchema : schema
        )
      );
    },
    onError: handleError,
  });

  const handleAddField = () => {
    const newSchemaId = `new-${Date.now()}`;
    const newField = {
      schemaId: newSchemaId,
      fieldName: "", // 필수 입력
      dataType: "STRING", // 기본값
      isRequired: false, // 기본값
      description: "", // 기본값 (비어있음)
      isActive: true, // 기본값
      fieldOrder: editableSchemas.length + 1, // 마지막 순서
      isNew: true,
    };
    setEditableSchemas([...editableSchemas, newField]);

    // 새 필드를 editingSchemas에도 추가하여 변경사항을 추적
    setEditingSchemas((prev) => ({
      ...prev,
      [newSchemaId]: newField,
    }));
  };

  const handleRemoveField = (schemaId: string) => {
    setEditableSchemas(
      editableSchemas.filter((schema) => schema.schemaId !== schemaId)
    );

    // editingSchemas에서도 제거
    setEditingSchemas((prev) => {
      const newEditingSchemas = { ...prev };
      delete newEditingSchemas[schemaId];
      return newEditingSchemas;
    });
  };

  const handleStandardFieldMappingChange = (
    schemaId: string,
    standardFieldId?: string
  ) => {
    // editableSchemas를 직접 업데이트 (저장 버튼 클릭 전까지는 로컬 상태만 변경)
    setEditableSchemas((prevSchemas) =>
      prevSchemas.map((schema) =>
        schema.schemaId === schemaId
          ? { ...schema, standardFieldId: standardFieldId || undefined }
          : schema
      )
    );

    // editingSchemas에도 변경사항 추가 (저장 시 추적용)
    setEditingSchemas((prev) => {
      const currentSchema = editableSchemas.find((s) => s.schemaId === schemaId);
      const existingEdit = prev[schemaId] || currentSchema || {};
      return {
        ...prev,
        [schemaId]: { ...existingEdit, standardFieldId: standardFieldId || undefined },
      };
    });
  };

  const handleFieldChange = (
    schemaId: string,
    field: string,
    value: any,
    isEditingExisting?: boolean
  ) => {
    // 새 필드인 경우 editableSchemas에도 직접 업데이트
    const targetSchema = editableSchemas.find((s) => s.schemaId === schemaId);

    if (targetSchema?.isNew) {
      // 새 필드는 editableSchemas에 직접 업데이트
      setEditableSchemas(
        editableSchemas.map((schema) =>
          schema.schemaId === schemaId ? { ...schema, [field]: value } : schema
        )
      );
      // editingSchemas에도 동시에 업데이트 (순서 변경 등을 위해)
      setEditingSchemas((prev) => {
        const currentSchema = editableSchemas.find(
          (s) => s.schemaId === schemaId
        );
        const existingEdit = prev[schemaId] || currentSchema || {};
        return {
          ...prev,
          [schemaId]: { ...existingEdit, [field]: value },
        };
      });
    } else {
      // 기존 필드는 editingSchemas에만 업데이트
      if (isEditingExisting) {
        setEditingSchemas((prev) => {
          const currentSchema = editableSchemas.find(
            (s) => s.schemaId === schemaId
          );
          const existingEdit = prev[schemaId] || currentSchema || {};
          return {
            ...prev,
            [schemaId]: { ...existingEdit, [field]: value },
          };
        });
      } else {
        setEditableSchemas(
          editableSchemas.map((schema) =>
            schema.schemaId === schemaId
              ? { ...schema, [field]: value }
              : schema
          )
        );
      }
    }
  };

  // 순서변경
  const handleDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (active.id !== over?.id) {
      const oldIndex = editableSchemas.findIndex(
        (item) => item.schemaId === active.id
      );
      const newIndex = editableSchemas.findIndex(
        (item) => item.schemaId === over?.id
      );

      const newSchemas = arrayMove(editableSchemas, oldIndex, newIndex);
      // 순서 정보 업데이트
      const updatedSchemas = newSchemas.map((schema, index) => ({
        ...schema,
        fieldOrder: index + 1,
      }));

      setEditableSchemas(updatedSchemas);

      // 순서가 변경된 모든 필드를 editingSchemas에 추가 (신규 필드 포함)
      const newEditingSchemas = { ...editingSchemas };
      updatedSchemas.forEach((schema, index) => {
        const newOrder = index + 1;
        const originalOrder =
          editableSchemas.find((s) => s.schemaId === schema.schemaId)
            ?.fieldOrder || 0;

        if (originalOrder !== newOrder) {
          // 순서가 변경된 필드를 editingSchemas에 추가 (기존 수정사항이 있으면 병합)
          const existingEdit = newEditingSchemas[schema.schemaId] || {};
          newEditingSchemas[schema.schemaId] = {
            ...schema,
            ...existingEdit, // 기존 수정사항 유지
            fieldOrder: newOrder,
          };
          console.log(
            `Added to editingSchemas: ${schema.schemaId}`,
            newEditingSchemas[schema.schemaId]
          );
        }
      });

      setEditingSchemas(newEditingSchemas);
    }
  };

  const handleSaveAllChanges = async () => {
    const editedSchemas = Object.keys(editingSchemas);
    const hasOrderChanges = editableSchemas.some(
      (schema, index) => schema.fieldOrder !== index + 1
    );

    // 새 필드가 있는지 확인
    const hasNewFields = editableSchemas.some((schema) => schema.isNew);

    console.log("=== 통합 저장 디버깅 ===");
    console.log("editedSchemas:", editedSchemas);
    console.log("editedSchemas.length:", editedSchemas.length);
    console.log("hasOrderChanges:", hasOrderChanges);
    console.log("hasNewFields:", hasNewFields);
    console.log("editingSchemas:", editingSchemas);
    console.log("editableSchemas:", editableSchemas);

    // 저장 버튼이 항상 노출되므로 변경사항이 없어도 경고 메시지만 표시하고 진행
    if (editedSchemas.length === 0 && !hasOrderChanges && !hasNewFields) {
      toast.success("변경된 내용이 없습니다.");
      return;
    }

    // 유효성 검사: 새 필드는 필드명이 필수
    const invalidNewFields = editableSchemas.filter(
      (schema) =>
        schema.isNew && (!schema.fieldName || !schema.fieldName.trim())
    );

    if (invalidNewFields.length > 0) {
      toast.error("새 필드의 필드명을 입력해주세요.");
      return;
    }

    // 유효성 검사: 기존 필드의 수정된 필드명 검사
    const invalidEditedFields = editedSchemas.filter((schemaId) => {
      const editData = editingSchemas[schemaId];
      const originalSchema = editableSchemas.find(
        (s) => s.schemaId === schemaId
      );

      // 새 필드가 아니고 fieldName이 수정된 경우에만 유효성 검사
      return (
        !originalSchema?.isNew &&
        editData?.fieldName !== undefined &&
        (!editData.fieldName || !editData.fieldName.trim())
      );
    });

    if (invalidEditedFields.length > 0) {
      toast.error("필드명을 모두 입력해주세요.");
      return;
    }

    // 모든 필드 (새 필드 + 기존 필드)를 하나의 배열로 통합 처리
    const allChanges: any[] = [];

    editableSchemas.forEach((schema, index) => {
      const editData = editingSchemas[schema.schemaId];
      const fieldOrder = index + 1; // 현재 인덱스 + 1

      if (schema.isNew) {
        // 새 필드: schemaId를 빈 문자열로 설정
        const newFieldData: any = {
          schemaId: "", // 빈 문자열로 새 필드임을 표시
          fieldName: schema.fieldName.trim(),
          dataType: schema.dataType || "STRING",
          isRequired: schema.isRequired || false,
          description: schema.description?.trim() || undefined,
          fieldOrder: fieldOrder,
          isActive: true, // 새 필드는 항상 활성
          // 표준 필드 매핑 정보도 포함
          standardFieldId: schema.standardFieldId || undefined,
          transformRule: schema.transformRule || undefined,
        };
        allChanges.push(newFieldData);
      } else {
        // 기존 필드: 수정된 항목만 전송 (순서 변경도 포함)
        const originalFieldOrder = schemas.find(s => s.schemaId === schema.schemaId)?.fieldOrder || 0;
        const hasOrderChanged = originalFieldOrder !== fieldOrder;
        const hasEditData = editData && Object.keys(editData).length > 0;

        // 순서가 변경되었거나 수정된 데이터가 있는 경우에만 포함
        if (hasOrderChanged || hasEditData) {
          const changeData: any = {
            schemaId: schema.schemaId,
            fieldOrder: fieldOrder,
          };

          // editingSchemas에 수정된 데이터가 있으면 추가
          if (editData) {
            if (editData.fieldName !== undefined)
              changeData.fieldName = editData.fieldName;
            if (editData.dataType !== undefined)
              changeData.dataType = editData.dataType;
            if (editData.isRequired !== undefined)
              changeData.isRequired = editData.isRequired;
            if (editData.description !== undefined)
              changeData.description = editData.description;
            if (editData.isActive !== undefined)
              changeData.isActive = editData.isActive;
          }

          allChanges.push(changeData);
        }
      }
    });

    console.log("통합 저장할 데이터:", allChanges);

    // 표준 필드 매핑 변경사항 수집
    const mappingChanges: Array<{ schemaId: string; request: StandardFieldMappingRequest }> = [];
    
    editableSchemas.forEach((schema) => {
      // 새 필드는 제외 (아직 저장되지 않은 필드)
      if (schema.isNew) return;
      
      const originalSchema = schemas.find(s => s.schemaId === schema.schemaId);
      const editData = editingSchemas[schema.schemaId];
      
      // 표준 필드 매핑, 변환 규칙 변경 여부 확인
      const hasStandardFieldChange = editData && (
        editData.standardFieldId !== undefined ||
        editData.transformRule !== undefined
      );

      if (hasStandardFieldChange) {
        mappingChanges.push({
          schemaId: schema.schemaId,
          request: {
            standardFieldId: editData.standardFieldId,
            transformRule: editData.transformRule,
            isActive: editData.isActive !== undefined ? editData.isActive : true,
          }
        });
      }
    });

    // 모든 변경사항 저장
    const savePromises: Promise<any>[] = [];
    
    // 필드 변경사항 저장
    if (allChanges.length > 0) {
      savePromises.push(updateSchemaMutation.mutateAsync(allChanges));
    }
    
    // 표준 필드 매핑 변경사항 저장
    mappingChanges.forEach((change) => {
      savePromises.push(updateMappingMutation.mutateAsync(change));
    });
    
    if (savePromises.length > 0) {
      try {
        await Promise.all(savePromises);
        toast.success("모든 변경사항이 저장되었습니다.");
        setEditingSchemas({}); // 편집 상태 초기화
        queryClient.invalidateQueries({
          queryKey: ["dataSourceOriginalSchemas", dataSourceId],
        });
      } catch (error) {
        console.error("저장 중 오류 발생:", error);
        toast.error("일부 변경사항 저장에 실패했습니다.");
      }
    } else {
      toast.success("저장할 변경사항이 없습니다.");
    }
  };

  const handleCancel = () => {
    setEditableSchemas(schemas);
    setEditingSchemas({});
  };

  const handleCancelAllEdits = () => {
    // 변경된 내용이 있는지 확인
    const hasChanges =
      Object.keys(editingSchemas).length > 0 ||
      editableSchemas.some(
        (schema, index) => schema.fieldOrder !== index + 1 && !schema.isNew
      ) ||
      editableSchemas.some((schema) => schema.isNew);

    if (hasChanges) {
      // 변경사항을 롤백
      setEditingSchemas({});
      // 순서 변경 및 새 필드 추가도 초기화
      setEditableSchemas(schemas);
      toast.success("변경사항이 취소되었습니다.");
    } else {
      toast.success("취소할 변경사항이 없습니다.");
    }
  };

  if (isLoading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* 헤더 */}
      <div className="flex justify-between items-center">
        <div>
          <h3 className="text-lg font-semibold text-gray-900">원본 필드</h3>
          <p className="mt-1 text-sm text-gray-600">
            {dataSourceName}의 실제 데이터 필드 정보입니다. (드래그하여 순서
            변경 가능)
          </p>
        </div>
        <div className="flex gap-2">
          <LoadingButton
            onClick={handleRefresh}
            className="inline-flex items-center gap-2"
            size="small"
          >
            <ArrowPathIcon className="h-4 w-4" />
            새로고침
          </LoadingButton>
          <LoadingButton
            onClick={handleSaveAllChanges}
            loading={updateSchemaMutation.isPending}
            className="inline-flex items-center gap-2 px-3 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
            size="small"
          >
            저장
          </LoadingButton>
          <button
            onClick={handleCancelAllEdits}
            className="inline-flex items-center gap-2 px-3 py-2 border border-gray-300 shadow-sm text-sm leading-4 font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50"
          >
            취소
          </button>
        </div>
      </div>

      {/* 검색 */}
      <div className="relative">
        <MagnifyingGlassIcon className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-gray-400" />
        <input
          type="text"
          placeholder="필드명으로 검색..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-10 pr-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
        />
      </div>

      {/* 스키마 목록 */}
      {filteredSchemas.length === 0 ? (
        <div className="text-center py-12">
          <DocumentTextIcon className="mx-auto h-12 w-12 text-gray-400" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">
            {searchTerm ? "검색 결과가 없습니다" : "스키마가 없습니다"}
          </h3>
          <p className="mt-1 text-sm text-gray-500">
            {searchTerm
              ? "다른 검색어를 시도해보세요."
              : "데이터소스에서 스키마를 가져오거나 수동으로 추가하세요."}
          </p>
          <button
            onClick={handleAddField}
            className="mt-4 inline-flex items-center gap-2 px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700"
          >
            <PlusIcon className="h-5 w-5" />
            필드 추가
          </button>
        </div>
      ) : (
        <div className="bg-white shadow-sm rounded-lg overflow-hidden">
          <DndContext
            sensors={sensors}
            collisionDetection={closestCenter}
            onDragEnd={handleDragEnd}
          >
            <table className="min-w-full divide-y divide-gray-200">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-3 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    <span className="pl-8">순서</span>
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    필드명
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    데이터 타입
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    매핑된 표준 필드
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    필수
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    상태
                  </th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                    설명
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white divide-y divide-gray-200">
                <SortableContext
                  items={filteredSchemas.map((s: any) => s.schemaId)}
                  strategy={verticalListSortingStrategy}
                >
                  {filteredSchemas.map((schema: any, index: number) => (
                    <SortableRow
                      key={schema.schemaId}
                      schema={schema}
                      index={index}
                      editingSchemas={editingSchemas}
                      handleFieldChange={handleFieldChange}
                      handleRemoveField={handleRemoveField}
                      handleStandardFieldMappingChange={handleStandardFieldMappingChange}
                      standardFields={standardFields}
                    />
                  ))}
                </SortableContext>
                <tr>
                  <td
                    colSpan={7}
                    className="px-6 py-4 text-center border-t border-gray-200"
                  >
                    <button
                      onClick={handleAddField}
                      className="inline-flex items-center gap-2 px-4 py-2 text-sm text-blue-600 hover:text-blue-700 hover:bg-blue-50 rounded-md transition-colors"
                    >
                      <PlusIcon className="h-5 w-5" />
                      필드 추가
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </DndContext>
        </div>
      )}

      {/* 총 개수 */}
      <div className="text-sm text-gray-600 text-right">
        총 {filteredSchemas.length}개의 필드
      </div>
    </div>
  );
}
