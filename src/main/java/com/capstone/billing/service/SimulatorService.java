package com.capstone.billing.service;

import com.capstone.billing.ai.RuleBasedAiEngine;
import com.capstone.billing.ai.model.CollectionRecommendation;
import com.capstone.billing.ai.model.PolicyAiContext;
import com.capstone.billing.domain.AiRecommendationType;
import com.capstone.billing.domain.Policy;
import com.capstone.billing.repository.PolicyRepository;
import com.capstone.billing.service.dto.SimulatorResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Autonomous Decision Simulator — uses rule engine for fast bulk scan (demo-friendly, low cost).
 */
@Service
public class SimulatorService {

    private final PolicyRepository policyRepository;
    private final RuleBasedAiEngine ruleBasedAiEngine;

    public SimulatorService(PolicyRepository policyRepository, RuleBasedAiEngine ruleBasedAiEngine) {
        this.policyRepository = policyRepository;
        this.ruleBasedAiEngine = ruleBasedAiEngine;
    }

    @Transactional(readOnly = true)
    public SimulatorResult run() {
        List<Policy> policies = policyRepository.findAll();
        // touch customers for context
        policies.forEach(p -> {
            if (p.getCustomer() != null) {
                p.getCustomer().getName();
            }
            if (p.getPaymentHistories() != null) {
                p.getPaymentHistories().size();
            }
            if (p.getClaims() != null) {
                p.getClaims().size();
            }
        });

        int risky = 0;
        int leakages = 0;
        int lapse = 0;
        BigDecimal recovery = BigDecimal.ZERO;
        Set<String> recLabels = new LinkedHashSet<>();
        List<String> highlights = new ArrayList<>();

        for (Policy policy : policies) {
            int risk = policy.getRiskScore();
            if (risk >= 70) {
                risky++;
                PolicyAiContext ctx = PolicyAiContext.from(policy);
                CollectionRecommendation rec = ruleBasedAiEngine.recommendCollection(ctx);
                if (rec.getPrimaryAction() != null) {
                    recLabels.add(rec.getPrimaryAction().getLabel());
                }
                for (AiRecommendationType support : rec.getSupportingActions()) {
                    recLabels.add(support.getLabel());
                }
                // premium leakage candidates: high risk + no autopay or missed payments
                if (!policy.isAutoPay() || ctx.getMissedPayments() >= 2) {
                    leakages++;
                    recovery = recovery.add(policy.getPremium() != null ? policy.getPremium() : BigDecimal.ZERO);
                }
            }
            if (risk >= 85) {
                lapse++;
            }
        }

        // Demo recovery uplift (installments / reminders typically recover a share)
        recovery = recovery.multiply(BigDecimal.valueOf(0.78)).setScale(0, RoundingMode.HALF_UP);

        // Ensure demo-friendly recommendation parade even if set is small
        if (recLabels.isEmpty()) {
            recLabels.add(AiRecommendationType.WHATSAPP_REMINDER.getLabel());
            recLabels.add(AiRecommendationType.AGENT_CALL.getLabel());
            recLabels.add(AiRecommendationType.OFFER_INSTALLMENTS.getLabel());
        }
        List<String> parade = new ArrayList<>(recLabels);
        ensureParade(parade,
                AiRecommendationType.WHATSAPP_REMINDER.getLabel(),
                AiRecommendationType.AGENT_CALL.getLabel(),
                AiRecommendationType.OFFER_INSTALLMENTS.getLabel(),
                AiRecommendationType.AUTOPAY_DISCOUNT.getLabel(),
                AiRecommendationType.GRACE_PERIOD.getLabel());

        if (risky > 0) {
            highlights.add(risky + " risky customers need action");
        }
        if (leakages > 0) {
            highlights.add(leakages + " premium leakage cases detected");
        }
        if (lapse > 0) {
            highlights.add(lapse + " policies likely to lapse without intervention");
        }

        SimulatorResult result = new SimulatorResult();
        result.setTotalScanned(policies.size());
        result.setRiskyCustomers(risky);
        result.setPremiumLeakages(leakages);
        result.setLikelyToLapse(lapse);
        result.setPotentialRecovery(recovery);
        result.setRecommendations(parade);
        result.setHighlights(highlights);
        return result;
    }

    private static void ensureParade(List<String> parade, String... mustHave) {
        for (String label : mustHave) {
            if (!parade.contains(label)) {
                parade.add(label);
            }
        }
    }
}
