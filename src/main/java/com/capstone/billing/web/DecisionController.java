package com.capstone.billing.web;

import com.capstone.billing.domain.AiDecision;
import com.capstone.billing.service.DecisionService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/api/decisions")
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Model model) {
        AiDecision decision = decisionService.approve(id);
        model.addAttribute("appName", "AI Billing Intelligence");
        model.addAttribute("decision", decision);
        model.addAttribute("policy", decision.getPolicy());
        model.addAttribute("customer", decision.getPolicy().getCustomer());
        return "decision-approved";
    }

    @PostMapping("/approve-policy")
    public String approvePolicy(@RequestParam Long policyId, Model model) {
        AiDecision decision = decisionService.approveForPolicy(policyId);
        model.addAttribute("appName", "AI Billing Intelligence");
        model.addAttribute("decision", decision);
        model.addAttribute("policy", decision.getPolicy());
        model.addAttribute("customer", decision.getPolicy().getCustomer());
        return "decision-approved";
    }
}
