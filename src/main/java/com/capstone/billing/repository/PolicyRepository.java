package com.capstone.billing.repository;

import com.capstone.billing.domain.Policy;
import com.capstone.billing.domain.PolicyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    Optional<Policy> findByPolicyNumber(String policyNumber);

    List<Policy> findByRiskScoreGreaterThanEqualOrderByRiskScoreDesc(int riskScore);

    long countByStatus(PolicyStatus status);

    @Query("select coalesce(sum(p.premium), 0) from Policy p where p.dueDate = current_date")
    java.math.BigDecimal sumPremiumDueToday();

    @Query("select coalesce(sum(p.premium), 0) from Policy p where p.riskScore >= 70")
    java.math.BigDecimal sumPremiumAtRisk();
}
