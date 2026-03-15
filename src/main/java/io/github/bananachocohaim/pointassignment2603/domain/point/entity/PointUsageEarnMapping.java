package io.github.bananachocohaim.pointassignment2603.domain.point.entity;

import java.time.LocalDate;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "point_usage_earn_mapping")
@Getter
@NoArgsConstructor
public class PointUsageEarnMapping {

    @EmbeddedId
    private PointUsageEarnMappingId id;

    private LocalDate earnCreatedDate;
    private long amount;

    @Enumerated(EnumType.STRING)
    private UsageType usageType;
}
