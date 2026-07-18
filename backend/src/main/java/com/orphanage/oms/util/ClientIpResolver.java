package com.orphanage.oms.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves the client IP from the current HTTP request (X-Forwarded-For aware).
 */
@Component
public class ClientIpResolver {

    /**
     * Resolves IP from an explicit request.
     *
     * @param request HTTP request
     * @return client IP or null
     */
    public String resolve(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return truncate(forwarded.split(",")[0].trim(), 45);
        }
        return truncate(request.getRemoteAddr(), 45);
    }

    /**
     * Resolves IP from the request bound to the current thread, if any.
     *
     * @return client IP or null when no request context exists
     */
    public String resolveCurrent() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        return resolve(attributes.getRequest());
    }

    private static String truncate(String value, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
