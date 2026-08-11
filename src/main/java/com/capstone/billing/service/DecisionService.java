package com.capstone.billing.service;

import com.capstone.billing.ai.AiEngine;
import com.capstone.billing.ai.model.CollectionRecommendation;
import com.capstone.billing.ai.model.PolicyAiContext;
import com.capstone.billing.ai.model.RiskExplanation;
import com.capstone.billing.domain.AiDecision;
import com.capstone.billing.domain.DecisionStatus;
import com.capstone.billing.domain.Policy;
import com.capstone.billing.repository.AiDecisionRepository;
import com.capstone.billing.repository.PolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class DecisionService {

    private final PolicyRepository policyRepository;
    private final AiDecisionRepository aiDecisionRepository;
    private final AiEngine aiEngine;

    public DecisionService(PolicyRepository policyRepository,
                           AiDecisionRepository aiDecisionRepository,
                           AiEngine aiEngine) {
        this.policyRepository = policyRepository;
        this.aiDecisionRepository = aiDecisionRepository;
        this.aiEngine = aiEngine;
    }

    @Transactional(readOnly = true)
    public Policy getPolicyDetail(Long policyId) {
        return policyRepository.findDetailById(policyId)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found: " + policyId));
    }

    @Transactional(readOnly = true)
    public AiDecision latestPendingOrCreateView(Policy policy) {
        return policy.getAiDecisions() == null ? null
                : policy.getAiDecisions().stream()
                .filter(d -> d.getStatus() == DecisionStatus.PENDING)
                .max(Comparator.comparing(AiDecision::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
    }

    @Transactional
    public AiDecision ensurePendingDecision(Long policyId) {
        Policy policy = getPolicyDetail(policyId);
        AiDecision existing = latestPendingOrCreateView(policy);
        if (existing != null) {
            return existing;
        }
        PolicyAiContext ctx = PolicyAiContext.from(policy);
        CollectionRecommendation rec = aiEngine.recommendCollection(ctx);
        RiskExplanation explanation = aiEngine.explainRisk(ctx);

        AiDecision decision = new AiDecision();
        decision.setPolicy(policy);
        decision.setRecommendation(rec.getPrimaryAction());
        decision.setPredictedSuccess(rec.getPredictedSuccess());
        decision.setStatus(DecisionStatus.PENDING);
        decision.setCreatedAt(LocalDateTime.now());
        decision.setReasoning(explanation.getNarrative() + " " + rec.getReasoning());
        policy.addAiDecision(decision);
        return aiDecisionRepository.save(decision);
    }

    @Transactional
    public AiDecision approve(Long decisionId) {
        AiDecision decision = aiDecisionRepository.findById(decisionId)
                .orElseThrow(() -> new EntityNotFoundException("Decision not found: " + decisionId));
        decision.setStatus(DecisionStatus.APPROVED);
        // initialize associations for the confirmation page
        decision.getPolicy().getPolicyNumber();
        decision.getPolicy().getCustomer().getName();
        return aiDecisionRepository.save(decision);
    }

    @Transactional
    public AiDecision approveForPolicy(Long policyId) {
        AiDecision decision = ensurePendingDecision(policyId);
        decision.setStatus(DecisionStatus.APPROVED);
        decision.getPolicy().getPolicyNumber();
        decision.getPolicy().getCustomer().getName();
        return aiDecisionRepository.save(decision);
    }

    @Transactional(readOnly = true)
    public List<AiDecision> historyForPolicy(Long policyId) {
        return aiDecisionRepository.findByPolicyIdOrderByCreatedAtDesc(policyId);
    }
}
