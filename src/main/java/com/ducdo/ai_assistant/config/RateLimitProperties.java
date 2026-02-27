package com.ducdo.ai_assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class RateLimitProperties {

    private int askPer10Minutes;
    private int uploadPer10Minutes;

    public int getAskPer10Minutes() {
        return askPer10Minutes;
    }

    public void setAskPer10Minutes(int askPer10Minutes) {
        this.askPer10Minutes = askPer10Minutes;
    }

    public int getUploadPer10Minutes() {
        return uploadPer10Minutes;
    }

    public void setUploadPer10Minutes(int uploadPer10Minutes) {
        this.uploadPer10Minutes = uploadPer10Minutes;
    }
}