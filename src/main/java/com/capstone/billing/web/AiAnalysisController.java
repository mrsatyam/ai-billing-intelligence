package com.capstone.billing.web;

import com.capstone.billing.ai.model.CallScript;
import com.capstone.billing.ai.model.CollectionRecommendation;
import com.capstone.billing.ai.model.DelinquencyPrediction;
import com.capstone.billing.ai.model.GeneratedEmail;
import com.capstone.billing.ai.model.PaymentPlanProposal;
import com.capstone.billing.ai.model.RiskExplanation;
import com.capstone.billing.service.AiAnalysisService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST surface for AI analysis features used by the analysis page UI.
 */
@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    private final AiAnalysisService aiAnalysisService;

    public AiAnalysisController(AiAnalysisService aiAnalysisService) {
        this.aiAnalysisService = aiAnalysisService;
    }

    @GetMapping("/policies/{id}/predict")
    public ResponseEntity<DelinquencyPrediction> predict(@PathVariable Long id) {
        return ResponseEntity.ok(aiAnalysisService.predict(id));
    }

    @GetMapping("/policies/{id}/recommend")
    public ResponseEntity<CollectionRecommendation> recommend(@PathVariable Long id) {
        return ResponseEntity.ok(aiAnalysisService.recommend(id));
    }

    @GetMapping("/policies/{id}/explain")
    public ResponseEntity<RiskExplanation> explain(@PathVariable Long id) {
        return ResponseEntity.ok(aiAnalysisService.explain(id));
    }

    @GetMapping("/policies/{id}/email")
    public ResponseEntity<GeneratedEmail> email(@PathVariable Long id) {
        return ResponseEntity.ok(aiAnalysisService.email(id));
    }

    @GetMapping("/policies/{id}/call-script")
    public ResponseEntity<CallScript> callScript(@PathVariable Long id) {
        return ResponseEntity.ok(aiAnalysisService.callScript(id));
    }

    @GetMapping("/policies/{id}/payment-plans")
    public ResponseEntity<PaymentPlanProposal> paymentPlans(@PathVariable Long id) {
        return ResponseEntity.ok(aiAnalysisService.paymentPlans(id));
    }
}
