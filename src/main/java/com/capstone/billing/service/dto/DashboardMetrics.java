package com.capstone.billing.service.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DashboardMetrics {

    private BigDecimal premiumDueToday = BigDecimal.ZERO;
    private double collectionRate;
    private long policiesAtRisk;
    private BigDecimal predictedRevenueLeakage = BigDecimal.ZERO;
    private long aiRecommendations;
    private long totalPolicies;
    private long approvedDecisions;
    private List<String> riskLabels = new ArrayList<>();
    private List<Long> riskCounts = new ArrayList<>();
    private List<String> regionLabels = new ArrayList<>();
    private List<Long> regionRiskCounts = new ArrayList<>();
    private List<String> collectionTrendLabels = new ArrayList<>();
    private List<Double> collectionTrendValues = new ArrayList<>();
    private Map<String, Object> extras;

    public BigDecimal getPremiumDueToday() {
        return premiumDueToday;
    }

    public void setPremiumDueToday(BigDecimal premiumDueToday) {
        this.premiumDueToday = premiumDueToday;
    }

    public double getCollectionRate() {
        return collectionRate;
    }

    public void setCollectionRate(double collectionRate) {
        this.collectionRate = collectionRate;
    }

    public long getPoliciesAtRisk() {
        return policiesAtRisk;
    }

    public void setPoliciesAtRisk(long policiesAtRisk) {
        this.policiesAtRisk = policiesAtRisk;
    }

    public BigDecimal getPredictedRevenueLeakage() {
        return predictedRevenueLeakage;
    }

    public void setPredictedRevenueLeakage(BigDecimal predictedRevenueLeakage) {
        this.predictedRevenueLeakage = predictedRevenueLeakage;
    }

    public long getAiRecommendations() {
        return aiRecommendations;
    }

    public void setAiRecommendations(long aiRecommendations) {
        this.aiRecommendations = aiRecommendations;
    }

    public long getTotalPolicies() {
        return totalPolicies;
    }

    public void setTotalPolicies(long totalPolicies) {
        this.totalPolicies = totalPolicies;
    }

    public long getApprovedDecisions() {
        return approvedDecisions;
    }

    public void setApprovedDecisions(long approvedDecisions) {
        this.approvedDecisions = approvedDecisions;
    }

    public List<String> getRiskLabels() {
        return riskLabels;
    }

    public void setRiskLabels(List<String> riskLabels) {
        this.riskLabels = riskLabels;
    }

    public List<Long> getRiskCounts() {
        return riskCounts;
    }

    public void setRiskCounts(List<Long> riskCounts) {
        this.riskCounts = riskCounts;
    }

    public List<String> getRegionLabels() {
        return regionLabels;
    }

    public void setRegionLabels(List<String> regionLabels) {
        this.regionLabels = regionLabels;
    }

    public List<Long> getRegionRiskCounts() {
        return regionRiskCounts;
    }

    public void setRegionRiskCounts(List<Long> regionRiskCounts) {
        this.regionRiskCounts = regionRiskCounts;
    }

    public List<String> getCollectionTrendLabels() {
        return collectionTrendLabels;
    }

    public void setCollectionTrendLabels(List<String> collectionTrendLabels) {
        this.collectionTrendLabels = collectionTrendLabels;
    }

    public List<Double> getCollectionTrendValues() {
        return collectionTrendValues;
    }

    public void setCollectionTrendValues(List<Double> collectionTrendValues) {
        this.collectionTrendValues = collectionTrendValues;
    }

    public Map<String, Object> getExtras() {
        return extras;
    }

    public void setExtras(Map<String, Object> extras) {
        this.extras = extras;
    }
}
