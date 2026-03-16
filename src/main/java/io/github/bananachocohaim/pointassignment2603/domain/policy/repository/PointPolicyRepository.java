package io.github.bananachocohaim.pointassignment2603.domain.policy.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.bananachocohaim.pointassignment2603.domain.policy.entity.PointPolicy;

public interface PointPolicyRepository extends JpaRepository<PointPolicy, String> {

    Optional<PointPolicy> findByPolicyKey(String policyKey);
}
