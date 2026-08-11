package com.capstone.billing.ai;

import com.capstone.billing.ai.model.BillingHealthSnapshot;
import com.capstone.billing.ai.model.CallScript;
import com.capstone.billing.ai.model.CollectionRecommendation;
import com.capstone.billing.ai.model.DelinquencyPrediction;
import com.capstone.billing.ai.model.GeneratedEmail;
import com.capstone.billing.ai.model.PaymentPlanOption;
import com.capstone.billing.ai.model.PaymentPlanProposal;
import com.capstone.billing.ai.model.PolicyAiContext;
import com.capstone.billing.ai.model.RiskExplanation;
import com.capstone.billing.domain.AiRecommendationType;
import com.capstone.billing.domain.IncomeSegment;
import com.capstone.billing.domain.PaymentMethod;
import com.capstone.billing.domain.PolicyType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic India-aware billing AI used when OpenAI is unavailable.
 */
@Component
public class RuleBasedAiEngine implements AiEngine {

    private static final Locale INDIA = new Locale("en", "IN");

    @Override
    public DelinquencyPrediction predictDelinquency(PolicyAiContext context) {
        List<String> factors = new ArrayList<>();
        int score = 20;

        if (context.getMissedPayments() > 0) {
            int add = context.getMissedPayments() * 16;
            score += add;
            factors.add(context.getMissedPayments() + " missed payment(s) (+" + add + ")");
        }
        if (context.getLatePayments() > 0) {
            int add = context.getLatePayments() * 4;
            score += add;
            factors.add(context.getLatePayments() + " late payment(s) (+" + add + ")");
        }
        if (context.isFestiveMissPattern()) {
            score += 10;
            factors.add("Missed payments during festive months (+10)");
        }
        if (!context.isAutoPay()) {
            score += 10;
            factors.add("AutoPay disabled (+10)");
        } else {
            factors.add("AutoPay enabled (stabilizing)");
        }
        if (context.getClaimCount() > 0) {
            int add = context.getClaimCount() * 7;
            score += add;
            factors.add(context.getClaimCount() + " claim(s) on file (+" + add + ")");
        }
        if (context.getIncomeSegment() == IncomeSegment.LOW) {
            score += 12;
            factors.add("Low income segment (+12)");
        } else if (context.getIncomeSegment() == IncomeSegment.HIGH) {
            score -= 8;
            factors.add("High income segment (-8)");
        }
        if (context.getPreferredPaymentMethod() == PaymentMethod.UPI) {
            score -= 3;
            factors.add("Prefers UPI — easier recovery (-3)");
        }
        if (isTierTwoFrictionRegion(context.getRegion())) {
            score += 6;
            factors.add("Higher collection friction region: " + context.getRegion() + " (+6)");
        }
        if (context.getPremium() != null && context.getPremium().compareTo(new BigDecimal("25000")) > 0) {
            score += 5;
            factors.add("High premium ticket (+5)");
        }
        if (context.getAge() > 0 && context.getAge() < 28) {
            score += 4;
            factors.add("Younger age band (+4)");
        }

        score = clamp(score, 5, 98);
        // Prefer stored score when close — keeps seeded demo consistent
        if (context.getStoredRiskScore() > 0
                && Math.abs(context.getStoredRiskScore() - score) <= 15) {
            score = context.getStoredRiskScore();
        }

        String summary = String.format(
                "Delinquency risk for %s (%s) is %d%% based on payment behaviour, income, and channel preferences.",
                context.getCustomerName(), context.getPolicyNumber(), score);

        return new DelinquencyPrediction(score, factors, summary);
    }

