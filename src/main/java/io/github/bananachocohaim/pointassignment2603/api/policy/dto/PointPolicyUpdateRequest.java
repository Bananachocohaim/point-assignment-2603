package io.github.bananachocohaim.pointassignment2603.api.policy.dto;

import jakarta.validation.constraints.NotBlank;

public record PointPolicyUpdateRequest(
    @NotBlank(message = "value는 필수입니다.")
    String value,

    String updatedBy
) {}
