package io.github.bananachocohaim.pointassignment2603.api.point.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointEarnCancelRequest;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointEarnCancelResponse;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointEarnRequest;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointEarnResultResponse;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointUsageCancelRequest;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointUsageCancelResponse;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointUsageRequest;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointUsageResultResponse;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointUsageResultResponse.UsageEarnDetail;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.UserPointWalletInquiryRequest;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.UserPointWalletInquiryResponse;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.UserPointWalletInquiryResponse.WalletDetail;
import io.github.bananachocohaim.pointassignment2603.common.component.IdGenerator;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UsageType;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UserPointWallet;
import io.github.bananachocohaim.pointassignment2603.domain.point.service.PointDomainService;
import io.github.bananachocohaim.pointassignment2603.domain.point.service.PointDomainService.CancelEarnResult;
import io.github.bananachocohaim.pointassignment2603.domain.point.service.PointDomainService.CancelPointResult;
import io.github.bananachocohaim.pointassignment2603.domain.point.service.PointDomainService.EarnPointResult;
import io.github.bananachocohaim.pointassignment2603.domain.point.service.PointDomainService.UsePointResult;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointApiService {

    private final PointDomainService pointDomainService;
    private final IdGenerator idGenerator;

    private static final int DEFAULT_EXPIRY_DAYS = 365;

    /**
     * 포인트 지갑 조회
     */
    public UserPointWalletInquiryResponse getUserPointWalletInfo(UserPointWalletInquiryRequest requestDto) {
        List<UserPointWallet> wallets = pointDomainService.getWalletsWithExpirationRefreshed(
            requestDto.userId(), requestDto.walletType());

        long totalBalance = wallets.stream().mapToLong(UserPointWallet::getBalance).sum();
        List<WalletDetail> walletDetails = wallets.stream()
            .map(x -> WalletDetail.of(
                x.getWalletId(), x.getWalletType().name(), x.getBalance(),
                x.getNextExpirationDate(), x.getExpiringAmount()))
            .toList();

        return UserPointWalletInquiryResponse.of(requestDto.userId(), totalBalance, walletDetails);
    }

    /**
     * 포인트 적립
     */
    public PointEarnResultResponse earnPoint(PointEarnRequest requestDto) {
        //만료일 설정
        int expiryDays = requestDto.expiryDays() != null ? requestDto.expiryDays() : DEFAULT_EXPIRY_DAYS;
        LocalDate expirationDate = LocalDate.now().plusDays(expiryDays);
        
        //적립 아이디 채번
        String earnId = idGenerator.getPointEarnId();

        //포인트 적립 처리
        EarnPointResult result = pointDomainService.earnPoint(
            requestDto.walletId(), earnId, requestDto.orderNo(), requestDto.earnType(),
            requestDto.amount(), expiryDays, expirationDate, requestDto.originalEarnId());

        return PointEarnResultResponse.of(
            requestDto.walletId(), earnId, requestDto.amount(), result.balance(), result.expirationDate());
    }

    /**
     * 포인트 적립 취소
     * 적립한 금액중 일부가 사용된 경우라면 적립 취소 될 수 없다
     */
    public PointEarnCancelResponse cancelEarn(PointEarnCancelRequest requestDto) {
        CancelEarnResult result = pointDomainService.cancelEarn(requestDto.walletId(), requestDto.earnId());
        return PointEarnCancelResponse.of(
            requestDto.walletId(), requestDto.earnId(), result.cancelledAmount());
    }

    /**
     * 포인트 사용 취소
     */
    public PointUsageCancelResponse cancelPoint(PointUsageCancelRequest requestDto) {
        UsageType cancelType = UsageType.valueOf(requestDto.cancelType());
        // 취소 ID 채번: 전체취소(CNC), 부분취소(PCN)
        String cancelUsageId = (cancelType == UsageType.PARTIAL_CANCEL)
            ? idGenerator.getPointPartialCancelId()
            : idGenerator.getPointCancelId();

        // FULL_CANCEL 시 금액 참조하지 않음 (도메인에서 자동 계산)
        long cancelAmountValue = requestDto.cancelAmount() != null ? requestDto.cancelAmount() : 0L;

        CancelPointResult result = pointDomainService.cancelPoint(
            requestDto.walletId(),
            cancelUsageId,
            requestDto.originalUsageId(),
            cancelType,
            cancelAmountValue
        );

        return PointUsageCancelResponse.of(
            requestDto.walletId(),
            cancelUsageId,
            requestDto.originalUsageId(),
            result.orderNo(),
            result.cancelledAmount(),
            result.remainingUsageAmount(),
            result.balance(),
            cancelType
        );
    }

    /**
     * 포인트 사용
     */
    public PointUsageResultResponse usePoint(PointUsageRequest requestDto) {
        //사용ID 채번
        String usageId = idGenerator.getPointUsageId();

        //포인트 사용 처리
        UsePointResult result = pointDomainService.usePoint(
            requestDto.walletId(), usageId, requestDto.orderNo(), requestDto.amount());

        List<UsageEarnDetail> usageEarnDetails = result.earnDetails().stream()
            .map(d -> UsageEarnDetail.of(
                d.earnType(), d.earnId(), d.originalAmount(),
                d.usedAmount(), d.remainingAmount(), d.earnStatus()))
            .toList();

        return PointUsageResultResponse.of(
            requestDto.walletId(), usageId, requestDto.orderNo(), requestDto.amount(),
            result.balance(), UsageType.USE, usageEarnDetails);
    }
}