    @Override
    public CollectionRecommendation recommendCollection(PolicyAiContext context) {
        DelinquencyPrediction prediction = predictDelinquency(context);
        int risk = prediction.getRiskScore();

        CollectionRecommendation rec = new CollectionRecommendation();
        List<AiRecommendationType> supporting = new ArrayList<>();
        List<String> avoid = new ArrayList<>();
        avoid.add("Legal notice / hard escalation");

        AiRecommendationType primary;
        StringBuilder reasoning = new StringBuilder();

        if (context.getMissedPayments() >= 2 && risk >= 80) {
            primary = AiRecommendationType.OFFER_INSTALLMENTS;
            supporting.add(AiRecommendationType.WAIVE_LATE_FEE);
            supporting.add(AiRecommendationType.DELAY_REMINDER);
            reasoning.append("Customer has repeated misses and high risk — flexible installments plus late-fee relief work better than pressure. ");
        } else if (risk >= 85) {
            primary = AiRecommendationType.AGENT_CALL;
            supporting.add(AiRecommendationType.WHATSAPP_REMINDER);
            supporting.add(AiRecommendationType.GRACE_PERIOD);
            reasoning.append("Very high risk warrants a personal agent call with a short grace window. ");
        } else if (risk >= 70) {
            primary = AiRecommendationType.WHATSAPP_REMINDER;
            supporting.add(AiRecommendationType.FRIENDLY_REMINDER);
            if (!context.isAutoPay()) {
                supporting.add(AiRecommendationType.AUTOPAY_DISCOUNT);
            }
            reasoning.append("Moderate-high risk — start with WhatsApp nudge aligned to preferred channel. ");
        } else if (!context.isAutoPay()) {
            primary = AiRecommendationType.AUTOPAY_DISCOUNT;
            supporting.add(AiRecommendationType.FRIENDLY_REMINDER);
            reasoning.append("Risk is manageable; converting to AutoPay reduces future leakage. ");
        } else {
            primary = AiRecommendationType.FRIENDLY_REMINDER;
            supporting.add(AiRecommendationType.DELAY_REMINDER);
            reasoning.append("Low-moderate risk — gentle reminder timed after salary credit is enough. ");
        }

        if (context.getPreferredPaymentMethod() == PaymentMethod.UPI) {
            reasoning.append("Preferred method is UPI — keep payment link / UPI intent handy. ");
        }
        if (context.isFestiveMissPattern()) {
            reasoning.append("Prior festive-month misses suggest cash-flow timing issues, not intent to lapse. ");
        }
        reasoning.append("Avoid legal notices at this stage to protect retention.");

        int success = clamp(100 - risk + 12 + (context.getPreferredPaymentMethod() == PaymentMethod.UPI ? 5 : 0), 55, 95);

        rec.setPrimaryAction(primary);
        rec.setSupportingActions(supporting);
        rec.setAvoidActions(avoid);
        rec.setReasoning(reasoning.toString().trim());
        rec.setPredictedSuccess(success);
        return rec;
    }

    @Override
    public RiskExplanation explainRisk(PolicyAiContext context) {
        DelinquencyPrediction prediction = predictDelinquency(context);
        RiskExplanation explanation = new RiskExplanation();
        explanation.setRiskScore(prediction.getRiskScore());

        List<String> why = new ArrayList<>();
        List<String> mitigate = new ArrayList<>();

        if (context.getMissedPayments() > 0) {
            why.add(context.getMissedPayments() + " missed premium payment(s)");
        }
        if (context.isFestiveMissPattern()) {
            why.add("Payment slips clustered around festive months");
        }
        if (!context.isAutoPay()) {
            why.add("AutoPay is off — relies on manual remittance");
        }
        if (context.getClaimCount() > 0) {
            why.add(context.getClaimCount() + " prior claim(s) on the policy");
        }
        if (context.getIncomeSegment() == IncomeSegment.LOW) {
            why.add("Income segment indicates tighter monthly cash flow");
        }
        if (isTierTwoFrictionRegion(context.getRegion())) {
            why.add("Region (" + context.getRegion() + ") shows higher collection friction in demo model");
        }
        if (context.getPolicyType() == PolicyType.MOTOR) {
            why.add("Motor book — competitive shopping increases lapse risk after reminders");
        }
        if (why.isEmpty()) {
            why.add("Composite behavioural score elevated versus peer cohort");
        }

        if (context.getPreferredPaymentMethod() == PaymentMethod.UPI) {
            mitigate.add("Strong UPI preference — high likelihood of same-day pay once nudged");
        }
        if (context.getSalaryCreditDay() > 0) {
            mitigate.add("Salary typically credited on the " + dayLabel(context.getSalaryCreditDay())
                    + " — timing reminders after that day improves hit rate");
        }
        if (context.getIncomeSegment() == IncomeSegment.HIGH) {
            mitigate.add("High income segment — recovery usually succeeds with flexible options");
        }
        if (context.isAutoPay()) {
            mitigate.add("AutoPay already enabled");
        }

        String band = prediction.getRiskScore() >= 85 ? "Critical"
                : prediction.getRiskScore() >= 70 ? "High"
                : prediction.getRiskScore() >= 50 ? "Elevated" : "Moderate";
        explanation.setHeadline(band + " delinquency risk (" + prediction.getRiskScore() + "%)");
        explanation.setWhyHighRisk(why);
        explanation.setMitigatingFactors(mitigate);
        explanation.setNarrative(String.format(
                "%s on policy %s is scored at %d%% risk. Key drivers: %s. "
                        + "Recommended posture: explainable, customer-friendly recovery — not aggressive recovery.",
                context.getCustomerName(),
                context.getPolicyNumber(),
                prediction.getRiskScore(),
                String.join("; ", why)));
        return explanation;
    }

