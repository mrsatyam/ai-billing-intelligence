package com.capstone.billing.ai.model;

import java.util.ArrayList;
import java.util.List;

public class DelinquencyPrediction {

    private int riskScore;
    private List<String> factors = new ArrayList<>();
    private String summary;

    public DelinquencyPrediction() {
    }

    public DelinquencyPrediction(int riskScore, List<String> factors, String summary) {
        this.riskScore = riskScore;
        this.factors = factors;
        this.summary = summary;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public List<String> getFactors() {
        return factors;
    }

    public void setFactors(List<String> factors) {
        this.factors = factors;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
