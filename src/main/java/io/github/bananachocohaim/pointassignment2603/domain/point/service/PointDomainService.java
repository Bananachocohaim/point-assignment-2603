package io.github.bananachocohaim.pointassignment2603.domain.point.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.bananachocohaim.pointassignment2603.common.error.ErrorCode;
import io.github.bananachocohaim.pointassignment2603.common.error.PointApiException;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.EarnStatus;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.EarnType;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointEarnRecord;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UsageStatus;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UsageType;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UserPointWallet;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.WalletType;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.PointEarnRecordRepository;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.PointUsageEarnMappingRepository;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.PointUsageRecordRepository;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.UserPointWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 포인트 도메인의 비즈니스 로직 서비스
 * 포인트 적립, 적립취소, 사용, 사용취소 로직을 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointDomainService {

    private final PointEarnRecordRepository pointEarnRecordRepository;
    private final PointUsageEarnMappingRepository pointUsageEarnMappingRepository;
    private final PointUsageRecordRepository pointUsageRecordRepository;
    private final UserPointWalletRepository userPointWalletRepository;

    // ────────────────────────────── 결과 record ──────────────────────────────

    /** 적립 처리 결과 */
    public record EarnPointResult(long balance, LocalDate expirationDate) {}

    /** 사용 처리 결과 */
    public record UsePointResult(long balance, List<EarnDetail> earnDetails) {
        public record EarnDetail(
            EarnType earnType,
            String earnId,
            long originalAmount,
            long usedAmount,
            long remainingAmount,
            EarnStatus earnStatus
        ) {}
    }

    // ────────────────────────────── 조회 ──────────────────────────────

    /**
     * 지갑 목록 조회, 적립 포인트 만료 처리, 예정 만료 정보 갱신
     * 만료 갱신이 처리 후 최신 상태를 반환하기 위해 DB를 재조회한다.
     */
    @Transactional
    public List<UserPointWallet> getWalletsWithExpirationRefreshed(String userId, WalletType walletType) {
        List<UserPointWallet> wallets = userPointWalletRepository.findUserWallets(userId, walletType);
        //만료 정보 동기화
        ensureExpirationInfoUpToDate(wallets);
        
        return userPointWalletRepository.findUserWallets(userId, walletType);
    }

    // ────────────────────────────── 적립 ──────────────────────────────

    /**
     * 포인트 적립 처리
     * 지갑 조회 → 만료 갱신 → 한도 체크 → 적립 원장 INSERT → 지갑 잔액 증가
     */
    @Transactional
    public EarnPointResult earnPoint(
        String walletId,
        String earnId,
        String orderNo,
        EarnType earnType,
        long amount,
        LocalDate expirationDate,
        String originalEarnId
    ) {
        // 1. 지갑 조회
        UserPointWallet wallet = userPointWalletRepository.findById(walletId)
            .orElseThrow(() -> new PointApiException(ErrorCode.WALLET_NOT_FOUND));

        // 2. 만료 갱신 (오늘 이전 만료건 차감 + 다음 만료 정보 갱신)
        ensureExpirationInfoUpToDate(List.of(wallet));

        // 3. 갱신 후 최신 지갑 재조회
        wallet = userPointWalletRepository.findById(walletId)
            .orElseThrow(() -> new PointApiException(ErrorCode.WALLET_NOT_FOUND));

        // 4. 보유 한도 체크
        if (wallet.getBalance() + amount > wallet.getMax_balance_limit()) {
            throw new PointApiException(ErrorCode.BALANCE_LIMIT_EXCEEDED);
        }

        // 5. 적립 원장 INSERT
        LocalDate createdDate = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        try {
            pointEarnRecordRepository.insert(
                walletId, earnId, createdDate, orderNo, expirationDate,
                amount, amount, earnType.name(), EarnStatus.ACTIVE.name(), originalEarnId);

        } catch (DataIntegrityViolationException e) {
            throw new PointApiException(ErrorCode.DUPLICATE_EARN_RECORD);
        }

        // 6. 지갑 잔액 증가
        userPointWalletRepository.userPointWalletAddBalance(walletId, amount, now);

        return new EarnPointResult(wallet.getBalance() + amount, expirationDate);
    }

    // ────────────────────────────── 사용 ──────────────────────────────

    /**
     * 포인트 사용 처리.
     * 지갑 조회 → 만료 갱신 → 잔액 체크 → 지갑 잔액 차감 → 적립 건별 차감 + 매핑 INSERT
     */
    @Transactional
    public UsePointResult usePoint(String walletId, String usageId, String orderNo, long amount) {
        // 1. 지갑 조회
        UserPointWallet wallet = userPointWalletRepository.findById(walletId)
            .orElseThrow(() -> new PointApiException(ErrorCode.WALLET_NOT_FOUND));

        // 2. 만료 갱신
        ensureExpirationInfoUpToDate(List.of(wallet));

        // 3. 갱신 후 최신 지갑 재조회
        wallet = userPointWalletRepository.findById(walletId)
            .orElseThrow(() -> new PointApiException(ErrorCode.WALLET_NOT_FOUND));

        // 4. 잔액 체크
        if (wallet.getBalance() < amount) {
            throw new PointApiException(ErrorCode.BALANCE_NOT_ENOUGH);
        }

        // 5. 지갑 잔액 차감
        int updCnt = userPointWalletRepository.userPointWalletWithdrawBalance(walletId, amount);
        if (updCnt == 0) {
            throw new PointApiException(ErrorCode.BALANCE_NOT_ENOUGH);
        }

        LocalDate createdDate = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 6. 차감 대상 적립 건 추출 (MANUAL 우선, 나머지는 만료일 짧은 순, 적립 포인트 전부가 아닌 사용에 필요한 건만 조회)
        List<PointEarnRecord> targetRecords = pointEarnRecordRepository.findUsePointTargetRecords(walletId, amount);
        long targetSum = targetRecords.stream().mapToLong(PointEarnRecord::getRemainingAmount).sum();

        if (amount > 0 && (targetRecords.isEmpty() || targetSum < amount)) {
            throw new PointApiException(ErrorCode.BALANCE_NOT_ENOUGH);
        }

        // 7. 적립 건별 잔액 차감 + 사용-적립 매핑 INSERT + 상세 수집
        List<UsePointResult.EarnDetail> earnDetails = new ArrayList<>();
        long amountToWithdraw = amount;
        for (PointEarnRecord r : targetRecords) {
            if (amountToWithdraw <= 0) break;

            long useAmount = Math.min(r.getRemainingAmount(), amountToWithdraw);
            if (useAmount <= 0) continue;

            int updated = pointEarnRecordRepository.pointEarnRecordWithdrawBalance(
                walletId, r.getId().getEarnId(), r.getId().getCreatedDate(), useAmount);
            if (updated == 0) {
                throw new PointApiException(ErrorCode.BALANCE_NOT_ENOUGH);
            }
            amountToWithdraw -= useAmount;

            pointUsageEarnMappingRepository.insert(
                walletId, usageId, createdDate,
                r.getId().getEarnId(), r.getId().getCreatedDate(),
                useAmount, UsageType.USE.name());

            long remainingAfter = r.getRemainingAmount() - useAmount;
            EarnStatus statusAfter = remainingAfter == 0 ? EarnStatus.EXPIRED : r.getEarnStatus();
            earnDetails.add(new UsePointResult.EarnDetail(
                r.getEarnType(), r.getId().getEarnId(), r.getOriginalAmount(),
                useAmount, remainingAfter, statusAfter));
        }

        // 8. 사용 이력 INSERT
        try {
            pointUsageRecordRepository.insert(
                walletId, usageId, createdDate, orderNo,
                UsageType.USE.name(), amount, null, UsageStatus.COMPLETED.name());
        } catch (DataIntegrityViolationException e) {
            throw new PointApiException(ErrorCode.DUPLICATE_USAGE_RECORD);
        }

        return new UsePointResult(wallet.getBalance() - amount, earnDetails);
    }

    /**
     * 적립 포인트 만료 처리, 예정 만료 정보 갱신
     */
    @Transactional
    void ensureExpirationInfoUpToDate(List<UserPointWallet> wallets) {
        if (wallets == null || wallets.isEmpty()) return;

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        for (UserPointWallet wallet : wallets) {
            if (!isRefreshExpirationInfo(wallet, today)) continue; //만료 정보 갱신 필요 여부 체크
            
            refreshExpirationInfo(wallet, today, now); //만료 정보 갱신 처리
        }
    }

    /**
     * 만료 정보 갱신 필요 여부 체크
     */
    private boolean isRefreshExpirationInfo(UserPointWallet wallet, LocalDate today) {
        if (wallet.getExpirationUpdatedAt() == null) return true;
       
        return wallet.getExpirationUpdatedAt().toLocalDate().isBefore(today);
    }

    /**
     * 만료 정보 갱신 처리
     */
    private void refreshExpirationInfo(UserPointWallet wallet, LocalDate today, LocalDateTime now) {
        String walletId = wallet.getWalletId();

        // 만료대상 적립 건 조회 
        List<PointEarnRecord> expiredRecords = pointEarnRecordRepository 
            .findById_WalletIdAndEarnStatusAndExpirationDateLessThanEqual(walletId, EarnStatus.ACTIVE, today);

        if (!expiredRecords.isEmpty()) { 
            //만료대상 적립 건이 있으면 잔액 차감 처리
            long totalExpiredAmount = expiredRecords.stream()
                .mapToLong(PointEarnRecord::getRemainingAmount).sum();
            
            int updCnt = userPointWalletRepository.userPointWalletWithdrawBalance(walletId, totalExpiredAmount);
            if (updCnt == 0) throw new PointApiException(ErrorCode.BALANCE_NOT_ENOUGH);

            //TODO 적립 포인트도 만료처리
        }

        // 다음 만료예정 정보 조회 및 갱신
        pointEarnRecordRepository
            .findNextExpirationSummary(walletId, EarnStatus.ACTIVE.name(), today)
            .ifPresentOrElse(
                summary -> {
                    userPointWalletRepository.updateExpirationInfo(
                        walletId, summary.getExpirationDate(), summary.getTotalAmount(), now, now);
                    wallet.updateExpirationInfo(summary.getExpirationDate(), summary.getTotalAmount(), now);
                },
                () -> {
                    userPointWalletRepository.updateExpirationInfo(walletId, null, 0L, now, now);
                    wallet.updateExpirationInfo(null, 0L, now);
                }
            );
    }
}
