package io.github.bananachocohaim.pointassignment2603.api.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WalletMaxBalanceUpdateRequest(
    @NotNull(message = "maxBalanceLimit은 필수입니다.")
    @Min(value = 1, message = "maxBalanceLimit은 1 이상이어야 합니다.")
    Long maxBalanceLimit    // 변경할 최대 보유 한도 금액 (원)
) {}
