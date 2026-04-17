package com.itmasters.icon.api.transaction.adapter.in.web;

import com.itmasters.icon.api.transaction.application.service.TransactionService;
import com.itmasters.icon.api.transaction.dto.TransactionDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 트랜잭션 추적 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Tag(name = "Transaction Tracking", description = "트랜잭션 추적 API")
public class TransactionController {
    
    private final TransactionService transactionService;
    
    /**
     * 최근 트랜잭션 목록 조회 (30개)
     *
     * @return 최근 트랜잭션 목록
     */
    @GetMapping
    @Operation(summary = "최근 트랜잭션 목록", description = "최근 30개 트랜잭션을 조회합니다")
    public List<TransactionDto.TransactionListItem> getRecentTransactions() {
        log.info("GET /api/v1/transactions - 최근 트랜잭션 목록 조회");
        return transactionService.getRecentTransactions();
    }
    
    /**
     * 트랜잭션 ID로 전체 파이프라인 추적
     *
     * @param transactionId 트랜잭션 ID (예: order_12345, tx_20250101_001, session_abc123)
     * @return 트랜잭션 전체 추적 정보 (PREP-1 → DET-1 → DET-2 전 단계)
     */
    @GetMapping("/{transactionId}")
    @Operation(summary = "트랜잭션 추적", description = "트랜잭션 ID로 파이프라인 전체를 추적합니다")
    public TransactionDto.TrackingInfo trackTransaction(@PathVariable String transactionId) {
        log.info("GET /api/v1/transactions/{} - 트랜잭션 추적 요청", transactionId);
        return transactionService.trackTransaction(transactionId);
    }
}
