package io.github.bananachocohaim.pointassignment2603.api.point.dto;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.WalletType;
import jakarta.validation.constraints.NotBlank;

public record UserPointWalletInquiryRequest(    

    @NotBlank(message = "userId는 필수 입니다.")
    String userId,
    
    WalletType walletType   //walletType은 'FREE' 또는 'CASH'만 가능
) {}
