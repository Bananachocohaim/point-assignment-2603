package io.github.bananachocohaim.pointassignment2603.domain.point.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointUsageEarnMapping;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointUsageEarnMappingId;

public interface PointUsageEarnMappingRepository extends JpaRepository<PointUsageEarnMapping, PointUsageEarnMappingId> {

    /**
     * 사용-적립 매핑 INSERT: 사용 건에서 어느 적립 건에서 얼마 차감했는지 기록.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        INSERT INTO point_usage_earn_mapping
          (wallet_id, usage_id, usage_created_date, earn_id, earn_created_date, amount, usage_type)
        VALUES
          (:walletId, :usageId, :usageCreatedDate, :earnId, :earnCreatedDate, :amount, :usageType)
        """, nativeQuery = true)
    void insert(
        @Param("walletId") String walletId,
        @Param("usageId") String usageId,
        @Param("usageCreatedDate") LocalDate usageCreatedDate,
        @Param("earnId") String earnId,
        @Param("earnCreatedDate") LocalDate earnCreatedDate,
        @Param("amount") long amount,
        @Param("usageType") String usageType
    );

    /**
     * 원거래(usageId)에 연결된 USE 타입 매핑 전체 조회 (취소 시 적립 건별 환급 대상 파악용).
     * 사용일(usageCreatedDate) 오름차순 정렬로 반환 (먼저 사용된 적립부터 환급 처리).
     */
    @Query("SELECT m FROM PointUsageEarnMapping m WHERE m.id.walletId = :walletId AND m.id.usageId = :usageId ORDER BY m.id.usageCreatedDate ASC")
    List<PointUsageEarnMapping> findUseMappingsByOriginalUsage(
        @Param("walletId") String walletId,
        @Param("usageId") String usageId
    );

    /**
     * 원거래에 대한 기존 취소 건들에서 적립 건별로 이미 환급된 금액을 일괄 조회.
     * - ACTIVE 건: 취소 매핑의 earn_id = 원 적립 earn_id (직접 환급)
     * - EXPIRED 건: 취소 매핑의 earn_id = RE_EARN earn_id -> original_earn_id로 역추적
     * 2차 이상 부분취소 시 루프 전 한 번만 조회해 Map으로 활용.
     */
    @Query(value = """
        SELECT
            CASE WHEN p.original_earn_id IS NOT NULL THEN p.original_earn_id
                 ELSE m.earn_id END AS earnId,
            COALESCE(SUM(m.amount), 0) AS totalRestored
        FROM point_usage_earn_mapping m
        INNER JOIN point_usage_record r
            ON m.wallet_id = r.wallet_id
            AND m.usage_id = r.usage_id
        LEFT JOIN point_earn_record p
            ON m.wallet_id = p.wallet_id
            AND m.earn_id = p.earn_id
            AND p.earn_type = 'RE_EARN'
        WHERE m.wallet_id = :walletId
          AND r.original_usage_id = :originalUsageId
        GROUP BY CASE WHEN p.original_earn_id IS NOT NULL THEN p.original_earn_id
                      ELSE m.earn_id END
        """, nativeQuery = true)
    List<EarnRestoreSummary> findAllRestoredAmountsByOriginalUsage(
        @Param("walletId") String walletId,
        @Param("originalUsageId") String originalUsageId
    );

    interface EarnRestoreSummary {
        String getEarnId();
        long getTotalRestored();
    }
}
