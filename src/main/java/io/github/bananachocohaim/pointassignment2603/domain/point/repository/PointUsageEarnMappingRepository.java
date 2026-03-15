package io.github.bananachocohaim.pointassignment2603.domain.point.repository;

import java.time.LocalDate;

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

}
