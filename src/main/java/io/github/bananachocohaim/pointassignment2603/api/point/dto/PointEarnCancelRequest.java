package io.github.bananachocohaim.pointassignment2603.api.point.dto;

import jakarta.validation.constraints.NotBlank;

public record PointEarnCancelRequest(

    @NotBlank(message = "지갑 ID는 필수입니다.")
    String walletId,

    @NotBlank(message = "적립 ID는 필수입니다.")
    String earnId
) {}
