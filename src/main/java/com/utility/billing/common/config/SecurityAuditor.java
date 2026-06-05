package com.utility.billing.common.config;

import java.util.UUID;

public final class SecurityAuditor {

    private static final ThreadLocal<UUID> CURRENT_USER = new ThreadLocal<>();

    private SecurityAuditor() {
    }

    public static void setCurrentUserId(UUID userId) {
        CURRENT_USER.set(userId);
    }

    public static UUID getCurrentUserId() {
        return CURRENT_USER.get();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
