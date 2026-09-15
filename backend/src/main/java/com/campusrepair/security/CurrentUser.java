package com.campusrepair.security;

import com.campusrepair.common.BusinessException;
import com.campusrepair.common.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

/** Reads the authenticated user identity established by the JWT filter. */
public final class CurrentUser {
    private CurrentUser() { }

    public static Long requireId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return principalToUserId(authentication.getPrincipal());
    }

    private static Long principalToUserId(Object principal) {
        try {
            if (principal instanceof Number number) return number.longValue();
            String username = principal instanceof UserDetails details
                    ? details.getUsername()
                    : String.valueOf(principal);
            return Long.valueOf(username);
        } catch (NumberFormatException ex) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
    }
}
