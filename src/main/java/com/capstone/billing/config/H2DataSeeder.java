package com.capstone.billing.config;

import com.capstone.billing.domain.AiDecision;
import com.capstone.billing.domain.AiRecommendationType;
import com.capstone.billing.domain.Claim;
import com.capstone.billing.domain.ClaimStatus;
import com.capstone.billing.domain.Customer;
import com.capstone.billing.domain.DecisionStatus;
import com.capstone.billing.domain.IncomeSegment;
import com.capstone.billing.domain.PaymentHistory;
import com.capstone.billing.domain.PaymentMethod;
import com.capstone.billing.domain.Policy;
import com.capstone.billing.domain.PolicyStatus;
import com.capstone.billing.domain.PolicyType;
import com.capstone.billing.repository.CustomerRepository;
import com.capstone.billing.repository.PolicyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * India-localized demo seeder (default H2 and Oracle app profile).
 * For pure Oracle SQL loading, run {@code db/oracle/schema.sql} + {@code seed-data.sql}
 * then set {@code billing.seed.enabled=false}.
 */
@Component
@Order(1)
public class H2DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(H2DataSeeder.class);

    private static final String[] FIRST_NAMES = {
            "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Ayaan",
            "Krishna", "Ishaan", "Ananya", "Aadhya", "Diya", "Pari", "Anika", "Myra",
            "Sara", "Aisha", "Kiara", "Priya", "Rahul", "Neha", "Rohan", "Sneha",
            "Karan", "Meera", "Amit", "Pooja", "Vikram", "Nisha", "Suresh", "Kavya"
    };

    private static final String[] LAST_NAMES = {
            "Sharma", "Verma", "Patel", "Reddy", "Iyer", "Nair", "Khan", "Singh",
            "Gupta", "Mehta", "Joshi", "Desai", "Chopra", "Malhotra", "Banerjee", "Das"
    };

    private static final String[] OCCUPATIONS = {
            "Software Engineer", "Bank Officer", "Teacher", "Shop Owner", "CA",
            "Sales Executive", "Doctor", "Driver", "Government Clerk", "Freelancer",
            "Business Analyst", "Nurse", "Factory Supervisor", "Retail Manager"
    };

    private static final String[] REGIONS = {
            "Mumbai", "Pune", "Bengaluru", "Hyderabad", "Chennai", "Delhi NCR",
            "Kolkata", "Ahmedabad", "Jaipur", "Lucknow", "Indore", "Kochi",
            "Chandigarh", "Nagpur", "Coimbatore"
    };

    private static final int[] FESTIVE_MONTHS = {10, 11, 3}; // Diwali/Navratri, wedding season-ish

    private final SeedProperties seedProperties;
    private final CustomerRepository customerRepository;
    private final PolicyRepository policyRepository;

    public H2DataSeeder(SeedProperties seedProperties,
                        CustomerRepository customerRepository,
                        PolicyRepository policyRepository) {
        this.seedProperties = seedProperties;
        this.customerRepository = customerRepository;
        this.policyRepository = policyRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedProperties.isEnabled()) {
            log.info("Demo data seeding disabled (billing.seed.enabled=false)");
            return;
        }
        if (policyRepository.count() > 0) {
            log.info("Skipping demo seed — {} policies already present", policyRepository.count());
            return;
        }

        int count = Math.max(1, seedProperties.getPolicyCount());
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= count; i++) {
            Customer customer = buildCustomer(i, rnd);
            customerRepository.save(customer);

            Policy policy = buildPolicy(i, customer, today, rnd);
            int missed = seedPayments(policy, today, rnd);
            int claimCount = seedClaims(policy, today, rnd);
            int risk = computeRisk(policy, customer, missed, claimCount, rnd);
            policy.setRiskScore(risk);
            policy.setStatus(statusForRisk(risk, policy.getDueDate(), today));

            if (risk >= 70) {
                seedPendingDecision(policy, risk, missed, rnd);
            }

            policyRepository.save(policy);
        }

        log.info("Demo seeder created {} India-localized policies", count);
    }

    private Customer buildCustomer(int index, ThreadLocalRandom rnd) {
        Customer c = new Customer();
        c.setName(FIRST_NAMES[rnd.nextInt(FIRST_NAMES.length)] + " " + LAST_NAMES[rnd.nextInt(LAST_NAMES.length)]);
        c.setAge(22 + rnd.nextInt(40));
        c.setOccupation(OCCUPATIONS[rnd.nextInt(OCCUPATIONS.length)]);
        c.setIncomeSegment(IncomeSegment.values()[rnd.nextInt(IncomeSegment.values().length)]);
        c.setRegion(REGIONS[rnd.nextInt(REGIONS.length)]);
        // UPI-heavy India mix
        int pm = rnd.nextInt(100);
        c.setPreferredPaymentMethod(pm < 65 ? PaymentMethod.UPI : pm < 85 ? PaymentMethod.NEFT : PaymentMethod.CARD);
        c.setSalaryCreditDay(List.of(1, 5, 7, 10, 15, 25, 28).get(rnd.nextInt(7)));
        // ensure unique-ish demos for first few
        if (index == 1) {
            c.setName("John D'Souza");
            c.setPreferredPaymentMethod(PaymentMethod.UPI);
            c.setSalaryCreditDay(5);
            c.setRegion("Mumbai");
            c.setIncomeSegment(IncomeSegment.MID);
            c.setOccupation("Sales Executive");
        }
        return c;
    }

    private Policy buildPolicy(int index, Customer customer, LocalDate today, ThreadLocalRandom rnd) {
        Policy p = new Policy();
        p.setPolicyNumber(String.format("P%04d", 1000 + index));
        p.setPolicyType(PolicyType.values()[rnd.nextInt(PolicyType.values().length)]);
        BigDecimal premium = BigDecimal.valueOf(1500 + rnd.nextInt(45000))
                .setScale(2, RoundingMode.HALF_UP);
        p.setPremium(premium);
        p.setAutoPay(rnd.nextInt(100) < 35);
        // Mix of due today / overdue / upcoming
        int dueOffset = rnd.nextInt(100) < 25 ? 0 : rnd.nextInt(-20, 25);
        p.setDueDate(today.plusDays(dueOffset));
        p.setCustomer(customer);
        p.setStatus(PolicyStatus.ACTIVE);
        p.setRiskScore(0);
        if (index == 1) {
            p.setPolicyNumber("P1234");
            p.setPremium(new BigDecimal("2500.00"));
            p.setPolicyType(PolicyType.MOTOR);
            p.setAutoPay(false);
            p.setDueDate(today);
        }
        return p;
    }

    private int seedPayments(Policy policy, LocalDate today, ThreadLocalRandom rnd) {
        int missed = 0;
        for (int m = 1; m <= 6; m++) {
            PaymentHistory ph = new PaymentHistory();
            LocalDate due = today.minusMonths(m).withDayOfMonth(Math.min(28, policy.getCustomer().getSalaryCreditDay()));
            // Bias misses toward festive months
            boolean festive = false;
            for (int fm : FESTIVE_MONTHS) {
                if (due.getMonthValue() == fm) {
                    festive = true;
                    break;
                }
            }
            boolean isMissed = rnd.nextInt(100) < (festive ? 45 : 18);
            boolean isLate = !isMissed && rnd.nextInt(100) < (festive ? 35 : 15);
            ph.setDueDate(due);
            ph.setAmount(policy.getPremium());
            ph.setMissed(isMissed);
            ph.setLate(isLate);
            if (isMissed) {
                ph.setPaidDate(null);
                missed++;
            } else if (isLate) {
                ph.setPaidDate(due.plusDays(3 + rnd.nextInt(12)));
            } else {
                ph.setPaidDate(due.minusDays(rnd.nextInt(3)));
            }
            // John demo: two festive misses
            if ("P1234".equals(policy.getPolicyNumber()) && festive && missed < 2) {
                ph.setMissed(true);
                ph.setLate(false);
                ph.setPaidDate(null);
                missed = Math.max(missed, 1);
            }
            policy.addPaymentHistory(ph);
        }
        return (int) policy.getPaymentHistories().stream().filter(PaymentHistory::isMissed).count();
    }

    private int seedClaims(Policy policy, LocalDate today, ThreadLocalRandom rnd) {
        if (rnd.nextInt(100) >= 28) {
            return 0;
        }
        int n = 1 + rnd.nextInt(2);
        for (int i = 0; i < n; i++) {
            Claim claim = new Claim();
            claim.setAmount(BigDecimal.valueOf(5000 + rnd.nextInt(95000)).setScale(2, RoundingMode.HALF_UP));
            claim.setStatus(ClaimStatus.values()[rnd.nextInt(ClaimStatus.values().length)]);
            claim.setClaimDate(today.minusDays(30 + rnd.nextInt(300)));
            claim.setDescription(policy.getPolicyType() + " claim #" + (i + 1));
            policy.addClaim(claim);
        }
        if ("P1234".equals(policy.getPolicyNumber())) {
            policy.getClaims().clear();
            return 0;
        }
        return policy.getClaims().size();
    }

    private int computeRisk(Policy policy, Customer customer, int missed, int claims, ThreadLocalRandom rnd) {
        int score = 25;
        score += missed * 18;
        score += claims * 8;
        if (!policy.isAutoPay()) {
            score += 10;
        }
        if (customer.getIncomeSegment() == IncomeSegment.LOW) {
            score += 12;
        } else if (customer.getIncomeSegment() == IncomeSegment.HIGH) {
            score -= 8;
        }
        if (customer.getPreferredPaymentMethod() == PaymentMethod.UPI) {
            score -= 3;
        }
        String region = customer.getRegion();
        if (region != null && (region.contains("Lucknow") || region.contains("Nagpur") || region.contains("Indore"))) {
            score += 6; // tier-2 inflation / collection friction demo signal
        }
        score += rnd.nextInt(-5, 6);
        if ("P1234".equals(policy.getPolicyNumber())) {
            score = 91;
        }
        return Math.max(5, Math.min(98, score));
    }

    private PolicyStatus statusForRisk(int risk, LocalDate dueDate, LocalDate today) {
        if (risk >= 90) {
            return PolicyStatus.AT_RISK;
        }
        if (dueDate.isBefore(today) && risk >= 60) {
            return PolicyStatus.PAST_DUE;
        }
        if (risk >= 70) {
            return PolicyStatus.AT_RISK;
        }
        return PolicyStatus.ACTIVE;
    }

    private void seedPendingDecision(Policy policy, int risk, int missed, ThreadLocalRandom rnd) {
        AiRecommendationType rec;
        if (missed >= 2 && risk >= 85) {
            rec = AiRecommendationType.OFFER_INSTALLMENTS;
        } else if (risk >= 80) {
            rec = AiRecommendationType.AGENT_CALL;
        } else if (!policy.isAutoPay()) {
            rec = AiRecommendationType.AUTOPAY_DISCOUNT;
        } else {
            rec = AiRecommendationType.WHATSAPP_REMINDER;
        }
        if ("P1234".equals(policy.getPolicyNumber())) {
            rec = AiRecommendationType.OFFER_INSTALLMENTS;
        }

        AiDecision decision = new AiDecision();
        decision.setRecommendation(rec);
        decision.setPredictedSuccess(Math.max(55, 100 - risk + rnd.nextInt(10)));
        decision.setStatus(DecisionStatus.PENDING);
        decision.setCreatedAt(LocalDateTime.now().minusHours(rnd.nextInt(48)));
        decision.setReasoning(String.format(
                "Customer shows elevated delinquency risk (%d%%). Missed payments=%d, AutoPay=%s, preferred method=%s. Recommended action: %s.",
                risk,
                missed,
                policy.isAutoPay() ? "ON" : "OFF",
                policy.getCustomer().getPreferredPaymentMethod(),
                rec.getLabel()));
        policy.addAiDecision(decision);
    }
}
