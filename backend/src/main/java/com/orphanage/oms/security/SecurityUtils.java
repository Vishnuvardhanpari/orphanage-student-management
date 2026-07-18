package com.orphanage.oms.security;

import com.orphanage.oms.exception.ApiException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helpers for reading the current {@link UserPrincipal} from the security context.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Optional<UserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return Optional.empty();
        }
        return Optional.of(principal);
    }

    public static UserPrincipal requirePrincipal() {
        return currentPrincipal()
                .orElseThrow(() -> new ApiException(
                        HttpStatus.UNAUTHORIZED, "Unauthorized", "Authentication required."));
    }

    public static UUID requireUserId() {
        return requirePrincipal().getId();
    }

    public static String requireUsername() {
        return requirePrincipal().getUsername();
    }

    public static Optional<String> currentUsername() {
        return currentPrincipal().map(UserPrincipal::getUsername);
    }
}
