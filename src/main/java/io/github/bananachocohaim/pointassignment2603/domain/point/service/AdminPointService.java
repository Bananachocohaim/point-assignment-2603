package io.github.bananachocohaim.pointassignment2603.domain.point.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.bananachocohaim.pointassignment2603.api.admin.dto.EarnHistoryItemResponse;
import io.github.bananachocohaim.pointassignment2603.api.admin.dto.UsageHistoryItemResponse;
import io.github.bananachocohaim.pointassignment2603.api.admin.dto.UsageHistoryItemResponse.EarnMappingDetail;
import io.github.bananachocohaim.pointassignment2603.api.admin.dto.UsageHistoryItemResponse.OriginalUsageInfo;
import io.github.bananachocohaim.pointassignment2603.api.admin.dto.WalletInfoResponse;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UsageType;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.WalletType;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.PointEarnRecordRepository;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.PointUsageEarnMappingRepository;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.PointUsageRecordRepository;
import io.github.bananachocohaim.pointassignment2603.domain.point.repository.UserPointWalletRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPointService {

    private final UserPointWalletRepository userPointWalletRepository;
    private final PointEarnRecordRepository pointEarnRecordRepository;
    private final PointUsageRecordRepository pointUsageRecordRepository;
    private final PointUsageEarnMappingRepository pointUsageEarnMappingRepository;

    /** 전체 지갑 목록 조회 (어드민 초기 화면용) */
    public List<WalletInfoResponse> getAllWallets() {
        return userPointWalletRepository.findAll()
            .stream()
            .map(WalletInfoResponse::from)
            .toList();
    }

    /** userId로 지갑 목록 조회 */
    public List<WalletInfoResponse> searchWallets(String userId) {
        return userPointWalletRepository.findUserWallets(userId, null)
            .stream()
            .map(WalletInfoResponse::from)
            .toList();
    }

    /** 지갑의 적립 이력 조회 (earnStatus 없으면 전체) */
    public List<EarnHistoryItemResponse> getEarnHistory(String walletId, String earnStatus) {
        String status = (earnStatus != null && !earnStatus.isBlank()) ? earnStatus : null;

        return pointEarnRecordRepository.findAllByWalletIdForAdmin(walletId, status)
            .stream()
            .map(EarnHistoryItemResponse::from)
            .toList();
    }

    /** 지갑의 사용/취소 이력 조회 (usageType 없으면 전체), 각 건의 적립 매핑 포함 */
    public List<UsageHistoryItemResponse> getUsageHistory(String walletId, String usageType) {
        UsageType type = (usageType != null && !usageType.isBlank())
            ? UsageType.valueOf(usageType) : null;

        return pointUsageRecordRepository.findAllByWalletIdForAdmin(walletId, type)
            .stream()
            .map(record -> {
                // 이 사용/취소 건에 매핑된 적립 건 목록 조회
                var rawMappings = pointUsageEarnMappingRepository
                    .findUseMappingsByOriginalUsage(walletId, record.getId().getUsageId());

                // 매핑된 earnId 목록으로 적립 원장 배치 조회 -> Map으로 변환
                List<String> earnIds = rawMappings.stream()
                    .map(m -> m.getId().getEarnId()).distinct().toList();

                Map<String, io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointEarnRecord> earnMap =
                    earnIds.isEmpty() ? Map.of() :
                    pointEarnRecordRepository.findByWalletIdAndEarnIdIn(walletId, earnIds)
                        .stream()
                        .collect(java.util.stream.Collectors.toMap(
                            e -> e.getId().getEarnId(), e -> e));

                List<EarnMappingDetail> mappings = rawMappings.stream()
                    .map(m -> {
                        var earn = earnMap.get(m.getId().getEarnId());
                        return new EarnMappingDetail(
                            m.getId().getEarnId(),
                            earn != null ? earn.getEarnType().name() : null,
                            earn != null ? earn.getEarnStatus().name() : null,
                            earn != null ? earn.getOriginalAmount() : 0L,
                            earn != null ? earn.getRemainingAmount() : 0L,
                            m.getAmount(),
                            m.getUsageType().name()
                        );
                    })
                    .toList();

                // 취소 건인 경우 원거래 요약 포함
                OriginalUsageInfo originalUsageInfo = null;
                if (record.getOriginalUsageId() != null) {
                    originalUsageInfo = pointUsageRecordRepository
                        .findByWalletIdAndUsageId(walletId, record.getOriginalUsageId())
                        .map(orig -> new OriginalUsageInfo(
                            orig.getId().getUsageId(),
                            orig.getId().getCreatedDate(),
                            orig.getUsageType().name(),
                            orig.getUsageStatus() != null ? orig.getUsageStatus().name() : null,
                            orig.getOrderNo(),
                            orig.getUsedAmount()
                        ))
                        .orElse(null);
                }

                return new UsageHistoryItemResponse(
                    record.getId().getUsageId(),
                    record.getId().getCreatedDate(),
                    record.getUsageType().name(),
                    record.getUsageStatus() != null ? record.getUsageStatus().name() : null,
                    record.getOrderNo(),
                    record.getUsedAmount(),
                    record.getOriginalUsageId(),
                    originalUsageInfo,
                    mappings
                );
            })
            .toList();
    }

    /** 지갑 walletType 목록 (HTML 드롭다운용) */
    public List<String> getWalletTypes() {
        return List.of(WalletType.FREE.name(), WalletType.CASH.name());
    }
}