    @Override
    public GeneratedEmail generateEmail(PolicyAiContext context) {
        CollectionRecommendation rec = recommendCollection(context);
        boolean soft = rec.getPrimaryAction() == AiRecommendationType.FRIENDLY_REMINDER
                || rec.getPrimaryAction() == AiRecommendationType.WHATSAPP_REMINDER
                || rec.getPrimaryAction() == AiRecommendationType.DELAY_REMINDER;

        String tone = soft ? "Empathetic / Helpful" : "Supportive / Solution-oriented";
        String subject = soft
                ? "Friendly reminder: premium for policy " + context.getPolicyNumber()
                : "Flexible options for your " + context.getPolicyType() + " premium — " + context.getPolicyNumber();

        String body = """
                Dear %s,

                Namaste. This is a courtesy note regarding your %s policy %s.

                Premium amount due: %s
                Preferred payment method on file: %s
                Salary credit day (typical): %s

                Our AI billing assistant suggests: %s.

                You can pay instantly via UPI / net banking, or reply to this email if you would like an installment plan or a short grace period.

                Warm regards,
                Capstone Billing Intelligence
                Customer Care · India
                """.formatted(
                context.getFirstName(),
                context.getPolicyType(),
                context.getPolicyNumber(),
                inr(context.getPremium()),
                context.getPreferredPaymentMethod(),
                dayLabel(context.getSalaryCreditDay()),
                rec.getPrimaryAction().getLabel());

        return new GeneratedEmail(subject, body.trim(), tone, "English (India)");
    }

    @Override
    public CallScript generateCallScript(PolicyAiContext context) {
        CollectionRecommendation rec = recommendCollection(context);
        CallScript script = new CallScript();
        script.setTone("Respectful, consultative — Hindi/English mix acceptable");

        String opening = "Hello " + context.getFirstName() + ", namaste. Main Capstone Insurance billing team se baat kar raha/rahi hoon regarding your policy "
                + context.getPolicyNumber() + ".";
        script.setOpening(opening);

        String full = opening + "\n\n"
                + "I noticed your " + context.getPolicyType().name().toLowerCase() + " premium of "
                + inr(context.getPremium()) + " is due. "
                + (context.getMissedPayments() > 0
                ? "I can also see " + context.getMissedPayments() + " earlier missed payment(s) on the account — no judgment, we want to help you stay covered.\n\n"
                : "Your payment history is mostly steady, so this should be a quick confirmation.\n\n")
                + "Our recommendation today is: " + rec.getPrimaryAction().getLabel() + ".\n"
                + (context.getSalaryCreditDay() > 0
                ? "If salary usually credits on the " + dayLabel(context.getSalaryCreditDay())
                + ", we can schedule the reminder right after that date.\n\n"
                : "\n")
                + "Would you prefer: (1) pay now via UPI, (2) a 3-month installment, or (3) a short grace period?\n\n"
                + "Avoid: threatening cancellation or legal notice on this call.";

        script.setFullScript(full);
        script.setClosing("Dhanyavaad, " + context.getFirstName()
                + ". I'll note your preference on the policy and send a WhatsApp confirmation. Stay safe.");
        return script;
    }

