import api from "@/lib/api";

export type ExcludedValue = { id: number; value: string; isActive: boolean };

export async function fetchExcludedValues(params?: { activeOnly?: boolean }) {
  const res = await api.get("/api/v1/engine-settings/excluded-group-values", { params });
  return (res.data?.data || []) as ExcludedValue[];
}

export async function addExcludedValue(value: string, isActive = true) {
  const res = await api.post("/api/v1/engine-settings/excluded-group-values", { value, isActive });
  return res.data?.data as ExcludedValue;
}

export async function updateExcludedValue(id: number, patch: Partial<ExcludedValue>) {
  const res = await api.patch(`/api/v1/engine-settings/excluded-group-values/${id}`, patch);
  return res.data?.data as ExcludedValue;
}

export async function deleteExcludedValue(id: number) {
  await api.delete(`/api/v1/engine-settings/excluded-group-values/${id}`);
}

export async function reloadExcludedCache() {
  // 엔진 endpoint (동일 베이스 URL 상정)
  const res = await api.post("/api/v1/engine/settings/excluded-group-values/reload");
  return res.data;
}

