package com.capstone.billing.ai.model;

import com.capstone.billing.domain.Customer;
import com.capstone.billing.domain.IncomeSegment;
import com.capstone.billing.domain.PaymentHistory;
import com.capstone.billing.domain.PaymentMethod;
import com.capstone.billing.domain.Policy;
import com.capstone.billing.domain.PolicyType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Snapshot of policy/customer signals used by AI engines.
 */
public class PolicyAiContext {

    private Long policyId;
    private String policyNumber;
    private String customerName;
    private int age;
    private String occupation;
    private IncomeSegment incomeSegment;
    private String region;
    private PaymentMethod preferredPaymentMethod;
    private int salaryCreditDay;
    private PolicyType policyType;
    private BigDecimal premium;
    private boolean autoPay;
    private int storedRiskScore;
    private int missedPayments;
    private int latePayments;
    private int claimCount;
    private boolean festiveMissPattern;
    private List<PaymentHistory> recentPayments = new ArrayList<>();

    public static PolicyAiContext from(Policy policy) {
        Customer customer = policy.getCustomer();
        PolicyAiContext ctx = new PolicyAiContext();
        ctx.policyId = policy.getId();
        ctx.policyNumber = policy.getPolicyNumber();
        ctx.customerName = customer != null ? customer.getName() : "Customer";
        ctx.age = customer != null ? customer.getAge() : 0;
        ctx.occupation = customer != null ? customer.getOccupation() : "";
        ctx.incomeSegment = customer != null ? customer.getIncomeSegment() : IncomeSegment.MID;
        ctx.region = customer != null ? customer.getRegion() : "";
        ctx.preferredPaymentMethod = customer != null ? customer.getPreferredPaymentMethod() : PaymentMethod.UPI;
        ctx.salaryCreditDay = customer != null ? customer.getSalaryCreditDay() : 1;
        ctx.policyType = policy.getPolicyType();
        ctx.premium = policy.getPremium();
        ctx.autoPay = policy.isAutoPay();
        ctx.storedRiskScore = policy.getRiskScore();
        ctx.claimCount = policy.getClaims() != null ? policy.getClaims().size() : 0;

        List<PaymentHistory> history = policy.getPaymentHistories() != null
                ? policy.getPaymentHistories()
                : List.of();
        ctx.recentPayments = new ArrayList<>(history);
        int missed = 0;
        int late = 0;
        boolean festive = false;
        for (PaymentHistory ph : history) {
            if (ph.isMissed()) {
                missed++;
                if (ph.getDueDate() != null) {
                    int month = ph.getDueDate().getMonthValue();
                    if (month == 3 || month == 10 || month == 11) {
                        festive = true;
                    }
                }
            }
            if (ph.isLate()) {
                late++;
            }
        }
        ctx.missedPayments = missed;
        ctx.latePayments = late;
        ctx.festiveMissPattern = festive;
        return ctx;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getFirstName() {
        if (customerName == null || customerName.isBlank()) {
            return "Customer";
        }
        return customerName.split("\\s+")[0];
    }

    public int getAge() {
        return age;
    }

    public String getOccupation() {
        return occupation;
    }

    public IncomeSegment getIncomeSegment() {
        return incomeSegment;
    }

    public String getRegion() {
        return region;
    }

    public PaymentMethod getPreferredPaymentMethod() {
        return preferredPaymentMethod;
    }

    public int getSalaryCreditDay() {
        return salaryCreditDay;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public BigDecimal getPremium() {
        return premium;
    }

    public boolean isAutoPay() {
        return autoPay;
    }

    public int getStoredRiskScore() {
        return storedRiskScore;
    }

    public int getMissedPayments() {
        return missedPayments;
    }

    public int getLatePayments() {
        return latePayments;
    }

    public int getClaimCount() {
        return claimCount;
    }

    public boolean isFestiveMissPattern() {
        return festiveMissPattern;
    }

    public List<PaymentHistory> getRecentPayments() {
        return recentPayments;
    }
}
