package io.github.bananachocohaim.pointassignment2603.api.admin.dto;

public record WalletMaxBalanceUpdateResponse(
    String walletId,
    long balance,           // 현재 잔액
    long maxBalanceLimit    // 변경된 최대 보유 한도
) {}
