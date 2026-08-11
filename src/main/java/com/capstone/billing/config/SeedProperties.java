package com.capstone.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "billing.seed")
public class SeedProperties {

    /**
     * Number of policies to seed for local/demo runs (H2 Java seeder).
     */
    private int policyCount = 100;

    private boolean enabled = true;

    public int getPolicyCount() {
        return policyCount;
    }

    public void setPolicyCount(int policyCount) {
        this.policyCount = policyCount;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
