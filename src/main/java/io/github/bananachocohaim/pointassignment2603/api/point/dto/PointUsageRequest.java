package io.github.bananachocohaim.pointassignment2603.api.point.dto;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UsageType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PointUsageRequest(
    @NotBlank(message = "지갑 ID는 필수입니다.")
    String walletId,

    @NotBlank(message = "주문번호는 필수입니다.")
    String orderNo,

    @NotNull(message = "사용 금액은 필수입니다.")
    @Positive(message = "사용 금액은 양수여야 합니다.")
    @Min(value = 1, message = "사용 금액은 최소 1원 이상이어야 합니다.")
    Long amount,

    UsageType usageType
) {}
