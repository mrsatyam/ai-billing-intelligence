package com.capstone.billing.service.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class SimulatorResult {

    private int totalScanned;
    private int riskyCustomers;
    private int premiumLeakages;
    private int likelyToLapse;
    private BigDecimal potentialRecovery = BigDecimal.ZERO;
    private List<String> recommendations = new ArrayList<>();
    private List<String> highlights = new ArrayList<>();

    public int getTotalScanned() {
        return totalScanned;
    }

    public void setTotalScanned(int totalScanned) {
        this.totalScanned = totalScanned;
    }

    public int getRiskyCustomers() {
        return riskyCustomers;
    }

    public void setRiskyCustomers(int riskyCustomers) {
        this.riskyCustomers = riskyCustomers;
    }

    public int getPremiumLeakages() {
        return premiumLeakages;
    }

    public void setPremiumLeakages(int premiumLeakages) {
        this.premiumLeakages = premiumLeakages;
    }

    public int getLikelyToLapse() {
        return likelyToLapse;
    }

    public void setLikelyToLapse(int likelyToLapse) {
        this.likelyToLapse = likelyToLapse;
    }

    public BigDecimal getPotentialRecovery() {
        return potentialRecovery;
    }

    public void setPotentialRecovery(BigDecimal potentialRecovery) {
        this.potentialRecovery = potentialRecovery;
    }

    public List<String> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<String> recommendations) {
        this.recommendations = recommendations;
    }

    public List<String> getHighlights() {
        return highlights;
    }

    public void setHighlights(List<String> highlights) {
        this.highlights = highlights;
    }
}
