package com.capstone.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "billing.ai")
public class AiProperties {

    private String mode = "auto";
    private final OpenAi openai = new OpenAi();

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public OpenAi getOpenai() {
        return openai;
    }

    public static class OpenAi {
        private String apiKey = "";
        private String model = "gpt-4o-mini";
        private String baseUrl = "https://api.openai.com/v1";

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
