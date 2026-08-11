package com.capstone.billing.ai.model;

import java.util.ArrayList;
import java.util.List;

public class PaymentPlanProposal {

    private List<PaymentPlanOption> options = new ArrayList<>();
    private PaymentPlanOption bestOption;
    private String rationale;

    public List<PaymentPlanOption> getOptions() {
        return options;
    }

    public void setOptions(List<PaymentPlanOption> options) {
        this.options = options;
    }

    public PaymentPlanOption getBestOption() {
        return bestOption;
    }

    public void setBestOption(PaymentPlanOption bestOption) {
        this.bestOption = bestOption;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}
