package com.capstone.billing.domain;

public enum AiRecommendationType {
    WHATSAPP_REMINDER("WhatsApp Reminder"),
    AGENT_CALL("Agent Call"),
    OFFER_INSTALLMENTS("Offer Installments"),
    AUTOPAY_DISCOUNT("AutoPay Discount"),
    GRACE_PERIOD("Grace Period"),
    WAIVE_LATE_FEE("Waive Late Fee"),
    FRIENDLY_REMINDER("Friendly Reminder"),
    DELAY_REMINDER("Delay Reminder Until Salary Credit");

    private final String label;

    AiRecommendationType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
