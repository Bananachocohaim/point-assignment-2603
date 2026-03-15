package io.github.bananachocohaim.pointassignment2603.domain.point.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UserPointWallet;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.WalletType;

@Repository
public interface UserPointWalletRepository extends JpaRepository<UserPointWallet, String> {

    //사용자 포인트지갑 조회
    @Query(
        "SELECT u FROM UserPointWallet u WHERE u.userId = :userId AND (:walletType IS NULL OR u.walletType = :walletType)"
    )
    List<UserPointWallet> findUserWallets(@Param("userId") String userId, @Param("walletType") WalletType walletType);
}
