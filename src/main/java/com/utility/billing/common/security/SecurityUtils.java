package com.utility.billing.common.security;

import com.utility.billing.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID userId) {
            return userId;
        }
        return null;
    }

    public static UUID requireCurrentUserId() {
        UUID userId = getCurrentUserId();
        if (userId == null) {
            throw new BusinessException("Authentication required");
        }
        return userId;
    }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

    public static void assertCustomerOwns(UUID resourceUserId) {
        if (hasRole(RoleName.CUSTOMER)) {
            UUID current = requireCurrentUserId();
            if (resourceUserId == null || !current.equals(resourceUserId)) {
                throw new BusinessException("Access denied");
            }
        }
    }

    public static UUID resolveUserScope(UUID requestedUserId) {
        if (hasRole(RoleName.CUSTOMER)) {
            UUID current = requireCurrentUserId();
            if (requestedUserId != null && !requestedUserId.equals(current)) {
                throw new BusinessException("Access denied");
            }
            return current;
        }
        return requestedUserId;
    }
}
