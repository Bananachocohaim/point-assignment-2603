package io.github.bananachocohaim.pointassignment2603.api.admin.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.bananachocohaim.pointassignment2603.api.admin.dto.EarnHistoryItemResponse;
import io.github.bananachocohaim.pointassignment2603.api.admin.dto.UsageHistoryItemResponse;
import io.github.bananachocohaim.pointassignment2603.api.admin.dto.WalletInfoResponse;
import io.github.bananachocohaim.pointassignment2603.api.admin.dto.WalletMaxBalanceUpdateRequest;
import io.github.bananachocohaim.pointassignment2603.api.admin.dto.WalletMaxBalanceUpdateResponse;
import io.github.bananachocohaim.pointassignment2603.domain.point.service.AdminPointService;
import io.github.bananachocohaim.pointassignment2603.domain.point.service.PointDomainService;
import io.github.bananachocohaim.pointassignment2603.domain.point.service.PointDomainService.UpdateMaxBalanceLimitResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/wallet")
@RequiredArgsConstructor
public class AdminWalletController {

    private final PointDomainService pointDomainService;
    private final AdminPointService adminPointService;

    /** 전체 지갑 목록 조회 */
    @GetMapping
    public ResponseEntity<List<WalletInfoResponse>> getAllWallets() {
        return ResponseEntity.ok(adminPointService.getAllWallets());
    }

    /** userId로 지갑 목록 조회 */
    @GetMapping("/search")
    public ResponseEntity<List<WalletInfoResponse>> searchWallets(
        @RequestParam String userId
    ) {
        return ResponseEntity.ok(adminPointService.searchWallets(userId));
    }

    /** 지갑의 적립 이력 조회 (earnStatus 없으면 전체) */
    @GetMapping("/{walletId}/earn-history")
    public ResponseEntity<List<EarnHistoryItemResponse>> getEarnHistory(
        @PathVariable String walletId,
        @RequestParam(required = false) String earnStatus
    ) {
        return ResponseEntity.ok(adminPointService.getEarnHistory(walletId, earnStatus));
    }

    /** 지갑의 사용/취소 이력 조회 (usageType 없으면 전체) */
    @GetMapping("/{walletId}/usage-history")
    public ResponseEntity<List<UsageHistoryItemResponse>> getUsageHistory(
        @PathVariable String walletId,
        @RequestParam(required = false) String usageType
    ) {
        return ResponseEntity.ok(adminPointService.getUsageHistory(walletId, usageType));
    }

    /** 지갑별 무료 포인트 최대 보유 한도 변경 */
    @PutMapping("/{walletId}/max-balance")
    public ResponseEntity<WalletMaxBalanceUpdateResponse> updateMaxBalance(
        @PathVariable String walletId,
        @Valid @RequestBody WalletMaxBalanceUpdateRequest requestDto
    ) {
        log.info("지갑 최대 보유 한도 변경 요청: walletId={}, maxBalanceLimit={}", walletId, requestDto.maxBalanceLimit());
        UpdateMaxBalanceLimitResult result = pointDomainService.updateMaxBalanceLimit(walletId, requestDto.maxBalanceLimit());
        WalletMaxBalanceUpdateResponse res = new WalletMaxBalanceUpdateResponse(
            result.walletId(), result.balance(), result.maxBalanceLimit());
        log.info("지갑 최대 보유 한도 변경 완료: {}", res);
        return ResponseEntity.ok(res);
    }
}
