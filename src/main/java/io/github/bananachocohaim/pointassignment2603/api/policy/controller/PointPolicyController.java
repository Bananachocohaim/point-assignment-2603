package io.github.bananachocohaim.pointassignment2603.api.policy.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.bananachocohaim.pointassignment2603.api.policy.dto.PointPolicyResponse;
import io.github.bananachocohaim.pointassignment2603.api.policy.dto.PointPolicyUpdateRequest;
import io.github.bananachocohaim.pointassignment2603.common.error.ErrorCode;
import io.github.bananachocohaim.pointassignment2603.common.error.PointApiException;
import io.github.bananachocohaim.pointassignment2603.domain.policy.entity.PointPolicy;
import io.github.bananachocohaim.pointassignment2603.domain.policy.entity.PolicyKey;
import io.github.bananachocohaim.pointassignment2603.domain.policy.service.PointPolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/admin/policy")
@RequiredArgsConstructor
public class PointPolicyController {

    private final PointPolicyService pointPolicyService;

    /** 전체 정책 조회 */
    @GetMapping
    public ResponseEntity<List<PointPolicyResponse>> getAllPolicies() {
        List<PointPolicyResponse> response = pointPolicyService.findAll()
            .stream()
            .map(PointPolicyResponse::from)
            .toList();
        return ResponseEntity.ok(response);
    }

    /** 단건 정책 수정 */
    @PutMapping("/{key}")
    public ResponseEntity<PointPolicyResponse> updatePolicy(
        @PathVariable String key,
        @Valid @RequestBody PointPolicyUpdateRequest requestDto
    ) {
        PolicyKey policyKey;
        try {
            policyKey = PolicyKey.valueOf(key);
        } catch (IllegalArgumentException e) {
            throw new PointApiException(ErrorCode.POLICY_NOT_FOUND);
        }

        String updatedBy = (requestDto.updatedBy() != null && !requestDto.updatedBy().isBlank())
            ? requestDto.updatedBy() : "ADMIN";

        log.info("정책 수정 요청: key={}, value={}, updatedBy={}", key, requestDto.value(), updatedBy);
        PointPolicy updated = pointPolicyService.updatePolicy(policyKey, requestDto.value(), updatedBy);
        return ResponseEntity.ok(PointPolicyResponse.from(updated));
    }
}
