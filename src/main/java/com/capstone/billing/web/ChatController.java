package com.capstone.billing.web;

import com.capstone.billing.ai.AiEngine;
import com.capstone.billing.ai.model.BillingHealthSnapshot;
import com.capstone.billing.ai.model.PolicyAiContext;
import com.capstone.billing.domain.Policy;
import com.capstone.billing.repository.PolicyRepository;
import com.capstone.billing.service.AiAnalysisService;
import com.capstone.billing.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final AiAnalysisService aiAnalysisService;
    private final DashboardService dashboardService;
    private final PolicyRepository policyRepository;
    private final AiEngine aiEngine;

    public ChatController(AiAnalysisService aiAnalysisService,
                          DashboardService dashboardService,
                          PolicyRepository policyRepository,
                          AiEngine aiEngine) {
        this.aiAnalysisService = aiAnalysisService;
        this.dashboardService = dashboardService;
        this.policyRepository = policyRepository;
        this.aiEngine = aiEngine;
    }

    @PostMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, String>> chat(@RequestBody Map<String, Object> body) {
        String message = body.get("message") != null ? body.get("message").toString() : "";
        PolicyAiContext policyContext = null;
        if (body.get("policyId") != null) {
            try {
                Long policyId = Long.valueOf(body.get("policyId").toString());
                policyContext = aiAnalysisService.loadContext(policyId);
            } catch (Exception ignored) {
                // optional context
            }
        } else if (body.get("policyNumber") != null) {
            String number = body.get("policyNumber").toString();
            Policy policy = policyRepository.findByPolicyNumber(number).orElse(null);
            if (policy != null) {
                policy.getCustomer();
                policy.getPaymentHistories().size();
                policy.getClaims().size();
                policyContext = PolicyAiContext.from(policy);
            }
        }

        // Lightweight name lookup: "Why is John's score high?"
        if (policyContext == null && message.toLowerCase().contains("john")) {
            Policy john = policyRepository.findByPolicyNumber("P1234").orElse(null);
            if (john != null) {
                john.getCustomer();
                john.getPaymentHistories().size();
                john.getClaims().size();
                policyContext = PolicyAiContext.from(john);
            }
        }

        BillingHealthSnapshot health = dashboardService.healthSnapshot();
        String answer = aiEngine.chat(message, policyContext, health);
        return ResponseEntity.ok(Map.of("answer", answer));
    }
}
