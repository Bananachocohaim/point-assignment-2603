package io.github.bananachocohaim.pointassignment2603.domain.point.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.bananachocohaim.pointassignment2603.common.error.ErrorCode;
import io.github.bananachocohaim.pointassignment2603.common.error.PointApiException;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.EarnStatus;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.EarnType;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointEarnRecord;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointUsageEarnMapping;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointUsageRecord;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UsageStatus;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UsageType;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UserPointWallet;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.WalletType;
import io.github.bananachocohaim.pointassignment2603.common.component.IdGenerator;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointEarnRecordId;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.PointEarnRecordRepository;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.PointUsageEarnMappingRepository;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.PointUsageRecordRepository;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.UserPointWalletRepository;
import io.github.bananachocohaim.pointassignment2603.domain.policy.entity.PolicyKey;
import io.github.bananachocohaim.pointassignment2603.domain.policy.service.PointPolicyService;
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
    private final IdGenerator idGenerator;
    private final PointPolicyService pointPolicyService;

    // ────────────────────────────── 결과 record ──────────────────────────────

    /** 적립 처리 결과 */
    public record EarnPointResult(long balance, LocalDate expirationDate) {}

    /** 지갑 보유 한도 변경 결과 */
    public record UpdateMaxBalanceLimitResult(String walletId, long balance, long maxBalanceLimit) {}

    /** 적립 취소 처리 결과 */
    public record CancelEarnResult(long cancelledAmount) {}

    /** 취소 처리 결과 */
    public record CancelPointResult(long balance, long cancelledAmount, long remainingUsageAmount, String orderNo) {}

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
     * 지갑 조회 -> 만료 갱신 -> 한도 체크 -> 적립 원장 INSERT -> 지갑 잔액 증가
     */
    @Transactional
    public EarnPointResult earnPoint(
        String walletId,
        String earnId,
        String orderNo,
        EarnType earnType,
        long amount,
        int expiryDays,
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

        // 4. 1회 적립 금액 상한 체크 (정책)
        long maxEarnPerTx = pointPolicyService.getLongValue(PolicyKey.MAX_EARN_AMOUNT_PER_TX);
        if (amount > maxEarnPerTx) {
            throw new PointApiException(ErrorCode.EARN_AMOUNT_EXCEEDED);
        }

        // 5. 보유 한도 체크 (지갑 DB의 max_balance_limit 기준)
        if (wallet.getBalance() + amount > wallet.getMax_balance_limit()) {
            throw new PointApiException(ErrorCode.BALANCE_LIMIT_EXCEEDED);
        }

        // 6. 적립 원장 INSERT
        LocalDate createdDate = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        try {
            pointEarnRecordRepository.insert(
                walletId, earnId, createdDate, orderNo,
                amount, amount, expiryDays, expirationDate,
                earnType.name(), EarnStatus.ACTIVE.name(), originalEarnId);

        } catch (DataIntegrityViolationException e) {
            throw new PointApiException(ErrorCode.DUPLICATE_EARN_RECORD);
        }

        // 7. 지갑 잔액 증가
        userPointWalletRepository.userPointWalletAddBalance(walletId, amount, now);

        return new EarnPointResult(wallet.getBalance() + amount, expirationDate);
    }

    // ────────────────────────────── 사용 ──────────────────────────────

    /**
     * 포인트 사용 처리.
     * 지갑 조회 -> 만료 갱신 -> 잔액 체크 -> 지갑 잔액 차감 -> 적립 건별 차감 + 매핑 INSERT
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
            log.info("PointEarnRecord: {}", r);
            log.info(" amountToWithdraw : " + amountToWithdraw );

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
            earnDetails.add(new UsePointResult.EarnDetail(
                r.getEarnType(), r.getId().getEarnId(), r.getOriginalAmount(),
                useAmount, remainingAfter, r.getEarnStatus()));
        }

        // 8. 사용 이력 INSERT
        try {
            pointUsageRecordRepository.insert(
                walletId, usageId, createdDate, orderNo,
                UsageType.USE.name(), amount, null, UsageStatus.COMPLETED.name()); // 사용 시점엔 COMPLETED, 이후 취소되면 PARTIAL_CANCEL/FULL_CANCEL로 업데이트
        } catch (DataIntegrityViolationException e) {
            throw new PointApiException(ErrorCode.DUPLICATE_USAGE_RECORD);
        }

        return new UsePointResult(wallet.getBalance() - amount, earnDetails);
    }

    // ────────────────────────────── 적립 취소 ──────────────────────────────

    /**
     * 포인트 적립 취소 처리.
     * 적립 건 조회 -> 취소 가능 여부 검증 -> 적립 건 CANCELLED 처리 -> 지갑 잔액 차감
     * 조건: 적립 금액 전체가 미사용 상태인 경우만 취소 가능.
     */
    @Transactional
    public CancelEarnResult cancelEarn(String walletId, String earnId) {

        log.info("cancelEarn walletId: {}, earnId: {}", walletId, earnId);

        // 지갑 존재하는지 검증 (존재하지 않으면 예외)
        UserPointWallet wallet = userPointWalletRepository.findById(walletId)
            .orElseThrow(() -> new PointApiException(ErrorCode.WALLET_NOT_FOUND));

        // 만료 정보 동기화
        ensureExpirationInfoUpToDate(List.of(wallet));

        // 1. 적립 건 조회
        PointEarnRecord earnRecord = pointEarnRecordRepository.findByWalletIdAndEarnId(walletId, earnId)
            .orElseThrow(() -> new PointApiException(ErrorCode.EARN_NOT_FOUND));

        // 2. 이미 취소된 건 검증
        if (earnRecord.getEarnStatus() == EarnStatus.CANCELLED) {
            throw new PointApiException(ErrorCode.EARN_ALREADY_CANCELLED);
        }

        // 3. 만료 건 검증 (만료된 건은 잔액이 0이므로 취소 대상 없음)
        if (earnRecord.getEarnStatus() == EarnStatus.EXPIRED) {
            throw new PointApiException(ErrorCode.EARN_PARTIALLY_USED);
        }

        // 4. 일부라도 사용된 경우 취소 불가 (remaining < original)
        if (earnRecord.getRemainingAmount() < earnRecord.getOriginalAmount()) {
            throw new PointApiException(ErrorCode.EARN_PARTIALLY_USED);
        }

        // 5. 적립 건 취소 처리 (earn_status = CANCELLED, remaining_amount = 0)
        int updated = pointEarnRecordRepository.cancelEarnRecord(
            walletId, earnId, earnRecord.getId().getCreatedDate());
        if (updated == 0) {
            throw new PointApiException(ErrorCode.EARN_PARTIALLY_USED);
        }

        // 6. 지갑 잔액 차감
        int walletUpdated = userPointWalletRepository.userPointWalletWithdrawBalance(
            walletId, earnRecord.getOriginalAmount());

        if (walletUpdated == 0) throw new PointApiException(ErrorCode.BALANCE_NOT_ENOUGH);

        return new CancelEarnResult(earnRecord.getOriginalAmount());
    }

    // ────────────────────────────── 사용 취소 ──────────────────────────────

    /**
     * 포인트 사용 취소 처리.
     * 원거래 조회 -> 취소 가능 여부 검증 -> 적립 건별 잔액 환급
     * -> 취소-적립 매핑 INSERT -> 취소 이력 INSERT -> 원거래 상태 업데이트 -> 지갑 잔액 환급
     */
    @Transactional
    public CancelPointResult cancelPoint(
        String walletId,
        String cancelUsageId,
        String originalUsageId,
        UsageType cancelType,
        long cancelAmount
    ) {
        LocalDate createdDate = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        // 1. 원거래 이력 조회 
        PointUsageRecord original = pointUsageRecordRepository
            .findByWalletIdAndUsageId(walletId, originalUsageId)
            .orElseThrow(() -> new PointApiException(ErrorCode.USAGE_NOT_FOUND));

        // 2. 취소 가능 여부 검증

        // A. 원거래가 전체 취소 상태라면 취소 불가
        if (original.getUsageStatus() == UsageStatus.FULL_CANCEL) {
            throw new PointApiException(ErrorCode.USAGE_ALREADY_CANCELLED);
        }

        // B. 원거래가 부분 취소 상태인데 전체 취소가 요청되면 취소 불가
        if (original.getUsageStatus() == UsageStatus.PARTIAL_CANCEL
                && cancelType == UsageType.FULL_CANCEL) {
            throw new PointApiException(ErrorCode.FULL_CANCEL_NOT_ALLOWED);
        }

        long alreadyCancelled = 0L;
        long actualCancelAmount;

        if (cancelType == UsageType.FULL_CANCEL) {
            // FULL_CANCEL: 전달된 금액을 무시하고 잔여 전체 금액으로 자동 계산
            alreadyCancelled = pointUsageRecordRepository
                .sumCancelledAmountByOriginalUsage(walletId, originalUsageId);
            actualCancelAmount = original.getUsedAmount() - alreadyCancelled;
        } else {
            // PARTIAL_CANCEL: 금액 필수 & 양수 검증 (null은 API 레이어에서 0으로 전달됨)
            if (cancelAmount <= 0) {
                throw new PointApiException(ErrorCode.CANCEL_AMOUNT_REQUIRED);
            }
            if (cancelAmount > original.getUsedAmount()) {
                throw new PointApiException(ErrorCode.CANCEL_AMOUNT_EXCEEDED);
            }
            if (original.getUsageStatus() == UsageStatus.PARTIAL_CANCEL) {
                alreadyCancelled = pointUsageRecordRepository
                    .sumCancelledAmountByOriginalUsage(walletId, originalUsageId);
                long cancellableAmount = original.getUsedAmount() - alreadyCancelled;
                if (cancelAmount > cancellableAmount) {
                    throw new PointApiException(ErrorCode.CANCEL_AMOUNT_EXCEEDED);
                }
            }
            actualCancelAmount = cancelAmount;
        }

        // 3. 원거래에 연결된 적립 건 매핑 조회
        List<PointUsageEarnMapping> mappings = pointUsageEarnMappingRepository.findUseMappingsByOriginalUsage(walletId, originalUsageId);

        // 4. 적립 건별 잔액 환급 + 취소-적립 매핑 INSERT

        // 이전 부분취소에서 적립 건별 이미 환급된 금액을 한 번에 조회 (N+1 방지)
        Map<String, Long> alreadyRestoredMap = pointUsageEarnMappingRepository
            .findAllRestoredAmountsByOriginalUsage(walletId, originalUsageId)
            .stream()
            .collect(Collectors.toMap(
                s -> s.getEarnId(),
                s -> s.getTotalRestored()
            ));

        long amountToRestore = actualCancelAmount;

        for (PointUsageEarnMapping mapping : mappings) {
            if (amountToRestore <= 0) break;

            // 이미 이 적립 건(earnId)에서 환급(restore) 처리된 금액을 가져온다. (이전 취소들에서 환급된 누적 금액)
            long alreadyRestored = alreadyRestoredMap.getOrDefault(mapping.getId().getEarnId(), 0L);

            // - 현재 매핑에서 환급 가능한 최대 금액을 계산한다.
            //   (이 매핑에 남아있는 금액 = 사용-적립 매핑의 원래 amount - 이미 환급된 금액)
            long maxRestorableFromThisEarn = mapping.getAmount() - alreadyRestored;     //최대 환급 가능잔액 현재 적립에서

            // - 실제로 이번에 환급할 금액은
            //   (이 적립건에서 최대 환급 가능 금액)과 (취소를 통해 환급해야할 남은 전체 금액) 중 작은 값이다.
            long restoreAmount = Math.min(maxRestorableFromThisEarn, amountToRestore);

            log.debug("[walletId:{}, usageId:{}, earnId:{}], 이미 환급된 금액:{}, 매핑금액:{}",
                mapping.getId().getWalletId(), mapping.getId().getUsageId(), mapping.getId().getEarnId(), alreadyRestored, mapping.getAmount()
            );
            // - 환급할 금액이 0 이하이면(이미 다 환급된 적립건이면) 다음 매핑으로 skip
            if (restoreAmount <= 0) {
                continue;
            }

            //적립 이력 조회
            PointEarnRecord earnRecord = pointEarnRecordRepository
                .findById(new PointEarnRecordId(walletId, mapping.getId().getEarnId(), mapping.getEarnCreatedDate()))
                .orElseThrow(() -> new PointApiException(ErrorCode.EARN_NOT_FOUND));

            String targetEarnId;
            LocalDate targetEarnCreatedDate;

            if (earnRecord.getEarnStatus() == EarnStatus.EXPIRED || !earnRecord.getExpirationDate().isAfter(createdDate)) {
                // 만료된 건은 RE_EARN 타입으로 신규 적립 (원 적립 건의 expiryDays 기준으로 만료일 재산정)
                targetEarnId = idGenerator.getPointEarnId();    //적립 ID 채번
                targetEarnCreatedDate = createdDate;
                
                int reEarnExpiryDays = earnRecord.getExpiryDays();
                
                LocalDate reEarnExpirationDate = createdDate.plusDays(reEarnExpiryDays); //만료일 재산정
                
                //적립
                pointEarnRecordRepository.insert(
                    walletId, targetEarnId, targetEarnCreatedDate, original.getOrderNo(),
                    restoreAmount, restoreAmount, reEarnExpiryDays, reEarnExpirationDate,
                    EarnType.RE_EARN.name(), EarnStatus.ACTIVE.name(), mapping.getId().getEarnId());
            } else {
                // ACTIVE 건은 잔액 환급
                targetEarnId = mapping.getId().getEarnId();
                targetEarnCreatedDate = mapping.getEarnCreatedDate();
                
                int updatedRows = pointEarnRecordRepository.pointEarnRecordRestoreBalance(
                    walletId, targetEarnId, targetEarnCreatedDate, restoreAmount);
                
                    if (updatedRows == 0) {
                    throw new PointApiException(ErrorCode.EARN_RECORD_RESTORE_FAILED);
                }
            }

            //매핑 이력 insert
            pointUsageEarnMappingRepository.insert(
                walletId, cancelUsageId, createdDate,
                targetEarnId, targetEarnCreatedDate,
                restoreAmount, cancelType.name());

            amountToRestore -= restoreAmount;
        }

        // 5. 취소 이력 INSERT 취소여부에 따라 전체취소 또는 부분취소 이력 저장
        try {
            pointUsageRecordRepository.insert(
                walletId, cancelUsageId, createdDate, original.getOrderNo(),
                cancelType.name(), actualCancelAmount, originalUsageId, UsageStatus.COMPLETED.name());
        } catch (DataIntegrityViolationException e) {
            throw new PointApiException(ErrorCode.DUPLICATE_USAGE_RECORD);
        }

        // 6. 원거래 상태 업데이트 (요청받은 cancelType 기준)
        UsageStatus newStatus = cancelType == UsageType.FULL_CANCEL
            ? UsageStatus.FULL_CANCEL
            : UsageStatus.PARTIAL_CANCEL;
        pointUsageRecordRepository.updateUsageStatus(walletId, originalUsageId, newStatus.name());

        // 7. 포인트 지갑 잔액 환급
        userPointWalletRepository.userPointWalletAddBalance(walletId, actualCancelAmount, now);

        // 8. 고객 포인트 재조회
        UserPointWallet wallet = userPointWalletRepository.findById(walletId)
            .orElseThrow(() -> new PointApiException(ErrorCode.WALLET_NOT_FOUND));

        long remainingUsageAmount = original.getUsedAmount() - (alreadyCancelled + actualCancelAmount);
        return new CancelPointResult(wallet.getBalance(), actualCancelAmount, remainingUsageAmount, original.getOrderNo());
    }

    /**
     * 적립 포인트 만료 처리, 예정 만료 정보 갱신
     */
    @Transactional
    public void ensureExpirationInfoUpToDate(List<UserPointWallet> wallets) {
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

            // 만료 대상 적립 건 상태를 EXPIRED, 잔액을 0으로 일괄 변경
            pointEarnRecordRepository.expireEarnRecords(walletId, today);
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

    // ────────────────────────────── 관리 ──────────────────────────────

    /**
     * 지갑별 최대 보유 한도 변경.
     * FREE 타입 지갑만 변경 가능.
     * 변경 후 현재 잔액이 새 한도를 초과해도 강제 적용 (기존 잔액은 유지)
     */
    @Transactional
    public UpdateMaxBalanceLimitResult updateMaxBalanceLimit(String walletId, long maxBalanceLimit) {
        UserPointWallet wallet = userPointWalletRepository.findById(walletId)
            .orElseThrow(() -> new PointApiException(ErrorCode.WALLET_NOT_FOUND));

        if (wallet.getWalletType() != WalletType.FREE) {
            throw new PointApiException(ErrorCode.WALLET_LIMIT_UPDATE_NOT_ALLOWED);
        }

        int updated = userPointWalletRepository.updateMaxBalanceLimit(walletId, maxBalanceLimit);
        if (updated == 0) throw new PointApiException(ErrorCode.WALLET_NOT_FOUND);

        log.info("지갑 최대 보유 한도 변경: walletId={}, {} -> {}", walletId, wallet.getMax_balance_limit(), maxBalanceLimit);
        return new UpdateMaxBalanceLimitResult(walletId, wallet.getBalance(), maxBalanceLimit);
    }
}
