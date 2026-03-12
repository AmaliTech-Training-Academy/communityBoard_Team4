package com.amalitech.communityboard.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Logs every HTTP request with method, URI, status, duration, and authenticated user.
 * Output goes to the performance log (communityboard-performance.log) via logback routing.
 * Requests to /actuator/** are skipped to prevent health-check polling from flooding the log.
 *
 * All user-controlled values (URI, principal) are sanitized before logging to prevent
 * log injection attacks (OWASP A09 / CWE-117).
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000L;

    /**
     * Strips CR, LF, and other control characters from user-supplied strings
     * to prevent log forging / log injection (CWE-117).
     */
    private static String sanitize(String value) {
        if (value == null) {
            return "null";
        }
        return value.replaceAll("[\r\n\t]", "_");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();

        // Skip actuator paths — health-check polling would create excessive noise
        if (uri.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            int status = response.getStatus();
            // Sanitize all user-controlled values before including in log output
            String safeMethod = sanitize(request.getMethod());
            String safeUri = sanitize(uri);
            String safeUser = request.getUserPrincipal() != null
                    ? sanitize(request.getUserPrincipal().getName())
                    : "anonymous";

            if (duration > SLOW_REQUEST_THRESHOLD_MS) {
                log.warn("[SLOW REQUEST] [PERF] {} {} -> {} | {}ms | user={}",
                        safeMethod, safeUri, status, duration, safeUser);
            } else {
                log.info("[PERF] {} {} -> {} | {}ms | user={}",
                        safeMethod, safeUri, status, duration, safeUser);
            }
        }
    }
}
