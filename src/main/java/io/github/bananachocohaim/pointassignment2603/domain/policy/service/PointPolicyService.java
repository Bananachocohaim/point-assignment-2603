package io.github.bananachocohaim.pointassignment2603.domain.policy.service;

import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.bananachocohaim.pointassignment2603.common.config.CacheConfig;
import io.github.bananachocohaim.pointassignment2603.common.error.ErrorCode;
import io.github.bananachocohaim.pointassignment2603.common.error.PointApiException;
import io.github.bananachocohaim.pointassignment2603.domain.policy.entity.PointPolicy;
import io.github.bananachocohaim.pointassignment2603.domain.policy.entity.PolicyKey;
import io.github.bananachocohaim.pointassignment2603.domain.policy.repository.PointPolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PointPolicyService {

    private final PointPolicyRepository pointPolicyRepository;

    /** 전체 정책 목록 조회 (캐시 없음 - 관리 화면용) */
    public List<PointPolicy> findAll() {
        return pointPolicyRepository.findAll();
    }

    /** 정책 값 조회 - 캐시 적용 */
    @Cacheable(value = CacheConfig.POLICY_CACHE, key = "#policyKey.name()")
    public long getLongValue(PolicyKey policyKey) {
        log.debug("정책 DB 조회: key={}", policyKey);
        PointPolicy policy = pointPolicyRepository.findByPolicyKey(policyKey.name())
            .orElseThrow(() -> new PointApiException(ErrorCode.POLICY_NOT_FOUND));
        return Long.parseLong(policy.getPolicyValue());
    }

    /** 정책 값 수정 - 캐시 무효화 */
    @CacheEvict(value = CacheConfig.POLICY_CACHE, key = "#policyKey.name()")
    @Transactional
    public PointPolicy updatePolicy(PolicyKey policyKey, String value, String updatedBy) {
        PointPolicy policy = pointPolicyRepository.findByPolicyKey(policyKey.name())
            .orElseThrow(() -> new PointApiException(ErrorCode.POLICY_NOT_FOUND));
        policy.update(value, updatedBy);
        log.info("정책 수정: key={}, value={}, updatedBy={}", policyKey, value, updatedBy);
        return policy;
    }
}
