package io.github.bananachocohaim.pointassignment2603.domain.point.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UserPointWallet;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.WalletType;

@Repository
public interface UserPointWalletRepository extends JpaRepository<UserPointWallet, String> {

    //사용자 포인트지갑 조회
    @Query(value = """
        SELECT u 
        FROM UserPointWallet u 
        WHERE u.userId = :userId 
            AND (:walletType IS NULL OR u.walletType = :walletType)
        """)
    List<UserPointWallet> findUserWallets(@Param("userId") String userId, @Param("walletType") WalletType walletType);

    /**
     * 지갑 잔액만 네이티브 SQL로 처리 동시성 대응을 위해 balance = balance + :amount 로 합산.
     * @return 업데이트된 행 수 (1: 성공)
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE user_point_wallet
        SET balance = balance + :amount,
            updated_at = :updatedAt
        WHERE wallet_id = :walletId
        """, nativeQuery = true)
    int userPointWalletAddBalance(
        @Param("walletId") String walletId,
        @Param("amount") long amount,
        @Param("updatedAt") LocalDateTime updatedAt
    );

    /**
     * 지갑 잔액만 네이티브 SQL로 차감 (동시성·정합성 대응). 만료 차감·사용 처리 등에서 재사용.
     * WHERE balance >= :amount 로 잔액 부족 시 UPDATE 미실행(0건), 음수 잔액 방지.
     * @return 업데이트된 행 수 (1: 성공, 0: 잔액 부족 등으로 미적용)
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE user_point_wallet
        SET balance = balance - :amount
        WHERE wallet_id = :walletId
          AND balance >= :amount
        """, nativeQuery = true)
    int userPointWalletWithdrawBalance(
        @Param("walletId") String walletId,
        @Param("amount") long amount
    );

    /**
     * 만료예정일, 만료예정금액, 갱신일시만 갱신 (잔액 차감과 분리).
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE user_point_wallet
        SET next_expiration_date = :nextExpirationDate,
            expiring_amount = :expiringAmount,
            expiration_updated_at = :expirationUpdatedAt,
            updated_at = :updatedAt
        WHERE wallet_id = :walletId
        """, nativeQuery = true)
    int updateExpirationInfo(
        @Param("walletId") String walletId,
        @Param("nextExpirationDate") LocalDate nextExpirationDate,
        @Param("expiringAmount") long expiringAmount,
        @Param("expirationUpdatedAt") LocalDateTime expirationUpdatedAt,
        @Param("updatedAt") LocalDateTime updatedAt
    );
}
