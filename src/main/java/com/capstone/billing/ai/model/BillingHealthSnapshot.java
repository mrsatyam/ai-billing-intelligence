package com.capstone.billing.ai.model;

/**
 * Lightweight KPI snapshot for rule-based chat answers.
 */
public class BillingHealthSnapshot {

    private long totalPolicies;
    private long policiesAtRisk;
    private long pendingRecommendations;
    private java.math.BigDecimal premiumDueToday;
    private java.math.BigDecimal predictedLeakage;
    private double collectionRate;

    public long getTotalPolicies() {
        return totalPolicies;
    }

    public void setTotalPolicies(long totalPolicies) {
        this.totalPolicies = totalPolicies;
    }

    public long getPoliciesAtRisk() {
        return policiesAtRisk;
    }

    public void setPoliciesAtRisk(long policiesAtRisk) {
        this.policiesAtRisk = policiesAtRisk;
    }

    public long getPendingRecommendations() {
        return pendingRecommendations;
    }

    public void setPendingRecommendations(long pendingRecommendations) {
        this.pendingRecommendations = pendingRecommendations;
    }

    public java.math.BigDecimal getPremiumDueToday() {
        return premiumDueToday;
    }

    public void setPremiumDueToday(java.math.BigDecimal premiumDueToday) {
        this.premiumDueToday = premiumDueToday;
    }

    public java.math.BigDecimal getPredictedLeakage() {
        return predictedLeakage;
    }

    public void setPredictedLeakage(java.math.BigDecimal predictedLeakage) {
        this.predictedLeakage = predictedLeakage;
    }

    public double getCollectionRate() {
        return collectionRate;
    }

    public void setCollectionRate(double collectionRate) {
        this.collectionRate = collectionRate;
    }
}
