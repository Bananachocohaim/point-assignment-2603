package io.github.bananachocohaim.pointassignment2603.domain.point.entity;

import java.time.LocalDate;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_earn_record")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointEarnRecord {

    @EmbeddedId
    private PointEarnRecordId id;

    private String orderNo;
    private long originalAmount;
    private long remainingAmount;
    private int expiryDays;
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    private EarnType earnType;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private EarnStatus earnStatus = EarnStatus.ACTIVE;

    private String originalEarnId;

    /** 만료 처리: 상태를 EXPIRED로, 사용가능 잔액을 0으로 변경 */
    public void expire() {
        this.earnStatus = EarnStatus.EXPIRED;
        this.remainingAmount = 0L;
    }
}
