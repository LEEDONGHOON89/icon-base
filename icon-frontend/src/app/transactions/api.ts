import api from "@/lib/api";
import { TransactionTrackingInfo, TransactionListItem } from "@/types/transaction";

/**
 * 트랜잭션 추적 API
 */

/**
 * 트랜잭션 ID로 전체 파이프라인 추적
 */
export async function trackTransaction(
  transactionId: string
): Promise<TransactionTrackingInfo> {
  const response = await api.get<TransactionTrackingInfo>(
    `/api/v1/transactions/${encodeURIComponent(transactionId)}`
  );
  return response.data;
}

/**
 * 최근 트랜잭션 목록 조회 (30개)
 */
export async function getRecentTransactions(): Promise<TransactionListItem[]> {
  const response = await api.get<TransactionListItem[]>("/api/v1/transactions");
  return response.data;
}
