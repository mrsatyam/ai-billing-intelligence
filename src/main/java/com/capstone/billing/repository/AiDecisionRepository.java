package com.capstone.billing.repository;

import com.capstone.billing.domain.AiDecision;
import com.capstone.billing.domain.DecisionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiDecisionRepository extends JpaRepository<AiDecision, Long> {

    List<AiDecision> findTop10ByOrderByCreatedAtDesc();

    List<AiDecision> findByStatus(DecisionStatus status);

    long countByStatus(DecisionStatus status);
}
