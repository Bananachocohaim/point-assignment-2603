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
public class PointUsageEarnMappingId implements Serializable {

    private String walletId;
    private String usageId;
    private LocalDate usageCreatedDate;
    private String earnId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointUsageEarnMappingId that = (PointUsageEarnMappingId) o;
        return Objects.equals(walletId, that.walletId)
            && Objects.equals(usageId, that.usageId)
            && Objects.equals(usageCreatedDate, that.usageCreatedDate)
            && Objects.equals(earnId, that.earnId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(walletId, usageId, usageCreatedDate, earnId);
    }
}
