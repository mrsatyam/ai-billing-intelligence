package com.capstone.billing.repository;

import com.capstone.billing.domain.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findByPolicyId(Long policyId);
}
