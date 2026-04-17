import {
  useQuery,
  UseQueryOptions,
  UseQueryResult,
} from "@tanstack/react-query";
import { useEffect } from "react";
import { useErrorHandling, ErrorResponse } from "./useErrorHandling";

/**
 * useQuery와 useErrorHandling을 결합한 커스텀 훅
 * - useQuery의 에러를 자동으로 useErrorHandling으로 처리
 * - toast 알림 자동 표시
 */
export function useQueryWithErrorHandling<
  TQueryFnData = unknown,
  TError = Error,
  TData = TQueryFnData
>(
  options: Omit<
    UseQueryOptions<TQueryFnData, TError, TData>,
    "throwOnError" | "retry"
  >
): UseQueryResult<TData, TError> & { errorQuery: ErrorResponse | null } {
  const { error, handleError } = useErrorHandling();

  const query = useQuery({
    ...options,
    // 에러가 발생해도 컴포넌트를 언마운트하지 않도록 설정
    throwOnError: false,
    // 401 에러는 재시도하지 않음 (인터셉터에서 처리)
    retry: (failureCount, error: any) => {
      // 401 에러인 경우 재시도하지 않음
      if (error?.response?.status === 401) {
        return false;
      }
      // 다른 에러는 2회까지 재시도
      return failureCount < 2;
    },
  });

  useEffect(() => {
    if (query.error) {
      handleError(query.error);
    }
  }, [query.error, handleError]);

  return { ...query, errorQuery: error };
}
