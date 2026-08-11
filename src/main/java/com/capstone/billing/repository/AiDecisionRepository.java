package com.capstone.billing.repository;

import com.capstone.billing.domain.AiDecision;
import com.capstone.billing.domain.DecisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AiDecisionRepository extends JpaRepository<AiDecision, Long> {

    List<AiDecision> findTop10ByOrderByCreatedAtDesc();

    List<AiDecision> findByStatus(DecisionStatus status);

    long countByStatus(DecisionStatus status);

    @Query("""
            select d from AiDecision d
            join fetch d.policy p
            join fetch p.customer
            order by d.createdAt desc
            """)
    List<AiDecision> findRecentWithPolicy();

    Optional<AiDecision> findFirstByPolicyIdAndStatusOrderByCreatedAtDesc(Long policyId, DecisionStatus status);

    List<AiDecision> findByPolicyIdOrderByCreatedAtDesc(Long policyId);
}
