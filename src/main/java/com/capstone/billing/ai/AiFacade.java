package com.capstone.billing.ai;

import com.capstone.billing.ai.model.BillingHealthSnapshot;
import com.capstone.billing.ai.model.CallScript;
import com.capstone.billing.ai.model.CollectionRecommendation;
import com.capstone.billing.ai.model.DelinquencyPrediction;
import com.capstone.billing.ai.model.GeneratedEmail;
import com.capstone.billing.ai.model.PaymentPlanProposal;
import com.capstone.billing.ai.model.PolicyAiContext;
import com.capstone.billing.ai.model.RiskExplanation;
import com.capstone.billing.domain.AiRecommendationType;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Hybrid AI: Gemini when {@code GEMINI_API_KEY} is set, otherwise rule-based engine.
 * Numeric plans/scores stay anchored to rules so demos remain stable.
 */
@Component
@Primary
public class AiFacade implements AiEngine {

    private static final Logger log = LoggerFactory.getLogger(AiFacade.class);

    private static final String SYSTEM = """
            You are an Insurance Billing Expert briefing P&C billing managers and collection admins in India.
            Write in third person about the customer (name, "the customer", "this policy") — never "you" or "your".
            Be concise and practical. Prefer UPI, installments, and agent calls over legal notices.
            Currency is INR (₹). Never invent policy numbers.
            """;

    private static final String CUSTOMER_FACING = """
            You are an Insurance Billing Expert drafting customer-facing copy for P&C insurers in India.
            Address the policyholder in second person ("you" / "your"). Be empathetic and practical.
            Prefer UPI, installments, and agent calls over legal notices. Currency is INR (₹).
            Never invent policy numbers.
            """;

    private final RuleBasedAiEngine rules;
    private final GeminiClient geminiClient;

    public AiFacade(RuleBasedAiEngine rules, GeminiClient geminiClient) {
        this.rules = rules;
        this.geminiClient = geminiClient;
    }

    public boolean isLiveGemini() {
        return geminiClient.isAvailable();
    }

    @Override
    public DelinquencyPrediction predictDelinquency(PolicyAiContext context) {
        DelinquencyPrediction baseline = rules.predictDelinquency(context);
        if (!geminiClient.isAvailable()) {
            return baseline;
        }
        String user = contextBlock(context)
                + "\nWrite an internal admin summary (third person). Return JSON: "
                + "{\"summary\": string, \"factors\": string[] } "
                + "Use riskScore=" + baseline.getRiskScore() + " as given. Keep factors short.";
        Optional<JsonNode> json = geminiClient.generateJson(SYSTEM, user);
        if (json.isEmpty()) {
            return baseline;
        }
        JsonNode node = json.get();
        if (node.hasNonNull("summary")) {
            baseline.setSummary(node.get("summary").asText());
        }
        if (node.has("factors") && node.get("factors").isArray() && node.get("factors").size() > 0) {
            List<String> factors = new ArrayList<>();
            node.get("factors").forEach(n -> factors.add(n.asText()));
            baseline.setFactors(factors);
        }
        return baseline;
    }

    @Override
    public CollectionRecommendation recommendCollection(PolicyAiContext context) {
        CollectionRecommendation baseline = rules.recommendCollection(context);
        if (!geminiClient.isAvailable()) {
            return baseline;
        }
        String user = contextBlock(context)
                + "\nSuggest a collection strategy for the billing admin. "
                + "Reasoning must be third person (about the customer, not to them). Return JSON: "
                + "{\"primaryAction\": one of "
                + enumNames()
                + ", \"supportingActions\": string[], \"reasoning\": string, \"predictedSuccess\": number 55-95, "
                + "\"avoidActions\": string[] }";
        Optional<JsonNode> json = geminiClient.generateJson(SYSTEM, user);
        if (json.isEmpty()) {
            return baseline;
        }
        try {
            JsonNode node = json.get();
            parseAction(node.path("primaryAction").asText(null))
                    .ifPresent(baseline::setPrimaryAction);
            if (node.has("supportingActions") && node.get("supportingActions").isArray()) {
                List<AiRecommendationType> supporting = new ArrayList<>();
                node.get("supportingActions").forEach(n -> parseAction(n.asText()).ifPresent(supporting::add));
                if (!supporting.isEmpty()) {
                    baseline.setSupportingActions(supporting);
                }
            }
            if (node.hasNonNull("reasoning")) {
                baseline.setReasoning(node.get("reasoning").asText());
            }
            if (node.has("predictedSuccess")) {
                int success = node.get("predictedSuccess").asInt(baseline.getPredictedSuccess());
                baseline.setPredictedSuccess(Math.max(55, Math.min(95, success)));
            }
            if (node.has("avoidActions") && node.get("avoidActions").isArray()) {
                List<String> avoid = new ArrayList<>();
                node.get("avoidActions").forEach(n -> avoid.add(n.asText()));
                if (!avoid.isEmpty()) {
                    baseline.setAvoidActions(avoid);
                }
            }
        } catch (Exception ex) {
            log.warn("Gemini recommendation merge failed, using rules: {}", ex.getMessage());
        }
        return baseline;
    }

