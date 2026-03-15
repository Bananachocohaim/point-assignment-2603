package io.github.bananachocohaim.pointassignment2603.api.point.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;

import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointEarnRequest;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointEarnResultResponse;
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
            requestDto.amount(), expirationDate, requestDto.originalEarnId());

        return PointEarnResultResponse.of(
            requestDto.walletId(), earnId, requestDto.amount(), result.balance(), result.expirationDate());
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
