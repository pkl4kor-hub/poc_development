package com.ecommerce.poc.dto;

public class ChatbotResponse {
    private String reply;
    private String matchedRule;

    public ChatbotResponse() {}

    public ChatbotResponse(String reply, String matchedRule) {
        this.reply = reply;
        this.matchedRule = matchedRule;
    }

    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public String getMatchedRule() { return matchedRule; }
    public void setMatchedRule(String matchedRule) { this.matchedRule = matchedRule; }
}
