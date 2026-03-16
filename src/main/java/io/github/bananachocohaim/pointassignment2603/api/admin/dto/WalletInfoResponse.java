package io.github.bananachocohaim.pointassignment2603.api.admin.dto;

import java.time.LocalDate;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UserPointWallet;

public record WalletInfoResponse(
    String walletId,
    String userId,
    String walletType,
    long balance,
    long maxBalanceLimit,
    LocalDate nextExpirationDate,
    long expiringAmount
) {
    public static WalletInfoResponse from(UserPointWallet w) {
        return new WalletInfoResponse(
            w.getWalletId(), w.getUserId(), w.getWalletType().name(),
            w.getBalance(), w.getMax_balance_limit(),
            w.getNextExpirationDate(), w.getExpiringAmount()
        );
    }
}
