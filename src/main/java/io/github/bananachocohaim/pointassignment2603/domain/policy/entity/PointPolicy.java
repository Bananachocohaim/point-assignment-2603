package io.github.bananachocohaim.pointassignment2603.domain.policy.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "point_policy")
public class PointPolicy {

    @Id
    private String policyKey;

    private String policyValue;

    private String description;

    private String updatedBy;

    private LocalDateTime updatedAt;

    public void update(String policyValue, String updatedBy) {
        this.policyValue = policyValue;
        this.updatedBy = updatedBy;
        this.updatedAt = LocalDateTime.now();
    }
}
