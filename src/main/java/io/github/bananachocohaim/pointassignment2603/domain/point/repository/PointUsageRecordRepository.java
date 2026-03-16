package io.github.bananachocohaim.pointassignment2603.domain.point.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointUsageRecord;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointUsageRecordId;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UsageType;

public interface PointUsageRecordRepository extends JpaRepository<PointUsageRecord, PointUsageRecordId> {

    /**
     * 사용 이력 INSERT 전용.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        INSERT INTO point_usage_record
          (wallet_id, usage_id, created_date, order_no, usage_type, used_amount, original_usage_id, usage_status)
        VALUES
          (:walletId, :usageId, :createdDate, :orderNo, :usageType, :usedAmount, :originalUsageId, :usageStatus)
        """, nativeQuery = true)
    int insert(
        @Param("walletId") String walletId,
        @Param("usageId") String usageId,
        @Param("createdDate") LocalDate createdDate,
        @Param("orderNo") String orderNo,
        @Param("usageType") String usageType,
        @Param("usedAmount") long usedAmount,
        @Param("originalUsageId") String originalUsageId,
        @Param("usageStatus") String usageStatus
    );

    /**
     * walletId + usageId로 사용 이력 단건 조회 (취소 처리 시 원거래 조회용).
     */
    @Query("SELECT p FROM PointUsageRecord p WHERE p.id.walletId = :walletId AND p.id.usageId = :usageId")
    Optional<PointUsageRecord> findByWalletIdAndUsageId(
        @Param("walletId") String walletId,
        @Param("usageId") String usageId
    );

    /**
     * 원거래에 대해 이미 처리된 취소 금액 합계 조회 (부분 취소 후 추가 취소 가능 잔액 계산용).
     */
    @Query(value = """
        SELECT COALESCE(SUM(used_amount), 0)
        FROM point_usage_record
        WHERE wallet_id = :walletId
          AND original_usage_id = :originalUsageId
          AND usage_type IN ('CANCEL', 'PARTIAL_CANCEL')
        """, nativeQuery = true)
    long sumCancelledAmountByOriginalUsage(
        @Param("walletId") String walletId,
        @Param("originalUsageId") String originalUsageId
    );

    /**
     * 관리용 - 지갑의 전체 사용 이력 조회 (usageType 없으면 전체 반환)
     */
    @Query("SELECT p FROM PointUsageRecord p WHERE p.id.walletId = :walletId AND (:usageType IS NULL OR p.usageType = :usageType) ORDER BY p.id.createdDate DESC, p.id.usageId DESC")
    List<PointUsageRecord> findAllByWalletIdForAdmin(
        @Param("walletId") String walletId,
        @Param("usageType") UsageType usageType
    );

    /**
     * 사용 이력 상태 업데이트 (취소 후 원거래 상태 변경용).
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE point_usage_record
        SET usage_status = :usageStatus,
            updated_at = CURRENT_TIMESTAMP
        WHERE wallet_id = :walletId AND usage_id = :usageId
        """, nativeQuery = true)
    int updateUsageStatus(
        @Param("walletId") String walletId,
        @Param("usageId") String usageId,
        @Param("usageStatus") String usageStatus
    );
}
