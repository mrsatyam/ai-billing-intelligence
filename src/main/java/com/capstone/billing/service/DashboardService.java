package com.capstone.billing.service;

import com.capstone.billing.ai.AiEngine;
import com.capstone.billing.ai.model.BillingHealthSnapshot;
import com.capstone.billing.ai.model.CollectionRecommendation;
import com.capstone.billing.ai.model.PolicyAiContext;
import com.capstone.billing.domain.AiDecision;
import com.capstone.billing.domain.DecisionStatus;
import com.capstone.billing.domain.Policy;
import com.capstone.billing.repository.AiDecisionRepository;
import com.capstone.billing.repository.PolicyRepository;
import com.capstone.billing.service.dto.AtRiskPolicyRow;
import com.capstone.billing.service.dto.DashboardMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class DashboardService {

    private static final int AT_RISK_THRESHOLD = 70;

    private final PolicyRepository policyRepository;
    private final AiDecisionRepository aiDecisionRepository;
    private final AiEngine aiEngine;

    public DashboardService(PolicyRepository policyRepository,
                            AiDecisionRepository aiDecisionRepository,
                            AiEngine aiEngine) {
        this.policyRepository = policyRepository;
        this.aiDecisionRepository = aiDecisionRepository;
        this.aiEngine = aiEngine;
    }

    @Transactional(readOnly = true)
    public DashboardMetrics metrics() {
        DashboardMetrics m = new DashboardMetrics();
        long total = policyRepository.count();
        long atRisk = policyRepository.countByRiskScoreGreaterThanEqual(AT_RISK_THRESHOLD);
        BigDecimal dueToday = nullSafe(policyRepository.sumPremiumDueToday());
        BigDecimal leakage = nullSafe(policyRepository.sumPremiumAtRisk())
                .multiply(BigDecimal.valueOf(0.22))
                .setScale(0, RoundingMode.HALF_UP);

        long pending = aiDecisionRepository.countByStatus(DecisionStatus.PENDING);
        long approved = aiDecisionRepository.countByStatus(DecisionStatus.APPROVED);

        // Demo collection rate: inverse of at-risk share with a floor
        double collectionRate = total == 0 ? 100.0
                : Math.max(78.0, Math.min(98.0, 100.0 - (atRisk * 100.0 / total) * 0.35));

        m.setTotalPolicies(total);
        m.setPoliciesAtRisk(atRisk);
        m.setPremiumDueToday(dueToday);
        m.setPredictedRevenueLeakage(leakage);
        m.setAiRecommendations(pending);
        m.setApprovedDecisions(approved);
        m.setCollectionRate(round1(collectionRate));

        // Risk bands
        List<Policy> all = policyRepository.findAll();
        long low = all.stream().filter(p -> p.getRiskScore() < 50).count();
        long mid = all.stream().filter(p -> p.getRiskScore() >= 50 && p.getRiskScore() < 70).count();
        long high = all.stream().filter(p -> p.getRiskScore() >= 70 && p.getRiskScore() < 85).count();
        long critical = all.stream().filter(p -> p.getRiskScore() >= 85).count();
        m.setRiskLabels(List.of("Low <50", "Mid 50-69", "High 70-84", "Critical 85+"));
        m.setRiskCounts(List.of(low, mid, high, critical));

        List<String> regionLabels = new ArrayList<>();
        List<Long> regionCounts = new ArrayList<>();
        for (Object[] row : policyRepository.countAtRiskByRegion()) {
            regionLabels.add(row[0] != null ? row[0].toString() : "Unknown");
            regionCounts.add((Long) row[1]);
        }
        m.setRegionLabels(regionLabels);
        m.setRegionRiskCounts(regionCounts);

        // Synthetic but stable collection trend for Chart.js
        List<String> trendLabels = new ArrayList<>();
        List<Double> trendValues = new ArrayList<>();
        LocalDate month = LocalDate.now().minusMonths(5).withDayOfMonth(1);
        double base = collectionRate - 4;
        for (int i = 0; i < 6; i++) {
            trendLabels.add(month.plusMonths(i).getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
            trendValues.add(round1(base + i * 0.7 + (i % 2 == 0 ? 0.4 : -0.2)));
        }
        m.setCollectionTrendLabels(trendLabels);
        m.setCollectionTrendValues(trendValues);
        return m;
    }

    @Transactional(readOnly = true)
    public BillingHealthSnapshot healthSnapshot() {
        DashboardMetrics m = metrics();
        BillingHealthSnapshot snap = new BillingHealthSnapshot();
        snap.setTotalPolicies(m.getTotalPolicies());
        snap.setPoliciesAtRisk(m.getPoliciesAtRisk());
        snap.setPendingRecommendations(m.getAiRecommendations());
        snap.setPremiumDueToday(m.getPremiumDueToday());
        snap.setPredictedLeakage(m.getPredictedRevenueLeakage());
        snap.setCollectionRate(m.getCollectionRate());
        return snap;
    }

    @Transactional(readOnly = true)
    public List<AtRiskPolicyRow> atRiskRows() {
        List<Policy> policies = policyRepository.findAtRiskWithCustomer(AT_RISK_THRESHOLD);
        List<AtRiskPolicyRow> rows = new ArrayList<>();
        for (Policy policy : policies) {
            String recommendation = "Review";
            Long decisionId = null;
            AiDecision pending = policy.getAiDecisions() == null ? null
                    : policy.getAiDecisions().stream()
                    .filter(d -> d.getStatus() == DecisionStatus.PENDING)
                    .max(Comparator.comparing(AiDecision::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(null);
            if (pending != null) {
                recommendation = pending.getRecommendation().getLabel();
                decisionId = pending.getId();
            } else {
                PolicyAiContext ctx = PolicyAiContext.from(policy);
                CollectionRecommendation rec = aiEngine.recommendCollection(ctx);
                recommendation = rec.getPrimaryAction().getLabel();
            }
            rows.add(new AtRiskPolicyRow(
                    policy.getId(),
                    policy.getPolicyNumber(),
                    policy.getCustomer().getName(),
                    policy.getRiskScore(),
                    recommendation,
                    decisionId));
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public List<AiDecision> recentDecisions(int limit) {
        List<AiDecision> all = aiDecisionRepository.findRecentWithPolicy();
        if (all.size() <= limit) {
            return all;
        }
        return all.subList(0, limit);
    }

    private static BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
