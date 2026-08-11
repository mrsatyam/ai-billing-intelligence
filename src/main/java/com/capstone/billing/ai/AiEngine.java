package com.capstone.billing.ai;

import com.capstone.billing.ai.model.BillingHealthSnapshot;
import com.capstone.billing.ai.model.CallScript;
import com.capstone.billing.ai.model.CollectionRecommendation;
import com.capstone.billing.ai.model.DelinquencyPrediction;
import com.capstone.billing.ai.model.GeneratedEmail;
import com.capstone.billing.ai.model.PaymentPlanProposal;
import com.capstone.billing.ai.model.PolicyAiContext;
import com.capstone.billing.ai.model.RiskExplanation;

public interface AiEngine {

    DelinquencyPrediction predictDelinquency(PolicyAiContext context);

    CollectionRecommendation recommendCollection(PolicyAiContext context);

    RiskExplanation explainRisk(PolicyAiContext context);

    GeneratedEmail generateEmail(PolicyAiContext context);

    CallScript generateCallScript(PolicyAiContext context);

    PaymentPlanProposal generatePaymentPlans(PolicyAiContext context);

    String chat(String question, PolicyAiContext policyContext, BillingHealthSnapshot health);
}
