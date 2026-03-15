package io.github.bananachocohaim.pointassignment2603.api.point.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.bananachocohaim.pointassignment2603.api.point.dto.UserPointWalletInquiryRequest;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.UserPointWalletInquiryResponse;
import io.github.bananachocohaim.pointassignment2603.api.point.service.PointApiService;
import io.github.bananachocohaim.pointassignment2603.common.component.IdGenerator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequiredArgsConstructor
public class PointApiController {
    
    private final IdGenerator idGenerator;
    private final PointApiService pointApiService;

    @GetMapping("/api/point")
    public ResponseEntity<UserPointWalletInquiryResponse> getUserPointWallet(
        @Valid UserPointWalletInquiryRequest requestDto
    ) {
        log.info("PointApiInquiryRequest: userId={}, walletType={}", requestDto.userId(), requestDto.walletType());
        log.info(idGenerator.getPointEarnId());
        log.info(idGenerator.getPointUsageId());
        log.info(idGenerator.getPointWalletId());

        UserPointWalletInquiryResponse res = pointApiService.getUserPointWalletInfo(requestDto);
        log.info("UserPointWalletInquiryResponse: {}", res);

        return ResponseEntity.ok(res);
    }
}
