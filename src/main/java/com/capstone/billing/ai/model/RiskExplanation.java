package com.capstone.billing.ai.model;

import java.util.ArrayList;
import java.util.List;

public class RiskExplanation {

    private int riskScore;
    private String headline;
    private List<String> whyHighRisk = new ArrayList<>();
    private List<String> mitigatingFactors = new ArrayList<>();
    private String narrative;

    public int getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(int riskScore) {
        this.riskScore = riskScore;
    }

    public String getHeadline() {
        return headline;
    }

    public void setHeadline(String headline) {
        this.headline = headline;
    }

    public List<String> getWhyHighRisk() {
        return whyHighRisk;
    }

    public void setWhyHighRisk(List<String> whyHighRisk) {
        this.whyHighRisk = whyHighRisk;
    }

    public List<String> getMitigatingFactors() {
        return mitigatingFactors;
    }

    public void setMitigatingFactors(List<String> mitigatingFactors) {
        this.mitigatingFactors = mitigatingFactors;
    }

    public String getNarrative() {
        return narrative;
    }

    public void setNarrative(String narrative) {
        this.narrative = narrative;
    }
}
