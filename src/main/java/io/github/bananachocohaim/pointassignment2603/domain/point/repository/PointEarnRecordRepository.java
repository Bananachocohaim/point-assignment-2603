package io.github.bananachocohaim.pointassignment2603.domain.point.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.EarnStatus;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointEarnRecord;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointEarnRecordId;

public interface PointEarnRecordRepository extends JpaRepository<PointEarnRecord, PointEarnRecordId> {

    /**
     * 적립 원장 INSERT 전용. 중복키 시 DB 제약 위반 → DataIntegrityViolationException.
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        INSERT INTO point_earn_record
          (wallet_id, earn_id, created_date, order_no, expiration_date, original_amount, remaining_amount, earn_type, earn_status, original_earn_id)
        VALUES
          (:walletId, :earnId, :createdDate, :orderNo, :expirationDate, :originalAmount, :remainingAmount, :earnType, :earnStatus, :originalEarnId)
        """, nativeQuery = true)
    void insert(
        @Param("walletId") String walletId,
        @Param("earnId") String earnId,
        @Param("createdDate") LocalDate createdDate,
        @Param("orderNo") String orderNo,
        @Param("expirationDate") LocalDate expirationDate,
        @Param("originalAmount") long originalAmount,
        @Param("remainingAmount") long remainingAmount,
        @Param("earnType") String earnType,
        @Param("earnStatus") String earnStatus,
        @Param("originalEarnId") String originalEarnId
    );

    /**
     * 차감에 사용할 적립 건만 조회. MANUAL 우선, 나머지는 만료일 짧은 순.
     * 사용 금액(amount)을 채우는 최소 건수만 반환 (윈도우 함수로 DB에서 제한).
     */
    @Query(value = """
        WITH use_point_target_records AS (
            SELECT 
                earn_id,
                created_date,
                -- 내 앞 행(1 PRECEDING)까지의 잔액 누적 합계 계산
                COALESCE(
                    SUM(remaining_amount) OVER (
                        ORDER BY 
                            -- 1 MANUAL 타입 우선 사용 (0: MANUAL, 1: 그 외)
                            CASE WHEN earn_type = 'MANUAL' THEN 0 ELSE 1 END ASC,
                            -- 2 만료일 짧은 순
                            expiration_date ASC, 
                            -- 3 가장 예전 적립일 순
                            created_date ASC 
                        ROWS BETWEEN UNBOUNDED PRECEDING AND 1 PRECEDING
                    ), 0
                ) as prev_cumulative_sum
            FROM point_earn_record
            WHERE wallet_id = :walletId 
              AND remaining_amount > 0
              AND earn_status = 'ACTIVE'
              AND expiration_date > CURRENT_DATE -- 만료일이 오늘보다 큰 경우
        )
        -- 위에서 만든 가상 테이블에서 이전 누적 합계가 필요 금액보다 작은 행들의 PK를 뽑아냄
        SELECT * FROM point_earn_record 
        WHERE wallet_id = :walletId
          AND (earn_id, created_date) IN (
              SELECT earn_id, created_date 
              FROM use_point_target_records 
              WHERE prev_cumulative_sum < :amount
          )
        FOR UPDATE
         -- 정합성 유지를 위해 락
    """, nativeQuery = true)
    List<PointEarnRecord> findUsePointTargetRecords(
        @Param("walletId") String walletId,
        @Param("amount") long amount
    );

    /**
     * 적립 건별 remaining_amount 차감 (사용 금액만큼 FIFO 차감 시 로우별 호출).
     * remaining_amount >= :deductAmount 인 경우에만 차감 (0이면 미적용).
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE point_earn_record
        SET remaining_amount = remaining_amount - :useAmount,
            earn_status = CASE WHEN (remaining_amount - :useAmount) = 0 THEN 'EXPIRED' ELSE earn_status END
        WHERE wallet_id = :walletId
          AND earn_id = :earnId
          AND created_date = :createdDate
          AND remaining_amount >= :useAmount
        """, nativeQuery = true)
    int pointEarnRecordWithdrawBalance(
        @Param("walletId") String walletId,
        @Param("earnId") String earnId,
        @Param("createdDate") LocalDate createdDate,
        @Param("useAmount") long useAmount
    );

    /**
     * 이미 만료된 ACTIVE 적립 건 조회 (잔액 차감,만료 처리용)
     */
    List<PointEarnRecord> findById_WalletIdAndEarnStatusAndExpirationDateLessThanEqual(
        String walletId,
        EarnStatus earnStatus,
        LocalDate expirationDate
    );

    /**
     * 해당 포인트 지갑의 "다음 만료예정일" 1건만 조회 (가장 빠른 만료일 , 그 날짜의 만료예정 금액 합계).
     * 실제 SQL: SELECT ... GROUP BY expiration_date ORDER BY ... LIMIT 1
     */
    @Query(value = """
        SELECT expiration_date AS expirationDate, SUM(remaining_amount) AS totalAmount
        FROM point_earn_record
        WHERE wallet_id = :walletId
          AND earn_status = :earnStatus
          AND expiration_date >= :fromDate
        GROUP BY expiration_date
        ORDER BY expiration_date ASC
        LIMIT 1
        """, nativeQuery = true)
    Optional<NextExpirationSummary> findNextExpirationSummary(
        @Param("walletId") String walletId,
        @Param("earnStatus") String earnStatus,
        @Param("fromDate") LocalDate fromDate
    );

    /** 다음 만료예정일 조회 결과 */
    interface NextExpirationSummary {
        LocalDate getExpirationDate();
        long getTotalAmount();
    }
}
