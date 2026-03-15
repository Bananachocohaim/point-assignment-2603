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
public class PointUsageRecordId implements Serializable {

    private String walletId;
    private String usageId;
    private LocalDate createdDate;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointUsageRecordId that = (PointUsageRecordId) o;
        return Objects.equals(walletId, that.walletId)
            && Objects.equals(usageId, that.usageId)
            && Objects.equals(createdDate, that.createdDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(walletId, usageId, createdDate);
    }
}
