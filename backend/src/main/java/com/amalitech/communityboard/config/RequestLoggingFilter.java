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
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final long SLOW_REQUEST_THRESHOLD_MS = 1000L;

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
            String method = request.getMethod();
            String user = request.getUserPrincipal() != null
                    ? request.getUserPrincipal().getName()
                    : "anonymous";

            String message = String.format("[PERF] %s %s -> %d | %dms | user=%s",
                    method, uri, status, duration, user);

            if (duration > SLOW_REQUEST_THRESHOLD_MS) {
                log.warn("[SLOW REQUEST] {}", message);
            } else {
                log.info("{}", message);
            }
        }
    }
}