    @Override
    public RiskExplanation explainRisk(PolicyAiContext context) {
        RiskExplanation baseline = rules.explainRisk(context);
        if (!geminiClient.isAvailable()) {
            return baseline;
        }
        String user = contextBlock(context)
                + "\nRisk score is " + baseline.getRiskScore()
                + "%. Write an internal briefing for a billing admin reviewing this case. "
                + "Use third person only (e.g. the customer / this policy / their occupation). "
                + "Do not address the policyholder. Do not draft a customer message. Return JSON: "
                + "{\"headline\": string, \"whyHighRisk\": string[], \"mitigatingFactors\": string[], \"narrative\": string}";
        Optional<JsonNode> json = geminiClient.generateJson(SYSTEM, user);
        if (json.isEmpty()) {
            return baseline;
        }
        JsonNode node = json.get();
        if (node.hasNonNull("headline")) {
            baseline.setHeadline(node.get("headline").asText());
        }
        if (node.hasNonNull("narrative")) {
            baseline.setNarrative(node.get("narrative").asText());
        }
        copyStringArray(node, "whyHighRisk", baseline.getWhyHighRisk());
        copyStringArray(node, "mitigatingFactors", baseline.getMitigatingFactors());
        return baseline;
    }

    @Override
    public GeneratedEmail generateEmail(PolicyAiContext context) {
        GeneratedEmail baseline = rules.generateEmail(context);
        if (!geminiClient.isAvailable()) {
            return baseline;
        }
        String user = contextBlock(context)
                + "\nGenerate a collection email for India. Return JSON: "
                + "{\"subject\": string, \"body\": string, \"tone\": string, \"language\": string}";
        Optional<JsonNode> json = geminiClient.generateJson(CUSTOMER_FACING, user);
        if (json.isEmpty()) {
            return baseline;
        }
        JsonNode node = json.get();
        if (node.hasNonNull("subject")) {
            baseline.setSubject(node.get("subject").asText());
        }
        if (node.hasNonNull("body")) {
            baseline.setBody(node.get("body").asText());
        }
        if (node.hasNonNull("tone")) {
            baseline.setTone(node.get("tone").asText());
        }
        if (node.hasNonNull("language")) {
            baseline.setLanguage(node.get("language").asText());
        }
        return baseline;
    }

    @Override
    public CallScript generateCallScript(PolicyAiContext context) {
        CallScript baseline = rules.generateCallScript(context);
        if (!geminiClient.isAvailable()) {
            return baseline;
        }
        String user = contextBlock(context)
                + "\nWrite a short collection call script for an Indian agent (Hinglish OK). Return JSON: "
                + "{\"opening\": string, \"fullScript\": string, \"closing\": string, \"tone\": string}";
        Optional<JsonNode> json = geminiClient.generateJson(CUSTOMER_FACING, user);
        if (json.isEmpty()) {
            return baseline;
        }
        JsonNode node = json.get();
        if (node.hasNonNull("opening")) {
            baseline.setOpening(node.get("opening").asText());
        }
        if (node.hasNonNull("fullScript")) {
            baseline.setFullScript(node.get("fullScript").asText());
        }
        if (node.hasNonNull("closing")) {
            baseline.setClosing(node.get("closing").asText());
        }
        if (node.hasNonNull("tone")) {
            baseline.setTone(node.get("tone").asText());
        }
        return baseline;
    }

