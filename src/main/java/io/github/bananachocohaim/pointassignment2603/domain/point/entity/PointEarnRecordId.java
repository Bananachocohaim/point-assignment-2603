package io.github.bananachocohaim.pointassignment2603.domain.point.entity;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PointEarnRecordId implements Serializable {

    private String walletId;
    private String earnId;
    private LocalDate createdDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointEarnRecordId that = (PointEarnRecordId) o;
        return Objects.equals(walletId, that.walletId)
            && Objects.equals(earnId, that.earnId)
            && Objects.equals(createdDate, that.createdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(walletId, earnId, createdDate);
    }
}
