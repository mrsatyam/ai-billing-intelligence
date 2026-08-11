package com.capstone.billing.web;

import com.capstone.billing.ai.AiEngine;
import com.capstone.billing.ai.model.CollectionRecommendation;
import com.capstone.billing.ai.model.PolicyAiContext;
import com.capstone.billing.ai.model.RiskExplanation;
import com.capstone.billing.domain.AiDecision;
import com.capstone.billing.domain.Policy;
import com.capstone.billing.service.DashboardService;
import com.capstone.billing.service.DecisionService;
import com.capstone.billing.service.dto.AtRiskPolicyRow;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/policies")
public class PolicyPageController {

    private final DashboardService dashboardService;
    private final DecisionService decisionService;
    private final AiEngine aiEngine;

    public PolicyPageController(DashboardService dashboardService,
                                DecisionService decisionService,
                                AiEngine aiEngine) {
        this.dashboardService = dashboardService;
        this.decisionService = decisionService;
        this.aiEngine = aiEngine;
    }

    @GetMapping("/at-risk")
    public String atRisk(Model model) {
        List<AtRiskPolicyRow> rows = dashboardService.atRiskRows();
        model.addAttribute("appName", "AI Billing Intelligence");
        model.addAttribute("rows", rows);
        return "at-risk";
    }

    @GetMapping("/{id}")
    public String analysis(@PathVariable Long id, Model model) {
        Policy policy = decisionService.getPolicyDetail(id);
        // initialize lazy collections for the view
        policy.getClaims().size();
        policy.getPaymentHistories().size();
        policy.getAiDecisions().size();

        PolicyAiContext ctx = PolicyAiContext.from(policy);
        RiskExplanation explanation = aiEngine.explainRisk(ctx);
        CollectionRecommendation recommendation = aiEngine.recommendCollection(ctx);
        AiDecision pending = decisionService.ensurePendingDecision(id);

        List<AiDecision> history = decisionService.historyForPolicy(id);

        model.addAttribute("appName", "AI Billing Intelligence");
        model.addAttribute("policy", policy);
        model.addAttribute("customer", policy.getCustomer());
        model.addAttribute("explanation", explanation);
        model.addAttribute("recommendation", recommendation);
        model.addAttribute("pendingDecision", pending);
        model.addAttribute("history", history);
        model.addAttribute("payments", policy.getPaymentHistories().stream()
                .sorted(Comparator.comparing(p -> p.getDueDate(), Comparator.nullsLast(Comparator.reverseOrder())))
                .toList());
        model.addAttribute("claims", policy.getClaims());
        model.addAttribute("policyId", id);
        return "analysis";
    }
}
