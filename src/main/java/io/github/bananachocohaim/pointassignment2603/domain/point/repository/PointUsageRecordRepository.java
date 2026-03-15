package io.github.bananachocohaim.pointassignment2603.domain.point.repository;

import java.time.LocalDate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointUsageRecord;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointUsageRecordId;

public interface PointUsageRecordRepository extends JpaRepository<PointUsageRecord, PointUsageRecordId> {

    /**
     * 사용 이력 INSERT 전용.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        INSERT INTO point_usage_record
          (wallet_id, usage_id, created_date, order_no, usage_type, total_amount, original_usage_id, usage_status)
        VALUES
          (:walletId, :usageId, :createdDate, :orderNo, :usageType, :totalAmount, :originalUsageId, :usageStatus)
        """, nativeQuery = true)
    int insert(
        @Param("walletId") String walletId,
        @Param("usageId") String usageId,
        @Param("createdDate") LocalDate createdDate,
        @Param("orderNo") String orderNo,
        @Param("usageType") String usageType,
        @Param("totalAmount") long totalAmount,
        @Param("originalUsageId") String originalUsageId,
        @Param("usageStatus") String usageStatus
    );
}
