package io.github.bananachocohaim.pointassignment2603.api.point.dto;

import java.time.LocalDate;
import java.util.List;

public record UserPointWalletInquiryResponse(
    String userId,  //사용자 ID
    Long totalBalance,      //총 잔액  
    List<WalletDetail> wallets
) { 
    //고객 포인트 지갑 상세
    public record WalletDetail (
        String walletType,      //지갑 타입 FREE, CASH (무료, 충전)
        long balance,           //잔액
        LocalDate nextExpirationDate,   //다음 포인트 만료일
        long expiringAmount             //만료 예정잔액
    ) {
        public static WalletDetail of(String uswalletTypeerId, Long balance, LocalDate nextExpirationDate, long expiringAmount) {
            return new WalletDetail(uswalletTypeerId, balance, nextExpirationDate, expiringAmount);
        }
    }

    public static UserPointWalletInquiryResponse of(String userId, Long totalBalance, List<WalletDetail> wallets) {
        return new UserPointWalletInquiryResponse(userId, totalBalance, wallets);
    }
}
