package io.github.bananachocohaim.pointassignment2603.domain.point.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_point_wallet")
@Getter
@NoArgsConstructor
public class UserPointWallet {
    
    @Id
    private String walletId;   //지갑 ID
    
    private String userId;              //사용자 ID

    @Enumerated(EnumType.STRING)
    private WalletType walletType; // 포인트지갑타입 FREE, CASH 무료 또는 고객충전 추후 확장성 고려
    
    private long balance;           //포인트 잔액

    private long max_balance_limit;     //최대 보유 한도 금액

    private LocalDate nextExpirationDate; //다음 만료일 

    private long expiringAmount;        //만료예정금액

    private LocalDateTime expirationUpdatedAt; //만료 정보 갱신 일시

    /** 만료로 인한 포인트 차감 (잔액에서 차감, 0 미만으로는 내려가지 않음) */
    public void deductBalance(long amount) {
        this.balance = Math.max(0L, this.balance - amount);
    }

    /** 적립 시 잔액 증가 */
    public void addBalance(long amount) {
        this.balance += amount;
    }

    /**
     * 다음 만료예정일·만료예정금액·갱신일시를 한 번에 갱신 (도메인 서비스에서 호출)
     */
    public void updateExpirationInfo(LocalDate nextExpirationDate, long expiringAmount, LocalDateTime expirationUpdatedAt) {
        this.nextExpirationDate = nextExpirationDate;
        this.expiringAmount = expiringAmount;
        this.expirationUpdatedAt = expirationUpdatedAt;
    }
}