    @Override
    public PaymentPlanProposal generatePaymentPlans(PolicyAiContext context) {
        BigDecimal owed = context.getPremium() != null ? context.getPremium() : BigDecimal.ZERO;
        // Demo: treat outstanding as premium (or slightly higher if misses)
        if (context.getMissedPayments() >= 2) {
            owed = owed.multiply(BigDecimal.valueOf(1.0 + 0.15 * Math.min(context.getMissedPayments(), 3)))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        PaymentPlanProposal proposal = new PaymentPlanProposal();
        List<PaymentPlanOption> options = new ArrayList<>();
        int[] months = {3, 6, 9};
        int bestMonths = context.getIncomeSegment() == IncomeSegment.LOW ? 6
                : context.getMissedPayments() >= 2 ? 3 : 3;

        for (int m : months) {
            BigDecimal monthly = owed.divide(BigDecimal.valueOf(m), 2, RoundingMode.HALF_UP);
            boolean recommended = m == bestMonths;
            String note = recommended ? "Best fit for cash-flow and recovery odds"
                    : m == 9 ? "Lowest monthly burden; slower recovery" : "Balanced tenure";
            options.add(new PaymentPlanOption(m, monthly, owed, recommended, note));
        }

        PaymentPlanOption best = options.stream()
                .filter(PaymentPlanOption::isRecommended)
                .findFirst()
                .orElse(options.get(0));

        proposal.setOptions(options);
        proposal.setBestOption(best);
        proposal.setRationale(String.format(
                "Outstanding estimate %s. Given income=%s and missed=%d, recommend %d-month plan at %s / month.",
                inr(owed),
                context.getIncomeSegment(),
                context.getMissedPayments(),
                best.getMonths(),
                inr(best.getMonthlyAmount())));
        return proposal;
    }

    @Override
    public String chat(String question, PolicyAiContext policyContext, BillingHealthSnapshot health) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) {
            return "Ask me about risk scores, policies to save today, or today's billing health.";
        }

        if (policyContext != null && (q.contains("score") || q.contains("risk") || q.contains("why"))) {
            RiskExplanation explanation = explainRisk(policyContext);
            return explanation.getNarrative() + " Drivers: " + String.join("; ", explanation.getWhyHighRisk()) + ".";
        }

        if (health != null && (q.contains("billing health") || q.contains("summarize") || q.contains("summary")
                || q.contains("today"))) {
            return String.format(
                    "Today's billing health: Premium due %s · Collection rate %.1f%% · Policies at risk %d · "
                            + "Predicted leakage %s · Pending AI recommendations %d.",
                    inr(health.getPremiumDueToday()),
                    health.getCollectionRate(),
                    health.getPoliciesAtRisk(),
                    inr(health.getPredictedLeakage()),
                    health.getPendingRecommendations());
        }

        if (health != null && (q.contains("save") || q.contains("how many") || q.contains("at risk"))) {
            long save = Math.max(1, Math.round(health.getPoliciesAtRisk() * 0.65));
            return String.format(
                    "Focus list: %d policies are at risk. AI estimates we can realistically save about %d today "
                            + "with WhatsApp, agent calls, and installment offers. Potential recovery near %s.",
                    health.getPoliciesAtRisk(),
                    save,
                    inr(health.getPredictedLeakage()));
        }

        if (policyContext != null && (q.contains("recommend") || q.contains("what should") || q.contains("next"))) {
            CollectionRecommendation rec = recommendCollection(policyContext);
            return "For " + policyContext.getCustomerName() + ", primary action is "
                    + rec.getPrimaryAction().getLabel()
                    + " (predicted success " + rec.getPredictedSuccess() + "%). " + rec.getReasoning();
        }

        if (q.contains("installment") || q.contains("payment plan")) {
            if (policyContext == null) {
                return "Open a policy first, or ask about a specific customer — I can generate 3/6/9 month plans.";
            }
            PaymentPlanProposal plans = generatePaymentPlans(policyContext);
            return plans.getRationale();
        }

        return "I can explain a customer's risk, recommend a collection action, draft email/call scripts, "
                + "propose installment plans, or summarize today's billing health. Try: \"Why is the score high?\" "
                + "or \"Summarize today's billing health.\"";
    }

    private static boolean isTierTwoFrictionRegion(String region) {
        if (region == null) {
            return false;
        }
        String r = region.toLowerCase(Locale.ROOT);
        return r.contains("lucknow") || r.contains("nagpur") || r.contains("indore")
                || r.contains("jaipur") || r.contains("coimbatore");
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String dayLabel(int day) {
        return day + getDaySuffix(day);
    }

    private static String getDaySuffix(int day) {
        if (day >= 11 && day <= 13) {
            return "th";
        }
        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

    private static String inr(BigDecimal amount) {
        if (amount == null) {
            return "₹0";
        }
        NumberFormat nf = NumberFormat.getCurrencyInstance(INDIA);
        return nf.format(amount);
    }
}
