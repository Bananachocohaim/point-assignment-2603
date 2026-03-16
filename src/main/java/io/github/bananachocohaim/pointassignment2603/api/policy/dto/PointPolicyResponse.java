package io.github.bananachocohaim.pointassignment2603.api.policy.dto;

import java.time.LocalDateTime;

import io.github.bananachocohaim.pointassignment2603.domain.policy.entity.PointPolicy;

public record PointPolicyResponse(
    String policyKey,
    String policyValue,
    String description,
    String updatedBy,
    LocalDateTime updatedAt
) {
    public static PointPolicyResponse from(PointPolicy policy) {
        return new PointPolicyResponse(
            policy.getPolicyKey(),
            policy.getPolicyValue(),
            policy.getDescription(),
            policy.getUpdatedBy(),
            policy.getUpdatedAt()
        );
    }
}
