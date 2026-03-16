package io.github.bananachocohaim.pointassignment2603.domain.point.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_usage_record")
@Getter
@NoArgsConstructor
public class PointUsageRecord {

    @EmbeddedId
    private PointUsageRecordId id;

    private String orderNo;
    private long usedAmount;
    private String originalUsageId;

    @Enumerated(EnumType.STRING)
    private UsageType usageType;

    @Enumerated(EnumType.STRING)
    private UsageStatus usageStatus;
}
