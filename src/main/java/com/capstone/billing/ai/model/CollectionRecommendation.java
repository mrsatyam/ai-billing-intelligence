package com.capstone.billing.ai.model;

import com.capstone.billing.domain.AiRecommendationType;

import java.util.ArrayList;
import java.util.List;

public class CollectionRecommendation {

    private AiRecommendationType primaryAction;
    private List<AiRecommendationType> supportingActions = new ArrayList<>();
    private String reasoning;
    private int predictedSuccess;
    private List<String> avoidActions = new ArrayList<>();

    public AiRecommendationType getPrimaryAction() {
        return primaryAction;
    }

    public void setPrimaryAction(AiRecommendationType primaryAction) {
        this.primaryAction = primaryAction;
    }

    public List<AiRecommendationType> getSupportingActions() {
        return supportingActions;
    }

    public void setSupportingActions(List<AiRecommendationType> supportingActions) {
        this.supportingActions = supportingActions;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public int getPredictedSuccess() {
        return predictedSuccess;
    }

    public void setPredictedSuccess(int predictedSuccess) {
        this.predictedSuccess = predictedSuccess;
    }

    public List<String> getAvoidActions() {
        return avoidActions;
    }

    public void setAvoidActions(List<String> avoidActions) {
        this.avoidActions = avoidActions;
    }
}
