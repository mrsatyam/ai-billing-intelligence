package com.capstone.billing.ai.model;

public class GeneratedEmail {

    private String subject;
    private String body;
    private String tone;
    private String language;

    public GeneratedEmail() {
    }

    public GeneratedEmail(String subject, String body, String tone, String language) {
        this.subject = subject;
        this.body = body;
        this.tone = tone;
        this.language = language;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getTone() {
        return tone;
    }

    public void setTone(String tone) {
        this.tone = tone;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
