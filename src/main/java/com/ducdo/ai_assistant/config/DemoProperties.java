package com.ducdo.ai_assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.demo")
public class DemoProperties {

    private boolean autoCleanupEnabled;
    private int cleanupIntervalHours;

    public boolean isAutoCleanupEnabled() {
        return autoCleanupEnabled;
    }

    public void setAutoCleanupEnabled(boolean autoCleanupEnabled) {
        this.autoCleanupEnabled = autoCleanupEnabled;
    }

    public int getCleanupIntervalHours() {
        return cleanupIntervalHours;
    }

    public void setCleanupIntervalHours(int cleanupIntervalHours) {
        this.cleanupIntervalHours = cleanupIntervalHours;
    }
}