package com.capstone.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "billing.ai")
public class AiProperties {

    /**
     * auto = Gemini when API key present, else rules.
     * rules = force rule engine.
     * gemini = require Gemini (falls back to rules on error).
     */
    private String mode = "auto";
    private final Gemini gemini = new Gemini();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public Gemini getGemini() {
        return gemini;
    }

    public boolean useGemini() {
        if ("rules".equalsIgnoreCase(mode)) {
            return false;
        }
        return gemini.isConfigured();
    }

    public static class Gemini {
        private String apiKey = "";
        private String model = "gemini-3.1-flash-lite";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public boolean isConfigured() {
            return apiKey != null && !apiKey.isBlank();
        }
    }
}
