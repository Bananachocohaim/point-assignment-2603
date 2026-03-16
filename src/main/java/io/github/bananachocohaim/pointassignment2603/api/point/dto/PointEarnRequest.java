package io.github.bananachocohaim.pointassignment2603.api.point.dto;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.EarnType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PointEarnRequest(
    @NotBlank(message = "walletId 필수 입니다.")
    String walletId,        //고객 지갑 ID
    
    @NotBlank(message = "연계주문번호 는 필수 입니다. 추적용")
    String orderNo,         //연계주문번호

    @NotNull(message = "earnType는 필수 입니다.")
    EarnType earnType,        //적립 유형 MANUAL, ORDER, EVENT, RE_EARN (관리자 수기적립, 주문으로 인한 적립, 이벤트 적립, 재적립 취소 후 적립 등)
    
    @NotNull(message = "amount는 필수 입니다.")
    @Positive(message = "amount는 양수여야 합니다.")
    @Min(value = 1, message = "amount는 최소 1원 이상이어야 합니다.")
    Long amount,            //적립 금액 (최대 금액은 정책 테이블 MAX_EARN_AMOUNT_PER_TX 기준)

    @Min(value = 1, message = "expiryDays는 최소 1일 이상이어야 합니다.")
    @Max(value = 1825, message = "expiryDays는 최대 5년(1825일) 이하여야 합니다.")
    Integer expiryDays, // 기본 365일로 설정
    String originalEarnId   //만료 후 재적립 시 원본 적립 ID 참조
) {}
