package io.github.bananachocohaim.pointassignment2603.domain.point.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Entity
@Table(name = "userPointWallet")
@Getter
@AllArgsConstructor
public class UserPointWallet {
    
    @Id
    private String userPointWalletId;   //사용자지갑ID
    
    private String userId;              //사용자 ID

    @Enumerated(EnumType.STRING)
    private WalletType walletType; // 포인트지갑타입 FREE, CASH 무료 또는 고객충전 추후 확장성 고려
    
    private long balance;           //포인트 잔액

    private LocalDate nextExpirationDate; //다음 만료일 

    private long expiringAmount;        //만료예정금액

}
