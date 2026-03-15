package io.github.bananachocohaim.pointassignment2603.api.point.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointEarnRequest;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointEarnResultResponse;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointUsageRequest;
import io.github.bananachocohaim.pointassignment2603.api.point.dto.PointUsageResultResponse;
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

    @PostMapping("/api/point/earn")
    public ResponseEntity<PointEarnResultResponse> earnPoint(@Valid @RequestBody PointEarnRequest requestDto) {
        log.info("PointEarnRequest: walletId={}, orderNo={}, amount={}", requestDto.walletId(), requestDto.orderNo(), requestDto.amount());

        PointEarnResultResponse res = pointApiService.earnPoint(requestDto);
        
        log.info("PointEarnResultResponse: {}", res);
        
        return ResponseEntity.ok(res);
    }

    @PostMapping("/api/point/use")
    public ResponseEntity<PointUsageResultResponse> usePoint(@Valid @RequestBody PointUsageRequest requestDto) {
        log.info("PointUsageRequest: walletId={}, orderNo={}, amount={}", requestDto.walletId(), requestDto.orderNo(), requestDto.amount());

        PointUsageResultResponse res = pointApiService.usePoint(requestDto);

        log.info("PointUsageResultResponse: {}", res);
        return ResponseEntity.ok(res);
    }
}
