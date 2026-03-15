package io.github.bananachocohaim.pointassignment2603.domain.point.service;

import java.util.List;

import org.springframework.stereotype.Service;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UserPointWallet;
import lombok.extern.slf4j.Slf4j;

/**
 * 포인트 도메인의 비즈니스 로직 서비스
 * 포인트 적립, 적립취소, 사용, 사용취소 로직을 처리
 */
@Slf4j
@Service
public class PointDomainService {

    /**
     * 고객의 총 포인트 잔액을 반환
     * @param walletList
     */
    public long getUserPointTotalBalance(List<UserPointWallet> walletList) {
        return walletList == null ? 0L : walletList.stream()
            .mapToLong(wallet -> wallet.getBalance())
            .sum();
    }
}
