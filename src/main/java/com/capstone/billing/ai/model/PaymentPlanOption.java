package com.capstone.billing.ai.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class PaymentPlanOption {

    private int months;
    private BigDecimal monthlyAmount;
    private BigDecimal totalAmount;
    private boolean recommended;
    private String note;

    public PaymentPlanOption() {
    }

    public PaymentPlanOption(int months, BigDecimal monthlyAmount, BigDecimal totalAmount,
                             boolean recommended, String note) {
        this.months = months;
        this.monthlyAmount = monthlyAmount;
        this.totalAmount = totalAmount;
        this.recommended = recommended;
        this.note = note;
    }

    public int getMonths() {
        return months;
    }

    public void setMonths(int months) {
        this.months = months;
    }

    public BigDecimal getMonthlyAmount() {
        return monthlyAmount;
    }

    public void setMonthlyAmount(BigDecimal monthlyAmount) {
        this.monthlyAmount = monthlyAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
