package io.github.bananachocohaim.pointassignment2603.api.point.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import io.github.bananachocohaim.pointassignment2603.api.point.dto.UserPointWalletInquiryRequest;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.UserPointWalletInquiryResponse;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.UserPointWalletInquiryResponse.WalletDetail;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UserPointWallet;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.UserPointWalletRepository;
import io.github.bananachocohaim.pointassignment2603.domain.point.service.PointDomainService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PointApiService {

    private final UserPointWalletRepository userPointWalletRepository;
    private final PointDomainService pointDomainService;

    /**
     * 고객의 포인트 정보를 조회
     * @param requestDto
     * @return
     */
    public UserPointWalletInquiryResponse getUserPointWalletInfo(UserPointWalletInquiryRequest requestDto) {

        //고객 
        List<UserPointWallet> walletList = userPointWalletRepository.findUserWallets(requestDto.userId(), requestDto.walletType());
        long totalBalance = pointDomainService.getUserPointTotalBalance(walletList);

        List<WalletDetail> wallets = walletList.stream().map(x -> UserPointWalletInquiryResponse.WalletDetail.of(
            x.getWalletType().name(), x.getBalance(), x.getNextExpirationDate(), x.getExpiringAmount())).collect(Collectors.toList());

        return UserPointWalletInquiryResponse.of(requestDto.userId(), totalBalance, wallets);
    }
}
