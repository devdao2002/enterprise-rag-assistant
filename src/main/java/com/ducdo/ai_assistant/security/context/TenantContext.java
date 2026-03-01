package com.ducdo.ai_assistant.security.context;

import java.util.UUID;

public class TenantContext {

    private static final ThreadLocal<UUID> currentTenant = new ThreadLocal<>();

    public static void set(UUID tenantId) {
        currentTenant.set(tenantId);
    }

    public static UUID get() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}