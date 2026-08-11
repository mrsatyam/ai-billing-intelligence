package com.capstone.billing.service;

import com.capstone.billing.ai.AiEngine;
import com.capstone.billing.ai.model.BillingHealthSnapshot;
import com.capstone.billing.ai.model.CallScript;
import com.capstone.billing.ai.model.CollectionRecommendation;
import com.capstone.billing.ai.model.DelinquencyPrediction;
import com.capstone.billing.ai.model.GeneratedEmail;
import com.capstone.billing.ai.model.PaymentPlanProposal;
import com.capstone.billing.ai.model.PolicyAiContext;
import com.capstone.billing.ai.model.RiskExplanation;
import com.capstone.billing.domain.Policy;
import com.capstone.billing.repository.PolicyRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thin service that loads policy graphs and delegates to the active {@link AiEngine}
 * (Gemini hybrid facade with rule-based fallback).
 */
@Service
public class AiAnalysisService {

    private final PolicyRepository policyRepository;
    private final AiEngine aiEngine;

    public AiAnalysisService(PolicyRepository policyRepository, AiEngine aiEngine) {
        this.policyRepository = policyRepository;
        this.aiEngine = aiEngine;
    }

    @Transactional(readOnly = true)
    public PolicyAiContext loadContext(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new EntityNotFoundException("Policy not found: " + policyId));
        // touch lazy collections inside transaction
        policy.getClaims().size();
        policy.getPaymentHistories().size();
        policy.getCustomer().getName();
        return PolicyAiContext.from(policy);
    }

    @Transactional(readOnly = true)
    public DelinquencyPrediction predict(Long policyId) {
        return aiEngine.predictDelinquency(loadContext(policyId));
    }

    @Transactional(readOnly = true)
    public CollectionRecommendation recommend(Long policyId) {
        return aiEngine.recommendCollection(loadContext(policyId));
    }

    @Transactional(readOnly = true)
    public RiskExplanation explain(Long policyId) {
        return aiEngine.explainRisk(loadContext(policyId));
    }

    @Transactional(readOnly = true)
    public GeneratedEmail email(Long policyId) {
        return aiEngine.generateEmail(loadContext(policyId));
    }

    @Transactional(readOnly = true)
    public CallScript callScript(Long policyId) {
        return aiEngine.generateCallScript(loadContext(policyId));
    }

    @Transactional(readOnly = true)
    public PaymentPlanProposal paymentPlans(Long policyId) {
        return aiEngine.generatePaymentPlans(loadContext(policyId));
    }

    public String chat(String question, PolicyAiContext context, BillingHealthSnapshot health) {
        return aiEngine.chat(question, context, health);
    }
}
