package com.capstone.billing.service.dto;

public class AtRiskPolicyRow {

    private Long policyId;
    private String policyNumber;
    private String customerName;
    private int riskScore;
    private String recommendation;
    private Long decisionId;

    public AtRiskPolicyRow() {
    }

    public AtRiskPolicyRow(Long policyId, String policyNumber, String customerName,
                           int riskScore, String recommendation, Long decisionId) {
        this.policyId = policyId;
        this.policyNumber = policyNumber;
        this.customerName = customerName;
        this.riskScore = riskScore;
        this.recommendation = recommendation;
        this.decisionId = decisionId;
    }

    public Long getPolicyId() {
        return policyId;
    }

    public void setPolicyId(Long policyId) {
        this.policyId = policyId;
    }

    public String getPolicyNumber() {
        return policyNumber;
    }

    public void setPolicyNumber(String policyNumber) {
        this.policyNumber = policyNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }

    public Long getDecisionId() {
        return decisionId;
    }

    public void setDecisionId(Long decisionId) {
        this.decisionId = decisionId;
    }
}
