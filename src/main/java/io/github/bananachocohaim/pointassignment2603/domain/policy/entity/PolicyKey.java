package io.github.bananachocohaim.pointassignment2603.domain.policy.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PolicyKey {

    MAX_EARN_AMOUNT_PER_TX("1회 최대 적립 가능 금액 (원)");

    private final String description;
}