    @Override
    public PaymentPlanProposal generatePaymentPlans(PolicyAiContext context) {
        PaymentPlanProposal baseline = rules.generatePaymentPlans(context);
        if (!geminiClient.isAvailable()) {
            return baseline;
        }
        String user = contextBlock(context)
                + "\nWe already computed plans. Best is "
                + (baseline.getBestOption() != null ? baseline.getBestOption().getMonths() + " months" : "3 months")
                + ". Return JSON: {\"rationale\": string} explaining why that tenure fits this customer.";
        Optional<JsonNode> json = geminiClient.generateJson(SYSTEM, user);
        json.filter(n -> n.hasNonNull("rationale"))
                .ifPresent(n -> baseline.setRationale(n.get("rationale").asText()));
        return baseline;
    }

    @Override
    public String chat(String question, PolicyAiContext policyContext, BillingHealthSnapshot health) {
        String fallback = rules.chat(question, policyContext, health);
        if (!geminiClient.isAvailable()) {
            return fallback;
        }
        StringBuilder user = new StringBuilder("Manager question: ").append(question).append("\n");
        if (policyContext != null) {
            user.append(contextBlock(policyContext)).append("\n");
        }
        if (health != null) {
            user.append("Dashboard KPIs: atRisk=").append(health.getPoliciesAtRisk())
                    .append(", collectionRate=").append(health.getCollectionRate())
                    .append("%, premiumDueToday=").append(health.getPremiumDueToday())
                    .append(", leakage=").append(health.getPredictedLeakage())
                    .append(", pendingRecs=").append(health.getPendingRecommendations())
                    .append("\n");
        }
        user.append("Answer in 2-5 short sentences for an insurance billing manager.");
        return geminiClient.generateText(SYSTEM, user.toString()).orElse(fallback);
    }

    private static String contextBlock(PolicyAiContext ctx) {
        return """
                Customer: %s
                Policy: %s (%s)
                Premium: %s
                Age: %d | Occupation: %s | Income: %s | Region: %s
                Payment method: %s | Salary credit day: %d | AutoPay: %s
                Missed payments: %d | Late: %d | Claims: %d | Festive miss pattern: %s
                Stored risk score: %d
                """.formatted(
                ctx.getCustomerName(),
                ctx.getPolicyNumber(),
                ctx.getPolicyType(),
                ctx.getPremium(),
                ctx.getAge(),
                ctx.getOccupation(),
                ctx.getIncomeSegment(),
                ctx.getRegion(),
                ctx.getPreferredPaymentMethod(),
                ctx.getSalaryCreditDay(),
                ctx.isAutoPay() ? "ON" : "OFF",
                ctx.getMissedPayments(),
                ctx.getLatePayments(),
                ctx.getClaimCount(),
                ctx.isFestiveMissPattern(),
                ctx.getStoredRiskScore()
        );
    }

    private static String enumNames() {
        StringBuilder sb = new StringBuilder("[");
        AiRecommendationType[] values = AiRecommendationType.values();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(values[i].name());
        }
        return sb.append("]").toString();
    }

    private static Optional<AiRecommendationType> parseAction(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        try {
            return Optional.of(AiRecommendationType.valueOf(normalized));
        } catch (IllegalArgumentException ex) {
            for (AiRecommendationType type : AiRecommendationType.values()) {
                if (type.getLabel().equalsIgnoreCase(raw.trim())
                        || type.name().replace("_", "").equalsIgnoreCase(normalized.replace("_", ""))) {
                    return Optional.of(type);
                }
            }
            return Optional.empty();
        }
    }

    private static void copyStringArray(JsonNode node, String field, List<String> target) {
        if (!node.has(field) || !node.get(field).isArray() || node.get(field).isEmpty()) {
            return;
        }
        target.clear();
        node.get(field).forEach(n -> target.add(n.asText()));
    }
}
